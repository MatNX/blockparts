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
import net.minecraft.sounds.SoundEvents;
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

        // Redirect placement for all slab items
        if (block instanceof net.minecraft.world.level.block.SlabBlock) {
            // your replacement logic
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            Player player = context.getPlayer();

            // Your custom block to place
            BlockState originalState = block.getStateForPlacement(context);
            BlockState customState = STATE_STORE_BLOCK.get().defaultBlockState();

            if (!context.canPlace()) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }

            if (level.setBlock(pos, customState, 11)) { // 11 = flags: update neighbors + send to clients
                SoundType soundtype = originalState.getSoundType(level, pos, player);
                level.playSound(player, pos, soundtype.getPlaceSound(), SoundSource.BLOCKS,
                        (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);

                if (player instanceof ServerPlayer serverPlayer) {
                    CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
                }

                level.gameEvent(GameEvent.BLOCK_PLACE, pos, Context.of(player, originalState));

                stack.consume(1, player);
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof StateStoreBlockEntity stateStore) {
                    BlockState state = block.getStateForPlacement(context);
                    stateStore.tryAddBlock(state, new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside()), context.getPlayer(), context.getHand());
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
            } else {
                cir.setReturnValue(InteractionResult.FAIL);
            }
        }
    }
}