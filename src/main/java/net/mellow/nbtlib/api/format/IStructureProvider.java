package net.mellow.nbtlib.api.format;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.mellow.nbtlib.Config;
import net.mellow.nbtlib.api.BlockMeta;
import net.mellow.nbtlib.block.BlockPos;
import net.mellow.nbtlib.block.BlockReplace;
import net.mellow.nbtlib.block.ModBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public interface IStructureProvider {

    /**
     * The file extension this structure file format provides, EXLUCDING the leading dot
     * eg: "nbt"
     */
    public String getFileExtension();

    public void saveArea(OutputStream stream, World world, int x1, int y1, int z1, int x2, int y2, int z2, Set<BlockMeta> exclude);
    public NBTStructureData loadStructure(InputStream stream);


    public static BlockMeta fetchBlockMeta(World world, int x, int y, int z, Set<BlockMeta> exclude) {
        BlockMeta definition = new BlockMeta(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));

        if (exclude.contains(definition)) return null;

        if (definition.block instanceof BlockReplace) {
            definition = new BlockMeta(((BlockReplace) definition.block).exportAs, definition.meta);
        }

        return definition;
    }


    public static class NBTStructureData {

        public BlockPos size;
        public List<ItemPaletteEntry> itemPalette;
        public BlockState[][][] blockArray;

        public List<List<JigsawConnection>> fromConnections;
        public Map<String, List<JigsawConnection>> toTopConnections;
        public Map<String, List<JigsawConnection>> toBottomConnections;
        public Map<String, List<JigsawConnection>> toHorizontalConnections;

        // check the loaded data and perform transformations on structure blocks
        public void processStructureBlocks() {
            List<JigsawConnection> connections = new ArrayList<>();

            for (int x = 0; x < size.x; x++)
            for (int y = 0; y < size.y; y++)
            for (int z = 0; z < size.z; z++) {
                BlockState blockState = blockArray[x][y][z];
                if (blockState == null) continue;

                if (!Config.debugStructures && blockState.definition.block == ModBlocks.structure_block) {
                    blockState = blockArray[x][y][z] = new BlockState(new BlockMeta(Blocks.air, 0));
                }

                if (Config.debugStructures && blockState.definition.block == Blocks.air) {
                    blockState = blockArray[x][y][z] = new BlockState(new BlockMeta(ModBlocks.structure_air, 0));
                }

                if (blockState.nbt != null) {
                    BlockPos pos = new BlockPos(x, y, z);

                    // Load in connection points for jigsaws
                    if (blockState.definition.block == ModBlocks.structure_jigsaw) {
                        if (toTopConnections == null) toTopConnections = new HashMap<>();
                        if (toBottomConnections == null) toBottomConnections = new HashMap<>();
                        if (toHorizontalConnections == null) toHorizontalConnections = new HashMap<>();

                        int selectionPriority = blockState.nbt.getInteger("selection");
                        int placementPriority = blockState.nbt.getInteger("placement");
                        ForgeDirection direction = ForgeDirection.getOrientation(blockState.nbt.getInteger("direction"));
                        String poolName = blockState.nbt.getString("pool");
                        String ourName = blockState.nbt.getString("name");
                        String targetName = blockState.nbt.getString("target");
                        String replaceBlock = blockState.nbt.getString("block");
                        int replaceMeta = blockState.nbt.getInteger("meta");
                        boolean isRollable = blockState.nbt.getBoolean("roll");

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
                            blockState = blockArray[x][y][z] = new BlockState(new BlockMeta(replaceBlock, replaceMeta));
                        }
                    }
                }
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
        }

    }

    public static class BlockState {

        public final BlockMeta definition;
        public NBTTagCompound nbt;

        public BlockState(BlockMeta definition) {
            this.definition = definition;
        }

    }

    public static class ItemPaletteEntry {

        public final short id;
        public final String name;

        public ItemPaletteEntry(short id, String name) {
            this.id = id;
            this.name = name;
        }

    }

    // Each jigsaw block in a structure will instance one of these
    public static class JigsawConnection {

        public final BlockPos pos;
        public final ForgeDirection dir;

        // what pool should we look through to find a connection
        public final String poolName;

        // when we successfully find a pool, what connections in that jigsaw piece can we target
        public final String targetName;

        public final boolean isRollable;

        public final int selectionPriority;
        public final int placementPriority;

        public JigsawConnection(BlockPos pos, ForgeDirection dir, String poolName, String targetName, boolean isRollable, int selectionPriority, int placementPriority) {
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
