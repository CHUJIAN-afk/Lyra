package first.lyra.client.render.animated;

import java.util.Locale;

/**
 * Gecko/Bedrock easing 名称的轻量实现，不依赖 GeckoLib。
 */
final class Easing {

    private Easing() {
    }

    static double apply(String easingName, double t) {
        String name = easingName == null ? "linear" : easingName.toLowerCase(Locale.ROOT);
        return switch (name) {
            case "none", "linear" -> t;
            case "step" -> t <= 0 ? 0 : 1;
            case "easeinsine" -> easeIn(t, Easing::sine);
            case "easeoutsine" -> easeOut(t, Easing::sine);
            case "easeinoutsine" -> easeInOut(t, Easing::sine);
            case "easeinquad" -> easeIn(t, Easing::quad);
            case "easeoutquad" -> easeOut(t, Easing::quad);
            case "easeinoutquad" -> easeInOut(t, Easing::quad);
            case "easeincubic" -> easeIn(t, Easing::cubic);
            case "easeoutcubic" -> easeOut(t, Easing::cubic);
            case "easeinoutcubic" -> easeInOut(t, Easing::cubic);
            case "easeinquart" -> easeIn(t, v -> pow(v, 4));
            case "easeoutquart" -> easeOut(t, v -> pow(v, 4));
            case "easeinoutquart" -> easeInOut(t, v -> pow(v, 4));
            case "easeinquint" -> easeIn(t, v -> pow(v, 5));
            case "easeoutquint" -> easeOut(t, v -> pow(v, 5));
            case "easeinoutquint" -> easeInOut(t, v -> pow(v, 5));
            case "easeinexpo" -> easeIn(t, Easing::expo);
            case "easeoutexpo" -> easeOut(t, Easing::expo);
            case "easeinoutexpo" -> easeInOut(t, Easing::expo);
            case "easeincirc" -> easeIn(t, Easing::circ);
            case "easeoutcirc" -> easeOut(t, Easing::circ);
            case "easeinoutcirc" -> easeInOut(t, Easing::circ);
            case "easeinback" -> easeIn(t, Easing::back);
            case "easeoutback" -> easeOut(t, Easing::back);
            case "easeinoutback" -> easeInOut(t, Easing::back);
            case "easeinelastic" -> easeIn(t, Easing::elastic);
            case "easeoutelastic" -> easeOut(t, Easing::elastic);
            case "easeinoutelastic" -> easeInOut(t, Easing::elastic);
            case "easeinbounce" -> easeIn(t, Easing::bounce);
            case "easeoutbounce" -> easeOut(t, Easing::bounce);
            case "easeinoutbounce" -> easeInOut(t, Easing::bounce);
            default -> t;
        };
    }

    private static double easeIn(double t, Function fn) {
        return fn.apply(t);
    }

    private static double easeOut(double t, Function fn) {
        return 1 - fn.apply(1 - t);
    }

    private static double easeInOut(double t, Function fn) {
        if (t < 0.5) {
            return fn.apply(t * 2) / 2;
        }
        return 1 - fn.apply((1 - t) * 2) / 2;
    }

    private static double sine(double t) {
        return 1 - Math.cos(t * Math.PI / 2);
    }

    private static double quad(double t) {
        return t * t;
    }

    private static double cubic(double t) {
        return t * t * t;
    }

    private static double pow(double t, double power) {
        return Math.pow(t, power);
    }

    private static double expo(double t) {
        return Math.pow(2, 10 * (t - 1));
    }

    private static double circ(double t) {
        return 1 - Math.sqrt(1 - t * t);
    }

    private static double back(double t) {
        double c = 1.70158;
        return t * t * ((c + 1) * t - c);
    }

    private static double elastic(double t) {
        return 1 - Math.pow(Math.cos(t * Math.PI / 2), 3) * Math.cos(t * Math.PI);
    }

    private static double bounce(double t) {
        double n = 0.5;
        return Math.min(Math.min(b1(t), b2(t, n)), Math.min(b3(t, n), b4(t, n)));
    }

    private static double b1(double x) {
        return 121d / 16d * x * x;
    }

    private static double b2(double x, double n) {
        return 121d / 4d * n * Math.pow(x - 6d / 11d, 2) + 1 - n;
    }

    private static double b3(double x, double n) {
        return 121 * n * n * Math.pow(x - 9d / 11d, 2) + 1 - n * n;
    }

    private static double b4(double x, double n) {
        return 484 * n * n * n * Math.pow(x - 10.5d / 11d, 2) + 1 - n * n * n;
    }

    @FunctionalInterface
    private interface Function {
        double apply(double t);
    }
}
