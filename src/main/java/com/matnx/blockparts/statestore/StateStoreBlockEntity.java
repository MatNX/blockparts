package com.matnx.blockparts.statestore;

import com.matnx.blockparts.BlockParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A {@link BlockEntity} that can store up to 64 block states inside a single
 * world block. A state can be placed as long as its (translated) voxel shape
 * does not overlap with the shapes that are already present.
 */
public class StateStoreBlockEntity extends BlockEntity {
    private static final int SIZE = 4; // 4 × 4 × 4 grid → 64 micro‐blocks

    private final BlockState[][][] storedStates = new BlockState[SIZE][SIZE][SIZE];

    /**
     * Cumulative shape of everything currently stored, kept up‑to‑date via
     * {@link #recalculateShape()} so we can perform cheap overlap tests.
     */
    private VoxelShape shape = Shapes.empty();

    public StateStoreBlockEntity(BlockPos pos, BlockState state) {
        super(BlockParts.STATE_STORE_BLOCK_ENTITY.get(), pos, state);
        initializeStates();
    }

    /* --------------------------------------------------------------------- */
    /*                             Serialisation                             */
    /* --------------------------------------------------------------------- */

    private void initializeStates() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    storedStates[x][y][z] = Blocks.AIR.defaultBlockState();
                }
            }
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ListTag statesList = tag.getList("StoredStates", Tag.TAG_COMPOUND);
        for (int i = 0; i < statesList.size(); i++) {
            CompoundTag stateTag = statesList.getCompound(i);
            int x = stateTag.getInt("X");
            int y = stateTag.getInt("Y");
            int z = stateTag.getInt("Z");
            BlockState blockState = BlockState.CODEC.parse(NbtOps.INSTANCE, stateTag.get("State")).getOrThrow();
            if (inBounds(x, y, z)) {
                storedStates[x][y][z] = blockState;
            }
        }
        recalculateShape();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag statesList = new ListTag();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    CompoundTag stateTag = new CompoundTag();
                    stateTag.putInt("X", x);
                    stateTag.putInt("Y", y);
                    stateTag.putInt("Z", z);

                    BlockState blockState = storedStates[x][y][z];
                    BlockState.CODEC.encodeStart(NbtOps.INSTANCE, blockState)
                            .resultOrPartial(e -> System.out.println("Failed to serialize BlockState: " + e))
                            .ifPresent(serialized -> stateTag.put("State", serialized));

                    statesList.add(stateTag);
                }
            }
        }
        tag.put("StoredStates", statesList);
    }

    /* --------------------------------------------------------------------- */
    /*                              Placement                                */
    /* --------------------------------------------------------------------- */

    /**
     * @return {@code true} if the target slot is free <strong>and</strong> the
     *         voxel shape of the given {@code state} does not intersect with the
     *         shapes that are already present inside this block.
     */
    public boolean canPlaceAt(int x, int y, int z, BlockState state) {
        if (!inBounds(x, y, z)) return false;
        if (!isAir(storedStates[x][y][z])) return false;

        VoxelShape candidate = translatedShape(x, y, z, state);
        // Intersection test: if AND is not empty, they overlap → placement denied
        boolean intersects = Shapes.joinIsNotEmpty(this.shape, candidate, BooleanOp.AND);
        return !intersects;
    }

    /**
     * Places a block state at the given cell <em>if</em> it fits. This will
     * automatically update the stored shape and inform clients via block
     * updates.
     */
    public void placeAt(int x, int y, int z, BlockState state) {
        if (!canPlaceAt(x, y, z, state)) return;

        storedStates[x][y][z] = state;
        recalculateShape();
        setChanged();

        if (this.level != null) {
            this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Removes the stored state from the given slot, if any, and updates the
     * cached shape.
     */
    public void removeStoredState(int x, int y, int z) {
        if (!inBounds(x, y, z)) return;
        if (isAir(storedStates[x][y][z])) return;

        storedStates[x][y][z] = Blocks.AIR.defaultBlockState();
        recalculateShape();
        setChanged();
    }

    public InteractionResult tryAddBlock(BlockState state, BlockHitResult hit, Player player, InteractionHand hand) {
        VoxelShape voxelShape = state.getShape(this.getLevel(), this.getBlockPos());
        Vec3 clickPos = hit.getLocation().subtract(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());

        Direction dir = hit.getDirection();
        clickPos = clickPos.add(
                dir.getStepX() * 0.001,
                dir.getStepY() * 0.001,
                dir.getStepZ() * 0.001
        );

        int x = getSnappedSlot(voxelShape.min(Direction.Axis.X), voxelShape.max(Direction.Axis.X), clickPos.x);
        int y = getSnappedSlot(voxelShape.min(Direction.Axis.Y), voxelShape.max(Direction.Axis.Y), clickPos.y);
        int z = getSnappedSlot(voxelShape.min(Direction.Axis.Z), voxelShape.max(Direction.Axis.Z), clickPos.z);

        if (canPlaceAt(x, y, z, state)) {
            placeAt(x, y, z, state);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    public InteractionResult tryAddBlockOnSurface(BlockState state, BlockHitResult hit, Player player, InteractionHand hand) {
        VoxelShape voxelShape = state.getShape(this.getLevel(), this.getBlockPos());
        Vec3 clickPos = hit.getLocation().subtract(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());

        Direction dir = hit.getDirection();
        clickPos = clickPos.add(
                dir.getStepX() * 0.001,
                dir.getStepY() * 0.001,
                dir.getStepZ() * 0.001
        );

        int x = getSnappedSlot(voxelShape.min(Direction.Axis.X), voxelShape.max(Direction.Axis.X), clickPos.x);
        int y = getSnappedSlot(voxelShape.min(Direction.Axis.Y), voxelShape.max(Direction.Axis.Y), clickPos.y);
        int z = getSnappedSlot(voxelShape.min(Direction.Axis.Z), voxelShape.max(Direction.Axis.Z), clickPos.z);

        if (dir.getAxis() == Direction.Axis.X) {
            x = getSurfaceSlot(voxelShape.min(Direction.Axis.X), voxelShape.max(Direction.Axis.X), dir);
        } else if (dir.getAxis() == Direction.Axis.Y) {
            y = getSurfaceSlot(voxelShape.min(Direction.Axis.Y), voxelShape.max(Direction.Axis.Y), dir);
        } else if (dir.getAxis() == Direction.Axis.Z) {
            z = getSurfaceSlot(voxelShape.min(Direction.Axis.Z), voxelShape.max(Direction.Axis.Z), dir);
        }

        if (canPlaceAt(x, y, z, state)) {
            placeAt(x, y, z, state);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    public static int getSnappedSlot(double min, double max, double click) {
        double size = max - min;
        int slots = 4;
        double slotSize = 1.0 / slots;

        if (size % slotSize != 0) {
            throw new IllegalArgumentException("Block size must be a multiple of 0.25");
        }

        int step = (int)(size / slotSize); // how many slots the block spans
        int snappedSlot = -1;
        double minDistance = Double.MAX_VALUE;

        // Only consider starting indices that are multiples of `step`
        for (int i = 0; i <= slots - step; i++) {
            if (i % step != 0) continue; // ensure tiling compatibility

            double slotStart = i * slotSize;
            double slotEnd = slotStart + size;
            double center = (slotStart + slotEnd) / 2.0;
            double distance = Math.abs(center - click);
            if (distance < minDistance) {
                minDistance = distance;
                snappedSlot = i;
            }
        }

        return snappedSlot;
    }

    public static int getSurfaceSlot(double min, double max, Direction face) {
        double size = max - min;
        int slots = 4;
        double slotSize = 1.0 / slots;

        if (size % slotSize != 0) {
            throw new IllegalArgumentException("Block size must be a multiple of 0.25");
        }

        int step = (int)(size / slotSize);
        return face.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 0 : slots - step;
    }

    /* --------------------------------------------------------------------- */
    /*                            Shape helpers                              */
    /* --------------------------------------------------------------------- */

    /**
     * Rebuilds {@link #shape} from scratch. Simpler and less error–prone than
     * trying to surgically remove/merge shapes.
     */
    private void recalculateShape() {
        VoxelShape combined = Shapes.empty();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    BlockState state = storedStates[x][y][z];
                    if (!isAir(state)) {
                        combined = Shapes.join(combined, translatedShape(x, y, z, state), BooleanOp.OR);
                    }
                }
            }
        }
        this.shape = combined;
    }

    public VoxelShape getShape() {
        return shape;
    }

    /**
     * Translates the voxel shape of {@code state} so that it fits into the slot
     * at (x, y, z).
     */
    private VoxelShape translatedShape(int x, int y, int z, BlockState state) {
        double cell = 1.0D / SIZE; // 0.25 for SIZE = 4
        return state.getShape(this.getLevel(), this.getBlockPos()).move(x * cell, y * cell, z * cell);
    }

    /* --------------------------------------------------------------------- */
    /*                               Misc                                    */
    /* --------------------------------------------------------------------- */

    public BlockState getStoredState(int x, int y, int z) {
        return inBounds(x, y, z) ? storedStates[x][y][z] : Blocks.AIR.defaultBlockState();
    }

    private static boolean isAir(BlockState state) {
        return state.getBlock() == Blocks.AIR;
    }

    private static boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE;
    }

    /* --------------------------------------------------------------------- */
    /*                     Networking / synchronisation                      */
    /* --------------------------------------------------------------------- */

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, lookup);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookup) {
        loadAdditional(tag, lookup);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}