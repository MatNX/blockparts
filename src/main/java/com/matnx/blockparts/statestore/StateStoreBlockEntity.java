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
    private static final double SLOT_SIZE = 1.0D / SIZE;
    private static final double EPSILON = 1.0E-6D;

    private final BlockState[][][] storedStates = new BlockState[SIZE][SIZE][SIZE];
    private final boolean[][][] occupiedSlots = new boolean[SIZE][SIZE][SIZE];

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

    private void initializeOccupiedSlots() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    occupiedSlots[x][y][z] = false;
                }
            }
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        initializeStates();
        initializeOccupiedSlots();
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
        ListTag occupiedList = tag.getList("OccupiedSlots", Tag.TAG_COMPOUND);
        for (int i = 0; i < occupiedList.size(); i++) {
            CompoundTag occupiedTag = occupiedList.getCompound(i);
            int x = occupiedTag.getInt("X");
            int y = occupiedTag.getInt("Y");
            int z = occupiedTag.getInt("Z");
            if (inBounds(x, y, z)) {
                occupiedSlots[x][y][z] = true;
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
        ListTag occupiedList = new ListTag();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    if (!occupiedSlots[x][y][z]) {
                        continue;
                    }
                    CompoundTag occupiedTag = new CompoundTag();
                    occupiedTag.putInt("X", x);
                    occupiedTag.putInt("Y", y);
                    occupiedTag.putInt("Z", z);
                    occupiedList.add(occupiedTag);
                }
            }
        }
        tag.put("OccupiedSlots", occupiedList);
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
        if (!isAir(storedStates[x][y][z]) || occupiedSlots[x][y][z]) return false;

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
        clickPos = clampClickPos(clickPos);

        int x = getSnappedSlot(voxelShape.min(Direction.Axis.X), voxelShape.max(Direction.Axis.X), clickPos.x);
        int y = getSnappedSlot(voxelShape.min(Direction.Axis.Y), voxelShape.max(Direction.Axis.Y), clickPos.y);
        int z = getSnappedSlot(voxelShape.min(Direction.Axis.Z), voxelShape.max(Direction.Axis.Z), clickPos.z);

        PlacementTarget target = resolvePlacementTarget(x, y, z);
        if (target == null) {
            return InteractionResult.FAIL;
        }
        return placeWithTarget(target, state, voxelShape);
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
        clickPos = clampClickPos(clickPos);

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

        PlacementTarget target = resolvePlacementTarget(x, y, z);
        if (target == null) {
            return InteractionResult.FAIL;
        }
        return placeWithTarget(target, state, voxelShape);
    }

    public static int getSnappedSlot(double min, double max, double click) {
        double size = max - min;
        int slots = SIZE;
        int step = sizeToSlots(size); // how many slots the block spans
        int snapStep = getSnapStep(step);
        int snappedSlot = 0;
        double minDistance = Double.MAX_VALUE;
        int startMin = -(step - 1);
        int startMax = slots - 1;

        // Only consider starting indices that are multiples of `step`
        for (int i = startMin; i <= startMax; i++) {
            if (Math.floorMod(i, snapStep) != 0) continue; // ensure tiling compatibility

            double slotStart = i * SLOT_SIZE;
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
        int step = sizeToSlots(size);
        return face.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? -step : SIZE - step;
    }

    private static int getSnapStep(int step) {
        return Math.max(1, step / 2);
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
                    } else if (occupiedSlots[x][y][z]) {
                        combined = Shapes.join(combined, slotShape(x, y, z), BooleanOp.OR);
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
        return state.getShape(this.getLevel(), this.getBlockPos()).move(x * SLOT_SIZE, y * SLOT_SIZE, z * SLOT_SIZE);
    }

    private static VoxelShape slotShape(int x, int y, int z) {
        return Shapes.box(x * SLOT_SIZE, y * SLOT_SIZE, z * SLOT_SIZE, (x + 1) * SLOT_SIZE, (y + 1) * SLOT_SIZE, (z + 1) * SLOT_SIZE);
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

    private boolean isSlotFree(int x, int y, int z) {
        return inBounds(x, y, z) && isAir(storedStates[x][y][z]) && !occupiedSlots[x][y][z];
    }

    private void markSlotOccupied(int x, int y, int z) {
        if (!inBounds(x, y, z)) return;
        occupiedSlots[x][y][z] = true;
    }

    private InteractionResult placeWithOverflow(BlockState state, VoxelShape voxelShape, int x, int y, int z) {
        if (this.level == null) {
            return InteractionResult.FAIL;
        }

        int stepX = getStepForSize(voxelShape.min(Direction.Axis.X), voxelShape.max(Direction.Axis.X));
        int stepY = getStepForSize(voxelShape.min(Direction.Axis.Y), voxelShape.max(Direction.Axis.Y));
        int stepZ = getStepForSize(voxelShape.min(Direction.Axis.Z), voxelShape.max(Direction.Axis.Z));

        AxisSegment[] xSegments = getAxisSegments(x, stepX);
        AxisSegment[] ySegments = getAxisSegments(y, stepY);
        AxisSegment[] zSegments = getAxisSegments(z, stepZ);

        VoxelShape candidate = translatedShape(x, y, z, state);
        if (Shapes.joinIsNotEmpty(this.shape, candidate, BooleanOp.AND)) {
            return InteractionResult.FAIL;
        }

        StoreReservations reservations = new StoreReservations();
        for (AxisSegment xSegment : xSegments) {
            for (AxisSegment ySegment : ySegments) {
                for (AxisSegment zSegment : zSegments) {
                    BlockPos targetPos = worldPosition.offset(xSegment.offset, ySegment.offset, zSegment.offset);
                    StoreReservation reservation = xSegment.offset == 0 && ySegment.offset == 0 && zSegment.offset == 0
                            ? new StoreReservation(targetPos, this, null)
                            : reservations.getOrReserve(targetPos);
                    if (reservation == null) {
                        return InteractionResult.FAIL;
                    }
                    if (reservation.store != null && !areSlotsFree(reservation.store, xSegment, ySegment, zSegment)) {
                        return InteractionResult.FAIL;
                    }
                }
            }
        }

        if (!reservations.ensureCreated(this.level)) {
            return InteractionResult.FAIL;
        }

        placeAt(x, y, z, state);

        for (AxisSegment xSegment : xSegments) {
            for (AxisSegment ySegment : ySegments) {
                for (AxisSegment zSegment : zSegments) {
                    if (xSegment.offset == 0 && ySegment.offset == 0 && zSegment.offset == 0) {
                        continue;
                    }
                    BlockPos targetPos = worldPosition.offset(xSegment.offset, ySegment.offset, zSegment.offset);
                    StateStoreBlockEntity targetStore = reservations.getStore(targetPos);
                    if (targetStore != null) {
                        targetStore.markSlotsOccupied(xSegment, ySegment, zSegment);
                    }
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    private int getStepForSize(double min, double max) {
        double size = max - min;
        return sizeToSlots(size);
    }

    private AxisSegment[] getAxisSegments(int start, int step) {
        int overflow = start + step - SIZE;
        int underflow = -start;
        if (overflow <= 0 && underflow <= 0) {
            return new AxisSegment[] { new AxisSegment(0, start, step) };
        }
        if (overflow > 0) {
            return new AxisSegment[] {
                    new AxisSegment(0, start, step - overflow),
                    new AxisSegment(1, 0, overflow)
            };
        }
        return new AxisSegment[] {
                new AxisSegment(0, 0, step - underflow),
                new AxisSegment(-1, SIZE - underflow, underflow)
        };
    }

    private PlacementTarget resolvePlacementTarget(int x, int y, int z) {
        int offsetX = 0;
        int offsetY = 0;
        int offsetZ = 0;
        if (x < 0) {
            offsetX = -1;
            x += SIZE;
        } else if (x >= SIZE) {
            offsetX = 1;
            x -= SIZE;
        }
        if (y < 0) {
            offsetY = -1;
            y += SIZE;
        } else if (y >= SIZE) {
            offsetY = 1;
            y -= SIZE;
        }
        if (z < 0) {
            offsetZ = -1;
            z += SIZE;
        } else if (z >= SIZE) {
            offsetZ = 1;
            z -= SIZE;
        }
        if (offsetX == 0 && offsetY == 0 && offsetZ == 0) {
            return new PlacementTarget(this, worldPosition, x, y, z);
        }
        BlockPos targetPos = worldPosition.offset(offsetX, offsetY, offsetZ);
        StateStoreBlockEntity targetStore = getExistingStore(targetPos);
        if (targetStore == null && !canCreateStore(targetPos)) {
            return null;
        }
        return new PlacementTarget(targetStore, targetPos, x, y, z);
    }

    private boolean areSlotsFree(StateStoreBlockEntity targetStore, AxisSegment xSegment, AxisSegment ySegment, AxisSegment zSegment) {
        for (int xi = xSegment.start; xi < xSegment.start + xSegment.length; xi++) {
            for (int yi = ySegment.start; yi < ySegment.start + ySegment.length; yi++) {
                for (int zi = zSegment.start; zi < zSegment.start + zSegment.length; zi++) {
                    if (!targetStore.isSlotFree(xi, yi, zi)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void markSlotsOccupied(AxisSegment xSegment, AxisSegment ySegment, AxisSegment zSegment) {
        for (int xi = xSegment.start; xi < xSegment.start + xSegment.length; xi++) {
            for (int yi = ySegment.start; yi < ySegment.start + ySegment.length; yi++) {
                for (int zi = zSegment.start; zi < zSegment.start + zSegment.length; zi++) {
                    markSlotOccupied(xi, yi, zi);
                }
            }
        }
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private StateStoreBlockEntity getExistingStore(BlockPos pos) {
        if (this.level == null) {
            return null;
        }
        BlockEntity entity = this.level.getBlockEntity(pos);
        return entity instanceof StateStoreBlockEntity store ? store : null;
    }

    private boolean canCreateStore(BlockPos pos) {
        if (this.level == null) {
            return false;
        }
        BlockEntity entity = this.level.getBlockEntity(pos);
        if (entity != null) {
            return false;
        }
        BlockState state = this.level.getBlockState(pos);
        return state.canBeReplaced();
    }

    private StoreReservation createStore(BlockPos pos) {
        if (this.level == null) {
            return null;
        }
        BlockState previousState = this.level.getBlockState(pos);
        if (!previousState.canBeReplaced() || this.level.getBlockEntity(pos) != null) {
            return null;
        }
        if (!this.level.setBlock(pos, BlockParts.STATE_STORE_BLOCK.get().defaultBlockState(), 11)) {
            return null;
        }
        BlockEntity newEntity = this.level.getBlockEntity(pos);
        if (newEntity instanceof StateStoreBlockEntity store) {
            return new StoreReservation(pos, store, previousState);
        }
        this.level.setBlock(pos, previousState, 11);
        return null;
    }

    private InteractionResult placeWithTarget(PlacementTarget target, BlockState state, VoxelShape voxelShape) {
        StateStoreBlockEntity targetStore = target.store;
        StoreReservation created = null;
        if (targetStore == null) {
            created = createStore(target.pos);
            if (created == null) {
                return InteractionResult.FAIL;
            }
            targetStore = created.store;
        }
        InteractionResult result = targetStore.placeWithOverflow(state, voxelShape, target.x, target.y, target.z);
        if (result != InteractionResult.SUCCESS && created != null && this.level != null) {
            this.level.setBlock(created.pos, created.previousState, 11);
        }
        return result;
    }

    private static Vec3 clampClickPos(Vec3 clickPos) {
        return new Vec3(
                clampCoord(clickPos.x),
                clampCoord(clickPos.y),
                clampCoord(clickPos.z)
        );
    }

    private static double clampCoord(double value) {
        if (value < EPSILON) {
            return EPSILON;
        }
        if (value > 1.0D - EPSILON) {
            return 1.0D - EPSILON;
        }
        return value;
    }

    private static int sizeToSlots(double size) {
        double slotsExact = size / SLOT_SIZE;
        int slots = (int) Math.round(slotsExact);
        if (Math.abs(slotsExact - slots) > 1.0E-5D) {
            throw new IllegalArgumentException("Block size must be a multiple of 0.25");
        }
        return slots;
    }

    private static class AxisSegment {
        private final int offset;
        private final int start;
        private final int length;

        private AxisSegment(int offset, int start, int length) {
            this.offset = offset;
            this.start = start;
            this.length = length;
        }
    }

    private static class PlacementTarget {
        private final StateStoreBlockEntity store;
        private final BlockPos pos;
        private final int x;
        private final int y;
        private final int z;

        private PlacementTarget(StateStoreBlockEntity store, BlockPos pos, int x, int y, int z) {
            this.store = store;
            this.pos = pos;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static class StoreReservation {
        private final BlockPos pos;
        private final StateStoreBlockEntity store;
        private final BlockState previousState;

        private StoreReservation(BlockPos pos, StateStoreBlockEntity store, BlockState previousState) {
            this.pos = pos;
            this.store = store;
            this.previousState = previousState;
        }
    }

    private class StoreReservations {
        private final java.util.Map<BlockPos, StoreReservation> reservations = new java.util.HashMap<>();
        private final java.util.List<StoreReservation> created = new java.util.ArrayList<>();

        private StoreReservation getOrReserve(BlockPos pos) {
            if (pos.equals(worldPosition)) {
                return new StoreReservation(pos, StateStoreBlockEntity.this, null);
            }
            StoreReservation existing = reservations.get(pos);
            if (existing != null) {
                return existing;
            }
            StateStoreBlockEntity store = getExistingStore(pos);
            if (store != null) {
                StoreReservation reservation = new StoreReservation(pos, store, null);
                reservations.put(pos, reservation);
                return reservation;
            }
            if (!canCreateStore(pos)) {
                return null;
            }
            StoreReservation reservation = new StoreReservation(pos, null, null);
            reservations.put(pos, reservation);
            return reservation;
        }

        private boolean ensureCreated(net.minecraft.world.level.Level level) {
            for (StoreReservation reservation : reservations.values()) {
                if (reservation.store != null) {
                    continue;
                }
                StoreReservation createdStore = createStore(reservation.pos);
                if (createdStore == null) {
                    rollback(level);
                    return false;
                }
                created.add(createdStore);
                reservations.put(reservation.pos, createdStore);
            }
            return true;
        }

        private StateStoreBlockEntity getStore(BlockPos pos) {
            StoreReservation reservation = reservations.get(pos);
            if (reservation == null) {
                return null;
            }
            return reservation.store;
        }

        private void rollback(net.minecraft.world.level.Level level) {
            for (int i = created.size() - 1; i >= 0; i--) {
                StoreReservation reservation = created.get(i);
                level.setBlock(reservation.pos, reservation.previousState, 11);
            }
            created.clear();
        }
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
