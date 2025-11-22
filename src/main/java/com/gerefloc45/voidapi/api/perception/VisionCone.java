package com.gerefloc45.voidapi.api.perception;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Utility class for calculating field-of-view (vision cone) detection.
 * Provides methods to determine if a target is within an entity's vision cone.
 * 
 * @author VoidAPI Framework
 * @version 0.7.0
 */
public class VisionCone {
    private final float horizontalFOV; // In degrees
    private final float verticalFOV; // In degrees
    private final double range;
    private final boolean enablePeripheralVision;
    private final float peripheralFOV; // Wider angle with reduced effectiveness

    /**
     * Creates a vision cone with default parameters.
     * Default: 90° horizontal, 60° vertical, no peripheral vision
     *
     * @param range Maximum vision range in blocks
     */
    public VisionCone(double range) {
        this(range, 90.0f, 60.0f, false, 180.0f);
    }

    /**
     * Creates a vision cone with custom parameters.
     *
     * @param range         Maximum vision range in blocks
     * @param horizontalFOV Horizontal field of view in degrees (0-360)
     * @param verticalFOV   Vertical field of view in degrees (0-180)
     */
    public VisionCone(double range, float horizontalFOV, float verticalFOV) {
        this(range, horizontalFOV, verticalFOV, false, 180.0f);
    }

    /**
     * Creates a vision cone with full configuration including peripheral vision.
     *
     * @param range                  Maximum vision range in blocks
     * @param horizontalFOV          Horizontal field of view in degrees (0-360)
     * @param verticalFOV            Vertical field of view in degrees (0-180)
     * @param enablePeripheralVision Enable peripheral vision detection
     * @param peripheralFOV          Peripheral vision angle (wider than main FOV)
     */
    public VisionCone(double range, float horizontalFOV, float verticalFOV,
            boolean enablePeripheralVision, float peripheralFOV) {
        this.range = range;
        this.horizontalFOV = Math.max(0, Math.min(360, horizontalFOV));
        this.verticalFOV = Math.max(0, Math.min(180, verticalFOV));
        this.enablePeripheralVision = enablePeripheralVision;
        this.peripheralFOV = Math.max(0, Math.min(360, peripheralFOV));
    }

    /**
     * Checks if a target position is within the vision cone.
     *
     * @param observer  The entity doing the observing
     * @param targetPos The target position to check
     * @return True if the target is within the vision cone
     */
    public boolean isInVisionCone(LivingEntity observer, Vec3d targetPos) {
        Vec3d observerPos = observer.getEyePos();
        Vec3d toTarget = targetPos.subtract(observerPos);
        double distance = toTarget.length();

        // Check range first
        if (distance > range || distance < 0.1) {
            return false;
        }

        // Calculate angles
        Vec3d observerLookVec = observer.getRotationVec(1.0f);

        // Horizontal angle (yaw)
        float horizontalAngle = calculateHorizontalAngle(observerLookVec, toTarget);

        // Vertical angle (pitch)
        float verticalAngle = calculateVerticalAngle(observerLookVec, toTarget);

        // Check if within main FOV
        boolean inMainFOV = horizontalAngle <= (horizontalFOV / 2.0f) &&
                verticalAngle <= (verticalFOV / 2.0f);

        // Check peripheral vision if enabled
        if (!inMainFOV && enablePeripheralVision) {
            return horizontalAngle <= (peripheralFOV / 2.0f) &&
                    verticalAngle <= (verticalFOV / 2.0f); // Use same vertical for peripheral
        }

        return inMainFOV;
    }

    /**
     * Checks if a target entity is within the vision cone.
     *
     * @param observer The entity doing the observing
     * @param target   The target entity to check
     * @return True if the target is within the vision cone
     */
    public boolean isInVisionCone(LivingEntity observer, LivingEntity target) {
        return isInVisionCone(observer, target.getEyePos());
    }

