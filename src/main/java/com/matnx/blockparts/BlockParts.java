package com.matnx.blockparts;

import com.matnx.blockparts.part.PartBlock;
import com.matnx.blockparts.part.TileBlock;
import com.matnx.blockparts.statestore.StateStoreAssembler;
import com.matnx.blockparts.statestore.StateStoreBlock;
import com.matnx.blockparts.statestore.StateStoreBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.neoforged.neoforge.registries.*;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.minecraft.world.level.Level;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(BlockParts.MODID)
public class BlockParts
{
    public static final String MODID = "blockparts";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredBlock<Block> STATE_STORE_BLOCK = BLOCKS.registerBlock("state_store_block", StateStoreBlock::new, BlockBehaviour.Properties.of());
    public static final Supplier<BlockEntityType<StateStoreBlockEntity>> STATE_STORE_BLOCK_ENTITY = BLOCK_ENTITIES.register("state_store_block_entity", () -> new BlockEntityType<>(StateStoreBlockEntity::new, STATE_STORE_BLOCK.get()));

    private static final Map<String, int[]> PART_SIZES = Map.of(
            "brick", new int[]{8, 16, 8},
            "cube", new int[]{8, 8, 8},
            "small_slab", new int[]{8, 4, 8},
            "small_brick", new int[]{4, 8, 4},
            "small_cube", new int[]{4, 4, 4},
            "plate", new int[]{16, 4, 16},
            "tile", new int[]{4, 16, 8},
            "rod", new int[]{4, 16, 4}
    );

    private static final String[] SLAB_VARIANTS = {
            "oak_slab", "spruce_slab", "birch_slab", "jungle_slab",
            "acacia_slab", "dark_oak_slab", "mangrove_slab", "cherry_slab",
            "bamboo_slab", "bamboo_mosaic_slab", "crimson_slab", "warped_slab",
            "stone_slab", "smooth_stone_slab", "cobblestone_slab", "mossy_cobblestone_slab",
            "stone_brick_slab", "mossy_stone_brick_slab", "brick_slab", "sandstone_slab",
            "cut_sandstone_slab", "smooth_sandstone_slab", "red_sandstone_slab",
            "cut_red_sandstone_slab", "smooth_red_sandstone_slab", "purpur_slab",
            "prismarine_slab", "prismarine_brick_slab", "dark_prismarine_slab",
            "nether_brick_slab", "red_nether_brick_slab", "quartz_slab", "smooth_quartz_slab",
            "polished_granite_slab", "granite_slab", "polished_diorite_slab", "diorite_slab",
            "polished_andesite_slab", "andesite_slab", "copper_slab", "exposed_copper_slab",
            "weathered_copper_slab", "oxidized_copper_slab", "waxed_copper_slab",
            "waxed_exposed_copper_slab", "waxed_weathered_copper_slab", "waxed_oxidized_copper_slab",
            "cut_copper_slab", "exposed_cut_copper_slab", "weathered_cut_copper_slab",
            "oxidized_cut_copper_slab", "waxed_cut_copper_slab", "waxed_exposed_cut_copper_slab",
            "waxed_weathered_cut_copper_slab", "waxed_oxidized_cut_copper_slab",
            "blackstone_slab", "polished_blackstone_slab", "polished_blackstone_brick_slab",
            "petrified_oak_slab", "mud_brick_slab", "deepslate_brick_slab", "end_stone_brick_slab",
            "deepslate_tile_slab", "cobbled_deepslate_slab", "polished_deepslate_slab"
    };

    public static final Map<String, DeferredBlock<Block>> PART_BLOCKS = new HashMap<>();

    static {
        for (String slab : SLAB_VARIANTS) {
            String material = slab.endsWith("_slab") ? slab.substring(0, slab.length() - 5) : slab;
            for (Map.Entry<String, int[]> entry : PART_SIZES.entrySet()) {
                int[] s = entry.getValue();
                String name = material + "_" + entry.getKey();
                DeferredBlock<Block> block = BLOCKS.registerBlock(name,
                        (props) -> ("tile".equals(entry.getKey())
                                ? new TileBlock(s[0], s[1], s[2], props)
                                : new PartBlock(s[0], s[1], s[2], props)),
                        BlockBehaviour.Properties.of());
                PART_BLOCKS.put(name, block);
                ITEMS.registerSimpleBlockItem(name, block, new Item.Properties());
            }
        }
    }

    public BlockParts(IEventBus modEventBus, ModContainer modContainer)
    {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
    }

    @Nullable
    public static BlockState getStateStorePlacementState(Block block, BlockPlaceContext context, BlockState originalState) {
        if (block instanceof SlabBlock) {
            BlockState slabState = getPartStateForBlock(block, "small_slab", context.getClickedFace().getAxis());
            if (slabState != null) {
                return slabState;
            }
        }
        if (block instanceof StairBlock) {
            BlockState cubeState = getPartStateForBlock(block, "cube", Direction.Axis.Y);
            if (cubeState != null) {
                return cubeState;
            }
        }
        return originalState;
    }

    @Nullable
    private static BlockState getPartStateForBlock(Block block, String partKey, Direction.Axis axis) {
        String material = getMaterialFromBlock(block);
        if (material == null) {
            return null;
        }
        DeferredBlock<Block> partBlock = PART_BLOCKS.get(material + "_" + partKey);
        if (partBlock == null) {
            return null;
        }
        BlockState partState = partBlock.get().defaultBlockState();
        if (partState.hasProperty(BlockStateProperties.AXIS)) {
            partState = partState.setValue(BlockStateProperties.AXIS, axis);
        }
        return partState;
    }

    @Nullable
    private static String getMaterialFromBlock(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        if (key == null) {
            return null;
        }
        String path = key.getPath();
        if (path.endsWith("_stairs")) {
            return path.substring(0, path.length() - "_stairs".length());
        }
        if (path.endsWith("_slab")) {
            return path.substring(0, path.length() - "_slab".length());
        }
        return null;
    }

    public static boolean hasReplacementParts(String material) {
        return PART_BLOCKS.containsKey(material + "_small_slab")
                && PART_BLOCKS.containsKey(material + "_brick")
                && PART_BLOCKS.containsKey(material + "_cube");
    }

    public static void replaceWithStateStore(Level level, BlockPos pos, BlockState originalState) {
        if (level.isClientSide) {
            return;
        }
        String material = getMaterialFromBlock(originalState.getBlock());
        if (material == null || !hasReplacementParts(material)) {
            return;
        }
        BlockState slabState = getPartStateForBlock(originalState.getBlock(), "small_slab", Direction.Axis.Y);
        BlockState brickState = getPartStateForBlock(originalState.getBlock(), "brick", Direction.Axis.Y);
        BlockState cubeState = getPartStateForBlock(originalState.getBlock(), "cube", Direction.Axis.Y);
        if (slabState == null || brickState == null || cubeState == null) {
            return;
        }
        BlockState stateStore = STATE_STORE_BLOCK.get().defaultBlockState();
        if (!level.setBlock(pos, stateStore, 11)) {
            return;
        }
        StateStoreBlockEntity store = level.getBlockEntity(pos) instanceof StateStoreBlockEntity entity ? entity : null;
        if (store == null) {
            return;
        }
        StateStoreAssembler.fillFromShape(store, originalState, level, pos, slabState, brickState, cubeState);
    }
}
