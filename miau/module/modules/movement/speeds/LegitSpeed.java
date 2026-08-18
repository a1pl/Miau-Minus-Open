package miau.module.modules.movement.speeds;

import java.util.Arrays;
import java.util.List;
import miau.event.impl.LivingUpdateEvent;
import miau.module.modules.movement.Speed;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import net.minecraft.client.settings.KeyBinding;

public class LegitSpeed extends SpeedMode {
    public final BooleanProperty legitCancelSneak = new BooleanProperty(
        "cancel-when-sneaking", true, () -> this.parent.mode.getValue() == 1
    );
    private boolean legitJumping;

    public LegitSpeed(String name, Speed parent) {
        super(name, parent);
    }

    @Override
    public List<Property<?>> getProperties() {
        return Arrays.asList(this.legitCancelSneak);
    }

    @Override
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (mc.field_71439_g.field_70122_E && (!this.legitCancelSneak.getValue() || !mc.field_71439_g.func_70093_af())) {
            if (mc.field_71441_e
                .func_72945_a(
                    mc.field_71439_g,
                    mc.field_71439_g
                        .func_174813_aQ()
                        .func_72317_d(mc.field_71439_g.field_70159_w / 3.0, -1.0, mc.field_71439_g.field_70179_y / 3.0)
                )
                .isEmpty()) {
                this.legitJumping = true;
                KeyBinding.func_74510_a(mc.field_71474_y.field_74314_A.func_151463_i(), true);
            } else if (this.legitJumping) {
                this.legitJumping = false;
                KeyBinding.func_74510_a(mc.field_71474_y.field_74314_A.func_151463_i(), false);
            }
        } else if (this.legitJumping) {
            this.legitJumping = false;
            KeyBinding.func_74510_a(mc.field_71474_y.field_74314_A.func_151463_i(), false);
        }
    }

    @Override
    public void onDisable() {
        if (this.legitJumping) {
            this.legitJumping = false;
            KeyBinding.func_74510_a(mc.field_71474_y.field_74314_A.func_151463_i(), false);
        }
    }
}
