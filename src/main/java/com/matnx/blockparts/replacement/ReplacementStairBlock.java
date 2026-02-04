package com.matnx.blockparts.replacement;

import com.matnx.blockparts.BlockParts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ReplacementStairBlock extends StairBlock {
    public ReplacementStairBlock(BlockState baseState, BlockBehaviour.Properties properties) {
        super(baseState, properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        BlockParts.replaceWithStateStore(level, pos, state);
    }
}
