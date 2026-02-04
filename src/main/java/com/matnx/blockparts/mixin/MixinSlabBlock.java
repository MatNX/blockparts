package com.matnx.blockparts.mixin;

import com.matnx.blockparts.BlockParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.world.level.block.SlabBlock.WATERLOGGED;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE;
import static net.minecraft.world.level.block.state.properties.SlabType.BOTTOM;

@Mixin(SlabBlock.class)
public abstract class MixinSlabBlock extends Block {

    public MixinSlabBlock(Properties properties) {
        super(properties);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstruct(BlockBehaviour.Properties properties, CallbackInfo ci) {
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(WATERLOGGED, false).setValue(SLAB_TYPE, BOTTOM));
    }

    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving, CallbackInfo ci) {
        if (BlockParts.replaceSlabWithStateStore(level, pos, state)) {
            ci.cancel();
        }
    }

    @Inject(method = "useShapeForLightOcclusion", at = @At("HEAD"), cancellable = true)
    private void useShapeForOcclusion(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method = "canBeReplaced", at = @At("HEAD"), cancellable = true)
    private void getState(BlockState state, BlockPlaceContext useContext, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
    private void getFluidState(BlockState state, CallbackInfoReturnable<FluidState> cir) {
        cir.setReturnValue(Fluids.EMPTY.defaultFluidState());
    }

    @Inject(method = "canPlaceLiquid", at = @At("HEAD"), cancellable = true)
    private void canPlaceLiquid(Player p_294165_, BlockGetter p_56363_, BlockPos p_56364_, BlockState p_56365_, Fluid p_56366_, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "placeLiquid", at = @At("HEAD"), cancellable = true)
    private void placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void onGetShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        Direction.Axis axis = state.getValue(AXIS);
        switch (axis) {
            case X -> {
                cir.setReturnValue(Shapes.create(0, 0, 0, 0.5, 1, 1));
            }
            case Y -> {
                cir.setReturnValue(Shapes.create(0, 0, 0, 1, 0.5, 1));
            }
            default -> {
                cir.setReturnValue(Shapes.create(0, 0, 0, 1, 1, 0.5));
            }
        }
    }

    @Inject(method = "createBlockStateDefinition", at = @At("HEAD"))
    private void redefineProperties(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(AXIS);
    }

    @Inject(method = "getStateForPlacement", at = @At("HEAD"), cancellable = true)
    private void onGetStateForPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        Direction.Axis axis = context.getClickedFace().getAxis();
        BlockState state = ((SlabBlock)(Object)this).defaultBlockState().setValue(AXIS, axis);
        cir.setReturnValue(state);
    }
}
