package net.mellow.nbtlib.api.format;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

import net.mellow.nbtlib.api.BlockMeta;
import net.mellow.nbtlib.api.BlockPos;
import net.mellow.nbtlib.block.BlockReplace;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public interface IStructureProvider {

    /**
     * The file extension this structure file format provides, EXLUCDING the leading dot
     * eg: "nbt"
     */
    public String getFileExtension();

    public void saveArea(OutputStream stream, World world, int x1, int y1, int z1, int x2, int y2, int z2, Set<BlockMeta> exclude);
    public NBTStructureData loadStructure(InputStream stream);


    public static BlockMeta fetchBlockMeta(World world, int x, int y, int z, Set<BlockMeta> exclude) {
        BlockMeta definition = new BlockMeta(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));

        if (exclude.contains(definition)) return null;

        if (definition.block instanceof BlockReplace) {
            definition = new BlockMeta(((BlockReplace) definition.block).exportAs, definition.meta);
        }

        return definition;
    }


    public static class NBTStructureData {

        public BlockPos size;
        public List<ItemPaletteEntry> itemPalette;
        public BlockState[][][] blockArray;

    }

    public static class BlockState {

        public final BlockMeta definition;
        public NBTTagCompound nbt;

        public BlockState(BlockMeta definition) {
            this.definition = definition;
        }

    }

    public static class ItemPaletteEntry {

        public final short id;
        public final String name;

        public ItemPaletteEntry(short id, String name) {
            this.id = id;
            this.name = name;
        }

    }

}
