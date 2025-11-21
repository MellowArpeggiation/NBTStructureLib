package net.mellow.nbtlib.pack;

import java.io.File;
import java.io.FileFilter;

import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.api.JigsawPiece;
import net.mellow.nbtlib.api.JigsawPool;
import net.mellow.nbtlib.api.NBTGeneration;
import net.mellow.nbtlib.api.NBTStructure;
import net.mellow.nbtlib.api.SpawnCondition;
import net.mellow.nbtlib.pack.AbstractStructurePack.StructureExtension;
import net.minecraft.client.Minecraft;

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
        structurePackDir = new File(Minecraft.getMinecraft().mcDataDir, "structurepacks");
        structurePackDir.mkdir();

        for (File file : structurePackDir.listFiles(structurePackFilter)) {
            AbstractStructurePack pack = file.isDirectory() ? new FolderStructurePack(file) : new FileStructurePack(file);

            // Basic non-jigsaw structures with no specified spawning conditions
            // Grabs every .nbt file at the ROOT of the structurepack!
            for (NBTStructure basicStructure : pack.loadBasicStructures()) {
                NBTGeneration.registerStructure(0, new SpawnCondition(pack.getPackName(), basicStructure.getName()) {{
                    structure = new JigsawPiece(pack.getPackName() + ":" + basicStructure.getName(), basicStructure, -1);
                    canSpawn = biome -> biome.rootHeight > 0.0F;
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
                    Registry.LOG.warn("[StructurePack] SpawnCondition does not contain pool: " + extension.targetPool);
                    continue;
                }

                // If no defined weight, make this piece have an average weight
                if (extension.weight <= 0) extension.weight = pool.getAverageWeight();

                pool.add(new JigsawPiece(pack.getPackName() + ":" + extension.structure.getName(), extension.structure, -1), extension.weight);
            }
        }
    }

}
