package com.github.lunatrius.schematica.client.printer.blocksynch;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class BlockSync {
  protected final Minecraft minecraft = Minecraft.getMinecraft();

  public abstract boolean execute(final EntityPlayerSP player, final World schematic, final BlockPos schematicPos,
      final WorldClient mcWorld, final BlockPos pos);

  public abstract boolean blockNeedsChange(final IBlockState stateA, final IBlockState stateB);
  
  public final <T extends INetHandler> boolean sendPacket(final Packet<T> packet) {
    final NetHandlerPlayClient connection = this.minecraft.getConnection();
    if (connection == null) {
      return false;
    }

    connection.sendPacket(packet);
    return true;
  }
}
