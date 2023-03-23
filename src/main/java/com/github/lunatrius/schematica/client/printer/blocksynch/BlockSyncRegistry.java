package com.github.lunatrius.schematica.client.printer.blocksynch;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import java.util.HashMap;

public class BlockSyncRegistry {
  public static final BlockSyncRegistry INSTANCE = new BlockSyncRegistry();

  public static final boolean CanWork(IBlockState stateA, IBlockState stateB) {
    // loop through all the blocks in the map
    for (final BlockSync handler : INSTANCE.map.values()) {
      // if the handler can work, return true
      if (handler.blockNeedsChange(stateA, stateB)) {
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
  }
}
