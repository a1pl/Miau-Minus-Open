package miau.module.modules.movement.speeds;

import java.util.Collections;
import java.util.List;
import miau.event.impl.MoveInputEvent;
import miau.module.modules.movement.Speed;
import miau.property.Property;
import miau.util.player.MoveUtil;

public class PolarSpeed extends SpeedMode {
    private int offGroundTicks = 0;

    public PolarSpeed(String name, Speed parent) {
        super(name, parent);
    }

    @Override
    public List<Property<?>> getProperties() {
        return Collections.emptyList();
    }

    @Override
    public void onEnable() {
        this.offGroundTicks = 0;
    }

    @Override
    public void onDisable() {
        this.offGroundTicks = 0;
    }

    @Override
    public void onMoveInput(MoveInputEvent event) {
        if (!MoveUtil.isMoving()) {
            this.offGroundTicks = 0;
        } else {
            if (mc.field_71439_g.field_70122_E) {
                this.offGroundTicks = 0;
                mc.field_71439_g.field_71158_b.field_78901_c = true;
            } else {
                this.offGroundTicks++;
                mc.field_71439_g.field_71158_b.field_78901_c = false;
                float multiplier = this.offGroundTicks == 1 ? 1.0020001F : 1.0030001F;
                mc.field_71439_g.field_70159_w *= multiplier;
                mc.field_71439_g.field_70179_y *= multiplier;
                if (this.offGroundTicks == 5) {
                    mc.field_71439_g.field_70181_x -= 0.008F;
                }
            }
        }
    }
}
