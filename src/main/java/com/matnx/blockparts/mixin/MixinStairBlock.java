package com.matnx.blockparts.mixin;

import com.matnx.blockparts.BlockParts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StairBlock.class)
public abstract class MixinStairBlock extends Block {
    public MixinStairBlock(Properties properties) {
        super(properties);
    }

    @Inject(method = "useShapeForLightOcclusion", at = @At("HEAD"), cancellable = true)
    private void useShapeForOcclusion(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method = "canBeReplaced", at = @At("HEAD"), cancellable = true)
    private void canBeReplaced(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
    private void getFluidState(BlockState state, CallbackInfoReturnable<FluidState> cir) {
        cir.setReturnValue(Fluids.EMPTY.defaultFluidState());
    }

    @Inject(method = "canPlaceLiquid", at = @At("HEAD"), cancellable = true)
    private void canPlaceLiquid(Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "placeLiquid", at = @At("HEAD"), cancellable = true)
    private void placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        cir.setReturnValue(Shapes.block());
    }

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        cir.setReturnValue(Shapes.block());
    }

    @Inject(method = "getDescriptionId", at = @At("HEAD"), cancellable = true)
    private void getDescriptionId(CallbackInfoReturnable<String> cir) {
        Block cubeBlock = BlockParts.getCubeBlockForStair((Block) (Object) this);
        if (cubeBlock != null) {
            cir.setReturnValue(cubeBlock.getDescriptionId());
        }
    }
}
