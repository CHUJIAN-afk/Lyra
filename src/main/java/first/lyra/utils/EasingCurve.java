package first.lyra.utils;

import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
public interface EasingCurve {

    float apply(float progress);

    EasingCurve LINEAR = t -> t;

    EasingCurve EASE_IN_QUAD = t -> t * t;

    EasingCurve EASE_OUT_QUAD = t -> t * (2 - t);

    EasingCurve EASE_IN_OUT_QUAD = t -> t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;

    EasingCurve EASE_IN_CUBIC = t -> t * t * t;

    EasingCurve EASE_OUT_CUBIC = t -> { float t1 = t - 1; return t1 * t1 * t1 + 1; };

    EasingCurve EASE_IN_OUT_CUBIC = t -> t < 0.5f ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;

    EasingCurve EASE_IN_BACK = t -> { float s = 1.70158f; return t * t * ((s + 1) * t - s); };

    EasingCurve EASE_OUT_BACK = t -> {
        float s = 1.70158f;
        float t1 = t - 1;
        return t1 * t1 * ((s + 1) * t1 + s) + 1;
    };

    EasingCurve EASE_IN_OUT_BACK = t -> {
        float s = 1.70158f * 1.525f;
        if (t < 0.5f) {
            float t2 = 2 * t;
            return 0.5f * (t2 * t2 * ((s + 1) * t2 - s));
        } else {
            float t2 = 2 * t - 2;
            return 0.5f * (t2 * t2 * ((s + 1) * t2 + s) + 2);
        }
    };

    EasingCurve EASE_OUT_BOUNCE = t -> {
        float n = 7.5625f, d = 2.75f;
        if (t < 1 / d) return n * t * t;
        if (t < 2 / d) { float t1 = t - 1.5f / d; return n * t1 * t1 + 0.75f; }
        if (t < 2.5 / d) { float t1 = t - 2.25f / d; return n * t1 * t1 + 0.9375f; }
        float t1 = t - 2.625f / d;
        return n * t1 * t1 + 0.984375f;
    };

    EasingCurve EASE_OUT_ELASTIC = t -> {
        if (t == 0 || t == 1) return t;
        return (float) Math.pow(2, -10 * t) * (float) Math.sin((t - 0.075f) * (2 * Math.PI) / 0.3f) + 1;
    };

    default EasingCurve compose(EasingCurve outer) {
        return t -> outer.apply(this.apply(t));
    }

    default EasingCurve andThen(EasingCurve after) {
        return t -> this.apply(after.apply(t));
    }

    static BezierBuilder bezier() {
        return new BezierBuilder();
    }

    final class BezierBuilder {

        private static final int TABLE_SIZE = 256;

        private final List<Float> controlY = new ArrayList<>();
        private boolean uniformX = true;
        private final List<Float> explicitX = new ArrayList<>();

        BezierBuilder() {
        }

        public BezierBuilder control(float y) {
            uniformX = true;
            controlY.add(y);
            return this;
        }

        public BezierBuilder control(float x, float y) {
            uniformX = false;
            if (!explicitX.isEmpty() && x <= explicitX.get(explicitX.size() - 1)) {
                throw new IllegalArgumentException(
                        "控制点 X 必须单调递增：当前 x=" + x + "，前一个 x=" + explicitX.get(explicitX.size() - 1));
            }
            explicitX.add(x);
            controlY.add(y);
            return this;
        }

        public EasingCurve build() {
            if (controlY.isEmpty()) {
                return LINEAR;
            }

            int n = controlY.size() + 2;
            float[] px = new float[n];
            float[] py = new float[n];
            px[0] = 0;
            py[0] = 0;
            if (uniformX) {
                for (int i = 0; i < controlY.size(); i++) {
                    px[i + 1] = (float) (i + 1) / (controlY.size() + 1);
                    py[i + 1] = controlY.get(i);
                }
            } else {
                for (int i = 0; i < controlY.size(); i++) {
                    px[i + 1] = explicitX.get(i);
                    py[i + 1] = controlY.get(i);
                }
            }
            px[n - 1] = 1;
            py[n - 1] = 1;

            float[] tableX = new float[TABLE_SIZE + 1];
            float[] tableY = new float[TABLE_SIZE + 1];
            for (int i = 0; i <= TABLE_SIZE; i++) {
                float t = (float) i / TABLE_SIZE;
                float[] point = deCasteljau(px, py, t);
                tableX[i] = point[0];
                tableY[i] = point[1];
            }

            return progress -> {
                if (progress <= 0) return 0;
                if (progress >= 1) return 1;
                return lookupY(tableX, tableY, progress);
            };
        }

        private static float[] deCasteljau(float[] px, float[] py, float t) {
            int n = px.length;
            float[] x = px.clone();
            float[] y = py.clone();
            float invT = 1 - t;
            for (int k = n - 1; k > 0; k--) {
                for (int i = 0; i < k; i++) {
                    x[i] = x[i] * invT + x[i + 1] * t;
                    y[i] = y[i] * invT + y[i + 1] * t;
                }
            }
            return new float[]{x[0], y[0]};
        }

        private static float lookupY(float[] tableX, float[] tableY, float x) {
            for (int i = 1; i < tableX.length; i++) {
                if (tableX[i] >= x) {
                    float x0 = tableX[i - 1], x1 = tableX[i];
                    float y0 = tableY[i - 1], y1 = tableY[i];
                    float denom = x1 - x0;
                    if (denom < 1e-8f) return (y0 + y1) * 0.5f;
                    float t = (x - x0) / denom;
                    return y0 + t * (y1 - y0);
                }
            }
            return tableY[tableY.length - 1];
        }
    }
}
