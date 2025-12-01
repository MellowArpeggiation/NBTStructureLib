package net.mellow.nbtlib;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
import net.mellow.nbtlib.api.selector.BiomeFillerSelector;
import net.mellow.nbtlib.api.selector.BrickSelector;
import net.mellow.nbtlib.api.selector.StoneBrickSelector;
import net.mellow.nbtlib.block.ModBlocks;
import net.mellow.nbtlib.command.CommandLocate;
import net.mellow.nbtlib.gui.GuiHandler;
import net.mellow.nbtlib.item.ModItems;
import net.mellow.nbtlib.network.NetworkHandler;
import net.mellow.nbtlib.pack.StructurePackLoader;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.launchwrapper.Launch;
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
                sizeLimit = 128;
                minHeight = 63;
                startPool = "start";
                pools = new HashMap<String, JigsawPool>() {{
                    put("start", new JigsawPool() {{
                        add(new JigsawPiece("example_structure_core", StructureManager.test_jigsaw_core) {{
                            platform = new StoneBrickSelector();
                        }}, 1);
                    }});
                    put("default", new JigsawPool() {{
                        add(new JigsawPiece("example_structure_junction", StructureManager.test_jigsaw_junction) {{
                            platform = new BiomeFillerSelector();
                        }}, 20);
                        add(new JigsawPiece("example_structure_hall", StructureManager.test_jigsaw_hall) {{
                            platform = new BrickSelector();
                        }}, 20);
                    }});
                }};
            }});

            // Example: add new pieces to an existing structure
            SpawnCondition existing = NBTGeneration.getStructure("nbtlib", "example_structure");
            existing.pools.get("default").add(new JigsawPiece("example_structure_core_repeat", StructureManager.test_jigsaw_core) {{
                platform = new StoneBrickSelector();
                instanceLimit = 1;
                required = true;
            }}, 1);
        }
    }

    public void postInit(FMLPostInitializationEvent event) {
        // in post so structure extensions can be applied to structures in other mods
        StructurePackLoader.init();
    }

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandLocate());
    }

    private Map<String, Block> serverBlockRegistry = new HashMap<>();

    public void registerBlock(Block block, Class<? extends ItemBlock> itemclass, String name) {
        serverBlockRegistry.put(Registry.addPrefix(name), block);
    }

    public void registerItem(Item item, String name) {

    }

    public Block getBlockFromName(String name) {
        Block block = Block.getBlockFromName(name);
        if (block != null) return block;

        return serverBlockRegistry.get(name);
    }

    public SpawnCondition getStructureAt(World world, int chunkX, int chunkZ) {
        return worldGenerator.getStructureAt(world, chunkX, chunkZ);
    }

    public File getStructurePackDir() {
        return new File(Launch.minecraftHome, "structurepacks");
    }

}
