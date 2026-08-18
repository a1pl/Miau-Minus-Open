package miau.util.player;

import java.security.SecureRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class AimAssistRotationUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final SecureRandom random = new SecureRandom();

    private AimAssistRotationUtil() {
    }

    public static float[] getRotations(Vec3 from_, Vec3 to) {
        double x = to.field_72450_a - from_.field_72450_a;
        double y = to.field_72448_b - from_.field_72448_b;
        double z = to.field_72449_c - from_.field_72449_c;
        double dist = MathHelper.func_76133_a(x * x + z * z);
        float yaw = (float)(MathHelper.func_181159_b(z, x) * 180.0 / Math.PI - 90.0);
        float pitch = (float)(-(MathHelper.func_181159_b(y, dist) * 180.0 / Math.PI));
        return new float[]{yaw, pitch};
    }

    public static double[] heuristics(Entity entity, double[] xyz) {
        double boxSize = 0.2;
        float f11 = entity.func_70111_Y();
        double minX = MathHelper.func_151237_a(
            xyz[0] - boxSize, entity.func_174813_aQ().field_72340_a - f11, entity.func_174813_aQ().field_72336_d + f11
        );
        double minY = MathHelper.func_151237_a(
            xyz[1] - boxSize, entity.func_174813_aQ().field_72338_b - f11, entity.func_174813_aQ().field_72337_e + f11
        );
        double minZ = MathHelper.func_151237_a(
            xyz[2] - boxSize, entity.func_174813_aQ().field_72339_c - f11, entity.func_174813_aQ().field_72334_f + f11
        );
        double maxX = MathHelper.func_151237_a(
            xyz[0] + boxSize, entity.func_174813_aQ().field_72340_a - f11, entity.func_174813_aQ().field_72336_d + f11
        );
        double maxY = MathHelper.func_151237_a(
            xyz[1] + boxSize, entity.func_174813_aQ().field_72338_b - f11, entity.func_174813_aQ().field_72337_e + f11
        );
        double maxZ = MathHelper.func_151237_a(
            xyz[2] + boxSize, entity.func_174813_aQ().field_72339_c - f11, entity.func_174813_aQ().field_72334_f + f11
        );
        xyz[0] = MathHelper.func_151237_a(xyz[0] + randomSin(), minX, maxX);
        xyz[1] = MathHelper.func_151237_a(xyz[1] + randomSin(), minY, maxY);
        xyz[2] = MathHelper.func_151237_a(xyz[2] + randomSin(), minZ, maxZ);
        return xyz;
    }

    public static float randomSin() {
        return MathHelper.func_76126_a(getInRange(0.0F, (float) (Math.PI * 2)));
    }

    public static Vec3 getNearestHitVec(Entity entity, boolean heuristics) {
        Vec3 positionEyes = mc.field_71439_g.func_174824_e(1.0F);
        Vec3 result = getNearestPointBB(positionEyes, entity.func_174813_aQ());
        if (!heuristics) {
            return result;
        }

        double[] pq = heuristics(entity, new double[]{result.field_72450_a, result.field_72448_b, result.field_72449_c});
        return new Vec3(pq[0], pq[1], pq[2]);
    }

    private static Vec3 getNearestPointBB(Vec3 eye, AxisAlignedBB box) {
        double[] origin = new double[]{eye.field_72450_a, eye.field_72448_b, eye.field_72449_c};
        double[] destMins = new double[]{box.field_72340_a, box.field_72338_b, box.field_72339_c};
        double[] destMaxs = new double[]{box.field_72336_d, box.field_72337_e, box.field_72334_f};

        for (int i = 0; i < 3; i++) {
            if (origin[i] > destMaxs[i]) {
                origin[i] = destMaxs[i];
            } else if (origin[i] < destMins[i]) {
                origin[i] = destMins[i];
            }
        }

        return new Vec3(origin[0], origin[1], origin[2]);
    }

    public static float[] face(
        Vec3 pointToFace,
        float yawSpeed,
        float pitchSpeed,
        float currentYaw,
        float currentPitch,
        float randomizeStrength
    ) {
        Vec3 point = pointToFace;
        if (Math.random() <= 0.08) {
            yawSpeed *= 0.04F;
            pitchSpeed *= 0.03F;
        }

        float[] current = new float[]{mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A};
        float[] origin = getRotations(mc.field_71439_g.func_174824_e(1.0F), point);
        float[] target = new float[]{origin[0], origin[1]};
        float[] finalR = smoothRotation(target, current, yawSpeed, pitchSpeed, point);
        float calcYaw = finalR[0];
        float calcPitch = finalR[1];
        calcYaw += randomizeStrength
            * (
                getInRange(-1.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F) * getInRange(0.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F) * getInRange(0.0F, 1.0F) * getInRange(0.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
            );
        calcPitch += randomizeStrength
            * 0.96981317F
            * (
                getInRange(-1.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F) * getInRange(0.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F) * getInRange(0.0F, 1.0F) * getInRange(0.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
            );
        float fixed = (float)(
            calcPitch
                + 1.356526526
                    * Math.sin(0.06981317007977318 * (updateRotation(currentYaw, calcYaw, 180.0F) - calcYaw))
                    * 8.0
        );
        if (!Float.isNaN(fixed)) {
            calcPitch = fixed;
        }

        if (mc.field_71474_y.field_74341_c == 0.5) {
            mc.field_71474_y.field_74341_c = 0.47887325F;
        }

        float f1 = mc.field_71474_y.field_74341_c * 0.6F + 0.2F;
        float f2 = f1 * f1 * f1 * 8.0F;
        int deltaX = (int)((6.667 * calcYaw - 6.667 * currentYaw) / f2);
        int deltaY = (int)((6.667 * calcPitch - 6.667 * currentPitch) / f2) * -1;
        float f5 = deltaX * f2;
        float f3 = deltaY * f2;
        calcYaw = (float)(currentYaw + f5 * 0.15);
        float f4 = (float)(currentPitch - f3 * 0.15);
        calcPitch = MathHelper.func_76131_a(f4, -90.0F, 90.0F);
        return new float[]{calcYaw, calcPitch};
    }

    public static float[] face(
        EntityLivingBase entity,
        float yawSpeed,
        float pitchSpeed,
        float currentYaw,
        float currentPitch,
        boolean heuristics,
        boolean shortStop,
        float P1Max,
        float P1Min,
        float entropyFactor,
        float randomizeStrength
    ) {
        if (shortStop && Math.random() <= 0.08) {
            yawSpeed *= 0.04F;
            pitchSpeed *= 0.03F;
        }

        Vec3 targetPos = getNearestHitVec(entity, heuristics);
        float[] current = new float[]{mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A};
        float[] origin = getRotations(mc.field_71439_g.func_174824_e(1.0F), targetPos);
        float[] target = new float[]{origin[0], origin[1]};
        float[] finalR = smoothRotation(target, current, yawSpeed, pitchSpeed, entity, P1Max, P1Min, entropyFactor);
        float calcYaw = finalR[0];
        float calcPitch = finalR[1];
        calcYaw += randomizeStrength
            * (
                getInRange(-1.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F) * getInRange(0.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F) * getInRange(0.0F, 1.0F) * getInRange(0.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
            );
        calcPitch += randomizeStrength
            * 0.96981317F
            * (
                getInRange(-1.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F) * getInRange(0.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F) * getInRange(0.0F, 1.0F) * getInRange(0.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                    + getInRange(-1.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
                        * getInRange(0.0F, 1.0F)
            );
        float fixed = (float)(
            calcPitch
                + 1.356526526
                    * Math.sin(0.06981317007977318 * (updateRotation(currentYaw, calcYaw, 180.0F) - calcYaw))
                    * 8.0
        );
        if (!Float.isNaN(fixed)) {
            calcPitch = fixed;
        }

        if (mc.field_71474_y.field_74341_c == 0.5) {
            mc.field_71474_y.field_74341_c = 0.47887325F;
        }

        float f1 = mc.field_71474_y.field_74341_c * 0.6F + 0.2F;
        float f2 = f1 * f1 * f1 * 8.0F;
        int deltaX = (int)((6.667 * calcYaw - 6.667 * currentYaw) / f2);
        int deltaY = (int)((6.667 * calcPitch - 6.667 * currentPitch) / f2) * -1;
        float f5 = deltaX * f2;
        float f3 = deltaY * f2;
        calcYaw = (float)(currentYaw + f5 * 0.15);
        float f4 = (float)(currentPitch - f3 * 0.15);
        calcPitch = MathHelper.func_76131_a(f4, -90.0F, 90.0F);
        return new float[]{calcYaw, calcPitch};
    }

    public static float getInRange(float min, float max) {
        return min + (float)(Math.random() * (max - min));
    }

    public static float updateRotation(float current, float calc, float maxDelta) {
        float f = MathHelper.func_76142_g(calc - current);
        if (f > maxDelta) {
            f = maxDelta;
        }

        if (f < -maxDelta) {
            f = -maxDelta;
        }

        return current + f;
    }

    public static float[] smoothRotation(
        float[] currentRotations, float[] targetRotations, float deltaYaw, float deltaPitch, Vec3 point
    ) {
        float smoothFactor = MathHelper.func_76131_a(
            (float)(
                1.0
                    - mc.field_71439_g.func_70011_f(point.field_72450_a, point.field_72448_b, point.field_72449_c)
                        / 15.0
            ),
            0.3F,
            0.9F
        );
        targetRotations = new float[]{targetRotations[0], targetRotations[1]};
        float deltaYawDiff = MathHelper.func_76142_g(targetRotations[0] - currentRotations[0]);
        float deltaPitchDiff = targetRotations[1] - currentRotations[1];
        float controlYaw1 = currentRotations[0] + deltaYawDiff * 0.25F;
        float controlYaw2 = currentRotations[0] + deltaYawDiff * 0.75F;
        float controlPitch1 = currentRotations[1] + deltaPitchDiff * 0.25F;
        float controlPitch2 = currentRotations[1] + deltaPitchDiff * 0.75F;
        float invT = 1.0F - smoothFactor;
        float yaw = invT * invT * invT * currentRotations[0]
            + 3.0F * invT * invT * smoothFactor * controlYaw1
            + 3.0F * invT * smoothFactor * smoothFactor * controlYaw2
            + smoothFactor * smoothFactor * smoothFactor * targetRotations[0];
        float pitch = invT * invT * invT * currentRotations[1]
            + 3.0F * invT * invT * smoothFactor * controlPitch1
            + 3.0F * invT * smoothFactor * smoothFactor * controlPitch2
            + smoothFactor * smoothFactor * smoothFactor * targetRotations[1];
        yaw = currentRotations[0]
            + MathHelper.func_76131_a(MathHelper.func_76142_g(yaw - currentRotations[0]), -deltaYaw, deltaYaw);
        pitch = currentRotations[1] + MathHelper.func_76131_a(pitch - currentRotations[1], -deltaPitch, deltaPitch);
        return new float[]{yaw, pitch};
    }

    public static float[] smoothRotation(
        float[] currentRotations,
        float[] targetRotations,
        float deltaYaw,
        float deltaPitch,
        EntityLivingBase target,
        float maxP,
        float minP,
        float enF
    ) {
        double speed = Math.sqrt(
            target.field_70159_w * target.field_70159_w
                + target.field_70181_x * target.field_70181_x
                + target.field_70179_y * target.field_70179_y
        );
        float smoothFactor = MathHelper.func_76131_a(
            (float)(1.0 - mc.field_71439_g.func_70032_d(target) / 15.0 + speed * 0.3), 0.3F, 0.9F
        );
        double perturbationYaw = (random.nextDouble() * (maxP - minP) + minP) * enF;
        double perturbationPitch = (random.nextDouble() * (maxP - minP) + minP) * enF;
        perturbationYaw *= random.nextBoolean() ? 1.0 : -1.0;
        perturbationPitch *= random.nextBoolean() ? 1.0 : -1.0;
        targetRotations = new float[]{
            targetRotations[0] + (float)perturbationYaw, targetRotations[1] + (float)perturbationPitch
        };
        float deltaYawDiff = MathHelper.func_76142_g(targetRotations[0] - currentRotations[0]);
        float deltaPitchDiff = targetRotations[1] - currentRotations[1];
        float controlYaw1 = currentRotations[0] + deltaYawDiff * 0.25F;
        float controlYaw2 = currentRotations[0] + deltaYawDiff * 0.75F;
        float controlPitch1 = currentRotations[1] + deltaPitchDiff * 0.25F;
        float controlPitch2 = currentRotations[1] + deltaPitchDiff * 0.75F;
        float invT = 1.0F - smoothFactor;
        float yaw = invT * invT * invT * currentRotations[0]
            + 3.0F * invT * invT * smoothFactor * controlYaw1
            + 3.0F * invT * smoothFactor * smoothFactor * controlYaw2
            + smoothFactor * smoothFactor * smoothFactor * targetRotations[0];
        float pitch = invT * invT * invT * currentRotations[1]
            + 3.0F * invT * invT * smoothFactor * controlPitch1
            + 3.0F * invT * smoothFactor * smoothFactor * controlPitch2
            + smoothFactor * smoothFactor * smoothFactor * targetRotations[1];
        yaw = currentRotations[0]
            + MathHelper.func_76131_a(MathHelper.func_76142_g(yaw - currentRotations[0]), -deltaYaw, deltaYaw);
        pitch = currentRotations[1] + MathHelper.func_76131_a(pitch - currentRotations[1], -deltaPitch, deltaPitch);
        return new float[]{yaw, pitch};
    }
}
