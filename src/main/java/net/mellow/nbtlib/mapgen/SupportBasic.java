package net.mellow.nbtlib.mapgen;

import net.mellow.nbtlib.api.SupportComponentBase;
import net.mellow.nbtlib.block.FloorPos;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class SupportBasic extends SupportComponentBase {

    public Block block = Blocks.stonebrick;

    @Override
    public void generateSupport(World world, StructureBoundingBox box) {
        for (int x = box.minX; x <= box.maxX; x++) {
            for (int z = box.minZ; z <= box.maxZ; z++) {
                Integer sy = floorplan.get(new FloorPos(x, z));

                if(sy == null) continue;

                for (int y = sy; world.getBlock(x, y, z).isReplaceable(world, x, y, z); y--) {
                    world.setBlock(x, y, z, block);
                }
            }
        }
    }

}
