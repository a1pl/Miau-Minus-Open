package miau.module.modules.misc;

import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.module.Module;
import miau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.play.server.S40PacketDisconnect;

public class AutoReconnect extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final IntProperty delay = new IntProperty("delay", 5, 1, 60);
    private ServerData lastServer = null;
    private long disconnectTime = 0L;
    private boolean shouldReconnect = false;

    public AutoReconnect() {
        super("AutoReconnect", false);
    }

    @Override
    public void onEnabled() {
        this.shouldReconnect = false;
        this.disconnectTime = 0L;
    }

    @Override
    public void onDisabled() {
        this.shouldReconnect = false;
        this.disconnectTime = 0L;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            if (event.getPacket() instanceof S40PacketDisconnect && mc.func_147104_D() != null) {
                this.lastServer = mc.func_147104_D();
                this.shouldReconnect = true;
                this.disconnectTime = System.currentTimeMillis();
            }
        }
    }

    public void tick() {
        if (this.isEnabled() && this.shouldReconnect) {
            if (mc.field_71462_r instanceof GuiDisconnected) {
                long elapsed = (System.currentTimeMillis() - this.disconnectTime) / 1000L;
                if (elapsed >= this.delay.getValue().intValue() && this.lastServer != null) {
                    mc.func_147108_a(new GuiConnecting(new GuiMultiplayer(new GuiMainMenu()), mc, this.lastServer));
                    this.shouldReconnect = false;
                }
            } else if (!(mc.field_71462_r instanceof GuiConnecting)) {
                this.shouldReconnect = false;
            }
        }
    }

    public long getRemainingTime() {
        if (!this.shouldReconnect) {
            return 0L;
        }

        long elapsed = (System.currentTimeMillis() - this.disconnectTime) / 1000L;
        return Math.max(0L, this.delay.getValue().intValue() - elapsed);
    }
}
