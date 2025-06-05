package net.mellow.nbtlib;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.mellow.nbtlib.render.RenderBlockReplace;

public class ClientProxy extends CommonProxy {

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods as well.

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        registerRendering();
    }

    private void registerRendering() {
        RenderingRegistry.registerBlockHandler(new RenderBlockReplace());
    }

}
