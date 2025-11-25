package net.mellow.nbtlib.pack;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
    public List<StructurePair> loadBasicStructures() {
        List<StructurePair> structures = new ArrayList<>();

        for (File file : dir.listFiles(structureFilter)) {
            StructurePair pair = loadPair(file);
            if (pair != null) structures.add(pair);
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
                    if (!poolFolder.isDirectory()) continue;

                    String targetPool = poolFolder.getName();

                    for (File structureFile : poolFolder.listFiles(structureFilter)) {
                        StructurePair pair = loadPair(structureFile);
                        if (pair != null) structures.add(new StructureExtension(targetModId, targetSpawnCondition, targetPool, pair));
                    }
                }
            }
        }

        return structures;
    }

    private StructurePair loadPair(File file) {
        try {
            NBTStructure structure = new NBTStructure(file.getName(), new FileInputStream(file));

            FileFilter filter = new FileFilter() {
                public boolean accept(File check) {
                    return check.isFile() && check.getName().equals(file.getName() + ".mcmeta");
                }
            };

            StructureMeta meta = null;
            for (File mcmeta : file.getParentFile().listFiles(filter)) {
                meta = StructureMeta.load(new FileInputStream(mcmeta));
                break;
            }

            if (meta == null) meta = StructureMeta.getDefault();

            return new StructurePair(structure, meta);

        } catch (FileNotFoundException ex) {
            // squelch, for now
        }

        return null;
    }

    @Override
    public void close() throws IOException {

    }

}
