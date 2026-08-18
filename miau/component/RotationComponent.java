package miau.component;

import miau.event.EventTarget;
import miau.event.impl.JumpEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.PlayerUpdateEvent;
import miau.event.impl.StrafeEvent;
import miau.management.RotationState;
import miau.util.player.MoveUtil;
import miau.util.player.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MathHelper;

public final class RotationComponent {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static boolean active = false;
    private static boolean smoothed = false;
    public static float[] rotations;
    public static float[] lastRotations = new float[]{0.0F, 0.0F};
    public static float[] targetRotations;
    public static float[] lastServerRotations;
    private static int correctMovement;

    public static boolean isActive() {
        return active;
    }

    public static boolean isSmoothed() {
        return smoothed;
    }

    public static void setActive(boolean active, int correctMovement) {
        RotationComponent.active = active;
        RotationComponent.correctMovement = correctMovement;
    }

    public static void markSmoothed(float[] newRotations) {
        rotations = newRotations;
        smoothed = true;
        RotationState.applyState(true, newRotations[0], newRotations[1], lastRotations[0], 999);
    }

    public static void correctDisabledRotations() {
        if (rotations != null) {
            float[] current = new float[]{mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A};
            float[] fixed = RotationUtil.applySensitivityPatch(
                current[0], current[1], lastRotations[0], lastRotations[1]
            );
            float[] finalRot = RotationUtil.resetRotation(fixed);
            mc.field_71439_g.field_70177_z = finalRot[0];
            mc.field_71439_g.field_70125_A = finalRot[1];
        }
    }

    public static void reset() {
        active = false;
        smoothed = false;
        rotations = null;
        targetRotations = null;
        correctMovement = 0;
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (active && rotations != null) {
            float yaw = rotations[0];
            float pitch = rotations[1];
            RotationState.applyState(true, yaw, pitch, lastRotations[0], 999);
            mc.field_71439_g.field_70759_as = yaw;
            mc.field_71439_g.field_70761_aq = yaw;
            lastServerRotations = new float[]{yaw, pitch};
            if (correctMovement == 3) {
                float forward = mc.field_71439_g.field_71158_b.field_78900_b;
                float strafe = mc.field_71439_g.field_71158_b.field_78902_a;
                if ((forward != 0.0F || strafe != 0.0F)
                    && Math.abs(
                            yaw % 360.0F
                                - Math.toDegrees(MoveUtil.direction(mc.field_71439_g.field_70177_z, forward, strafe))
                                    % 360.0
                        )
                        > 45.0) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_151444_V.func_151463_i(), false);
                    mc.field_71439_g.func_70031_b(false);
                }
            }

            float currentYaw = mc.field_71439_g.field_70177_z;
            float currentPitch = mc.field_71439_g.field_70125_A;
            if (Math.abs(MathHelper.func_76142_g(yaw - currentYaw)) < 1.0F && Math.abs(pitch - currentPitch) < 1.0F) {
                active = false;
                correctDisabledRotations();
            }

            lastRotations = rotations;
        } else {
            lastRotations = new float[]{mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A};
        }

        if (rotations == null) {
            rotations = new float[]{mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A};
        }

        targetRotations = new float[]{mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A};
        smoothed = false;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (active && correctMovement == 1 && rotations != null) {
            MoveUtil.fixMovement(rotations[0]);
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (active && (correctMovement == 1 || correctMovement == 2) && rotations != null) {
            event.setYaw(rotations[0]);
        }
    }

    @EventTarget
    public void onJump(JumpEvent event) {
        if (active && (correctMovement == 1 || correctMovement == 2 || correctMovement == 3) && rotations != null) {
            event.setYaw(rotations[0]);
        }
    }
}
