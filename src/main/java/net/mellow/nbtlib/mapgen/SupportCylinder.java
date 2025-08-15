package net.mellow.nbtlib.mapgen;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import net.mellow.nbtlib.api.SupportComponentBase;
import net.mellow.nbtlib.block.FloorPos;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class SupportCylinder extends SupportComponentBase {

    public Block block = Blocks.stonebrick;

    // private Integer minX, maxX;
    // private Integer minZ, maxZ;
    private int yLevel = 1;

    private int midX, midZ;
    private float radiusSqr;

    @Override
    public void generateSupport(World world, StructureBoundingBox box) {
        for (int x = box.minX; x <= box.maxX; x++) {
            for (int z = box.minZ; z <= box.maxZ; z++) {
                if(distanceSquared(x, z, midX, midZ) < radiusSqr) {
                    for (int y = yLevel; world.getBlock(x, y, z).isReplaceable(world, x, y, z); y--) {
                        world.setBlock(x, y, z, block);
                    }
                }
            }
        }
    }

    @Override
    public void init(LinkedHashMap<FloorPos, Integer> floorplan, StructureBoundingBox structureBox) {
        super.init(floorplan, structureBox);

        for (Entry<FloorPos, Integer> entry : floorplan.entrySet()) {
            // int x = entry.getKey().x;
            // int z = entry.getKey().z;
            // if (minX == null || x < minX) minX = x;
            // if (minZ == null || z < minZ) minZ = z;
            // if (maxX == null || x > maxX) maxX = x;
            // if (maxZ == null || z > maxZ) maxZ = z;

            int y = entry.getValue();
            if (yLevel == 1 || y < yLevel) yLevel = y;
        }

        midX = (structureBox.minX + structureBox.maxX) / 2;
        midZ = (structureBox.minZ + structureBox.maxZ) / 2;

        radiusSqr = distanceSquared(structureBox.minX, structureBox.minZ, midX, midZ);

        // float radiusSqrMinMin = distanceSquared(minX, minZ, midX, midZ);
        // float radiusSqrMinMax = distanceSquared(minX, maxZ, midX, midZ);
        // float radiusSqrMaxMin = distanceSquared(maxX, minZ, midX, midZ);
        // float radiusSqrMaxMax = distanceSquared(maxX, maxZ, midX, midZ);

        // radiusSqr = Math.max(Math.max(Math.max(radiusSqrMinMin, radiusSqrMinMax), radiusSqrMaxMin), radiusSqrMaxMax);
        // radius = (float)Math.sqrt(radiusSqr);
    }

    private float distanceSquared(int x1, int z1, int x2, int z2) {
        float x = x2 - x1;
        float z = z2 - z1;
        return x * x + z * z;
    }

}
