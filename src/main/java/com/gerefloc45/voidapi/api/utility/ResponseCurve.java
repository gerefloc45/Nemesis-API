package com.gerefloc45.voidapi.api.utility;

/**
 * Represents a mathematical curve that maps input values [0, 1] to output values [0, 1].
 * Used by Considerations to shape how raw inputs affect the final utility score.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
@FunctionalInterface
public interface ResponseCurve {
    
    /**
     * Evaluates the curve at the given input point.
     * 
     * @param x Input value, should be in range [0, 1]
     * @return Output value, typically in range [0, 1]
     */
    double evaluate(double x);
    
    // ============================================
    // PREDEFINED CURVES
    // ============================================
    
    /**
     * Linear curve: output = input
     * No modification to the input value.
     * 
     * @return Linear response curve
     */
    static ResponseCurve linear() {
        return x -> x;
    }
    
    /**
     * Quadratic curve: output = x²
     * Emphasizes higher values, deemphasizes lower values.
     * 
     * @return Quadratic response curve
     */
    static ResponseCurve quadratic() {
        return x -> x * x;
    }
    
    /**
     * Cubic curve: output = x³
     * Even more dramatic emphasis on higher values.
     * 
     * @return Cubic response curve
     */
    static ResponseCurve cubic() {
        return x -> x * x * x;
    }
    
    /**
     * Inverse quadratic: output = 1 - (1-x)²
     * Emphasizes lower values, deemphasizes higher values.
     * 
     * @return Inverse quadratic curve
     */
    static ResponseCurve inverseQuadratic() {
        return x -> 1.0 - (1.0 - x) * (1.0 - x);
    }
    
    /**
     * Exponential curve with configurable slope.
     * 
     * @param k Slope factor (higher = steeper curve)
     * @return Exponential response curve
     */
    static ResponseCurve exponential(double k) {
        return x -> (Math.exp(k * x) - 1.0) / (Math.exp(k) - 1.0);
    }
    
    /**
     * Exponential curve with default slope (2.0).
     * 
     * @return Exponential response curve
     */
    static ResponseCurve exponential() {
        return exponential(2.0);
    }
    
    /**
     * Logistic (S-curve) with configurable steepness.
     * Smoothly transitions from 0 to 1 around the midpoint.
     * 
     * @param k Steepness factor (higher = steeper transition)
     * @return Logistic response curve
     */
    static ResponseCurve logistic(double k) {
        return x -> 1.0 / (1.0 + Math.exp(-k * (x - 0.5)));
    }
    
    /**
     * Logistic curve with default steepness (10.0).
     * 
     * @return Logistic response curve
     */
    static ResponseCurve logistic() {
        return logistic(10.0);
    }
    
    /**
     * Boolean step function: returns 0 below threshold, 1 above.
     * 
     * @param threshold The threshold value [0, 1]
     * @return Step response curve
     */
    static ResponseCurve step(double threshold) {
        return x -> x >= threshold ? 1.0 : 0.0;
    }
    
    /**
     * Smooth step function (smoothstep interpolation).
     * 
     * @return Smooth step response curve
     */
    static ResponseCurve smoothStep() {
        return x -> x * x * (3.0 - 2.0 * x);
    }
    
    /**
     * Power curve with configurable exponent.
     * 
     * @param power The exponent
     * @return Power response curve
     */
    static ResponseCurve power(double power) {
        return x -> Math.pow(x, power);
    }
    
    /**
     * Sine wave curve (0 to π/2).
     * 
     * @return Sine response curve
     */
    static ResponseCurve sine() {
        return x -> Math.sin(x * Math.PI / 2.0);
    }
    
    /**
     * Inverted sine wave curve.
     * 
     * @return Inverted sine response curve
     */
    static ResponseCurve inverseSine() {
        return x -> 1.0 - Math.cos(x * Math.PI / 2.0);
    }
    
    /**
     * Constant curve that always returns the same value.
     * 
     * @param value The constant value to return
     * @return Constant response curve
     */
    static ResponseCurve constant(double value) {
        return x -> value;
    }
    
    // ============================================
    // CURVE COMBINATORS
    // ============================================
    
    /**
     * Inverts this curve: output = 1 - curve(x)
     * 
     * @return Inverted curve
     */
    default ResponseCurve invert() {
        return x -> 1.0 - this.evaluate(x);
    }
    
    /**
     * Scales the output of this curve by a factor.
     * 
     * @param factor The scaling factor
     * @return Scaled curve
     */
    default ResponseCurve scale(double factor) {
        return x -> this.evaluate(x) * factor;
    }
    
    /**
     * Adds a constant offset to the output.
     * 
     * @param offset The offset value
     * @return Offset curve
     */
    default ResponseCurve offset(double offset) {
        return x -> this.evaluate(x) + offset;
    }
    
    /**
     * Clamps the output between min and max.
     * 
     * @param min Minimum output value
     * @param max Maximum output value
     * @return Clamped curve
     */
    default ResponseCurve clamp(double min, double max) {
        return x -> {
            double value = this.evaluate(x);
            return Math.max(min, Math.min(max, value));
        };
    }
    
    /**
     * Chains this curve with another (applies this curve, then the other).
     * 
     * @param other The curve to apply after this one
     * @return Chained curve
     */
    default ResponseCurve then(ResponseCurve other) {
        return x -> other.evaluate(this.evaluate(x));
    }
}
