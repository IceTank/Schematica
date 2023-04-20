package com.github.lunatrius.schematica.client.printer.registry;

import com.github.lunatrius.schematica.block.state.BlockStateHelper;
import com.github.lunatrius.schematica.reference.Constants;

import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockEndRod;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockObserver;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class PlacementRegistry {
    public static final PlacementRegistry INSTANCE = new PlacementRegistry();

    private final Map<Class<? extends Block>, PlacementData> classPlacementMap = new LinkedHashMap<Class<? extends Block>, PlacementData>();
    private final Map<Block, PlacementData> blockPlacementMap = new HashMap<Block, PlacementData>();
    private final Map<Item, PlacementData> itemPlacementMap = new HashMap<Item, PlacementData>();

    private void populateMappings() {
        this.classPlacementMap.clear();
        this.blockPlacementMap.clear();
        this.itemPlacementMap.clear();

        final IValidPlayerFacing playerFacingEntityIgnoreY = (final IBlockState blockState, final EntityPlayer player, final Vec3d pos, final World world) -> {
            final EnumFacing facing = BlockStateHelper.<EnumFacing>getPropertyValue(blockState, "facing");
            return isFacingCorrectly(facing, player, pos, world, true);
        };
        final IValidPlayerFacing playerFacingEntityOpposite = (final IBlockState blockState, final EntityPlayer player, final Vec3d pos, final World world) -> {
            final EnumFacing facing = BlockStateHelper.<EnumFacing>getPropertyValue(blockState, "facing");
            EnumFacing facing2 = facing;
            if (facing != EnumFacing.UP && facing != EnumFacing.DOWN) {
                facing2 = facing.getOpposite();
            }
            return isFacingCorrectly(facing2, player, pos, world, false);
        };
        final IValidPlayerFacing playerFacingEntityOppositeIgnoreY = (final IBlockState blockState, final EntityPlayer player, final Vec3d pos, final World world) -> {
            final EnumFacing facing = BlockStateHelper.<EnumFacing>getPropertyValue(blockState, "facing");
            EnumFacing facing2 = facing;
            if (facing != EnumFacing.UP && facing != EnumFacing.DOWN) {
                facing2 = facing.getOpposite();
            }
            return isFacingCorrectly(facing2, player, pos, world, true);
        };
        final IValidPlayerFacing playerFacingPiston = (final IBlockState blockState, final EntityPlayer player, final Vec3d pos, final World world) -> {
            final EnumFacing facing = BlockStateHelper.<EnumFacing>getPropertyValue(blockState, "facing");
            return isFacingCorrectly(facing.getOpposite(), player, pos, world, false);
        };
        final IValidPlayerFacing playerFacingObserver = (final IBlockState blockState, final EntityPlayer player, final Vec3d pos, final World world) -> {
            final EnumFacing facing = BlockStateHelper.<EnumFacing>getPropertyValue(blockState, "facing");
            return isFacingCorrectly(facing, player, pos, world, false);
        };
        final IValidPlayerFacing playerFacingRotateY = (final IBlockState blockState, final EntityPlayer player, final Vec3d pos, final World world) -> {
            final EnumFacing facing = BlockStateHelper.<EnumFacing>getPropertyValue(blockState, "facing");
            return isFacingCorrectly(facing, player, pos, world, true);
        };
        final IValidPlayerFacing playerFacingLever = (final IBlockState blockState, final EntityPlayer player, final Vec3d pos, final World world) -> {
            final BlockLever.EnumOrientation value = blockState.getValue(BlockLever.FACING);
            return !value.getFacing().getAxis().isVertical() || BlockLever.EnumOrientation.forFacings(value.getFacing(), player.getHorizontalFacing()) == value;
        };
        final IValidPlayerFacing playerFacingStandingSign = (final IBlockState blockState, final EntityPlayer player, final Vec3d pos, final World world) -> {
            final int value = blockState.getValue(BlockStandingSign.ROTATION);
            final int facing = MathHelper.floor((player.rotationYaw + 180.0) * 16.0 / 360.0 + 0.5) & 15;
            return value == facing;
        };
        final IValidPlayerFacing playerFacingIgnore = (final IBlockState blockState, final EntityPlayer player, final Vec3d pos, final World world) -> {
            return false;
        };

        final IOffset offsetSlab = (final IBlockState blockState) -> {
            if (!((BlockSlab) blockState.getBlock()).isDouble()) {
                final BlockSlab.EnumBlockHalf half = blockState.getValue(BlockSlab.HALF);
                return half == BlockSlab.EnumBlockHalf.TOP ? Constants.Blocks.BLOCK_TOP_HALF : Constants.Blocks.BLOCK_BOTTOM_HALF;
            }

            return 0;
        };
        final IOffset offsetStairs = (final IBlockState blockState) -> {
            final BlockStairs.EnumHalf half = blockState.getValue(BlockStairs.HALF);
            return half == BlockStairs.EnumHalf.TOP ? 1 : 0;
        };
        final IOffset offsetTrapDoor = (final IBlockState blockState) -> {
            final BlockTrapDoor.DoorHalf half = blockState.getValue(BlockTrapDoor.HALF);
            return half == BlockTrapDoor.DoorHalf.TOP ? 1 : 0;
        };

        final IValidBlockFacing blockFacingLog = (final List<EnumFacing> solidSides, final IBlockState blockState) -> {
            final List<EnumFacing> list = new ArrayList<EnumFacing>();

            final BlockLog.EnumAxis axis = blockState.getValue(BlockLog.LOG_AXIS);
            for (final EnumFacing side : solidSides) {
                if (axis != BlockLog.EnumAxis.fromFacingAxis(side.getAxis())) {
                    continue;
                }

                list.add(side);
            }

            return list;
        };
        final IValidBlockFacing blockFacingPillar = (final List<EnumFacing> solidSides, final IBlockState blockState) -> {
            final List<EnumFacing> list = new ArrayList<EnumFacing>();

            final EnumFacing.Axis axis = blockState.getValue(BlockRotatedPillar.AXIS);
            for (final EnumFacing side : solidSides) {
                if (axis != side.getAxis()) {
                    continue;
                }

                list.add(side);
            }

            return list;
        };
        final IValidBlockFacing blockFacingOpposite = (final List<EnumFacing> solidSides, final IBlockState blockState) -> {
            final List<EnumFacing> list = new ArrayList<EnumFacing>();

            final IProperty propertyFacing = BlockStateHelper.getProperty(blockState, "facing");
            if (propertyFacing != null && propertyFacing.getValueClass().equals(EnumFacing.class)) {
                final EnumFacing facing = ((EnumFacing) blockState.getValue(propertyFacing));
                for (final EnumFacing side : solidSides) {
                    if (facing.getOpposite() != side) {
                        continue;
                    }

                    list.add(side);
                }
            }

            return list;
        };
        final IValidBlockFacing blockFacingSame = (final List<EnumFacing> solidSides, final IBlockState blockState) -> {
            final List<EnumFacing> list = new ArrayList<EnumFacing>();

            final IProperty propertyFacing = BlockStateHelper.getProperty(blockState, "facing");
            if (propertyFacing != null && propertyFacing.getValueClass().equals(EnumFacing.class)) {
                final EnumFacing facing = (EnumFacing) blockState.getValue(propertyFacing);
                for (final EnumFacing side : solidSides) {
                    if (facing != side) {
                        continue;
                    }

                    list.add(side);
                }
            }

            return list;
        };
        final IValidBlockFacing blockFacingHopper = (final List<EnumFacing> solidSides, final IBlockState blockState) -> {
            final List<EnumFacing> list = new ArrayList<EnumFacing>();

            final EnumFacing facing = blockState.getValue(BlockHopper.FACING);
            for (final EnumFacing side : solidSides) {
                if (facing != side) {
                    continue;
                }

                list.add(side);
            }

            return list;
        };
        final IValidBlockFacing blockFacingLever = (final List<EnumFacing> solidSides, final IBlockState blockState) -> {
            final List<EnumFacing> list = new ArrayList<EnumFacing>();

            final BlockLever.EnumOrientation facing = blockState.getValue(BlockLever.FACING);
            for (final EnumFacing side : solidSides) {
                if (facing.getFacing().getOpposite() != side) {
                    continue;
                }

                list.add(side);
            }

            return list;
        };
        final IValidBlockFacing blockFacingQuartz = (final List<EnumFacing> solidSides, final IBlockState blockState) -> {
            final List<EnumFacing> list = new ArrayList<EnumFacing>();

            final BlockQuartz.EnumType variant = blockState.getValue(BlockQuartz.VARIANT);
            for (final EnumFacing side : solidSides) {
                if (variant == BlockQuartz.EnumType.LINES_X && side.getAxis() != EnumFacing.Axis.X) {
                    continue;
                } else if (variant == BlockQuartz.EnumType.LINES_Y && side.getAxis() != EnumFacing.Axis.Y) {
                    continue;
                } else if (variant == BlockQuartz.EnumType.LINES_Z && side.getAxis() != EnumFacing.Axis.Z) {
                    continue;
                }

                list.add(side);
            }

            return list;
        };

        final IExtraClick extraClickDoubleSlab = (final IBlockState blockState) -> {
            return ((BlockSlab) blockState.getBlock()).isDouble() ? 1 : 0;
        };

        /**
         * minecraft
         */
        // extends BlockRotatedPillar
        addPlacementMapping(BlockLog.class, new PlacementData(blockFacingLog));

        addPlacementMapping(BlockButton.class, new PlacementData(blockFacingOpposite));
        addPlacementMapping(BlockChest.class, new PlacementData(playerFacingEntityOppositeIgnoreY));
        addPlacementMapping(BlockDispenser.class, new PlacementData(playerFacingPiston));
        addPlacementMapping(BlockDoor.class, new PlacementData(playerFacingEntityIgnoreY));
        addPlacementMapping(BlockEnderChest.class, new PlacementData(playerFacingEntityOppositeIgnoreY));
        addPlacementMapping(BlockEndRod.class, new PlacementData(blockFacingOpposite));
        addPlacementMapping(BlockFenceGate.class, new PlacementData(playerFacingEntityIgnoreY));
        addPlacementMapping(BlockFurnace.class, new PlacementData(playerFacingEntityOppositeIgnoreY));
        addPlacementMapping(BlockHopper.class, new PlacementData(blockFacingHopper));
        addPlacementMapping(BlockObserver.class, new PlacementData(playerFacingObserver));
        addPlacementMapping(BlockPistonBase.class, new PlacementData(playerFacingPiston));
        addPlacementMapping(BlockPumpkin.class, new PlacementData(playerFacingEntityOppositeIgnoreY));
        addPlacementMapping(BlockRotatedPillar.class, new PlacementData(blockFacingPillar));
        addPlacementMapping(BlockSlab.class, new PlacementData().setOffsetY(offsetSlab).setExtraClick(extraClickDoubleSlab));
        addPlacementMapping(BlockStairs.class, new PlacementData(playerFacingEntityIgnoreY).setOffsetY(offsetStairs));
        addPlacementMapping(BlockTorch.class, new PlacementData(blockFacingOpposite));
        addPlacementMapping(BlockTrapDoor.class, new PlacementData(blockFacingOpposite).setOffsetY(offsetTrapDoor));

        addPlacementMapping(Blocks.ANVIL, new PlacementData(playerFacingRotateY));
        addPlacementMapping(Blocks.CHAIN_COMMAND_BLOCK, new PlacementData(playerFacingEntityOpposite));
        addPlacementMapping(Blocks.COCOA, new PlacementData(blockFacingSame));
        addPlacementMapping(Blocks.END_PORTAL_FRAME, new PlacementData(playerFacingEntityOpposite));
        addPlacementMapping(Blocks.LADDER, new PlacementData(blockFacingOpposite));
        addPlacementMapping(Blocks.LEVER, new PlacementData(playerFacingLever, blockFacingLever));
        addPlacementMapping(Blocks.QUARTZ_BLOCK, new PlacementData(blockFacingQuartz));
        addPlacementMapping(Blocks.REPEATING_COMMAND_BLOCK, new PlacementData(playerFacingEntityOpposite));
        addPlacementMapping(Blocks.STANDING_SIGN, new PlacementData(playerFacingStandingSign));
        addPlacementMapping(Blocks.TRIPWIRE_HOOK, new PlacementData(blockFacingOpposite));
        addPlacementMapping(Blocks.WALL_SIGN, new PlacementData(blockFacingOpposite));

        addPlacementMapping(Items.COMPARATOR, new PlacementData(playerFacingEntityOpposite));
        addPlacementMapping(Items.REPEATER, new PlacementData(playerFacingEntityOpposite));

        addPlacementMapping(Blocks.BED, new PlacementData(playerFacingIgnore));
        addPlacementMapping(Blocks.END_PORTAL, new PlacementData(playerFacingIgnore));
        addPlacementMapping(Blocks.PISTON_EXTENSION, new PlacementData(playerFacingIgnore));
        addPlacementMapping(Blocks.PISTON_HEAD, new PlacementData(playerFacingIgnore));
        addPlacementMapping(Blocks.PORTAL, new PlacementData(playerFacingIgnore));
        addPlacementMapping(Blocks.SKULL, new PlacementData(playerFacingIgnore));
        addPlacementMapping(Blocks.STANDING_BANNER, new PlacementData(playerFacingIgnore));
        addPlacementMapping(Blocks.WALL_BANNER, new PlacementData(playerFacingIgnore));
    }

    private PlacementData addPlacementMapping(final Class<? extends Block> clazz, final PlacementData data) {
        if (clazz == null || data == null) {
            return null;
        }

        return this.classPlacementMap.put(clazz, data);
    }

    private PlacementData addPlacementMapping(final Block block, final PlacementData data) {
        if (block == null || data == null) {
            return null;
        }

        return this.blockPlacementMap.put(block, data);
    }

    private PlacementData addPlacementMapping(final Item item, final PlacementData data) {
        if (item == null || data == null) {
            return null;
        }

        return this.itemPlacementMap.put(item, data);
    }

    public PlacementData getPlacementData(final IBlockState blockState, final ItemStack itemStack) {
        final Item item = itemStack.getItem();

        final PlacementData placementDataItem = this.itemPlacementMap.get(item);
        if (placementDataItem != null) {
            return placementDataItem;
        }

        final Block block = blockState.getBlock();

        final PlacementData placementDataBlock = this.blockPlacementMap.get(block);
        if (placementDataBlock != null) {
            return placementDataBlock;
        }

        for (final Class<? extends Block> clazz : this.classPlacementMap.keySet()) {
            if (clazz.isInstance(block)) {
                return this.classPlacementMap.get(clazz);
            }
        }

        return null;
    }

    boolean isFacingCorrectly(final EnumFacing facing, final EntityPlayer player, final Vec3d destinationPos, final World world, final boolean ignoreY) {
        final float minAngle = 0.52f;
        final Vec3d north = new Vec3d(0, 0, -1);
        final Vec3d south = new Vec3d(0, 0, 1);
        final Vec3d east = new Vec3d(1, 0, 0);
        final Vec3d west = new Vec3d(-1, 0, 0);
        final Vec3d up = new Vec3d(0, 1, 0);
        final Vec3d down = new Vec3d(0, -1, 0);
        double yLevelBlock = ignoreY ? 0.0f : destinationPos.y;
        Vec3d blockPos = new Vec3d(destinationPos.x, yLevelBlock, destinationPos.z);
        double yLevelPlayer = ignoreY ? 0.0f : (float) player.posY + player.height;
        final Vec3d playerPos = new Vec3d(player.posX, yLevelPlayer, player.posZ);
        Vec3d playerViewVector = blockPos.subtract(playerPos).normalize();

        Logger logger = Logger.getLogger("PlacementHelper");
        switch (facing) {
            case NORTH:
                logger.info("North " + Math.acos(playerViewVector.dotProduct(north)));
                return Math.acos(playerViewVector.dotProduct(north)) < minAngle;
            case SOUTH:
                logger.info("South " + Math.acos(playerViewVector.dotProduct(south)));
                return Math.acos(playerViewVector.dotProduct(south)) < minAngle;
            case EAST:
                logger.info("East " + Math.acos(playerViewVector.dotProduct(east)));
                return Math.acos(playerViewVector.dotProduct(east)) < minAngle;
            case WEST:
                logger.info("West " + Math.acos(playerViewVector.dotProduct(west)));
                return Math.acos(playerViewVector.dotProduct(west)) < minAngle;
            case UP:
                logger.info("Up " + Math.acos(playerViewVector.dotProduct(up)));
                return Math.acos(playerViewVector.dotProduct(up)) < minAngle;
            case DOWN:
                logger.info("Down " + Math.acos(playerViewVector.dotProduct(down)));
                return Math.acos(playerViewVector.dotProduct(down)) < minAngle;
            default:
                return false;
        }
    }

    static {
        INSTANCE.populateMappings();
    }
}
