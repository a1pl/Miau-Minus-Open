package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;

public class IntaveTimerVelocity extends VelocityMode {
    public IntaveTimerVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (!event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (player.field_70737_aN != 0
                    && event.getPacket() instanceof C03PacketPlayer
                    && !(event.getPacket() instanceof C04PacketPlayerPosition)
                    && !(event.getPacket() instanceof C05PacketPlayerLook)
                    && !(event.getPacket() instanceof C06PacketPlayerPosLook)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (player.field_70737_aN >= 8) {
                    VelocityUtil.changeTimer(0.3F);
                } else if (player.field_70737_aN > 2) {
                    VelocityUtil.changeTimer(5.0F);
                } else if (player.field_70737_aN == 2) {
                    VelocityUtil.changeTimer(1.0F);
                } else if (player.field_70737_aN == 0) {
                    VelocityUtil.changeTimer(1.0F);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        VelocityUtil.changeTimer(1.0F);
    }
}
