package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.LivingUpdateEvent;
import miau.mixin.IAccessorMinecraft;
import miau.module.Module;
import miau.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

public class TimerBalance extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final FloatProperty minSpd = new FloatProperty("Min speed", 0.8F, 0.1F, 0.99F);
    public final FloatProperty maxSpd = new FloatProperty("Max speed", 1.2F, 1.01F, 2.5F);
    private int direction = 0;
    private long phaseStart = 0L;

    public TimerBalance() {
        super("TimerBalance", false);
    }

    @Override
    public void onEnabled() {
        this.direction = 0;
        this.phaseStart = System.currentTimeMillis();
        this.setTimer(1.0F);
    }

    @Override
    public void onDisabled() {
        this.setTimer(1.0F);
    }

    private void setTimer(float speed) {
        ((IAccessorMinecraft)mc).getTimer().field_74278_d = speed;
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            long now = System.currentTimeMillis();
            double elapsed = (now - this.phaseStart) / 1000.0;
            float timerSpeed;
            if (this.direction == 0) {
                this.direction = 1;
                this.phaseStart = now;
                timerSpeed = this.minSpd.getValue();
            } else if (this.direction == 1) {
                timerSpeed = this.rampUpCurve(elapsed);
                if (timerSpeed >= 0.99F) {
                    this.direction = 2;
                    this.phaseStart = now;
                    timerSpeed = this.maxSpd.getValue();
                }
            } else if (this.direction == 2) {
                timerSpeed = this.maxSpd.getValue();
                if (elapsed >= this.boostDuration()) {
                    this.direction = -1;
                    this.phaseStart = now;
                    timerSpeed = this.maxSpd.getValue();
                }
            } else if (this.direction == -1) {
                timerSpeed = this.easeDownCurve(elapsed);
                if (timerSpeed <= 1.01F) {
                    this.direction = 0;
                    this.phaseStart = now;
                    timerSpeed = 1.0F;
                }
            } else {
                timerSpeed = 1.0F;
            }

            this.setTimer(timerSpeed);
        } else {
            this.setTimer(1.0F);
        }
    }

    private float rampUpCurve(double elapsed) {
        double min = this.minSpd.getValue().floatValue();
        double max = this.maxSpd.getValue().floatValue();
        double sharp = 2.0;
        double boostExcess = max - 1.0;
        double rampDuration = 1.0 + boostExcess * 4.0 * Math.max(1, this.countNearby());
        double t = Math.min(1.0, elapsed / rampDuration);
        double progress = Math.pow(t, sharp);
        return (float)(min + (1.0 - min) * progress);
    }

    private float easeDownCurve(double elapsed) {
        double max = this.maxSpd.getValue().floatValue();
        double sharp = 2.0;
        double boostExcess = max - 1.0;
        double easeDuration = 0.5 + boostExcess * 2.0 * Math.max(1, this.countNearby());
        double t = Math.min(1.0, elapsed / easeDuration);
        double progress = 1.0 - Math.pow(1.0 - t, sharp);
        return (float)(max - (max - 1.0) * progress);
    }

    private double boostDuration() {
        double base = 0.15;
        int players = Math.max(1, this.countNearby());
        return base + (players - 1) * 0.05;
    }

    private int countNearby() {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            int count = 0;

            for (Object o : mc.field_71441_e.field_73010_i) {
                Entity p = (Entity)o;
                if (p != mc.field_71439_g
                    && !p.field_70128_L
                    && !p.func_82150_aj()
                    && mc.field_71439_g.func_70032_d(p) <= 12.0F) {
                    count++;
                }
            }

            return count;
        } else {
            return 0;
        }
    }
}
