package com.myname.nemesisapi.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Utility class for running asynchronous AI operations.
 * Provides thread pool management and CompletableFuture helpers.
 * 
 * @author Nemesis-API Framework
 * @version 1.0.0
 */
public class AsyncHelper {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
        r -> {
            Thread thread = new Thread(r, "Nemesis-API-Async");
            thread.setDaemon(true);
            return thread;
        }
    );

    /**
     * Runs a task asynchronously and returns a CompletableFuture.
     *
     * @param task The task to run
     * @param <T> The return type
     * @return A CompletableFuture for the result
     */
    public static <T> CompletableFuture<T> runAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, EXECUTOR);
    }

    /**
     * Runs a task asynchronously without a return value.
     *
     * @param task The task to run
     * @return A CompletableFuture that completes when the task finishes
     */
    public static CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, EXECUTOR);
    }

    /**
     * Creates a completed future with the given value.
     *
     * @param value The value
     * @param <T> The type
     * @return A completed CompletableFuture
     */
    public static <T> CompletableFuture<T> completedFuture(T value) {
        return CompletableFuture.completedFuture(value);
    }

    /**
     * Creates a failed future with the given exception.
     *
     * @param throwable The exception
     * @param <T> The type
     * @return A failed CompletableFuture
     */
    public static <T> CompletableFuture<T> failedFuture(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    /**
     * Shuts down the async executor.
     * Should be called during mod shutdown.
     */
    public static void shutdown() {
        EXECUTOR.shutdown();
    }

    /**
     * Gets the executor service used for async operations.
     *
     * @return The executor service
     */
    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }
}
