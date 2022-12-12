package com.github.lunatrius.schematica.handler.client;

import ca.weblite.objc.Client;

import com.github.lunatrius.core.reference.Reference;
import com.github.lunatrius.schematica.client.gui.control.GuiSchematicControl;
import com.github.lunatrius.schematica.client.gui.load.GuiSchematicLoad;
import com.github.lunatrius.schematica.client.gui.save.GuiSchematicSave;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.renderer.RenderSchematic;
import com.github.lunatrius.schematica.client.util.BlockStateToItemStack;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.client.world.SchematicWorld.LayerMode;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Names;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

import javax.management.remote.rmi._RMIConnection_Stub;

public class InputHandler {
    public static final InputHandler INSTANCE = new InputHandler();

//    private static final KeyBinding KEY_BINDING_LOAD = new KeyBinding(Names.Keys.LOAD, Keyboard.KEY_DIVIDE, Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_SAVE = new KeyBinding(Names.Keys.SAVE, Keyboard.KEY_MULTIPLY, Names.Keys.CATEGORY);
//    private static final KeyBinding KEY_BINDING_CONTROL = new KeyBinding(Names.Keys.CONTROL, Keyboard.KEY_SUBTRACT, Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_LAYER_INC = new KeyBinding(Names.Keys.LAYER_INC, Keyboard.KEY_NONE, Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_LAYER_DEC = new KeyBinding(Names.Keys.LAYER_DEC, Keyboard.KEY_NONE, Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_LAYER_TOGGLE = new KeyBinding(Names.Keys.LAYER_TOGGLE, Keyboard.KEY_NONE, Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_RENDER_TOGGLE = new KeyBinding(Names.Keys.RENDER_TOGGLE, Keyboard.KEY_NONE, Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_PRINTER_TOGGLE = new KeyBinding(Names.Keys.PRINTER_TOGGLE, Keyboard.KEY_NONE, Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_MOVE_HERE = new KeyBinding(Names.Keys.MOVE_HERE, Keyboard.KEY_NONE, Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_PICK_BLOCK = new KeyBinding(Names.Keys.PICK_BLOCK, KeyConflictContext.IN_GAME, KeyModifier.SHIFT, -98, Names.Keys.CATEGORY);
    private static final  KeyBinding KEY_BINDING_LOAD_MANIPULATE = new KeyBinding(Names.Keys.LOAD_MANIPULATE, Keyboard.KEY_DIVIDE, Names.Keys.CATEGORY);
    private static final  KeyBinding KEY_BINDING_VIEW_ERRORS = new KeyBinding(Names.Keys.VIEW_ERRORS, Keyboard.KEY_LCONTROL, Names.Keys.CATEGORY);
    public static final KeyBinding[] KEY_BINDINGS = new KeyBinding[] {
            //KEY_BINDING_LOAD,
            KEY_BINDING_SAVE,
            //KEY_BINDING_CONTROL,
            KEY_BINDING_LAYER_INC,
            KEY_BINDING_LAYER_DEC,
            KEY_BINDING_LAYER_TOGGLE,
            KEY_BINDING_RENDER_TOGGLE,
            KEY_BINDING_PRINTER_TOGGLE,
            KEY_BINDING_MOVE_HERE,
            KEY_BINDING_PICK_BLOCK,
            KEY_BINDING_LOAD_MANIPULATE,
            KEY_BINDING_VIEW_ERRORS
    };

    private final Minecraft minecraft = Minecraft.getMinecraft();

    private InputHandler() {}

