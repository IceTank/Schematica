package com.github.lunatrius.schematica.client.printer.blocksynch;

import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockSyncRegistry {
  public static final BlockSyncRegistry INSTANCE = new BlockSyncRegistry();

  public static final boolean CanWork(IBlockState schematicBlock, IBlockState currentPos, final BlockPos blockPos, final EntityPlayerSP player, final World world) {
    // loop through all the blocks in the map
    for (final BlockSync handler : INSTANCE.map.values()) {
      // if the handler can work, return true
      if (handler.blockNeedsChange(schematicBlock, currentPos) && handler.canWorkInPosition(schematicBlock, currentPos, world, blockPos, player)) {
        return true;
      }
    }

    return false;
  }

  private HashMap<Block, BlockSync> map = new HashMap<Block, BlockSync>();

  public void register(final Block block, final BlockSync handler) {
    if (block == null || handler == null) {
      return;
    }

    this.map.put(block, handler);
  }

  public BlockSync getHandler(final Block block) {
    return this.map.get(block);
  }

  static {
    INSTANCE.register(Blocks.UNPOWERED_REPEATER, new RepeaterSynch());
    BlockSync fenceGate = new FenceGateSynch();
    INSTANCE.register(Blocks.OAK_FENCE_GATE, fenceGate);
    INSTANCE.register(Blocks.SPRUCE_FENCE_GATE, fenceGate);
    INSTANCE.register(Blocks.BIRCH_FENCE_GATE, fenceGate);
    INSTANCE.register(Blocks.JUNGLE_FENCE_GATE, fenceGate);
    INSTANCE.register(Blocks.DARK_OAK_FENCE_GATE, fenceGate);
    INSTANCE.register(Blocks.ACACIA_FENCE_GATE, fenceGate);
  }
}
