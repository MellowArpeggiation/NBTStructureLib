package net.mellow.nbtlib.gui;

import java.util.List;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.mellow.nbtlib.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;

public class LookOverlayHandler {

    @SubscribeEvent
    public void onOverlayRender(RenderGameOverlayEvent.Pre event) {
        if (event.type == ElementType.CROSSHAIRS) {
            Minecraft mc = Minecraft.getMinecraft();
            WorldClient world = mc.theWorld;
            MovingObjectPosition mop = mc.objectMouseOver;

            if (mop != null && mop.typeOfHit == MovingObjectType.BLOCK) {
                TileEntity tile = world.getTileEntity(mop.blockX, mop.blockY, mop.blockZ);

                if (tile instanceof ILookOverlay) {
                    ScaledResolution resolution = event.resolution;

                    GL11.glPushMatrix();
                    {

                        String title = I18n.format(ModBlocks.getUnlocalizedName(tile.getBlockType(), tile.getBlockMetadata()) + ".name");
                        List<String> text = ((ILookOverlay) tile).printOverlay();

                        if (text != null) {
                            int pX = resolution.getScaledWidth() / 2 + 8;
                            int pZ = resolution.getScaledHeight() / 2;

                            mc.fontRenderer.drawString(title, pX + 1, pZ - 9, 0x404000);
                            mc.fontRenderer.drawString(title, pX, pZ - 10, 0xffff00);

                            try {
                                for (String line : text) {

                                    int color = 0xFFFFFF;
                                    if (line.startsWith("&[")) {
                                        int end = line.lastIndexOf("&]");
                                        color = Integer.parseInt(line.substring(2, end));
                                        line = line.substring(end + 2);
                                    }

                                    mc.fontRenderer.drawStringWithShadow(line, pX, pZ, color);
                                    pZ += 10;
                                }
                            } catch (Exception ex) {
                                mc.fontRenderer.drawStringWithShadow(ex.getClass().getSimpleName(), pX, pZ + 10, 0xff0000);
                            }

                            GL11.glDisable(GL11.GL_BLEND);
                            GL11.glColor3f(1F, 1F, 1F);
                        }

                    }
                    GL11.glPopMatrix();

                    Minecraft.getMinecraft().renderEngine.bindTexture(Gui.icons);
                }
            }
        }
    }

}
