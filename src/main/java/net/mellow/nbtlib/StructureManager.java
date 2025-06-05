package net.mellow.nbtlib;

import net.mellow.nbtlib.api.NBTStructure;
import net.minecraft.util.ResourceLocation;

public class StructureManager {

	public static final NBTStructure test_jigsaw_core = new NBTStructure(new ResourceLocation(Registry.MODID, "structures/test_jigsaw_core.nbt"));
	public static final NBTStructure test_jigsaw_junction = new NBTStructure(new ResourceLocation(Registry.MODID, "structures/test_jigsaw_junction.nbt"));
	public static final NBTStructure test_jigsaw_hall = new NBTStructure(new ResourceLocation(Registry.MODID, "structures/test_jigsaw_hall.nbt"));

}
