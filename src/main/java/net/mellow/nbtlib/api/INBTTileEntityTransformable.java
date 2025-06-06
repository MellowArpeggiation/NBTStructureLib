package net.mellow.nbtlib.api;

import net.minecraft.world.World;

public interface INBTTileEntityTransformable {

    /**
     * Like INBTBlockTransformable but for TileEntities
     */

    // Allows for the TE to modify itself when spawned in an NBT structure
    public void transformTE(World world, int coordBaseMode);

}
