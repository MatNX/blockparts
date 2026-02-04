package com.matnx.blockparts.replacement;

import com.matnx.blockparts.BlockParts;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.HashSet;
import java.util.Set;

import static com.matnx.blockparts.BlockParts.MODID;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
public class ReplacementRegistry {
    private static final Set<ResourceLocation> REPLACED_KEYS = new HashSet<>();

    @SubscribeEvent
    public static void registerReplacements(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            for (ResourceLocation key : BuiltInRegistries.BLOCK.keySet()) {
                if (!"minecraft".equals(key.getNamespace())) {
                    continue;
                }
                String path = key.getPath();
                String material = null;
                boolean isSlab = false;
                if (path.endsWith("_slab")) {
                    material = path.substring(0, path.length() - "_slab".length());
                    isSlab = true;
                } else if (path.endsWith("_stairs")) {
                    material = path.substring(0, path.length() - "_stairs".length());
                }
                if (material == null || !BlockParts.hasReplacementParts(material)) {
                    continue;
                }
                Block original = BuiltInRegistries.BLOCK.get(key);
                BlockBehaviour.Properties properties = BlockBehaviour.Properties.copy(original);
                if (isSlab) {
                    helper.register(key, () -> new ReplacementSlabBlock(properties));
                } else {
                    helper.register(key, () -> new ReplacementStairBlock(Blocks.STONE.defaultBlockState(), properties));
                }
                REPLACED_KEYS.add(key);
            }
        });

        event.register(Registries.ITEM, helper -> {
            for (ResourceLocation key : REPLACED_KEYS) {
                Block block = BuiltInRegistries.BLOCK.get(key);
                helper.register(key, () -> new BlockItem(block, new Item.Properties()));
            }
        });
    }
}
