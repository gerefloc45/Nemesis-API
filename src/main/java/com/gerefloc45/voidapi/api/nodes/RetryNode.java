package com.gerefloc45.voidapi.api.nodes;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;

/**
 * Retry node - retries a failed child behavior a specified number of times.
 * Supports exponential backoff between retries.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class RetryNode implements Behavior {
    private final Behavior child;
    private final int maxRetries;
    private final float retryDelaySeconds;
    private final boolean useExponentialBackoff;
    private int currentRetry;
    private long lastRetryTime;
    private boolean isWaitingForRetry;

    /**
     * Creates a retry node with default settings (3 retries, no delay).
     *
     * @param child The child behavior to retry
     */
    public RetryNode(Behavior child) {
        this(child, 3, 0.0f, false);
    }

    /**
     * Creates a retry node with specified max retries.
     *
     * @param child The child behavior to retry
     * @param maxRetries Maximum number of retry attempts
     */
    public RetryNode(Behavior child, int maxRetries) {
        this(child, maxRetries, 0.0f, false);
    }

    /**
     * Creates a retry node with retry delay.
     *
     * @param child The child behavior to retry
     * @param maxRetries Maximum number of retry attempts
     * @param retryDelaySeconds Delay between retries in seconds
     */
    public RetryNode(Behavior child, int maxRetries, float retryDelaySeconds) {
        this(child, maxRetries, retryDelaySeconds, false);
    }

    /**
     * Creates a retry node with full configuration.
     *
     * @param child The child behavior to retry
     * @param maxRetries Maximum number of retry attempts
     * @param retryDelaySeconds Base delay between retries in seconds
     * @param useExponentialBackoff If true, delay increases exponentially with each retry
     */
    public RetryNode(Behavior child, int maxRetries, float retryDelaySeconds, boolean useExponentialBackoff) {
        this.child = child;
        this.maxRetries = maxRetries;
        this.retryDelaySeconds = retryDelaySeconds;
        this.useExponentialBackoff = useExponentialBackoff;
        this.currentRetry = 0;
        this.lastRetryTime = 0;
        this.isWaitingForRetry = false;
    }

    @Override
    public Status execute(BehaviorContext context) {
        // If waiting for retry delay
        if (isWaitingForRetry) {
            long currentTime = System.currentTimeMillis();
            float elapsedSeconds = (currentTime - lastRetryTime) / 1000.0f;
            float currentDelay = calculateDelay();

            if (elapsedSeconds < currentDelay) {
                return Status.RUNNING;
            }

            // Delay complete, restart child
            isWaitingForRetry = false;
            child.onStart(context);
        }

        // Execute child
        Status childStatus = child.execute(context);

        if (childStatus == Status.RUNNING) {
            return Status.RUNNING;
        }

        if (childStatus == Status.SUCCESS) {
            // Success, reset and return
            currentRetry = 0;
            return Status.SUCCESS;
        }

        // Child failed
        if (currentRetry < maxRetries) {
            // Retry
            currentRetry++;
            child.onEnd(context, childStatus);

            if (retryDelaySeconds > 0) {
                // Start retry delay
                lastRetryTime = System.currentTimeMillis();
                isWaitingForRetry = true;
                return Status.RUNNING;
            } else {
                // Immediate retry
                child.onStart(context);
                return Status.RUNNING;
            }
        }

        // Max retries exceeded
        currentRetry = 0;
        return Status.FAILURE;
    }

    @Override
    public void onStart(BehaviorContext context) {
        currentRetry = 0;
        isWaitingForRetry = false;
        child.onStart(context);
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        child.onEnd(context, status);
        currentRetry = 0;
        isWaitingForRetry = false;
    }

    /**
     * Calculates the current retry delay based on retry count and backoff settings.
     *
     * @return Delay in seconds
     */
    private float calculateDelay() {
        if (!useExponentialBackoff) {
            return retryDelaySeconds;
        }
        // Exponential backoff: delay * 2^(retry-1)
        return retryDelaySeconds * (float) Math.pow(2, currentRetry - 1);
    }

    /**
     * Gets the current retry attempt number.
     *
     * @return Current retry number (0 = first attempt)
     */
    public int getCurrentRetry() {
        return currentRetry;
    }

    /**
     * Gets the maximum number of retries.
     *
     * @return Max retries
     */
    public int getMaxRetries() {
        return maxRetries;
    }
}
