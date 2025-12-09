package net.mellow.nbtlib.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
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

        public final ForgeDirection dir;

        public final StructureBoundingBox boundingBox;

        public boolean hasBuilt;

        private Tandem(String spawnName, JigsawPiece piece, int x, int y, int z, ForgeDirection dir) {
            this.spawnName = spawnName;
            this.piece = piece;

            this.dir = dir;

            this.boundingBox = new StructureBoundingBox(x, y, z, x + piece.structure.size.x - 1, y + piece.structure.size.y - 1, z + piece.structure.size.z - 1);
        }

        public static Tandem selectPiece(String spawnName, String poolName, String targetName, Random rand, int x, int y, int z, ForgeDirection dir) {
            SpawnCondition spawn = NBTGeneration.getStructure(spawnName);
            if (spawn == null || spawn.pools == null) return null;

            JigsawPool pool = spawn.pools.get(poolName);
            if (pool == null) return null;

            return new Tandem(spawnName, pool.get(rand), x, y, z, dir);
        }

        public void build(World world, Random rand) {
            if (hasBuilt) return;

            System.out.println("[Tandem] building: " + piece.name + " at " + boundingBox.toString());

            // `flag: 2`: gotta send client updates for tandems, they occur after the chunk has already been sent to the client
            piece.structure.build(world, rand, spawnName, piece, boundingBox, boundingBox, 0, 2);

            hasBuilt = true;
        }

    }

    public static class QueueHandler {

        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.world.isRemote) return;

            NBTQueue queue = dimensionMap.get(event.world.provider.dimensionId);

            Random rand = new Random(event.world.getSeed());

            // we mark the piece as built and remove them later, because pieces can add MORE pieces,
            // causing a ConcurrentModificationException

            // Also we iterate via count because there is no reason to process new pieces immediately,
            // and this can also cause a CME

            int count = queue.tandemQueue.size();
            for (int i = 0; i < count; i++) {
                queue.tandemQueue.get(i).build(event.world, rand);
            }

            queue.tandemQueue.removeIf(tandem -> tandem.hasBuilt);
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
