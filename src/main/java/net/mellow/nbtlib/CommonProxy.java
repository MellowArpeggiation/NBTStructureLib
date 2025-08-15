package net.mellow.nbtlib;

import java.util.HashMap;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.mellow.nbtlib.api.JigsawPiece;
import net.mellow.nbtlib.api.JigsawPool;
import net.mellow.nbtlib.api.NBTGeneration;
import net.mellow.nbtlib.api.SpawnCondition;
import net.mellow.nbtlib.block.ModBlocks;
import net.mellow.nbtlib.command.CommandLocate;
import net.mellow.nbtlib.gui.GuiHandler;
import net.mellow.nbtlib.item.ModItems;
import net.mellow.nbtlib.mapgen.SupportBasic;
import net.mellow.nbtlib.network.NetworkHandler;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class CommonProxy {

    private NBTWorldGenerator worldGenerator;

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        Registry.LOG.info("Loading NBTStructureLib - version " + Tags.VERSION);

        ModBlocks.register();
        ModItems.register();
    }

    public void init(FMLInitializationEvent event) {
        NetworkHandler.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(Registry.instance, new GuiHandler());

        worldGenerator = new NBTWorldGenerator();
        GameRegistry.registerWorldGenerator(worldGenerator, 1);
        MinecraftForge.EVENT_BUS.register(worldGenerator);

        NBTGeneration.register();

        if (Config.spawnTestStructure) {
            NBTGeneration.registerStructure(0, new SpawnCondition("example_structure") {{
                checkCoordinates = (check) -> check.coords.chunkXPos == 0 && check.coords.chunkZPos == 0;
                minHeight = 63;
                startPool = "start";
                platform = SupportBasic.class;
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

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandLocate());
    }

    public SpawnCondition getStructureAt(World world, int chunkX, int chunkZ) {
        return worldGenerator.getStructureAt(world, chunkX, chunkZ);
    }

}
