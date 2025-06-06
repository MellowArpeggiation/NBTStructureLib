package net.mellow.nbtlib.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class ItemBlockTooltip extends ItemBlock {

    public ItemBlockTooltip(Block block) {
        super(block);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean bool) {
        for (String line : I18n.format(getUnlocalizedName() + ".desc").split("\\$")) {
            list.add(line);
        }
    }

}
