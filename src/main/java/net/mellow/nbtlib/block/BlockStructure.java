package net.mellow.nbtlib.block;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.api.BlockMeta;
import net.mellow.nbtlib.api.NBTStructure;
import net.mellow.nbtlib.gui.IGuiProvider;
import net.mellow.nbtlib.gui.ILookOverlay;
import net.mellow.nbtlib.network.IControlReceiver;
import net.mellow.nbtlib.network.NBTUpdatePacket;
import net.mellow.nbtlib.network.NetworkHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class BlockStructure extends BlockContainer {

    private IIcon saveIcon;
    private IIcon loadIcon;
    private IIcon cornerIcon;

    protected BlockStructure() {
        super(Material.iron);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityStructure();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        saveIcon = iconRegister.registerIcon(Registry.MODID + ":structure_block_save");
        loadIcon = iconRegister.registerIcon(Registry.MODID + ":structure_block_load");
        cornerIcon = iconRegister.registerIcon(Registry.MODID + ":structure_block_corner");
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (meta == 1) return loadIcon;
        if (meta == 2) return cornerIcon;
        return saveIcon;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(x, y, z);

        if (!(te instanceof TileEntityStructure)) return false;

        TileEntityStructure structure = (TileEntityStructure) te;

        if (!player.isSneaking()) {
            Block block = ModBlocks.getBlockFromStack(player.getHeldItem());
            if (block != null && !ModBlocks.isStructureBlock(block, true)) {
                BlockMeta bm = new BlockMeta(block, player.getHeldItem().getItemDamage());

                if (structure.blacklist.contains(bm)) {
                    structure.blacklist.remove(bm);
                } else {
                    structure.blacklist.add(bm);
                }

                return true;
            }

            if (world.isRemote) FMLNetworkHandler.openGui(player, Registry.instance, 0, world, x, y, z);

            return true;
        }

        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item itemIn, CreativeTabs tab, List<ItemStack> list) {
        list.add(new ItemStack(itemIn, 1, 0));
        list.add(new ItemStack(itemIn, 1, 1));
        list.add(new ItemStack(itemIn, 1, 2));
    }

    public static class TileEntityStructure extends TileEntity implements IControlReceiver, IGuiProvider, ILookOverlay {

        public String name = "";

        public int sizeX = 1;
        public int sizeY = 1;
        public int sizeZ = 1;

        public Set<BlockMeta> blacklist = new HashSet<>();

        @Override
        public void updateEntity() {
            if (!worldObj.isRemote && worldObj.getTotalWorldTime() % 10 == 0) {
                NBTTagCompound data = new NBTTagCompound();
                writeToNBT(data);
                NetworkHandler.instance.sendToAllAround(new NBTUpdatePacket(data, xCoord, yCoord, zCoord), new TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 64));
            }
        }

        public void saveStructure(EntityPlayer player) {
            if (name.isEmpty()) {
                player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Could not save: invalid name"));
                return;
            }

            if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
                player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Could not save: invalid dimensions"));
                return;
            }

            BlockMeta air = new BlockMeta(Blocks.air, 0);
            blacklist.add(air);

            File file = NBTStructure.quickSaveArea(name + ".nbt", worldObj, xCoord, yCoord + 1, zCoord, xCoord + sizeX - 1, yCoord + sizeY, zCoord + sizeZ - 1, blacklist);

            blacklist.remove(air);

            if (file == null) {
                player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Failed to save structure"));
                return;
            }

            ChatComponentText fileText = new ChatComponentText(file.getName());
            fileText.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getParentFile().getAbsolutePath()));
            fileText.getChatStyle().setUnderlined(true);

            player.addChatMessage(new ChatComponentText("Saved structure as ").appendSibling(fileText));
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            super.readFromNBT(nbt);

            name = nbt.getString("name");

            sizeX = nbt.getInteger("sizeX");
            sizeY = nbt.getInteger("sizeY");
            sizeZ = nbt.getInteger("sizeZ");

            int[] blocks = nbt.getIntArray("blocks");
            int[] metas = nbt.getIntArray("metas");

            blacklist = new HashSet<>();
            for (int i = 0; i < blocks.length; i++) {
                blacklist.add(new BlockMeta(Block.getBlockById(blocks[i]), metas[i]));
            }
        }

        @Override
        public void writeToNBT(NBTTagCompound nbt) {
            super.writeToNBT(nbt);

            nbt.setString("name", name);

            nbt.setInteger("sizeX", sizeX);
            nbt.setInteger("sizeY", sizeY);
            nbt.setInteger("sizeZ", sizeZ);

            nbt.setIntArray("blocks", blacklist.stream().mapToInt(b -> Block.getIdFromBlock(b.block)).toArray());
            nbt.setIntArray("metas", blacklist.stream().mapToInt(b -> b.meta).toArray());
        }

        @Override
        public boolean hasPermission(EntityPlayer player) {
            return true;
        }

        @Override
        public void receiveControl(EntityPlayer player, NBTTagCompound nbt) {
            readFromNBT(nbt);
            markDirty();

            if (nbt.getBoolean("save")) {
                saveStructure(player);
            }
        }

        @Override
        public Object provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
            return null;
        }

        @Override
        public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
            return new GuiStructure(this);
        }

        @Override
        public List<String> printOverlay() {
            List<String> text = new ArrayList<String>();

            text.add(EnumChatFormatting.GRAY + "Name: " + EnumChatFormatting.RESET + name);

            text.add(EnumChatFormatting.GRAY + "Blacklist:");
            for (BlockMeta bm : blacklist) {
                text.add(EnumChatFormatting.RED + "- " + bm.block.getUnlocalizedName() + " : " + bm.meta);
            }

            return text;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public AxisAlignedBB getRenderBoundingBox() {
            return INFINITE_EXTENT_AABB;
        }

    }

    public static class GuiStructure extends GuiScreen {

        private final TileEntityStructure tile;

        private GuiTextField textName;

        private GuiTextField textSizeX;
        private GuiTextField textSizeY;
        private GuiTextField textSizeZ;

        private GuiButton performAction;

        private boolean saveOnClose = false;

        public GuiStructure(TileEntityStructure tile) {
            this.tile = tile;
        }

        @Override
        public void initGui() {
            Keyboard.enableRepeatEvents(true);

            textName = new GuiTextField(fontRendererObj, width / 2 - 150, 50, 300, 20);
            textName.setText(tile.name);

            textSizeX = new GuiTextField(fontRendererObj, width / 2 - 150, 100, 50, 20);
            textSizeX.setText("" + tile.sizeX);
            textSizeY = new GuiTextField(fontRendererObj, width / 2 - 100, 100, 50, 20);
            textSizeY.setText("" + tile.sizeY);
            textSizeZ = new GuiTextField(fontRendererObj, width / 2 - 50, 100, 50, 20);
            textSizeZ.setText("" + tile.sizeZ);

            performAction = new GuiButton(0, width / 2 - 150, 150, 300, 20, "SAVE");
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();

            textName.drawTextBox();

            textSizeX.drawTextBox();
            textSizeY.drawTextBox();
            textSizeZ.drawTextBox();

            performAction.drawButton(mc, mouseX, mouseY);

            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        public void onGuiClosed() {
            Keyboard.enableRepeatEvents(false);

            NBTTagCompound data = new NBTTagCompound();
            tile.writeToNBT(data);

            data.setString("name", textName.getText());

            try { data.setInteger("sizeX", Integer.parseInt(textSizeX.getText())); } catch (Exception ex) {}
            try { data.setInteger("sizeY", Integer.parseInt(textSizeY.getText())); } catch (Exception ex) {}
            try { data.setInteger("sizeZ", Integer.parseInt(textSizeZ.getText())); } catch (Exception ex) {}

            if (saveOnClose) data.setBoolean("save", true);

            tile.readFromNBT(data);

            NetworkHandler.instance.sendToServer(new NBTUpdatePacket(data, tile.xCoord, tile.yCoord, tile.zCoord));
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) {
            super.keyTyped(typedChar, keyCode);

            textName.textboxKeyTyped(typedChar, keyCode);

            textSizeX.textboxKeyTyped(typedChar, keyCode);
            textSizeY.textboxKeyTyped(typedChar, keyCode);
            textSizeZ.textboxKeyTyped(typedChar, keyCode);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            textName.mouseClicked(mouseX, mouseY, mouseButton);

            textSizeX.mouseClicked(mouseX, mouseY, mouseButton);
            textSizeY.mouseClicked(mouseX, mouseY, mouseButton);
            textSizeZ.mouseClicked(mouseX, mouseY, mouseButton);

            if (performAction.mousePressed(mc, mouseX, mouseY)) {
                saveOnClose = true;

                mc.displayGuiScreen(null);
                mc.setIngameFocus();
            }
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

    }

}
