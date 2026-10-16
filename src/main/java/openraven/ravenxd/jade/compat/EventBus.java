package openraven.ravenxd.jade.compat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

// eventbus wrapper
// w deepseek
public final class EventBus {
    private static final List<Object[]> LISTENERS = new ArrayList<Object[]>();

    private EventBus() {
    }

    public static synchronized void register(Object listener) {
        for (Method m : listener.getClass().getDeclaredMethods()) {
            if (m.isAnnotationPresent(Subscribe.class) && m.getParameterTypes().length == 1) {
                m.setAccessible(true);
                LISTENERS.add(new Object[]{listener, m});
            }
        }
    }

    public static <T extends Event> T post(T event) {
        Object[][] listeners;
        synchronized (EventBus.class) {
            listeners = LISTENERS.toArray(new Object[0][]);
        }
        for (Object[] l : listeners) {
            Method m = (Method) l[1];
            if (!m.getParameterTypes()[0].isInstance(event)) continue;
            try {
                m.invoke(l[0], event);
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }
        return event;
    }
}
