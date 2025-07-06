package com.matnx.blockparts.part;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Generic micro block used for slabs, bricks, cubes and similar parts.
 * The shape is defined by the size along the X/Y/Z axes for the default
 * {@link Direction.Axis#Y} orientation. Other orientations rotate the
 * sizes accordingly.
 */
public class PartBlock extends Block {
    public static final Direction.Axis DEFAULT_AXIS = Direction.Axis.Y;
    protected final int sizeX;
    protected final int sizeY;
    protected final int sizeZ;

    public PartBlock(int sizeX, int sizeY, int sizeZ, BlockBehaviour.Properties properties) {
        super(properties);
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.AXIS, DEFAULT_AXIS));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.AXIS, context.getClickedFace().getAxis());
    }

    private VoxelShape shapeFor(BlockState state) {
        Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);
        int x = sizeX;
        int y = sizeY;
        int z = sizeZ;
        if (axis == Direction.Axis.X) {
            x = sizeY; y = sizeX; z = sizeZ;
        } else if (axis == Direction.Axis.Z) {
            x = sizeX; y = sizeZ; z = sizeY;
        }
        return Block.box(0, 0, 0, x, y, z);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return Fluids.EMPTY.defaultFluidState();
    }
}
