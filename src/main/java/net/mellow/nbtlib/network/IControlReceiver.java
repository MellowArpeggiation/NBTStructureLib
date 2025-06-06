package net.mellow.nbtlib.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public interface IControlReceiver {

    public boolean hasPermission(EntityPlayer player);
    public void receiveControl(EntityPlayer player, NBTTagCompound nbt);

}
