package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorS12PacketEntityVelocity;
import miau.module.modules.combat.Velocity;
import miau.property.properties.PercentProperty;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class StandardVelocity extends VelocityMode {
    public final PercentProperty horizontal = new PercentProperty("horizontal", 0);
    public final PercentProperty vertical = new PercentProperty("vertical", 100);
    public final PercentProperty explosionHorizontal = new PercentProperty("explosions-horizontal", 100);
    public final PercentProperty explosionVertical = new PercentProperty("explosions-vertical", 100);

    public StandardVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (!this.parent.onSwing.getValue() || mc.field_71439_g.field_82175_bq) {
            if (event.getType() == EventType.RECEIVE
                && !event.isCancelled()
                && event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                if (packet.func_149412_c() == mc.field_71439_g.func_145782_y()) {
                    double horizontal = this.horizontal.getValue().intValue();
                    double vertical = this.vertical.getValue().intValue();
                    if (horizontal == 0.0) {
                        IAccessorS12PacketEntityVelocity accessor = (IAccessorS12PacketEntityVelocity)packet;
                        accessor.setMotionX(0);
                        if (vertical != 0.0) {
                            accessor.setMotionY((int)(packet.func_149410_e() * vertical / 100.0));
                        } else {
                            accessor.setMotionY(0);
                        }

                        accessor.setMotionZ(0);
                        return;
                    }

                    IAccessorS12PacketEntityVelocity accessor = (IAccessorS12PacketEntityVelocity)packet;
                    accessor.setMotionX((int)(packet.func_149411_d() * horizontal / 100.0));
                    accessor.setMotionY((int)(packet.func_149410_e() * vertical / 100.0));
                    accessor.setMotionZ((int)(packet.func_149409_f() * horizontal / 100.0));
                }
            }
        }
    }
}
