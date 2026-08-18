package miau.module.modules.movement.nofalls;

import miau.Miau;
import miau.enums.BlinkModules;
import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorC03PacketPlayer;
import miau.module.modules.movement.NoFall;
import miau.util.client.ChatUtil;
import miau.util.player.PlayerUtil;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;

public class BlinkNoFall extends NoFallMode {
    private boolean lastOnGround = false;

    public BlinkNoFall(String name, NoFall parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C03PacketPlayer) {
            C03PacketPlayer packet = (C03PacketPlayer)event.getPacket();
            boolean allowed = !mc.field_71439_g.func_70617_f_()
                && !mc.field_71439_g.field_71075_bZ.field_75101_c
                && mc.field_71439_g.field_70737_aN == 0;
            if (Miau.blinkManager.getBlinkingModule() != BlinkModules.NO_FALL) {
                if (this.lastOnGround
                    && !packet.func_149465_i()
                    && allowed
                    && PlayerUtil.canFly(this.parent.distance.getValue().intValue())
                    && mc.field_71439_g.field_70181_x < 0.0) {
                    Miau.blinkManager.setBlinkState(false, Miau.blinkManager.getBlinkingModule());
                    Miau.blinkManager.setBlinkState(true, BlinkModules.NO_FALL);
                }
            } else if (!allowed) {
                Miau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
                ChatUtil.display("%s%s: &cFailed player check!&r", this.parent.getName());
            } else if (PlayerUtil.checkInWater(mc.field_71439_g.func_174813_aQ().func_72314_b(2.0, 0.0, 2.0))) {
                Miau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
                ChatUtil.display("%s%s: &cFailed void check!&r", this.parent.getName());
            } else if (packet.func_149465_i()) {
                for (Packet<?> blinkedPacket : Miau.blinkManager.blinkedPackets) {
                    if (blinkedPacket instanceof C03PacketPlayer) {
                        ((IAccessorC03PacketPlayer)blinkedPacket).setOnGround(true);
                    }
                }

                Miau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
                this.parent.packetDelayTimer.reset();
            }

            this.lastOnGround = packet.func_149465_i() && allowed && this.parent.canTrigger();
        }
    }

    @Override
    public void onDisable() {
        this.lastOnGround = false;
        Miau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
    }
}
