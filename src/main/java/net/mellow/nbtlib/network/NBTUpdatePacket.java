package net.mellow.nbtlib.network;

import java.io.IOException;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;

public class NBTUpdatePacket implements IMessage {

    PacketBuffer buffer;
    int x;
    int y;
    int z;

    public NBTUpdatePacket() {}

    public NBTUpdatePacket(NBTTagCompound nbt, int x, int y, int z) {
        this.buffer = new PacketBuffer(Unpooled.buffer());
        this.x = x;
        this.y = y;
        this.z = z;

        try {
            buffer.writeNBTTagCompoundToBuffer(nbt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();

        if (buffer == null) buffer = new PacketBuffer(Unpooled.buffer());

        buffer.writeBytes(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);

        if (buffer == null) buffer = new PacketBuffer(Unpooled.buffer());

        buf.writeBytes(buffer);
    }

    public static class HandlerServer implements IMessageHandler<NBTUpdatePacket, IMessage> {

        @Override
        public IMessage onMessage(NBTUpdatePacket message, MessageContext ctx) {
            EntityPlayer player = ctx.getServerHandler().playerEntity;
            if (player.worldObj == null) return null;

            TileEntity tile = player.worldObj.getTileEntity(message.x, message.y, message.z);

            try {
                NBTTagCompound nbt = message.buffer.readNBTTagCompoundFromBuffer();

                if (nbt != null && tile instanceof IControlReceiver) {
                    IControlReceiver receiver = (IControlReceiver) tile;

                    if (receiver.hasPermission(player)) {
                        receiver.receiveControl(player, nbt);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                message.buffer.release();
            }

            return null;
        }

    }

    public static class HandlerClient implements IMessageHandler<NBTUpdatePacket, IMessage> {

        @Override
        public IMessage onMessage(NBTUpdatePacket message, MessageContext ctx) {
            WorldClient world = Minecraft.getMinecraft().theWorld;
            if (world == null) return null;

            TileEntity tile = world.getTileEntity(message.x, message.y, message.z);

            try {
                NBTTagCompound nbt = message.buffer.readNBTTagCompoundFromBuffer();

                if (nbt != null) {
                    tile.readFromNBT(nbt);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                message.buffer.release();
            }

            return null;
        }

    }

}
