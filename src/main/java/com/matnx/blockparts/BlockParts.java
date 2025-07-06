package com.matnx.blockparts;

import com.matnx.blockparts.part.PartBlock;
import com.matnx.blockparts.statestore.StateStoreBlock;
import com.matnx.blockparts.statestore.StateStoreBlockEntity;
import net.neoforged.neoforge.registries.*;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(BlockParts.MODID)
public class BlockParts
{
    public static final String MODID = "blockparts";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredBlock<Block> STATE_STORE_BLOCK = BLOCKS.registerBlock("state_store_block", StateStoreBlock::new, BlockBehaviour.Properties.of());
    public static final Supplier<BlockEntityType<StateStoreBlockEntity>> STATE_STORE_BLOCK_ENTITY = BLOCK_ENTITIES.register("state_store_block_entity", () -> new BlockEntityType<>(StateStoreBlockEntity::new, STATE_STORE_BLOCK.get()));

    public static final DeferredBlock<Block> BRICK = BLOCKS.registerBlock("brick", () -> new PartBlock(8, 16, 8, BlockBehaviour.Properties.of()), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> CUBE = BLOCKS.registerBlock("cube", () -> new PartBlock(8, 8, 8, BlockBehaviour.Properties.of()), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> SMALL_SLAB = BLOCKS.registerBlock("small_slab", () -> new PartBlock(8, 4, 8, BlockBehaviour.Properties.of()), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> SMALL_BRICK = BLOCKS.registerBlock("small_brick", () -> new PartBlock(4, 8, 4, BlockBehaviour.Properties.of()), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> SMALL_CUBE = BLOCKS.registerBlock("small_cube", () -> new PartBlock(4, 4, 4, BlockBehaviour.Properties.of()), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> PLATE = BLOCKS.registerBlock("plate", () -> new PartBlock(16, 4, 16, BlockBehaviour.Properties.of()), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> TILE = BLOCKS.registerBlock("tile", () -> new PartBlock(16, 4, 8, BlockBehaviour.Properties.of()), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> ROD = BLOCKS.registerBlock("rod", () -> new PartBlock(4, 16, 4, BlockBehaviour.Properties.of()), BlockBehaviour.Properties.of());

    public BlockParts(IEventBus modEventBus, ModContainer modContainer)
    {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
    }
}
