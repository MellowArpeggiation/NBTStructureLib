package net.mellow.nbtlib.block;

import cpw.mods.fml.common.registry.GameRegistry;
import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.block.BlockJigsaw.TileEntityJigsaw;
import net.mellow.nbtlib.block.BlockJigsawTandem.TileEntityJigsawTandem;
import net.mellow.nbtlib.block.BlockLoot.TileEntityLoot;
import net.mellow.nbtlib.block.BlockStructure.TileEntityStructure;
import net.mellow.nbtlib.item.ItemBlockMulti;
import net.mellow.nbtlib.item.ItemBlockReplace;
import net.mellow.nbtlib.item.ItemBlockTooltip;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class ModBlocks {

    public static void register() {
        initBlocks();
        registerBlocks();
        registerTileEntities();
    }

    public static Block structure_block;

    public static Block structure_air;
    public static Block structure_jigsaw;
    public static Block structure_tandem;
    public static Block structure_loot;

    private static void initBlocks() {

        structure_block = new BlockStructure().setBlockName("structure_block");

        structure_air = new BlockReplace(Blocks.air).setBlockName("structure_air").setBlockTextureName(Registry.MODID + ":structure_air");
        structure_jigsaw = new BlockJigsaw().setBlockName("structure_jigsaw");
        structure_tandem = new BlockJigsawTandem().setBlockName("structure_tandem");
        structure_loot = new BlockLoot().setBlockName("structure_loot");

    }

    private static void registerBlocks() {

        register(structure_block, ItemBlockMulti.class);

        register(structure_air, ItemBlockReplace.class);
        register(structure_jigsaw);
        register(structure_tandem);
        register(structure_loot);

    }

    private static void registerTileEntities() {

        GameRegistry.registerTileEntity(TileEntityStructure.class, "tileentity_structure");

        GameRegistry.registerTileEntity(TileEntityJigsaw.class, "tileentity_structure_jigsaw");
        GameRegistry.registerTileEntity(TileEntityJigsawTandem.class, "tileentity_structure_tandem");
        GameRegistry.registerTileEntity(TileEntityLoot.class, "tileentity_structure_loot");

    }

    private static void register(Block block) {
        Registry.proxy.registerBlock(block, ItemBlockTooltip.class, block.getUnlocalizedName());
    }

    private static void register(Block block, Class<? extends ItemBlock> itemClass) {
        Registry.proxy.registerBlock(block, itemClass, block.getUnlocalizedName());
    }

    public static Block getBlockFromStack(ItemStack stack) {
        if (stack == null) return null;
        if (!(stack.getItem() instanceof ItemBlock)) return null;

        return ((ItemBlock) stack.getItem()).field_150939_a;
    }

    // Is this block a special structure handling block, so we can ignore it for blacklist selection, etc.
    public static boolean isStructureBlock(Block block, boolean includeAir) {
        if (block == null) return false;
        if (block == structure_air) return includeAir;
        if (block == structure_block) return true;
        if (block == structure_jigsaw) return true;
        if (block == structure_tandem) return true;
        if (block == structure_loot) return true;
        return false;
    }

    // Helper for fetching multi-block names
    public static String getUnlocalizedName(Block block, int meta) {
        if (block instanceof IBlockMulti) {
            return ((IBlockMulti) block).getUnlocalizedName(meta);
        }

        return block.getUnlocalizedName();
    }

}
