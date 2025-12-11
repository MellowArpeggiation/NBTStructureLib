package net.mellow.nbtlib.api.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import cpw.mods.fml.common.registry.GameRegistry;
import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.api.BlockMeta;
import net.mellow.nbtlib.api.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.Constants.NBT;

public class SchematicFormatProvider implements IStructureProvider {

    @Override
    public String getFileExtension() {
        return "schematic";
    }

    @Override
    public void saveArea(OutputStream stream, World world, int x1, int y1, int z1, int x2, int y2, int z2, Set<BlockMeta> exclude) {
        try {
            CompressedStreamTools.writeCompressed(doSaveArea(world, x1, y1, z1, x2, y2, z2, exclude), stream);
        } catch (IOException ex) {
            Registry.LOG.warn("Failed to save NBT structure", ex);
        }
    }

    private static NBTTagCompound doSaveArea(World world, int x1, int y1, int z1, int x2, int y2, int z2, Set<BlockMeta> exclude) {
        NBTTagCompound structure = new NBTTagCompound();

        NBTTagList nbtTileEntities = new NBTTagList();
        NBTTagCompound nbtBlockIds = new NBTTagCompound();
        NBTTagCompound nbtSchematicaIds = new NBTTagCompound(); // HOW MANY FUCKING WAYS OF SERIALIZING THE SAME SHIT DO YOU WANT??
        NBTTagCompound nbtItemIds = new NBTTagCompound();

        short width = (short)(Math.abs(x1 - x2) + 1);
        short height = (short)(Math.abs(y1 - y2) + 1);
        short length = (short)(Math.abs(z1 - z2) + 1);
        int arraySize = width * height * length;

        // horrors
        byte[] blocks = new byte[arraySize];
        byte[] add = new byte[arraySize];
        byte[] addNibble = new byte[(int) Math.ceil(arraySize / 2.0)];
        byte[] meta = new byte[arraySize];

        for (int ox = x1; ox <= x2; ox++)
        for (int oy = y1; oy <= y2; oy++)
        for (int oz = z1; oz <= z2; oz++) {
            BlockMeta definition = IStructureProvider.fetchBlockMeta(world, ox, oy, oz, exclude);

            if (definition == null) continue;

            int x = ox - x1;
            int y = oy - y1;
            int z = oz - z1;

            int index = (y * length + z) * width + x;

            int id = Block.getIdFromBlock(definition.block);

            blocks[index] = (byte)(id & 255);
            add[index] = (byte)(id >> 8);

            meta[index] = (byte)definition.meta;

            if (!nbtBlockIds.hasKey(id + "")) {
                String blockName = GameRegistry.findUniqueIdentifierFor(definition.block).toString();
                nbtBlockIds.setString(id + "", blockName);
                nbtSchematicaIds.setShort(blockName, (short)id); // It's the same shit but reversed I fucking swear to god
            }

            TileEntity te = world.getTileEntity(x, y, z);
            if (te != null) {
                NBTTagCompound nbt = new NBTTagCompound();
                te.writeToNBT(nbt);

                nbt.setInteger("x", te.xCoord - x1);
                nbt.setInteger("y", te.yCoord - y1);
                nbt.setInteger("z", te.zCoord - z1);

                String itemKey = null;
                if (nbt.hasKey("items"))
                    itemKey = "items";
                if (nbt.hasKey("Items"))
                    itemKey = "Items";

                if (nbt.hasKey(itemKey)) {
                    NBTTagList items = nbt.getTagList(itemKey, NBT.TAG_COMPOUND);
                    for (int i = 0; i < items.tagCount(); i++) {
                        NBTTagCompound item = items.getCompoundTagAt(i);
                        short itemId = item.getShort("id");

                        if (!nbtItemIds.hasKey(itemId + "")) {
                            nbtItemIds.setString(itemId + "", GameRegistry.findUniqueIdentifierFor(Item.getItemById(itemId)).toString());
                        }
                    }
                }

                nbtTileEntities.appendTag(nbt);
            }
        }

        for (int i = 0; i < addNibble.length; i++) {
            if (i * 2 + 1 < add.length) {
                addNibble[i] = (byte) ((add[i * 2 + 0] << 4) | add[i * 2 + 1]);
            } else {
                addNibble[i] = (byte) (add[i * 2 + 0] << 4);
            }
        }


        structure.setShort("Width", width);
        structure.setShort("Height", height);
        structure.setShort("Length", length);

        structure.setString("Materials", "Alpha"); // lmao

        structure.setByteArray("Blocks", blocks);
        structure.setByteArray("AddBlocks", addNibble);
        structure.setByteArray("Data", meta);

        structure.setTag("Entities", new NBTTagList());
        structure.setTag("TileEntities", nbtTileEntities);

        structure.setTag("SchematicaMapping", nbtSchematicaIds);

        structure.setByte("itemStackVersion", (byte)17);
        structure.setTag("BlockIDs", nbtBlockIds);
        structure.setTag("ItemIDs", nbtItemIds);

        return structure;
    }

