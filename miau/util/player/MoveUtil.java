package miau.util.player;

import miau.Miau;
import miau.event.impl.StrafeEvent;
import miau.management.RotationState;
import miau.module.modules.combat.TargetStrafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class MoveUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public static final double HEAD_HITTER_MOTION = -0.0784000015258789;

    public static boolean isForwardPressed() {
        return mc.field_71474_y.field_74351_w.func_151470_d() != mc.field_71474_y.field_74368_y.func_151470_d()
            ? true
            : mc.field_71474_y.field_74370_x.func_151470_d() != mc.field_71474_y.field_74366_z.func_151470_d();
    }

    public static boolean isMoving() {
        return mc.field_71439_g != null
            && (
                mc.field_71439_g.field_71158_b.field_78900_b != 0.0F
                    || mc.field_71439_g.field_71158_b.field_78902_a != 0.0F
            );
    }

    public static boolean isMovingStraight() {
        return isMoving()
            && mc.field_71439_g.field_71158_b.field_78900_b != 0.0F
            && mc.field_71439_g.field_71158_b.field_78902_a == 0.0F;
    }

    public static int getForwardValue() {
        int forwardValue = 0;
        if (mc.field_71474_y.field_74351_w.func_151470_d()) {
            forwardValue++;
        }

        if (mc.field_71474_y.field_74368_y.func_151470_d()) {
            forwardValue--;
        }

        return forwardValue;
    }

    public static int getLeftValue() {
        int leftValue = 0;
        if (mc.field_71474_y.field_74370_x.func_151470_d()) {
            leftValue++;
        }

        if (mc.field_71474_y.field_74366_z.func_151470_d()) {
            leftValue--;
        }

        return leftValue;
    }

    public static float getMoveYaw() {
        return adjustYaw(
            RotationState.isActived() ? RotationState.getSmoothedYaw() : mc.field_71439_g.field_70177_z,
            mc.field_71439_g.field_71158_b.field_78900_b,
            mc.field_71439_g.field_71158_b.field_78902_a
        );
    }

    public static float adjustYaw(float yaw, float forward, float strafe) {
        TargetStrafe targetStrafe = (TargetStrafe)Miau.moduleManager.modules.get(TargetStrafe.class);
        if (targetStrafe.isEnabled() && !Float.isNaN(targetStrafe.getTargetYaw())) {
            return targetStrafe.getTargetYaw();
        }

        if (forward < 0.0F) {
            yaw += 180.0F;
        }

        if (strafe != 0.0F) {
            float multiplier = forward == 0.0F ? 1.0F : 0.5F * Math.signum(forward);
            yaw += -90.0F * multiplier * Math.signum(strafe);
        }

        return MathHelper.func_76142_g(yaw);
    }

    public static float getDirectionYaw() {
        return getSpeed() == 0.0
            ? MathHelper.func_76142_g(mc.field_71439_g.field_70177_z)
            : MathHelper.func_76142_g(
                (float)Math.toDegrees(Math.atan2(mc.field_71439_g.field_70179_y, mc.field_71439_g.field_70159_w))
                    - 90.0F
            );
    }

    public static double getBaseMoveSpeed() {
        double baseSpeed = 0.28015;
        if (getSpeedTime() > 0) {
            baseSpeed = 0.28015 * (1.0 + 0.15 * getSpeedLevel());
        }

        return baseSpeed;
    }

    public static double getBaseJumpHigh(int speedLevel) {
        double jumpHeight = 0.452;
        if (speedLevel == 1) {
            jumpHeight = 0.49720000000000003;
        } else if (speedLevel >= 2) {
            jumpHeight *= 1.2;
        }

        return jumpHeight;
    }

    public static double getJumpMotion() {
        int speedLevel = 0;
        if (getSpeedTime() > 0) {
            speedLevel = getSpeedLevel();
        }

        return getBaseJumpHigh(speedLevel);
    }

    public static double getSpeed() {
        return getSpeed(mc.field_71439_g.field_70159_w, mc.field_71439_g.field_70179_y);
    }

    public static double getSpeed(double motionX, double motionZ) {
        return Math.hypot(motionX, motionZ);
    }

    public static double speed() {
        return getSpeed();
    }

    public static void strafe() {
        strafe(speed());
    }

    public static void strafe(double speed) {
        if (isMoving()) {
            double yaw = getMoveDirection();
            mc.field_71439_g.field_70159_w = -MathHelper.func_76126_a((float)yaw) * speed;
            mc.field_71439_g.field_70179_y = MathHelper.func_76134_b((float)yaw) * speed;
        }
    }

    public static void strafe(double speed, Entity entity) {
        if (isMoving()) {
            double yaw = getMoveDirection();
            entity.field_70159_w = -MathHelper.func_76126_a((float)yaw) * speed;
            entity.field_70179_y = MathHelper.func_76134_b((float)yaw) * speed;
        }
    }

    public static void strafe(double speed, float yaw) {
        if (isMoving()) {
            double rad = Math.toRadians(yaw);
            mc.field_71439_g.field_70159_w = -MathHelper.func_76126_a((float)rad) * speed;
            mc.field_71439_g.field_70179_y = MathHelper.func_76134_b((float)rad) * speed;
        }
    }

    public static void stop() {
        mc.field_71439_g.field_70159_w = 0.0;
        mc.field_71439_g.field_70179_y = 0.0;
    }

    public static double getMoveDirection() {
        float rotationYaw = mc.field_71439_g.field_70177_z;
        if (mc.field_71439_g.field_70701_bs < 0.0F) {
            rotationYaw += 180.0F;
        }

        float forward = 1.0F;
        if (mc.field_71439_g.field_70701_bs < 0.0F) {
            forward = -0.5F;
        } else if (mc.field_71439_g.field_70701_bs > 0.0F) {
            forward = 0.5F;
        }

        if (mc.field_71439_g.field_70702_br > 0.0F) {
            rotationYaw -= 90.0F * forward;
        }

        if (mc.field_71439_g.field_70702_br < 0.0F) {
            rotationYaw += 90.0F * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public static double predictedMotion(double motion, int ticks) {
        if (ticks <= 0) {
            return motion;
        }

        double predicted = motion;

        for (int i = 0; i < ticks; i++) {
            predicted = (predicted - 0.08) * 0.98F;
        }

        return predicted;
    }

    public static double getbaseMoveSpeed() {
        double baseSpeed = 0.2873;
        if (mc.field_71439_g.func_70644_a(Potion.field_76424_c)) {
            baseSpeed *= 1.0 + 0.2 * (mc.field_71439_g.func_70660_b(Potion.field_76424_c).func_76458_c() + 1);
        }

        return baseSpeed;
    }

    public static void preventDiagonalSpeed() {
        KeyBinding[] gameSettings = new KeyBinding[]{
            mc.field_71474_y.field_74351_w,
            mc.field_71474_y.field_74366_z,
            mc.field_71474_y.field_74368_y,
            mc.field_71474_y.field_74370_x
        };
        int down = 0;

        for (KeyBinding kb : gameSettings) {
            if (kb.func_151470_d()) {
                down++;
            }
        }

        if (down != 1) {
            double groundIncrease = 0.0026000750109401644;
            double airIncrease = 5.199896488849598E-4;
            double increase = mc.field_71439_g.field_70122_E ? 0.0026000750109401644 : 5.199896488849598E-4;
            moveFlying(-increase);
        }
    }

    public static void moveFlying(double increase) {
        if (isMoving()) {
            double yaw = getMoveDirection();
            mc.field_71439_g.field_70159_w = mc.field_71439_g.field_70159_w
                + -MathHelper.func_76126_a((float)yaw) * increase;
            mc.field_71439_g.field_70179_y = mc.field_71439_g.field_70179_y
                + MathHelper.func_76134_b((float)yaw) * increase;
        }
    }

    public static void setSpeed(double speed) {
        setSpeed(speed, getDirectionYaw());
    }

    public static void setSpeed(double speed, float yaw) {
        mc.field_71439_g.field_70159_w = -Math.sin(Math.toRadians(yaw)) * speed;
        mc.field_71439_g.field_70179_y = Math.cos(Math.toRadians(yaw)) * speed;
    }

    public static void addSpeed(double speed, float yaw) {
        mc.field_71439_g.field_70159_w = mc.field_71439_g.field_70159_w + -Math.sin(Math.toRadians(yaw)) * speed;
        mc.field_71439_g.field_70179_y = mc.field_71439_g.field_70179_y + Math.cos(Math.toRadians(yaw)) * speed;
    }

    public static int getSpeedLevel() {
        int speedLevel = 0;
        if (mc.field_71439_g.func_70644_a(Potion.field_76424_c)) {
            speedLevel = mc.field_71439_g.func_70660_b(Potion.field_76424_c).func_76458_c() + 1;
        }

        return speedLevel;
    }

    public static int getSpeedTime() {
        return mc.field_71439_g.func_70644_a(Potion.field_76424_c)
            ? mc.field_71439_g.func_70660_b(Potion.field_76424_c).func_76459_b()
            : 0;
    }

    public static float getAllowedHorizontalDistance() {
        float slipperiness = mc.field_71439_g
                .field_70170_p
                .func_180495_p(
                    new BlockPos(
                        MathHelper.func_76128_c(mc.field_71439_g.field_70165_t),
                        MathHelper.func_76128_c(mc.field_71439_g.func_174813_aQ().field_72338_b) - 1,
                        MathHelper.func_76128_c(mc.field_71439_g.field_70161_v)
                    )
                )
                .func_177230_c()
                .field_149765_K
            * 0.91F;
        return mc.field_71439_g.func_70689_ay() * (0.16277136F / (slipperiness * slipperiness * slipperiness));
    }

    public static double[] predictMovement() {
        float strafeInput = getLeftValue() * 0.98F;
        float forwardInput = getForwardValue() * 0.98F;
        float inputMagnitude = strafeInput * strafeInput + forwardInput * forwardInput;
        if (inputMagnitude >= 1.0E-4F) {
            inputMagnitude = MathHelper.func_76129_c(inputMagnitude);
            if (inputMagnitude < 1.0F) {
                inputMagnitude = 1.0F;
            }

            inputMagnitude = getAllowedHorizontalDistance() / inputMagnitude;
            float sinYaw = MathHelper.func_76126_a(mc.field_71439_g.field_70177_z * (float) Math.PI / 180.0F);
            float cosYaw = MathHelper.func_76134_b(mc.field_71439_g.field_70177_z * (float) Math.PI / 180.0F);
            strafeInput *= inputMagnitude;
            forwardInput *= inputMagnitude;
            return new double[]{
                strafeInput * cosYaw - forwardInput * sinYaw, forwardInput * cosYaw + strafeInput * sinYaw
            };
        } else {
            return new double[]{0.0, 0.0};
        }
    }

    public static void fixStrafe(float targetYaw) {
        float angle = MathHelper.func_76142_g(
            adjustYaw(mc.field_71439_g.field_70177_z, getForwardValue(), getLeftValue()) - targetYaw + 22.5F
        );
        switch ((int)(angle + 180.0F) / 45 % 8) {
            case 0:
                mc.field_71439_g.field_71158_b.field_78900_b = -1.0F;
                mc.field_71439_g.field_71158_b.field_78902_a = 0.0F;
                break;
            case 1:
                mc.field_71439_g.field_71158_b.field_78900_b = -1.0F;
                mc.field_71439_g.field_71158_b.field_78902_a = 1.0F;
                break;
            case 2:
                mc.field_71439_g.field_71158_b.field_78900_b = 0.0F;
                mc.field_71439_g.field_71158_b.field_78902_a = 1.0F;
                break;
            case 3:
                mc.field_71439_g.field_71158_b.field_78900_b = 1.0F;
                mc.field_71439_g.field_71158_b.field_78902_a = 1.0F;
                break;
            case 4:
                mc.field_71439_g.field_71158_b.field_78900_b = 1.0F;
                mc.field_71439_g.field_71158_b.field_78902_a = 0.0F;
                break;
            case 5:
                mc.field_71439_g.field_71158_b.field_78900_b = 1.0F;
                mc.field_71439_g.field_71158_b.field_78902_a = -1.0F;
                break;
            case 6:
                mc.field_71439_g.field_71158_b.field_78900_b = 0.0F;
                mc.field_71439_g.field_71158_b.field_78902_a = -1.0F;
                break;
            case 7:
                mc.field_71439_g.field_71158_b.field_78900_b = -1.0F;
                mc.field_71439_g.field_71158_b.field_78902_a = -1.0F;
        }

        if (mc.field_71439_g.field_71158_b.field_78899_d) {
            mc.field_71439_g.field_71158_b.field_78900_b *= 0.3F;
            mc.field_71439_g.field_71158_b.field_78902_a *= 0.3F;
        }
    }

    public static void fixMovement(float yaw) {
        float forward = mc.field_71439_g.field_71158_b.field_78900_b;
        float strafe = mc.field_71439_g.field_71158_b.field_78902_a;
        if (forward != 0.0F || strafe != 0.0F) {
            double angle = MathHelper.func_76138_g(
                Math.toDegrees(direction(mc.field_71439_g.field_70177_z, forward, strafe))
            );
            float closestForward = 0.0F;
            float closestStrafe = 0.0F;
            double closestDifference = Double.MAX_VALUE;

            for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward++) {
                for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe++) {
                    if (predictedStrafe != 0.0F || predictedForward != 0.0F) {
                        double predictedAngle = MathHelper.func_76138_g(
                            Math.toDegrees(direction(yaw, predictedForward, predictedStrafe))
                        );
                        double difference = wrappedDifference(angle, predictedAngle);
                        if (difference < closestDifference) {
                            closestDifference = difference;
                            closestForward = predictedForward;
                            closestStrafe = predictedStrafe;
                        }
                    }
                }
            }

            mc.field_71439_g.field_71158_b.field_78900_b = closestForward;
            mc.field_71439_g.field_71158_b.field_78902_a = closestStrafe;
            if (mc.field_71439_g.field_71158_b.field_78899_d) {
                mc.field_71439_g.field_71158_b.field_78900_b *= 0.3F;
                mc.field_71439_g.field_71158_b.field_78902_a *= 0.3F;
            }
        }
    }

    public static double direction(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0.0) {
            rotationYaw += 180.0F;
        }

        float forward = 1.0F;
        if (moveForward < 0.0) {
            forward = -0.5F;
        } else if (moveForward > 0.0) {
            forward = 0.5F;
        }

        if (moveStrafing > 0.0) {
            rotationYaw -= 90.0F * forward;
        }

        if (moveStrafing < 0.0) {
            rotationYaw += 90.0F * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public static double wrappedDifference(double number1, double number2) {
        return Math.min(
            Math.abs(number1 - number2),
            Math.min(
                Math.abs(number1 - 360.0) - Math.abs(number2 - 0.0),
                Math.abs(number2 - 360.0) - Math.abs(number1 - 0.0)
            )
        );
    }

    public static void silentMoveFix(StrafeEvent event) {
        int dif = (int)(
            (MathHelper.func_76142_g(mc.field_71439_g.field_70177_z - RotationUtil.serverYaw - 23.5F - 135.0F) + 180.0F)
                / 45.0F
        );
        float yaw = RotationUtil.serverYaw;
        float strafe = event.getStrafe();
        float forward = event.getForward();
        float friction = event.getFriction();
        float calcForward = 0.0F;
        float calcStrafe = 0.0F;
        switch (dif) {
            case 0:
                calcForward = forward;
                calcStrafe = strafe;
                break;
            case 1:
                calcForward += forward;
                calcStrafe -= forward;
                calcForward += strafe;
                calcStrafe += strafe;
                break;
            case 2:
                calcForward = strafe;
                calcStrafe = -forward;
                break;
            case 3:
                calcForward -= forward;
                calcStrafe -= forward;
                calcForward += strafe;
                calcStrafe -= strafe;
                break;
            case 4:
                calcForward = -forward;
                calcStrafe = -strafe;
                break;
            case 5:
                calcForward -= forward;
                calcStrafe += forward;
                calcForward -= strafe;
                calcStrafe -= strafe;
                break;
            case 6:
                calcForward = -strafe;
                calcStrafe = forward;
                break;
            case 7:
                calcForward += forward;
                calcStrafe += forward;
                calcForward -= strafe;
                calcStrafe += strafe;
        }

        if (calcForward > 1.0F
            || calcForward < 0.9F && calcForward > 0.3F
            || calcForward < -1.0F
            || calcForward > -0.9F && calcForward < -0.3F) {
            calcForward *= 0.5F;
        }

        if (calcStrafe > 1.0F
            || calcStrafe < 0.9F && calcStrafe > 0.3F
            || calcStrafe < -1.0F
            || calcStrafe > -0.9F && calcStrafe < -0.3F) {
            calcStrafe *= 0.5F;
        }

        float d = calcStrafe * calcStrafe + calcForward * calcForward;
        if (d >= 1.0E-4F) {
            d = MathHelper.func_76129_c(d);
            if (d < 1.0F) {
                d = 1.0F;
            }

            d = friction / d;
            calcStrafe *= d;
            calcForward *= d;
            float yawSin = MathHelper.func_76126_a((float)(yaw * Math.PI / 180.0));
            float yawCos = MathHelper.func_76134_b((float)(yaw * Math.PI / 180.0));
            mc.field_71439_g.field_70159_w += calcStrafe * yawCos - calcForward * yawSin;
        }
    }

    public static double findGround(Entity entity) {
        for (double y = entity.field_70163_u; y > 0.0; y--) {
            BlockPos pos = new BlockPos(entity.field_70165_t, y, entity.field_70161_v);
            if (!mc.field_71441_e.func_175623_d(pos)) {
                return pos.func_177956_o() + 1.0;
            }
        }

        return 0.0;
    }

    public static boolean isMovingMotion(Entity entity) {
        return Math.abs(entity.field_70159_w) > 0.005 || Math.abs(entity.field_70179_y) > 0.005;
    }
}
