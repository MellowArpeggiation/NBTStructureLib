package net.mellow.nbtlib.block;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.gui.IGuiProvider;
import net.mellow.nbtlib.gui.ILookOverlay;
import net.mellow.nbtlib.item.ModItems;
import net.mellow.nbtlib.network.IControlReceiver;
import net.mellow.nbtlib.network.NBTUpdatePacket;
import net.mellow.nbtlib.network.NetworkHandler;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class BlockJigsaw extends BlockSideRotation {

    protected BlockJigsaw() {
        super(Material.iron);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityJigsaw();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon(Registry.MODID + ":structure_jigsaw");
        iconTop = iconRegister.registerIcon(Registry.MODID + ":structure_jigsaw_top");
        iconSide = iconRegister.registerIcon(Registry.MODID + ":structure_jigsaw_side");
        iconBack = iconRegister.registerIcon(Registry.MODID + ":structure_jigsaw_back");
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(x, y, z);

        if (!(te instanceof TileEntityJigsaw)) return false;

        TileEntityJigsaw jigsaw = (TileEntityJigsaw) te;

        if (!player.isSneaking()) {
            Block block = getBlock(world, player.getHeldItem());
            if (block == ModBlocks.structure_air) block = Blocks.air;

            if (block != null && block != ModBlocks.structure_jigsaw && block != ModBlocks.structure_loot) {
                jigsaw.replaceBlock = block;
                jigsaw.replaceMeta = player.getHeldItem().getItemDamage();
                jigsaw.markDirty();

                return true;
            }

            if (player.getHeldItem() != null && player.getHeldItem().getItem() == ModItems.structure_wand) return false;

            if (world.isRemote) FMLNetworkHandler.openGui(player, Registry.instance, 0, world, x, y, z);

            return true;
        }

        return false;
    }

    public static class TileEntityJigsaw extends TileEntity implements IControlReceiver, IGuiProvider, ILookOverlay {

        private int selectionPriority = 0; // higher priority = this jigsaw block is selected first for generation
        private int placementPriority = 0; // higher priority = children of this jigsaw block are checked for jigsaw blocks of their own and selected first
        private String pool = "default";
        private String name = "default";
        private String target = "default";
        private Block replaceBlock = Blocks.air;
        private int replaceMeta = 0;
        private boolean isRollable = true; // sets joint type, rollable joints can be placed in any orientation for vertical jigsaw connections

        @Override
        public void updateEntity() {
            if (!worldObj.isRemote && worldObj.getTotalWorldTime() % 10 == 0) {
                NBTTagCompound data = new NBTTagCompound();
                writeToNBT(data);
                NetworkHandler.instance.sendToAllAround(new NBTUpdatePacket(data, xCoord, yCoord, zCoord), new TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 16));
            }
        }

        @Override
        public void writeToNBT(NBTTagCompound nbt) {
            super.writeToNBT(nbt);
            nbt.setInteger("direction", this.getBlockMetadata());

            nbt.setInteger("selection", selectionPriority);
            nbt.setInteger("placement", placementPriority);
            nbt.setString("pool", pool);
            nbt.setString("name", name);
            nbt.setString("target", target);
            nbt.setString("block", GameRegistry.findUniqueIdentifierFor(replaceBlock).toString());
            nbt.setInteger("meta", replaceMeta);
            nbt.setBoolean("roll", isRollable);
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            super.readFromNBT(nbt);

            selectionPriority = nbt.getInteger("selection");
            placementPriority = nbt.getInteger("placement");
            pool = nbt.getString("pool");
            name = nbt.getString("name");
            target = nbt.getString("target");
            replaceBlock = Block.getBlockFromName(nbt.getString("block"));
            replaceMeta = nbt.getInteger("meta");
            isRollable = nbt.getBoolean("roll");
        }

        @Override
        public boolean hasPermission(EntityPlayer player) {
            return true;
        }

        @Override
        public void receiveControl(EntityPlayer player, NBTTagCompound nbt) {
            readFromNBT(nbt);
            markDirty();
        }

        @Override
        public Object provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
            return null;
        }

        @Override
        public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
            return new GuiJigsaw(this);
        }

        @Override
        public List<String> printOverlay() {
            List<String> text = new ArrayList<String>();

            text.add(EnumChatFormatting.GRAY + "Target pool: " + EnumChatFormatting.RESET + pool);
            text.add(EnumChatFormatting.GRAY + "Name: " + EnumChatFormatting.RESET + name);
            text.add(EnumChatFormatting.GRAY + "Target name: " + EnumChatFormatting.RESET + target);
            text.add(EnumChatFormatting.GRAY + "Turns into: " + EnumChatFormatting.RESET + GameRegistry.findUniqueIdentifierFor(replaceBlock).toString());
            text.add(EnumChatFormatting.GRAY + "   with meta: " + EnumChatFormatting.RESET + replaceMeta);
            text.add(EnumChatFormatting.GRAY + "Selection/Placement priority: " + EnumChatFormatting.RESET + selectionPriority + "/" + placementPriority);
            text.add(EnumChatFormatting.GRAY + "Joint type: " + EnumChatFormatting.RESET + (isRollable ? "Rollable" : "Aligned"));

            return text;
        }

    }

    public static class GuiJigsaw extends GuiScreen {

        private final TileEntityJigsaw jigsaw;

        private GuiTextField textPool;
        private GuiTextField textName;
        private GuiTextField textTarget;

        private GuiTextField textSelectionPriority;
        private GuiTextField textPlacementPriority;

        private GuiButton jointToggle;

        public GuiJigsaw(TileEntityJigsaw jigsaw) {
            this.jigsaw = jigsaw;
        }

        @Override
        public void initGui() {
            Keyboard.enableRepeatEvents(true);

            textPool = new GuiTextField(fontRendererObj, this.width / 2 - 150, 50, 300, 20);
            textPool.setText(jigsaw.pool);

            textName = new GuiTextField(fontRendererObj, this.width / 2 - 150, 100, 140, 20);
            textName.setText(jigsaw.name);

            textTarget = new GuiTextField(fontRendererObj, this.width / 2 + 10, 100, 140, 20);
            textTarget.setText(jigsaw.target);

            textSelectionPriority = new GuiTextField(fontRendererObj, this.width / 2 - 150, 150, 90, 20);
            textSelectionPriority.setText("" + jigsaw.selectionPriority);

            textPlacementPriority = new GuiTextField(fontRendererObj, this.width / 2 - 40, 150, 90, 20);
            textPlacementPriority.setText("" + jigsaw.placementPriority);

            jointToggle = new GuiButton(0, this.width / 2 + 60, 150, 90, 20, jigsaw.isRollable ? "Rollable" : "Aligned");
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();

            drawString(fontRendererObj, "Target pool:", this.width / 2 - 150, 37, 0xA0A0A0);
            textPool.drawTextBox();

            drawString(fontRendererObj, "Name:", this.width / 2 - 150, 87, 0xA0A0A0);
            textName.drawTextBox();

            drawString(fontRendererObj, "Target name:", this.width / 2 + 10, 87, 0xA0A0A0);
            textTarget.drawTextBox();

            drawString(fontRendererObj, "Selection priority:", this.width / 2 - 150, 137, 0xA0A0A0);
            textSelectionPriority.drawTextBox();

            drawString(fontRendererObj, "Placement priority:", this.width / 2 - 40, 137, 0xA0A0A0);
            textPlacementPriority.drawTextBox();

            drawString(fontRendererObj, "Joint type:", this.width / 2 + 60, 137, 0xA0A0A0);
            jointToggle.drawButton(mc, mouseX, mouseY);

            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        public void onGuiClosed() {
            Keyboard.enableRepeatEvents(false);

            NBTTagCompound data = new NBTTagCompound();
            jigsaw.writeToNBT(data);

            data.setString("pool", textPool.getText());
            data.setString("name", textName.getText());
            data.setString("target", textTarget.getText());

            try { data.setInteger("selection", Integer.parseInt(textSelectionPriority.getText())); } catch (Exception ex) {}
            try { data.setInteger("placement", Integer.parseInt(textPlacementPriority.getText())); } catch (Exception ex) {}

            data.setBoolean("roll", jointToggle.displayString == "Rollable");

            NetworkHandler.instance.sendToServer(new NBTUpdatePacket(data, jigsaw.xCoord, jigsaw.yCoord, jigsaw.zCoord));
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) {
            super.keyTyped(typedChar, keyCode);
            textPool.textboxKeyTyped(typedChar, keyCode);
            textName.textboxKeyTyped(typedChar, keyCode);
            textTarget.textboxKeyTyped(typedChar, keyCode);
            textSelectionPriority.textboxKeyTyped(typedChar, keyCode);
            textPlacementPriority.textboxKeyTyped(typedChar, keyCode);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            textPool.mouseClicked(mouseX, mouseY, mouseButton);
            textName.mouseClicked(mouseX, mouseY, mouseButton);
            textTarget.mouseClicked(mouseX, mouseY, mouseButton);
            textSelectionPriority.mouseClicked(mouseX, mouseY, mouseButton);
            textPlacementPriority.mouseClicked(mouseX, mouseY, mouseButton);

            if (jointToggle.mousePressed(mc, mouseX, mouseY)) {
                System.out.println("displayString: " + jointToggle.displayString);
                jointToggle.displayString = jointToggle.displayString == "Rollable" ? "Aligned" : "Rollable";
            }
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

    }

    private static Block getBlock(World world, ItemStack stack) {
        if (stack == null) return null;
        if (!(stack.getItem() instanceof ItemBlock)) return null;

        return ((ItemBlock) stack.getItem()).field_150939_a;
    }

}