    @Override
    public NBTStructureData loadStructure(InputStream stream) {
        try {
            NBTTagCompound data = CompressedStreamTools.readCompressed(stream);

            return doLoadStructure(data);
        } catch (IOException ex) {
            Registry.LOG.error("Exception reading NBT Structure format", ex);
        }

        return null;
    }

    private static NBTStructureData doLoadStructure(NBTTagCompound data) {
        NBTStructureData structure = new NBTStructureData();

        short width = data.getShort("Width");
        short height = data.getShort("Height");
        short length = data.getShort("Length");

        // get size
        structure.size = new BlockPos(width, height, length);

        // parse block palette
        HashMap<Short, Block> palette = new HashMap<>();

        NBTTagCompound blockIds = data.getCompoundTag("BlockIDs");
        for (String blockIdString : blockIds.func_150296_c()) {
            short blockId = Short.parseShort(blockIdString);
            Block block = Registry.proxy.getBlockFromName(blockIds.getString(blockIdString));

            palette.put(blockId, block);
        }

        // parse item palette
        structure.itemPalette = new ArrayList<>();

        NBTTagCompound itemIds = data.getCompoundTag("ItemIDs");
        for (String itemIdString : itemIds.func_150296_c()) {
            short itemId = Short.parseShort(itemIdString);
            String itemName = itemIds.getString(itemIdString);

            structure.itemPalette.add(new ItemPaletteEntry(itemId, itemName));
        }

        // load in blocks
        structure.blockArray = new BlockState[structure.size.x][structure.size.y][structure.size.z];

        boolean hasExtra = data.hasKey("AddBlocks");

        byte[] blocks = data.getByteArray("Blocks");
        byte[] addNibble = data.getByteArray("AddBlocks");
        byte[] meta = data.getByteArray("Data");

        byte[] add = new byte[addNibble.length * 2];
        for (int i = 0; i < addNibble.length; i++) {
            add[i * 2 + 0] = (byte) ((addNibble[i] >> 4) & 0xF);
            add[i * 2 + 1] = (byte) (addNibble[i] & 0xF);
        }

        for (int x = 0; x < width; x++)
        for (int y = 0; y < height; y++)
        for (int z = 0; z < length; z++) {
            int index = (y * length + z) * width + x;

            short blockId = (short)((blocks[index] & 0xFF) | (hasExtra ? ((add[index] & 0xFF) << 8) : 0));
            byte blockMeta = meta[index];

            Block block = palette.get(blockId);
            if (block == null) block = Block.getBlockById(blockId);

            BlockMeta definition = new BlockMeta(block, blockMeta);

            structure.blockArray[x][y][z] = new BlockState(definition);
        }

        // map TE NBT back to blocks
        NBTTagList nbtTileEntities = data.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < nbtTileEntities.tagCount(); i++) {
            NBTTagCompound nbt = nbtTileEntities.getCompoundTagAt(i);

            int x = nbt.getInteger("x");
            int y = nbt.getInteger("y");
            int z = nbt.getInteger("z");

            structure.blockArray[x][y][z].nbt = nbt;
        }

        return structure;
    }

}
