package net.mellow.nbtlib.api.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cpw.mods.fml.common.registry.GameRegistry;
import net.mellow.nbtlib.Config;
import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.api.BlockMeta;
import net.mellow.nbtlib.block.BlockPos;
import net.mellow.nbtlib.block.BlockReplace;
import net.mellow.nbtlib.block.ModBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

public class NBTFormatProvider implements IStructureProvider {

    @Override
    public String getFileExtension() {
        return "nbt";
    }

    @Override
    public void saveArea(OutputStream stream, World world, int x1, int y1, int z1, int x2, int y2, int z2, Set<BlockMeta> exclude) {
        try {
            CompressedStreamTools.writeCompressed(doSaveArea(world, x1, y1, z1, x2, y2, z2, exclude), stream);
        } catch (IOException ex) {
            Registry.LOG.warn("Failed to save NBT structure", ex);
        }
    }

    // Saves a selected area into an NBT structure (+ some of our non-standard stuff to support 1.7.10)
    private static NBTTagCompound doSaveArea(World world, int x1, int y1, int z1, int x2, int y2, int z2, Set<BlockMeta> exclude) {
        NBTTagCompound structure = new NBTTagCompound();
        NBTTagList nbtBlocks = new NBTTagList();
        NBTTagList nbtPalette = new NBTTagList();
        NBTTagList nbtItemPalette = new NBTTagList();

        // Quick access hash slinging slashers
        Map<BlockMeta, Integer> palette = new HashMap<>();
        Map<Short, Integer> itemPalette = new HashMap<>();

        structure.setInteger("version", 1);

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    BlockMeta block = new BlockMeta(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));

                    if (exclude.contains(block)) continue;

                    // bock bock I'm a chicken
                    if (block.block instanceof BlockReplace) {
                        block = new BlockMeta(((BlockReplace) block.block).exportAs, block.meta);
                    }

                    int paletteId = palette.size();
                    if (palette.containsKey(block)) {
                        paletteId = palette.get(block);
                    } else {
                        palette.put(block, paletteId);

                        NBTTagCompound nbtBlock = new NBTTagCompound();
                        nbtBlock.setString("Name", GameRegistry.findUniqueIdentifierFor(block.block).toString());

                        NBTTagCompound nbtProp = new NBTTagCompound();
                        nbtProp.setString("meta", new Integer(block.meta).toString());

                        nbtBlock.setTag("Properties", nbtProp);

                        nbtPalette.appendTag(nbtBlock);
                    }

                    NBTTagCompound nbtBlock = new NBTTagCompound();
                    nbtBlock.setInteger("state", paletteId);

                    NBTTagList nbtPos = new NBTTagList();
                    nbtPos.appendTag(new NBTTagInt(x - x1));
                    nbtPos.appendTag(new NBTTagInt(y - y1));
                    nbtPos.appendTag(new NBTTagInt(z - z1));

                    nbtBlock.setTag("pos", nbtPos);

                    TileEntity te = world.getTileEntity(x, y, z);
                    if (te != null) {
                        NBTTagCompound nbt = new NBTTagCompound();
                        te.writeToNBT(nbt);

                        nbt.removeTag("x");
                        nbt.removeTag("y");
                        nbt.removeTag("z");

                        nbtBlock.setTag("nbt", nbt);

                        String itemKey = null;
                        if (nbt.hasKey("items"))
                            itemKey = "items";
                        if (nbt.hasKey("Items"))
                            itemKey = "Items";

                        if (nbt.hasKey(itemKey)) {
                            NBTTagList items = nbt.getTagList(itemKey, NBT.TAG_COMPOUND);
                            for (int i = 0; i < items.tagCount(); i++) {
                                NBTTagCompound item = items.getCompoundTagAt(i);
                                short id = item.getShort("id");
                                String name = GameRegistry.findUniqueIdentifierFor(Item.getItemById(id)).toString();

                                if (!itemPalette.containsKey(id)) {
                                    int itemPaletteId = itemPalette.size();
                                    itemPalette.put(id, itemPaletteId);

                                    NBTTagCompound nbtItem = new NBTTagCompound();
                                    nbtItem.setShort("ID", id);
                                    nbtItem.setString("Name", name);

                                    nbtItemPalette.appendTag(nbtItem);
                                }
                            }
                        }
                    }

