package net.mellow.nbtlib.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import cpw.mods.fml.common.registry.GameRegistry;
import net.mellow.nbtlib.Config;
import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.block.BlockPos;
import net.mellow.nbtlib.block.BlockReplace;
import net.mellow.nbtlib.block.FloorPos;
import net.mellow.nbtlib.block.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent.BlockSelector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.common.util.Constants.NBT;

/**
 * Handles placing blocks into the world based on an .nbt file (modern MC format)
 */
public class NBTStructure {

    protected String name;

    private boolean isLoaded;

    protected BlockPos size;
    private List<ItemPaletteEntry> itemPalette;
    private BlockState[][][] blockArray;

    protected List<List<JigsawConnection>> fromConnections;
    protected Map<String, List<JigsawConnection>> toTopConnections;
    protected Map<String, List<JigsawConnection>> toBottomConnections;
    protected Map<String, List<JigsawConnection>> toHorizontalConnections;

    public NBTStructure(ResourceLocation resource) {
        // Can't use regular resource loading, servers don't know how!
        InputStream stream = NBTStructure.class.getResourceAsStream("/assets/" + resource.getResourceDomain() + "/" + resource.getResourcePath());
        if (stream != null) {
            name = resource.getResourcePath();
            loadStructure(stream);
        } else {
            Registry.LOG.error("NBT Structure not found: " + resource.getResourcePath());
        }
    }

    // Build a piece with a default rotation (NOT cascade safe!)
    public void build(World world, int x, int y, int z) {
        build(world, x, y, z, 0);
    }

    // Build a piece with a specified rotation (NOT cascade safe!)
    public void build(World world, int x, int y, int z, int coordBaseMode) {
        if (!isLoaded) {
            Registry.LOG.info("NBTStructure is invalid");
            return;
        }

        HashMap<Short, Short> worldItemPalette = getWorldItemPalette();

        boolean swizzle = coordBaseMode == 1 || coordBaseMode == 3;
        x -= (swizzle ? size.z : size.x) / 2;
        z -= (swizzle ? size.x : size.z) / 2;

        int maxX = size.x;
        int maxZ = size.z;

        for (int bx = 0; bx < maxX; bx++) {
            for (int bz = 0; bz < maxZ; bz++) {
                int rx = rotateX(bx, bz, coordBaseMode) + x;
                int rz = rotateZ(bx, bz, coordBaseMode) + z;

                for (int by = 0; by < size.y; by++) {
                    BlockState state = blockArray[bx][by][bz];
                    if (state == null) continue;

                    int ry = by + y;

                    Block block = transformBlock(state.definition, null, world.rand);
                    int meta = transformMeta(state.definition, null, coordBaseMode);

                    world.setBlock(rx, ry, rz, block, meta, 2);

                    if (state.nbt != null) {
                        TileEntity te = buildTileEntity(world, block, worldItemPalette, state.nbt, coordBaseMode);
                        world.setTileEntity(rx, ry, rz, te);
                    }
                }
            }
        }
    }

