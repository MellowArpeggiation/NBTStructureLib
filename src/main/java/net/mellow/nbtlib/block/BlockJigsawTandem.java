package net.mellow.nbtlib.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mellow.nbtlib.Registry;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockJigsawTandem extends BlockJigsaw {

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityJigsawTandem();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon(Registry.MODID + ":structure_tandem");
        iconTop = iconRegister.registerIcon(Registry.MODID + ":structure_tandem_top");
        iconSide = iconRegister.registerIcon(Registry.MODID + ":structure_tandem_side");
        iconBack = iconRegister.registerIcon(Registry.MODID + ":structure_tandem_back");
    }

    public static class TileEntityJigsawTandem extends TileEntityJigsaw {

    }

}
