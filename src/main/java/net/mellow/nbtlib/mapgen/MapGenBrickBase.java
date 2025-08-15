package net.mellow.nbtlib.mapgen;

import net.mellow.nbtlib.api.MapGenComponent;
import net.mellow.nbtlib.block.FloorPos;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class MapGenBrickBase extends MapGenComponent {

    public Block block = Blocks.brick_block;

    @Override
    protected void addChunkContribution(World world, int offsetX, int offsetZ, int chunkX, int chunkZ) {
        int xCoord = chunkX << 4;
        int zCoord = chunkZ << 4;

        for(int bx = 0; bx < 16; bx++) {
            for(int bz = 0; bz < 16; bz++) {
                int x = xCoord + bx;
                int z = zCoord + bz;

                Integer sy = floorplan.get(new FloorPos(x, z));

                if(sy == null) continue;

                for (int y = sy; world.getBlock(x, y, z).isReplaceable(world, x, y, z); y--) {
                    world.setBlock(x, y, z, block);
                }
            }
        }
    }

}
