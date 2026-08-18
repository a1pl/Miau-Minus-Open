package miau.module.modules.misc;

import java.lang.reflect.Constructor;
import miau.module.Module;
import miau.util.client.RawMouseHelper;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;
import net.java.games.input.Controller.Type;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;

public class MouseRawInput extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static Mouse mouse;
    private static volatile int deltaX;
    private static volatile int deltaY;
    private static volatile boolean running;
    private static Thread inputThread;
    private MouseHelper previousMouseHelper;

    public MouseRawInput() {
        super("MouseRawInput", false);
    }

    @Override
    public void onEnabled() {
        this.previousMouseHelper = mc.field_71417_B;
        mc.field_71417_B = new RawMouseHelper();
        startInputThread();
    }

    @Override
    public void onDisabled() {
        running = false;
        mouse = null;
        deltaX = 0;
        deltaY = 0;
        if (this.previousMouseHelper != null) {
            mc.field_71417_B = this.previousMouseHelper;
            this.previousMouseHelper = null;
        } else {
            mc.field_71417_B = new MouseHelper();
        }
    }

    public static int consumeDeltaX() {
        int value = deltaX;
        deltaX = 0;
        return value;
    }

    public static int consumeDeltaY() {
        int value = deltaY;
        deltaY = 0;
        return value;
    }

    private static void startInputThread() {
        if (inputThread != null && inputThread.isAlive()) {
            running = true;
        } else {
            running = true;
            inputThread = new Thread(() -> {
                while (running) {
                    try {
                        if (mouse == null) {
                            mouse = findMouse();
                        } else if (mouse.poll()) {
                            deltaX = deltaX + (int)mouse.getX().getPollData();
                            deltaY = deltaY + (int)mouse.getY().getPollData();
                        } else {
                            mouse = null;
                        }

                        Thread.sleep(1L);
                    } catch (Throwable ignored) {
                        mouse = null;

                        try {
                            Thread.sleep(250L);
                        } catch (InterruptedException ignoredInterrupted) {
                            Thread.currentThread().interrupt();
                            running = false;
                        }
                    }
                }
            }, "MouseRawInput");
            inputThread.setDaemon(true);
            inputThread.start();
        }
    }

    private static ControllerEnvironment createDefaultEnvironment() throws ReflectiveOperationException {
        Constructor<ControllerEnvironment> constructor = (Constructor<ControllerEnvironment>)Class.forName(
                "net.java.games.input.DefaultControllerEnvironment"
            )
            .getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static Mouse findMouse() throws ReflectiveOperationException {
        Controller[] controllers = createDefaultEnvironment().getControllers();

        for (Controller controller : controllers) {
            if (controller.getType() == Type.MOUSE && controller instanceof Mouse) {
                controller.poll();
                Mouse candidate = (Mouse)controller;
                if (candidate.getX().getPollData() != 0.0F || candidate.getY().getPollData() != 0.0F) {
                    return candidate;
                }
            }
        }

        return null;
    }
}
