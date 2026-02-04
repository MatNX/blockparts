package com.matnx.blockparts.statestore;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class StateStoreAssembler {
    private static final int GRID = 4;
    private static final double CELL = 1.0D / GRID;

    private StateStoreAssembler() {
    }

    public static void fillFromShape(StateStoreBlockEntity store, BlockState originalState, Level level, BlockPos pos,
                                     BlockState slabState, BlockState brickState, BlockState cubeState) {
        boolean[][][] occupied = new boolean[GRID][GRID][GRID];
        VoxelShape shape = originalState.getShape(level, pos, CollisionContext.empty());

        for (int x = 0; x < GRID; x++) {
            for (int y = 0; y < GRID; y++) {
                for (int z = 0; z < GRID; z++) {
                    double minX = x * CELL;
                    double minY = y * CELL;
                    double minZ = z * CELL;
                    double maxX = minX + CELL;
                    double maxY = minY + CELL;
                    double maxZ = minZ + CELL;
                    VoxelShape cell = Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
                    occupied[x][y][z] = Shapes.joinIsNotEmpty(shape, cell, BooleanOp.AND);
                }
            }
        }

        boolean[][][] used = new boolean[GRID][GRID][GRID];
        placeParts(store, occupied, used, brickState, 2, 4, 2);
        placeParts(store, occupied, used, cubeState, 2, 2, 2);
        placeParts(store, occupied, used, slabState, 2, 1, 2);
    }

    private static void placeParts(StateStoreBlockEntity store, boolean[][][] occupied, boolean[][][] used,
                                   BlockState state, int sizeX, int sizeY, int sizeZ) {
        for (int x = 0; x <= GRID - sizeX; x++) {
            for (int y = 0; y <= GRID - sizeY; y++) {
                for (int z = 0; z <= GRID - sizeZ; z++) {
                    if (!canPlace(occupied, used, x, y, z, sizeX, sizeY, sizeZ)) {
                        continue;
                    }
                    store.placeAt(x, y, z, state);
                    markUsed(used, x, y, z, sizeX, sizeY, sizeZ);
                }
            }
        }
    }

    private static boolean canPlace(boolean[][][] occupied, boolean[][][] used,
                                    int startX, int startY, int startZ,
                                    int sizeX, int sizeY, int sizeZ) {
        for (int x = startX; x < startX + sizeX; x++) {
            for (int y = startY; y < startY + sizeY; y++) {
                for (int z = startZ; z < startZ + sizeZ; z++) {
                    if (!occupied[x][y][z] || used[x][y][z]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void markUsed(boolean[][][] used, int startX, int startY, int startZ,
                                 int sizeX, int sizeY, int sizeZ) {
        for (int x = startX; x < startX + sizeX; x++) {
            for (int y = startY; y < startY + sizeY; y++) {
                for (int z = startZ; z < startZ + sizeZ; z++) {
                    used[x][y][z] = true;
                }
            }
        }
    }
}
