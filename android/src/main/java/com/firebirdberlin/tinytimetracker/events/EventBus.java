package com.firebirdberlin.tinytimetracker.events;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class EventBus {
    private static final EventBus INSTANCE = new EventBus();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> stickyEvents = new ConcurrentHashMap<>();

    public static EventBus getDefault() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public <T> void register(Class<T> eventClass, Consumer<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>())
                .add((Consumer<Object>) listener);

        Object sticky = stickyEvents.get(eventClass);
        if (sticky != null) {
            mainHandler.post(() -> listener.accept((T) sticky));
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void unregister(Class<T> eventClass, Consumer<T> listener) {
        List<Consumer<Object>> eventListeners = listeners.get(eventClass);
        if (eventListeners != null) {
            eventListeners.remove(listener);
            if (eventListeners.isEmpty()) {
                listeners.remove(eventClass);
            }
        }
    }

    public void post(Object event) {
        if (event == null) return;
        dispatch(event);
    }

    public void postSticky(Object event) {
        if (event == null) return;
        stickyEvents.put(event.getClass(), event);
        dispatch(event);
    }

    @SuppressWarnings("unchecked")
    public <T> T getStickyEvent(Class<T> eventClass) {
        return (T) stickyEvents.get(eventClass);
    }

    public void removeStickyEvent(Class<?> eventClass) {
        stickyEvents.remove(eventClass);
    }

    private void dispatch(Object event) {
        List<Consumer<Object>> eventListeners = listeners.get(event.getClass());
        if (eventListeners == null || eventListeners.isEmpty()) return;

        Runnable r = () -> {
            List<Consumer<Object>> copy = new ArrayList<>(eventListeners);
            for (Consumer<Object> listener : copy) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            mainHandler.post(r);
        }
    }
}
