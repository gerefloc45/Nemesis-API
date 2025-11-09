package com.gerefloc45.voidapi.api.perception;

import com.gerefloc45.voidapi.api.BehaviorContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Sensor for detecting specific blocks in the environment.
 * Supports pattern matching and area scanning.
 * 
 * @author VoidAPI Framework
 * @version 1.1.0
 */
public class BlockSensor implements Sensor {
    private final double range;
    private final String blackboardKey;
    private final int updateFrequency;
    private final Predicate<BlockState> blockFilter;
    private final ScanPattern scanPattern;

    /**
     * Scan pattern types for block detection.
     */
    public enum ScanPattern {
        /** Scan all blocks in range (expensive) */
        FULL_SPHERE,
        /** Scan only horizontal plane around entity */
        HORIZONTAL_PLANE,
        /** Scan only in cardinal directions */
        CARDINAL_DIRECTIONS,
        /** Scan in a cone in front of entity */
        FORWARD_CONE
    }

    /**
     * Creates a new block sensor.
     *
     * @param range Detection range in blocks
     * @param blackboardKey Key to store detected block positions in blackboard
     * @param blockFilter Predicate to filter which blocks to detect
     */
    public BlockSensor(double range, String blackboardKey, Predicate<BlockState> blockFilter) {
        this(range, blackboardKey, blockFilter, ScanPattern.HORIZONTAL_PLANE, 20);
    }

    /**
     * Creates a new block sensor with full configuration.
     *
     * @param range Detection range in blocks
     * @param blackboardKey Key to store detected block positions in blackboard
     * @param blockFilter Predicate to filter which blocks to detect
     * @param scanPattern Pattern to use for scanning
     * @param updateFrequency Update frequency in ticks
     */
    public BlockSensor(double range, String blackboardKey, Predicate<BlockState> blockFilter,
                      ScanPattern scanPattern, int updateFrequency) {
        this.range = range;
        this.blackboardKey = blackboardKey;
        this.blockFilter = blockFilter;
        this.scanPattern = scanPattern;
        this.updateFrequency = updateFrequency;
    }

    @Override
    public void update(BehaviorContext context) {
        LivingEntity entity = context.getEntity();
        World world = entity.getWorld();
        BlockPos entityPos = entity.getBlockPos();

        List<BlockPos> detectedBlocks = scanForBlocks(world, entityPos);

        // Store in blackboard
        context.getBlackboard().set(blackboardKey, detectedBlocks);
        context.getBlackboard().set(blackboardKey + "_count", detectedBlocks.size());
        
        // Store nearest block if any detected
        if (!detectedBlocks.isEmpty()) {
            BlockPos nearest = findNearestBlock(entityPos, detectedBlocks);
            context.getBlackboard().set(blackboardKey + "_nearest", nearest);
        }
    }

    /**
     * Scans for blocks based on the configured pattern.
     *
     * @param world The world to scan in
     * @param center The center position to scan from
     * @return List of matching block positions
     */
    private List<BlockPos> scanForBlocks(World world, BlockPos center) {
        List<BlockPos> foundBlocks = new ArrayList<>();
        
        switch (scanPattern) {
            case FULL_SPHERE:
                foundBlocks = scanFullSphere(world, center);
                break;
            case HORIZONTAL_PLANE:
                foundBlocks = scanHorizontalPlane(world, center);
                break;
            case CARDINAL_DIRECTIONS:
                foundBlocks = scanCardinalDirections(world, center);
                break;
            case FORWARD_CONE:
                foundBlocks = scanForwardCone(world, center);
                break;
        }

        return foundBlocks;
    }

    /**
     * Scans all blocks in a spherical range.
     */
    private List<BlockPos> scanFullSphere(World world, BlockPos center) {
        List<BlockPos> foundBlocks = new ArrayList<>();
        int rangeInt = (int) Math.ceil(range);

        for (int x = -rangeInt; x <= rangeInt; x++) {
            for (int y = -rangeInt; y <= rangeInt; y++) {
                for (int z = -rangeInt; z <= rangeInt; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (center.getSquaredDistance(pos) <= range * range) {
                        BlockState state = world.getBlockState(pos);
                        if (blockFilter.test(state)) {
                            foundBlocks.add(pos);
                        }
                    }
                }
            }
        }

        return foundBlocks;
    }

    /**
     * Scans only horizontal plane around entity.
     */
    private List<BlockPos> scanHorizontalPlane(World world, BlockPos center) {
        List<BlockPos> foundBlocks = new ArrayList<>();
        int rangeInt = (int) Math.ceil(range);

        // Scan only at entity's Y level and +/- 1
        for (int y = -1; y <= 1; y++) {
            for (int x = -rangeInt; x <= rangeInt; x++) {
                for (int z = -rangeInt; z <= rangeInt; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (Math.sqrt(x * x + z * z) <= range) {
                        BlockState state = world.getBlockState(pos);
                        if (blockFilter.test(state)) {
                            foundBlocks.add(pos);
                        }
                    }
                }
            }
        }

        return foundBlocks;
    }

