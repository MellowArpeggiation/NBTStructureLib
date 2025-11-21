# NBT Structure Lib
> Author complex structures to spawn in your mod from within your mod!

Chunk aware, cascadeless structure generation, with modern jigsaw support! Utilises `.nbt` files which can be authored directly within your mod, removing the need to manually type out block locations. The NBT format used is compatible with modern Minecraft structure files.

This system is analogous to [jigsaw structures](https://minecraft.wiki/w/Jigsaw_structure) found in modern Minecraft versions, with a few differences. This means that any guides on how to design good jigsaw structures should be very helpful. But no `.json` structure definition support currently exists, you'll have to write Java to implement structures in your mod. The more significant differences are:
* Developer intent is respected, only blocks that you place will generate, meaning air is _not_ included by default, you must place your own air blocks.
* No structure block exists (yet), you save structures using a Structure Wand.
* Rollable/Aligned is based on the orientation the structures are authored in, rather than the rotation of the jigsaw block.
* All structure spawning is defined in code, rather than a `.json` file.


Instructions on how to build your own structures with this mod are available at the [GH Wiki](https://github.com/MellowArpeggiation/NBTStructureLib/wiki)!