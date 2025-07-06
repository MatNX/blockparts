package com.matnx.blockparts.events;

import com.matnx.blockparts.statestore.StateStoreBlockEntityRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import static com.matnx.blockparts.BlockParts.MODID;
import static com.matnx.blockparts.BlockParts.STATE_STORE_BLOCK_ENTITY;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(STATE_STORE_BLOCK_ENTITY.get(), StateStoreBlockEntityRenderer::new);
    }
}
