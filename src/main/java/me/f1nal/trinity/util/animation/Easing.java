package me.f1nal.trinity.util.animation;

import java.util.function.Function;

public enum Easing {
    LINEAR(x -> x),
    EASE_IN_QUAD(x -> x * x),
    EASE_OUT_QUAD(x -> x * (2 - x)),
    EASE_IN_OUT_QUAD(x -> x < 0.5 ? 2 * x * x : -1 + (4 - 2 * x) * x),
    EASE_IN_CUBIC(x -> x * x * x),
    EASE_OUT_CUBIC(x -> (--x) * x * x + 1),
    EASE_IN_OUT_CUBIC(x -> x < 0.5 ? 4 * x * x * x : (x - 1) * (2 * x - 2) * (2 * x - 2) + 1),
    EASE_IN_QUART(x -> x * x * x * x),
    EASE_OUT_QUART(x -> 1 - (--x) * x * x * x),
    EASE_IN_OUT_QUART(x -> x < 0.5 ? 8 * x * x * x * x : 1 - 8 * (--x) * x * x * x),
    EASE_IN_QUINT(x -> x * x * x * x * x),
    EASE_OUT_QUINT(x -> 1 + (--x) * x * x * x * x),
    EASE_IN_OUT_QUINT(x -> x < 0.5 ? 16 * x * x * x * x * x : 1 + 16 * (--x) * x * x * x * x),
    EASE_IN_EXPO(x -> x == 0 ? 0 : (float) Math.pow(2, 10 * x - 10)),
    EASE_OUT_EXPO(x -> x == 1 ? 1 : 1 - (float) Math.pow(2, -10 * x)),
    EASE_IN_OUT_EXPO(x -> x == 0 ? 0 : x == 1 ? 1 : x < 0.5 ? (float) Math.pow(2, 20 * x - 10) * 0.5F : (2 - (float) Math.pow(2, -20 * x + 10)) * 0.5F),
    EASE_IN_CIRC(x -> 1 - (float) Math.sqrt(1 - x * x)),
    EASE_OUT_CIRC(x -> (float) Math.sqrt(1 - (--x) * x)),
    EASE_IN_OUT_CIRC(x -> x < 0.5 ? (1 - (float) Math.sqrt(1 - 4 * x * x)) * 0.5F : ((float) Math.sqrt(1 - 4 * (x - 1) * x) + 1) * 0.5F),
    SIGMOID(x -> 1 / (1 + (float) Math.exp(-x))),
    EASE_OUT_ELASTIC(x -> x == 0 ? 0 : x == 1 ? 1 : (float) (Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * ((2 * Math.PI) / 3)) * 0.5F + 1)),
    EASE_IN_BACK(x -> (1.70158F + 1.0F) * x * x * x - 1.70158F * x * x);

    private final Function<Float, Float> function;

    Easing(final Function<Float, Float> function) {
        this.function = function;
    }

    public Function<Float, Float> getFunction() {
        return function;
    }
}