package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.LivingUpdateEvent;
import miau.mixin.IAccessorMinecraft;
import miau.module.Module;
import miau.property.properties.FloatProperty;
import miau.util.player.MoveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

public class TimerHop extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final FloatProperty minSpeed = new FloatProperty("Min speed", 0.8F, 0.1F, 0.99F);
    public final FloatProperty maxSpeed = new FloatProperty("Max speed", 1.2F, 1.01F, 2.5F);
    private int direction = 0;
    private long phaseStart = 0L;
    private boolean wasOnGround = true;
    private double highestY = 0.0;
    private boolean trackingFall = false;

    public TimerHop() {
        super("TimerHop", false);
    }

    @Override
    public void onEnabled() {
        this.direction = 0;
        this.phaseStart = System.currentTimeMillis();
        this.setTimer(1.0F);
        this.wasOnGround = true;
        this.highestY = 0.0;
        this.trackingFall = false;
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
            boolean onGround = mc.field_71439_g.field_70122_E;
            boolean moving = MoveUtil.isMoving();
            long now = System.currentTimeMillis();
            double elapsed = (now - this.phaseStart) / 1000.0;
            double min = this.minSpeed.getValue().floatValue();
            double max = this.maxSpeed.getValue().floatValue();
            double posY = mc.field_71439_g.field_70163_u;
            if (!onGround && posY > this.highestY) {
                this.highestY = posY;
            }

            if (onGround && !this.wasOnGround) {
                this.trackingFall = false;
                this.highestY = 0.0;
            }

            boolean inMultiBlockFall = false;
            if (!onGround && this.highestY > 0.0 && this.highestY - posY > 1.0) {
                inMultiBlockFall = true;
                if (!this.trackingFall) {
                    this.trackingFall = true;
                }
            }

            boolean justJumped = this.wasOnGround && !onGround && moving;
            float timerSpeed;
            if (justJumped) {
                this.direction = 2;
                this.phaseStart = now;
                this.highestY = posY;
                timerSpeed = (float)max;
            } else if (this.direction == 0) {
                if (onGround && moving) {
                    this.direction = 1;
                    this.phaseStart = now;
                    timerSpeed = (float)min;
                } else {
                    timerSpeed = 1.0F;
                }
            } else if (this.direction == 1) {
                if (!onGround) {
                    this.direction = 2;
                    this.phaseStart = now;
                    this.highestY = posY;
                    timerSpeed = (float)max;
                } else if (!moving) {
                    this.direction = 0;
                    this.phaseStart = now;
                    timerSpeed = 1.0F;
                } else {
                    timerSpeed = this.rampUpCurve(elapsed, min, max);
                    if (timerSpeed >= 1.0F) {
                        this.direction = 0;
                        this.phaseStart = now;
                        timerSpeed = 1.0F;
                    }
                }
            } else if (this.direction == 2) {
                if (onGround) {
                    this.direction = 1;
                    this.phaseStart = now;
                    timerSpeed = (float)min;
                } else if (elapsed >= this.boostDuration(max)) {
                    this.direction = -1;
                    this.phaseStart = now;
                    timerSpeed = 1.0F;
                } else {
                    timerSpeed = (float)max;
                }
            } else if (this.direction == -1) {
                if (onGround) {
                    this.direction = 0;
                    this.phaseStart = now;
                    timerSpeed = 1.0F;
                } else if (inMultiBlockFall) {
                    this.direction = 3;
                    this.phaseStart = now;
                    timerSpeed = (float)min;
                } else {
                    timerSpeed = 1.0F;
                }
            } else if (this.direction == 3) {
                if (onGround) {
                    this.direction = 1;
                    this.phaseStart = now;
                    timerSpeed = (float)(min + 0.05);
                } else if (!inMultiBlockFall) {
                    this.direction = -1;
                    this.phaseStart = now;
                    timerSpeed = 1.0F;
                } else {
                    timerSpeed = this.halfFallCurve(elapsed, min, max);
                    if (timerSpeed >= 1.0F) {
                        this.direction = -1;
                        this.phaseStart = now;
                        timerSpeed = 1.0F;
                    }
                }
            } else {
                timerSpeed = 1.0F;
            }

            this.setTimer(timerSpeed);
            this.wasOnGround = onGround;
        } else {
            this.setTimer(1.0F);
        }
    }

    private float rampUpCurve(double elapsed, double min, double max) {
        double sharp = 2.0;
        double boostExcess = max - 1.0;
        double rampDuration = 0.8 + boostExcess * 3.0 * Math.max(1, this.countNearby());
        double t = Math.min(1.0, elapsed / rampDuration);
        double progress = Math.pow(t, sharp);
        float val = (float)(min + (1.0 - min) * progress);
        return Math.min(val, 1.0F);
    }

    private float halfFallCurve(double elapsed, double min, double max) {
        double halfFallDuration = 0.3 + (max - 1.0) * 0.5 * Math.max(1, this.countNearby());
        double t = Math.min(1.0, elapsed / halfFallDuration);
        double progress = Math.pow(t, 1.5);
        float val = (float)(min + (1.0 - min) * progress);
        return Math.min(val, 1.0F);
    }

    private double boostDuration(double max) {
        double base = 0.08;
        int players = Math.max(1, this.countNearby());
        return base + (players - 1) * 0.02;
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
