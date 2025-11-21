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

}
