package xyz.daikirai.script;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.ClientConfig;
import xyz.daikirai.gui.Component;
import xyz.daikirai.gui.ModuleComponent;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.DescriptionSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraftforge.common.MinecraftForge;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class ScriptManager {
    public static final String EXTENSION = ".java";
    public static final String MANAGER_NAME = "Manager";
    public static final String TEMP_PATH = System.getProperty("java.io.tmpdir") + File.separator + "dmvk7zda.3ep";
    public static final File tempDir = new File(TEMP_PATH);
    public static final String BASE_CLASS = ScriptDefaults.class.getName();
    public static final Class<?> baseClass = ScriptDefaults.class;
    public static String classpath = ownJar();
    public static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    public static final List<String> IMPORTS = Arrays.asList("java.awt.Color", "java.util.List", "java.util.ArrayList",
            "java.util.Arrays", "java.util.Map", "java.util.HashMap", "java.util.HashSet",
            "java.util.concurrent.ConcurrentHashMap", "java.util.LinkedHashMap", "java.util.LinkedList",
            "java.util.Collections", "java.util.Iterator", "java.util.Comparator", "java.util.concurrent.atomic.AtomicBoolean",
            "java.util.concurrent.atomic.AtomicInteger", "java.util.concurrent.atomic.AtomicLong", "java.util.Random",
            "java.util.regex.Pattern", "java.util.regex.Matcher");
    public static final HashSet<String> BLOCKED = new HashSet<String>(Arrays.asList("getClass", ".getSuperclass",
            ".getStackTrace", "ClassLoader(", ".getDeclaredField", ".getField", "ProcessBuilder:", ".getDeclaredMethod",
            "java.", "Package:", "Package.", "sun.", "Runtime.", "System.", "Class:", "Runtime:", "System:", "Class.",
            "com.", ".getConstructor", "printStackTrace", ".getMethod", "Thread:", "jdk.", ".getDeclaredConstructor",
            "ClassLoader:", ".getPackage", "StackWalker:", "javax.", "ClassLoader.", "StackWalker.", "Thread.",
            "ProcessBuilder("));

    public static final HashMap<Script, Module> scripts = new HashMap<Script, Module>();
    public static final HashMap<String, String> classNames = new HashMap<String, String>();
    public static final List<Object[]> pendingSettings = new ArrayList<Object[]>();
    public static ScriptEvents events;
    public static boolean loading;
    public static int loadedCount;
    public static int removedCount;
    private static boolean firstLoad = true;
    private static boolean recompileFirst = true;

    private ScriptManager() {
    }

    private static String ownJar() {
        try {
            java.net.URL url = ScriptDefaults.class.getProtectionDomain().getCodeSource().getLocation();
            String s = url.toString();
            if (s.startsWith("jar:")) {
                int bang = s.indexOf("!/");
                url = new java.net.URL(s.substring(4, bang == -1 ? s.length() : bang));
            }
            String path = new File(url.toURI()).getAbsolutePath();
            return path.replace("%", "%25").replace("+", "%2B");
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isValidName(String string) {
        if (string.isEmpty()) {
            return false;
        }
        if (string.length() > 14) {
            Chat.print("&7Script &b" + string + "&7 blocked, name &ctoo long&7!");
            return false;
        }
        Module module = ModuleManager.getIgnoreCase(string);
        if (module != null && !module.isScript()) {
            Chat.print("&7Script &b" + string + "&7 blocked, &cconflicting &7module name!");
            return false;
        }
        for (char c : string.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') continue;
            Chat.print("&7Script &b" + string + "&7 blocked, &cinvalid characters&7!");
            return false;
        }
        return true;
    }

    public static void onLoaded(Script script) {
        ScriptInvoker.call(script, "onLoad");
        Module module = scripts.get(script);
        if (!pendingSettings.isEmpty()) {
            for (Object[] objectArray : pendingSettings) {
                switch ((Integer) objectArray[0]) {
                    case 0: {
                        module.registerSetting(new DescriptionSetting((String) objectArray[1]));
                        break;
                    }
                    case 1: {
                        module.registerSetting(new ButtonSetting((String) objectArray[1], (Boolean) objectArray[2]));
                        break;
                    }
                    case 2: {
                        module.registerSetting(new SliderSetting((String) objectArray[1], "", (String) objectArray[2],
                                (Double) objectArray[3], (Double) objectArray[4], (Double) objectArray[5], (Double) objectArray[6], null, null));
                        break;
                    }
                    case 3: {
                        String[] modes = (String[]) objectArray[4];
                        module.registerSetting(new SliderSetting((String) objectArray[1], "", (String) objectArray[2],
                                (double) (Integer) objectArray[3], 0.0, modes.length - 1, 1.0, modes, null));
                    }
                }
            }
            pendingSettings.clear();
        }
        Category.scripts.manager.addModule(module);
    }

    public static void onDisable(Script script) {
        if (events != null) {
            boolean none = true;
            for (Map.Entry<Script, Module> entry : scripts.entrySet()) {
                if (!entry.getValue().isEnabled()) continue;
                none = false;
                break;
            }
            if (none) {
                MinecraftForge.EVENT_BUS.unregister(events);
                events = null;
                ScriptInvoker.callStatic(baseClass, "protected_resetCache", Void.TYPE, false);
            }
        }
        ScriptInvoker.call(script, "onDisable");
    }

    public static void onEnable(Script script) {
        if (events == null) {
            ScriptInvoker.callStatic(baseClass, "protected_setupCache", Void.TYPE);
            events = new ScriptEvents();
            MinecraftForge.EVENT_BUS.register(events);
        }
        ScriptInvoker.call(script, "onEnable");
    }

    public static Script load(File file) {
        if (file.getName().startsWith("_") || !file.getName().endsWith(EXTENSION)) {
            return null;
        }
        String name = file.getName().replace(EXTENSION, "");
        if (!isValidName(name)) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        boolean encrypted = false;
        String trailer = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            List<String> lines = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("decrypt - \"")) {
                    String payload = ScriptCrypto.decryptLine(line);
                    if (payload != null) {
                        String t = ScriptCrypto.trailer(payload);
                        payload = ScriptCrypto.source(payload);
                        if (payload != null) {
                            encrypted = true;
                            trailer = t;
                            lines = Arrays.asList(payload.split("\n"));
                            break;
                        }
                    }
                }
                lines.add(line);
            }
            for (String s : lines) {
                stringBuilder.append(s).append('\n');
            }
            reader.close();
        } catch (Exception exception) {
        }
        if (stringBuilder.length() == 0) {
            return null;
        }
        String source = stringBuilder.toString();
        if (!ScriptText.isSafe(source, encrypted)) {
            Chat.print("&7Script &b" + name + "&7 blocked, &cunsafe code&7 detected!");
            return null;
        }
        Script script = null;
        String hash = ScriptCrypto.md5(name + source);
        for (Map.Entry<Script, Module> entry : scripts.entrySet()) {
            if (!entry.getKey().hash.equals(hash)) continue;
            if (!recompileFirst) {
                script = entry.getKey();
                Module module = entry.getValue();
                if (Category.scripts.manager != null) {
                    for (Component c : Category.scripts.manager.modules) {
                        if (((ModuleComponent) c).mod != module) continue;
                        module.disable();
                        Category.scripts.manager.removeModule(module, (ModuleComponent) c);
                        break;
                    }
                }
            } else {
                recompileFirst = false;
            }
            break;
        }
        if (script == null) {
            script = new Script(name);
            script.encrypted = encrypted;
            if (encrypted && !script.deriveNames(trailer)) {
                return null;
            }
            script.loadString = false;
            script.hash = hash;
            script.compile(source);
            classNames.put(script.className, script.name);
        } else {
            script.name = name;
            if (script.loadErrors != null) {
                script.printErrors(script.loadErrors);
            }
        }
        Module module = new Module(script, 0);
        scripts.put(script, module);
        return script;
    }

    public static void reload() {
        removedCount = 0;
        loadedCount = 0;
        if (classpath.isEmpty() || loading || compiler == null) {
            return;
        }
        loading = true;
        try {
            if (firstLoad) {
                firstLoad = false;
                File[] old;
                if (tempDir.exists() && tempDir.isDirectory() && (old = tempDir.listFiles()) != null && old.length > 0) {
                    for (File f : old) {
                        f.delete();
                    }
                }
            }
            File dir = new File(ClientConfig.DIR, "scripts");
            if (!dir.exists() || !dir.isDirectory()) {
                dir.mkdirs();
                return;
            }
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                if (scripts.isEmpty()) {
                    return;
                }
                Iterator<Map.Entry<Script, Module>> it = scripts.entrySet().iterator();
                while (it.hasNext()) {
                    Script script = it.next().getKey();
                    script.unload();
                    classNames.remove(script.className);
                    it.remove();
                }
                return;
            }
            HashSet<String> seen = new HashSet<String>();
            ArrayList<Script> loaded = new ArrayList<Script>();
            for (File file : files) {
                if (file.isFile() && file.canRead() && !seen.contains(file.getName())) {
                    seen.add(file.getName());
                    Script script = load(file);
                    if (script != null) {
                        loaded.add(script);
                        ++loadedCount;
                    }
                }
            }
            Iterator<Map.Entry<Script, Module>> it = scripts.entrySet().iterator();
            while (it.hasNext()) {
                Script script = it.next().getKey();
                if (loaded.contains(script)) continue;
                script.unload();
                classNames.remove(script.className);
                it.remove();
                ++removedCount;
            }
            for (Script script : loaded) {
                onLoaded(script);
            }
        } finally {
            loading = false;
        }
    }
}
