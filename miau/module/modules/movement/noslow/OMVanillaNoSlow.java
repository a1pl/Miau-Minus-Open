package miau.module.modules.movement.noslow;

import miau.event.impl.UpdateEvent;
import miau.module.modules.movement.NoSlow;

public class OMVanillaNoSlow extends NoSlowMode {
    public OMVanillaNoSlow(String name, NoSlow parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (this.getParent().isAnyActive()) {
            float multiplier = this.getParent().getMotionMultiplier();
            mc.field_71439_g.field_71158_b.field_78900_b *= multiplier;
            mc.field_71439_g.field_71158_b.field_78902_a *= multiplier;
            if (!this.getParent().canSprint()) {
                mc.field_71439_g.func_70031_b(false);
            }
        }
    }
}
