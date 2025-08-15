package net.mellow.nbtlib.api;

import java.util.LinkedHashMap;

import net.mellow.nbtlib.block.FloorPos;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.gen.MapGenBase;

public abstract class MapGenComponent extends MapGenBase {

    public LinkedHashMap<FloorPos, Integer> floorplan;

    /**
     * When generating a chunk at `chunkX` & `chunkZ`, this method will fire for every chunk
     * that is `range` (8) or less chunks away from it, with `offsetX` and `offsetZ` set to each of those pseudo-chunks.
     *
     * The seed for `rand` is set as if it were generating within the pseudo-chunk, such that the contribution of each pseudo-chunk
     * is consistent
     * @param world
     * @param offsetX - the pseudo-chunk we're adding the contribution for the current chunk from
     * @param offsetZ - the pseudo-chunk we're adding the contribution for the current chunk from
     * @param chunkX - the chunk that we are currently generating
     * @param chunkZ - the chunk that we are currently generating
     */
    protected abstract void addChunkContribution(World world, int offsetX, int offsetZ, int chunkX, int chunkZ);

    protected void init(LinkedHashMap<FloorPos, Integer> floorplan) {
        this.floorplan = floorplan;
    }

    // Redirected to deobfuscated `addChunkContribution`
    @Override
    protected void func_151538_a(World world, int offsetX, int offsetZ, int chunkX, int chunkZ, Block[] blocks) {
        addChunkContribution(world, offsetX, offsetZ, chunkX, chunkZ);
    }

}
