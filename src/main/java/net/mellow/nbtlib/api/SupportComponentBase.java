package net.mellow.nbtlib.api;

import java.util.LinkedHashMap;

import net.mellow.nbtlib.block.FloorPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public abstract class SupportComponentBase {

    protected LinkedHashMap<FloorPos, Integer> floorplan;

    public abstract void generateSupport(World world, StructureBoundingBox box);

    public void init(LinkedHashMap<FloorPos, Integer> floorplan, StructureBoundingBox structureBox) {
        this.floorplan = floorplan;
    }

}
