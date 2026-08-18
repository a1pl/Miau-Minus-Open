package miau.module.modules.combat.killaura.rotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import miau.component.RotationComponent;
import miau.event.impl.UpdateEvent;
import miau.module.modules.combat.KillAura;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class PolarRotation extends RotationMode {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final Random random = new Random();

    public PolarRotation(KillAura killAura) {
        super(killAura, "POLAR");
    }

    @Override
    public float[] processRotations(float[] targetRots, float[] lastRots, double rotSpeed, UpdateEvent event) {
        RotationComponent.setActive(true, this.killAura.moveFix.getValue());
        EntityLivingBase target = this.killAura.getTarget();
        if (target == null) {
            RotationComponent.markSmoothed(lastRots);
            return lastRots;
        }

        double playerSpeed = Math.hypot(mc.field_71439_g.field_70159_w, mc.field_71439_g.field_70179_y);
        double targetSpeed = Math.hypot(
            target.field_70165_t - target.field_70142_S, target.field_70161_v - target.field_70136_U
        );
        double xzTrim = Math.min(0.15, (playerSpeed + targetSpeed) * 0.2);
        double yTrim = Math.min(0.2, (playerSpeed + targetSpeed) * 0.25);
        AxisAlignedBB bb = target.func_174813_aQ().func_72331_e(xzTrim, yTrim, xzTrim);
        double motionPredFactor = this.random.nextDouble() * 0.7;
        double predX = target.field_70165_t + (target.field_70165_t - target.field_70142_S) * motionPredFactor;
        double predY = target.field_70163_u + (target.field_70163_u - target.field_70137_T) * motionPredFactor;
        double predZ = target.field_70161_v + (target.field_70161_v - target.field_70136_U) * motionPredFactor;
        bb = bb.func_72317_d(predX - target.field_70165_t, predY - target.field_70163_u, predZ - target.field_70161_v);
        List<Vec3> allPoints = this.findPoints(bb, 2048);
        Vec3 eyePos = mc.field_71439_g.func_174824_e(1.0F);
        List<Vec3> validPoints = new ArrayList<>();

        for (Vec3 point : allPoints) {
            if (this.canSeePoint(eyePos, point)) {
                validPoints.add(point);
            }
        }

        Vec3 lookDir = mc.field_71439_g.func_70676_i(1.0F).func_72432_b();
        Vec3 bestPoint;
        if (!validPoints.isEmpty()) {
            validPoints.sort((a, b) -> {
                double da = a.func_178788_d(eyePos).func_72431_c(lookDir).func_72433_c();
                double db = b.func_178788_d(eyePos).func_72431_c(lookDir).func_72433_c();
                return Double.compare(da, db);
            });
            bestPoint = validPoints.get(0);
        } else {
            allPoints.sort((a, b) -> {
                double da = a.func_178788_d(eyePos).func_72431_c(lookDir).func_72433_c();
                double db = b.func_178788_d(eyePos).func_72431_c(lookDir).func_72433_c();
                return Double.compare(da, db);
            });
            bestPoint = allPoints.get(0);
        }

        double diffX = bestPoint.field_72450_a - eyePos.field_72450_a;
        double diffY = bestPoint.field_72448_b - eyePos.field_72448_b;
        double diffZ = bestPoint.field_72449_c - eyePos.field_72449_c;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float)(Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
        yaw = (float)(yaw + this.random.nextGaussian() * 0.15);
        pitch = (float)(pitch + this.random.nextGaussian() * 0.15);
        float jitterFactor = 0.7F;
        if (this.random.nextBoolean()) {
            yaw += (this.random.nextFloat() - 0.5F) * jitterFactor;
            pitch += (this.random.nextFloat() - 0.5F) * jitterFactor;
        }

        float yawDiff = MathHelper.func_76142_g(yaw - lastRots[0]);
        float pitchDiff = pitch - lastRots[1];
        float maxStep = (float)rotSpeed;
        if (Math.abs(yawDiff) > maxStep) {
            yaw = lastRots[0] + Math.copySign(maxStep, yawDiff);
        }

        if (Math.abs(pitchDiff) > maxStep) {
            pitch = lastRots[1] + Math.copySign(maxStep, pitchDiff);
        }

        float[] result = new float[]{yaw, pitch};
        RotationComponent.markSmoothed(result);
        return result;
    }

    private List<Vec3> findPoints(AxisAlignedBB bb, int pointCount) {
        List<Vec3> points = new ArrayList<>();
        double cbrt = Math.cbrt(pointCount);
        double minX = bb.field_72340_a;
        double minY = bb.field_72338_b;
        double minZ = bb.field_72339_c;
        double maxX = bb.field_72336_d;
        double maxY = bb.field_72337_e;
        double maxZ = bb.field_72334_f;
        double width = maxX - minX;
        double height = maxY - minY;
        double depth = maxZ - minZ;
        double total = width + height + depth;
        int stepsX = Math.max(2, (int)(cbrt * (width / total) * 3.0));
        int stepsY = Math.max(2, (int)(cbrt * (height / total) * 3.0));
        int stepsZ = Math.max(2, (int)(cbrt * (depth / total) * 3.0));
        double stepX = width / (stepsX - 1);
        double stepY = height / (stepsY - 1);
        double stepZ = depth / (stepsZ - 1);

        for (int i = 0; i < stepsX; i++) {
            for (int j = 0; j < stepsY; j++) {
                double x = minX + stepX * i;
                double y = minY + stepY * j;
                points.add(new Vec3(x, y, minZ));
                points.add(new Vec3(x, y, maxZ));
            }
        }

        for (int i = 0; i < stepsX; i++) {
            for (int k = 0; k < stepsZ; k++) {
                double x = minX + stepX * i;
                double z = minZ + stepZ * k;
                points.add(new Vec3(x, minY, z));
                points.add(new Vec3(x, maxY, z));
            }
        }

        for (int j = 0; j < stepsY; j++) {
            for (int k = 0; k < stepsZ; k++) {
                double y = minY + stepY * j;
                double z = minZ + stepZ * k;
                points.add(new Vec3(minX, y, z));
                points.add(new Vec3(maxX, y, z));
            }
        }

        return points;
    }

    private boolean canSeePoint(Vec3 eyePos, Vec3 point) {
        return mc.field_71441_e.func_147447_a(eyePos, point, false, true, false) == null;
    }
}
