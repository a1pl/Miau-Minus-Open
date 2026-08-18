package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.PercentProperty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class SimpleVelocity extends VelocityMode {
    public final PercentProperty horizontal = new PercentProperty("horizontal", 0);
    public final PercentProperty vertical = new PercentProperty("vertical", 0);
    public final BooleanProperty limitMaxMotion = new BooleanProperty("limit-max-motion", false);
    public final FloatProperty maxXZMotion = new FloatProperty(
        "max-xz-motion", 0.4F, 0.0F, 1.9F, () -> this.limitMaxMotion.getValue()
    );
    public final FloatProperty maxYMotion = new FloatProperty(
        "max-y-motion", 0.36F, 0.0F, 0.46F, () -> this.limitMaxMotion.getValue()
    );

    public SimpleVelocity(String name, Velocity parent) {
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
                        event.setCancelled(true);
                        double h = this.horizontal.getValue().intValue() / 100.0;
                        double v = this.vertical.getValue().intValue() / 100.0;
                        double mX = packet.func_149411_d() / 8000.0 * h;
                        double mZ = packet.func_149409_f() / 8000.0 * h;
                        double mY = packet.func_149410_e() / 8000.0 * v;
                        if (this.limitMaxMotion.getValue()) {
                            double distXZ = Math.sqrt(mX * mX + mZ * mZ);
                            if (distXZ > this.maxXZMotion.getValue().floatValue()) {
                                double ratio = this.maxXZMotion.getValue().floatValue() / distXZ;
                                mX *= ratio;
                                mZ *= ratio;
                            }

                            mY = Math.min(mY, this.maxYMotion.getValue().floatValue() + 7.5E-4);
                        }

                        player.field_70159_w = mX;
                        player.field_70181_x = mY;
                        player.field_70179_y = mZ;
                    }
                }
            }
        }
    }
}
