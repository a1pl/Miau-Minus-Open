package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class MatrixReduce2Velocity extends VelocityMode {
    private boolean hasReceivedVelocity = false;

    public MatrixReduce2Velocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player == null) {
                return;
            }

            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                if (packet.func_149412_c() == player.func_145782_y()) {
                    this.hasReceivedVelocity = true;
                }
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (this.hasReceivedVelocity && player.field_70737_aN >= 9) {
                    if (VelocityUtil.isMoving()
                        && !player.func_70632_aY()
                        && !player.func_70093_af()
                        && !player.func_70113_ah()
                        && player.field_70122_E) {
                        VelocityUtil.reduceXZ(0.0);
                    } else if (!VelocityUtil.isMoving()
                        || player.func_70632_aY()
                        || player.func_70093_af()
                        || player.func_70113_ah()
                        || !player.field_70122_E) {
                        VelocityUtil.reduceXZ(0.2);
                    }

                    this.hasReceivedVelocity = false;
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.hasReceivedVelocity = false;
    }
}
