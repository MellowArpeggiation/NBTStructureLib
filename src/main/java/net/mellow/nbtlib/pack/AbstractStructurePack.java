package net.mellow.nbtlib.pack;

import java.util.List;

import net.mellow.nbtlib.api.NBTStructure;

public abstract class AbstractStructurePack {

    public abstract String getPackName();
    public abstract List<NBTStructure> loadBasicStructures();

}
