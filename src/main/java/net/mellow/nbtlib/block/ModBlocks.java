package net.mellow.nbtlib.block;

import cpw.mods.fml.common.registry.GameRegistry;
import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.block.BlockJigsaw.TileEntityJigsaw;
import net.mellow.nbtlib.block.BlockLoot.TileEntityLoot;
import net.mellow.nbtlib.item.ItemBlockReplace;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;

public class ModBlocks {

    public static void register() {
        initBlocks();
        registerBlocks();
        registerTileEntities();
    }

    public static Block structure_air;
    public static Block structure_jigsaw;
    public static Block structure_loot;

    private static void initBlocks() {

        structure_air = new BlockReplace(Blocks.air).setBlockName("structure_air").setBlockTextureName(Registry.MODID + ":structure_air");
        structure_jigsaw = new BlockJigsaw().setBlockName("structure_jigsaw");
        structure_loot = new BlockLoot().setBlockName("structure_loot");

    }

    private static void registerBlocks() {

        register(structure_air, ItemBlockReplace.class);
        register(structure_jigsaw);
        register(structure_loot);

    }

    private static void registerTileEntities() {

        GameRegistry.registerTileEntity(TileEntityJigsaw.class, "tileentity_structure_jigsaw");
        GameRegistry.registerTileEntity(TileEntityLoot.class, "tileentity_structure_loot");

    }

    private static void register(Block block) {
        GameRegistry.registerBlock(block, block.getUnlocalizedName());
    }

    private static void register(Block block, Class<? extends ItemBlock> itemClass) {
        GameRegistry.registerBlock(block, itemClass, block.getUnlocalizedName());
    }

}