    // Builds a structure piece within a given bounds (will not cascade, assuming the bounds provided are valid!)
    public boolean build(World world, JigsawPiece piece, StructureBoundingBox totalBounds, StructureBoundingBox generatingBounds, int coordBaseMode, LinkedHashMap<FloorPos, Integer> floorplan) {
        if (!isLoaded) {
            Registry.LOG.info("NBTStructure is invalid");
            return false;
        }

        HashMap<Short, Short> worldItemPalette = getWorldItemPalette();

        int sizeX = totalBounds.maxX - totalBounds.minX;
        int sizeZ = totalBounds.maxZ - totalBounds.minZ;

        // voxel grid transforms can fuck you up
        // you have my respect, vaer
        int absMinX = Math.max(generatingBounds.minX - totalBounds.minX, 0);
        int absMaxX = Math.min(generatingBounds.maxX - totalBounds.minX, sizeX);
        int absMinZ = Math.max(generatingBounds.minZ - totalBounds.minZ, 0);
        int absMaxZ = Math.min(generatingBounds.maxZ - totalBounds.minZ, sizeZ);

        // A check to see that we're actually inside the generating area at all
        if (absMinX > sizeX || absMaxX < 0 || absMinZ > sizeZ || absMaxZ < 0)
            return true;

        int rotMinX = unrotateX(absMinX, absMinZ, coordBaseMode);
        int rotMaxX = unrotateX(absMaxX, absMaxZ, coordBaseMode);
        int rotMinZ = unrotateZ(absMinX, absMinZ, coordBaseMode);
        int rotMaxZ = unrotateZ(absMaxX, absMaxZ, coordBaseMode);

        int minX = Math.min(rotMinX, rotMaxX);
        int maxX = Math.max(rotMinX, rotMaxX);
        int minZ = Math.min(rotMinZ, rotMaxZ);
        int maxZ = Math.max(rotMinZ, rotMaxZ);

        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                int rx = rotateX(bx, bz, coordBaseMode) + totalBounds.minX;
                int rz = rotateZ(bx, bz, coordBaseMode) + totalBounds.minZ;
                int oy = piece.conformToTerrain ? world.getTopSolidOrLiquidBlock(rx, rz) + piece.heightOffset : totalBounds.minY;

                boolean hasColumn = false;

                for (int by = 0; by < size.y; by++) {
                    BlockState state = blockArray[bx][by][bz];
                    if (state == null) continue;

                    hasColumn = true;

                    int ry = by + oy;

                    Block block = transformBlock(state.definition, piece.blockTable, world.rand);
                    int meta = transformMeta(state.definition, piece.blockTable, coordBaseMode);

                    world.setBlock(rx, ry, rz, block, meta, 2);

                    if (state.nbt != null) {
                        TileEntity te = buildTileEntity(world, block, worldItemPalette, state.nbt, coordBaseMode);
                        world.setTileEntity(rx, ry, rz, te);
                    }
                }

                if (hasColumn && !piece.conformToTerrain) {
                    FloorPos floor = new FloorPos(rx, rz);
                    Integer ry = floorplan.get(floor);
                    if (ry == null || ry > oy - 1)
                        floorplan.put(floor, oy - 1);
                }
            }
        }

        return true;
    }

    // Saves a selected area into an NBT structure (+ some of our non-standard stuff to support 1.7.10)
    public static NBTTagCompound saveArea(World world, int x1, int y1, int z1, int x2, int y2, int z2, Set<BlockMeta> exclude) {
        NBTTagCompound structure = new NBTTagCompound();
        NBTTagList nbtBlocks = new NBTTagList();
        NBTTagList nbtPalette = new NBTTagList();
        NBTTagList nbtItemPalette = new NBTTagList();

        // Quick access hash slinging slashers
        Map<BlockMeta, Integer> palette = new HashMap<>();
        Map<Short, Integer> itemPalette = new HashMap<>();

        structure.setInteger("version", 1);

        int ox = Math.min(x1, x2);
        int oy = Math.min(y1, y2);
        int oz = Math.min(z1, z2);

        for (int x = ox; x <= Math.max(x1, x2); x++) {
            for (int y = oy; y <= Math.max(y1, y2); y++) {
                for (int z = oz; z <= Math.max(z1, z2); z++) {
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
                    nbtPos.appendTag(new NBTTagInt(x - ox));
                    nbtPos.appendTag(new NBTTagInt(y - oy));
                    nbtPos.appendTag(new NBTTagInt(z - oz));

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

    // Writes out a specified area to an .nbt file with a given name
    public static void quickSaveArea(String filename, World world, int x1, int y1, int z1, int x2, int y2, int z2, Set<BlockMeta> exclude) {
        NBTTagCompound structure = saveArea(world, x1, y1, z1, x2, y2, z2, exclude);

        try {
            File structureDirectory = new File(Minecraft.getMinecraft().mcDataDir, "structures");
            structureDirectory.mkdir();

            File structureFile = new File(structureDirectory, filename);

            CompressedStreamTools.writeCompressed(structure, new FileOutputStream(structureFile));
        } catch (Exception ex) {
            Registry.LOG.warn("Failed to save NBT structure", ex);
        }
    }

    private void loadStructure(InputStream inputStream) {
        try {
            NBTTagCompound data = CompressedStreamTools.readCompressed(inputStream);

            // GET SIZE (for offsetting to center)
            size = parsePos(data.getTagList("size", NBT.TAG_INT));

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

                if (Config.debugStructures && palette[i].block == Blocks.air) {
                    palette[i] = new BlockMeta(ModBlocks.structure_air, meta);
                }
            }

            // PARSE ITEM PALETTE (custom shite)
            if (data.hasKey("itemPalette")) {
                NBTTagList itemPaletteList = data.getTagList("itemPalette", NBT.TAG_COMPOUND);
                itemPalette = new ArrayList<>(itemPaletteList.tagCount());

                for (int i = 0; i < itemPaletteList.tagCount(); i++) {
                    NBTTagCompound p = itemPaletteList.getCompoundTagAt(i);

                    short id = p.getShort("ID");
                    String name = p.getString("Name");

                    itemPalette.add(new ItemPaletteEntry(id, name));
                }
            } else {
                itemPalette = null;
            }

            // LOAD IN BLOCKS
            NBTTagList blockData = data.getTagList("blocks", NBT.TAG_COMPOUND);
            blockArray = new BlockState[size.x][size.y][size.z];

            List<JigsawConnection> connections = new ArrayList<>();

            for (int i = 0; i < blockData.tagCount(); i++) {
                NBTTagCompound block = blockData.getCompoundTagAt(i);
                int state = block.getInteger("state");
                BlockPos pos = parsePos(block.getTagList("pos", NBT.TAG_INT));

                BlockState blockState = new BlockState(palette[state]);

                if (block.hasKey("nbt")) {
                    NBTTagCompound nbt = block.getCompoundTag("nbt");
                    blockState.nbt = nbt;

                    // Load in connection points for jigsaws
                    if (blockState.definition.block == ModBlocks.structure_jigsaw) {
                        if (toTopConnections == null) toTopConnections = new HashMap<>();
                        if (toBottomConnections == null) toBottomConnections = new HashMap<>();
                        if (toHorizontalConnections == null) toHorizontalConnections = new HashMap<>();

                        int selectionPriority = nbt.getInteger("selection");
                        int placementPriority = nbt.getInteger("placement");
                        ForgeDirection direction = ForgeDirection.getOrientation(nbt.getInteger("direction"));
                        String poolName = nbt.getString("pool");
                        String ourName = nbt.getString("name");
                        String targetName = nbt.getString("target");
                        String replaceBlock = nbt.getString("block");
                        int replaceMeta = nbt.getInteger("meta");
                        boolean isRollable = nbt.getBoolean("roll");

                        JigsawConnection connection = new JigsawConnection(pos, direction, poolName, targetName, isRollable, selectionPriority, placementPriority);

                        connections.add(connection);

                        Map<String, List<JigsawConnection>> toConnections = null;
                        if (direction == ForgeDirection.UP) {
                            toConnections = toTopConnections;
                        } else if (direction == ForgeDirection.DOWN) {
                            toConnections = toBottomConnections;
                        } else {
                            toConnections = toHorizontalConnections;
                        }

                        List<JigsawConnection> namedConnections = toConnections.computeIfAbsent(ourName, name -> new ArrayList<>());
                        namedConnections.add(connection);

                        if (!Config.debugStructures) {
                            blockState = new BlockState(new BlockMeta(replaceBlock, replaceMeta));
                        }
                    }
                }

                blockArray[pos.x][pos.y][pos.z] = blockState;
            }

            // MAP OUT CONNECTIONS + PRIORITIES
            if (connections.size() > 0) {
                fromConnections = new ArrayList<>();

                connections.sort((a, b) -> b.selectionPriority - a.selectionPriority); // sort by descending priority, highest first

                // Sort out our from connections, splitting into individual lists for each
                // priority level
                List<JigsawConnection> innerList = null;
                int currentPriority = 0;
                for (JigsawConnection connection : connections) {
                    if (innerList == null || currentPriority != connection.selectionPriority) {
                        innerList = new ArrayList<>();
                        fromConnections.add(innerList);
                        currentPriority = connection.selectionPriority;
                    }

                    innerList.add(connection);
                }
            }

            isLoaded = true;

        } catch (Exception e) {
            Registry.LOG.error("Exception reading NBT Structure format", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // hush
            }
        }
    }

    private HashMap<Short, Short> getWorldItemPalette() {
        if (itemPalette == null)
            return null;

        HashMap<Short, Short> worldItemPalette = new HashMap<>();

        for (ItemPaletteEntry entry : itemPalette) {
            Item item = (Item) Item.itemRegistry.getObject(entry.name);
            worldItemPalette.put(entry.id, (short) Item.getIdFromItem(item));
        }

        return worldItemPalette;
    }

    private TileEntity buildTileEntity(World world, Block block, HashMap<Short, Short> worldItemPalette, NBTTagCompound nbt, int coordBaseMode) {
        nbt = (NBTTagCompound) nbt.copy();

        if (worldItemPalette != null) relinkItems(worldItemPalette, nbt);

        TileEntity te = TileEntity.createAndLoadEntity(nbt);

        if (te instanceof INBTTileEntityTransformable) {
            ((INBTTileEntityTransformable) te).transformTE(world, coordBaseMode);
        }

        return te;
    }

    // What a fucken mess, why even implement the IntArray NBT if ye aint gonna use
    // it Moe Yang?
    private BlockPos parsePos(NBTTagList pos) {
        NBTBase xb = (NBTBase) pos.tagList.get(0);
        int x = ((NBTTagInt) xb).func_150287_d();
        NBTBase yb = (NBTBase) pos.tagList.get(1);
        int y = ((NBTTagInt) yb).func_150287_d();
        NBTBase zb = (NBTBase) pos.tagList.get(2);
        int z = ((NBTTagInt) zb).func_150287_d();

        return new BlockPos(x, y, z);
    }

    // NON-STANDARD, items are serialized with IDs, which will differ from world to world!
    // So our fixed exporter adds an itemPalette, please don't hunt me down for fucking with the spec
    private void relinkItems(HashMap<Short, Short> palette, NBTTagCompound nbt) {
        NBTTagList items = null;
        if (nbt.hasKey("items")) items = nbt.getTagList("items", NBT.TAG_COMPOUND);
        if (nbt.hasKey("Items")) items = nbt.getTagList("Items", NBT.TAG_COMPOUND);

        if (items == null) return;

        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound item = items.getCompoundTagAt(i);
            item.setShort("id", palette.get(item.getShort("id")));
        }
    }

    private Block transformBlock(BlockMeta definition, Map<Block, BlockSelector> blockTable, Random rand) {
        if (blockTable != null && blockTable.containsKey(definition.block)) {
            final BlockSelector selector = blockTable.get(definition.block);
            selector.selectBlocks(rand, 0, 0, 0, false); // fuck the vanilla shit idc
            return selector.func_151561_a();
        }

        if (definition.block instanceof INBTBlockTransformable)
            return ((INBTBlockTransformable) definition.block).transformBlock(definition.block);

        return definition.block;
    }

    private int transformMeta(BlockMeta definition, Map<Block, BlockSelector> blockTable, int coordBaseMode) {
        if (blockTable != null && blockTable.containsKey(definition.block))
            return blockTable.get(definition.block).getSelectedBlockMetaData();

        // Our shit
        if (definition.block instanceof INBTBlockTransformable)
            return ((INBTBlockTransformable) definition.block).transformMeta(definition.meta, coordBaseMode);

        if (coordBaseMode == 0)
            return definition.meta;

        // Vanilla shit
        if (definition.block instanceof BlockStairs) return INBTBlockTransformable.transformMetaStairs(definition.meta, coordBaseMode);
        if (definition.block instanceof BlockRotatedPillar) return INBTBlockTransformable.transformMetaPillar(definition.meta, coordBaseMode);
        if (definition.block instanceof BlockDirectional) return INBTBlockTransformable.transformMetaDirectional(definition.meta, coordBaseMode);
        if (definition.block instanceof BlockTorch) return INBTBlockTransformable.transformMetaTorch(definition.meta, coordBaseMode);
        if (definition.block instanceof BlockButton) return INBTBlockTransformable.transformMetaTorch(definition.meta, coordBaseMode);
        if (definition.block instanceof BlockDoor) return INBTBlockTransformable.transformMetaDoor(definition.meta, coordBaseMode);
        if (definition.block instanceof BlockLever) return INBTBlockTransformable.transformMetaLever(definition.meta, coordBaseMode);
        if (definition.block instanceof BlockSign) return INBTBlockTransformable.transformMetaSignLadder(definition.meta, coordBaseMode);
        if (definition.block instanceof BlockLadder) return INBTBlockTransformable.transformMetaSignLadder(definition.meta, coordBaseMode);
        if (definition.block instanceof BlockTripWireHook) return INBTBlockTransformable.transformMetaDirectional(definition.meta, coordBaseMode);
        if (definition.block instanceof BlockVine) return INBTBlockTransformable.transformMetaVine(definition.meta, coordBaseMode);
		if (definition.block instanceof BlockTrapDoor) return INBTBlockTransformable.transformMetaTrapdoor(definition.meta, coordBaseMode);

        return definition.meta;
    }

    protected int rotateX(int x, int z, int coordBaseMode) {
        switch (coordBaseMode) {
        case 1: return size.z - 1 - z;
        case 2: return size.x - 1 - x;
        case 3: return z;
        default: return x;
        }
    }

    protected int rotateZ(int x, int z, int coordBaseMode) {
        switch (coordBaseMode) {
        case 1: return x;
        case 2: return size.z - 1 - z;
        case 3: return size.x - 1 - x;
        default: return z;
        }
    }

    protected int unrotateX(int x, int z, int coordBaseMode) {
        switch (coordBaseMode) {
        case 3: return size.x - 1 - z;
        case 2: return size.x - 1 - x;
        case 1: return z;
        default: return x;
        }
    }

    protected int unrotateZ(int x, int z, int coordBaseMode) {
        switch (coordBaseMode) {
        case 3: return x;
        case 2: return size.z - 1 - z;
        case 1: return size.z - 1 - x;
        default: return z;
        }
    }

    private static class BlockState {

        final BlockMeta definition;
        NBTTagCompound nbt;

        BlockState(BlockMeta definition) {
            this.definition = definition;
        }

    }

    private static class ItemPaletteEntry {

        final short id;
        final String name;

        ItemPaletteEntry(short id, String name) {
            this.id = id;
            this.name = name;
        }

    }

    // Each jigsaw block in a structure will instance one of these
    protected static class JigsawConnection {

        protected final BlockPos pos;
        protected final ForgeDirection dir;

        // what pool should we look through to find a connection
        protected final String poolName;

        // when we successfully find a pool, what connections in that jigsaw piece can we target
        protected final String targetName;

        protected final boolean isRollable;

        protected final int selectionPriority;
        protected final int placementPriority;

        private JigsawConnection(BlockPos pos, ForgeDirection dir, String poolName, String targetName, boolean isRollable, int selectionPriority, int placementPriority) {
            this.pos = pos;
            this.dir = dir;
            this.poolName = poolName;
            this.targetName = targetName;
            this.isRollable = isRollable;
            this.selectionPriority = selectionPriority;
            this.placementPriority = placementPriority;
        }

    }

}
