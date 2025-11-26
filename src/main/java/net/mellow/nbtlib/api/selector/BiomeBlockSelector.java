package net.mellow.nbtlib.api.selector;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.structure.StructureComponent.BlockSelector;

public abstract class BiomeBlockSelector extends BlockSelector {

    public BiomeGenBase nextBiome;

}
