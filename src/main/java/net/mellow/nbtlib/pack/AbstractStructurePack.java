package net.mellow.nbtlib.pack;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.compress.utils.IOUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.api.NBTStructure;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;

public abstract class AbstractStructurePack implements Closeable {

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

        public int weight = 0;

        public int heightOffset = -1;
        public int minHeight = 1;
        public int maxHeight = 128;

        public boolean conformToTerrain = false;

        public List<Integer> validDimensions;
        private Set<BiomeDictionary.Type> validBiomeTypes;
        private List<Predicate<BiomeGenBase>> conditions = new ArrayList<>();

        private StructureMeta() { }

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

            if (json.has("minHeight"))
                minHeight = json.get("minHeight").getAsInt();

            if (json.has("maxHeight"))
                maxHeight = json.get("maxHeight").getAsInt();

            if (json.has("conformToTerrain"))
                conformToTerrain = json.get("conformToTerrain").getAsBoolean();


            if (json.has("canSpawn")) {
                JsonObject canSpawn = json.getAsJsonObject("canSpawn");

                if (canSpawn.has("types")) {
                    JsonArray types = canSpawn.getAsJsonArray("types");

                    for (JsonElement element : types) {
                        BiomeDictionary.Type type = BiomeDictionary.Type.valueOf(element.getAsString());
                        if (type != null) {
                            if (validBiomeTypes == null) validBiomeTypes = new HashSet<>();
                            validBiomeTypes.add(type);
                        }
                    }
                }

                if (canSpawn.has("rootHeight")) {
                    loadConditions(canSpawn.getAsJsonObject("rootHeight"), biome -> biome.rootHeight);
                }

                if (canSpawn.has("heightVariation")) {
                    loadConditions(canSpawn.getAsJsonObject("heightVariation"), biome -> biome.heightVariation);
                }

                if (canSpawn.has("temperature")) {
                    loadConditions(canSpawn.getAsJsonObject("temperature"), biome -> biome.temperature);
                }

                if (canSpawn.has("rainfall")) {
                    loadConditions(canSpawn.getAsJsonObject("rainfall"), biome -> biome.rainfall);
                }
            }


            if (json.has("dimensions")) {
                JsonArray dimensions = json.getAsJsonArray("dimensions");

                for (JsonElement element : dimensions) {
                    if (validDimensions == null) validDimensions = new ArrayList<>();
                    validDimensions.add(element.getAsInt());
                }
            }
        }

        public boolean canSpawn(BiomeGenBase biome) {
            for (Predicate<BiomeGenBase> condition : conditions) {
                if (!condition.test(biome)) return false;
            }

            // If no biome types set, default to any non-underwater biomes
            if (validBiomeTypes == null) {
                return biome.rootHeight > 0.0F;
            }

            for (BiomeDictionary.Type type : BiomeDictionary.getTypesForBiome(biome)) {
                if (validBiomeTypes.contains(type)) return true;
            }

            return false;
        }

        private void loadConditions(JsonObject json, Function<BiomeGenBase, Float> supplier) {
            if (json.has("eq")) {
                float value = json.get("eq").getAsFloat();
                conditions.add(biome -> supplier.apply(biome) == value);
            }

            if (json.has("lt")) {
                float value = json.get("lt").getAsFloat();
                conditions.add(biome -> supplier.apply(biome) < value);
            }

            if (json.has("gt")) {
                float value = json.get("gt").getAsFloat();
                conditions.add(biome -> supplier.apply(biome) > value);
            }

            if (json.has("lte")) {
                float value = json.get("lte").getAsFloat();
                conditions.add(biome -> supplier.apply(biome) <= value);
            }

            if (json.has("gte")) {
                float value = json.get("gte").getAsFloat();
                conditions.add(biome -> supplier.apply(biome) >= value);
            }
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
