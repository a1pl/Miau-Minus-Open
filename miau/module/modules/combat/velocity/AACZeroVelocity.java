package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class AACZeroVelocity extends VelocityMode {
    private boolean hasReceivedVelocity = false;

    public AACZeroVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (player.field_70737_aN > 0) {
                    if (!this.hasReceivedVelocity || player.field_70122_E || player.field_70143_R > 2.0F) {
                        return;
                    }

                    player.field_70181_x--;
                    player.field_70160_al = true;
                    player.field_70122_E = true;
                } else {
                    this.hasReceivedVelocity = false;
                }
            }
        }
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE
            && !event.isCancelled()
            && event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
            if (Velocity.mc.field_71439_g != null
                && packet.func_149412_c() == Velocity.mc.field_71439_g.func_145782_y()) {
                this.hasReceivedVelocity = true;
            }
        }
    }

    @Override
    public void onDisable() {
        this.hasReceivedVelocity = false;
    }
}
