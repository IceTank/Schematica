package com.github.lunatrius.schematica.client.printer;

import com.github.lunatrius.core.util.math.BlockPosHelper;
import com.github.lunatrius.core.util.math.MBlockPos;
import com.github.lunatrius.schematica.block.state.BlockStateHelper;
import com.github.lunatrius.schematica.client.printer.blocksynch.BlockSyncRegistry;
import com.github.lunatrius.schematica.client.printer.blocksynch.BlockSync;
import com.github.lunatrius.schematica.client.printer.nbtsync.NBTSync;
import com.github.lunatrius.schematica.client.printer.nbtsync.SyncRegistry;
import com.github.lunatrius.schematica.client.printer.registry.PlacementData;
import com.github.lunatrius.schematica.client.printer.registry.PlacementRegistry;
import com.github.lunatrius.schematica.client.util.BlockStateToItemStack;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Constants;
import com.github.lunatrius.schematica.reference.Reference;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiShulkerBox;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.*;

class NeedsWaitingException extends Exception {}

public class SchematicPrinter {
    public static final SchematicPrinter INSTANCE = new SchematicPrinter();

    private final Minecraft minecraft = Minecraft.getMinecraft();

    private boolean isEnabled = true;
    private boolean isPrinting = false;

    private SchematicWorld schematic = null;
    private byte[][][] timeout = null;
    private HashMap<BlockPos, Integer> syncBlacklist = new HashMap<BlockPos, Integer>();
    private List<Vec3d> rollingVel = new ArrayList<>();
    private int rollingPos = 0;
    private Vec3d averageVelocity = new Vec3d(0,0,0);

    public int itemSelectCoolDown = 0;

