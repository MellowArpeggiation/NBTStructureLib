package net.mellow.nbtlib;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean debugSpawning = false;
    public static boolean debugStructures = false;

    public static int structureMinChunks = 8;
    public static int structureMaxChunks = 24;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        debugStructures = configuration.getBoolean("debugSpawning", Configuration.CATEGORY_GENERAL, debugSpawning, "Should we log where structures have generated?");
        debugStructures = configuration.getBoolean("debugStructures", Configuration.CATEGORY_GENERAL, debugStructures, "Should structures generate without running block replacements?");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
