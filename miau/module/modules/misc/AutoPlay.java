package miau.module.modules.misc;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.notification.NotificationType;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;

public class AutoPlay extends Module {
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Hypixel", "None"});
    public final FloatProperty autoPlayDelay = new FloatProperty("AutoPlay Delay", 2.5F, 0.0F, 10.0F);
    private String queuedMode = null;
    private final TimerUtil timer = new TimerUtil();

    public AutoPlay() {
        super("AutoPlay", false);
    }

    @Override
    public void onEnabled() {
        this.queuedMode = null;
        super.onEnabled();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            if (this.queuedMode != null && this.timer.hasTimeElapsed((long)(this.autoPlayDelay.getValue() * 1000.0F))) {
                Minecraft.func_71410_x().field_71439_g.func_71165_d(this.queuedMode);
                this.queuedMode = null;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.RECEIVE) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof S02PacketChat) {
                S02PacketChat chat = (S02PacketChat)packet;
                if (chat.func_148916_d() && chat.func_148915_c() == null) {
                    return;
                }

                if (this.mode.getValue() == 0) {
                    String m = chat.func_148915_c().toString();
                    if (m.contains("ClickEvent{action=RUN_COMMAND, value='/play ")) {
                        try {
                            String command = m.split("action=RUN_COMMAND, value='")[1].split("'\\}")[0];
                            this.sendToGame(command);
                        } catch (Exception var6) {
                        }
                    }
                }
            }
        }
    }

    private void sendToGame(String modeStr) {
        float delay = this.autoPlayDelay.getValue();
        Miau.notificationManager
            .pop(
                "AutoPlay",
                "Sending you to a new game" + (delay > 0.0F ? " in " + delay + "s" : "") + "!",
                (int)(delay * 1000.0F),
                NotificationType.INFO
            );
        this.queuedMode = modeStr;
        this.timer.reset();
    }
}
