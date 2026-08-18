package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class Intave1412Velocity extends VelocityMode {
    private boolean hasReceivedVelocity = false;

    public Intave1412Velocity(String name, Velocity parent) {
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
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (this.hasReceivedVelocity
                    && player.field_82175_bq
                    && (player.field_70701_bs != 0.0F || player.field_70702_br != 0.0F)
                    && player.field_70122_E
                    && player.func_70051_ag()) {
                    double yawRad = Math.toRadians(player.field_70177_z);
                    player.func_70024_g(-Math.sin(yawRad) * 0.5, 0.1, Math.cos(yawRad) * 0.5);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.hasReceivedVelocity = false;
    }
}
