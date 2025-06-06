package net.mellow.nbtlib.render;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.mellow.nbtlib.block.BlockReplace;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

public class RenderBlockReplace implements ISimpleBlockRenderingHandler {

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        renderingInventory = true;

        BlockReplace replace = (BlockReplace) block;

        renderer.setOverrideBlockTexture(replace.itemIcon);
        renderer.renderBlockAsItem(block, metadata, 1.0F);
        renderer.clearOverrideBlockTexture();

        renderingInventory = false;
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
        renderer.renderFromInside = true;
        renderer.renderStandardBlock(block, x, y, z);
        renderer.renderFromInside = false;
        renderer.renderStandardBlock(block, x, y, z);
        return true;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    public static boolean renderingInventory;
    public static int renderID = RenderingRegistry.getNextAvailableRenderId();

    @Override
    public int getRenderId() {
        return renderID;
    }

}