                    nbtBlocks.appendTag(nbtBlock);
                }
            }
        }

        structure.setTag("blocks", nbtBlocks);
        structure.setTag("palette", nbtPalette);
        structure.setTag("itemPalette", nbtItemPalette);

        NBTTagList nbtSize = new NBTTagList();
        nbtSize.appendTag(new NBTTagInt(Math.abs(x1 - x2) + 1));
        nbtSize.appendTag(new NBTTagInt(Math.abs(y1 - y2) + 1));
        nbtSize.appendTag(new NBTTagInt(Math.abs(z1 - z2) + 1));
        structure.setTag("size", nbtSize);

        structure.setTag("entities", new NBTTagList());

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

        // GET SIZE (for offsetting to center)
        structure.size = parsePos(data.getTagList("size", NBT.TAG_INT));

        // PARSE BLOCK PALETTE
        NBTTagList paletteList = data.getTagList("palette", NBT.TAG_COMPOUND);
        BlockMeta[] palette = new BlockMeta[paletteList.tagCount()];

        for (int i = 0; i < paletteList.tagCount(); i++) {
            NBTTagCompound p = paletteList.getCompoundTagAt(i);

            String blockName = p.getString("Name");
            NBTTagCompound prop = p.getCompoundTag("Properties");

            int meta = 0;
            try {
                meta = Integer.parseInt(prop.getString("meta"));
            } catch (NumberFormatException ex) {
                Registry.LOG.info("Failed to parse: " + prop.getString("meta"));
                meta = 0;
            }

            palette[i] = new BlockMeta(blockName, meta);

            if (!Config.debugStructures && palette[i].block == ModBlocks.structure_block) {
                palette[i] = new BlockMeta(Blocks.air, 0);
            }

            if (Config.debugStructures && palette[i].block == Blocks.air) {
                palette[i] = new BlockMeta(ModBlocks.structure_air, meta);
            }
        }

        // PARSE ITEM PALETTE (custom shite)
        if (data.hasKey("itemPalette")) {
            NBTTagList itemPaletteList = data.getTagList("itemPalette", NBT.TAG_COMPOUND);
            structure.itemPalette = new ArrayList<>(itemPaletteList.tagCount());

            for (int i = 0; i < itemPaletteList.tagCount(); i++) {
                NBTTagCompound p = itemPaletteList.getCompoundTagAt(i);

                short id = p.getShort("ID");
                String name = p.getString("Name");

                structure.itemPalette.add(new ItemPaletteEntry(id, name));
            }
        } else {
            structure.itemPalette = null;
        }

        // LOAD IN BLOCKS
        NBTTagList blockData = data.getTagList("blocks", NBT.TAG_COMPOUND);
        structure.blockArray = new BlockState[structure.size.x][structure.size.y][structure.size.z];

        for (int i = 0; i < blockData.tagCount(); i++) {
            NBTTagCompound block = blockData.getCompoundTagAt(i);
            int state = block.getInteger("state");
            BlockPos pos = parsePos(block.getTagList("pos", NBT.TAG_INT));

            BlockState blockState = new BlockState(palette[state]);

            if (block.hasKey("nbt")) {
                NBTTagCompound nbt = block.getCompoundTag("nbt");
                blockState.nbt = nbt;
            }

            structure.blockArray[pos.x][pos.y][pos.z] = blockState;
        }

        return structure;
    }

    // What a fucken mess, why even implement the IntArray NBT if ye aint gonna use
    // it Moe Yang?
    private static BlockPos parsePos(NBTTagList pos) {
        NBTBase xb = (NBTBase) pos.tagList.get(0);
        int x = ((NBTTagInt) xb).func_150287_d();
        NBTBase yb = (NBTBase) pos.tagList.get(1);
        int y = ((NBTTagInt) yb).func_150287_d();
        NBTBase zb = (NBTBase) pos.tagList.get(2);
        int z = ((NBTTagInt) zb).func_150287_d();

        return new BlockPos(x, y, z);
    }

}
