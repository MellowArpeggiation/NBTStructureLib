package net.mellow.nbtlib.pack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
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
    public List<StructureBasic> loadBasicStructures() {
        List<StructureBasic> structures = new ArrayList<>();
        if (zipFile == null) return structures;

        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry entry = enumeration.nextElement();

            // Grab all .nbt files at the ROOT of the zip
            String name = entry.getName();
            if (name.endsWith(".nbt") && !name.contains("/")) {
                try {
                    NBTStructure structure = new NBTStructure(name, zipFile.getInputStream(entry));
                    StructurePair pair = new StructurePair(structure, getJigsawMeta(name));
                    structures.add(new StructureBasic(pair, getSpawnMeta(name)));
                } catch (IOException ex) {
                    // TODO
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

                    structures.add(new StructureExtension(segments[0], segments[1], segments[2], new StructurePair(structure, getJigsawMeta(name))));
                } catch (IOException ex) {
                    // TODO
                }
            }
        }

        return structures;
    }

    @Override
    public List<StructureJigsaw> loadJigsawStructures() {
        List<StructureJigsaw> structures = new ArrayList<>();
        if (zipFile == null) return structures;

        HashMap<String, StructureJigsaw> structureMap = new HashMap<>();

        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry entry = enumeration.nextElement();

            // Grab structure jigsaw .nbt files
            String name = entry.getName();
            if (name.endsWith(".nbt")) {
                try {
                    String[] segments = name.split("/");
                    if (segments.length != 3) continue;

                    StructureJigsaw jigsaw = structureMap.get(segments[0]);
                    if (jigsaw == null) {
                        jigsaw = new StructureJigsaw(segments[0], getSpawnMeta(segments[0]));

                        structures.add(jigsaw);
                        structureMap.put(segments[0], jigsaw);
                    }

                    NBTStructure structure = new NBTStructure(segments[2], zipFile.getInputStream(entry));
                    StructurePair pair = new StructurePair(structure, getJigsawMeta(name));

                    jigsaw.add(segments[1], pair);
                } catch (IOException ex) {
                    // TODO
                }
            }
        }

        return structures;
    }

    private JigsawPieceMeta getJigsawMeta(String name) throws IOException {
        ZipEntry entry = zipFile.getEntry(name + ".mcmeta");

        if (entry == null) return JigsawPieceMeta.getDefault();

        return JigsawPieceMeta.load(zipFile.getInputStream(entry));
    }

    private SpawnConditionMeta getSpawnMeta(String name) throws IOException {
        ZipEntry entry = zipFile.getEntry(name + ".mcmeta");

        if (entry == null) return SpawnConditionMeta.getDefault();

        return SpawnConditionMeta.load(zipFile.getInputStream(entry));
    }

    @Override
    public void close() throws IOException {
        zipFile.close();
    }

}
