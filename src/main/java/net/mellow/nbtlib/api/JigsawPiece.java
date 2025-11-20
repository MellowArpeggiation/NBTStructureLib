package net.mellow.nbtlib.api;

import java.util.HashMap;
import java.util.Map;

import net.mellow.nbtlib.Registry;
import net.minecraft.block.Block;
import net.minecraft.world.gen.structure.StructureComponent.BlockSelector;

public class JigsawPiece {

    // Translates a given name into a jigsaw piece, for serialization
    protected static Map<String, JigsawPiece> jigsawMap = new HashMap<>();

    public final String name;
    public final NBTStructure structure;

    // Block modifiers, for randomization and terrain matching

    /**
     * Replaces matching blocks using the result of a BlockSelector
     */
    public Map<Block, BlockSelector> blockTable;

    /**
     * Moves every single column to the terrain (for digging out trenches, natural formations, etc)
     */
    public boolean conformToTerrain = false;

    /**
     * Aligns this component y-level individually, without moving individual columns (like village houses)
     */
    public boolean alignToTerrain = false;

    /**
     * Height offset for this structure piece, -1 will sink the floor flush into the ground
     */
    public int heightOffset = 0;

    /**
     * Will fill any air gap beneath the jigsaw piece using the given selector
     */
    public BlockSelector platform;

    /**
     * If greater than 0, will limit the number of times this piece can spawn in a structure
     */
    public int instanceLimit = 0;


    public JigsawPiece(String name, NBTStructure structure) {
        this(name, structure, 0);
    }

    public JigsawPiece(String name, NBTStructure structure, int heightOffset) {
        if (name == null || "".equals(name))
            throw new IllegalStateException("A severe error has occurred in NBTStructure! A jigsaw piece has been registered without a valid name!");

        name = Registry.addPrefix(name);

        if (jigsawMap.containsKey(name))
            throw new IllegalStateException("A severe error has occurred in NBTStructure! A jigsaw piece has been registered with the same name as another: " + name);

        this.name = name;
        this.structure = structure;
        jigsawMap.put(name, this);

        this.heightOffset = heightOffset;
    }

    protected static class WeightedJigsawPiece {

        public final JigsawPiece piece;
        public final int weight;

        public WeightedJigsawPiece(JigsawPiece piece, int weight) {
            this.piece = piece;
            this.weight = weight;
        }

    }

}
