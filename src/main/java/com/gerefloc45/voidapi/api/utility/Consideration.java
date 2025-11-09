package com.gerefloc45.voidapi.api.utility;

import com.gerefloc45.voidapi.api.BehaviorContext;

import java.util.function.Function;

/**
 * A consideration evaluates a single variable/aspect of the game state
 * and converts it to a normalized score using a response curve.
 * 
 * <p>Considerations are the building blocks of Utility AI.
 * Multiple considerations can be combined to create complex decision-making.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class Consideration implements Scorer {
    private final Function<BehaviorContext, Double> inputFunction;
    private final ResponseCurve curve;
    private final double minInput;
    private final double maxInput;
    
    /**
     * Creates a new consideration.
     * 
     * @param inputFunction Function that extracts the raw value from context
     * @param curve Response curve to normalize the value
     * @param minInput Minimum expected input value
     * @param maxInput Maximum expected input value
     */
    public Consideration(Function<BehaviorContext, Double> inputFunction, 
                        ResponseCurve curve, 
                        double minInput, 
                        double maxInput) {
        this.inputFunction = inputFunction;
        this.curve = curve;
        this.minInput = minInput;
        this.maxInput = maxInput;
    }
    
    /**
     * Creates a consideration with default range [0, 1].
     * 
     * @param inputFunction Function that extracts the raw value from context
     * @param curve Response curve to normalize the value
     */
    public Consideration(Function<BehaviorContext, Double> inputFunction, ResponseCurve curve) {
        this(inputFunction, curve, 0.0, 1.0);
    }
    
    @Override
    public double score(BehaviorContext context) {
        // Get raw input value
        double rawValue = inputFunction.apply(context);
        
        // Normalize to [0, 1] range
        double normalized = normalize(rawValue);
        
        // Apply response curve
        return curve.evaluate(normalized);
    }
    
    /**
     * Normalizes a raw value to the [0, 1] range based on min/max bounds.
     * 
     * @param value The raw value to normalize
     * @return Normalized value between 0 and 1
     */
    private double normalize(double value) {
        if (maxInput <= minInput) {
            return 0.5; // Avoid division by zero
        }
        
        double normalized = (value - minInput) / (maxInput - minInput);
        return Math.max(0.0, Math.min(1.0, normalized)); // Clamp to [0, 1]
    }
    
    /**
     * Builder for creating Consideration instances.
     */
    public static class Builder {
        private Function<BehaviorContext, Double> inputFunction;
        private ResponseCurve curve = ResponseCurve.linear();
        private double minInput = 0.0;
        private double maxInput = 1.0;
        
        /**
         * Sets the input function that extracts a value from context.
         * 
         * @param function The input function
         * @return This builder
         */
        public Builder input(Function<BehaviorContext, Double> function) {
            this.inputFunction = function;
            return this;
        }
        
        /**
         * Sets the response curve.
         * 
         * @param curve The response curve
         * @return This builder
         */
        public Builder curve(ResponseCurve curve) {
            this.curve = curve;
            return this;
        }
        
        /**
         * Sets the expected input range.
         * 
         * @param min Minimum expected input
         * @param max Maximum expected input
         * @return This builder
         */
        public Builder range(double min, double max) {
            this.minInput = min;
            this.maxInput = max;
            return this;
        }
        
        /**
         * Builds the consideration.
         * 
         * @return A new Consideration instance
         */
        public Consideration build() {
            if (inputFunction == null) {
                throw new IllegalStateException("Input function must be set");
            }
            return new Consideration(inputFunction, curve, minInput, maxInput);
        }
    }
    
    /**
     * Creates a new builder for Consideration.
     * 
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
