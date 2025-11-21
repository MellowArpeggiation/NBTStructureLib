package net.mellow.nbtlib.pack;

import java.util.List;

import net.mellow.nbtlib.api.NBTStructure;

public abstract class AbstractStructurePack {

    public abstract String getPackName();
    public abstract List<NBTStructure> loadBasicStructures();
    public abstract List<StructureExtension> loadExtensionStructures();

    public static class StructureExtension {

        public final String targetModId;
        public final String targetSpawnCondition;
        public final String targetPool;
        public final NBTStructure structure;

        public int weight;

        public StructureExtension(String modId, String spawnCondition, String pool, NBTStructure structure) {
            this.targetModId = modId;
            this.targetSpawnCondition = spawnCondition;
            this.targetPool = pool;
            this.structure = structure;
        }

    }

}
