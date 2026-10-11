package xyz.daikirai.module.setting;

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
        d = Math.max(this.min, Math.min(this.max, d));
        d = (double) Math.round(d * (1.0 / this.interval)) / (1.0 / this.interval);
        if (this.value != d) {
            this.value = d;
            xyz.daikirai.client.Profiles.markModified();
        }
        this.setVisualInput(d);
    }

    public static void order(SliderSetting min, SliderSetting max) {
        if (min.getInput() > max.getInput()) {
            double d = min.getInput();
            min.setValue(max.getInput());
            max.setValue(d);
        }
    }

    public void setValueRaw(double d) {
        this.value = d;
        this.setVisualInput(d);
        xyz.daikirai.client.Profiles.markModified();
    }

    public String getDisplay(boolean colored) {
        String s;
        if (this.isMode()) {
            if (colored) {
                if (this.labels != null && (s = this.labels.get(this.value)) != null) {
                    return s;
                }
                return "§e" + this.options[(int) this.value];
            }
            return this.options[(int) this.value];
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
