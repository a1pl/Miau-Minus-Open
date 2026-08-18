package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.JumpEvent;
import miau.event.impl.StrafeEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorEntity;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

public class Strafe extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Vanilla", "Matrix"});
    public final FloatProperty strength = new FloatProperty(
        "Strength", 0.5F, 0.0F, 1.0F, () -> this.mode.getModeString().equals("Vanilla")
    );
    public final BooleanProperty noMoveStop = new BooleanProperty(
        "NoMoveStop", false, () -> this.mode.getModeString().equals("Vanilla")
    );
    public final BooleanProperty onGroundStrafe = new BooleanProperty(
        "OnGroundStrafe", false, () -> this.mode.getModeString().equals("Vanilla")
    );
    public final BooleanProperty allDirectionsJump = new BooleanProperty("AllDirectionsJump", false);
    private boolean wasDown = false;
    private boolean jump = false;

    public Strafe() {
        super("Strafe", false);
    }

    private double getDirection() {
        float yaw = mc.field_71439_g.field_70177_z;
        float forward = 1.0F;
        if (mc.field_71439_g.field_71158_b.field_78900_b < 0.0F) {
            yaw += 180.0F;
            forward = -0.5F;
        } else if (mc.field_71439_g.field_71158_b.field_78900_b > 0.0F) {
            forward = 0.5F;
        }

        if (mc.field_71439_g.field_71158_b.field_78902_a < 0.0F) {
            yaw += 90.0F * forward;
        } else if (mc.field_71439_g.field_71158_b.field_78902_a > 0.0F) {
            yaw -= 90.0F * forward;
        }

        return Math.toRadians(yaw);
    }

    @EventTarget
    public void onJump(JumpEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null) {
            if (this.jump) {
                mc.field_71439_g.field_70181_x = 0.0;
            }
        }
    }

    @Override
    public void onEnabled() {
        this.wasDown = false;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null && mc.field_71441_e != null) {
                if (mc.field_71439_g.field_70122_E
                    && mc.field_71474_y.field_74314_A.func_151470_d()
                    && this.allDirectionsJump.getValue()
                    && MoveUtil.isMoving()
                    && !mc.field_71439_g.func_70090_H()
                    && !mc.field_71439_g.func_180799_ab()
                    && !mc.field_71439_g.func_70617_f_()
                    && !((IAccessorEntity)mc.field_71439_g).getIsInWeb()) {
                    if (mc.field_71474_y.field_74314_A.func_151470_d()) {
                        KeyBinding.func_74510_a(mc.field_71474_y.field_74314_A.func_151463_i(), false);
                        this.wasDown = true;
                    }

                    float yaw = mc.field_71439_g.field_70177_z;
                    mc.field_71439_g.field_70177_z = (float)Math.toDegrees(this.getDirection());
                    if (!mc.field_71474_y.field_74314_A.func_151470_d()) {
                        mc.field_71439_g.func_70664_aZ();
                    }

                    mc.field_71439_g.field_70177_z = yaw;
                    this.jump = true;
                    if (this.wasDown) {
                        KeyBinding.func_74510_a(mc.field_71474_y.field_74314_A.func_151463_i(), true);
                        this.wasDown = false;
                    }
                } else {
                    this.jump = false;
                }
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            if (this.mode.getModeString().equals("Vanilla")) {
                this.handleLiquidBounceStrafe();
            } else {
                this.handleMatrixStrafe();
            }
        }
    }

    private void handleLiquidBounceStrafe() {
        if (!MoveUtil.isMoving()) {
            if (this.noMoveStop.getValue()) {
                mc.field_71439_g.field_70159_w = 0.0;
                mc.field_71439_g.field_70179_y = 0.0;
            }
        } else {
            double shotSpeed = MoveUtil.getSpeed();
            double speed = shotSpeed * this.strength.getValue().floatValue();
            double motionX = mc.field_71439_g.field_70159_w * (1.0F - this.strength.getValue());
            double motionZ = mc.field_71439_g.field_70179_y * (1.0F - this.strength.getValue());
            if (!mc.field_71439_g.field_70122_E || this.onGroundStrafe.getValue()) {
                double yaw = this.getDirection();
                mc.field_71439_g.field_70159_w = -Math.sin(yaw) * speed + motionX;
                mc.field_71439_g.field_70179_y = Math.cos(yaw) * speed + motionZ;
            }
        }
    }

    private void handleMatrixStrafe() {
        if (MoveUtil.isMoving()) {
            double currentSpeed = MoveUtil.getSpeed();
            if (!(currentSpeed <= 0.0)) {
                float yaw = mc.field_71439_g.field_70177_z;
                if (mc.field_71439_g.field_70701_bs < 0.0F) {
                    yaw += 180.0F;
                } else {
                    float forwardMultiplier;
                    if (mc.field_71439_g.field_70701_bs < 0.0F) {
                        forwardMultiplier = -0.5F;
                    } else if (mc.field_71439_g.field_70701_bs > 0.0F) {
                        forwardMultiplier = 0.5F;
                    } else {
                        forwardMultiplier = 1.0F;
                    }

                    if (mc.field_71439_g.field_70702_br > 0.0F) {
                        yaw -= 90.0F * forwardMultiplier;
                    }

                    if (mc.field_71439_g.field_70702_br < 0.0F) {
                        yaw += 90.0F * forwardMultiplier;
                    }
                }

                double direction = Math.toRadians(yaw);
                mc.field_71439_g.field_70159_w = -Math.sin(direction) * currentSpeed;
                mc.field_71439_g.field_70179_y = Math.cos(direction) * currentSpeed;
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
