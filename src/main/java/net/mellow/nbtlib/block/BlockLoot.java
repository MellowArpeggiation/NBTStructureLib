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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.World;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.util.ForgeDirection;

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
        ((TileEntityLoot) te).updateLootBlock(player, Blocks.chest, 0, itemStack);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(x, y, z);

        if (!(te instanceof TileEntityLoot)) return false;

        TileEntityLoot loot = (TileEntityLoot) te;

        if (!player.isSneaking()) {
            ItemStack held = player.getHeldItem();
            if (held != null && held.getItem() == ModItems.structure_wand) return false;

            Block block = getLootableBlock(world, held);

            if (block != null) {
                loot.updateLootBlock(player, block, held.getItemDamage(), held);
            } else {
                if (world.isRemote) FMLNetworkHandler.openGui(player, Registry.instance, 0, world, x, y, z);
            }

            return true;
        }

        return false;
    }

    private Block getLootableBlock(World world, ItemStack stack) {
        Block block = ModBlocks.getBlockFromStack(stack);
        if (block == null || !(block instanceof ITileEntityProvider)) return null;

        int meta = stack.getItemDamage();
        TileEntity te = ((ITileEntityProvider) block).createNewTileEntity(world, meta);

        if (te instanceof IInventory) return block;

        return null;
    }

    public static class TileEntityLoot extends TileEntity implements IControlReceiver, IGuiProvider, ILookOverlay, INBTTileEntityTransformable {

        private String lootCategory = ChestGenHooks.DUNGEON_CHEST;
        private Block replaceBlock = Blocks.chest;
        private int replaceMeta;

        private int minItems;
        private int maxItems = 1;

        private int[] rotMetas = { 0, 0, 0, 0 };

        @Override
        public void updateEntity() {
            if (!worldObj.isRemote) {
                NBTTagCompound data = new NBTTagCompound();
                writeToNBT(data);
                NetworkHandler.instance.sendToAllAround(new NBTUpdatePacket(data, xCoord, yCoord, zCoord), new TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 16));
            }
        }

        // Tries to figure out the rotation metas for this loot block
        public void updateLootBlock(EntityLivingBase placer, Block block, int meta, ItemStack stack) {
            if (worldObj.isRemote) return;

            replaceBlock = block;
            replaceMeta = meta;

            ForgeDirection dir = ForgeDirection.getOrientation(worldObj.getBlockMetadata(xCoord, yCoord, zCoord));

            float placedYaw = getYaw(dir);
            float placerYaw = placer.rotationYaw;
            float placerYawHead = placer.rotationYawHead;

            Block wasBlock = worldObj.getBlock(xCoord, yCoord + 1, zCoord);
            int wasMeta = worldObj.getBlockMetadata(xCoord, yCoord + 1, zCoord);
            TileEntity wasTe = worldObj.getTileEntity(xCoord, yCoord + 1, zCoord);

            worldObj.removeTileEntity(xCoord, yCoord + 1, zCoord);
            worldObj.setBlock(xCoord, yCoord + 1, zCoord, replaceBlock, replaceMeta, 2);

            for (int i = 0; i < 4; i++) {
                placer.rotationYaw = placer.rotationYawHead = MathHelper.wrapAngleTo180_float(placedYaw + i * 90);
                replaceBlock.onBlockPlacedBy(worldObj, xCoord, yCoord + 1, zCoord, placer, stack);

                rotMetas[i] = worldObj.getBlockMetadata(xCoord, yCoord + 1, zCoord);
            }

            worldObj.setBlock(xCoord, yCoord + 1, zCoord, wasBlock, wasMeta, 2);

            if (wasTe != null) {
                wasTe.validate(); // put that back
                worldObj.setTileEntity(xCoord, yCoord + 1, zCoord, wasTe);
            }

            placer.rotationYaw = placerYaw;
            placer.rotationYawHead = placerYawHead;
        }

        private float getYaw(ForgeDirection dir) {
            switch (dir) {
                case NORTH: return 0;
                case SOUTH: return 180;
                case EAST: return 90;
                case WEST: return -90;
                default: return 0;
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
            nbt.setIntArray("rotMetas", rotMetas);
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            super.readFromNBT(nbt);
            replaceBlock = Block.getBlockFromName(nbt.getString("block"));
            replaceMeta = nbt.getInteger("meta");
            minItems = nbt.getInteger("min");
            maxItems = nbt.getInteger("max");
            lootCategory = nbt.getString("category");
            if (nbt.hasKey("rotMetas")) rotMetas = nbt.getIntArray("rotMetas");

            if (replaceBlock == null) replaceBlock = Blocks.chest;
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
        public TileEntity transformTE(World world, int coordBaseMode) {
            TileEntity te = replaceBlock.createTileEntity(world, replaceMeta);
            te.blockType = replaceBlock;
            te.blockMetadata = rotMetas[coordBaseMode];

            if (te instanceof IInventory) {
                WeightedRandomChestContent[] pool = ChestGenHooks.getItems(lootCategory, world.rand);
                int count = minItems;
                if (maxItems - minItems > 0) count += world.rand.nextInt(maxItems - minItems);
                count = (int)Math.floor(count);
                WeightedRandomChestContent.generateChestContents(world.rand, pool, (IInventory) te, count);
            }

            return te;
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

            loot.readFromNBT(data);

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
