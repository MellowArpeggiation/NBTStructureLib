package net.mellow.nbtlib.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.mellow.nbtlib.Config;
import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.api.format.IStructureProvider.JigsawConnection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

public class NBTQueue {

    private static HashMap<Integer, NBTQueue> dimensionMap = new HashMap<>();

    public static void init() {
        QueueHandler queueHandler = new QueueHandler();
        MinecraftForge.EVENT_BUS.register(queueHandler);
        FMLCommonHandler.instance().bus().register(queueHandler);
    }

    public static void queueStructurePiece(String spawnName, String poolName, String targetName, World world, Random rand, int x, int y, int z, ForgeDirection dir) {
        NBTQueue queue = dimensionMap.get(world.provider.dimensionId);

        Tandem tandem = Tandem.selectPiece(spawnName, poolName, targetName, rand, x, y, z, dir);
        if (tandem == null) return;

        queue.tandemQueue.add(tandem);
    }


    private List<Tandem> tandemQueue = new ArrayList<>();

    private static class Tandem {

        public final String spawnName;
        public final JigsawPiece piece;

        public final int coordBaseMode;

        public final StructureBoundingBox boundingBox;

        public final long seed;

        public boolean hasBuilt;

        private Tandem(String spawnName, JigsawPiece piece, int x, int y, int z, int coordBaseMode, long seed) {
            this.spawnName = spawnName;
            this.piece = piece;

            this.coordBaseMode = coordBaseMode;

            this.seed = seed;

            switch (coordBaseMode) {
            case 1:
            case 3:
                this.boundingBox = new StructureBoundingBox(x, y, z, x + piece.structure.size.z - 1, y + piece.structure.size.y - 1, z + piece.structure.size.x - 1);
                break;
            default:
                this.boundingBox = new StructureBoundingBox(x, y, z, x + piece.structure.size.x - 1, y + piece.structure.size.y - 1, z + piece.structure.size.z - 1);
                break;
            }
        }

        private Tandem(String spawnName, String jigsawName, int x1, int y1, int z1, int x2, int y2, int z2, int coordBaseMode, long seed) {
            this.spawnName = spawnName;
            this.piece = JigsawPiece.jigsawMap.get(jigsawName);

            this.coordBaseMode = coordBaseMode;

            this.boundingBox = new StructureBoundingBox(x1, y1, z1, x2, y2, z2);

            this.seed = seed;
        }

        public static Tandem selectPiece(String spawnName, String poolName, String targetName, Random rand, int x, int y, int z, ForgeDirection dir) {
            SpawnCondition spawn = NBTGeneration.getStructure(spawnName);
            if (spawn == null || spawn.pools == null) return null;

            JigsawPool pool = spawn.pools.get(poolName);
            if (pool == null) return null;

            JigsawPiece nextPiece = pool.get(rand);

            List<JigsawConnection> connectionPool = nextPiece.structure.getConnectionPool(dir, targetName);
            if (connectionPool == null) return null;

            JigsawConnection toConnection = connectionPool.get(rand.nextInt(connectionPool.size()));
            int nextCoordBase = directionOffsetToCoordBase(dir.getOpposite(), toConnection.dir);

            // offset the starting point to the connecting point
            int ox = nextPiece.structure.rotateX(toConnection.pos.x, toConnection.pos.z, nextCoordBase);
            int oy = toConnection.pos.y;
            int oz = nextPiece.structure.rotateZ(toConnection.pos.x, toConnection.pos.z, nextCoordBase);

            return new Tandem(spawnName, nextPiece, x - ox + dir.offsetX, y - oy + dir.offsetY, z - oz + dir.offsetZ, nextCoordBase, rand.nextLong());
        }

        private static int directionOffsetToCoordBase(ForgeDirection from, ForgeDirection to) {
            for (int i = 0; i < 4; i++) {
                if (from == to) return i % 4;
                from = from.getRotation(ForgeDirection.DOWN);
            }
            return 0;
        }

        public void attemptBuild(World world) {
            if (hasBuilt) return;

            if (!world.checkChunksExist(boundingBox.minX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ)) return;

            Random rand = new Random(seed);

            if (Config.debugSpawning) {
                Registry.LOG.info("[Tandem] building: " + piece.name + " at " + boundingBox.toString());
            }

            // `flag: 2`: gotta send client updates for tandems, they occur after the chunk has already been sent to the client
            piece.structure.build(world, rand, spawnName, piece, boundingBox, boundingBox, coordBaseMode, 2);

            hasBuilt = true;
        }

