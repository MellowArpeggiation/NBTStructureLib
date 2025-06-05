package net.mellow.nbtlib.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mellow.nbtlib.render.RenderBlockReplace;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockReplace extends Block {

	public final Block exportAs;

	protected BlockReplace(Block exportAs) {
		super(Material.glass);
		this.exportAs = exportAs;
		setBlockBounds(1F/16F, 1F/16F, 1F/16F, 15F/16F, 15F/16F, 15F/16F);
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
