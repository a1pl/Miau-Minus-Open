package miau.module.modules.movement.noslow;

import java.util.ArrayList;
import java.util.List;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.movement.NoSlow;
import miau.util.network.PacketUtil;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class OMGrimTestNoSlow extends NoSlowMode {
    private final List<Packet<?>> packetBuffer = new ArrayList<>();
    private boolean isHolding = false;
    private boolean fakePacket = false;
    private boolean pendingRelease = false;
    private int ticksElapsed = 0;

    public OMGrimTestNoSlow(String name, NoSlow parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.clearBuffer();
        this.isHolding = false;
        this.fakePacket = false;
        this.pendingRelease = false;
        this.ticksElapsed = 0;
    }

    @Override
    public void onDisable() {
        this.releaseHold();
        this.clearBuffer();
        this.isHolding = false;
        this.fakePacket = false;
        this.pendingRelease = false;
        this.ticksElapsed = 0;
    }

    private void clearBuffer() {
        this.packetBuffer.clear();
    }

    private void releaseHold() {
        if (this.isHolding) {
            this.isHolding = false;
            this.flushBuffer();
        }
    }

    private void flushBuffer() {
        for (Packet<?> p : this.packetBuffer) {
            PacketUtil.sendPacketNoEvent(p);
        }

        this.packetBuffer.clear();
    }

    private void acquireHold() {
        this.isHolding = true;
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            this.handleGrimLiving();
        }
    }

    private void handleGrimLiving() {
        if (this.pendingRelease) {
            this.pendingRelease = false;
            this.fakePacket = true;
            PacketUtil.sendPacketNoEvent(
                new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.field_177992_a, EnumFacing.DOWN)
            );
            this.fakePacket = false;
        }

        if (this.getParent().isAnyActive() && !this.isHolding) {
            this.acquireHold();
            this.pendingRelease = true;
            this.ticksElapsed = 0;
        }

        if (this.isHolding) {
            this.ticksElapsed++;
            int maxTicks = this.getParent().grimTestMaxTicks.getValue();
            if (this.ticksElapsed >= maxTicks) {
                this.releaseHold();
                this.ticksElapsed = 0;
                if (this.getParent().isAnyActive()) {
                    PacketUtil.sendPacketNoEvent(
                        new C08PacketPlayerBlockPlacement(
                            new BlockPos(-1, -1, -1), 255, mc.field_71439_g.func_70694_bm(), 0.0F, 0.0F, 0.0F
                        )
                    );
                }
            }
        }
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND) {
            Packet<?> packet = event.getPacket();
            if (this.isHolding) {
                if (packet instanceof C00PacketKeepAlive || packet instanceof C01PacketChatMessage) {
                    return;
                }

                if (!this.fakePacket && packet instanceof C07PacketPlayerDigging) {
                    C07PacketPlayerDigging digging = (C07PacketPlayerDigging)packet;
                    if (digging.func_180762_c() == Action.RELEASE_USE_ITEM) {
                        event.setCancelled(true);
                        this.releaseHold();
                        return;
                    }
                }

                this.packetBuffer.add(packet);
                event.setCancelled(true);
            }
        }
    }

    public boolean shouldCancelSlowdown() {
        return this.isHolding;
    }
}
