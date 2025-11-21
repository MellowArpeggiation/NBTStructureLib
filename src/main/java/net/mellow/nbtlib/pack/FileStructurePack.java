package net.mellow.nbtlib.pack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
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
        if (zipFile == null) return structures;

        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry entry = enumeration.nextElement();

            // Grab all .nbt files at the ROOT of the zip
            String name = entry.getName();
            if (name.endsWith(".nbt") && !name.contains("/")) {
                try {
                    structures.add(new NBTStructure(name, zipFile.getInputStream(entry)));
                } catch (IOException ex) {
                    // also squelch, I should handle these properly soon
                }
            }
        }

        return structures;
    }

    @Override
    public List<StructureExtension> loadExtensionStructures() {
        List<StructureExtension> structures = new ArrayList<>();
        if (zipFile == null) return structures;

        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry entry = enumeration.nextElement();

            // Grab structure extension .nbt files
            String name = entry.getName();
            if (name.endsWith(".nbt")) {
                try {
                    String[] segments = name.split("/");
                    if (segments.length != 4) continue;

                    NBTStructure structure = new NBTStructure(segments[3], zipFile.getInputStream(entry));
                    structures.add(new StructureExtension(segments[0], segments[1], segments[2], structure));
                } catch (IOException ex) {
                    // awawa
                }
            }
        }

        return structures;
    }

}
