package net.mellow.nbtlib.pack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import net.mellow.nbtlib.api.NBTStructure;

public class FileStructurePack extends AbstractStructurePack {

    private final File file;
    private final ZipFile zipFile;

    public FileStructurePack(File file) {
        this.file = file;

        ZipFile zip = null;
        try {
            zip = new ZipFile(file);
        } catch (IOException ex) {
            // Bad zip file, will not load any structures
        }
        this.zipFile = zip;
    }

    @Override
    public String getPackName() {
        String fileName = file.getName();
        return fileName.substring(0, fileName.length() - 4); // trim .zip
    }

    @Override
    public List<NBTStructure> loadBasicStructures() {
        List<NBTStructure> structures = new ArrayList<>();
        return structures;
    }

}
