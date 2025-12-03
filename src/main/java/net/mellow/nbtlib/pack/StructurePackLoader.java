package net.mellow.nbtlib.pack;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;

import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.api.JigsawPiece;
import net.mellow.nbtlib.api.JigsawPool;
import net.mellow.nbtlib.api.NBTGeneration;
import net.mellow.nbtlib.api.SpawnCondition;
import net.mellow.nbtlib.pack.AbstractStructurePack.StructureBasic;
import net.mellow.nbtlib.pack.AbstractStructurePack.StructureExtension;
import net.mellow.nbtlib.pack.AbstractStructurePack.StructureJigsaw;
import net.mellow.nbtlib.pack.AbstractStructurePack.StructurePairPool;

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
            for (StructureBasic basic : pack.loadBasicStructures()) {
                if (basic.pair.pieceMeta.weight <= 0) basic.pair.pieceMeta.weight = 1;

                int[] dimensions = {0};
                if (basic.spawnMeta.validDimensions != null) {
                    dimensions = basic.spawnMeta.validDimensions.stream().mapToInt(i -> (int)i).toArray();
                }

                NBTGeneration.registerStructure(dimensions, new SpawnCondition(pack.getPackName(), basic.pair.structure.getName()) {{
                    structure = new JigsawPiece(pack.getPackName() + ":" + basic.pair.structure.getName(), basic.pair.structure, basic.pair.pieceMeta.heightOffset) {{
                        alignToTerrain = basic.pair.pieceMeta.alignToTerrain;
                        conformToTerrain = basic.pair.pieceMeta.conformToTerrain;
                    }};
                    canSpawn = basic.spawnMeta::canSpawn;
                    spawnWeight = basic.pair.pieceMeta.weight;
                    minHeight = basic.spawnMeta.minHeight;
                    maxHeight = basic.spawnMeta.maxHeight;
                }});
            }

            // Iterates through folders looking for fully formed jigsaw structures!
            // Pack your structurepacks like so:
            //     spawncondition/pool/structure.nbt
            // Place an .mcmeta at the root to change the default spawn conditions
            //     spawncondition.mcmeta
            for (StructureJigsaw jigsaw : pack.loadJigsawStructures()) {
                if (jigsaw.spawnMeta.weight <= 0) jigsaw.spawnMeta.weight = 1;

                int[] dimensions = {0};
                if (jigsaw.spawnMeta.validDimensions != null) {
                    dimensions = jigsaw.spawnMeta.validDimensions.stream().mapToInt(i -> (int)i).toArray();
                }

                HashMap<String, JigsawPool> jigsawPools = new HashMap<>();

                for (StructurePairPool piece : jigsaw.pieces) {
                    if (piece.pieceMeta.weight <= 0) piece.pieceMeta.weight = 1;

                    JigsawPool pool = jigsawPools.computeIfAbsent(piece.pool, p -> new JigsawPool());

                    pool.add(new JigsawPiece(pack.getPackName() + ":" + piece.pool + ":" + piece.structure.getName(), piece.structure, piece.pieceMeta.heightOffset) {{
                        alignToTerrain = piece.pieceMeta.alignToTerrain;
                        conformToTerrain = piece.pieceMeta.conformToTerrain;
                    }}, piece.pieceMeta.weight);
                }

                NBTGeneration.registerStructure(dimensions, new SpawnCondition(pack.getPackName(), jigsaw.name) {{
                    startPool = jigsaw.spawnMeta.startPool;
                    pools = jigsawPools;
                    canSpawn = jigsaw.spawnMeta::canSpawn;
                    spawnWeight = jigsaw.spawnMeta.weight;
                    minHeight = jigsaw.spawnMeta.minHeight;
                    maxHeight = jigsaw.spawnMeta.maxHeight;
                }});
            }

            IOUtils.closeQuietly(pack);
        }

        // Iterate on additional structure pieces separately, so packs can add to packs!
        for (File file : structurePackDir.listFiles(structurePackFilter)) {
            AbstractStructurePack pack = file.isDirectory() ? new FolderStructurePack(file) : new FileStructurePack(file);

            // Iterates through folders, looking for .nbt files to add to existing mod structure pools!
            // Pack your structurepacks like so:
            //     modid/spawncondition/pool/structure.nbt
            // modid for structurepack structures is the file/folder name NOT including .zip
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
                if (extension.pair.pieceMeta.weight <= 0) extension.pair.pieceMeta.weight = pool.getAverageWeight();

                pool.add(new JigsawPiece(pack.getPackName() + ":" + extension.pair.structure.getName(), extension.pair.structure, extension.pair.pieceMeta.heightOffset) {{
                    conformToTerrain = extension.pair.pieceMeta.conformToTerrain;
                }}, extension.pair.pieceMeta.weight);
            }

            IOUtils.closeQuietly(pack);
        }
    }

}