    /**
     * Gets the visibility factor (0.0 to 1.0) based on position in vision cone.
     * 1.0 = center of vision, 0.0 = outside vision cone
     *
     * @param observer  The entity doing the observing
     * @param targetPos The target position
     * @return Visibility factor from 0.0 to 1.0
     */
    public float getVisibilityFactor(LivingEntity observer, Vec3d targetPos) {
        Vec3d observerPos = observer.getEyePos();
        Vec3d toTarget = targetPos.subtract(observerPos);
        double distance = toTarget.length();

        if (distance > range || distance < 0.1) {
            return 0.0f;
        }

        Vec3d observerLookVec = observer.getRotationVec(1.0f);
        float horizontalAngle = calculateHorizontalAngle(observerLookVec, toTarget);
        float verticalAngle = calculateVerticalAngle(observerLookVec, toTarget);

        // Calculate factors
        float horizontalFactor = 1.0f - Math.min(1.0f, horizontalAngle / (horizontalFOV / 2.0f));
        float verticalFactor = 1.0f - Math.min(1.0f, verticalAngle / (verticalFOV / 2.0f));
        float rangeFactor = 1.0f - (float) (distance / range);

        // Combine factors (weighted average)
        float visibilityFactor = (horizontalFactor * 0.5f + verticalFactor * 0.3f + rangeFactor * 0.2f);

        // Reduce for peripheral vision
        if (enablePeripheralVision && horizontalAngle > (horizontalFOV / 2.0f)) {
            visibilityFactor *= 0.5f; // 50% reduction in peripheral vision
        }

        return Math.max(0.0f, Math.min(1.0f, visibilityFactor));
    }

    /**
     * Calculates the horizontal angle between look vector and target direction.
     *
     * @param lookVec  The observer's look vector
     * @param toTarget Vector from observer to target
     * @return Angle in degrees
     */
    private float calculateHorizontalAngle(Vec3d lookVec, Vec3d toTarget) {
        // Project both vectors onto horizontal plane (ignore Y)
        Vec3d lookHorizontal = new Vec3d(lookVec.x, 0, lookVec.z).normalize();
        Vec3d targetHorizontal = new Vec3d(toTarget.x, 0, toTarget.z).normalize();

        if (lookHorizontal.length() < 0.01 || targetHorizontal.length() < 0.01) {
            return 0.0f; // Looking straight up/down
        }

        double dotProduct = lookHorizontal.dotProduct(targetHorizontal);
        dotProduct = Math.max(-1.0, Math.min(1.0, dotProduct)); // Clamp to [-1, 1]

        return (float) Math.toDegrees(Math.acos(dotProduct));
    }

    /**
     * Calculates the vertical angle between look vector and target direction.
     *
     * @param lookVec  The observer's look vector
     * @param toTarget Vector from observer to target
     * @return Angle in degrees
     */
    private float calculateVerticalAngle(Vec3d lookVec, Vec3d toTarget) {
        Vec3d lookNorm = lookVec.normalize();
        Vec3d targetNorm = toTarget.normalize();

        double dotProduct = lookNorm.dotProduct(targetNorm);
        dotProduct = Math.max(-1.0, Math.min(1.0, dotProduct));

        float totalAngle = (float) Math.toDegrees(Math.acos(dotProduct));

        // Get horizontal angle to subtract
        float horizontalAngle = calculateHorizontalAngle(lookVec, toTarget);

        // Use Pythagorean theorem to get vertical component
        // totalAngle² ≈ horizontalAngle² + verticalAngle²
        float verticalAngle = (float) Math.sqrt(Math.max(0,
                totalAngle * totalAngle - horizontalAngle * horizontalAngle));

        return verticalAngle;
    }

    // Getters
    public float getHorizontalFOV() {
        return horizontalFOV;
    }

    public float getVerticalFOV() {
        return verticalFOV;
    }

    public double getRange() {
        return range;
    }

    public boolean isPeripheralVisionEnabled() {
        return enablePeripheralVision;
    }

    public float getPeripheralFOV() {
        return peripheralFOV;
    }
}
