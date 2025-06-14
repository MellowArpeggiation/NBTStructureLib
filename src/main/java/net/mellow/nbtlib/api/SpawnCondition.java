package net.mellow.nbtlib.api;

import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import net.mellow.nbtlib.Config;
import net.mellow.nbtlib.api.JigsawPiece.WeightedJigsawPiece;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public class SpawnCondition {

    // If defined, will spawn a single jigsaw piece, for single nbt structures
    public JigsawPiece structure;

    // If defined, will override regular spawn location checking, for placing at specific coordinates or with special rules
    public Predicate<WorldCoordinate> checkCoordinates;

    // Determine whether the current biome is valid for spawning
    public Predicate<BiomeGenBase> canSpawn;
    public int spawnWeight = 1;

    // Named jigsaw pools that are referenced within the structure
    public Map<String, JigsawPool> pools;
    public String startPool;

    // Maximum amount of components in this structure
    public int sizeLimit = 8;

    // How far the structure can extend horizontally from the center, maximum of 128
    // This could be increased by changing GenStructure:range from 8, but this is
    // already quite reasonably large
    public int rangeLimit = 128;

    // Height modifiers, will clamp height that the start generates at, allowing for:
    // * Submarines that must spawn under the ocean surface
    // * Bunkers that sit underneath the ground
    public int minHeight = 1;
    public int maxHeight = 128;

    // Can this spawn in the current biome
    protected boolean isValid(BiomeGenBase biome) {
        if (canSpawn == null) return true;
        return canSpawn.test(biome);
    }

    protected JigsawPool getPool(String name) {
        return pools.get(name).clone();
    }

    // Builds all of the pools into neat rows and columns, for editing and debugging!
    // Make sure structure debug is enabled, or it will no-op
    // Do not use in generation
    public void buildAll(World world, int x, int y, int z) {
        if (!Config.debugStructures) return;

        int padding = 5;
        int oz = 0;

        for (JigsawPool pool : pools.values()) {
            int highestWidth = 0;
            int ox = 0;

            for (WeightedJigsawPiece weighted : pool.pieces) {
                NBTStructure structure = weighted.piece.structure;
                structure.build(world, x + ox + (structure.size.x / 2), y, z + oz + (structure.size.z / 2));

                ox += structure.size.x + padding;
                highestWidth = Math.max(highestWidth, structure.size.z);
            }

            oz += highestWidth + padding;
        }
    }

    /**
     * Provides information about the currently structure gen chunk,
     * use the included random for consistent seeding!
     */
    public static class WorldCoordinate {

        public final World world;
        public final ChunkCoordIntPair coords;
        public final Random rand;

        protected WorldCoordinate(World world, ChunkCoordIntPair coords, Random rand) {
            this.world = world;
            this.coords = coords;
            this.rand = rand;
        }

    }

}
