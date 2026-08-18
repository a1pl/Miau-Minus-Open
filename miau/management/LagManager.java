package miau.management;

import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.util.network.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.util.Vec3;

public class LagManager {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final Deque<LagManager.LagPacket> packetQueue = new ConcurrentLinkedDeque<>();
    private int tickDelay = 0;
    private long msDelay = 0L;
    private boolean usingMsDelay = false;
    private boolean flushing = false;
    private Vec3 lastPosition = new Vec3(0.0, 0.0, 0.0);
    public Set<Packet<?>> fastTrackSet;

    private void flushQueue() {
        if (mc.func_147114_u() == null) {
            this.packetQueue.clear();
        } else {
            this.flushing = true;

            try {
                while (!this.packetQueue.isEmpty()) {
                    LagManager.LagPacket lp = this.packetQueue.peek();
                    boolean canRelease;
                    if (this.usingMsDelay) {
                        canRelease = this.msDelay <= 0L
                            || System.currentTimeMillis() - lp.enqueueTimeMs >= this.msDelay;
                    } else {
                        canRelease = this.tickDelay <= 0 || lp.delay > this.tickDelay;
                    }

                    if (!canRelease) {
                        break;
                    }

                    this.packetQueue.poll();
                    PacketUtil.sendPacketNoEvent(lp.packet);
                    if (lp.packet instanceof C03PacketPlayer) {
                        C03PacketPlayer c03 = (C03PacketPlayer)lp.packet;
                        if (c03.func_149466_j()) {
                            this.lastPosition = new Vec3(c03.func_149464_c(), c03.func_149467_d(), c03.func_149472_e());
                        }
                    }
                }
            } finally {
                this.flushing = false;
            }
        }
    }

    private void incrementDelays() {
        this.packetQueue.forEach(z -> z.delay++);
    }

    public boolean handlePacket(Packet<?> packet) {
        this.flushQueue();
        if (this.fastTrackSet != null && this.fastTrackSet.remove(packet)) {
            if (packet instanceof C03PacketPlayer) {
                C03PacketPlayer c03 = (C03PacketPlayer)packet;
                if (c03.func_149466_j()) {
                    this.lastPosition = new Vec3(c03.func_149464_c(), c03.func_149467_d(), c03.func_149472_e());
                }
            }

            return false;
        } else if (!(packet instanceof C00PacketKeepAlive) && !(packet instanceof C01PacketChatMessage)) {
            boolean shouldQueue = this.usingMsDelay ? this.msDelay > 0L : this.tickDelay > 0;
            if (shouldQueue) {
                this.packetQueue.offer(new LagManager.LagPacket(packet));
                return true;
            }

            if (packet instanceof C03PacketPlayer) {
                C03PacketPlayer c03 = (C03PacketPlayer)packet;
                if (c03.func_149466_j()) {
                    this.lastPosition = new Vec3(c03.func_149464_c(), c03.func_149467_d(), c03.func_149472_e());
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public void setDelay(int ticks) {
        this.tickDelay = ticks;
        this.usingMsDelay = false;
        this.msDelay = 0L;
    }

    public void setDelayMs(long ms) {
        this.msDelay = ms;
        this.usingMsDelay = true;
        this.tickDelay = 0;
    }

    public void resetDelay() {
        this.tickDelay = 0;
        this.msDelay = 0L;
        this.usingMsDelay = false;
    }

    public Vec3 getLastPosition() {
        return this.lastPosition;
    }

    public boolean isFlushing() {
        return this.flushing;
    }

    public boolean isUsingMsDelay() {
        return this.usingMsDelay;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.POST) {
            if (mc.field_71439_g.field_70128_L) {
                this.resetDelay();
            }

            if (!this.usingMsDelay) {
                this.incrementDelays();
            }

            this.flushQueue();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof C00Handshake
            || event.getPacket() instanceof C00PacketLoginStart
            || event.getPacket() instanceof C00PacketServerQuery
            || event.getPacket() instanceof C01PacketPing
            || event.getPacket() instanceof C01PacketEncryptionResponse) {
            this.resetDelay();
        }
    }

    public static class LagPacket {
        public final Packet<?> packet;
        public int delay;
        public final long enqueueTimeMs;

        public LagPacket(Packet<?> packet) {
            this.packet = packet;
            this.delay = 0;
            this.enqueueTimeMs = System.currentTimeMillis();
        }
    }
}
