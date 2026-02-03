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
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public TileBlock(int sizeX, int sizeY, int sizeZ, BlockBehaviour.Properties props) {
        super(sizeX, sizeY, sizeZ, props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected VoxelShape shapeFor(BlockState state) {
        Direction facing = state.getValue(FACING);
        int x = sizeX;
        int y = sizeY;
        int z = sizeZ;
        if (facing.getAxis() == Direction.Axis.X) {
            x = sizeY; y = sizeX; z = sizeZ;
        } else if (facing.getAxis() == Direction.Axis.Z) {
            x = sizeX; y = sizeZ; z = sizeY;
        }
        if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
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
