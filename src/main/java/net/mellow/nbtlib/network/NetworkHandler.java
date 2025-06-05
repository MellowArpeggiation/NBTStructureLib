package net.mellow.nbtlib.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.mellow.nbtlib.Registry;

public class NetworkHandler {

    public static final SimpleNetworkWrapper instance = NetworkRegistry.INSTANCE.newSimpleChannel(Registry.MODID);

    public static void init() {
        int i = 0;

        instance.registerMessage(NBTUpdatePacket.HandlerServer.class, NBTUpdatePacket.class, i++, Side.SERVER);
        instance.registerMessage(NBTUpdatePacket.HandlerClient.class, NBTUpdatePacket.class, i++, Side.CLIENT);
    }

}
