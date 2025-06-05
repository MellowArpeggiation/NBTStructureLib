package net.mellow.nbtlib;

import java.util.HashMap;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.mellow.nbtlib.api.NBTStructure;
import net.mellow.nbtlib.api.NBTStructure.JigsawPiece;
import net.mellow.nbtlib.api.NBTStructure.JigsawPool;
import net.mellow.nbtlib.api.NBTStructure.SpawnCondition;
import net.mellow.nbtlib.block.ModBlocks;
import net.mellow.nbtlib.gui.GuiHandler;
import net.mellow.nbtlib.item.ModItems;
import net.mellow.nbtlib.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        Registry.LOG.info("Loading NBTStructureLib - version " + Tags.VERSION);

        ModBlocks.register();
        ModItems.register();
    }

    public void init(FMLInitializationEvent event) {
        NetworkHandler.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(Registry.instance, new GuiHandler());

        NBTWorldGenerator worldGenerator = new NBTWorldGenerator();
        GameRegistry.registerWorldGenerator(worldGenerator, 1);
        MinecraftForge.EVENT_BUS.register(worldGenerator);

        NBTStructure.register();

        if (Config.spawnTestStructure) {
            NBTStructure.registerStructure(0, new SpawnCondition() {{
                startPool = "start";
                pools = new HashMap<String, JigsawPool>() {{
                    put("start", new JigsawPool() {{
                        add(new JigsawPiece("example_structure_core", StructureManager.test_jigsaw_core), 1);
                    }});
                    put("default", new JigsawPool() {{
                        add(new JigsawPiece("example_structure_junction", StructureManager.test_jigsaw_junction), 1);
                        add(new JigsawPiece("example_structure_hall", StructureManager.test_jigsaw_hall), 1);
                    }});
                }};
            }});
        }
    }

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {}

}
