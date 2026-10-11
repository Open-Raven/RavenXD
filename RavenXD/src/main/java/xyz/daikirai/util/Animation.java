package xyz.daikirai.util;

public class Animation {
    public long start;
    public float last;
    public float value;
    public float duration;

    public Animation(float duration) {
        this.duration = duration;
    }

    public void start() {
        this.last = 0.0f;
        this.start = System.currentTimeMillis();
    }

    public float getValue(float from, float to, int mode) {
        if (this.last == to) {
            return this.last;
        }
        float t = (float) (System.currentTimeMillis() - this.start) / this.duration;
        switch (mode) {
            case 1:
                t = t < 0.5f ? 4.0f * t * t * t : (t - 1.0f) * (2.0f * t - 2.0f) * (2.0f * t - 2.0f) + 1.0f;
                break;
            case 2:
                t = (float) (1.0 - Math.pow(1.0f - t, 5.0));
                break;
            case 3:
                t = bounce(t);
        }
        float v = from + t * (to - from);
        if (to < v) {
            v = to;
        }
        if (v == to) {
            this.last = v;
        }
        this.value = v;
        return this.value;
    }

    public int getValueInt(int from, int to, int mode) {
        return Math.round(this.getValue(from, to, mode));
    }

    public float bounce(float f) {
        double n = 7.5625;
        double d = 2.75;
        if ((double) f < 1.0 / d) {
            return (float) (n * (double) f * (double) f);
        }
        if ((double) f < 2.0 / d) {
            f = (float) ((double) f - 1.5 / d);
            return (float) (n * (double) f * (double) f + 0.75);
        }
        if ((double) f < 2.5 / d) {
            f = (float) ((double) f - 2.25 / d);
            return (float) (n * (double) f * (double) f + 0.9375);
        }
        f = (float) ((double) f - 2.625 / d);
        return (float) (n * (double) f * (double) f + 0.984375);
    }
}
