package miau.module.modules.combat.velocity;

import java.util.concurrent.ConcurrentLinkedDeque;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.property.properties.FloatProperty;
import miau.util.network.PacketUtil;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class DelayVelocity extends VelocityMode {
    public final FloatProperty minimumDelay = new FloatProperty("Minimum-delay", 100.0F, 0.0F, 1000.0F);
    public final FloatProperty maximumDelay = new FloatProperty("Maximum-delay", 200.0F, 50.0F, 1000.0F);
    private final ConcurrentLinkedDeque<Packet<?>> delayedPackets = new ConcurrentLinkedDeque<>();
    private long lagStartTime = -1L;
    private long targetDelay = 0L;

    public DelayVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.delayedPackets.clear();
        this.lagStartTime = -1L;
    }

    @Override
    public void onDisable() {
        this.flushDelayedPackets();
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (mc.field_71439_g == null || mc.field_71441_e == null || mc.field_71439_g.field_70128_L) {
                this.flushDelayedPackets();
                return;
            }

            if (this.lagStartTime == -1L) {
                return;
            }

            long nowMs = System.currentTimeMillis();
            if (nowMs - this.lagStartTime >= this.targetDelay) {
                this.flushDelayedPackets();
            }
        }
    }

    @Override
    public void onPacket(PacketEvent e) {
        if (e.getType() == EventType.RECEIVE) {
            if (e.getPacket() instanceof S08PacketPlayerPosLook) {
                this.flushDelayedPackets();
            } else if (this.lagStartTime != -1L) {
                e.setCancelled(true);
                this.delayedPackets.addLast(e.getPacket());
            } else if (e.getPacket() instanceof S12PacketEntityVelocity) {
                if (mc.field_71439_g != null && mc.field_71441_e != null) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity)e.getPacket();
                    if (packet.func_149412_c() == mc.field_71439_g.func_145782_y()) {
                        e.setCancelled(true);
                        this.delayedPackets.addLast(e.getPacket());
                        this.lagStartTime = System.currentTimeMillis();
                        long minD = this.minimumDelay.getValue().longValue();
                        long maxD = this.maximumDelay.getValue().longValue();
                        if (minD > maxD) {
                            minD = maxD;
                        }

                        this.targetDelay = minD + (long)(Math.random() * (maxD - minD + 1L));
                    }
                }
            }
        }
    }

    private void flushDelayedPackets() {
        this.lagStartTime = -1L;
        if (mc.field_71439_g != null && mc.func_147114_u() != null) {
            while (!this.delayedPackets.isEmpty()) {
                Packet<?> packet = this.delayedPackets.pollFirst();
                if (packet != null) {
                    PacketUtil.handlePacket((Packet<INetHandlerPlayClient>)packet);
                }
            }
        }

        this.delayedPackets.clear();
    }
}
