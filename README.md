# NBT Structure Lib
> Author complex structures to spawn in your mod from within your mod!

Chunk aware, cascadeless structure generation, with modern jigsaw support! Utilises `.nbt` files which can be authored directly within your mod, removing the need to manually type out block locations. The NBT format used is compatible with modern Minecraft structure files.

This system is analogous to [jigsaw structures](https://minecraft.wiki/w/Jigsaw_structure) found in modern Minecraft versions, with a few differences. This means that any guides on how to design good jigsaw structures should be very helpful. But no `.json` structure definition support currently exists, you'll have to write Java to implement structures in your mod. The more significant differences are:
* Developer intent is respected, only blocks that you place will generate, meaning air is _not_ included by default, you must place your own air blocks.
* No structure block exists (yet), you save structures using a Structure Wand.
* Rollable/Aligned is based on the orientation the structures are authored in, rather than the rotation of the jigsaw block.
* All structure spawning is defined in code, rather than a `.json` file.



## How to build basic structures
Basic structures, as in, ones that don't generate using any jigsaw pieces and are just one big structure- are very easy to implement within NBTStructureLib. All you need is a structure wand and a structure you've built.


### Preparing your structure
Before we save our structure, we'll need to add blocks that indicate to the structure generation system to place certain blocks. The structure system is designed with developer intent in mind, in that only blocks explicitly placed will spawn in the structure.

In pursuit of this, two placeholder blocks are available for you to place in your structures:

* **Structure Air Block** - This block will replace any block it finds with air, underground structures will need air placed or else they'll be vitrified with rock. Smart placement of air can create cool effects (say, placing air everywhere except the bottom-most blocks to make a nuclear sub placed in water be half flooded).
* **Structure Loot Block** - Normal chests will always spawn with exactly the items placed in them, this block will instead randomly generate its contents from `GenChestHook` categories (which can be added to). The arrow indicates the facing direction of the inventory block when generated. These blocks can be set to generate any type of single block inventory (chests, etc) by using the desired block on them directly.


### Saving your structure
Next, we need to create our structure `.nbt`. In order to create that, grab a structure wand, and click two corners that enclose your entire build. If you have blocks inside your build that you don't want (say, you build in a Redstone Ready superflat world and don't want to include sandstone), you can crouch click that block to blacklist it from saving.

A useful tip for saving the bounds of your structure is to pick a block that you definitely won't be using within the structure (like say, gold blocks), adding that to the blacklist, and using those blocks to mark the corners of your structure. Then for any changes you know exactly where to click to get the exact same bounds each time.

Once you've saved your structure, it'll be saved in the same directory as your `screenshots/` folder, under `structures/`.


### Adding your structure to your mod
Now that you have your structure .nbt, you must define how it spawns in your mod. You'll want some location to load in these structures (Note that this loads on servers, so it should _not_ be done where you load models/textures). Your loading should look something like this:

```java
public static final NBTStructure my_structure = new NBTStructure(new ResourceLocation(RefStrings.MODID, "structures/my_structure.nbt"));
```

Now that you have a structure loading in, you'll want to indicate to the world generator to spawn it into specifc dimensions/biomes. The example below will spawn `my_structure` in dimension 0 (Overworld). All fields here are set to their defaults except for `canSpawn` and `structure`. Your spawning defintion should look like the below:

```java
NBTGeneration.registerStructure(0, new SpawnCondition() {{

	// This is the structure to spawn, the optional number at the end defines the y-offset of the piece.
	// If you had, say, a concrete floor, you'd put in -1 to sink the floor into the ground.
	structure = new JigsawPiece("my_structure", StructureManager.my_structure, 0) {{

		// If true, moves every single column to the terrain
		// (terrain conforming natural structures, or digging out trenches if negative and has air blocks)
		conformToTerrain = false;

		// Defines block replacements based on a BlockSelector, which can randomly pick new blocks
		blockTable = new HashMap<Block, BlockSelector>() {{
			put(Blocks.wool, new RandomWoolColor()); // example
		}};

	}};

	// This defines what biomes the structure can spawn in, any biome fields can be used, like temperature or height.
	canSpawn = biome -> biome == BiomeGenBase.deepOcean; // example

	// How likely this structure is to spawn compared to others, higher = more likely
	spawnWeight = 1;

	// Height modifiers, will clamp height that the start generates at, allowing for:
	//  * Submarines that must spawn under the ocean surface
	//  * Bunkers that sit underneath the ground
	//  * Airships that must float in the sky
	maxHeight = 1;
	minHeight = 128;

}});
```

