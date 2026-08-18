package me.ksyz.accountmanager.utils;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.ASMEventHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class Nan0EventRegister {
    public static void register(EventBus bus, Object target) {
        ConcurrentHashMap<Object, ArrayList<IEventListener>> listeners = (ConcurrentHashMap<Object, ArrayList<IEventListener>>)ReflectionHelper.getPrivateValue(
            EventBus.class, bus, new String[]{"listeners"}
        );
        Map<Object, ModContainer> listenerOwners = (Map<Object, ModContainer>)ReflectionHelper.getPrivateValue(
            EventBus.class, bus, new String[]{"listenerOwners"}
        );
        if (!listeners.containsKey(target)) {
            ModContainer activeModContainer = Loader.instance().getMinecraftModContainer();
            listenerOwners.put(target, activeModContainer);
            ReflectionHelper.setPrivateValue(EventBus.class, bus, listenerOwners, new String[]{"listenerOwners"});
            Set<? extends Class<?>> supers = TypeToken.of(target.getClass()).getTypes().rawTypes();

            for (Method method : target.getClass().getMethods()) {
                for (Class<?> cls : supers) {
                    try {
                        Method real = cls.getDeclaredMethod(method.getName(), method.getParameterTypes());
                        if (real.isAnnotationPresent(SubscribeEvent.class)) {
                            Class<?>[] parameterTypes = method.getParameterTypes();
                            Class<?> eventType = parameterTypes[0];
                            register(bus, eventType, target, method, activeModContainer);
                            break;
                        }
                    } catch (NoSuchMethodException var15) {
                    }
                }
            }
        }
    }

    private static void register(EventBus bus, Class<?> eventType, Object target, Method method, ModContainer owner) {
        try {
            int busID = (Integer)ReflectionHelper.getPrivateValue(EventBus.class, bus, new String[]{"busID"});
            ConcurrentHashMap<Object, ArrayList<IEventListener>> listeners = (ConcurrentHashMap<Object, ArrayList<IEventListener>>)ReflectionHelper.getPrivateValue(
                EventBus.class, bus, new String[]{"listeners"}
            );
            Constructor<?> ctr = eventType.getConstructor();
            ctr.setAccessible(true);
            Event event = (Event)ctr.newInstance();
            ASMEventHandler listener = new ASMEventHandler(target, method, owner);
            event.getListenerList().register(busID, listener.getPriority(), listener);
            ArrayList<IEventListener> others = listeners.get(target);
            if (others == null) {
                others = new ArrayList<>();
                listeners.put(target, others);
                ReflectionHelper.setPrivateValue(EventBus.class, bus, listeners, new String[]{"listeners"});
            }

            others.add(listener);
        } catch (Exception var11) {
        }
    }
}
