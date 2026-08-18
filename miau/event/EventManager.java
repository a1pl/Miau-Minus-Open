package miau.event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import miau.event.types.Priority;
import miau.module.Module;

public final class EventManager {
    private static final Logger LOGGER = Logger.getLogger(EventManager.class.getName());
    private static final HashMap<Class<? extends Event>, List<EventManager.MethodData>> REGISTRY_MAP = new HashMap<>();
    private static final HashMap<Class<? extends Event>, EventManager.MethodData[]> CACHED_HANDLERS = new HashMap<>();

    private EventManager() {
    }

    public static void register(Object object) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            if (!isMethodBad(method)) {
                register(method, object);
            }
        }
    }

    public static void register(Object object, Class<? extends Event> eventClass) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            if (!isMethodBad(method, eventClass)) {
                register(method, object);
            }
        }
    }

    public static void unregister(Object object) {
        if (object != null) {
            for (Entry<Class<? extends Event>, List<EventManager.MethodData>> entry : REGISTRY_MAP.entrySet()) {
                if (entry.getValue().removeIf(data -> data.getSource().equals(object))) {
                    CACHED_HANDLERS.put(entry.getKey(), entry.getValue().toArray(new EventManager.MethodData[0]));
                }
            }

            cleanMap(true);
        }
    }

    public static void unregister(Object object, Class<? extends Event> eventClass) {
        if (object != null && eventClass != null) {
            List<EventManager.MethodData> dataList = REGISTRY_MAP.get(eventClass);
            if (dataList != null) {
                if (dataList.removeIf(data -> data.getSource().equals(object))) {
                    CACHED_HANDLERS.put(eventClass, dataList.toArray(new EventManager.MethodData[0]));
                }

                cleanMap(true);
            }
        }
    }

    private static void register(Method method, Object object) {
        Class<? extends Event> indexClass = (Class<? extends Event>)method.getParameterTypes()[0];
        final EventManager.MethodData data = new EventManager.MethodData(
            object, method, method.getAnnotation(EventTarget.class).value()
        );
        if (REGISTRY_MAP.containsKey(indexClass)) {
            if (!REGISTRY_MAP.get(indexClass).contains(data)) {
                REGISTRY_MAP.get(indexClass).add(data);
                sortListValue(indexClass);
            }
        } else {
            REGISTRY_MAP.put(indexClass, new CopyOnWriteArrayList<EventManager.MethodData>() {
                private static final long serialVersionUID = 666L;

                {
                    this.add(data);
                }
            });
            CACHED_HANDLERS.put(indexClass, new EventManager.MethodData[]{data});
        }
    }

    public static void removeEntry(Class<? extends Event> indexClass) {
        Iterator<Entry<Class<? extends Event>, List<EventManager.MethodData>>> mapIterator = REGISTRY_MAP.entrySet()
            .iterator();

        while (mapIterator.hasNext()) {
            if (mapIterator.next().getKey().equals(indexClass)) {
                mapIterator.remove();
                CACHED_HANDLERS.remove(indexClass);
                break;
            }
        }
    }

    public static void cleanMap(boolean onlyEmptyEntries) {
        Iterator<Entry<Class<? extends Event>, List<EventManager.MethodData>>> mapIterator = REGISTRY_MAP.entrySet()
            .iterator();

        while (mapIterator.hasNext()) {
            Entry<Class<? extends Event>, List<EventManager.MethodData>> entry = mapIterator.next();
            if (!onlyEmptyEntries || entry.getValue().isEmpty()) {
                mapIterator.remove();
                CACHED_HANDLERS.remove(entry.getKey());
            }
        }
    }

    private static void sortListValue(Class<? extends Event> indexClass) {
        List<EventManager.MethodData> sortedList = new CopyOnWriteArrayList<>();

        for (byte priority : Priority.VALUE_ARRAY) {
            for (EventManager.MethodData data : REGISTRY_MAP.get(indexClass)) {
                if (data.getPriority() == priority) {
                    sortedList.add(data);
                }
            }
        }

        REGISTRY_MAP.put(indexClass, sortedList);
        CACHED_HANDLERS.put(indexClass, sortedList.toArray(new EventManager.MethodData[0]));
    }

    private static boolean isMethodBad(Method method) {
        return method.getParameterTypes().length != 1 || !method.isAnnotationPresent(EventTarget.class);
    }

    private static boolean isMethodBad(Method method, Class<? extends Event> eventClass) {
        return isMethodBad(method) || !method.getParameterTypes()[0].equals(eventClass);
    }

    public static Event call(Event event) {
        EventManager.MethodData[] dataList = CACHED_HANDLERS.get(event.getClass());
        if (dataList != null) {
            if (event instanceof EventStoppable) {
                EventStoppable stoppable = (EventStoppable)event;

                for (EventManager.MethodData data : dataList) {
                    invoke(data, event);
                    if (stoppable.isStopped()) {
                        break;
                    }
                }
            } else {
                for (EventManager.MethodData data : dataList) {
                    invoke(data, event);
                }
            }
        }

        return event;
    }

    private static void invoke(EventManager.MethodData data, Event argument) {
        if (!(data.getSource() instanceof Module) || ((Module)data.getSource()).isEnabled()) {
            try {
                data.getTargetHandle().invoke((Object)data.getSource(), (Event)argument);
            } catch (Throwable e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                LOGGER.log(
                    Level.WARNING,
                    String.format(
                        "Event handler failed: event=%s source=%s method=%s",
                        argument.getClass().getName(),
                        data.getSource().getClass().getName(),
                        data.getTarget().getName()
                    ),
                    cause
                );
            }
        }
    }

    private static final class MethodData {
        private final Object source;
        private final Method target;
        private final byte priority;
        private final MethodHandle targetHandle;

        public MethodData(Object source, Method target, byte priority) {
            this.source = source;
            this.target = target;
            this.priority = priority;

            try {
                if (!target.isAccessible()) {
                    target.setAccessible(true);
                }

                this.targetHandle = MethodHandles.lookup().unreflect(target);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        public MethodHandle getTargetHandle() {
            return this.targetHandle;
        }

        public Object getSource() {
            return this.source;
        }

        public Method getTarget() {
            return this.target;
        }

        public byte getPriority() {
            return this.priority;
        }
    }
}