Now your structure should spawn in the biomes you've selected with rarity defined by spawn weight.


### Adding Subtle Variation
A way of adding simple variation to your structures to make them somewhat visually distinct and randomized is to use BlockSelectors, these can take a specified block and randomly turn it into another block. For example, you can turn concrete bricks into their cracked and mossy variants. See the above code definitions to add these.


### Conforming to Terrain
If a structure is defined as conforming to terrain, every single column in the structure file will be moved such that the starting y-level (before offset is applied) is moved to the highest generated non-liquid block in the world.



## Building a jigsaw structure
Earlier we mentioned two structure blocks used for defining structure information for the generator to use. However, there is another:
* **Structure Jigsaw Block** - When the generator finds one of these in a structure, it'll grab another `.nbt` structure from a defined pool of structures and attach it directly, based on the connecting arrow direction. The system will continuously add new pieces until it connects all available jigsaw blocks or it hits either the piece limit (sizeLimit) or horizontal range limit (rangeLimit).

In order to define the pools that must be used in the structure, instead of defining a single structure, we instead define pools, and the first piece that the structure places down will be pulled from the pool named in startPool.

```java
NBTGeneration.registerStructure(0, new SpawnCondition() {{

	// Which of the below pools should be used to select our first piece.
	startPool = "start";

	// The pools from which structure pieces are pulled from to connect to a given jigsaw.
	pools = new HashMap<String, JigsawPool>() {{

		// Our starting pool, this one has just one structure in it, so we'll always generate from the same piece.
		put("start", new JigsawPool() { {
			add(new JigsawPiece("my_structure_core", StructureManager.my_structure_core), 1);
		}});

		// By default, jigsaw blocks will target a pool named "default" to pull from, so it's easiest to use that name.
		// There are multiple pieces in here, so one will be randomly selected, based on weight
		put("default", new JigsawPool() { {
			add(new JigsawPiece("my_structure_corner", StructureManager.my_structure_corner), 2);
			add(new JigsawPiece("my_structure_t", StructureManager.my_structure_t), 3);
			add(new JigsawPiece("my_structure_stairs", StructureManager.my_structure_stairs), 1);

			// If the structure runs out of space or has too many pieces, it'll instead grab a piece from this pool.
			// Fallbacks do not generate any more connecting pieces.
			fallback = "fallback";
		}});

		// Our fallback pool, note that none of these names are explicit, they can be called whatever you want,
		// as long as the jigsaw blocks use them in their defined Target Pool.
		put("fallback", new JigsawPool() { {
			add(new JigsawPiece("my_structure_fallback", StructureManager.my_structure_fallback), 1);
		}});
	}};

}});
```


### Defining Jigsaw Connections

Before you save your .nbt structure however, you'll need to add and configure the jigsaw blocks. Jigsaw blocks have fields that determine how they connect new pieces to themselves, which are:

* Target Pool - The name of the pool to grab from, as defined in the code above. For example, you may have a "loot" pool with many small 3x3x3 structures inside, to randomize loot placed at a certain location within your structure.
* Name - The name of this jigsaw block.
* Target Name - When the generator goes through all unconnected jigsaw blocks, it'll use this field to target specific jigsaw blocks in the incoming piece. This can be used to make sure that a piece with further connections doesn't pick the wrong jigsaw piece to connect at.
* Selection Priority - The order in which to evaluate unconnected jigsaw blocks for new connections. This can ensure that a given jigsaw is connected to before others.
* Placement Priority - The order in which incoming pieces are evaluated for further connections. This is useful in niche scenarios where you want a specific room type to always generate early, so it doesn't get skipped due to range/size limits.
* Joint Type - This only applies to vertical connections: Rollable joints will rotate randomly, and Aligned joints will be fixed in rotation relative to each other.



## How to design good jigsaw structures
[This video by CodeNeon](https://www.youtube.com/watch?v=BIa_gqc93ok) is a great resource for designing good dungeons with jigsaw blocks, exploring interesting ways of generating rooms. Note that the vanilla 8 structure piece limit does not apply here, and air is _not_ included by default!

A good way to quickly iterate on your structures is to create a new world with `debugSpawning` enabled within `nbtlib.cfg`, then fly around until the log outputs that your structure generated. Once you've located a structure and inspected it, leave the world, open the world in your file browser, and delete both the region/ folder and the data/NBTStructures.dat. Upon reloading the world, it'll regenerate the structure from scratch, but due to the deterministic nature of world generation, the structure will generate the same way at the same location!