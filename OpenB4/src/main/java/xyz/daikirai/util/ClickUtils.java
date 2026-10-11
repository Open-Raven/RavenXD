package xyz.daikirai.util;

import xyz.daikirai.client.MiddleClickFriends;
import net.minecraftforge.client.event.MouseEvent;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

public final class ClickUtils {
    private static final List<Long> leftClicks = new ArrayList<Long>();
    private static final List<Long> attackClicks = new ArrayList<Long>();
    private static final List<Long> rightClicks = new ArrayList<Long>();
    private static final List<Long> useClicks = new ArrayList<Long>();
    public static long lastLeftClick;
    public static long lastAttackClick;
    public static long lastRightClick;
    public static long lastUseClick;

    private ClickUtils() {
    }

    public static void onMouse(MouseEvent e) {
        if (!e.buttonstate) {
            return;
        }
        if (e.button == 0) {
            onLeftClick();
        } else if (e.button == 1) {
            onRightClick();
        } else {
            MiddleClickFriends.onMouse(e);
        }
    }

    public static void onLeftClick() {
        lastLeftClick = System.currentTimeMillis();
        leftClicks.add(lastLeftClick);
    }

    public static void onAttackClick() {
        lastAttackClick = System.currentTimeMillis();
        attackClicks.add(lastAttackClick);
    }

    public static void onRightClick() {
        lastRightClick = System.currentTimeMillis();
        rightClicks.add(lastRightClick);
    }

    public static void onUseClick() {
        lastUseClick = System.currentTimeMillis();
        useClicks.add(lastUseClick);
    }

    public static boolean leftDown() {
        return Mouse.isButtonDown(0) || System.currentTimeMillis() - lastAttackClick < 50L;
    }

    public static boolean rightDown() {
        return Mouse.isButtonDown(1) || System.currentTimeMillis() - lastUseClick < 50L;
    }

    public static int leftCps() {
        prune(leftClicks);
        return leftClicks.size();
    }

    public static int totalLeftCps() {
        prune(leftClicks);
        prune(attackClicks);
        return leftClicks.size() + attackClicks.size();
    }

    public static int rightCps() {
        prune(rightClicks);
        return rightClicks.size();
    }

    public static int totalRightCps() {
        prune(rightClicks);
        prune(useClicks);
        return rightClicks.size() + useClicks.size();
    }

    private static void prune(List<Long> list) {
        long l = System.currentTimeMillis() - 1000L;
        list.removeIf(t -> t < l);
    }
}
