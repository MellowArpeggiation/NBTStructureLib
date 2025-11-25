package net.mellow.nbtlib.pack;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.compress.utils.IOUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.api.NBTStructure;
import net.minecraft.world.biome.BiomeGenBase;

public abstract class AbstractStructurePack {

    public abstract String getPackName();
    public abstract List<StructurePair> loadBasicStructures();
    public abstract List<StructureExtension> loadExtensionStructures();

    public static class StructurePair {

        public final NBTStructure structure;
        public final StructureMeta meta;

        public StructurePair(NBTStructure structure, StructureMeta meta) {
            this.structure = structure;
            this.meta = meta;
        }

    }

    public static class StructureMeta {

        public Predicate<BiomeGenBase> canSpawn;
        public int weight;

        public int heightOffset;

        private StructureMeta() {
            canSpawn = biome -> biome.rootHeight > 0.0F;
            weight = 0;
            heightOffset = -1;
        }

        public static StructureMeta getDefault() {
            return new StructureMeta();
        }

        public static StructureMeta load(InputStream stream) {
            StructureMeta meta = new StructureMeta();

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(stream));
                JsonObject json = (new JsonParser().parse(reader)).getAsJsonObject();

                meta.loadFromJson(json);
            } catch (Exception ex) {
                // TODO: which file failed?
                Registry.LOG.error("[StructurePack] failed to load .mcmeta for a structure");
            } finally {
                IOUtils.closeQuietly(reader);
            }

            return meta;
        }

        private void loadFromJson(JsonObject json) {
            if (json.has("spawnWeight"))
                weight = json.get("spawnWeight").getAsInt();

            if (json.has("heightOffset"))
                heightOffset = json.get("heightOffset").getAsInt();
        }

    }

    public static class StructureExtension {

        public final String targetModId;
        public final String targetSpawnCondition;
        public final String targetPool;
        public final StructurePair pair;

        public StructureExtension(String modId, String spawnCondition, String pool, StructurePair pair) {
            this.targetModId = modId;
            this.targetSpawnCondition = spawnCondition;
            this.targetPool = pool;
            this.pair = pair;
        }

    }

}
