package net.mellow.nbtlib.block;

import net.minecraft.block.Block;

public class BlockMeta {

    public Block block;
    public int meta;

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
