package net.mellow.nbtlib.gui;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public interface ILookOverlay {

    @SideOnly(Side.CLIENT)
    public List<String> printOverlay();

}
