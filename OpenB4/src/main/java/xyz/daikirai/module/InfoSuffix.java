package xyz.daikirai.module;

import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.Strings;

public final class InfoSuffix {
    private InfoSuffix() {
    }

    private static String display(Module m, String setting) {
        return slider(m, setting).getDisplay(false);
    }

    private static SliderSetting slider(Module m, String setting) {
        return (SliderSetting) ModuleManager.setting(m, setting);
    }

    public static String get(Module m) {
        switch (m.getName()) {
            case "Speed":
                return display(m, "Speed");
            case "BedAura":
                return display(m, "Break mode");
            case "Timer": {
                int mode = (int) slider(m, "Multiplier slider").getInput();
                return display(m, mode == 1 ? "Slider B" : mode == 2 ? "Slider C" : "Slider A");
            }
            case "Lag Range": {
                SliderSetting close = slider(m, "Packet delay (close)");
                SliderSetting far = slider(m, "Packet delay (far)");
                if (close.isAtMin()) {
                    return far.isAtMin() ? null : far.getDisplay(false);
                }
                return close.getDisplay(false);
            }
            case "WTap":
                return display(m, "Chance");
            case "Velocity":
                return display(m, "Horizontal") + " " + display(m, "Vertical");
            case "Hit Select": {
                SliderSetting pause = slider(m, "Pause");
                return pause.isAtMin() ? null : pause.getDisplay(false);
            }
            case "NoSlow":
                return display(m, "Mode");
            case "AntiKnockback": {
                SliderSetting delay = slider(m, "Delay");
                SliderSetting h = slider(m, "Horizontal");
                SliderSetting v = slider(m, "Vertical");
                if (delay.isNotDefault() && !h.isNotDefault() && !v.isNotDefault()) {
                    return delay.getDisplay(false);
                }
                return h.getDisplay(false) + " " + v.getDisplay(false);
            }
            case "Jump Reset":
                return display(m, "Chance");
            case "Backtrack":
                return display(m, "Delay");
            case "Hitbox":
                return display(m, "Multiplier");
            case "Reach": {
                double max = slider(m, "Max").getInput();
                double min = slider(m, "Min").getInput();
                double d = Math.abs(max - min) / 2.0;
                return SliderSetting.formatNumber(Strings.round(min + d, 2));
            }
            case "FastMine":
                return display(m, "Break speed");
            default:
                return null;
        }
    }
}
