package com.gerefloc45.voidapi.api.debug;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorNode;
import com.gerefloc45.voidapi.api.BehaviorTree;
import net.minecraft.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Behavior tree debugger - provides visualization and debugging tools for behavior trees.
 * Tracks execution history, node status, and performance metrics.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class BehaviorTreeDebugger {
    private static final Logger LOGGER = LoggerFactory.getLogger(BehaviorTreeDebugger.class);
    private static BehaviorTreeDebugger instance;

    private final Map<UUID, DebugSession> activeSessions;
    private boolean enabled;

    /**
     * Debug session for an entity's behavior tree.
     */
    public static class DebugSession {
        public final UUID entityId;
        public final BehaviorTree tree;
        public final List<ExecutionRecord> history;
        public final Map<Behavior, NodeStatistics> statistics;
        public long sessionStartTime;

        public DebugSession(UUID entityId, BehaviorTree tree) {
            this.entityId = entityId;
            this.tree = tree;
            this.history = new ArrayList<>();
            this.statistics = new HashMap<>();
            this.sessionStartTime = System.currentTimeMillis();
        }

        public void recordExecution(Behavior behavior, Behavior.Status status, long executionTimeMs) {
            ExecutionRecord record = new ExecutionRecord(behavior, status, executionTimeMs);
            history.add(record);

            // Update statistics
            NodeStatistics stats = statistics.computeIfAbsent(behavior, b -> new NodeStatistics());
            stats.recordExecution(status, executionTimeMs);

            // Limit history size
            if (history.size() > 1000) {
                history.remove(0);
            }
        }

        public String generateReport() {
            StringBuilder report = new StringBuilder();
            report.append("=== Behavior Tree Debug Report ===\n");
            report.append("Entity: ").append(entityId).append("\n");
            report.append("Session Duration: ").append(getSessionDuration()).append("ms\n");
            report.append("Total Executions: ").append(history.size()).append("\n\n");

            report.append("=== Node Statistics ===\n");
            for (Map.Entry<Behavior, NodeStatistics> entry : statistics.entrySet()) {
                Behavior behavior = entry.getKey();
                NodeStatistics stats = entry.getValue();
                report.append(behavior.getClass().getSimpleName()).append(":\n");
                report.append("  Executions: ").append(stats.totalExecutions).append("\n");
                report.append("  Success: ").append(stats.successCount).append("\n");
                report.append("  Failure: ").append(stats.failureCount).append("\n");
                report.append("  Running: ").append(stats.runningCount).append("\n");
                report.append("  Avg Time: ").append(stats.getAverageExecutionTime()).append("ms\n");
                report.append("  Max Time: ").append(stats.maxExecutionTime).append("ms\n\n");
            }

            return report.toString();
        }

        public long getSessionDuration() {
            return System.currentTimeMillis() - sessionStartTime;
        }
    }

    /**
     * Execution record for a single behavior execution.
     */
    public static class ExecutionRecord {
        public final Behavior behavior;
        public final Behavior.Status status;
        public final long executionTimeMs;
        public final long timestamp;

        public ExecutionRecord(Behavior behavior, Behavior.Status status, long executionTimeMs) {
            this.behavior = behavior;
            this.status = status;
            this.executionTimeMs = executionTimeMs;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Statistics for a specific behavior node.
     */
    public static class NodeStatistics {
        public int totalExecutions;
        public int successCount;
        public int failureCount;
        public int runningCount;
        public long totalExecutionTime;
        public long maxExecutionTime;
        public long minExecutionTime = Long.MAX_VALUE;

        public void recordExecution(Behavior.Status status, long executionTimeMs) {
            totalExecutions++;
            totalExecutionTime += executionTimeMs;
            maxExecutionTime = Math.max(maxExecutionTime, executionTimeMs);
            minExecutionTime = Math.min(minExecutionTime, executionTimeMs);

            switch (status) {
                case SUCCESS -> successCount++;
                case FAILURE -> failureCount++;
                case RUNNING -> runningCount++;
            }
        }

        public double getAverageExecutionTime() {
            return totalExecutions > 0 ? (double) totalExecutionTime / totalExecutions : 0.0;
        }

        public double getSuccessRate() {
            int completed = successCount + failureCount;
            return completed > 0 ? (double) successCount / completed : 0.0;
        }
    }

    private BehaviorTreeDebugger() {
        this.activeSessions = new HashMap<>();
        this.enabled = false;
    }

    /**
     * Gets the singleton instance.
     *
     * @return Debugger instance
     */
    public static BehaviorTreeDebugger getInstance() {
        if (instance == null) {
            instance = new BehaviorTreeDebugger();
        }
        return instance;
    }

    /**
     * Enables debugging.
     */
    public void enable() {
        enabled = true;
        LOGGER.info("Behavior tree debugging enabled");
    }

    /**
     * Disables debugging.
     */
    public void disable() {
        enabled = false;
        LOGGER.info("Behavior tree debugging disabled");
    }

    /**
     * Checks if debugging is enabled.
     *
     * @return True if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Starts a debug session for an entity.
     *
     * @param entity Entity to debug
     * @param tree Behavior tree
     */
    public void startSession(Entity entity, BehaviorTree tree) {
        if (!enabled) {
            return;
        }

        UUID entityId = entity.getUuid();
        DebugSession session = new DebugSession(entityId, tree);
        activeSessions.put(entityId, session);
        LOGGER.info("Started debug session for entity: {}", entityId);
    }

    /**
     * Stops a debug session and generates report.
     *
     * @param entity Entity to stop debugging
     * @return Debug report or null if no session
     */
    public String stopSession(Entity entity) {
        UUID entityId = entity.getUuid();
        DebugSession session = activeSessions.remove(entityId);

        if (session != null) {
            String report = session.generateReport();
            LOGGER.info("Stopped debug session for entity: {}", entityId);
            return report;
        }

        return null;
    }

    /**
     * Records a behavior execution.
     *
     * @param entity Entity
     * @param behavior Behavior that executed
     * @param status Execution status
     * @param executionTimeMs Execution time in milliseconds
     */
    public void recordExecution(Entity entity, Behavior behavior, Behavior.Status status, long executionTimeMs) {
        if (!enabled) {
            return;
        }

        UUID entityId = entity.getUuid();
        DebugSession session = activeSessions.get(entityId);

        if (session != null) {
            session.recordExecution(behavior, status, executionTimeMs);
        }
    }

    /**
     * Gets the debug session for an entity.
     *
     * @param entity Entity
     * @return Debug session or null
     */
    public DebugSession getSession(Entity entity) {
        return activeSessions.get(entity.getUuid());
    }

    /**
     * Prints the tree structure.
     *
     * @param tree Behavior tree
     * @return Tree structure as string
     */
    public String printTreeStructure(BehaviorTree tree) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Behavior Tree Structure ===\n");
        printNode(tree.getRootBehavior(), sb, 0);
        return sb.toString();
    }

    /**
     * Recursively prints a node and its children.
     *
     * @param behavior Behavior node
     * @param sb String builder
     * @param depth Current depth
     */
    private void printNode(Behavior behavior, StringBuilder sb, int depth) {
        String indent = "  ".repeat(depth);
        sb.append(indent).append("- ").append(behavior.getClass().getSimpleName()).append("\n");

        if (behavior instanceof BehaviorNode node) {
            for (Behavior child : node.getChildren()) {
                printNode(child, sb, depth + 1);
            }
        }
    }

    /**
     * Clears all debug sessions.
     */
    public void clearAll() {
        activeSessions.clear();
        LOGGER.info("Cleared all debug sessions");
    }

    /**
     * Gets the number of active debug sessions.
     *
     * @return Session count
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}
