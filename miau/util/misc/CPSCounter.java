package miau.util.misc;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;

public final class CPSCounter {
    private static final Map<CPSCounter.MouseButton, Deque<Long>> clicks = new EnumMap<>(CPSCounter.MouseButton.class);

    private CPSCounter() {
    }

    public static void registerClick(CPSCounter.MouseButton button) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = clicks.get(button);
        synchronized (deque) {
            deque.addLast(now);

            while (!deque.isEmpty() && now - deque.peekFirst() > 1000L) {
                deque.pollFirst();
            }
        }
    }

    public static int getCPS(CPSCounter.MouseButton button) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = clicks.get(button);
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() > 1000L) {
                deque.pollFirst();
            }

            return deque.size();
        }
    }

    static {
        for (CPSCounter.MouseButton button : CPSCounter.MouseButton.values()) {
            clicks.put(button, new ArrayDeque<>());
        }
    }

    public enum MouseButton {
        LEFT,
        MIDDLE,
        RIGHT;
    }
}
