package com.matnx.blockparts.part;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Specialized part block for tiles that rotates around the longest axis
 * when placed on a negative facing side.
 */
public class TileBlock extends PartBlock {
    public static final EnumProperty<Direction.AxisDirection> AXIS_DIR =
            EnumProperty.create("axis_dir", Direction.AxisDirection.class);

    public TileBlock(int sizeX, int sizeY, int sizeZ, BlockBehaviour.Properties props) {
        super(sizeX, sizeY, sizeZ, props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.AXIS, DEFAULT_AXIS)
                .setValue(AXIS_DIR, Direction.AxisDirection.POSITIVE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS_DIR);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(BlockStateProperties.AXIS, context.getClickedFace().getAxis())
                .setValue(AXIS_DIR, context.getClickedFace().getAxisDirection());
    }

    @Override
    protected VoxelShape shapeFor(BlockState state) {
        Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);
        Direction.AxisDirection dir = state.getValue(AXIS_DIR);
        int x = sizeX;
        int y = sizeY;
        int z = sizeZ;
        if (axis == Direction.Axis.X) {
            x = sizeY; y = sizeX; z = sizeZ;
        } else if (axis == Direction.Axis.Z) {
            x = sizeX; y = sizeZ; z = sizeY;
        }
        if (dir == Direction.AxisDirection.NEGATIVE) {
            if (x >= y && x >= z) {
                int t = y; y = z; z = t;
            } else if (y >= x && y >= z) {
                int t = x; x = z; z = t;
            } else {
                int t = x; x = y; y = t;
            }
        }
        return Block.box(0, 0, 0, x, y, z);
    }
}
