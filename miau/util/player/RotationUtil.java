package miau.util.player;

import com.google.common.base.Predicates;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import miau.mixin.IAccessorEntity;
import miau.util.math.RandomUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class RotationUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final float FAR_THRESHOLD = 180.0F;
    private static final double BACKUP_FACE_INSET = 0.05;
    private static final int BACKUP_TARGET_TOTAL = 30;
    public static float serverYaw;
    public static float serverPitch;
    public static boolean customRots;
    private static float randomAngle = 0.0F;
    private static float offsetX = 0.0F;
    private static float offsetY = 0.0F;

    public static float unwrapYaw(float yaw, float prevYaw) {
        return prevYaw + (((yaw - prevYaw + 180.0F) % 360.0F + 360.0F) % 360.0F - 180.0F);
    }

    public static float wrapAngleDiff(float angle, float target) {
        return target + MathHelper.func_76142_g(angle - target);
    }

    public static float clampAngle(float angle, float maxAngle) {
        maxAngle = Math.max(0.0F, Math.min(180.0F, maxAngle));
        if (angle > maxAngle) {
            angle = maxAngle;
        } else if (angle < -maxAngle) {
            angle = -maxAngle;
        }

        return angle;
    }

    public static float smoothAngle(float angle, float smoothFactor) {
        return angle
            * (0.5F + 0.5F * (1.0F - Math.max(0.0F, Math.min(1.0F, smoothFactor + RandomUtil.nextFloat(-0.1F, 0.1F)))));
    }

    public static float quantizeAngle(float angle) {
        return (float)(angle - angle % 0.0096F);
    }

    public static Vec3 closestPointOnAabb(AxisAlignedBB box, Vec3 point) {
        double x = Math.max(box.field_72340_a, Math.min(box.field_72336_d, point.field_72450_a));
        double y = Math.max(box.field_72338_b, Math.min(box.field_72337_e, point.field_72448_b));
        double z = Math.max(box.field_72339_c, Math.min(box.field_72334_f, point.field_72449_c));
        return new Vec3(x, y, z);
    }

    public static double distanceSqFromEyeToClosestOnAABB(Entity entity) {
        if (entity != null && mc.field_71439_g != null) {
            Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
            float borderSize = entity.func_70111_Y();
            AxisAlignedBB bb = entity.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
            Vec3 closest = closestPointOnAabb(bb, eye);
            double dx = eye.field_72450_a - closest.field_72450_a;
            double dy = eye.field_72448_b - closest.field_72448_b;
            double dz = eye.field_72449_c - closest.field_72449_c;
            return dx * dx + dy * dy + dz * dz;
        } else {
            return Double.MAX_VALUE;
        }
    }

    public static double distanceFromEyeToClosestOnAABB(Entity entity) {
        double dSq = distanceSqFromEyeToClosestOnAABB(entity);
        return dSq == Double.MAX_VALUE ? Double.MAX_VALUE : Math.sqrt(dSq);
    }

    public static Vec3 getAimPoint(Entity entity, double horizontalMultipoint, double verticalMultipoint) {
        if (entity != null && mc.field_71439_g != null) {
            float borderSize = entity.func_70111_Y();
            AxisAlignedBB bb = entity.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
            double centerX = (bb.field_72340_a + bb.field_72336_d) / 2.0;
            double centerY;
            if (entity instanceof EntityLivingBase) {
                centerY = entity.field_70163_u + ((EntityLivingBase)entity).func_70047_e();
            } else {
                centerY = (bb.field_72338_b + bb.field_72337_e) / 2.0;
            }

            double centerZ = (bb.field_72339_c + bb.field_72334_f) / 2.0;
            Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
            if (bb.func_72318_a(eye)) {
                return new Vec3(centerX, eye.field_72448_b, centerZ);
            }

            Vec3 cl = closestPointOnAabb(bb, eye);
            double tH = Math.max(0.0, Math.min(1.0, horizontalMultipoint / 100.0));
            double tV = Math.max(0.0, Math.min(1.0, verticalMultipoint / 100.0));
            double targetX = centerX + (cl.field_72450_a - centerX) * tH;
            double targetY = centerY + (cl.field_72448_b - centerY) * tV;
            double targetZ = centerZ + (cl.field_72449_c - centerZ) * tH;
            return new Vec3(targetX, targetY, targetZ);
        } else {
            return null;
        }
    }

    public static List<Vec3> buildBackupPoints(Entity entity, Vec3 eye) {
        if (entity != null && mc.field_71439_g != null) {
            float borderSize = entity.func_70111_Y();
            AxisAlignedBB bb = entity.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
            double sizeX = bb.field_72336_d - bb.field_72340_a;
            double sizeY = bb.field_72337_e - bb.field_72338_b;
            double sizeZ = bb.field_72334_f - bb.field_72339_c;
            boolean xPos = eye.field_72450_a > bb.field_72336_d;
            boolean xNeg = eye.field_72450_a < bb.field_72340_a;
            boolean yPos = eye.field_72448_b > bb.field_72337_e;
            boolean yNeg = eye.field_72448_b < bb.field_72338_b;
            boolean zPos = eye.field_72449_c > bb.field_72334_f;
            boolean zNeg = eye.field_72449_c < bb.field_72339_c;
            int visibleFaceCount = (!xPos && !xNeg ? 0 : 1) + (!yPos && !yNeg ? 0 : 1) + (!zPos && !zNeg ? 0 : 1);
            if (visibleFaceCount == 0) {
                return new ArrayList<>();
            }

            int pointsPerFace = 30 / visibleFaceCount;
            List<Vec3> points = new ArrayList<>(36);
            if (xPos || xNeg) {
                double fixedX = xPos ? bb.field_72336_d - 0.05 : bb.field_72340_a + 0.05;
                addFaceGrid(
                    points,
                    0,
                    fixedX,
                    bb.field_72338_b + 0.05,
                    bb.field_72337_e - 0.05,
                    bb.field_72339_c + 0.05,
                    bb.field_72334_f - 0.05,
                    pointsPerFace,
                    sizeY,
                    sizeZ
                );
            }

            if (yPos || yNeg) {
                double fixedY = yPos ? bb.field_72337_e - 0.05 : bb.field_72338_b + 0.05;
                addFaceGrid(
                    points,
                    1,
                    fixedY,
                    bb.field_72340_a + 0.05,
                    bb.field_72336_d - 0.05,
                    bb.field_72339_c + 0.05,
                    bb.field_72334_f - 0.05,
                    pointsPerFace,
                    sizeX,
                    sizeZ
                );
            }

            if (zPos || zNeg) {
                double fixedZ = zPos ? bb.field_72334_f - 0.05 : bb.field_72339_c + 0.05;
                addFaceGrid(
                    points,
                    2,
                    fixedZ,
                    bb.field_72340_a + 0.05,
                    bb.field_72336_d - 0.05,
                    bb.field_72338_b + 0.05,
                    bb.field_72337_e - 0.05,
                    pointsPerFace,
                    sizeX,
                    sizeY
                );
            }

            return points;
        } else {
            return new ArrayList<>();
        }
    }

    private static void addFaceGrid(
        List<Vec3> out,
        int fixedAxis,
        double fixedVal,
        double uMin,
        double uMax,
        double vMin,
        double vMax,
        int targetPoints,
        double dimU,
        double dimV
    ) {
        if (!(dimU < 1.0E-4) && !(dimV < 1.0E-4)) {
            double ratio = dimU / dimV;
            int gridU = Math.max(2, (int)Math.round(Math.sqrt(targetPoints * ratio)));
            int gridV = Math.max(2, (int)Math.round(Math.sqrt(targetPoints / ratio)));

            for (int i = 0; i < gridU; i++) {
                double u = uMin + (uMax - uMin) * i / (gridU - 1);

                for (int j = 0; j < gridV; j++) {
                    double v = vMin + (vMax - vMin) * j / (gridV - 1);
                    switch (fixedAxis) {
                        case 0:
                            out.add(new Vec3(fixedVal, u, v));
                            break;
                        case 1:
                            out.add(new Vec3(u, fixedVal, v));
                            break;
                        case 2:
                            out.add(new Vec3(u, v, fixedVal));
                    }
                }
            }
        } else {
            double uMid = (uMin + uMax) / 2.0;
            double vMid = (vMin + vMax) / 2.0;
            switch (fixedAxis) {
                case 0:
                    out.add(new Vec3(fixedVal, uMid, vMid));
                    break;
                case 1:
                    out.add(new Vec3(uMid, fixedVal, vMid));
                    break;
                case 2:
                    out.add(new Vec3(uMid, vMid, fixedVal));
            }
        }
    }

    private static boolean mainRayHitsTargetAABB(Vec3 eye, Vec3 point, Entity target, double range) {
        double dx = point.field_72450_a - eye.field_72450_a;
        double dy = point.field_72448_b - eye.field_72448_b;
        double dz = point.field_72449_c - eye.field_72449_c;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-6) {
            return false;
        }

        double scale = range / len;
        Vec3 end = new Vec3(
            eye.field_72450_a + dx * scale, eye.field_72448_b + dy * scale, eye.field_72449_c + dz * scale
        );
        float borderSize = target.func_70111_Y();
        AxisAlignedBB aabb = target.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
        return aabb.func_72327_a(eye, end) != null;
    }

    private static boolean hasEntityBlockingPath(Vec3 eye, Vec3 end, Entity target, double targetDistSq) {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            Vec3 delta = end.func_178788_d(eye);
            AxisAlignedBB searchBox = mc.field_71439_g
                .func_174813_aQ()
                .func_72321_a(delta.field_72450_a, delta.field_72448_b, delta.field_72449_c)
                .func_72314_b(1.0, 1.0, 1.0);

            for (Entity entity : mc.field_71441_e
                .func_175674_a(
                    mc.field_71439_g, searchBox, Predicates.and(EntitySelectors.field_180132_d, Entity::func_70067_L)
                )) {
                if (entity != null && entity != target && !entity.field_70128_L) {
                    float border = entity.func_70111_Y();
                    AxisAlignedBB bb = entity.func_174813_aQ().func_72314_b(border, border, border);
                    MovingObjectPosition hit = bb.func_72327_a(eye, end);
                    if (bb.func_72318_a(eye)) {
                        return true;
                    }

                    if (hit != null) {
                        double entityDistSq = eye.func_72436_e(hit.field_72307_f);
                        if (entityDistSq < targetDistSq - 1.0E-7) {
                            return true;
                        }
                    }
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public static boolean isPathBlockedByEntity(Vec3 eye, Vec3 hitVec, Entity target) {
        if (eye != null && hitVec != null && target != null) {
            double targetDistSq = eye.func_72436_e(hitVec);
            return hasEntityBlockingPath(eye, hitVec, target, targetDistSq);
        } else {
            return false;
        }
    }

    public static boolean canAimAtPoint(
        Vec3 eye, Vec3 point, Entity target, double range, boolean allowThroughBlocks, boolean allowThroughEntities
    ) {
        if (target == null) {
            return false;
        }

        double dx = point.field_72450_a - eye.field_72450_a;
        double dy = point.field_72448_b - eye.field_72448_b;
        double dz = point.field_72449_c - eye.field_72449_c;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-6) {
            return false;
        }

        double scale = range / len;
        Vec3 end = new Vec3(
            eye.field_72450_a + dx * scale, eye.field_72448_b + dy * scale, eye.field_72449_c + dz * scale
        );
        float borderSize = target.func_70111_Y();
        AxisAlignedBB aabb = target.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
        MovingObjectPosition entityHit = aabb.func_72327_a(eye, end);
        if (entityHit == null) {
            return false;
        }

        double entityDistSq = eye.func_72436_e(entityHit.field_72307_f);
        if (!allowThroughBlocks) {
            MovingObjectPosition blockHit = mc.field_71441_e.func_147447_a(eye, end, false, false, false);
            if (blockHit != null && blockHit.field_72313_a == MovingObjectType.BLOCK) {
                double blockDistSq = eye.func_72436_e(blockHit.field_72307_f);
                if (blockDistSq < entityDistSq) {
                    return false;
                }
            }
        }

        return allowThroughEntities || !hasEntityBlockingPath(eye, end, target, entityDistSq);
    }

    public static boolean canAimAtPoint(Vec3 eye, Vec3 point, Entity target, double range) {
        return canAimAtPoint(eye, point, target, range, false, true);
    }

    public static boolean hasValidAimPoint(
        Entity entity,
        double hMult,
        double vMult,
        double range,
        boolean allowThroughBlocks,
        boolean allowThroughEntities
    ) {
        if (entity != null && mc.field_71439_g != null) {
            Vec3 mainPoint = getAimPoint(entity, hMult, vMult);
            if (mainPoint == null) {
                return false;
            }

            Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
            if (eye.func_72436_e(mainPoint) < 1.0E-6) {
                return true;
            }

            if (!mainRayHitsTargetAABB(eye, mainPoint, entity, range)) {
                return false;
            }

            if (canAimAtPoint(eye, mainPoint, entity, range, allowThroughBlocks, allowThroughEntities)) {
                return true;
            }

            List<Vec3> backups = buildBackupPoints(entity, eye);
            Collections.sort(backups, Comparator.comparingDouble(px -> {
                double dx = px.field_72450_a - eye.field_72450_a;
                double dy = px.field_72448_b - eye.field_72448_b;
                double dz = px.field_72449_c - eye.field_72449_c;
                return dx * dx + dy * dy + dz * dz;
            }));

            for (Vec3 p : backups) {
                if (canAimAtPoint(eye, p, entity, range, allowThroughBlocks, allowThroughEntities)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public static boolean hasValidAimPoint(Entity entity, double hMult, double vMult, double range) {
        return hasValidAimPoint(entity, hMult, vMult, range, false, true);
    }

    public static float[] getRotationsWithBackup(
        Entity entity,
        double horizontalMultipoint,
        double verticalMultipoint,
        float baseYaw,
        float basePitch,
        double range,
        boolean allowThroughBlocks,
        boolean allowThroughEntities
    ) {
        if (entity != null && mc.field_71439_g != null) {
            Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
            float borderSize = entity.func_70111_Y();
            AxisAlignedBB bb = entity.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
            if (bb.func_72318_a(eye)) {
                double centerX = (bb.field_72340_a + bb.field_72336_d) / 2.0;
                double centerZ = (bb.field_72339_c + bb.field_72334_f) / 2.0;
                return getRotationsToPoint(centerX, eye.field_72448_b, centerZ, baseYaw, basePitch);
            }

            Vec3 mainPoint = getAimPoint(entity, horizontalMultipoint, verticalMultipoint);
            if (mainPoint == null) {
                return null;
            }

            if (eye.func_72436_e(mainPoint) < 1.0E-6) {
                return null;
            }

            if (!mainRayHitsTargetAABB(eye, mainPoint, entity, range)) {
                return getRotationsToPoint(
                    mainPoint.field_72450_a, mainPoint.field_72448_b, mainPoint.field_72449_c, baseYaw, basePitch
                );
            }

            if (canAimAtPoint(eye, mainPoint, entity, range, allowThroughBlocks, allowThroughEntities)) {
                return getRotationsToPoint(
                    mainPoint.field_72450_a, mainPoint.field_72448_b, mainPoint.field_72449_c, baseYaw, basePitch
                );
            }

            List<Vec3> backups = buildBackupPoints(entity, eye);
            Collections.sort(backups, Comparator.comparingDouble(px -> {
                double dx = px.field_72450_a - eye.field_72450_a;
                double dy = px.field_72448_b - eye.field_72448_b;
                double dz = px.field_72449_c - eye.field_72449_c;
                return dx * dx + dy * dy + dz * dz;
            }));

            for (Vec3 p : backups) {
                if (canAimAtPoint(eye, p, entity, range, allowThroughBlocks, allowThroughEntities)) {
                    return getRotationsToPoint(p.field_72450_a, p.field_72448_b, p.field_72449_c, baseYaw, basePitch);
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public static float[] getRotationsWithBackup(
        Entity entity,
        double horizontalMultipoint,
        double verticalMultipoint,
        float baseYaw,
        float basePitch,
        double range
    ) {
        return getRotationsWithBackup(
            entity, horizontalMultipoint, verticalMultipoint, baseYaw, basePitch, range, false, true
        );
    }

    public static float[] getRotationsToBox(
        AxisAlignedBB boundingBox, float yaw, float pitch, float maxAngle, float smoothFactor
    ) {
        Vec3 eyePos = mc.field_71439_g.func_174824_e(1.0F);
        double minTargetY = boundingBox.field_72338_b + 0.05 * (boundingBox.field_72337_e - boundingBox.field_72338_b);
        double maxTargetY = boundingBox.field_72338_b + 0.75 * (boundingBox.field_72337_e - boundingBox.field_72338_b);
        double deltaX = (boundingBox.field_72340_a + boundingBox.field_72336_d) / 2.0 - eyePos.field_72450_a;
        double deltaY = eyePos.field_72448_b >= maxTargetY
            ? maxTargetY - eyePos.field_72448_b
            : (eyePos.field_72448_b <= minTargetY ? minTargetY - eyePos.field_72448_b : 0.0);
        double deltaZ = (boundingBox.field_72339_c + boundingBox.field_72334_f) / 2.0 - eyePos.field_72449_c;
        return getRotations(deltaX, deltaY, deltaZ, yaw, pitch, maxAngle, smoothFactor);
    }

    public static float[] getRotationsTo(
        double targetX, double targetY, double targetZ, float currentYaw, float currentPitch
    ) {
        return getRotations(targetX, targetY, targetZ, currentYaw, currentPitch, 180.0F, 0.0F);
    }

    public static float[] getRotations(
        double targetX,
        double targetY,
        double targetZ,
        float currentYaw,
        float currentPitch,
        float maxAngle,
        float smoothFactor
    ) {
        double horizontalDistance = Math.sqrt(targetX * targetX + targetZ * targetZ);
        float yawDelta = MathHelper.func_76142_g(
            (float)(Math.atan2(targetZ, targetX) * 180.0 / Math.PI) - 90.0F - currentYaw
        );
        float pitchDelta = MathHelper.func_76142_g(
            (float)(-Math.atan2(targetY, horizontalDistance) * 180.0 / Math.PI) - currentPitch
        );
        yawDelta = Math.abs(yawDelta) <= 1.0F ? 0.0F : smoothAngle(clampAngle(yawDelta, maxAngle), smoothFactor);
        pitchDelta = Math.abs(pitchDelta) <= 1.0F ? 0.0F : smoothAngle(clampAngle(pitchDelta, maxAngle), smoothFactor);
        return new float[]{quantizeAngle(currentYaw + yawDelta), quantizeAngle(currentPitch + pitchDelta)};
    }

    public static float[] getRotationsToPoint(double x, double y, double z, float baseYaw, float basePitch) {
        double deltaX = x - mc.field_71439_g.field_70165_t;
        double deltaZ = z - mc.field_71439_g.field_70161_v;
        double deltaY = y - (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
        double horizDistSq = deltaX * deltaX + deltaZ * deltaZ;
        float yaw;
        float targetPitch;
        if (horizDistSq < 1.0E-12) {
            yaw = baseYaw;
            targetPitch = (float)(-(Math.atan2(deltaY, 0.0) * (float) (180.0 / Math.PI)));
        } else {
            float targetYaw = (float)(Math.atan2(deltaZ, deltaX) * (float) (180.0 / Math.PI)) - 90.0F;
            yaw = baseYaw + MathHelper.func_76142_g(targetYaw - baseYaw);
            double horizDist = MathHelper.func_76133_a(horizDistSq);
            targetPitch = (float)(-(Math.atan2(deltaY, horizDist) * (float) (180.0 / Math.PI)));
        }

        float pitch = basePitch + MathHelper.func_76142_g(targetPitch - basePitch);
        return new float[]{yaw, clampPitch(pitch)};
    }

    public static float[] getRotationsFromEye(Vec3 eye, double tx, double ty, double tz) {
        double dx = tx - eye.field_72450_a;
        double dy = ty - eye.field_72448_b;
        double dz = tz - eye.field_72449_c;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
        return new float[]{yaw, pitch};
    }

    public static float[] getRotations(Entity entity) {
        double yOffset = Math.max(
            0.0,
            Math.min(
                mc.field_71439_g.field_70163_u - entity.field_70163_u + mc.field_71439_g.func_70047_e(),
                (entity.func_174813_aQ().field_72337_e - entity.func_174813_aQ().field_72338_b) * 0.9
            )
        );
        return calculate(new Vec3(entity.field_70165_t, entity.field_70163_u + yOffset, entity.field_70161_v));
    }

    public static float[] calculate(Vec3 to) {
        Vec3 from = mc.field_71439_g.func_174824_e(1.0F);
        double diffX = to.field_72450_a - from.field_72450_a;
        double diffY = to.field_72448_b - from.field_72448_b;
        double diffZ = to.field_72449_c - from.field_72449_c;
        double distance = Math.hypot(diffX, diffZ);
        float yaw = (float)(MathHelper.func_181159_b(diffZ, diffX) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float)(-(MathHelper.func_181159_b(diffY, distance) * 180.0 / Math.PI));
        return new float[]{MathHelper.func_76142_g(yaw), MathHelper.func_76142_g(pitch)};
    }

    public static float[] calculate(Entity entity) {
        double yOffset = Math.max(
            0.0,
            Math.min(
                mc.field_71439_g.field_70163_u - entity.field_70163_u + mc.field_71439_g.func_70047_e(),
                (entity.func_174813_aQ().field_72337_e - entity.func_174813_aQ().field_72338_b) * 0.9
            )
        );
        return calculate(new Vec3(entity.field_70165_t, entity.field_70163_u + yOffset, entity.field_70161_v));
    }

    public static float[] calculate(Entity entity, boolean adaptive, double range) {
        float[] normalRotations = calculate(entity);
        if (adaptive && !rayCastHit(normalRotations, range, entity)) {
            AxisAlignedBB boundingBox = entity.func_174813_aQ();
            double width = boundingBox.field_72336_d - boundingBox.field_72340_a;
            double height = boundingBox.field_72337_e - boundingBox.field_72338_b;
            double depth = boundingBox.field_72334_f - boundingBox.field_72339_c;

            for (double yPercent = 1.0; yPercent >= 0.0; yPercent -= 0.25 + Math.random() * 0.1) {
                for (double xPercent = 1.0; xPercent >= -0.5; xPercent -= 0.5) {
                    for (double zPercent = 1.0; zPercent >= -0.5; zPercent -= 0.5) {
                        Vec3 targetPoint = new Vec3(
                            boundingBox.field_72340_a + width * xPercent,
                            boundingBox.field_72338_b + height * yPercent,
                            boundingBox.field_72339_c + depth * zPercent
                        );
                        float[] adaptiveRotations = calculate(targetPoint);
                        if (rayCastHit(adaptiveRotations, range, entity)) {
                            return adaptiveRotations;
                        }
                    }
                }
            }

            return normalRotations;
        } else {
            return normalRotations;
        }
    }

    public static boolean rayCastHit(float[] rotations, double range, Entity target) {
        MovingObjectPosition mop = RayCastUtil.rayCast(rotations[0], rotations[1], range, 0.0F, target);
        return mop != null && mop.field_72313_a == MovingObjectType.ENTITY;
    }

    public static float clampPitch(float n) {
        return MathHelper.func_76131_a(n, -90.0F, 90.0F);
    }

    public static float[] applySensitivityPatch(float yaw, float pitch, float prevYaw, float prevPitch) {
        float mouseSensitivity = (float)(mc.field_71474_y.field_74341_c * (1.0 + Math.random() / 1.0E7) * 0.6F + 0.2F);
        double multiplier = mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15;
        float fixedYaw = prevYaw + (float)(Math.round((yaw - prevYaw) / multiplier) * multiplier);
        float fixedPitch = prevPitch + (float)(Math.round((pitch - prevPitch) / multiplier) * multiplier);
        return new float[]{fixedYaw, MathHelper.func_76131_a(fixedPitch, -90.0F, 90.0F)};
    }

    public static float[] smoothRotation(
        float baseYaw, float basePitch, float targetYaw, float targetPitch, int speed, float randomizationPercent
    ) {
        if (speed <= 0) {
            return new float[]{baseYaw, clampPitch(basePitch)};
        }

        if (speed >= 30) {
            return new float[]{targetYaw, clampPitch(targetPitch)};
        }

        float deltaYaw = MathHelper.func_76142_g(targetYaw - baseYaw);
        float deltaPitch = targetPitch - basePitch;
        float magnitude = MathHelper.func_76133_a(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
        if (magnitude < 0.001F) {
            return new float[]{targetYaw, clampPitch(targetPitch)};
        }

        float t = speed / 30.0F;
        float stepSize = t * t * 180.0F;
        float range = 0.6F * (float)(randomizationPercent / 100.0);
        float multiplier = range <= 0.001F ? 1.0F : 1.0F - range / 2.0F + (float)(Math.random() * range);
        stepSize *= multiplier;
        float proximityFactor = Math.min(1.0F, magnitude / 180.0F);
        proximityFactor = (float)Math.pow(proximityFactor, 0.7);
        float maxSlowdown = (float)(randomizationPercent / 100.0);
        float proximityMult = Math.max(0.8F, 1.0F - maxSlowdown * (1.0F - proximityFactor));
        stepSize *= proximityMult;
        float stepLength = Math.min(stepSize, magnitude);
        float scale = stepLength / magnitude;
        float stepYaw = deltaYaw * scale;
        float stepPitch = deltaPitch * scale;
        float yaw = baseYaw + stepYaw;
        float pitch = basePitch + stepPitch;
        return new float[]{yaw, clampPitch(pitch)};
    }

    public static double mouseGcdStepMultiplier() {
        float sensitivity = mc.field_71474_y.field_74341_c * 0.6F + 0.2F;
        return sensitivity * sensitivity * sensitivity * 8.0F * 0.15;
    }

    public static float[] flexRotation(float targetYaw, float targetPitch, float baseYaw, float basePitch) {
        float sensitivity = (float)(mc.field_71474_y.field_74341_c * (1.0 + Math.random() / 1.0E7) * 0.6F + 0.2F);
        double multiplier = sensitivity * sensitivity * sensitivity * 8.0F * 0.15;
        float yaw = baseYaw + (float)(Math.round((targetYaw - baseYaw) / multiplier) * multiplier);
        float pitch = basePitch + (float)(Math.round((targetPitch - basePitch) / multiplier) * multiplier);
        return new float[]{yaw, MathHelper.func_76131_a(pitch, -90.0F, 90.0F)};
    }

    public static float[] antiDetectionRotation(
        float targetYaw,
        float targetPitch,
        float baseYaw,
        float basePitch,
        long lastPitchQuotient,
        boolean clampPitchForScaffoldE
    ) {
        float mcpSensitivity = (float)(mc.field_71474_y.field_74341_c * (1.0 + Math.random() / 1.0E7) * 0.6F + 0.2F);
        double multiplier = mcpSensitivity * mcpSensitivity * mcpSensitivity * 8.0F * 0.15;
        if (multiplier < 0.01) {
            multiplier = 0.01;
        }

        float rawYawDelta = MathHelper.func_76142_g(targetYaw - baseYaw);
        long yawK = Math.round(rawYawDelta / multiplier);
        if (yawK == 0L) {
            yawK = rawYawDelta > 0.0F ? 1L : -1L;
        }

        float yaw = baseYaw + (float)(yawK * multiplier);
        float rawPitchDelta = targetPitch - basePitch;
        long pitchK = Math.round(rawPitchDelta / multiplier);
        if (pitchK == 0L) {
            pitchK = rawPitchDelta > 0.0F ? 1L : -1L;
        }

        long absPitchK = Math.abs(pitchK);
        long absLastPitchK = Math.abs(lastPitchQuotient);
        if (absLastPitchK > 0L && absPitchK > 0L && gcd(absPitchK, absLastPitchK) > 1L) {
            long alt1 = pitchK > 0L ? pitchK + 1L : pitchK - 1L;
            long alt2 = pitchK > 0L ? pitchK - 1L : pitchK + 1L;
            if (alt2 == 0L) {
                alt2 = pitchK > 0L ? 1L : -1L;
            }

            long absAlt1 = Math.abs(alt1);
            long absAlt2 = Math.abs(alt2);
            if (absAlt1 > 0L && gcd(absAlt1, absLastPitchK) == 1L) {
                pitchK = alt1;
            } else if (absAlt2 > 0L && gcd(absAlt2, absLastPitchK) == 1L) {
                pitchK = alt2;
            }
        }

        float pitch = basePitch + (float)(pitchK * multiplier);
        pitch = MathHelper.func_76131_a(pitch, -90.0F, 90.0F);
        return new float[]{yaw, pitch};
    }

    private static long gcd(long a, long b) {
        return b == 0L ? a : gcd(b, a % b);
    }

    public static float[] smooth(
        float[] lastRotation, float[] targetRotation, double speed, Entity targetEntity, double range
    ) {
        float targetYaw = targetRotation[0];
        float targetPitch = targetRotation[1];
        float lastYaw = lastRotation[0];
        float lastPitch = lastRotation[1];
        if (targetEntity != null && (Math.abs(targetYaw - lastYaw) > 5.0F || Math.abs(targetPitch - lastPitch) > 5.0F)) {
            double driftSpeed = Math.random() * Math.random() * Math.random() * 20.0;
            randomAngle = randomAngle
                + (float)(
                    (20.0 + (Math.random() - 0.5) * (Math.random() * Math.random() * Math.random() * 360.0))
                        * (mc.field_71439_g.field_70173_aa / 10 % 2 == 0 ? -1 : 1)
                );
            offsetX = (float)(offsetX + -MathHelper.func_76126_a((float)Math.toRadians(randomAngle)) * driftSpeed);
            offsetY = (float)(offsetY + MathHelper.func_76134_b((float)Math.toRadians(randomAngle)) * driftSpeed);
            targetYaw += offsetX;
            targetPitch += offsetY;
            if (!rayCastHit(new float[]{targetYaw, targetPitch}, range, targetEntity)) {
                randomAngle = (float)Math.toDegrees(
                        Math.atan2(targetRotation[0] - targetYaw, lastPitch - targetRotation[1])
                    )
                    - 180.0F;
                targetYaw -= offsetX;
                targetPitch -= offsetY;
                offsetX = (float)(offsetX + -MathHelper.func_76126_a((float)Math.toRadians(randomAngle)) * driftSpeed);
                offsetY = (float)(offsetY + MathHelper.func_76134_b((float)Math.toRadians(randomAngle)) * driftSpeed);
                targetYaw += offsetX;
                targetPitch += offsetY;
            }

            if (!rayCastHit(new float[]{targetYaw, targetPitch}, range, targetEntity)) {
                offsetX = 0.0F;
                offsetY = 0.0F;
                targetYaw = (float)(targetRotation[0] + Math.random() * 2.0);
                targetPitch = (float)(targetRotation[1] + Math.random() * 2.0);
            }
        }

        if (speed != 0.0) {
            double deltaYaw = MathHelper.func_76142_g(targetYaw - lastYaw);
            double deltaPitch = targetPitch - lastPitch;
            double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
            if (distance > 0.001) {
                double distributionYaw = Math.abs(deltaYaw / distance);
                double distributionPitch = Math.abs(deltaPitch / distance);
                double maxYaw = speed * distributionYaw;
                double maxPitch = speed * distributionPitch;
                float moveYaw = (float)Math.max(Math.min(deltaYaw, maxYaw), -maxYaw);
                float movePitch = (float)Math.max(Math.min(deltaPitch, maxPitch), -maxPitch);
                float yaw = lastYaw + moveYaw;
                float pitch = lastPitch + movePitch;

                for (int i = 1; i <= (int)(Minecraft.func_175610_ah() / 20.0F + Math.random() * 10.0); i++) {
                    if (Math.abs(moveYaw) + Math.abs(movePitch) > 1.0E-4) {
                        yaw = (float)(yaw + (Math.random() - 0.5) / 1000.0);
                        pitch = (float)(pitch - Math.random() / 200.0);
                    }

                    float[] fixedRotations = applySensitivityPatch(yaw, pitch, lastYaw, lastPitch);
                    yaw = fixedRotations[0];
                    pitch = Math.max(-90.0F, Math.min(90.0F, fixedRotations[1]));
                }

                return new float[]{yaw, pitch};
            }
        }

        return targetRotation;
    }

    public static Vec3 clampVecToBox(Vec3 vector, AxisAlignedBB boundingBox) {
        double[] coords = new double[]{vector.field_72450_a, vector.field_72448_b, vector.field_72449_c};
        double[] minCoords = new double[]{
            boundingBox.field_72340_a, boundingBox.field_72338_b, boundingBox.field_72339_c
        };
        double[] maxCoords = new double[]{
            boundingBox.field_72336_d, boundingBox.field_72337_e, boundingBox.field_72334_f
        };

        for (int i = 0; i < 3; i++) {
            if (coords[i] > maxCoords[i]) {
                coords[i] = maxCoords[i];
            } else if (coords[i] < minCoords[i]) {
                coords[i] = minCoords[i];
            }
        }

        return new Vec3(coords[0], coords[1], coords[2]);
    }

    public static double distanceToEntity(Entity entity) {
        float borderSize = entity.func_70111_Y();
        AxisAlignedBB boundingBox = entity.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
        return distanceToBox(boundingBox);
    }

    public static double distanceToBox(Entity entity, Vec3 point) {
        float borderSize = entity.func_70111_Y();
        return clampVecToBox(entity.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize), point);
    }

    public static double distanceToBox(AxisAlignedBB boundingBox) {
        return clampVecToBox(boundingBox, mc.field_71439_g.func_174824_e(1.0F));
    }

    public static double clampVecToBox(AxisAlignedBB boundingBox, Vec3 point) {
        if (boundingBox.func_72318_a(point)) {
            return 0.0;
        }

        Vec3 clampedPoint = clampVecToBox(point, boundingBox);
        double deltaX = clampedPoint.field_72450_a - point.field_72450_a;
        double deltaY = clampedPoint.field_72448_b - point.field_72448_b;
        double deltaZ = clampedPoint.field_72449_c - point.field_72449_c;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    public static float angleToEntity(Entity entity) {
        Vec3 eyePos = mc.field_71439_g.func_174824_e(1.0F);
        float borderSize = entity.func_70111_Y();
        AxisAlignedBB boundingBox = entity.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
        if (boundingBox.func_72318_a(eyePos)) {
            return 0.0F;
        }

        double deltaX = entity.field_70165_t - eyePos.field_72450_a;
        double deltaZ = entity.field_70161_v - eyePos.field_72449_c;
        return Math.abs(
                MathHelper.func_76142_g(
                    (float)(Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0F - mc.field_71439_g.field_70177_z
                )
            )
            * 2.0F;
    }

    public static float getYawBetween(double x1, double z1, double x2, double z2) {
        return MathHelper.func_76142_g(
            (float)(Math.atan2(z2 - z1, x2 - x1) * 180.0 / Math.PI) - 90.0F - mc.field_71439_g.field_70177_z
        );
    }

    public static MovingObjectPosition rayTrace(float yaw, float pitch, double distance, float partialTicks) {
        Vec3 eyePos = mc.field_71439_g.func_174824_e(partialTicks);
        Vec3 lookVec = ((IAccessorEntity)mc.field_71439_g).callGetVectorForRotation(pitch, yaw);
        Vec3 targetPos = eyePos.func_72441_c(
            lookVec.field_72450_a * distance, lookVec.field_72448_b * distance, lookVec.field_72449_c * distance
        );
        return mc.field_71441_e.func_72933_a(eyePos, targetPos);
    }

    public static MovingObjectPosition rayTrace(Entity entity) {
        Vec3 eyePos = mc.field_71439_g.func_174824_e(1.0F);
        float borderSize = entity.func_70111_Y();
        Vec3 targetPos = clampVecToBox(eyePos, entity.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize));
        return mc.field_71441_e.func_72933_a(eyePos, targetPos);
    }

    public static MovingObjectPosition rayCastBlock(double distance, float yaw, float pitch) {
        Vec3 eyeVec = mc.field_71439_g.func_174824_e(1.0F);
        float f = MathHelper.func_76134_b(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f1 = MathHelper.func_76126_a(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f2 = -MathHelper.func_76134_b(-pitch * (float) (Math.PI / 180.0));
        float f3 = MathHelper.func_76126_a(-pitch * (float) (Math.PI / 180.0));
        Vec3 lookVec = new Vec3(f1 * f2, f3, f * f2);
        Vec3 sumVec = eyeVec.func_72441_c(
            lookVec.field_72450_a * distance, lookVec.field_72448_b * distance, lookVec.field_72449_c * distance
        );
        MovingObjectPosition mop = mc.field_71441_e.func_147447_a(eyeVec, sumVec, false, false, false);
        return mop != null && mop.field_72313_a == MovingObjectType.BLOCK ? mop : null;
    }

    public static float[] resetRotation(float[] rotation) {
        if (rotation == null) {
            return null;
        }

        float yaw = rotation[0] + MathHelper.func_76142_g(mc.field_71439_g.field_70177_z - rotation[0]);
        float pitch = mc.field_71439_g.field_70125_A;
        return new float[]{yaw, pitch};
    }
}
