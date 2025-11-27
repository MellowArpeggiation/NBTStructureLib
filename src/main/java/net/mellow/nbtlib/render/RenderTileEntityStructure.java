package net.mellow.nbtlib.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

public class RenderTileEntityStructure extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float interp) {
        GL11.glPushMatrix();
        {



        }
        GL11.glPopMatrix();
    }

}
