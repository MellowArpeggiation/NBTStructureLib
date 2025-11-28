package net.mellow.nbtlib.block;

import net.minecraft.item.ItemStack;

public interface IBlockMulti {

    public String getUnlocalizedName(int meta);

    public default String getUnlocalizedName(ItemStack stack) {
        if (stack == null) return getUnlocalizedName(0);
        return getUnlocalizedName(stack.getItemDamage());
    }

}