    @SubscribeEvent
    public void onKeyInput(final InputEvent event) {
        if (this.minecraft.currentScreen == null) {

            /*if (KEY_BINDING_LOAD.isPressed()) {
                this.minecraft.displayGuiScreen(new GuiSchematicLoad(this.minecraft.currentScreen));
            }*/

            if (KEY_BINDING_SAVE.isPressed()) {
                this.minecraft.displayGuiScreen(new GuiSchematicSave(this.minecraft.currentScreen));
            }

            /*if (KEY_BINDING_CONTROL.isPressed()) {
                this.minecraft.displayGuiScreen(new GuiSchematicControl(this.minecraft.currentScreen));
            }*/

            if (KEY_BINDING_LOAD_MANIPULATE.isPressed()) {
                if (ClientProxy.schematic == null) {
                    this.minecraft.displayGuiScreen(new GuiSchematicLoad(this.minecraft.currentScreen));
                } else {
                    this.minecraft.displayGuiScreen(new GuiSchematicControl(this.minecraft.currentScreen));
                }
            }

            if (KEY_BINDING_LAYER_INC.isPressed()) {
                final SchematicWorld schematic = ClientProxy.schematic;
                if (schematic != null && schematic.layerMode != LayerMode.ALL) {
                    schematic.renderingLayer = MathHelper.clamp(schematic.renderingLayer + 1, 0, schematic.getHeight() - 1);
                    RenderSchematic.INSTANCE.refresh();
                }
            }

            if (KEY_BINDING_LAYER_DEC.isPressed()) {
                final SchematicWorld schematic = ClientProxy.schematic;
                if (schematic != null && schematic.layerMode != LayerMode.ALL) {
                    schematic.renderingLayer = MathHelper.clamp(schematic.renderingLayer - 1, 0, schematic.getHeight() - 1);
                    RenderSchematic.INSTANCE.refresh();
                }
            }

            if (KEY_BINDING_LAYER_TOGGLE.isPressed()) {
                final SchematicWorld schematic = ClientProxy.schematic;
                if (schematic != null) {
                    schematic.layerMode = LayerMode.next(schematic.layerMode);
                    RenderSchematic.INSTANCE.refresh();
                }
            }

            if (KEY_BINDING_RENDER_TOGGLE.isPressed()) {
                final SchematicWorld schematic = ClientProxy.schematic;
                if (schematic != null) {
                    schematic.isRendering = !schematic.isRendering;
                    RenderSchematic.INSTANCE.refresh();
                }
            }

            if (KEY_BINDING_PRINTER_TOGGLE.isPressed()) {
                if (ClientProxy.schematic != null) {
                    final boolean printing = SchematicPrinter.INSTANCE.togglePrinting();
                    this.minecraft.player.sendMessage(new TextComponentTranslation(Names.Messages.TOGGLE_PRINTER, I18n.format(printing ? Names.Gui.ON : Names.Gui.OFF)));
                }
            }

            if (KEY_BINDING_MOVE_HERE.isPressed()) {
                final SchematicWorld schematic = ClientProxy.schematic;
                if (schematic != null) {
                    ClientProxy.moveSchematicToPlayer(schematic);
                    RenderSchematic.INSTANCE.refresh();
                }
            }

            if (KEY_BINDING_PICK_BLOCK.isPressed()) {
                final SchematicWorld schematic = ClientProxy.schematic;
                if (schematic != null && schematic.isRendering) {
                    pickBlock(schematic, ClientProxy.objectMouseOver);
                }
            }

            if (KEY_BINDING_VIEW_ERRORS.isPressed()) {
                if (ClientProxy.viewingErrors) {
                    ClientProxy.viewingErrors = false;
                } else {
                    ClientProxy.viewingErrors = true;
                }
            }
            if (!KEY_BINDING_VIEW_ERRORS.isKeyDown() && !ClientProxy.viewErrorToggle) {
                ClientProxy.viewingErrors = false;
            }
        }
    }

    private boolean canPickFromInventory(final InventoryPlayer inventory, final ItemStack itemStack) {
        for (int i = 0; i < inventory.mainInventory.size(); i++) {
            ItemStack item = inventory.mainInventory.get(i);
            if (item.isItemEqual(itemStack)) {
                return true;
            }
        }
        return false;
    }

