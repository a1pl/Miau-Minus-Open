package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class GlitchVelocity extends VelocityMode {
    private boolean hasReceivedVelocity = false;

    public GlitchVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                player.field_70145_X = this.hasReceivedVelocity;
                if (player.field_70737_aN == 7) {
                    player.field_70181_x = 0.4;
                }

                this.hasReceivedVelocity = false;
            }
        }
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (player.field_70122_E) {
                    if (event.getPacket() instanceof S12PacketEntityVelocity) {
                        S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                        if (packet.func_149412_c() == player.func_145782_y()) {
                            this.hasReceivedVelocity = true;
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.hasReceivedVelocity = false;
        if (Velocity.mc.field_71439_g != null) {
            Velocity.mc.field_71439_g.field_70145_X = false;
        }
    }
}
