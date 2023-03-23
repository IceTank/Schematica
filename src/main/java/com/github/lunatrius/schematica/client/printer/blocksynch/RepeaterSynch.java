package com.github.lunatrius.schematica.client.printer.blocksynch;

import com.github.lunatrius.schematica.client.printer.PrinterUtil;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class RepeaterSynch extends BlockSync {
  @Override
  public boolean execute(EntityPlayerSP player, World schematic, BlockPos schematicPos, WorldClient mcWorld,
      BlockPos pos) {
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

  public boolean blockNeedsChange(final IBlockState stateA, final IBlockState stateB) {
    final Block blockA = stateA.getBlock();
    final Block blockB = stateB.getBlock();
    if (blockA instanceof BlockRedstoneRepeater && blockB instanceof BlockRedstoneRepeater) {
      final BlockRedstoneRepeater repeaterA = (BlockRedstoneRepeater) blockA;
      final BlockRedstoneRepeater repeaterB = (BlockRedstoneRepeater) blockB;

      final int rotationA = repeaterA.getMetaFromState(stateA) & 3;
      final int rotationB = repeaterB.getMetaFromState(stateB) & 3;
      if (rotationA != rotationB) {
        return false;
      }

      final int delayA = (repeaterA.getMetaFromState(stateA) >> 2);
      final int delayB = (repeaterB.getMetaFromState(stateB) >> 2);

      return delayA != delayB;
    }
    return true;
  }
}
