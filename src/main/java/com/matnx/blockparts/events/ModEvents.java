package com.matnx.blockparts.events;

import com.matnx.blockparts.BlockParts;
import com.matnx.blockparts.statestore.StateStoreBlockEntity;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.matnx.blockparts.BlockParts.MODID;
import static com.matnx.blockparts.BlockParts.STATE_STORE_BLOCK;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME)
public class ModEvents {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();

        Direction face = event.getFace();
        Vec3 hitVec = event.getHitVec().getLocation();
        BlockPos clickedPos = event.getPos();

        // Check if the face was on a full block edge (0 or 1) or in between (e.g. slab top)
        double axisValue = switch (face.getAxis()) {
            case X -> hitVec.x - clickedPos.getX();
            case Y -> hitVec.y - clickedPos.getY();
            case Z -> hitVec.z - clickedPos.getZ();
        };

        boolean isFullFace = axisValue == 0.0 || axisValue == 1.0;

        BlockPos targetPos = event.getPos();
        // Example usage (you can remove or adapt this as needed)
        if (isFullFace) targetPos = event.getPos().relative(event.getFace()); // Only proceed for full block face clicks

        BlockState state = level.getBlockState(targetPos);

        if (state.getBlock() != BlockParts.STATE_STORE_BLOCK.get()) return;

        BlockEntity be = level.getBlockEntity(targetPos);
        if (!(be instanceof StateStoreBlockEntity store)) return;

        ItemStack held = event.getItemStack();
        if (held.isEmpty() || !(held.getItem() instanceof BlockItem blockItem)) return;

        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(event.getEntity(), event.getHand(), event.getHitVec()));
        BlockState toPlace = blockItem.getBlock().getStateForPlacement(context);
        InteractionResult result = store.tryAddBlock(toPlace, event.getHitVec(), event.getEntity(), event.getHand());

        if (!result.consumesAction()) return;

        SoundType soundtype = toPlace.getSoundType(level, targetPos, event.getEntity());
        level.playSound(event.getEntity(), targetPos, soundtype.getPlaceSound(), SoundSource.BLOCKS,
                (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);

        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, targetPos, held);
        }

        level.gameEvent(GameEvent.BLOCK_PLACE, targetPos, GameEvent.Context.of(event.getEntity(), toPlace));

        if (!event.getEntity().getAbilities().instabuild) {
            held.shrink(1);
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

}

