package com.matnx.blockparts;

import com.matnx.blockparts.part.PartBlock;
import com.matnx.blockparts.part.TileBlock;
import com.matnx.blockparts.statestore.StateStoreBlock;
import com.matnx.blockparts.statestore.StateStoreBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

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
        if (block instanceof StairBlock) {
            BlockState cubeState = getCubeStateForStair(block, context);
            if (cubeState != null) {
                return cubeState;
            }
        }
        return originalState;
    }

    @Nullable
    private static BlockState getCubeStateForStair(Block block, BlockPlaceContext context) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        if (key == null) {
            return null;
        }
        String path = key.getPath();
        if (!path.endsWith("_stairs")) {
            return null;
        }
        String material = path.substring(0, path.length() - "_stairs".length());
        DeferredBlock<Block> cubeBlock = PART_BLOCKS.get(material + "_cube");
        if (cubeBlock == null) {
            return null;
        }
        BlockState cubeState = cubeBlock.get().getStateForPlacement(context);
        return cubeState != null ? cubeState : cubeBlock.get().defaultBlockState();
    }

    public static boolean replaceStairWithStateStore(LevelAccessor level, BlockPos pos, BlockState stairState) {
        if (level.isClientSide()) {
            return false;
        }
        Block stairBlock = stairState.getBlock();
        if (!(stairBlock instanceof StairBlock)) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(stairBlock);
        if (key == null) {
            return false;
        }
        String path = key.getPath();
        if (!path.endsWith("_stairs")) {
            return false;
        }
        String material = path.substring(0, path.length() - "_stairs".length());
        DeferredBlock<Block> cubeBlock = PART_BLOCKS.get(material + "_small_cube");
        if (cubeBlock == null) {
            return false;
        }
        BlockState cubeState = cubeBlock.get().defaultBlockState();
        if (!level.setBlock(pos, STATE_STORE_BLOCK.get().defaultBlockState(), 11)) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof StateStoreBlockEntity store)) {
            return false;
        }
        VoxelShape stairShape = stairState.getShape(level, pos);
        double cell = 1.0D / 4.0D;
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    double minX = x * cell;
                    double minY = y * cell;
                    double minZ = z * cell;
                    VoxelShape cellShape = Shapes.box(minX, minY, minZ, minX + cell, minY + cell, minZ + cell);
                    if (Shapes.joinIsNotEmpty(stairShape, cellShape, BooleanOp.AND)) {
                        store.placeAt(x, y, z, cubeState);
                    }
                }
            }
        }
        return true;
    }
}
