package net.mellow.nbtlib.api;

import net.mellow.nbtlib.Registry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

public class BlockMeta {

    public final Block block;
    public final int meta;

    public BlockMeta(String name, int meta) {
        Block block = Registry.proxy.getBlockFromName(name);
        if (block == null) block = Blocks.air;

        this.block = block;
        this.meta = meta;
    }

    public BlockMeta(Block block, int meta) {
        this.block = block;
        this.meta = meta;
    }

    @Override
    public int hashCode() {
        final int prime = 27644437;
        int result = 1;
        result = prime * result + ((block == null) ? 0 : block.hashCode());
        result = prime * result + meta;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        BlockMeta other = (BlockMeta) obj;
        if (block == null) {
            if (other.block != null) return false;
        } else if (!block.equals(other.block)) {
            return false;
        }

        return meta == other.meta;
    }

}
