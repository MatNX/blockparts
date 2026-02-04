package com.matnx.blockparts.mixin;

import com.matnx.blockparts.BlockParts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StairBlock.class)
public abstract class MixinStairBlock extends Block {
    public MixinStairBlock(Properties properties) {
        super(properties);
    }

    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving, CallbackInfo ci) {
        if (BlockParts.replaceStairWithStateStore(level, pos, state)) {
            ci.cancel();
        }
    }
}