    private int countItemsInShulker(NBTTagCompound tag, ItemStack itemStack) {
        if (tag == null) return 0;
        NBTTagCompound tag2 = tag.getCompoundTag("BlockEntityTag");
        if (tag2 == null) return 0;

        NBTTagList list = tag2.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        if (list == null) return 0;

        int foundCount = 0;
        for (NBTBase shulkerItem : list) {
            if (shulkerItem instanceof NBTTagCompound) {
                String shulkerItemName = ((NBTTagCompound) shulkerItem).getString("id");
                int shulkerItemDamage = ((NBTTagCompound) shulkerItem).getInteger("Damage");
                int shulkerItemCount = ((NBTTagCompound) shulkerItem).getByte("Count");
                ResourceLocation registryEntry = itemStack.getItem().getRegistryName();
                if (registryEntry == null) continue;
                String registryName = registryEntry.toString();
                try {
                    Reference.logger.info("Damage " + shulkerItemDamage + " " + shulkerItemName + " " + itemStack.getMetadata());
                    if (shulkerItemName.indexOf(registryName) != -1 && itemStack.getMetadata() == shulkerItemDamage) {
                        foundCount += shulkerItemCount;
                    }
                } catch (Exception e) {
                    Reference.logger.error("Oh no", e);
                }
            }
        }

        return foundCount;
    }

    /**
     * Find the shulker box with the most items off one kind in it
     * @param inventory
     * @param itemStack
     * @return
     */
    private boolean shulkerContainsItemStack(final InventoryPlayer inventory, final ItemStack itemStack) {
        boolean foundShulker = false;
        int lowestCount = Integer.MAX_VALUE;
        int bestShulkerSlot = 0;
        for (int i = 0; i < inventory.mainInventory.size(); i++) {
            ItemStack item = inventory.mainInventory.get(i);
            if (item.getItem() instanceof ItemShulkerBox) {
                NBTTagCompound tag = item.getTagCompound();

                int count = countItemsInShulker(tag, itemStack);
                if (count != 0 && count < lowestCount) {
                    foundShulker = true;
                    bestShulkerSlot = i;
                    lowestCount = count;
                }
            }
        }
        
        Reference.logger.info("Picking block " + itemStack.toString());
        if (foundShulker) {
            if (bestShulkerSlot >= 0 && bestShulkerSlot < 9) {
                inventory.currentItem = bestShulkerSlot;
            } else if (bestShulkerSlot >= 9 && bestShulkerSlot < 9 + 27) {
                this.minecraft.playerController.pickItem(bestShulkerSlot);
            }
            return true;
        }
        
        return false;
    }

    private boolean pickBlock(final SchematicWorld schematic, final RayTraceResult objectMouseOver) {
        // Minecraft.func_147112_ai
        if (objectMouseOver == null) {
            return false;
        }

        if (objectMouseOver.typeOfHit == RayTraceResult.Type.MISS) {
            return false;
        }

        final EntityPlayerSP player = this.minecraft.player;
        
        BlockPos pos = objectMouseOver.getBlockPos();
        final IBlockState blockState = schematic.getBlockState(pos);
        final ItemStack itemStack = BlockStateToItemStack.getItemStack(blockState, objectMouseOver, schematic, pos, player);
        if (!canPickFromInventory(player.inventory, itemStack)) {
            if (!itemStack.isEmpty()) {
                if (shulkerContainsItemStack(player.inventory, itemStack)) {
                    return true;
                }
            }
        }

        if (!ForgeHooks.onPickBlock(objectMouseOver, player, schematic)) {
            return true;
        }

        if (player.capabilities.isCreativeMode) {
            final int slot = player.inventoryContainer.inventorySlots.size() - 10 + player.inventory.currentItem;
            this.minecraft.playerController.sendSlotPacket(player.inventory.getStackInSlot(player.inventory.currentItem), slot);
            return true;
        }

        return false;
    }
}
