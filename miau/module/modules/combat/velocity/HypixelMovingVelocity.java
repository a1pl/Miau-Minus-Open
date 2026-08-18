package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.module.modules.combat.Velocity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class HypixelMovingVelocity extends VelocityMode {
    public HypixelMovingVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (!event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (VelocityUtil.isMoving()) {
                    if (event.getPacket() instanceof C0FPacketConfirmTransaction
                        || event.getPacket() instanceof S12PacketEntityVelocity) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
