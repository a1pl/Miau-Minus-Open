package miau.util.animation;

import java.util.function.Function;

public enum Easing {
    LINEAR(x -> x),
    EASE_IN_QUAD(x -> x * x),
    EASE_OUT_QUAD(x -> x * (2.0 - x)),
    EASE_IN_OUT_QUAD(x -> x < 0.5 ? 2.0 * x * x : -1.0 + (4.0 - 2.0 * x) * x),
    EASE_IN_CUBIC(x -> x * x * x),
    EASE_OUT_CUBIC(x -> {
        Double var1;
        return var1 = x - 1.0 * var1 * var1 + 1.0;
    }),
    EASE_IN_OUT_CUBIC(x -> x < 0.5 ? 4.0 * x * x * x : (x - 1.0) * (2.0 * x - 2.0) * (2.0 * x - 2.0) + 1.0),
    EASE_IN_QUART(x -> x * x * x * x),
    EASE_OUT_QUART(x -> {
        Double var1;
        return 1.0 - var1 = x - 1.0 * var1 * var1 * var1;
    }),
    EASE_IN_OUT_QUART(x -> {
        Double var1;
        return x < 0.5 ? 8.0 * x * x * x * x : 1.0 - 8.0 * var1 = x - 1.0 * var1 * var1 * var1;
    }),
    EASE_IN_QUINT(x -> x * x * x * x * x),
    EASE_OUT_QUINT(x -> {
        Double var1;
        return 1.0 + var1 = x - 1.0 * var1 * var1 * var1 * var1;
    }),
    EASE_IN_OUT_QUINT(x -> {
        Double var1;
        return x < 0.5 ? 16.0 * x * x * x * x * x : 1.0 + 16.0 * var1 = x - 1.0 * var1 * var1 * var1 * var1;
    }),
    EASE_IN_SINE(x -> 1.0 - Math.cos(x * Math.PI / 2.0)),
    EASE_OUT_SINE(x -> Math.sin(x * Math.PI / 2.0)),
    EASE_IN_OUT_SINE(x -> 1.0 - Math.cos(Math.PI * x / 2.0)),
    EASE_IN_EXPO(x -> x == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * x - 10.0)),
    EASE_OUT_EXPO(x -> x == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * x)),
    EASE_IN_OUT_EXPO(
        x -> x == 0.0
            ? 0.0
            : (
                x == 1.0
                    ? 1.0
                    : (x < 0.5 ? Math.pow(2.0, 20.0 * x - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * x + 10.0)) / 2.0)
            )
    ),
    EASE_IN_CIRC(x -> 1.0 - Math.sqrt(1.0 - x * x)),
    EASE_OUT_CIRC(x -> {
        Double var1;
        return Math.sqrt(1.0 - var1 = x - 1.0 * var1);
    }),
    EASE_IN_OUT_CIRC(
        x -> x < 0.5 ? (1.0 - Math.sqrt(1.0 - 4.0 * x * x)) / 2.0 : (Math.sqrt(1.0 - 4.0 * (x - 1.0) * x) + 1.0) / 2.0
    ),
    SIGMOID(x -> 1.0 / (1.0 + Math.exp(-x))),
    EASE_OUT_ELASTIC(
        x -> x == 0.0
            ? 0.0
            : (
                x == 1.0
                    ? 1.0
                    : Math.pow(2.0, -10.0 * x) * Math.sin((x * 10.0 - 0.75) * (Math.PI * 2.0 / 3.0)) * 0.5 + 1.0
            )
    ),
    EASE_IN_BACK(x -> 2.70158 * x * x * x - 1.70158 * x * x),
    DECELERATE(x -> 1.0 - (x - 1.0) * (x - 1.0)),
    SMOOTH_STEP(x -> -2.0 * Math.pow(x, 3.0) + 3.0 * Math.pow(x, 2.0)),
    DYNAMIC_ISLAND(x -> 1.0 - Math.cos(x * Math.PI * (0.2 + 2.5 * Math.pow(x, 3.0))) * Math.exp(-x * 5.0));

    private final Function<Double, Double> function;

    Easing(Function<Double, Double> function) {
        this.function = function;
    }

    public double apply(double x) {
        return this.function.apply(x);
    }

    public String getName() {
        return this.name().toLowerCase().replace("_", " ");
    }
}
