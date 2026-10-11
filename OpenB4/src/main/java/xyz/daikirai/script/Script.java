package xyz.daikirai.script;

import xyz.daikirai.client.Chat;
import xyz.daikirai.gui.Component;
import xyz.daikirai.gui.ModuleComponent;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Script {
    public boolean failed;
    public Class<?> scriptClass;
    public String hash;
    public Object instance;
    public URLClassLoader classLoader;
    public String name;
    public String[] encryptedNames;
    public String className;
    public boolean encrypted;
    public boolean loadString;
    public List<String> loadErrors;

    public Script(String string) {
        this.name = string;
        this.className = ScriptText.randomClassName(200);
    }

    public String methodName(ScriptEvent event) {
        if (!this.encrypted || this.encryptedNames == null) {
            return event.getNormalName();
        }
        return this.encryptedNames[event.ordinal()];
    }

    public void printErrors(List<String> list) {
        if (!ScriptUtil.inGame()) {
            return;
        }
        if (this.loadString || this.encrypted) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 1; i < list.size(); ++i) {
                String string = Chat.strip(Chat.color(list.get(i).trim()));
                stringBuilder.append(string).append("\n");
            }
            String string2 = list.get(0);
            String string = "";
            try {
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(1, new SecretKeySpec(ScriptCrypto.logKey().getBytes(StandardCharsets.UTF_8), "AES"));
                byte[] object = cipher.doFinal(stringBuilder.toString().trim().getBytes(StandardCharsets.UTF_8));
                string = Base64.getEncoder().encodeToString(object);
            } catch (Exception exception) {
            }
            Chat.print(string2);
            Chat.print(" &7err: &cHidden due to " + (this.loadString ? "load string" : "encryption"));
            if (string.isEmpty()) {
                Chat.print(" &7enc: &cError encrypting error log");
            } else {
                ChatComponentText text = new ChatComponentText(Chat.color(Chat.PREFIX + " &7enc: &cClick to copy"));
                ChatStyle style = new ChatStyle();
                style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(Chat.color("&7Click to copy"))));
                style.setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, string));
                Defaults.mc.thePlayer.addChatMessage(text.setChatStyle(style));
            }
            return;
        }
        for (String string : list) {
            Chat.print(string);
        }
    }

    public void reportError(Exception exception, boolean runtime) {
        if (exception.getCause() == null) {
            return;
        }
        ArrayList<String> arrayList = new ArrayList<String>();
        if (runtime) {
            arrayList.add("&7Runtime error during script &b" + this.name);
        } else {
            arrayList.add("&7Error loading script &b" + this.name);
        }
        arrayList.add(" &7err: &c" + exception.getCause().getClass().getSimpleName());
        StackTraceElement[] stackTraceElementArray = exception.getCause().getStackTrace();
        if (stackTraceElementArray.length > 0) {
            StackTraceElement stackTraceElement = stackTraceElementArray[0];
            for (StackTraceElement stackTraceElement2 : exception.getCause().getStackTrace()) {
                if (!stackTraceElement2.getClassName().equalsIgnoreCase(this.className)) continue;
                stackTraceElement = stackTraceElement2;
                break;
            }
            arrayList.add(" &7line: &c" + (stackTraceElement.getLineNumber() - 20));
            arrayList.add(" &7src: &c" + (runtime ? stackTraceElement.getMethodName() : "init"));
        }
        this.printErrors(arrayList);
        if (!runtime) {
            if (this.loadErrors == null) {
                this.loadErrors = new ArrayList<String>();
            }
            this.loadErrors.addAll(arrayList);
        }
    }

    public void deleteClassFiles() {
        File[] fileArray = ScriptManager.tempDir.listFiles(this::isOwnClassFile);
        if (fileArray == null) {
            return;
        }
        for (File file : fileArray) {
            file.delete();
        }
    }

    public boolean deriveNames(String string) {
        if (string == null || string.length() != 32) {
            return false;
        }
        ScriptEvent[] events = ScriptEvent.values();
        this.encryptedNames = new String[events.length];
        for (int i = 0; i < events.length; ++i) {
            String string2 = events[i].deriveEncryptedName(string);
            if (string2 == null) {
                this.encryptedNames = null;
                return false;
            }
            this.encryptedNames[i] = string2;
        }
        return true;
    }

    public boolean isOwnClassFile(File file, String string) {
        return string.equals(this.className + ".class") || string.startsWith(this.className + "$") && string.endsWith(".class");
    }

    public boolean isInnerClassFile(File file, String string) {
        return string.startsWith(this.className + "$") && string.endsWith(".class");
    }

    public void removeModule() {
        Module module = ScriptManager.scripts.get(this);
        if (module != null && Category.scripts.manager != null) {
            for (Component c : Category.scripts.manager.modules) {
                if (((ModuleComponent) c).mod != module) continue;
                module.disable();
                Category.scripts.manager.removeModule(module, (ModuleComponent) c);
                break;
            }
        }
    }

    public boolean compile(String source) {
        if (!ScriptManager.tempDir.isDirectory()) {
            ScriptManager.tempDir.mkdirs();
        }
        if (ScriptManager.compiler == null) {
            this.failed = true;
            return false;
        }
        JavaFileManager javaFileManager = null;
        try {
            Diagnostics diagnostics = new Diagnostics(this);
            javaFileManager = ScriptManager.compiler.getStandardFileManager(diagnostics, null, null);
            ArrayList<String> arrayList = new ArrayList<String>();
            arrayList.add("-d");
            arrayList.add(ScriptManager.TEMP_PATH);
            arrayList.add("-XDuseUnsharedTable");
            boolean deobf = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
            if (!deobf) {
                arrayList.add("-classpath");
                String classpath = ScriptManager.classpath;
                try {
                    classpath = URLDecoder.decode(classpath, "UTF-8");
                } catch (UnsupportedOperationException | UnsupportedEncodingException e) {
                }
                arrayList.add(classpath);
            }
            StringBuilder stringBuilder = new StringBuilder();
            for (String imp : ScriptManager.IMPORTS) {
                stringBuilder.append("import ").append(imp).append(";\n");
            }
            stringBuilder.append("public class ").append(this.className).append(" extends ").append(ScriptManager.BASE_CLASS).append(" {\n").append(source).append("\n}");
            Source src = new Source(this.className, stringBuilder.toString());
            ScriptManager.compiler.getTask(null, javaFileManager, diagnostics, arrayList, null, Collections.singletonList(src)).call();
            this.classLoader = new URLClassLoader(new URL[]{ScriptManager.tempDir.toURI().toURL()}, Launch.classLoader);
            this.scriptClass = this.classLoader.loadClass(this.className);
            this.instance = this.scriptClass.getDeclaredConstructor().newInstance();
            File[] fileArray = ScriptManager.tempDir.listFiles(this::isInnerClassFile);
            if (fileArray != null) {
                for (File file : fileArray) {
                    String inner = file.getName().substring(0, file.getName().length() - ".class".length());
                    this.classLoader.loadClass(inner);
                }
            }
            return true;
        } catch (Exception exception) {
            this.failed = true;
            this.reportError(exception, false);
        } finally {
            this.deleteClassFiles();
            if (javaFileManager != null) {
                try {
                    javaFileManager.close();
                } catch (IOException iOException) {
                }
            }
        }
        return false;
    }

    public void unload() {
        this.scriptClass = null;
        this.instance = null;
        this.encryptedNames = null;
        if (this.classLoader != null) {
            try {
                this.classLoader.close();
            } catch (IOException iOException) {
            }
            this.classLoader = null;
        }
        this.deleteClassFiles();
        this.removeModule();
    }

    static class Source extends SimpleJavaFileObject {
        final String code;

        Source(String string, String string2) {
            super(URI.create("string:///" + string.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
            this.code = string2;
        }

        @Override
        public CharSequence getCharContent(boolean bl) {
            return this.code;
        }
    }

    static class Diagnostics implements DiagnosticListener<JavaFileObject> {
        final Script script;

        Diagnostics(Script script) {
            this.script = script;
        }

        @Override
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            String string2 = diagnostic.getMessage(null);
            if (string2.equals("unchecked or unsafe operations")) {
                return;
            }
            if (string2.contains("SpongePowered") || string2.contains("recompile with")) {
                return;
            }
            if (string2.contains("\n")) {
                string2 = string2.split("\n")[0];
            }
            ArrayList<String> arrayList = new ArrayList<String>();
            arrayList.add("&7Error loading script &b" + this.script.name);
            if (string2.contains("ScriptDefaults")) {
                if (string2.startsWith("incompatible types: ")) {
                    StringBuilder sb = new StringBuilder();
                    for (String string3 : string2.split(" ")) {
                        if (string3.contains("ScriptDefaults.")) {
                            int n = string3.lastIndexOf(46);
                            string3 = string3.substring(n + 1);
                        }
                        sb.append(string3).append(' ');
                    }
                    string2 = sb.toString();
                } else {
                    string2 = string2.contains("no suitable constructor found") ? "no suitable constructor found"
                            : (string2.contains("cannot be applied to given types") ? "method cannot be applied to given types"
                            : (string2.contains("bad operand type") ? "bad operand type for unary operator"
                            : (string2.contains("is not public in") || string2.contains("has private access") || string2.contains("has protected access") ? "cannot find symbol"
                            : (string2.contains("is already defined") || string2.contains("incomparable types:")
                            ? string2.replace(ScriptDefaults.class.getName() + ".", "")
                            : "unknown error, report this to dev (err 1)"))));
                }
            } else if (string2.contains("cannot access net.")) {
                string2 = "no suitable constructor found";
            }
            if (string2.contains(this.script.className)) {
                int n = string2.indexOf(" in class");
                if (n != -1) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(string2, 0, n);
                    int n2 = string2.indexOf(" ", n + " in class ".length());
                    if (n2 != -1) {
                        sb.append(string2.substring(n2));
                    }
                    string2 = sb.toString();
                } else if (string2.contains(".java")) {
                    string2 = string2.contains("unsafe") ? "unchecked or unsafe operations" : "unknown error, report this to dev (err 2)";
                }
                if (string2.contains(this.script.className + ".")) {
                    string2 = string2.replace(this.script.className + ".", "");
                }
            }
            arrayList.add(" &7err: &c" + string2);
            JavaFileObject source = diagnostic.getSource();
            if (source != null) {
                try {
                    String text = source.getCharContent(true).toString();
                    int n = (int) diagnostic.getPosition();
                    if (n < 0) {
                        return;
                    }
                    int n3 = text.substring(0, n).split("\\n").length - 20;
                    if (n3 < 0) {
                        return;
                    }
                    arrayList.add(" &7line: &c" + n3);
                    String string4 = text.substring(n).split("\\n")[0].trim();
                    if (!string4.isEmpty()) {
                        arrayList.add(" &7src: &c" + string4);
                    }
                } catch (IOException iOException) {
                }
            }
            this.script.printErrors(arrayList);
            if (this.script.loadErrors == null) {
                this.script.loadErrors = new ArrayList<String>();
            }
            this.script.loadErrors.addAll(arrayList);
        }
    }
}
