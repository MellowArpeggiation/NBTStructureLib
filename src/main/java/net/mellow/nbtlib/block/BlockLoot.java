package net.mellow.nbtlib.block;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.lwjgl.input.Keyboard;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mellow.nbtlib.Config;
import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.api.INBTTileEntityTransformable;
import net.mellow.nbtlib.gui.IGuiProvider;
import net.mellow.nbtlib.gui.ILookOverlay;
import net.mellow.nbtlib.item.ModItems;
import net.mellow.nbtlib.network.IControlReceiver;
import net.mellow.nbtlib.network.NBTUpdatePacket;
import net.mellow.nbtlib.network.NetworkHandler;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

public class BlockLoot extends BlockSideRotation {

    protected BlockLoot() {
        super(Material.iron);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityLoot();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon(Registry.MODID + ":structure_loot");
        iconTop = iconRegister.registerIcon(Registry.MODID + ":structure_loot_top");
        iconSide = iconRegister.registerIcon(Registry.MODID + ":structure_loot_back");
        iconBack = iconRegister.registerIcon(Registry.MODID + ":structure_loot_back");
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack itemStack) {
        int i = MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;

        if (i == 0) world.setBlockMetadataWithNotify(x, y, z, 2, 2);
        if (i == 1) world.setBlockMetadataWithNotify(x, y, z, 5, 2);
        if (i == 2) world.setBlockMetadataWithNotify(x, y, z, 3, 2);
        if (i == 3) world.setBlockMetadataWithNotify(x, y, z, 4, 2);

        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityLoot)) return;
        ((TileEntityLoot) te).placedRotation = player.rotationYaw;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(x, y, z);

        if (!(te instanceof TileEntityLoot)) return false;

        TileEntityLoot loot = (TileEntityLoot) te;

        if (!player.isSneaking()) {
            if (player.getHeldItem() != null && player.getHeldItem().getItem() == ModItems.structure_wand) return false;

            Block block = getLootableBlock(world, player.getHeldItem());

            if (block != null) {
                loot.replaceBlock = block;
                loot.replaceMeta = player.getHeldItem().getItemDamage();
            } else {
                if (world.isRemote) FMLNetworkHandler.openGui(player, Registry.instance, 0, world, x, y, z);
            }

            return true;
        }

        return false;
    }

    private Block getLootableBlock(World world, ItemStack stack) {
        if (stack == null) return null;

        if (stack.getItem() instanceof ItemBlock) {
            Block block = ((ItemBlock) stack.getItem()).field_150939_a;
            int meta = stack.getItemDamage();

            if (block instanceof ITileEntityProvider) {
                TileEntity te = ((ITileEntityProvider) block).createNewTileEntity(world, meta);
                if (te instanceof IInventory) return block;
            }
        }

        return null;
    }

    public static class TileEntityLoot extends TileEntity implements IControlReceiver, IGuiProvider, ILookOverlay, INBTTileEntityTransformable {

        private boolean triggerReplace;

        private String lootCategory = ChestGenHooks.DUNGEON_CHEST;
        private Block replaceBlock = Blocks.chest;
        private int replaceMeta;

        private int minItems;
        private int maxItems = 1;

        private float placedRotation;

        private static final GameProfile FAKE_PROFILE = new GameProfile(UUID.fromString("839eb18c-50bc-400c-8291-9383f09763e7"), "[nbtlib]");
        private static FakePlayer fakePlayer;

        @Override
        public void updateEntity() {
            if (!worldObj.isRemote) {
                if (triggerReplace) {
                    // On the first tick of this TE, replace with intended block and fill with loot
                    replace();
                } else {
                    NBTTagCompound data = new NBTTagCompound();
                    writeToNBT(data);
                    NetworkHandler.instance.sendToAllAround(new NBTUpdatePacket(data, xCoord, yCoord, zCoord), new TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 16));
                }
            }
        }

        private void replace() {
            if (!(worldObj.getBlock(xCoord, yCoord, zCoord) instanceof BlockLoot)) {
                Registry.LOG.warn("Somehow the block at: " + xCoord + ", " + yCoord + ", " + zCoord + " isn't a loot block but we're doing a TE update as if it is, cancelling!");
                return;
            }

            WeightedRandomChestContent[] pool = ChestGenHooks.getItems(lootCategory, worldObj.rand);

            worldObj.setBlock(xCoord, yCoord, zCoord, replaceBlock, replaceMeta, 2);

            TileEntity te = worldObj.getTileEntity(xCoord, yCoord, zCoord);

            if (te == null || te instanceof TileEntityLoot) {
                // Some generator has broken the TE->block relationship, which, honestly, rude.
                // so we're just gonna hop in and force update the TE

                Registry.LOG.warn("TE set incorrectly at: " + xCoord + ", " + yCoord + ", " + zCoord + ". If you're using some sort of world generation mod, report it to the author!");

                te = replaceBlock.createTileEntity(worldObj, replaceMeta);
                worldObj.setTileEntity(xCoord, yCoord, zCoord, te);
            }

            if (te instanceof IInventory) {
                int count = minItems;
                if (maxItems - minItems > 0) count += worldObj.rand.nextInt(maxItems - minItems);
                count = (int)Math.floor(count);
                WeightedRandomChestContent.generateChestContents(worldObj.rand, pool, (IInventory) te, count);
            }

            // Shouldn't happen but let's guard anyway, if it fails we just don't rotate the chest block correctly
            if (!(worldObj instanceof WorldServer)) return;

            try {
                if (fakePlayer == null || fakePlayer.worldObj != worldObj) {
                    fakePlayer = FakePlayerFactory.get((WorldServer)worldObj, FAKE_PROFILE);
                }

                fakePlayer.rotationYaw = fakePlayer.rotationYawHead = placedRotation;

                ItemStack fakeStack = new ItemStack(replaceBlock, 1, replaceMeta);

                replaceBlock.onBlockPlacedBy(worldObj, xCoord, yCoord, zCoord, fakePlayer, fakeStack);
            } catch(Exception ex) {
                Registry.LOG.warn("Failed to correctly rotate loot block at: " + xCoord + ", " + yCoord + ", " + zCoord);
                Registry.LOG.catching(ex);
            }
        }

        @Override
        public void writeToNBT(NBTTagCompound nbt) {
            super.writeToNBT(nbt);
            Block writeBlock = replaceBlock == null ? Blocks.chest : replaceBlock;
            nbt.setString("block", GameRegistry.findUniqueIdentifierFor(writeBlock).toString());
            nbt.setInteger("meta", replaceMeta);
            nbt.setInteger("min", minItems);
            nbt.setInteger("max", maxItems);
            nbt.setString("category", lootCategory);
            nbt.setFloat("rot", placedRotation);

            nbt.setBoolean("trigger", triggerReplace);
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            super.readFromNBT(nbt);
            replaceBlock = Block.getBlockFromName(nbt.getString("block"));
            replaceMeta = nbt.getInteger("meta");
            minItems = nbt.getInteger("min");
            maxItems = nbt.getInteger("max");
            lootCategory = nbt.getString("category");
            placedRotation = nbt.getFloat("rot");

            if (replaceBlock == null) replaceBlock = Blocks.chest;

            triggerReplace = nbt.getBoolean("trigger");
        }

        @Override
        public List<String> printOverlay() {
            List<String> text = new ArrayList<String>();

            text.add("Will replace with: " + replaceBlock.getUnlocalizedName());
            text.add("   meta: " + replaceMeta);
            text.add("Loot category: " + lootCategory);
            text.add("Minimum items: " + minItems);
            text.add("Maximum items: " + maxItems);

            return text;
        }

        @Override
        public Object provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
            return null;
        }

        @Override
        public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
            return new GuiLoot(this);
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
        public void transformTE(World world, int coordBaseMode) {
            triggerReplace = !Config.debugStructures;
            placedRotation = MathHelper.wrapAngleTo180_float(placedRotation + coordBaseMode * 90);
        }

    }

    public static class GuiLoot extends GuiScreen {

        private final TileEntityLoot loot;

        private GuiTextField textCategory;

        private GuiTextField textMinItems;
        private GuiTextField textMaxItems;

        public GuiLoot(TileEntityLoot jigsaw) {
            this.loot = jigsaw;
        }

        @Override
        public void initGui() {
            Keyboard.enableRepeatEvents(true);

            textCategory = new GuiTextField(fontRendererObj, this.width / 2 - 150, 50, 300, 20);
            textCategory.setText(loot.lootCategory);

            textMinItems = new GuiTextField(fontRendererObj, this.width / 2 - 150, 100, 140, 20);
            textMinItems.setText("" + loot.minItems);

            textMaxItems = new GuiTextField(fontRendererObj, this.width / 2 + 10, 100, 140, 20);
            textMaxItems.setText("" + loot.maxItems);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();

            drawString(fontRendererObj, "Loot category:", this.width / 2 - 150, 37, 0xA0A0A0);
            textCategory.drawTextBox();

            drawString(fontRendererObj, "Minimum items:", this.width / 2 - 150, 87, 0xA0A0A0);
            textMinItems.drawTextBox();

            drawString(fontRendererObj, "Maximum items:", this.width / 2 + 10, 87, 0xA0A0A0);
            textMaxItems.drawTextBox();

            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        public void onGuiClosed() {
            Keyboard.enableRepeatEvents(false);

            NBTTagCompound data = new NBTTagCompound();
            loot.writeToNBT(data);

            data.setString("category", textCategory.getText());

            try { data.setInteger("min", Integer.parseInt(textMinItems.getText())); } catch (Exception ex) {}
            try { data.setInteger("max", Integer.parseInt(textMaxItems.getText())); } catch (Exception ex) {}

            NetworkHandler.instance.sendToServer(new NBTUpdatePacket(data, loot.xCoord, loot.yCoord, loot.zCoord));
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) {
            super.keyTyped(typedChar, keyCode);
            textCategory.textboxKeyTyped(typedChar, keyCode);
            textMinItems.textboxKeyTyped(typedChar, keyCode);
            textMaxItems.textboxKeyTyped(typedChar, keyCode);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            textCategory.mouseClicked(mouseX, mouseY, mouseButton);
            textMinItems.mouseClicked(mouseX, mouseY, mouseButton);
            textMaxItems.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

    }

}
