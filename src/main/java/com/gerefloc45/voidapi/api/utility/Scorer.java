package com.gerefloc45.voidapi.api.utility;

import com.gerefloc45.voidapi.api.BehaviorContext;

/**
 * Interface for scoring behaviors based on current context.
 * Used by Utility AI system to evaluate and select the best action.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
@FunctionalInterface
public interface Scorer {
    
    /**
     * Calculates a score for this behavior based on the current context.
     * Higher scores indicate higher priority/utility.
     * 
     * @param context The current behavior context
     * @return Score value, typically between 0.0 (lowest) and 1.0 (highest)
     */
    double score(BehaviorContext context);
    
    /**
     * Creates a constant scorer that always returns the same value.
     * 
     * @param value The constant score value
     * @return A scorer that always returns the given value
     */
    static Scorer constant(double value) {
        return context -> value;
    }
    
    /**
     * Combines this scorer with another using addition.
     * 
     * @param other The other scorer to add
     * @return A new scorer that returns the sum of both scores
     */
    default Scorer add(Scorer other) {
        return context -> this.score(context) + other.score(context);
    }
    
    /**
     * Combines this scorer with another using multiplication.
     * 
     * @param other The other scorer to multiply
     * @return A new scorer that returns the product of both scores
     */
    default Scorer multiply(Scorer other) {
        return context -> this.score(context) * other.score(context);
    }
    
    /**
     * Scales this scorer by a constant factor.
     * 
     * @param factor The scaling factor
     * @return A new scorer with scaled values
     */
    default Scorer scale(double factor) {
        return context -> this.score(context) * factor;
    }
    
    /**
     * Clamps the score between min and max values.
     * 
     * @param min Minimum score value
     * @param max Maximum score value
     * @return A new scorer with clamped values
     */
    default Scorer clamp(double min, double max) {
        return context -> {
            double score = this.score(context);
            return Math.max(min, Math.min(max, score));
        };
    }
    
    /**
     * Inverts the score (1.0 - score).
     * Useful for converting "goodness" to "badness" or vice versa.
     * 
     * @return A new scorer with inverted values
     */
    default Scorer invert() {
        return context -> 1.0 - this.score(context);
    }
}