    public void onTick() {
        if (itemSelectCoolDown > 0) itemSelectCoolDown--;
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void setEnabled(final boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public boolean togglePrinting() {
        this.isPrinting = !this.isPrinting && this.schematic != null;
        return this.isPrinting;
    }

    public boolean isPrinting() {
        return this.isPrinting;
    }

    public void setPrinting(final boolean isPrinting) {
        this.isPrinting = isPrinting;
    }

    public SchematicWorld getSchematic() {
        return this.schematic;
    }

    public void setSchematic(final SchematicWorld schematic) {
        this.isPrinting = false;
        this.schematic = schematic;
        refresh();
    }

    public void refresh() {
        if (this.schematic != null) {
            this.timeout = new byte[this.schematic.getWidth()][this.schematic.getHeight()][this.schematic.getLength()];
        } else {
            this.timeout = null;
        }
        this.syncBlacklist.clear();
    }

    public boolean print(final WorldClient world, final EntityPlayerSP player) {
        if (ConfigurationHandler.disableWhileMoving) {
            Vec3d playerspeed = new Vec3d(player.motionX,player.motionY+.0784,player.motionZ);
            //printDebug(player.motionX + ", "+ player.motionY + ", " + player.motionZ);
            // printDebug(playerspeed.lengthVector()+"");
            if (playerspeed.lengthVector()>.001) {
                return false;
            }
        }

        final double dX = ClientProxy.playerPosition.x - this.schematic.position.x;
        final double dY = ClientProxy.playerPosition.y - this.schematic.position.y+player.getEyeHeight();
        final double dZ = ClientProxy.playerPosition.z - this.schematic.position.z;
        final int x = (int) Math.floor(dX);
        final int y = (int) Math.floor(dY);
        final int z = (int) Math.floor(dZ);
        final double range = ConfigurationHandler.placeDistance;
        final int rangeInt = (int) Math.ceil(range);
        final int minX = Math.max(0, x - rangeInt);
        final int maxX = Math.min(this.schematic.getWidth() - 1, x + rangeInt);
        int minY = Math.max(0, y - rangeInt);
        int maxY = Math.min(this.schematic.getHeight() - 1, y + rangeInt);
        final int minZ = Math.max(0, z - rangeInt);
        final int maxZ = Math.min(this.schematic.getLength() - 1, z + rangeInt);
        final int priority = ConfigurationHandler.priority;

        final int rollover = ConfigurationHandler.directionalPriority;

        if (rollover > 0) {
            if (rollover < rollingVel.size()) {
                rollingVel.clear();
                rollingPos = 0;
            }

            Vec3d cm = new Vec3d(player.motionX, 0, player.motionZ);

            if (cm.x != 0 && cm.z != 0) {
                if (rollingVel.size() == rollover) {
                    rollingVel.set(rollingPos, cm.normalize());
                } else {
                    rollingVel.add(rollingPos, cm.normalize());
                }
                rollingPos++;
                if (rollingPos >= rollover) {
                    rollingPos = 0;
                }
                double vx = 0;
                double vz = 0;
                for (final Vec3d pos : rollingVel) {
                    vx = vx + pos.x;
                    vz = vz + pos.z;
                }
                averageVelocity = new Vec3d(vx, 0, vz).normalize().scale(-1000);
            }
        } else if (rollover == 0) {
            averageVelocity = new Vec3d(0,0,0);
        } else {
            averageVelocity = new Vec3d(10000,0,10);
        }



        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return false;
        }

        final int slot = player.inventory.currentItem;
        final boolean isSneaking = player.isSneaking();

        switch (schematic.layerMode) {
        case ALL: break;
        case SINGLE_LAYER:
            if (schematic.renderingLayer > maxY) {
                return false;
            }
            maxY = schematic.renderingLayer;
            minY = schematic.renderingLayer;
            //$FALL-THROUGH$
        case ALL_BELOW:
            if (schematic.renderingLayer < minY) {
                return false;
            }
            maxY = schematic.renderingLayer;
            break;
        }
        
        if (ConfigurationHandler.disableInGui) {
            if (this.minecraft.currentScreen != null && (this.minecraft.currentScreen instanceof GuiShulkerBox || this.minecraft.currentScreen instanceof GuiChest)) {
                return false; // return value is not used?
            }
        }

        final double blockReachDistance = ConfigurationHandler.placeDistance;
        final double blockReachDistanceSq = blockReachDistance * blockReachDistance;
        List<MBlockPos> inRange = new ArrayList<>();
        List<MBlockPos> inRangeBelow = new ArrayList<>();
        for (final MBlockPos pos : BlockPosHelper.getAllInBoxXZY(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (pos.distanceSqToCenter(dX, dY, dZ) > blockReachDistanceSq) {
                continue;
            }
            if ((priority == 2 || priority == 3) && pos.y > dY-2) {
                inRangeBelow.add(new MBlockPos(pos));
            } else {
                inRange.add(new MBlockPos(pos));
            }
        }

        MBCompareDist distcomp = new MBCompareDist(new Vec3d(dX+ averageVelocity.x, dY, dZ+ averageVelocity.z));
        MBCompareHeight heightcomp = new MBCompareHeight();
        MBCompareDist closestComp = new MBCompareDist(new Vec3d(dX, dY, dZ));

        // 1 is layers, 2 is pillars, 3 is below only, 4 is closes first, 5 is closest last
        if (priority == 1) {
            inRange.sort(distcomp);
            inRange.sort(heightcomp);
        } else if (priority == 2) {
            inRange.sort(heightcomp);
            inRange.sort(distcomp);
            inRangeBelow.sort(heightcomp);
            inRangeBelow.sort(distcomp);
            inRange.addAll(inRangeBelow);
        } else if (priority == 3) {
            inRange.sort(heightcomp);
            inRange.sort(distcomp);
        } else if (priority == 4) { // closest first
            inRange.addAll(inRangeBelow);
            inRange.sort(closestComp);
        } else if (priority == 5) { // closest last
            inRange.addAll(inRangeBelow);
            inRange.sort(closestComp);
            Collections.reverse(inRange);
        }

        for (final MBlockPos pos: inRange) {
            try {
                if (placeBlock(world, player, pos)) {
                    return syncSlotAndSneaking(player, slot, isSneaking, true);
                }
            } catch (final Exception e) {
                if (!(e instanceof NeedsWaitingException)) {
                    Reference.logger.error("Could not place block!", e);
                }
                return syncSlotAndSneaking(player, slot, isSneaking, false);
            }
        }
        return syncSlotAndSneaking(player, slot, isSneaking, true);
    }



    private boolean placeBlock(final WorldClient world, final EntityPlayerSP player, final BlockPos pos) throws NeedsWaitingException {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
        if (this.timeout[x][y][z] > 0) {
            this.timeout[x][y][z]--;
            return false;
        }

        final int wx = this.schematic.position.x + x;
        final int wy = this.schematic.position.y + y;
        final int wz = this.schematic.position.z + z;
        final BlockPos realPos = new BlockPos(wx, wy, wz);

        final IBlockState blockState = this.schematic.getBlockState(pos);
        final IBlockState realBlockState = world.getBlockState(realPos);
        final Block realBlock = realBlockState.getBlock();

        if (BlockStateHelper.areBlockStatesEqual(blockState, realBlockState)) {
            // TODO: clean up this mess
            final NBTSync handler = SyncRegistry.INSTANCE.getHandler(realBlock);
            if (handler != null) {
                this.timeout[x][y][z] = (byte) ConfigurationHandler.timeout;

                Integer tries = this.syncBlacklist.get(realPos);
                if (tries == null) {
                    tries = 0;
                } else if (tries >= 10) {
                    return false;
                }

                Reference.logger.trace("Trying to sync block at {} {}", realPos, tries);
                final boolean success = handler.execute(player, this.schematic, pos, world, realPos);
                if (success) {
                    this.syncBlacklist.put(realPos, tries + 1);
                }

                return success;
            }

            return false;
        }

        if (BlockSyncRegistry.CanWork(blockState, realBlockState, realPos, player, world)) {
            final BlockSync blockSyncHandler = BlockSyncRegistry.INSTANCE.getHandler(realBlock);
            if (blockSyncHandler != null) {
                this.timeout[x][y][z] = (byte) ConfigurationHandler.timeout;

                Integer tries = this.syncBlacklist.get(realPos);
                if (tries == null) {
                    tries = 0;
                } else if (tries >= 10) {
                    printDebug("Block at " + realPos + " exceeded max tries.");
                    return false;
                }

                Reference.logger.info("Trying to adjust block at {} {}", realPos, tries);

                packetSneaking(player, false);
                final boolean success = blockSyncHandler.execute(player, this.schematic, pos, world, realPos);
                if (success) {
                    this.syncBlacklist.put(realPos, tries + 1);
                }

                return success;
            }
        }

        if (ConfigurationHandler.destroyBlocks && !world.isAirBlock(realPos) && this.minecraft.playerController.isInCreativeMode()) {
            this.minecraft.playerController.clickBlock(realPos, EnumFacing.DOWN);

            this.timeout[x][y][z] = (byte) ConfigurationHandler.timeout;

            return !ConfigurationHandler.destroyInstantly;
        }

        if (this.schematic.isAirBlock(pos)) {
            return false;
        }

        if (!realBlock.isReplaceable(world, realPos)) {
            return false;
        }

        final ItemStack itemStack = BlockStateToItemStack.getItemStack(blockState, new RayTraceResult(player), this.schematic, pos, player);
        if (itemStack.isEmpty()) {
            Reference.logger.debug("{} is missing a mapping!", blockState);
            return false;
        }

        if (placeBlockIfPossible(world, player, realPos, blockState, itemStack)) {
            this.timeout[x][y][z] = (byte) ConfigurationHandler.timeout;

            if (!ConfigurationHandler.placeInstantly) {
                return true;
            }
        }

        return false;
    }

    /*
     *This is called for every side, and checks if you can place against that side.
     * -pos is the block we're attempting to place.
     * -side is the side we're checking for placeability.
     */
    private boolean isSolid(final World world, final BlockPos pos, final EnumFacing side, final IBlockState blocc) {
        final BlockPos offset = pos.offset(side);
        final IBlockState blockState = world.getBlockState(offset);
        final Block block = blockState.getBlock();


        if (block.isAir(blockState, world, offset)) {
           //printDebug(side + ": failed- is Air.");
            return false;
        }


        if (!ConfigurationHandler.replace) {
            if (block instanceof BlockLiquid) {
                //printDebug(side + ": failed- is fluid.");
                return false;
            }

            if (block.isReplaceable(world, offset)) {
                //printDebug(side + ": failed- block is replaceable?");
                return false;
            }
        }

        if (!blocc.getBlock().canPlaceBlockOnSide(world,pos,side)) {
            return false; // Ensures we don't try to place torches on the sides of slabs and flowerpots on stairs, etc.
        }


        return true;
    }

    private List<EnumFacing> getSolidSides(final World world, final BlockPos pos, final IBlockState blocc) {
        if (!ConfigurationHandler.placeAdjacent) {
            return Arrays.asList(EnumFacing.VALUES);
        }

        final List<EnumFacing> list = new ArrayList<>();

        for (final EnumFacing side : EnumFacing.VALUES) {
            if (isSolid(world, pos, side, blocc)) {
                list.add(side);
            }
        }

        return list;
    }

    private boolean placeBlockIfPossible(final WorldClient world, final EntityPlayerSP player, final BlockPos pos, final IBlockState blockState, final ItemStack itemStack) throws NeedsWaitingException {
        if (itemStack.getItem() instanceof ItemBucket) {
            return false;
        }

        // Handle blocks that rely on facing direction. No, I don't fully understand how this works.
        // TODO: Hello invalid stair placement!
        // In Stealth mode, we kind of have to kill Stairs placement after sending the look packet, which is bad.
        // Pls find a way around this

        final PlacementData placementData = PlacementRegistry.INSTANCE.getPlacementData(blockState, itemStack);

        Block floor = world.getBlockState(pos.offset(EnumFacing.DOWN)).getBlock();
        if (ConfigurationHandler.strictGravityBlockPlacement && blockState.getBlock() instanceof BlockFalling && (floor instanceof BlockLiquid || floor instanceof BlockAir)) {
            printDebug("Skipping gravity block placement!");
            return false;
        }

        final List<EnumFacing> solidSides = getSolidSides(world, pos, blockState);

        if (solidSides.size() == 0) {
            return false;
        }

        printDebug("2: {"+pos+"} succeeded on: " + solidSides);

        EnumFacing direction = solidSides.get(0);
        Vec3d packetHitOffset = null;
        Vec3d viewHitOffset = new Vec3d(0.5, 0.5, 0.5);
        int extraClicks = 0;

        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        double px = player.posX;
        double py = player.posY;
        double pz = player.posZ;

        if (placementData != null) {
            packetHitOffset = new Vec3d(placementData.getOffsetX(blockState), placementData.getOffsetY(blockState), placementData.getOffsetZ(blockState));
            extraClicks = placementData.getExtraClicks(blockState);
        }

        if (ConfigurationHandler.stealthMode) {
            boolean passed = false;
            Block asBlock = blockState.getBlock();
            printDebug("2.5: Place with Stealth Mode. PlacementData: " + placementData);

            // Full blocks need other placing rules. Glass is not treated as a full block even though it should be.
            // Other not working blocks may have to be added here.
            boolean isFullBlock = blockState.isFullBlock();
            if (asBlock != null && (asBlock instanceof BlockGlass || asBlock instanceof BlockStainedGlass)) {
                isFullBlock = true;
            }
            List<EnumFacing> stealthsides = new ArrayList<>();
            for (EnumFacing face : solidSides) {
                switch (face) {
                    case UP:
                        if (y >= py && (packetHitOffset == null || packetHitOffset.y != Constants.Blocks.BLOCK_BOTTOM_HALF)) {
                            passed=true;
                            stealthsides.add(face);
                        }
                        break;
                    case DOWN:
                        if (y <= py +2 && (packetHitOffset == null || packetHitOffset.y != Constants.Blocks.BLOCK_TOP_HALF)) {
                            passed=true;
                            stealthsides.add(face);
                        }
                        break;
                    case SOUTH:
                        if (isFullBlock) {
                            if (z >= pz-1) {
                                passed=true;
                                stealthsides.add(face);
                            }
                        } else {
                            if (z >= pz-2) {
                                passed=true;
                                stealthsides.add(face);
                            }
                        }
                        break;
                    case NORTH:
                        if (isFullBlock) {
                            if (z <= pz) {
                                passed=true;
                                stealthsides.add(face);
                            }
                        } else {
                            if (z <= pz+1) {
                                passed=true;
                                stealthsides.add(face);
                            }
                        }
                        break;
                    case EAST:
                        if (isFullBlock) {
                            if (x >= px-1) {
                                passed=true;
                                stealthsides.add(face);
                            }
                        } else {
                            if (x >= px-2) {
                                passed=true;
                                stealthsides.add(face);
                            }
                        }
                        break;
                    case WEST:
                        if (isFullBlock) {
                            if (x <= px) {
                                passed=true;
                                stealthsides.add(face);
                            }
                        } else {
                            if (x <= px+1) {
                                passed=true;
                                stealthsides.add(face);
                            }
                        }
                        break;
                }
            }
            if (!passed) {
                return false;
            }
            // printDebug("Solid vs. Stealth");
            // printDebug(solidSides.toString());
            // printDebug(stealthsides.toString());

            if (placementData != null) {
                final List<EnumFacing> validDirections = placementData.getValidBlockFacings(solidSides, blockState);
                if (validDirections.size() == 0) {
                    return false;
                }
                float dX = placementData.getOffsetX(blockState);
                float dY = placementData.getOffsetY(blockState);
                float dZ = placementData.getOffsetZ(blockState);
                boolean foundStrictPlacementDirection = false;
    
                for (EnumFacing dir : validDirections) {
                    boolean isInStealSides = false;
                    for (EnumFacing stealth : stealthsides) {
                        if (dir == stealth) {
                            isInStealSides = true;
                            break;
                        }
                    }
                    if (!isInStealSides) {
                        continue;
                    }
                    viewHitOffset = PlacementData.directionToOffset(dir);
                    Vec3d targetPosition = viewHitOffset.addVector(x, y, z);
                    printDebug("Checking block to place " + blockState.getBlock().toString() + " with position " + targetPosition + " with viewoffset " + viewHitOffset + " with direction " + dir + " and packet offset " + dX + ", " + dY + ", " + dZ);
                    if (!placementData.isValidPlayerFacing(blockState, player, targetPosition, world)) {
                        continue;
                    }
                    direction = dir;
                    foundStrictPlacementDirection = true;
                    break;
                }
                if (!foundStrictPlacementDirection) {
                    return false;
                }
            } else {
                direction = solidSides.get(0);
                viewHitOffset = PlacementData.directionToOffset(direction);
                packetHitOffset = viewHitOffset;
            }
        }

        if (!swapToItemWrap(player.inventory, player, itemStack)) {
            return false;
        }
        printDebug("PlaceBlock: " + extraClicks + " " + viewHitOffset + " " + packetHitOffset);
        return placeBlock(world, player, pos, direction, extraClicks, viewHitOffset, packetHitOffset);
    }

    /*
     * Handles resources
     */

    private boolean placeBlock(final WorldClient world, final EntityPlayerSP player, final BlockPos pos, final EnumFacing direction, final int extraClicks, Vec3d viewHitOffset, Vec3d packetHitOffset) {
        printDebug("3: placing.");
        final EnumHand hand = EnumHand.MAIN_HAND;
        final ItemStack itemStack = player.getHeldItem(hand);
        boolean success = false;

        if (!this.minecraft.playerController.isInCreativeMode() && !itemStack.isEmpty() && itemStack.getCount() <= extraClicks) {
            return false;
        }

        packetSneaking(player, true);
        success = doRightClick(world, player, itemStack, pos, direction, viewHitOffset, packetHitOffset, hand);
        for (int i = 0; success && i < extraClicks; i++) {
            success = doRightClick(world, player, itemStack, pos, direction, viewHitOffset, packetHitOffset, hand);
        }

        if (itemStack.getCount() == 0 && success) {
            player.inventory.mainInventory.set(player.inventory.currentItem, ItemStack.EMPTY);
        }

        return success;
    }

    /*
     * Actually PLACES the blocks. Is called twice for multi-part blocks, meaning JUST slabs.
     */
    private boolean doRightClick(final WorldClient world, final EntityPlayerSP player, final ItemStack itemStack, final BlockPos pos, final EnumFacing direction, final Vec3d viewHitOffset, Vec3d packetHitOffset, final EnumHand hand) {
        final EnumFacing side = direction.getOpposite();

        final Vec3d lookAtVector = new Vec3d(pos.getX(), pos.getY(), pos.getZ()).add(viewHitOffset);
        final Vec3d hitPosition;
        if(ConfigurationHandler.stealthMode) {
            PrinterUtil.faceVectorPacketInstant(lookAtVector);
        }

        if (packetHitOffset != null) {
            hitPosition = new Vec3d(pos.getX(), pos.getY(), pos.getZ()).add(packetHitOffset);
        } else {
            hitPosition = PlacementData.directionToOffset(direction).addVector(pos.getX(), pos.getY(), pos.getZ());
        }

        final BlockPos referenceBlockPos = ConfigurationHandler.placeAdjacent ? pos.offset(direction) : pos;
        final EnumActionResult result = this.minecraft.playerController.processRightClickBlock(player, world, referenceBlockPos, side, hitPosition, hand);
        if ((result != EnumActionResult.SUCCESS)) {
            printDebug("4: failed to place block. " + pos + " " + referenceBlockPos + " " + side + " " + hitPosition + " " + hand);
            return false;
        } else {
            printDebug("4: successful place block. " + pos + " " + referenceBlockPos + " " + side + " " + hitPosition + " " + hand);
        }

        player.swingArm(hand);
        return true;
    }


    private boolean syncSlotAndSneaking(final EntityPlayerSP player, final int slot, final boolean isSneaking, final boolean success) {
        player.inventory.currentItem = slot;
        packetSneaking(player, isSneaking);
        return success;
    }

    /**
     * Set sneak state
     * @param player
     * @param isSneaking
     */
    private void packetSneaking(final EntityPlayerSP player, final boolean isSneaking) {
        player.setSneaking(isSneaking);
        player.connection.sendPacket(new CPacketEntityAction(player, isSneaking ? CPacketEntityAction.Action.START_SNEAKING : CPacketEntityAction.Action.STOP_SNEAKING));
    }

    private boolean swapToItemWrap(final InventoryPlayer inventory, final EntityPlayerSP player,  final ItemStack itemStack) throws NeedsWaitingException {
        return swapToItem(inventory, player, itemStack, true);
    }

    private boolean swapToItem(final InventoryPlayer inventory, final EntityPlayerSP player, final ItemStack itemStack, final boolean swapSlots) throws NeedsWaitingException {
        final int slot = getInventorySlotWithItem(inventory, itemStack);

        if (this.minecraft.playerController.isInCreativeMode() && (slot < Constants.Inventory.InventoryOffset.HOTBAR || slot >= Constants.Inventory.InventoryOffset.HOTBAR + Constants.Inventory.Size.HOTBAR) && ConfigurationHandler.swapSlotsQueue.size() > 0) {
            inventory.currentItem = getNextSlot();
            inventory.setInventorySlotContents(inventory.currentItem, itemStack.copy());
            this.minecraft.playerController.sendSlotPacket(inventory.getStackInSlot(inventory.currentItem), Constants.Inventory.SlotOffset.HOTBAR + inventory.currentItem);
            return true;
        }

        if (itemSelectCoolDown > 0) return false;

        if (slot >= Constants.Inventory.InventoryOffset.HOTBAR && slot < Constants.Inventory.InventoryOffset.HOTBAR + Constants.Inventory.Size.HOTBAR) {
            // Item is in hotbar
            if (needsSlowdown(inventory, slot)) {
                itemSelectCoolDown += ConfigurationHandler.placeSlowDownPace;
            }
            inventory.currentItem = slot;
            return true;
        } else if (swapSlots && slot >= Constants.Inventory.InventoryOffset.INVENTORY && slot < Constants.Inventory.InventoryOffset.INVENTORY + Constants.Inventory.Size.INVENTORY) {
            // Item is in inventory somewhere
            if (swapSlots(inventory, player, slot)) {
                // Need to wait for item swap to finish
                throw new NeedsWaitingException();
            }
        }

        return false;
    }

    private int getInventorySlotWithItem(final InventoryPlayer inventory, final ItemStack itemStack) {
        int smallestStack = Integer.MAX_VALUE;
        int smallestStackSlot = -1;
        for (int i = 0; i < inventory.mainInventory.size(); i++) {
            ItemStack item = inventory.mainInventory.get(i);
            if (item.isItemEqual(itemStack)) {
                if (item.getCount() < smallestStack) {
                    smallestStack = item.getCount();
                    smallestStackSlot = i;
                }
            }
        }
        return smallestStackSlot;
    }

    private boolean needsSlowdown(final InventoryPlayer inventory, final int slot) {
        return inventory.mainInventory.get(slot).getCount() < ConfigurationHandler.placeSlowDownTrigger;
    }

    private int getFreeSlot(final InventoryPlayer inventoryPlayer) {
        for (int slot : ConfigurationHandler.swapSlotsQueue.toArray(new Integer[0])) {
            if (inventoryPlayer.getStackInSlot(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * Swaps items in the inventory for other items. Returns a true if swapping is in progress false if no work can be done
     * 
     * @param inventory
     * @param from
     * @return 
     */
    private boolean swapSlots(final InventoryPlayer inventory, EntityPlayerSP player, final int from) {
        if (itemSelectCoolDown > 0) return false;
        if (ConfigurationHandler.swapSlotsQueue.size() > 0) {
            int slot = getFreeSlot(inventory);
            if (slot == -1) {
                slot = getNextSlot();
                printDebug("[Inventory] Swapping slot " + slot + " for inventory item at " + from);
            } else {
                printDebug("[Inventory] Found free slot to put item in " + slot);
            }

            player.connection.sendPacket(new CPacketHeldItemChange(slot));
            this.minecraft.playerController.pickItem(from);
            itemSelectCoolDown += ConfigurationHandler.equipCoolDown;
            // Don't return true as we need to wait for the item to appear in the hotbar first
            return true;
        }

        return false;
    }

    private int getNextSlot() {
        final int slot = ConfigurationHandler.swapSlotsQueue.poll() % Constants.Inventory.Size.HOTBAR;
        ConfigurationHandler.swapSlotsQueue.offer(slot);
        return slot;
    }

    private void printDebug(String message) {
        if(ConfigurationHandler.debugMode) {
            this.minecraft.player.sendMessage(new TextComponentTranslation(I18n.format(message)));
        }
    }

    private List<Vec3d> raycast(Vec3d endpoint) {
        List<Vec3d> blocks = null;
        blocks.add(new Vec3d(1,2,3));

        return blocks;
    }
}

class MBCompareDist implements Comparator<MBlockPos> {
    Vec3d p;
    public MBCompareDist(Vec3d point) {
        p = new Vec3d(point.x-.5, point.y-.5, point.z-.5);
    }
    public int compare(MBlockPos A, MBlockPos B) {
        if (A.x == B.x && A.z == B.z) {
            return 0;
        } else if (Math.pow(A.x-p.x, 2)+Math.pow(A.z-p.z,2) > Math.pow(B.x-p.x, 2)+Math.pow(B.z-p.z,2)) {
            return 1;
        } else {
            return -1;
        }
    }
}

class MBCompareHeight implements Comparator<MBlockPos> {

    public int compare(MBlockPos A, MBlockPos B) {
        if (A.y == B.y) {
            return 0;
        } else if (A.y > B.y) {
            return 1;
        } else {
            return -1;
        }
    }
}
