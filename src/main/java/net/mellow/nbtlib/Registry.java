package net.mellow.nbtlib;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = Registry.MODID, version = Tags.VERSION, name = "NBTStructureLib", acceptedMinecraftVersions = "[1.7.10]")
public class Registry {

    public static final String MODID = "nbtlib";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.Instance(Registry.MODID)
    public static Registry instance;

    @SidedProxy(clientSide = "net.mellow.nbtlib.ClientProxy", serverSide = "net.mellow.nbtlib.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

}