    /**
     * Scans only in cardinal directions (N, S, E, W).
     */
    private List<BlockPos> scanCardinalDirections(World world, BlockPos center) {
        List<BlockPos> foundBlocks = new ArrayList<>();
        int rangeInt = (int) Math.ceil(range);

        // North (negative Z)
        for (int i = 1; i <= rangeInt; i++) {
            checkAndAddBlock(world, center.add(0, 0, -i), foundBlocks);
        }

        // South (positive Z)
        for (int i = 1; i <= rangeInt; i++) {
            checkAndAddBlock(world, center.add(0, 0, i), foundBlocks);
        }

        // East (positive X)
        for (int i = 1; i <= rangeInt; i++) {
            checkAndAddBlock(world, center.add(i, 0, 0), foundBlocks);
        }

        // West (negative X)
        for (int i = 1; i <= rangeInt; i++) {
            checkAndAddBlock(world, center.add(-i, 0, 0), foundBlocks);
        }

        return foundBlocks;
    }

    /**
     * Scans in a cone in front of entity (placeholder implementation).
     */
    private List<BlockPos> scanForwardCone(World world, BlockPos center) {
        // Simplified: scan horizontal plane in front
        return scanHorizontalPlane(world, center);
    }

    /**
     * Helper method to check and add a block if it matches.
     */
    private void checkAndAddBlock(World world, BlockPos pos, List<BlockPos> foundBlocks) {
        BlockState state = world.getBlockState(pos);
        if (blockFilter.test(state)) {
            foundBlocks.add(pos);
        }
    }

    /**
     * Finds the nearest block to the given position.
     */
    private BlockPos findNearestBlock(BlockPos center, List<BlockPos> blocks) {
        BlockPos nearest = blocks.get(0);
        double nearestDist = center.getSquaredDistance(nearest);

        for (BlockPos pos : blocks) {
            double dist = center.getSquaredDistance(pos);
            if (dist < nearestDist) {
                nearest = pos;
                nearestDist = dist;
            }
        }

        return nearest;
    }

    @Override
    public double getRange() {
        return range;
    }

    @Override
    public int getUpdateFrequency() {
        return updateFrequency;
    }

    @Override
    public void reset(BehaviorContext context) {
        context.getBlackboard().remove(blackboardKey);
        context.getBlackboard().remove(blackboardKey + "_count");
        context.getBlackboard().remove(blackboardKey + "_nearest");
    }

    /**
     * Gets the detected block positions from the blackboard.
     *
     * @param context The behavior context
     * @return List of detected block positions
     */
    public List<BlockPos> getDetectedBlocks(BehaviorContext context) {
        return context.getBlackboard()
            .<List<BlockPos>>get(blackboardKey)
            .orElse(new ArrayList<>());
    }

    /**
     * Gets the nearest detected block position.
     *
     * @param context The behavior context
     * @return The nearest block position, or null if none detected
     */
    public BlockPos getNearestBlock(BehaviorContext context) {
        return context.getBlackboard()
            .<BlockPos>get(blackboardKey + "_nearest")
            .orElse(null);
    }

    /**
     * Builder for creating BlockSensor instances.
     */
    public static class Builder {
        private double range;
        private String blackboardKey;
        private Predicate<BlockState> blockFilter;
        private ScanPattern scanPattern = ScanPattern.HORIZONTAL_PLANE;
        private int updateFrequency = 20;

        public Builder range(double range) {
            this.range = range;
            return this;
        }

        public Builder blackboardKey(String key) {
            this.blackboardKey = key;
            return this;
        }

        public Builder filterByBlock(Block block) {
            this.blockFilter = state -> state.isOf(block);
            return this;
        }

        public Builder filterByPredicate(Predicate<BlockState> predicate) {
            this.blockFilter = predicate;
            return this;
        }

        public Builder scanPattern(ScanPattern pattern) {
            this.scanPattern = pattern;
            return this;
        }

        public Builder updateFrequency(int frequency) {
            this.updateFrequency = frequency;
            return this;
        }

        public BlockSensor build() {
            if (range <= 0 || blackboardKey == null || blockFilter == null) {
                throw new IllegalStateException("Range, blackboardKey, and blockFilter must be set");
            }
            return new BlockSensor(range, blackboardKey, blockFilter, scanPattern, updateFrequency);
        }
    }

    /**
     * Creates a new builder for BlockSensor.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
