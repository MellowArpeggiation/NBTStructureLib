package net.mellow.nbtlib.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.render.RenderBlockJigsaw;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockJigsaw extends BlockContainer {

    private IIcon iconTop;
    private IIcon iconSide;
    private IIcon iconBack;

    protected BlockJigsaw() {
        super(Material.iron);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return null;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
        int l = BlockPistonBase.determineOrientation(world, x, y, z, player);
        world.setBlockMetadataWithNotify(x, y, z, l, 2);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        this.blockIcon = iconRegister.registerIcon(Registry.MODID + ":structure_jigsaw");
        this.iconTop = iconRegister.registerIcon(Registry.MODID + ":structure_jigsaw_top");
        this.iconSide = iconRegister.registerIcon(Registry.MODID + ":structure_jigsaw_side");
        this.iconBack = iconRegister.registerIcon(Registry.MODID + ":structure_jigsaw_back");
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if(side == meta) return blockIcon;
        if(isOpposite(side, meta)) return iconBack;
        if(side <= 1) return iconTop;
        if(side > 3 && meta <= 1) return iconTop;
        return iconSide;
    }

    public int getRotationFromSide(IBlockAccess world, int x, int y, int z, int side) {
        if (side == 0)
            return topToBottom(getRotationFromSide(world, x, y, z, 1));

        int meta = world.getBlockMetadata(x, y, z);
        if (side == meta || isOpposite(side, meta)) return 0;

        // downwards facing has no changes, upwards flips anything not handled already
        if (meta == 0) return 0;
        if (meta == 1) return 3;

        // top (and bottom) is rotated fairly normally
        if (side == 1) {
            switch (meta) {
            case 2: return 3;
            case 3: return 0;
            case 4: return 1;
            case 5: return 2;
            }
        }

        // you know what I aint explaining further, it's a fucking mess here
        if (meta == 2) return side == 4 ? 2 : 1;
        if (meta == 3) return side == 4 ? 1 : 2;
        if (meta == 4) return side == 2 ? 1 : 2;
        if (meta == 5) return side == 2 ? 2 : 1;

        return 0;
    }

    // 0 1 3 2 becomes 0 2 3 1
    // I want to smoke that swedish kush because it clearly makes you fucking stupid
    private static int topToBottom(int topRotation) {
        switch(topRotation) {
        case 1: return 2;
        case 2: return 1;
        default: return topRotation;
        }
    }

    private static boolean isOpposite(int from, int to) {
        switch(from) {
        case 0: return to == 1;
        case 1: return to == 0;
        case 2: return to == 3;
        case 3: return to == 2;
        case 4: return to == 5;
        case 5: return to == 4;
        default: return false;
        }
    }

    @Override
    public int getRenderType() {
        return RenderBlockJigsaw.renderID;
    }

}
