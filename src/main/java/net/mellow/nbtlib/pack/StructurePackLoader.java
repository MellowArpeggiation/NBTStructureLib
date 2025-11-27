package net.mellow.nbtlib.pack;

import java.io.File;
import java.io.FileFilter;

import org.apache.commons.io.IOUtils;

import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.api.JigsawPiece;
import net.mellow.nbtlib.api.JigsawPool;
import net.mellow.nbtlib.api.NBTGeneration;
import net.mellow.nbtlib.api.SpawnCondition;
import net.mellow.nbtlib.pack.AbstractStructurePack.StructureExtension;
import net.mellow.nbtlib.pack.AbstractStructurePack.StructurePair;

/**
 * Custom structures added without code!
 */
public class StructurePackLoader {

    protected static final FileFilter structurePackFilter = new FileFilter() {

        public boolean accept(File file) {
            boolean isZip = file.isFile() && file.getName().endsWith(".zip");
            boolean isUnzip = file.isDirectory() && (new File(file, "pack.mcmeta")).isFile();
            return isZip || isUnzip;
        }

    };

    private static File structurePackDir;

    public static void init() {
        structurePackDir = Registry.proxy.getStructurePackDir();
        structurePackDir.mkdir();

        for (File file : structurePackDir.listFiles(structurePackFilter)) {
            AbstractStructurePack pack = file.isDirectory() ? new FolderStructurePack(file) : new FileStructurePack(file);

            // Basic non-jigsaw structures with no specified spawning conditions
            // Grabs every .nbt file at the ROOT of the structurepack!
            for (StructurePair basicPair : pack.loadBasicStructures()) {
                if (basicPair.meta.weight <= 0) basicPair.meta.weight = 1;

                int[] dimensions = {0};
                if (basicPair.meta.validDimensions != null) {
                    dimensions = basicPair.meta.validDimensions.stream().mapToInt(i -> (int)i).toArray();
                }

                NBTGeneration.registerStructure(dimensions, new SpawnCondition(pack.getPackName(), basicPair.structure.getName()) {{
                    structure = new JigsawPiece(pack.getPackName() + ":" + basicPair.structure.getName(), basicPair.structure, basicPair.meta.heightOffset) {{
                        conformToTerrain = basicPair.meta.conformToTerrain;
                    }};
                    canSpawn = basicPair.meta::canSpawn;
                    spawnWeight = basicPair.meta.weight;
                    minHeight = basicPair.meta.minHeight;
                    maxHeight = basicPair.meta.maxHeight;
                }});
            }

            // Iterates through folders, looking for .nbt files to add to existing mod structure pools!
            // Pack your structurepacks like so:
            //     modid/spawncondition/pool/structure.nbt
            for (StructureExtension extension : pack.loadExtensionStructures()) {
                SpawnCondition spawn = NBTGeneration.getStructure(extension.targetModId, extension.targetSpawnCondition);

                if (spawn == null) {
                    Registry.LOG.warn("[StructurePack] structure points to missing SpawnCondition: " + extension.targetModId + ":" + extension.targetSpawnCondition);
                    continue;
                }

                if (spawn.pools == null) {
                    Registry.LOG.warn("[StructurePack] SpawnCondition is not a jigsaw structure: " + extension.targetModId + ":" + extension.targetSpawnCondition);
                    continue;
                }

                JigsawPool pool = spawn.pools.get(extension.targetPool);

                if (pool == null) {
                    pool = new JigsawPool();
                    spawn.pools.put(extension.targetPool, pool);

                    Registry.LOG.info("[StructurePack] New pool created in SpawnCondition: " + extension.targetPool);
                }

                // If no defined weight, make this piece have an average weight
                if (extension.pair.meta.weight <= 0) extension.pair.meta.weight = pool.getAverageWeight();

                pool.add(new JigsawPiece(pack.getPackName() + ":" + extension.pair.structure.getName(), extension.pair.structure, extension.pair.meta.heightOffset) {{
                    conformToTerrain = extension.pair.meta.conformToTerrain;
                }}, extension.pair.meta.weight);
            }

            IOUtils.closeQuietly(pack);
        }
    }

}
