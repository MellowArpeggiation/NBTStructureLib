package net.mellow.nbtlib;

import java.io.File;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.mellow.nbtlib.block.BlockStructure.TileEntityStructure;
import net.mellow.nbtlib.gui.LookOverlayHandler;
import net.mellow.nbtlib.render.RenderBlockSideRotation;
import net.mellow.nbtlib.render.RenderTileEntityStructure;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.mellow.nbtlib.render.RenderBlockReplace;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods as well.

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        registerBlockRendering();
        registerTileEntityRendering();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        MinecraftForge.EVENT_BUS.register(new LookOverlayHandler());
    }

    private void registerBlockRendering() {

        RenderingRegistry.registerBlockHandler(new RenderBlockReplace());
        RenderingRegistry.registerBlockHandler(new RenderBlockSideRotation());

    }

    private void registerTileEntityRendering() {

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityStructure.class, new RenderTileEntityStructure());

    }

    @Override
    public void registerBlock(Block block, Class<? extends ItemBlock> itemclass, String name) {
        GameRegistry.registerBlock(block, itemclass, name);
    }

    @Override
    public void registerItem(Item item, String name) {
        GameRegistry.registerItem(item, name);
    }

    @Override
    public Block getBlockFromName(String name) {
        return Block.getBlockFromName(name);
    }

    @Override
    public File getStructurePackDir() {
        return new File(Minecraft.getMinecraft().mcDataDir, "structurepacks");
    }

}
