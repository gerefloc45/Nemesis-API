package com.gerefloc45.voidapi.api.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Performance profiler - tracks and analyzes AI system performance.
 * Identifies bottlenecks and provides optimization recommendations.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class PerformanceProfiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceProfiler.class);
    private static PerformanceProfiler instance;

    private final Map<String, ProfileSection> sections;
    private final Map<UUID, EntityProfile> entityProfiles;
    private boolean enabled;
    private long sessionStartTime;

    /**
     * Profile section for a specific operation or system.
     */
    public static class ProfileSection {
        public final String name;
        public long totalTime;
        public long callCount;
        public long maxTime;
        public long minTime = Long.MAX_VALUE;
        public final List<Long> recentSamples;
        private static final int MAX_SAMPLES = 100;

        public ProfileSection(String name) {
            this.name = name;
            this.recentSamples = new ArrayList<>();
        }

        public void record(long timeMs) {
            totalTime += timeMs;
            callCount++;
            maxTime = Math.max(maxTime, timeMs);
            minTime = Math.min(minTime, timeMs);

            recentSamples.add(timeMs);
            if (recentSamples.size() > MAX_SAMPLES) {
                recentSamples.remove(0);
            }
        }

        public double getAverageTime() {
            return callCount > 0 ? (double) totalTime / callCount : 0.0;
        }

        public double getRecentAverageTime() {
            if (recentSamples.isEmpty()) {
                return 0.0;
            }
            long sum = recentSamples.stream().mapToLong(Long::longValue).sum();
            return (double) sum / recentSamples.size();
        }

        public double getStandardDeviation() {
            if (recentSamples.size() < 2) {
                return 0.0;
            }

            double mean = getRecentAverageTime();
            double variance = recentSamples.stream()
                .mapToDouble(time -> Math.pow(time - mean, 2))
                .average()
                .orElse(0.0);

            return Math.sqrt(variance);
        }

        public String format() {
            return String.format("%s: avg=%.2fms, recent=%.2fms, min=%dms, max=%dms, calls=%d, stddev=%.2fms",
                name, getAverageTime(), getRecentAverageTime(), minTime, maxTime, callCount, getStandardDeviation());
        }
    }

    /**
     * Performance profile for a specific entity.
     */
    public static class EntityProfile {
        public final UUID entityId;
        public long totalTickTime;
        public long tickCount;
        public long maxTickTime;
        public final Map<String, Long> behaviorTimes;

        public EntityProfile(UUID entityId) {
            this.entityId = entityId;
            this.behaviorTimes = new HashMap<>();
        }

        public void recordTick(long timeMs) {
            totalTickTime += timeMs;
            tickCount++;
            maxTickTime = Math.max(maxTickTime, timeMs);
        }

        public void recordBehavior(String behaviorName, long timeMs) {
            behaviorTimes.merge(behaviorName, timeMs, Long::sum);
        }

        public double getAverageTickTime() {
            return tickCount > 0 ? (double) totalTickTime / tickCount : 0.0;
        }

        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Entity %s: avg=%.2fms, max=%dms, ticks=%d\n",
                entityId, getAverageTickTime(), maxTickTime, tickCount));

            if (!behaviorTimes.isEmpty()) {
                sb.append("  Top behaviors:\n");
                behaviorTimes.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> sb.append(String.format("    %s: %dms\n", entry.getKey(), entry.getValue())));
            }

            return sb.toString();
        }
    }

    /**
     * Profiling session for timing operations.
     */
    public static class ProfilingSession implements AutoCloseable {
        private final String sectionName;
        private final long startTime;
        private final PerformanceProfiler profiler;

        public ProfilingSession(PerformanceProfiler profiler, String sectionName) {
            this.profiler = profiler;
            this.sectionName = sectionName;
            this.startTime = System.nanoTime();
        }

        @Override
        public void close() {
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            profiler.recordSection(sectionName, durationMs);
        }
    }

    private PerformanceProfiler() {
        this.sections = new ConcurrentHashMap<>();
        this.entityProfiles = new ConcurrentHashMap<>();
        this.enabled = false;
        this.sessionStartTime = System.currentTimeMillis();
    }

    /**
     * Gets the singleton instance.
     *
     * @return Profiler instance
     */
    public static PerformanceProfiler getInstance() {
        if (instance == null) {
            instance = new PerformanceProfiler();
        }
        return instance;
    }

    /**
     * Enables profiling.
     */
    public void enable() {
        enabled = true;
        sessionStartTime = System.currentTimeMillis();
        LOGGER.info("Performance profiling enabled");
    }

    /**
     * Disables profiling.
     */
    public void disable() {
        enabled = false;
        LOGGER.info("Performance profiling disabled");
    }

    /**
     * Checks if profiling is enabled.
     *
     * @return True if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Starts a profiling section.
     *
     * @param sectionName Section name
     * @return Profiling session (use with try-with-resources)
     */
    public ProfilingSession startSection(String sectionName) {
        if (!enabled) {
            return new ProfilingSession(this, sectionName);
        }
        return new ProfilingSession(this, sectionName);
    }

    /**
     * Records time for a section.
     *
     * @param sectionName Section name
     * @param timeMs Time in milliseconds
     */
    public void recordSection(String sectionName, long timeMs) {
        if (!enabled) {
            return;
        }

        ProfileSection section = sections.computeIfAbsent(sectionName, ProfileSection::new);
        section.record(timeMs);

        // Warn if section is slow
        if (timeMs > 50) {
            LOGGER.warn("Slow operation detected: {} took {}ms", sectionName, timeMs);
        }
    }

    /**
     * Records entity tick time.
     *
     * @param entityId Entity UUID
     * @param timeMs Time in milliseconds
     */
    public void recordEntityTick(UUID entityId, long timeMs) {
        if (!enabled) {
            return;
        }

        EntityProfile profile = entityProfiles.computeIfAbsent(entityId, EntityProfile::new);
        profile.recordTick(timeMs);
    }

    /**
     * Records behavior execution time for an entity.
     *
     * @param entityId Entity UUID
     * @param behaviorName Behavior name
     * @param timeMs Time in milliseconds
     */
    public void recordBehavior(UUID entityId, String behaviorName, long timeMs) {
        if (!enabled) {
            return;
        }

        EntityProfile profile = entityProfiles.computeIfAbsent(entityId, EntityProfile::new);
        profile.recordBehavior(behaviorName, timeMs);
    }

    /**
     * Generates a comprehensive performance report.
     *
     * @return Performance report
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Performance Profile Report ===\n");
        report.append("Session Duration: ").append(getSessionDuration()).append("ms\n\n");

        // System sections
        report.append("=== System Performance ===\n");
        sections.values().stream()
            .sorted(Comparator.comparingDouble(ProfileSection::getAverageTime).reversed())
            .forEach(section -> report.append(section.format()).append("\n"));

        report.append("\n=== Entity Performance ===\n");
        report.append("Total Entities: ").append(entityProfiles.size()).append("\n");

        // Top slowest entities
        report.append("\nSlowest Entities:\n");
        entityProfiles.values().stream()
            .sorted(Comparator.comparingDouble(EntityProfile::getAverageTickTime).reversed())
            .limit(10)
            .forEach(profile -> report.append(profile.format()).append("\n"));

        // Recommendations
        report.append("\n=== Optimization Recommendations ===\n");
        report.append(generateRecommendations());

        return report.toString();
    }

    /**
     * Generates optimization recommendations based on profiling data.
     *
     * @return Recommendations
     */
    private String generateRecommendations() {
        StringBuilder recommendations = new StringBuilder();
        int recommendationCount = 0;

        // Check for slow sections
        for (ProfileSection section : sections.values()) {
            if (section.getAverageTime() > 10) {
                recommendations.append(String.format("- Consider optimizing '%s' (avg: %.2fms)\n",
                    section.name, section.getAverageTime()));
                recommendationCount++;
            }

            if (section.getStandardDeviation() > section.getAverageTime() * 0.5) {
                recommendations.append(String.format("- '%s' has high variance (stddev: %.2fms) - investigate inconsistent performance\n",
                    section.name, section.getStandardDeviation()));
                recommendationCount++;
            }
        }

        // Check for slow entities
        long slowEntityCount = entityProfiles.values().stream()
            .filter(profile -> profile.getAverageTickTime() > 5)
            .count();

        if (slowEntityCount > 0) {
            recommendations.append(String.format("- %d entities have slow AI (>5ms avg) - consider optimization\n", slowEntityCount));
            recommendationCount++;
        }

        if (recommendationCount == 0) {
            recommendations.append("- No performance issues detected. System is running optimally!\n");
        }

        return recommendations.toString();
    }

    /**
     * Gets the session duration.
     *
     * @return Duration in milliseconds
     */
    public long getSessionDuration() {
        return System.currentTimeMillis() - sessionStartTime;
    }

    /**
     * Clears all profiling data.
     */
    public void clear() {
        sections.clear();
        entityProfiles.clear();
        sessionStartTime = System.currentTimeMillis();
        LOGGER.info("Cleared all profiling data");
    }

    /**
     * Gets a specific profile section.
     *
     * @param sectionName Section name
     * @return Profile section or null
     */
    public ProfileSection getSection(String sectionName) {
        return sections.get(sectionName);
    }

    /**
     * Gets all profile sections.
     *
     * @return Map of sections
     */
    public Map<String, ProfileSection> getAllSections() {
        return new HashMap<>(sections);
    }
}
