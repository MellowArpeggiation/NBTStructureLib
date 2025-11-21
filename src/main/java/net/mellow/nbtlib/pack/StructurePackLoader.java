package net.mellow.nbtlib.pack;

import java.io.File;
import java.io.FileFilter;

import net.mellow.nbtlib.api.JigsawPiece;
import net.mellow.nbtlib.api.NBTGeneration;
import net.mellow.nbtlib.api.NBTStructure;
import net.mellow.nbtlib.api.SpawnCondition;
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
            for (NBTStructure basicStructure : pack.loadBasicStructures()) {
                NBTGeneration.registerStructure(0, new SpawnCondition(pack.getPackName(), basicStructure.getName()) {{
                    structure = new JigsawPiece(pack.getPackName() + ":" + basicStructure.getName(), basicStructure, -1);
                    canSpawn = biome -> biome.rootHeight > 0.0F;
                }});
            }
        }
    }

}
