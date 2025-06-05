package net.mellow.nbtlib.block;

import net.minecraft.block.Block;

public class BlockMeta {

    public Block block;
    public int meta;

    public BlockMeta(Block block, int meta) {
        this.block = block;
        this.meta = meta;
    }

}
