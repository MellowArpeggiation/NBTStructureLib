package net.mellow.nbtlib.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;

import org.apache.commons.compress.utils.IOUtils;

import net.mellow.nbtlib.Config;
import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.api.format.IStructureProvider;
import net.mellow.nbtlib.api.format.IStructureProvider.BlockState;
import net.mellow.nbtlib.api.format.IStructureProvider.ItemPaletteEntry;
import net.mellow.nbtlib.api.format.IStructureProvider.JigsawConnection;
import net.mellow.nbtlib.api.format.IStructureProvider.NBTStructureData;
import net.mellow.nbtlib.api.format.StructureProviderRegistry;
import net.mellow.nbtlib.api.selector.BiomeBlockSelector;
import net.mellow.nbtlib.block.BlockPos;
import net.mellow.nbtlib.block.BlockJigsawTandem.TileEntityJigsawTandem;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent.BlockSelector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.common.util.Constants.NBT;

/**
 * Handles placing blocks into the world based on an .nbt file (modern MC format)
 *
 * Use this class to load in structures from a `ResourceLocation` like below, make sure not to
 * load from a client only class!
 *
 * public static final NBTStructure DungeonCore = new NBTStructure(new ResourceLocation(Registry.MODID, "structures/dungeon_core.nbt"))
 *
 * Generally you don't want to call the build methods manually, register your structures using `NBTGeneration` instead!
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
            loadStructure(name, stream);
        } else {
            Registry.LOG.error("NBT Structure not found: " + resource.getResourcePath());
        }
    }

    public NBTStructure(String name, InputStream stream) {
        this.name = name;
        loadStructure(name, stream);
    }

    public NBTStructure(File file) throws FileNotFoundException {
        this.name = file.getName();
        InputStream stream = new FileInputStream(file);
        loadStructure(this.name, stream);
        IOUtils.closeQuietly(stream);
    }

    public String getName() {
        return name.substring(0, name.length() - 4); // trim .nbt
    }

    public int getSizeX() {
        return size.x;
    }

    public int getSizeY() {
        return size.y;
    }

    public int getSizeZ() {
        return size.z;
    }

    public List<JigsawConnection> getConnectionPool(ForgeDirection dir, String target) {
        if (dir == ForgeDirection.DOWN) {
            return toTopConnections.get(target);
        } else if (dir == ForgeDirection.UP) {
            return toBottomConnections.get(target);
        }

        return toHorizontalConnections.get(target);
    }

    // Build a piece with a default rotation (NOT cascade safe!)
    public void build(World world, int x, int y, int z) {
        build(world, x, y, z, 0, true, false);
    }

    // Build a piece with a specified rotation (NOT cascade safe!)
    public void build(World world, int x, int y, int z, int coordBaseMode) {
        build(world, x, y, z, coordBaseMode, true, false);
    }

    // Build a piece with a specified rotation (NOT cascade safe!)
    public void build(World world, int x, int y, int z, int coordBaseMode, boolean center, boolean wipeExisting) {
        if (!isLoaded) {
            Registry.LOG.info("NBTStructure is invalid");
            return;
        }

        HashMap<Short, Short> worldItemPalette = getWorldItemPalette();

        if (center) {
            boolean swizzle = coordBaseMode == 1 || coordBaseMode == 3;
            x -= (swizzle ? size.z : size.x) / 2;
            z -= (swizzle ? size.x : size.z) / 2;
        }

        int maxX = size.x;
        int maxZ = size.z;

        for (int bx = 0; bx < maxX; bx++) {
            for (int bz = 0; bz < maxZ; bz++) {
                int rx = rotateX(bx, bz, coordBaseMode) + x;
                int rz = rotateZ(bx, bz, coordBaseMode) + z;

                for (int by = 0; by < size.y; by++) {
                    BlockState state = blockArray[bx][by][bz];
                    if (state == null) {
                        if (wipeExisting) {
                            int ry = by + y;
                            world.removeTileEntity(rx, ry, rz);
                            world.setBlock(rx, ry, rz, Blocks.air, 0, 2);
                        }
                        continue;
                    }

                    int ry = by + y;

                    if (ry < 1) continue;

                    Block block = transformBlock(state.definition, null, world.rand);
                    int meta = transformMeta(state.definition, null, coordBaseMode);

                    TileEntity te = null;
                    if (state.nbt != null) {
                        te = buildTileEntity(world, block, meta, worldItemPalette, state.nbt, coordBaseMode);

                        if (!Config.debugStructures && te instanceof TileEntityJigsawTandem) {
                            TileEntityJigsawTandem tandem = (TileEntityJigsawTandem) te;

                            block = tandem.replaceBlock;
                            meta = tandem.replaceMeta;

                            te = null;
                        } else if (te != null) {
                            block = te.blockType;
                            meta = te.blockMetadata;
                        }
                    }

                    world.removeTileEntity(rx, ry, rz);
                    world.setBlock(rx, ry, rz, block, meta, 2);
                    world.setBlockMetadataWithNotify(rx, ry, rz, meta, 2);
                    world.setTileEntity(rx, ry, rz, te);
                }
            }
        }
    }

    // Builds a structure piece within a given bounds (will not cascade, assuming the bounds provided are valid!)
    public boolean build(World world, Random rand, String structureName, JigsawPiece piece, StructureBoundingBox totalBounds, StructureBoundingBox generatingBounds, int coordBaseMode, int flag) {
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

        if (piece.blockTable != null || piece.platform != null) {
            BiomeGenBase biome = world.getWorldChunkManager().getBiomeGenAt(generatingBounds.getCenterX(), generatingBounds.getCenterZ());

            if (piece.blockTable != null) {
                for (BlockSelector selector : piece.blockTable.values()) {
                    if (selector instanceof BiomeBlockSelector) {
                        ((BiomeBlockSelector) selector).nextBiome = biome;
                    }
                }
            }

            if (piece.platform instanceof BiomeBlockSelector) {
                ((BiomeBlockSelector) piece.platform).nextBiome = biome;
            }
        }

        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                int rx = rotateX(bx, bz, coordBaseMode) + totalBounds.minX;
                int rz = rotateZ(bx, bz, coordBaseMode) + totalBounds.minZ;
                int oy = piece.conformToTerrain ? world.getTopSolidOrLiquidBlock(rx, rz) + piece.heightOffset : totalBounds.minY;

                boolean hasBase = false;

                for (int by = 0; by < size.y; by++) {
                    BlockState state = blockArray[bx][by][bz];
                    if (state == null) continue;

                    int ry = by + oy;

                    if (ry < 1) continue;

                    Block block = transformBlock(state.definition, piece.blockTable, world.rand);
                    int meta = transformMeta(state.definition, piece.blockTable, coordBaseMode);

                    TileEntity te = null;
                    if (state.nbt != null) {
                        te = buildTileEntity(world, block, meta, worldItemPalette, state.nbt, coordBaseMode);

                        if (!Config.debugStructures && te instanceof TileEntityJigsawTandem) {
                            TileEntityJigsawTandem tandem = (TileEntityJigsawTandem) te;
                            int tandemMeta = state.nbt.getInteger("direction");
                            ForgeDirection dir = ForgeDirection.getOrientation(INBTBlockTransformable.transformMetaSignLadder(tandemMeta, coordBaseMode));

                            NBTQueue.queueStructurePiece(structureName, tandem.pool, tandem.target, world, rand, rx, ry, rz, dir);

                            block = tandem.replaceBlock;
                            meta = tandem.replaceMeta;

                            te = null;
                        } else if (te != null) {
                            block = te.blockType;
                            meta = te.blockMetadata;
                        }
                    }

                    world.setBlock(rx, ry, rz, block, meta, flag);
                    world.setBlockMetadataWithNotify(rx, ry, rz, meta, flag); // fucking Mojang bullshit I'll just set it TWICE then
                    world.setTileEntity(rx, ry, rz, te);

                    if (by == 0 && piece.platform != null && !block.getMaterial().isReplaceable()) hasBase = true;
                }

                if (hasBase && !piece.conformToTerrain) {
                    for (int y = oy - 1; y > 0; y--) {
                        if (!world.getBlock(rx, y, rz).isReplaceable(world, rx, y, rz)) break;
                        piece.platform.selectBlocks(world.rand, 0, 0, 0, false);
                        world.setBlock(rx, y, rz, piece.platform.func_151561_a(), piece.platform.getSelectedBlockMetaData(), flag);
                    }
                }
            }
        }

        return true;
    }

    /**
     * Writes out a specified area to a file with a given name (auto-selects format provider by file extension!)
     */
    public static File saveAreaToFile(String filename, World world, int x1, int y1, int z1, int x2, int y2, int z2, Set<BlockMeta> exclude) {
        IStructureProvider provider = StructureProviderRegistry.getFormatFor(filename);

        try {
            File structureDirectory = new File(Minecraft.getMinecraft().mcDataDir, "structures");
            structureDirectory.mkdir();

            File structureFile = new File(structureDirectory, filename);

            int minX = Math.min(x1, x2);
            int minY = Math.min(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxX = Math.max(x1, x2);
            int maxY = Math.max(y1, y2);
            int maxZ = Math.max(z1, z2);

            provider.saveArea(new FileOutputStream(structureFile), world, minX, minY, minZ, maxX, maxY, maxZ, exclude);

            return structureFile;
        } catch (Exception ex) {
            Registry.LOG.warn("Failed to save NBT structure", ex);

            return null;
        }
    }

    private void loadStructure(String filename, InputStream inputStream) {
        IStructureProvider provider = StructureProviderRegistry.getFormatFor(filename);

        NBTStructureData data = provider.loadStructure(inputStream);

        // isLoaded flag determines whether this structure is usable, so just return early on failure
        if (data == null) return;

        data.processStructureBlocks();

        size = data.size;
        itemPalette = data.itemPalette;
        blockArray = data.blockArray;

        fromConnections = data.fromConnections;
        toTopConnections = data.toTopConnections;
        toBottomConnections = data.toBottomConnections;
        toHorizontalConnections = data.toHorizontalConnections;

        isLoaded = true;
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

    private TileEntity buildTileEntity(World world, Block block, int meta, HashMap<Short, Short> worldItemPalette, NBTTagCompound nbt, int coordBaseMode) {
        nbt = (NBTTagCompound) nbt.copy();

        if (worldItemPalette != null) relinkItems(worldItemPalette, nbt);

        TileEntity tile = TileEntity.createAndLoadEntity(nbt);
        if (tile == null) return null;

        tile.blockType = block;
        tile.blockMetadata = meta;

        if (!Config.debugStructures && tile instanceof INBTTileEntityTransformable) {
            TileEntity newTile = ((INBTTileEntityTransformable) tile).transformTE(world, coordBaseMode);
            if (newTile != null && newTile.blockType != null && newTile.blockMetadata >= 0) {
                return newTile;
            }
        }

        return tile;
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
            item.setShort("id", palette.getOrDefault(item.getShort("id"), (short)0));
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

}
