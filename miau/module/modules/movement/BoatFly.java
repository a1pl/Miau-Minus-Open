package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

public class BoatFly extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty modeValue = new ModeProperty("Mode", 0, new String[]{"Motion", "Clip", "Velocity"});
    public final FloatProperty speedValue = new FloatProperty("Speed", 0.3F, 0.0F, 1.0F);

    public BoatFly() {
        super("BoatFly", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (!mc.field_71439_g.func_70115_ae()) {
                return;
            }

            Entity vehicle = mc.field_71439_g.field_70154_o;
            double x = -Math.sin(MoveUtil.getMoveDirection()) * this.speedValue.getValue().floatValue();
            double z = Math.cos(MoveUtil.getMoveDirection()) * this.speedValue.getValue().floatValue();
            String mode = this.modeValue.getModeString().toLowerCase();
            if (mode.equals("motion")) {
                vehicle.field_70159_w = x;
                vehicle.field_70181_x = mc.field_71474_y.field_74314_A.func_151470_d()
                    ? this.speedValue.getValue().doubleValue()
                    : 0.0;
                vehicle.field_70179_y = z;
            } else if (mode.equals("clip")) {
                vehicle.func_70107_b(
                    vehicle.field_70165_t + x,
                    vehicle.field_70163_u
                        + (
                            mc.field_71474_y.field_74314_A.func_151470_d()
                                ? this.speedValue.getValue().doubleValue()
                                : 0.0
                        ),
                    vehicle.field_70161_v + z
                );
            } else if (mode.equals("velocity")) {
                vehicle.func_70024_g(
                    x,
                    mc.field_71474_y.field_74314_A.func_151470_d() ? this.speedValue.getValue().doubleValue() : 0.0,
                    z
                );
            }
        }
    }
}
