package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;

public class UniversoCraftOldVelocity extends VelocityMode {
    private boolean hasReceivedVelocity = false;

    public UniversoCraftOldVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (event.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                    if (packet.func_149412_c() == player.func_145782_y()) {
                        this.hasReceivedVelocity = true;
                    }
                }

                if (this.hasReceivedVelocity) {
                    if (event.getPacket() instanceof S12PacketEntityVelocity
                        || event.getPacket() instanceof S27PacketExplosion) {
                        event.setCancelled(true);
                        player.field_70181_x = player.field_70181_x + Math.random() / 100.0;
                    }

                    if (player.field_70737_aN == 0) {
                        this.hasReceivedVelocity = false;
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.hasReceivedVelocity = false;
    }
}
