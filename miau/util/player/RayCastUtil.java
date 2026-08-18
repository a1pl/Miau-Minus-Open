package miau.util.player;

import com.google.common.base.Predicates;
import java.util.List;
import miau.mixin.IAccessorMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class RayCastUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public static MovingObjectPosition rayCast(float yaw, float pitch, double range) {
        return rayCast(yaw, pitch, range, 0.0F);
    }

    public static MovingObjectPosition rayCast(float yaw, float pitch, double range, float expand) {
        return rayCast(yaw, pitch, range, expand, mc.func_175606_aa());
    }

    public static MovingObjectPosition rayCast(float yaw, float pitch, double range, float expand, Entity entity) {
        float partialTicks = ((IAccessorMinecraft)mc).getTimer().field_74281_c;
        if (entity != null && mc.field_71441_e != null) {
            Vec3 eyePos = entity.func_174824_e(partialTicks);
            Vec3 lookVec = getVectorForRotation(pitch, yaw);
            Vec3 targetPos = eyePos.func_72441_c(
                lookVec.field_72450_a * range, lookVec.field_72448_b * range, lookVec.field_72449_c * range
            );
            MovingObjectPosition objectMouseOver = mc.field_71441_e
                .func_147447_a(eyePos, targetPos, false, false, true);
            double d1 = range;
            if (objectMouseOver != null) {
                d1 = objectMouseOver.field_72307_f.func_72438_d(eyePos);
            }

            Entity pointedEntity = null;
            Vec3 vec33 = null;
            float f = 1.0F;
            List<Entity> list = mc.field_71441_e
                .func_175674_a(
                    entity,
                    entity.func_174813_aQ()
                        .func_72321_a(
                            lookVec.field_72450_a * range, lookVec.field_72448_b * range, lookVec.field_72449_c * range
                        )
                        .func_72314_b(1.0, 1.0, 1.0),
                    Predicates.and(EntitySelectors.field_180132_d, Entity::func_70067_L)
                );
            double d2 = d1;

            for (Entity entity1 : list) {
                float f1 = entity1.func_70111_Y() + expand;
                AxisAlignedBB axisalignedbb = entity1.func_174813_aQ().func_72314_b(f1, f1, f1);
                MovingObjectPosition movingobjectposition = axisalignedbb.func_72327_a(eyePos, targetPos);
                if (axisalignedbb.func_72318_a(eyePos)) {
                    if (d2 >= 0.0) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition == null ? eyePos : movingobjectposition.field_72307_f;
                        d2 = 0.0;
                    }
                } else if (movingobjectposition != null) {
                    double d3 = eyePos.func_72438_d(movingobjectposition.field_72307_f);
                    if (d3 < d2 || d2 == 0.0) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition.field_72307_f;
                        d2 = d3;
                    }
                }
            }

            if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
            }

            return objectMouseOver;
        } else {
            return null;
        }
    }

    public static MovingObjectPosition getEntityIntercept(Entity target, float yaw, float pitch, double range) {
        float partialTicks = ((IAccessorMinecraft)mc).getTimer().field_74281_c;
        Entity viewEntity = mc.func_175606_aa();
        if (viewEntity != null && mc.field_71441_e != null && target != null) {
            Vec3 eyePos = viewEntity.func_174824_e(partialTicks);
            Vec3 lookVec = getVectorForRotation(pitch, yaw);
            Vec3 targetPos = eyePos.func_72441_c(
                lookVec.field_72450_a * range, lookVec.field_72448_b * range, lookVec.field_72449_c * range
            );
            float borderSize = target.func_70111_Y();
            AxisAlignedBB bb = target.func_174813_aQ().func_72314_b(borderSize, borderSize, borderSize);
            MovingObjectPosition mop = bb.func_72327_a(eyePos, targetPos);
            return mop == null && !bb.func_72318_a(eyePos)
                ? null
                : new MovingObjectPosition(target, mop != null ? mop.field_72307_f : eyePos);
        } else {
            return null;
        }
    }

    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.func_76134_b(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f1 = MathHelper.func_76126_a(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f2 = -MathHelper.func_76134_b(-pitch * (float) (Math.PI / 180.0));
        float f3 = MathHelper.func_76126_a(-pitch * (float) (Math.PI / 180.0));
        return new Vec3(f1 * f2, f3, f * f2);
    }

    public static boolean inView(Entity entity) {
        int renderDistance = 16 * mc.field_71474_y.field_151451_c;
        float[] rotations = RotationUtil.calculate(entity);
        if (MoveUtil.wrappedDifference(mc.field_71439_g.field_70177_z, rotations[0]) > mc.field_71474_y.field_74334_X) {
            return false;
        }

        if (!(entity instanceof EntityPlayer)) {
            MovingObjectPosition mop = rayCast(rotations[0], rotations[1], renderDistance, 0.2F);
            return mop != null && mop.field_72313_a == MovingObjectType.ENTITY;
        }

        AxisAlignedBB bb = entity.func_174813_aQ();
        double width = bb.field_72336_d - bb.field_72340_a;
        double height = bb.field_72337_e - bb.field_72338_b;
        double depth = bb.field_72334_f - bb.field_72339_c;

        for (double yPercent = 1.0; yPercent >= -1.0; yPercent -= 0.5) {
            for (double xPercent = 1.0; xPercent >= -1.0; xPercent--) {
                for (double zPercent = 1.0; zPercent >= -1.0; zPercent--) {
                    Vec3 point = new Vec3(
                        bb.field_72340_a + width * xPercent,
                        bb.field_72338_b + height * yPercent,
                        bb.field_72339_c + depth * zPercent
                    );
                    float[] subRotations = RotationUtil.calculate(point);
                    MovingObjectPosition mop = rayCast(subRotations[0], subRotations[1], renderDistance, 0.2F);
                    if (mop != null && mop.field_72313_a == MovingObjectType.ENTITY) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