        // should this tandem be saved into this chunk
        public boolean isStoredInChunk(Chunk chunk) {
            return boundingBox.minX >> 4 == chunk.xPosition && boundingBox.minZ >> 4 == chunk.zPosition;
        }

        public void writeToNBT(NBTTagCompound nbt) {
            nbt.setString("spawn", spawnName);
            nbt.setString("jigsaw", piece.name);
            nbt.setInteger("x1", boundingBox.minX);
            nbt.setInteger("y1", boundingBox.minY);
            nbt.setInteger("z1", boundingBox.minZ);
            nbt.setInteger("x2", boundingBox.maxX);
            nbt.setInteger("y2", boundingBox.maxY);
            nbt.setInteger("z2", boundingBox.maxZ);
            nbt.setByte("rot", (byte)coordBaseMode);
            nbt.setLong("seed", seed);
        }

        public static Tandem readFromNBT(NBTTagCompound nbt) {
            return new Tandem(
                nbt.getString("spawn"),
                nbt.getString("jigsaw"),
                nbt.getInteger("x1"),
                nbt.getInteger("y1"),
                nbt.getInteger("z1"),
                nbt.getInteger("x2"),
                nbt.getInteger("y2"),
                nbt.getInteger("z2"),
                nbt.getByte("rot"),
                nbt.getLong("seed")
            );
        }

    }

    public static class QueueHandler {

        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.world.isRemote) return;

            NBTQueue queue = dimensionMap.get(event.world.provider.dimensionId);

            // we mark the piece as built and remove them later, because pieces can add MORE pieces,
            // causing a ConcurrentModificationException

            // Also we iterate via count because there is no reason to process new pieces immediately,
            // and this can also cause a CME

            int count = queue.tandemQueue.size();
            for (int i = 0; i < count; i++) {
                queue.tandemQueue.get(i).attemptBuild(event.world);
            }

            queue.tandemQueue.removeIf(tandem -> tandem.hasBuilt);
        }

        @SubscribeEvent
        public void onChunkLoad(ChunkDataEvent.Load event) {
            NBTQueue queue = dimensionMap.get(event.world.provider.dimensionId);

            NBTTagCompound nbt = event.getData();

            if (nbt.hasKey("NBTQueue")) {
                NBTTagList list = nbt.getTagList("NBTQueue", Constants.NBT.TAG_COMPOUND);

                int count = list.tagCount();
                for (int i = 0; i < count; i++) {
                    NBTTagCompound tandemData = list.getCompoundTagAt(i);
                    queue.tandemQueue.add(Tandem.readFromNBT(tandemData));
                }
            }
        }

        @SubscribeEvent
        public void onChunkSave(ChunkDataEvent.Save event) {
            NBTQueue queue = dimensionMap.get(event.world.provider.dimensionId);

            Chunk chunk = event.getChunk();
            NBTTagCompound nbt = event.getData();

            NBTTagList list = new NBTTagList();

            Iterator<Tandem> iter = queue.tandemQueue.iterator();
            while (iter.hasNext()) {
                Tandem tandem = iter.next();
                if (tandem.isStoredInChunk(chunk)) {
                    NBTTagCompound tandemData = new NBTTagCompound();
                    tandem.writeToNBT(tandemData);
                    list.appendTag(tandemData);
                }
            }

            if (list.tagCount() > 0) {
                nbt.setTag("NBTQueue", list);
            }
        }

        @SubscribeEvent
        public void onChunkUnload(ChunkEvent.Unload event) {
            NBTQueue queue = dimensionMap.get(event.world.provider.dimensionId);

            Chunk chunk = event.getChunk();

            Iterator<Tandem> iter = queue.tandemQueue.iterator();
            while (iter.hasNext()) {
                Tandem tandem = iter.next();
                if (tandem.isStoredInChunk(chunk)) {
                    iter.remove();
                }
            }
        }

        @SubscribeEvent
        public void onWorldLoad(WorldEvent.Load event) {
            if (event.world.isRemote) return;
            dimensionMap.put(event.world.provider.dimensionId, new NBTQueue());
        }

        @SubscribeEvent
        public void onWorldUnload(WorldEvent.Unload event) {
            if (event.world.isRemote) return;
            dimensionMap.remove(event.world.provider.dimensionId);
        }

    }

}
