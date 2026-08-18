package miau.module.modules.combat.velocity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import miau.event.impl.PacketEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.property.properties.IntProperty;
import miau.util.network.PacketUtil;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

public class LiquidBounceDelayVelocity extends VelocityMode {
    public final IntProperty spoofDelay = new IntProperty("spoof-delay", 500, 0, 5000);
    private final Map<Packet<?>, Long> packets = new HashMap<>();
    private boolean delayMode = false;

    public LiquidBounceDelayVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.packets.clear();
        this.delayMode = false;
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            if (event.getPacket() instanceof S32PacketConfirmTransaction
                || event.getPacket() instanceof S12PacketEntityVelocity) {
                event.setCancelled(true);
                synchronized (this.packets) {
                    this.packets.put(event.getPacket(), System.currentTimeMillis());
                }

                this.delayMode = true;
            }
        }
    }

    @Override
    public void onTick(TickEvent event) {
        if (this.delayMode) {
            this.sendPacketsByOrder(false);
        }
    }

    @Override
    public void onDisable() {
        this.sendPacketsByOrder(true);
        this.packets.clear();
        this.delayMode = false;
    }

    private void sendPacketsByOrder(boolean velocity) {
        synchronized (this.packets) {
            Iterator<Entry<Packet<?>, Long>> it = this.packets.entrySet().iterator();

            while (it.hasNext()) {
                Entry<Packet<?>, Long> entry = it.next();
                if (velocity || entry.getValue() <= System.currentTimeMillis() - this.spoofDelay.getValue().intValue()) {
                    PacketUtil.handlePacket((Packet<INetHandlerPlayClient>)entry.getKey());
                    it.remove();
                }
            }
        }
    }
}
