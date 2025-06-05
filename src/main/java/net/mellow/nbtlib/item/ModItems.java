package net.mellow.nbtlib.item;

import cpw.mods.fml.common.registry.GameRegistry;
import net.mellow.nbtlib.Registry;
import net.minecraft.item.Item;

public class ModItems {

    public static void register() {
        initItems();
        registerItems();
    }

    public static Item structure_wand;

    private static void initItems() {

        structure_wand = new ItemStructureWand().setUnlocalizedName("structure_wand").setMaxStackSize(1).setFull3D().setTextureName(Registry.MODID + ":structure_wand");

    }

    private static void registerItems() {

        register(structure_wand);

    }

    private static void register(Item item) {
        GameRegistry.registerItem(item, item.getUnlocalizedName());
    }

}
