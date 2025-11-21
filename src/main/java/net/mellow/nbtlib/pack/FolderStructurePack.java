package net.mellow.nbtlib.pack;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import net.mellow.nbtlib.api.NBTStructure;

public class FolderStructurePack extends AbstractStructurePack {

    protected static final FileFilter structureFilter = new FileFilter() {

        public boolean accept(File file) {
            return file.isFile() && file.getName().endsWith(".nbt");
        }

    };

    private final File dir;

    public FolderStructurePack(File file) {
        this.dir = file;
    }

    @Override
    public String getPackName() {
        return dir.getName();
    }

    @Override
    public List<NBTStructure> loadBasicStructures() {
        List<NBTStructure> structures = new ArrayList<>();

        for (File file : dir.listFiles(structureFilter)) {
            try {
                structures.add(new NBTStructure(file.getName(), new FileInputStream(file)));
            } catch (FileNotFoundException ex) {
                // squelch, this can't really happen
            }
        }

        return structures;
    }

    @Override
    public List<StructureExtension> loadExtensionStructures() {
        List<StructureExtension> structures = new ArrayList<>();

        for (File modFolder : dir.listFiles()) {
            if (!modFolder.isDirectory()) continue;

            String targetModId = modFolder.getName();

            for (File spawnFolder : modFolder.listFiles()) {
                if (!spawnFolder.isDirectory()) continue;

                String targetSpawnCondition = spawnFolder.getName();

                for (File poolFolder : spawnFolder.listFiles()) {
                    if(!poolFolder.isDirectory()) continue;

                    String targetPool = poolFolder.getName();

                    for (File structureFile : poolFolder.listFiles(structureFilter)) {
                        try {
                            NBTStructure structure = new NBTStructure(structureFile.getName(), new FileInputStream(structureFile));
                            structures.add(new StructureExtension(targetModId, targetSpawnCondition, targetPool, structure));
                        } catch (FileNotFoundException ex) {
                            // not happening
                        }
                    }
                }
            }
        }

        return structures;
    }

}
