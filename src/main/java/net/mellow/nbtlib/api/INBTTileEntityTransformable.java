package net.mellow.nbtlib.api;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Like INBTBlockTransformable but for TileEntities
 */
public interface INBTTileEntityTransformable {

    /**
     * Allows for the TE to modify itself when spawned in an NBT structure.
     *
     * You should always return null unless you wish to switch to a new block & TE.
     *
     * Can return a different TE to switch to an entirely new block, if
     * this is done, you MUST set `blockType` on the new TE so the structure
     * builder knows what block to place!
     *
     * If no `blockType` is set, the returned TE will be ignored.
     *
     * Note: the TE is not in a world yet, so use the provided `world`!
     */
    public TileEntity transformTE(World world, int coordBaseMode);

}
