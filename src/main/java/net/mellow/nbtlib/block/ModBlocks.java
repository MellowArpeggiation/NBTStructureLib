package net.mellow.nbtlib.block;

import cpw.mods.fml.common.registry.GameRegistry;
import net.mellow.nbtlib.Registry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

public class ModBlocks {

    public static void register() {
        initBlocks();
        registerBlocks();
    }

    public static Block structure_air;
    public static Block structure_jigsaw;

    private static void initBlocks() {

        structure_air = new BlockReplace(Blocks.air).setBlockName("structure_air").setBlockTextureName(Registry.MODID + ":structure_air");

    }

    private static void registerBlocks() {

        register(structure_air);

    }

    private static void register(Block block) {
        GameRegistry.registerBlock(block, block.getUnlocalizedName());
    }

}
