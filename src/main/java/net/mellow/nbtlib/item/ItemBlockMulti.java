package net.mellow.nbtlib.item;

import net.mellow.nbtlib.block.IBlockMulti;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class ItemBlockMulti extends ItemBlock {

    public ItemBlockMulti(Block block) {
        super(block);
        setMaxDamage(0);
        setHasSubtypes(true);
    }

	@Override
	public int getMetadata(int meta) {
        return meta;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return ((IBlockMulti) this.field_150939_a).getUnlocalizedName(stack);
    }

}
