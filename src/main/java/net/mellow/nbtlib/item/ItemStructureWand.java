package net.mellow.nbtlib.item;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mellow.nbtlib.api.NBTStructure;
import net.mellow.nbtlib.block.BlockMeta;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class ItemStructureWand extends Item {

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean bool) {
        for (String line : I18n.format(getUnlocalizedName() + ".desc").split("\\$")) {
            list.add(line);
        }

        if (stack.stackTagCompound != null) {
            int px = stack.stackTagCompound.getInteger("x");
            int py = stack.stackTagCompound.getInteger("y");
            int pz = stack.stackTagCompound.getInteger("z");

            if (px != 0 || py != 0 || pz != 0) {
                list.add(EnumChatFormatting.AQUA + "From: " + px + ", " + py + ", " + pz);
            } else {
                list.add(EnumChatFormatting.AQUA + "No start position set");
            }

            Set<BlockMeta> blocks = getBlocks(stack);

            if (blocks.size() > 0) {
                list.add("Blacklist:");
                for (BlockMeta block : blocks) {
                    list.add(EnumChatFormatting.RED + "- " + block.block.getUnlocalizedName());
                }
            }
        }
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float fx, float fy, float fz) {
        if (stack.stackTagCompound == null) {
            stack.stackTagCompound = new NBTTagCompound();
        }

        if (player.isSneaking()) {
            BlockMeta target = new BlockMeta(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
            Set<BlockMeta> blocks = getBlocks(stack);

            if (blocks.contains(target)) {
                blocks.remove(target);
                if (world.isRemote)
                    player.addChatMessage(new ChatComponentText("Removed from blacklist " + target.block.getUnlocalizedName()));
            } else {
                blocks.add(target);
                if (world.isRemote)
                    player.addChatMessage(new ChatComponentText("Added to blacklist " + target.block.getUnlocalizedName()));
            }

            setBlocks(stack, blocks);

        } else {
            int px = stack.stackTagCompound.getInteger("x");
            int py = stack.stackTagCompound.getInteger("y");
            int pz = stack.stackTagCompound.getInteger("z");

            if (px == 0 && py == 0 && pz == 0) {
                setPosition(stack, x, y, z);

                if (world.isRemote)
                    player.addChatMessage(new ChatComponentText("First position set!"));
            } else {
                setPosition(stack, 0, 0, 0);

                Set<BlockMeta> blocks = getBlocks(stack);
                blocks.add(new BlockMeta(Blocks.air, 0));

                String filename = "structure_" + dateFormat.format(new Date()).toString() + ".nbt";

                NBTStructure.saveArea(filename, world, x, y, z, px, py, pz, blocks);

                if (world.isRemote)
                    player.addChatMessage(new ChatComponentText("Structure saved to: .minecraft/structures/" + filename));
            }
        }

        return true;
    }

    private void setPosition(ItemStack stack, int x, int y, int z) {
        stack.stackTagCompound.setInteger("x", x);
        stack.stackTagCompound.setInteger("y", y);
        stack.stackTagCompound.setInteger("z", z);
    }

    private Set<BlockMeta> getBlocks(ItemStack stack) {
        if (stack.stackTagCompound == null) {
            return new HashSet<>();
        }

        int[] blockIds = stack.stackTagCompound.getIntArray("blocks");
        int[] metas = stack.stackTagCompound.getIntArray("metas");
        Set<BlockMeta> blocks = new HashSet<>(blockIds.length);

        for (int i = 0; i < blockIds.length; i++) {
            blocks.add(new BlockMeta(Block.getBlockById(blockIds[i]), metas[i]));
        }

        return blocks;
    }

    private void setBlocks(ItemStack stack, Set<BlockMeta> blocks) {
        if (stack.stackTagCompound == null) {
            stack.stackTagCompound = new NBTTagCompound();
        }

        stack.stackTagCompound.setIntArray("blocks", collectionToIntArray(blocks, i -> Block.getIdFromBlock(((BlockMeta) i).block)));
        stack.stackTagCompound.setIntArray("metas", collectionToIntArray(blocks, i -> ((BlockMeta) i).meta));
    }

    private static int[] collectionToIntArray(Collection<? extends Object> in, ToIntFunction<? super Object> mapper) {
        return Arrays.stream(in.toArray()).mapToInt(mapper).toArray();
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (stack.stackTagCompound == null) {
            stack.stackTagCompound = new NBTTagCompound();
        }

        if (player.isSneaking()) {
            stack.stackTagCompound.setIntArray("blocks", new int[0]);
            stack.stackTagCompound.setIntArray("metas", new int[0]);

            if (world.isRemote) {
                player.addChatMessage(new ChatComponentText("Cleared blacklist"));
            }
        }

        return stack;
    }

}
