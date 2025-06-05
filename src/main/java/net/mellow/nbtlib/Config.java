package net.mellow.nbtlib;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean spawnTestStructure = false;

    public static boolean debugSpawning = false;
    public static boolean debugStructures = false;

    public static int structureMinChunks = 8;
    public static int structureMaxChunks = 24;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        spawnTestStructure = configuration.getBoolean("spawnTestStructure", Configuration.CATEGORY_GENERAL, spawnTestStructure, "Should we spawn example structures in the overworld?");

        debugSpawning = configuration.getBoolean("debugSpawning", Configuration.CATEGORY_GENERAL, debugSpawning, "Should we log where structures have generated?");
        debugStructures = configuration.getBoolean("debugStructures", Configuration.CATEGORY_GENERAL, debugStructures, "Should structures generate exactly as they are in the structure file, without replacements?");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
