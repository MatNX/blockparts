package com.matnx.blockparts.mixin;

import com.matnx.blockparts.statestore.StateStoreBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.matnx.blockparts.BlockParts.STATE_STORE_BLOCK;

@Mixin(BlockItem.class)
public abstract class MixinBlockItem {

    @Final
    @Shadow
    private Block block;

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void redirectSlab(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = context.getItemInHand();

        // Redirect placement for slabs and custom part blocks
        if (block instanceof net.minecraft.world.level.block.SlabBlock || block instanceof com.matnx.blockparts.part.PartBlock) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            Player player = context.getPlayer();
            BlockState originalState = block.getStateForPlacement(context);
            BlockState customState = STATE_STORE_BLOCK.get().defaultBlockState();

            if (!context.canPlace()) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }

            BlockEntity existingEntity = level.getBlockEntity(pos);
            if (existingEntity instanceof StateStoreBlockEntity stateStore) {
                InteractionResult addResult = stateStore.tryAddBlock(
                        originalState,
                        new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside()),
                        context.getPlayer(),
                        context.getHand()
                );
                if (addResult == InteractionResult.SUCCESS) {
                    finalizePlacement(level, pos, player, stack, originalState);
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }
                InteractionResult adjacentResult = tryPlaceInAdjacent(level, context, originalState, stack);
                cir.setReturnValue(adjacentResult);
                return;
            }

            if (!level.getBlockState(pos).canBeReplaced()) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }

            BlockState replacedState = level.getBlockState(pos);
            if (level.setBlock(pos, customState, 11)) { // 11 = flags: update neighbors + send to clients
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof StateStoreBlockEntity stateStore) {
                    InteractionResult addResult = stateStore.tryAddBlock(
                            originalState,
                            new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside()),
                            context.getPlayer(),
                            context.getHand()
                    );
                    if (addResult == InteractionResult.SUCCESS) {
                        finalizePlacement(level, pos, player, stack, originalState);
                        cir.setReturnValue(InteractionResult.SUCCESS);
                        return;
                    }
                }
                level.setBlock(pos, replacedState, 11);
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    private void finalizePlacement(Level level, BlockPos pos, Player player, ItemStack stack, BlockState originalState) {
        SoundType soundtype = originalState.getSoundType(level, pos, player);
        level.playSound(player, pos, soundtype.getPlaceSound(), SoundSource.BLOCKS,
                (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);

        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
        }

        level.gameEvent(GameEvent.BLOCK_PLACE, pos, Context.of(player, originalState));
        stack.consume(1, player);
    }

    private InteractionResult tryPlaceInAdjacent(Level level, BlockPlaceContext context, BlockState originalState, ItemStack stack) {
        BlockPos adjacentPos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();

        if (!level.getBlockState(adjacentPos).canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        BlockState customState = STATE_STORE_BLOCK.get().defaultBlockState();
        BlockState replacedState = level.getBlockState(adjacentPos);
        if (!level.setBlock(adjacentPos, customState, 11)) {
            return InteractionResult.FAIL;
        }

        BlockEntity be = level.getBlockEntity(adjacentPos);
        if (!(be instanceof StateStoreBlockEntity stateStore)) {
            return InteractionResult.FAIL;
        }

        BlockHitResult adjacentHit = new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace().getOpposite(),
                adjacentPos,
                context.isInside()
        );

        InteractionResult addResult = stateStore.tryAddBlock(originalState, adjacentHit, context.getPlayer(), context.getHand());
        if (addResult == InteractionResult.SUCCESS) {
            finalizePlacement(level, adjacentPos, player, stack, originalState);
            return InteractionResult.SUCCESS;
        }
        level.setBlock(adjacentPos, replacedState, 11);
        return InteractionResult.FAIL;
    }
}
