package com.gerefloc45.voidapi.api.utility;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;
import com.gerefloc45.voidapi.api.BehaviorNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A selector node that chooses which child behavior to execute
 * based on utility scores calculated at runtime.
 * 
 * <p>Unlike traditional Selector nodes that try children in order,
 * UtilitySelector evaluates all children and picks the one with the highest score.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class UtilitySelector extends BehaviorNode {
    private final List<ScoredBehavior> scoredBehaviors;
    private Behavior currentBehavior;
    private double reevaluateInterval; // in ticks
    private int ticksSinceLastEvaluation;
    
    /**
     * Represents a behavior paired with its scorer.
     */
    private static class ScoredBehavior {
        final Behavior behavior;
        final Scorer scorer;
        double lastScore;
        
        ScoredBehavior(Behavior behavior, Scorer scorer) {
            this.behavior = behavior;
            this.scorer = scorer;
            this.lastScore = 0.0;
        }
    }
    
    /**
     * Creates a new utility selector.
     */
    public UtilitySelector() {
        this(20.0); // Re-evaluate every second by default (20 ticks)
    }
    
    /**
     * Creates a new utility selector with custom re-evaluation interval.
     * 
     * @param reevaluateInterval How often to re-evaluate scores (in ticks)
     */
    public UtilitySelector(double reevaluateInterval) {
        this.scoredBehaviors = new ArrayList<>();
        this.reevaluateInterval = reevaluateInterval;
        this.ticksSinceLastEvaluation = 0;
    }
    
    /**
     * Adds a child behavior with its associated scorer.
     * 
     * @param behavior The behavior to add
     * @param scorer The scorer to evaluate this behavior's utility
     * @return This selector for method chaining
     */
    public UtilitySelector addChild(Behavior behavior, Scorer scorer) {
        scoredBehaviors.add(new ScoredBehavior(behavior, scorer));
        return this;
    }
    
    /**
     * Adds a child behavior with a constant score.
     * 
     * @param behavior The behavior to add
     * @param constantScore The constant score for this behavior
     * @return This selector for method chaining
     */
    public UtilitySelector addChild(Behavior behavior, double constantScore) {
        return addChild(behavior, Scorer.constant(constantScore));
    }
    
    @Override
    public void onStart(BehaviorContext context) {
        ticksSinceLastEvaluation = 0;
        currentBehavior = selectBestBehavior(context);
        if (currentBehavior != null) {
            currentBehavior.onStart(context);
        }
    }
    
    @Override
    public Status execute(BehaviorContext context) {
        if (scoredBehaviors.isEmpty()) {
            return Status.FAILURE;
        }
        
        // Re-evaluate periodically or if no current behavior
        ticksSinceLastEvaluation++;
        if (currentBehavior == null || ticksSinceLastEvaluation >= reevaluateInterval) {
            Behavior newBehavior = selectBestBehavior(context);
            
            // If best behavior changed, switch to it
            if (newBehavior != currentBehavior) {
                if (currentBehavior != null) {
                    currentBehavior.onEnd(context, Status.RUNNING);
                }
                currentBehavior = newBehavior;
                if (currentBehavior != null) {
                    currentBehavior.onStart(context);
                }
            }
            
            ticksSinceLastEvaluation = 0;
        }
        
        // Execute current best behavior
        if (currentBehavior != null) {
            Status status = currentBehavior.execute(context);
            
            // If behavior completed, clear it
            if (status != Status.RUNNING) {
                currentBehavior.onEnd(context, status);
                currentBehavior = null;
                return status;
            }
            
            return Status.RUNNING;
        }
        
        return Status.FAILURE;
    }
    
    @Override
    public void onEnd(BehaviorContext context, Status status) {
        if (currentBehavior != null) {
            currentBehavior.onEnd(context, status);
            currentBehavior = null;
        }
    }
    
    /**
     * Evaluates all behaviors and selects the one with the highest score.
     * 
     * @param context The current behavior context
     * @return The behavior with the highest score, or null if none
     */
    private Behavior selectBestBehavior(BehaviorContext context) {
        if (scoredBehaviors.isEmpty()) {
            return null;
        }
        
        ScoredBehavior best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        for (ScoredBehavior scoredBehavior : scoredBehaviors) {
            double score = scoredBehavior.scorer.score(context);
            scoredBehavior.lastScore = score;
            
            if (score > bestScore) {
                bestScore = score;
                best = scoredBehavior;
            }
        }
        
        // Store best score in blackboard for debugging
        if (best != null) {
            context.getBlackboard().set("utility_best_score", bestScore);
        }
        
        return best != null ? best.behavior : null;
    }
    
    /**
     * Gets the current scores for all behaviors.
     * Useful for debugging and visualization.
     * 
     * @return List of (behavior, score) pairs
     */
    public List<BehaviorScore> getScores() {
        List<BehaviorScore> scores = new ArrayList<>();
        for (ScoredBehavior sb : scoredBehaviors) {
            scores.add(new BehaviorScore(sb.behavior, sb.lastScore));
        }
        return scores;
    }
    
    /**
     * Sets how often to re-evaluate behavior scores.
     * 
     * @param interval Interval in ticks (20 ticks = 1 second)
     * @return This selector for method chaining
     */
    public UtilitySelector setReevaluateInterval(double interval) {
        this.reevaluateInterval = interval;
        return this;
    }
    
    /**
     * Represents a behavior paired with its current score.
     */
    public static class BehaviorScore {
        public final Behavior behavior;
        public final double score;
        
        public BehaviorScore(Behavior behavior, double score) {
            this.behavior = behavior;
            this.score = score;
        }
    }
}
