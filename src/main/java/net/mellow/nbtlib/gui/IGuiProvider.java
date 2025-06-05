package net.mellow.nbtlib.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public interface IGuiProvider {

    public Object provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z);
    public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z);

}
