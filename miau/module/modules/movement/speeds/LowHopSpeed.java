package miau.module.modules.movement.speeds;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import miau.event.impl.LivingUpdateEvent;
import miau.module.modules.movement.Speed;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.util.player.MoveUtil;

public class LowHopSpeed extends SpeedMode {
    public final FloatProperty sevenTickSpeed = new FloatProperty(
        "lowhop-speed", 2.0F, 0.8F, 2.5F, () -> this.parent.mode.getValue() == 2
    );
    public final BooleanProperty liquidDisable = new BooleanProperty(
        "disable-in-liquid", true, () -> this.parent.mode.getValue() == 2
    );
    public final BooleanProperty sneakDisable = new BooleanProperty(
        "disable-while-sneaking", true, () -> this.parent.mode.getValue() == 2
    );
    public final BooleanProperty jumpMoving = new BooleanProperty(
        "only-jump-when-moving", true, () -> this.parent.mode.getValue() == 2
    );
    private boolean hopping;

    public LowHopSpeed(String name, Speed parent) {
        super(name, parent);
    }

    @Override
    public List<Property<?>> getProperties() {
        return Arrays.asList(this.sevenTickSpeed, this.liquidDisable, this.sneakDisable, this.jumpMoving);
    }

    private boolean canSevenTick() {
        if (mc.field_71439_g != null && mc.field_71441_e != null && !mc.field_71439_g.field_71075_bZ.field_75100_b) {
            return !this.liquidDisable.getValue()
                    || !mc.field_71439_g.func_70090_H() && !mc.field_71439_g.func_180799_ab()
                ? !this.sneakDisable.getValue() || !mc.field_71439_g.func_70093_af()
                : false;
        } else {
            return false;
        }
    }

    private double randomizeDouble(double min, double max) {
        return min + (max - min) * ThreadLocalRandom.current().nextDouble();
    }

    @Override
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.canSevenTick()) {
            if (mc.field_71439_g.field_70122_E && (!this.jumpMoving.getValue() || MoveUtil.isMoving())) {
                mc.field_71439_g.func_70664_aZ();
                double speed = this.sevenTickSpeed.getValue().floatValue() - 0.52;
                int speedAmplifier = MoveUtil.getSpeedLevel();
                if (speedAmplifier == 1) {
                    speed += 0.02;
                } else if (speedAmplifier == 2) {
                    speed += 0.04;
                } else if (speedAmplifier >= 3) {
                    speed += 0.1;
                }

                if (MoveUtil.isMoving()) {
                    MoveUtil.setSpeed(speed - this.randomizeDouble(1.0E-4, 3.0E-4), MoveUtil.getMoveYaw());
                }

                this.hopping = true;
            }

            if (!mc.field_71439_g.field_70122_E) {
                this.hopping = false;
            }

            mc.field_71439_g.field_71158_b.field_78901_c = false;
        }
    }

    @Override
    public void onDisable() {
        this.hopping = false;
    }
}
