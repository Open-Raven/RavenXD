package xyz.daikirai.client;

import xyz.daikirai.util.ClickUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Keystrokes {
    public static int x = 0, y = 0;
    public static int color = 0;
    public static boolean showMouseButtons = false;
    public static boolean enabled = false;
    public static boolean outline = false;

    public static Keystrokes instance;

    private static File dir() {
        return new File(Minecraft.getMinecraft().mcDataDir + File.separator + "keystrokes" + "mod");
    }

    public static void load() {
        try {
            File file = new File(dir(), "config");
            if (!file.exists()) {
                return;
            }
            BufferedReader reader = new BufferedReader(new FileReader(file));
            int n = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                switch (++n) {
                    case 1:
                        x = Integer.parseInt(line);
                        break;
                    case 2:
                        y = Integer.parseInt(line);
                        break;
                    case 3:
                        color = Integer.parseInt(line);
                        break;
                    case 4:
                        showMouseButtons = Boolean.parseBoolean(line);
                        break;
                    case 5:
                        enabled = Boolean.parseBoolean(line);
                        break;
                    case 6:
                        outline = Boolean.parseBoolean(line);
                }
            }
            reader.close();
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            File file = new File(dir(), "config");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file, false);
            writer.write(x + "\n" + y + "\n" + color + "\n" + showMouseButtons + "\n" + enabled + "\n" + outline);
            writer.close();
        } catch (Exception ignored) {
        }
    }

    private static final int[] COLORS = {16777215, 16711680, 65280, 255, 16776960, 11141290};

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Key[] keys = new Key[4];
    private final MouseButton[] buttons = new MouseButton[2];

    public Keystrokes() {
        this.keys[0] = new Key(this.mc.gameSettings.keyBindForward, 26, 2);
        this.keys[1] = new Key(this.mc.gameSettings.keyBindBack, 26, 26);
        this.keys[2] = new Key(this.mc.gameSettings.keyBindLeft, 2, 26);
        this.keys[3] = new Key(this.mc.gameSettings.keyBindRight, 50, 26);
        this.buttons[0] = new MouseButton(0, 2, 50);
        this.buttons[1] = new MouseButton(1, 38, 50);
    }

    private int color(int n) {
        if (n == 6) {
            return Color.getHSBColor((float) (System.currentTimeMillis() % 3750L) / 3750.0f, 1.0f, 1.0f).getRGB();
        }
        return COLORS[n];
    }

    private void drawButtons(int x, int y, int c) {
        for (MouseButton b : this.buttons) {
            b.draw(x, y, c);
        }
    }

    private void drawKeys(int x, int y, int c) {
        for (Key k : this.keys) {
            k.draw(x, y, c);
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (this.mc.currentScreen != null) {
            if (this.mc.currentScreen instanceof KeystrokesGui) {
                try {
                    this.mc.currentScreen.handleInput();
                } catch (IOException ignored) {
                }
            }
            return;
        }
        if (!this.mc.inGameHasFocus || this.mc.gameSettings.showDebugInfo) {
            return;
        }
        this.render();
    }

    public void render() {
        if (!enabled) {
            return;
        }
        int px = x;
        int py = y;
        int c = this.color(color);
        boolean mouse = showMouseButtons;
        ScaledResolution sr = new ScaledResolution(this.mc);
        int h = mouse ? 74 : 50;
        if (px < 0) {
            px = x = 0;
        } else if (px > sr.getScaledWidth() - 74) {
            px = x = sr.getScaledWidth() - 74;
        }
        if (py < 0) {
            py = y = 0;
        } else if (py > sr.getScaledHeight() - h) {
            py = y = sr.getScaledHeight() - h;
        }
        this.drawKeys(px, py, c);
        if (mouse) {
            this.drawButtons(px, py, c);
        }
    }

    static class Key {
        private final int ox, oy;
        private long changed;
        private boolean down = true;
        private int shade = 25;
        private double textFade = 1.0;
        private final KeyBinding key;

        Key(KeyBinding key, int ox, int oy) {
            this.key = key;
            this.ox = ox;
            this.oy = oy;
        }

        void draw(int x, int y, int c) {
            boolean pressed = this.key.isKeyDown();
            String name = Keyboard.getKeyName(this.key.getKeyCode());
            if (pressed != this.down) {
                this.down = pressed;
                this.changed = System.currentTimeMillis();
            }
            long l = System.currentTimeMillis() - this.changed;
            if (pressed) {
                this.shade = Math.min(255, (int) (2L * l));
                this.textFade = Math.max(0.0, 1.0 - (double) l / 20.0);
            } else {
                this.shade = Math.max(0, 255 - (int) (2L * l));
                this.textFade = Math.min(1.0, (double) l / 20.0);
            }
            int r = c >> 16 & 0xFF;
            int g = c >> 8 & 0xFF;
            int b = c & 0xFF;
            int line = 0xFF000000 | r << 16 | g << 8 | b;
            Gui.drawRect(x + this.ox, y + this.oy, x + this.ox + 22, y + this.oy + 22, 0x78000000 + (this.shade << 16) + (this.shade << 8) + this.shade);
            if (outline) {
                Gui.drawRect(x + this.ox, y + this.oy, x + this.ox + 22, y + this.oy + 1, line);
                Gui.drawRect(x + this.ox, y + this.oy + 21, x + this.ox + 22, y + this.oy + 22, line);
                Gui.drawRect(x + this.ox, y + this.oy, x + this.ox + 1, y + this.oy + 22, line);
                Gui.drawRect(x + this.ox + 21, y + this.oy, x + this.ox + 22, y + this.oy + 22, line);
            }
            Minecraft.getMinecraft().fontRendererObj.drawString(name, x + this.ox + 8, y + this.oy + 8,
                    -16777216 + ((int) ((double) g * this.textFade) << 16) + ((int) ((double) g * this.textFade) << 8) + (int) ((double) b * this.textFade));
        }
    }

    static class MouseButton {
        private static final String[] NAMES = {"LMB", "RMB"};
        private final int oy, ox;
        private final int button;
        private double textFade = 1.0;
        private final List<Long> clicks = new ArrayList<Long>();
        private long changed;
        private boolean down = true;
        private int shade = 255;

        MouseButton(int button, int ox, int oy) {
            this.button = button;
            this.ox = ox;
            this.oy = oy;
        }

        void draw(int x, int y, int c) {
            Minecraft mc = Minecraft.getMinecraft();
            boolean pressed = this.button == 0 ? ClickUtils.leftDown() : ClickUtils.rightDown();
            String name = NAMES[this.button];
            if (pressed != this.down) {
                this.down = pressed;
                this.changed = System.currentTimeMillis();
                if (pressed) {
                    this.clicks.add(this.changed);
                }
            }
            long l = System.currentTimeMillis() - this.changed;
            if (pressed) {
                this.shade = Math.min(255, (int) (2L * l));
                this.textFade = Math.max(0.0, 1.0 - (double) l / 20.0);
            } else {
                this.shade = Math.max(0, 255 - (int) (2L * l));
                this.textFade = Math.min(1.0, (double) l / 20.0);
            }
            int r = c >> 16 & 0xFF;
            int g = c >> 8 & 0xFF;
            int b = c & 0xFF;
            int line = 0xFF000000 | r << 16 | g << 8 | b;
            Gui.drawRect(x + this.ox, y + this.oy, x + this.ox + 34, y + this.oy + 22, 0x78000000 + (this.shade << 16) + (this.shade << 8) + this.shade);
            if (outline) {
                Gui.drawRect(x + this.ox, y + this.oy, x + this.ox + 34, y + this.oy + 1, line);
                Gui.drawRect(x + this.ox, y + this.oy + 21, x + this.ox + 34, y + this.oy + 22, line);
                Gui.drawRect(x + this.ox, y + this.oy, x + this.ox + 1, y + this.oy + 22, line);
                Gui.drawRect(x + this.ox + 33, y + this.oy, x + this.ox + 34, y + this.oy + 22, line);
            }
            mc.fontRendererObj.drawString(name, x + this.ox + 8, y + this.oy + 4,
                    -16777216 + ((int) ((double) r * this.textFade) << 16) + ((int) ((double) g * this.textFade) << 8) + (int) ((double) b * this.textFade));
            String left = ClickUtils.totalLeftCps() + " CPS";
            String right = ClickUtils.totalRightCps() + " CPS";
            int lw = mc.fontRendererObj.getStringWidth(left);
            int rw = mc.fontRendererObj.getStringWidth(right);
            boolean isLeft = this.button == 0;
            int w = isLeft ? lw : rw;
            GL11.glScalef(0.5f, 0.5f, 0.5f);
            mc.fontRendererObj.drawString(isLeft ? left : right, (x + this.ox + 17) * 2 - w / 2, (y + this.oy + 14) * 2,
                    -16777216 + ((int) (255.0 * this.textFade) << 16) + ((int) (255.0 * this.textFade) << 8) + (int) (255.0 * this.textFade));
            GL11.glScalef(2.0f, 2.0f, 20.0f);
        }
    }
}
