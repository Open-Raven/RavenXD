package xyz.daikirai.script;

import xyz.daikirai.module.Module;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ScriptInvoker {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private ScriptInvoker() {
    }

    public static int dispatchBoolean(ScriptEvent event, Object... objectArray) {
        boolean handled = false;
        boolean cancelled = false;
        for (Map.Entry<Script, Module> entry : ScriptManager.scripts.entrySet()) {
            Script script;
            int n3;
            if (!entry.getValue().isEnabled() || (n3 = callBoolean(script = entry.getKey(), event.getMethodName(script), objectArray)) == -1) continue;
            handled = true;
            if (n3 != 0 || cancelled) continue;
            cancelled = true;
        }
        return handled ? (cancelled ? 0 : 1) : -1;
    }

    public static void dispatch(ScriptEvent event, Object... objectArray) {
        for (Map.Entry<Script, Module> entry : ScriptManager.scripts.entrySet()) {
            if (!entry.getValue().isEnabled()) continue;
            Script script = entry.getKey();
            call(script, event.getMethodName(script), objectArray);
        }
    }

    public static Float dispatchFloat(ScriptEvent event, Object... objectArray) {
        for (Map.Entry<Script, Module> entry : ScriptManager.scripts.entrySet()) {
            Script script;
            Float f;
            if (!entry.getValue().isEnabled() || (f = callFloat(script = entry.getKey(), event.getMethodName(script), objectArray)) == null) continue;
            return f;
        }
        return null;
    }

    public static float[] dispatchFloatArray(ScriptEvent event, Object... objectArray) {
        for (Map.Entry<Script, Module> entry : ScriptManager.scripts.entrySet()) {
            Script script;
            float[] fArray;
            if (!entry.getValue().isEnabled() || (fArray = callFloatArray(script = entry.getKey(), event.getMethodName(script), objectArray)) == null) continue;
            return fArray;
        }
        return null;
    }

    public static boolean call(Script script, String string, Object... objectArray) {
        if (script.scriptClass == null || script.instance == null) {
            return false;
        }
        Method method = findMethod(script.scriptClass, string, Void.TYPE, objectArray);
        if (method != null) {
            try {
                method.setAccessible(true);
                method.invoke(script.instance, objectArray);
                return true;
            } catch (IllegalAccessException | InvocationTargetException reflectiveOperationException) {
                script.reportError(reflectiveOperationException, true);
            }
        }
        return false;
    }

    public static Object callStatic(Class<?> clazz, String string, Class<?> clazz2, Object... objectArray) {
        try {
            Method method = findMethod(clazz, string, clazz2, objectArray);
            if (method != null) {
                return method.invoke(null, objectArray);
            }
        } catch (IllegalAccessException | InvocationTargetException reflectiveOperationException) {
        }
        return null;
    }

    public static Method findMethod(Class<?> clazz, String string, Class<?> clazz2, Object... objectArray) {
        if (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.getName().equals(string) || method.getParameterCount() != objectArray.length || method.getReturnType() != clazz2) continue;
                return method;
            }
        }
        return null;
    }

    public static int callBoolean(Script script, String string, Object... objectArray) {
        if (script.scriptClass == null || script.instance == null) {
            return -1;
        }
        Method method = findMethod(script.scriptClass, string, Boolean.TYPE, objectArray);
        if (method != null) {
            try {
                method.setAccessible(true);
                Object object = method.invoke(script.instance, objectArray);
                if (object instanceof Boolean) {
                    return (Boolean) object ? 1 : 0;
                }
            } catch (IllegalAccessException | InvocationTargetException reflectiveOperationException) {
                script.reportError(reflectiveOperationException, true);
            }
        }
        return -1;
    }

    public static float[] callFloatArray(Script script, String string, Object... objectArray) {
        if (script.scriptClass == null || script.instance == null) {
            return null;
        }
        Method method = findMethod(script.scriptClass, string, float[].class, objectArray);
        if (method != null) {
            try {
                method.setAccessible(true);
                Object object = method.invoke(script.instance, objectArray);
                if (object instanceof float[]) {
                    return (float[]) object;
                }
            } catch (IllegalAccessException | InvocationTargetException reflectiveOperationException) {
                script.reportError(reflectiveOperationException, true);
            }
        }
        return null;
    }

    public static Float callFloat(Script script, String string, Object... objectArray) {
        if (script.scriptClass == null || script.instance == null) {
            return null;
        }
        AccessibleObject accessibleObject = null;
        for (Method method : script.scriptClass.getDeclaredMethods()) {
            if (method.getParameterCount() != objectArray.length || !method.getReturnType().equals(Float.TYPE) && !method.getReturnType().equals(Float.class) || !method.getName().equals(string)) continue;
            accessibleObject = method;
            break;
        }
        if (accessibleObject != null) {
            try {
                accessibleObject.setAccessible(true);
                Object object = ((Method) accessibleObject).invoke(script.instance, objectArray);
                if (object instanceof Float) {
                    return (Float) object;
                }
                if (object instanceof Number) {
                    return ((Number) object).floatValue();
                }
            } catch (IllegalAccessException | InvocationTargetException reflectiveOperationException) {
                script.reportError(reflectiveOperationException, true);
            }
        }
        return null;
    }

    public static String currentScriptName() {
        String string;
        String string2 = currentScriptClass();
        if (string2 != null && (string = ScriptManager.classNames.get(string2)) != null) {
            return string;
        }
        return "";
    }

    public static String currentScriptClass() {
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            String string = stackTraceElement.getFileName();
            if (string == null || !string.endsWith(".java")) continue;
            String string2 = string.substring(0, string.length() - 5);
            if (string2.length() != 200) continue;
            return string2;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <FI> FI bind(Class<?> clazz, String string, MethodType methodType, Class<FI> clazz2, String string2, MethodType methodType2) {
        try {
            MethodHandle methodHandle = LOOKUP.findStatic(clazz, string, methodType);
            CallSite callSite = LambdaMetafactory.metafactory(LOOKUP, string2, MethodType.methodType(clazz2), methodType2, methodHandle, methodType);
            return (FI) callSite.getTarget().invoke();
        } catch (Throwable throwable) {
            return null;
        }
    }

    public static Runnable runnable(Class<?> clazz, String string) {
        return bind(clazz, string, MethodType.methodType(Void.TYPE), Runnable.class, "run", MethodType.methodType(Void.TYPE));
    }

    @SuppressWarnings("unchecked")
    public static <A> Consumer<A> consumer(Class<?> clazz, String string, Class<A> clazz2) {
        return bind(clazz, string, MethodType.methodType(Void.TYPE, clazz2), Consumer.class, "accept", MethodType.methodType(Void.TYPE, Object.class));
    }

    @SuppressWarnings("unchecked")
    public static <A, B> BiConsumer<A, B> biConsumer(Class<?> clazz, String string, Class<A> clazz2, Class<B> clazz3) {
        return bind(clazz, string, MethodType.methodType(Void.TYPE, clazz2, clazz3), BiConsumer.class, "accept", MethodType.methodType(Void.TYPE, Object.class, Object.class));
    }

    @SuppressWarnings("unchecked")
    public static <A> Predicate<A> predicate(Class<?> clazz, String string, Class<A> clazz2) {
        return bind(clazz, string, MethodType.methodType(Boolean.TYPE, clazz2), Predicate.class, "test", MethodType.methodType(Boolean.TYPE, Object.class));
    }

    @SuppressWarnings("unchecked")
    public static <A, R> Function<A, R> function(Class<?> clazz, String string, Class<R> clazz2, Class<A> clazz3) {
        return bind(clazz, string, MethodType.methodType(clazz2, clazz3), Function.class, "apply", MethodType.methodType(Object.class, Object.class));
    }
}
