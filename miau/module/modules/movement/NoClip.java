package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.FloatProperty;
import miau.util.player.MoveUtil;
import net.minecraft.client.Minecraft;

public class NoClip extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final FloatProperty speedValue = new FloatProperty("Speed", 0.5F, 0.0F, 10.0F);

    public NoClip() {
        super("NoClip", false);
    }

    @Override
    public void onDisabled() {
        if (mc.field_71439_g != null) {
            mc.field_71439_g.field_70145_X = false;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g == null || mc.field_71441_e == null) {
                return;
            }

            MoveUtil.strafe(this.speedValue.getValue().doubleValue());
            mc.field_71439_g.field_70145_X = true;
            mc.field_71439_g.field_70122_E = false;
            mc.field_71439_g.field_71075_bZ.field_75100_b = false;
            double ySpeed = 0.0;
            if (mc.field_71474_y.field_74314_A.func_151470_d()) {
                ySpeed += this.speedValue.getValue().floatValue();
            }

            if (mc.field_71474_y.field_74311_E.func_151470_d()) {
                ySpeed -= this.speedValue.getValue().floatValue();
            }

            mc.field_71439_g.field_70181_x = ySpeed;
        }
    }
}
