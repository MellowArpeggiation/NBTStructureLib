package net.mellow.nbtlib.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mellow.nbtlib.render.RenderBlockReplace;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockReplace extends Block {

    /**
     * This block is only visible and interactable while holding the block in your hand
     * making for invisible air placeholders when not editing the air directly!
     */

    public final Block exportAs;
    public IIcon itemIcon;

    protected BlockReplace(Block exportAs) {
        super(Material.glass);
        this.exportAs = exportAs;
        setBlockBounds(1F/16F, 1F/16F, 1F/16F, 15F/16F, 15F/16F, 15F/16F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        itemIcon = reg.registerIcon(getTextureName() + "_item");

        if (!(reg instanceof TextureMap)) {
            super.registerBlockIcons(reg);
            return;
        }

        TextureMap map = (TextureMap) reg;

        TextureAtlasSprite sprite = new TextureReplace(getTextureName(), this);
        if(map.setTextureEntry(getTextureName(), sprite)) {
            blockIcon = sprite;
        }
    }

    // We can tweak raycasting on the client only and still get desired results on the server!
    @Override
    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 startVec, Vec3 endVec) {
        if(world.isRemote && !TextureReplace.shouldRender(this)) return null;
        return super.collisionRayTrace(world, x, y, z, startVec, endVec);
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public int getRenderType() {
        return RenderBlockReplace.renderID;
    }

    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return world.getBlock(x, y, z) != this;
    }

}
