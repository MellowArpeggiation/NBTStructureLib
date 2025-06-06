package net.mellow.nbtlib.block;

import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

@SideOnly(Side.CLIENT)
public class TextureReplace extends TextureAtlasSprite {

    private final Block block;
    private boolean wasRendering = true;

    protected TextureReplace(String name, Block block) {
        super(name);
        this.block = block;
    }

    @Override
    public void updateAnimation() {
        boolean shouldRender = shouldRender(block);
        if(shouldRender != wasRendering) {
            TextureUtil.uploadTextureMipmap((int[][])this.framesTextureData.get(shouldRender ? 0 : 1), this.width, this.height, this.originX, this.originY, false, false);
            wasRendering = shouldRender;
        }
    }

    public static boolean shouldRender(Block block) {
        Minecraft mc = Minecraft.getMinecraft();

        if(mc.theWorld == null || mc.thePlayer == null) return true;

        ItemStack held = mc.thePlayer.getHeldItem();
        if(held == null) return false;

        if (!(held.getItem() instanceof ItemBlock)) return false;
        return ((ItemBlock) held.getItem()).field_150939_a == block;
    }

}
