package net.mellow.nbtlib;

import java.util.Random;

import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.mellow.nbtlib.api.NBTStructure;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.event.terraingen.InitMapGenEvent.EventType;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.TerrainGen;
import net.minecraftforge.event.world.WorldEvent;

public class NBTWorldGenerator implements IWorldGenerator {

    private NBTStructure.GenStructure nbtGen = new NBTStructure.GenStructure();

    private final Random rand = new Random(); //A central random, used to cleanly generate our stuff without affecting vanilla or modded seeds.
    private boolean hasPopulationEvent = false; // Does the given chunkGenerator have a population event? If not (flatlands), default to using generate.

    /** Inits all MapGen upon the loading of a new world. Hopefully clears out structureMaps and structureData when a different world is loaded. */
    @SubscribeEvent
    public void onLoad(WorldEvent.Load event) {
        nbtGen = (NBTStructure.GenStructure) TerrainGen.getModdedMapGen(new NBTStructure.GenStructure(), EventType.CUSTOM);

        hasPopulationEvent = false;
    }

    /** Called upon the initial population of a chunk. Called in the pre-population event first; called again if pre-population didn't occur (flatland) */
    private void setRandomSeed(World world, int chunkX, int chunkZ) {
        rand.setSeed(world.getSeed() + world.provider.dimensionId);
        final long i = rand.nextLong() / 2L * 2L + 1L;
        final long j = rand.nextLong() / 2L * 2L + 1L;
        rand.setSeed((long)chunkX * i + (long)chunkZ * j ^ world.getSeed());
    }

    /*
     * Pre-population Events / Structure Generation
     * Used to generate structures without unnecessary intrusion by biome decoration, like trees.
     */
    @SubscribeEvent
    public void generateStructures(PopulateChunkEvent.Pre event) {
        hasPopulationEvent = true;

        if (!event.world.getWorldInfo().isMapFeaturesEnabled()) return;

        setRandomSeed(event.world, event.chunkX, event.chunkZ); //Set random for population down the line.

        nbtGen.generateStructures(event.world, rand, event.chunkProvider, event.chunkX, event.chunkZ);
    }


    /*
     * Post-Vanilla / Modded Generation
     * Used to generate features that don't care about intrusions (ores, craters, caves, etc.)
     */
    @Override
    public void generate(Random unusedRandom, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        if(hasPopulationEvent) return; //If we've failed to generate any structures (flatlands)

        if (!world.getWorldInfo().isMapFeaturesEnabled()) return;

        setRandomSeed(world, chunkX, chunkZ); //Reset the random seed to compensate

        nbtGen.generateStructures(world, rand, chunkProvider, chunkX, chunkZ);
    }

}
