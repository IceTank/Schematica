package com.github.lunatrius.schematica.client.printer.blocksynch;

import com.github.lunatrius.schematica.client.printer.PrinterUtil;
import com.github.lunatrius.schematica.client.printer.registry.PlacementRegistry;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;

import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class FenceGateSynch extends BlockSync {
  @Override
  public boolean execute(EntityPlayerSP player, World schematic, BlockPos schematicPos, WorldClient mcWorld, BlockPos pos) {
    final EnumHand hand = EnumHand.MAIN_HAND;
    final EnumFacing side = EnumFacing.UP;
    final BlockPos ref = pos.offset(side.getOpposite());
    final Vec3d cent = new Vec3d(ref.getX() + .5, ref.getY() + .5, ref.getZ() + .5);
    final double x = pos.getX() - ref.getX();
    final double y = pos.getY() - ref.getY();
    final double z = pos.getZ() - ref.getZ();
    final Vec3d epiclook = new Vec3d(x * .5 + cent.x, y * .5 + cent.y, z * .5 + cent.z);

    if (ConfigurationHandler.stealthMode) {
      PrinterUtil.faceVectorPacketInstant(epiclook);
    }

    final Vec3d hitVec = new Vec3d(.5, .5, .5);
    final CPacketPlayerTryUseItemOnBlock packet = new CPacketPlayerTryUseItemOnBlock(pos, side, hand, (float) hitVec.x,
        (float) hitVec.y, (float) hitVec.z);
    boolean status = this.sendPacket(packet);

    player.swingArm(hand);
    return status;
  }

  @Override
  public boolean canWorkInPosition(IBlockState schematicBlock, IBlockState currentBlock, World world, BlockPos pos, final EntityPlayerSP player) {
    EnumFacing facingSchematic = schematicBlock.getActualState(world, pos).getValue(net.minecraft.block.BlockHorizontal.FACING);
    Vec3d vec = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    return PlacementRegistry.isFacingCorrectly(facingSchematic, player, vec, world, true);
  }

  public boolean blockNeedsChange(IBlockState schematicBlock, IBlockState currentBlock, IBlockAccess world, BlockPos pos) {
    if (schematicBlock.getBlock() instanceof BlockFenceGate && currentBlock.getBlock() instanceof BlockFenceGate) {
      
    }
    return false;
  }

  @Override
  public boolean blockNeedsChange(IBlockState schematicBlock, IBlockState currentBlock) {
    if (schematicBlock.getBlock() instanceof BlockFenceGate && currentBlock.getBlock() instanceof BlockFenceGate) {
      return currentBlock.getBlock().getMetaFromState(currentBlock) != schematicBlock.getBlock().getMetaFromState(schematicBlock);
    }
    return false;
  }
}
