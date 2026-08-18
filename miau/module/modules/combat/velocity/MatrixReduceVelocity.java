package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorS12PacketEntityVelocity;
import miau.module.modules.combat.Velocity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class MatrixReduceVelocity extends VelocityMode {
    public MatrixReduceVelocity(String name, Velocity parent) {
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
                        IAccessorS12PacketEntityVelocity accessor = (IAccessorS12PacketEntityVelocity)packet;
                        accessor.setMotionX((int)(packet.func_149411_d() * 0.33));
                        accessor.setMotionZ((int)(packet.func_149409_f() * 0.33));
                        if (player.field_70122_E) {
                            accessor.setMotionX((int)(packet.func_149411_d() * 0.86));
                            accessor.setMotionZ((int)(packet.func_149409_f() * 0.86));
                        }
                    }
                }
            }
        }
    }
}
