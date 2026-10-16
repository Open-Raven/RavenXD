package openraven.ravenxd.util.rots.compat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class MethodEventShim {
    private final Map<Class<?>, List<Consumer<Object>>> listeners = new HashMap<Class<?>, List<Consumer<Object>>>();

    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> type, Consumer<T> listener) {
        this.listeners.computeIfAbsent(type, k -> new ArrayList<Consumer<Object>>()).add((Consumer<Object>) listener);
    }

    public void post(Object event) {
        List<Consumer<Object>> list = this.listeners.get(event.getClass());
        if (list != null) {
            for (Consumer<Object> l : list) l.accept(event);
        }
    }
}
