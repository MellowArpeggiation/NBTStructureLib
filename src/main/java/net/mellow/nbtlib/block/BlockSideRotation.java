package net.mellow.nbtlib.block;

import net.mellow.nbtlib.api.INBTBlockTransformable;
import net.mellow.nbtlib.render.RenderBlockSideRotation;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public abstract class BlockSideRotation extends BlockContainer implements INBTBlockTransformable {

    protected IIcon iconTop;
    protected IIcon iconSide;
    protected IIcon iconBack;

    protected BlockSideRotation(Material mat) {
        super(mat);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
        int l = BlockPistonBase.determineOrientation(world, x, y, z, player);
        world.setBlockMetadataWithNotify(x, y, z, l, 2);
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (side == meta) return blockIcon;
        if (isOpposite(side, meta)) return iconBack;
        if (side <= 1) return iconTop;
        if (side > 3 && meta <= 1) return iconTop;
        return iconSide;
    }

    @Override
    public int getRenderType() {
        return RenderBlockSideRotation.renderID;
    }

    @Override
    public int transformMeta(int meta, int coordBaseMode) {
        return INBTBlockTransformable.transformMetaSignLadder(meta, coordBaseMode);
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
    public static int topToBottom(int topRotation) {
        switch(topRotation) {
        case 1: return 2;
        case 2: return 1;
        default: return topRotation;
        }
    }

    public static boolean isOpposite(int from, int to) {
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

}
