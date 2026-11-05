package openraven.ravenxd.module.setting;

import java.util.HashMap;

public class SliderSetting extends Setting {
    public String prefix = "";
    public double max;
    public double interval;
    public double min;
    public HashMap<Double, String> labels;
    public double defaultValue;
    public double value;
    public String[] options;
    public String settingName;
    public double input;
    public String suffix = "";

    public SliderSetting(String name, String prefix, String suffix, double value, double min, double max, double interval, String[] options, HashMap<Double, String> labels) {
        super(name);
        this.settingName = name;
        this.prefix = prefix == null ? "" : prefix;
        this.suffix = suffix == null ? "" : suffix;
        this.value = this.input = value;
        this.defaultValue = this.input;
        this.min = min;
        this.max = max;
        this.interval = interval;
        this.options = options;
        this.labels = labels;
        // Same sanitization for the initial value (modules.json /
        // script defaults are untrusted input too). No-op for the
        // shipped modules.json, which is fully in range.
        this.value = this.input = sanitized(this.value);
        this.defaultValue = this.input;
    }

    @Override
    public String getName() {
        return this.settingName;
    }

    public boolean isMode() {
        return this.options != null;
    }

    public double getInput() {
        return this.value;
    }

    public boolean isAtMin() {
        return this.value == this.min;
    }

    public boolean isNotDefault() {
        return this.value != this.defaultValue;
    }

    public double getVisualInput() {
        return this.input;
    }

    public void setVisualInput(double d) {
        this.input = d;
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public void setValue(double d) {
        d = sanitized(d);
        if (this.value != d) {
            this.value = d;
            openraven.ravenxd.client.Profiles.markModified();
        }
        this.setVisualInput(d);
    }

    /**
     * Clamp to [min, max] and snap to the step, rejecting non-finite input.
     * NaN previously propagated straight into `value` (Math.min/max return
     * NaN for NaN input), and a zero `interval` made the rounding maths
     * collapse every value to 0. Non-finite input falls back to `min`
     * (not `defaultValue`: at construction time the default may itself
     * be the non-finite value being sanitized).
     * Callers reach this with untrusted numbers (profile JSON via
     * setValueRaw, Console `.setting` commands via Double.parseDouble,
     * which accepts "NaN"/"Infinity" literals), so this must be total.
     */
    private double sanitized(double d) {
        if (Double.isNaN(d)) {
            d = this.min;
        } else if (Double.isInfinite(d)) {
            d = d > 0 ? this.max : this.min;
        }
        d = Math.max(this.min, Math.min(this.max, d));
        if (this.interval != 0.0 && !Double.isNaN(this.interval) && !Double.isInfinite(this.interval)) {
            d = (double) Math.round(d * (1.0 / this.interval)) / (1.0 / this.interval);
            // Rounding can land a hair outside the range via float error.
            d = Math.max(this.min, Math.min(this.max, d));
        }
        return d;
    }

    public static void order(SliderSetting min, SliderSetting max) {
        if (min.getInput() > max.getInput()) {
            double d = min.getInput();
            min.setValue(max.getInput());
            max.setValue(d);
        }
    }

    public void setValueRaw(double d) {
        // Previously this bypassed clamping/rounding entirely, so a
        // corrupt profile or a Console ".setting ... NaN" command could
        // store NaN/Infinity/out-of-range values that later crash
        // getDisplay() (mode index) or poison module maths. Raw now
        // means "skip the modified-flag / change check", not "skip
        // validation" -- sanitized() is idempotent for valid values,
        // which are already clamped and snapped when first set.
        d = sanitized(d);
        this.value = d;
        this.setVisualInput(d);
        openraven.ravenxd.client.Profiles.markModified();
    }

    public String getDisplay(boolean colored) {
        String s;
        if (this.isMode()) {
            // Clamp the index: `value` can be set directly (public
            // field) or from an older profile, and an out-of-range
            // index used to throw ArrayIndexOutOfBoundsException here,
            // in the HUD render path.
            int index = (int) this.value;
            if (index < 0) {
                index = 0;
            } else if (index >= this.options.length) {
                index = this.options.length - 1;
            }
            if (colored) {
                if (this.labels != null && (s = this.labels.get(this.value)) != null) {
                    return s;
                }
                return "§e" + this.options[index];
            }
            return this.options[index];
        }
        if (this.labels != null && (s = this.labels.get(this.value)) != null) {
            return s;
        }
        double d = (double) Math.round(this.value * 1000.0) / 1000.0;
        return (colored ? "§b" : "") + this.prefix + formatNumber(d) + this.suffix;
    }

    public static String formatNumber(double d) {
        return d == Math.floor(d) ? Long.toString((long) d) : Double.toString(d);
    }
}
