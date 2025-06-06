package net.mellow.nbtlib.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mellow.nbtlib.Registry;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class BlockStructure extends BlockContainer {

    private IIcon saveIcon;
    private IIcon loadIcon;
    private IIcon cornerIcon;

    protected BlockStructure() {
        super(Material.iron);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        saveIcon = iconRegister.registerIcon(Registry.MODID + ":structure_block_save");
        loadIcon = iconRegister.registerIcon(Registry.MODID + ":structure_block_load");
        cornerIcon = iconRegister.registerIcon(Registry.MODID + ":structure_block_corner");
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (meta == 0) return saveIcon;
        if (meta == 1) return loadIcon;
        return cornerIcon;
    }

}
