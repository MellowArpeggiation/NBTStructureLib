package net.mellow.nbtlib;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean spawnTestStructure = false;

    public static boolean debugSpawning = false;
    public static boolean debugStructures = false;

    public static boolean registerOnDedicated = false;

    public static int structureMinChunks = 8;
    public static int structureMaxChunks = 24;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        spawnTestStructure = configuration.getBoolean("spawnTestStructure", Configuration.CATEGORY_GENERAL, spawnTestStructure, "Should we spawn example structures in the overworld?");

        debugSpawning = configuration.getBoolean("debugSpawning", Configuration.CATEGORY_GENERAL, debugSpawning, "Should we log where structures have generated?");
        debugStructures = configuration.getBoolean("debugStructures", Configuration.CATEGORY_GENERAL, debugStructures, "Should structures generate exactly as they are in the structure file, without replacements?");

        registerOnDedicated = configuration.getBoolean("registerOnDedicated", Configuration.CATEGORY_GENERAL, registerOnDedicated, "Should we register blocks on dedicated servers? If enabled, all connecting clients must have this mod installed!");

        structureMinChunks = configuration.getInt("structureMinChunks", Configuration.CATEGORY_GENERAL, structureMinChunks, 1, 256, "Minimum distance between generated structures in chunks");
        structureMaxChunks = configuration.getInt("structureMaxChunks", Configuration.CATEGORY_GENERAL, structureMaxChunks, 1, 256, "Maximum distance between generated structures in chunks");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
