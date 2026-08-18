package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;

public class VoidBouncer extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final FloatProperty bounceFactor = new FloatProperty("BounceFactor", 1.0F, 0.0F, 100.0F);
    private boolean bounced = false;

    public VoidBouncer() {
        super("VoidBouncer", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g == null || mc.field_71441_e == null) {
                return;
            }

            if (!mc.field_71439_g.field_70122_E
                && mc.field_71439_g.field_70163_u < -64.0
                && mc.field_71439_g.field_70737_aN != 0
                && !this.bounced) {
                mc.field_71439_g.field_70181_x = mc.field_71439_g.field_70181_x * -this.bounceFactor.getValue();
                this.bounced = true;
            }

            if (mc.field_71439_g.field_70122_E || mc.field_71439_g.field_70163_u >= -64.0) {
                this.bounced = false;
            }
        }
    }
}
