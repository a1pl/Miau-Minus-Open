package miau.module.modules.combat.velocity;

import miau.mixin.IAccessorEntity;
import miau.mixin.IAccessorMinecraft;
import miau.util.network.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;

public final class VelocityUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();

    private VelocityUtil() {
    }

    public static float normalizeAngle(float angle) {
        return (angle % 360.0F + 360.0F) % 360.0F;
    }

    public static void reduceXZ(double factor, Integer hurtTimeMin, Integer hurtTimeMax) {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            if (hurtTimeMin == null
                || player.field_70737_aN >= hurtTimeMin
                    && (hurtTimeMax == null || player.field_70737_aN <= hurtTimeMax)) {
                player.field_70159_w *= factor;
                player.field_70179_y *= factor;
            }
        }
    }

    public static void reduceXZ(double factor) {
        reduceXZ(factor, null, null);
    }

    public static void reduceY(double factor, Integer hurtTimeMin, Integer hurtTimeMax) {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            if (hurtTimeMin == null
                || player.field_70737_aN >= hurtTimeMin
                    && (hurtTimeMax == null || player.field_70737_aN <= hurtTimeMax)) {
                player.field_70181_x *= factor;
            }
        }
    }

    public static void reduceY(double factor) {
        reduceY(factor, null, null);
    }

    public static void setMotion(Double x, Double y, Double z) {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            if (x != null) {
                player.field_70159_w = x;
            }

            if (y != null) {
                player.field_70181_x = y;
            }

            if (z != null) {
                player.field_70179_y = z;
            }
        }
    }

    public static void changeSprint(boolean setState, boolean sendPacketToServer) {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            player.func_70031_b(setState);
            if (sendPacketToServer) {
                PacketUtil.sendPacket(
                    new C0BPacketEntityAction(player, setState ? Action.START_SPRINTING : Action.STOP_SPRINTING)
                );
            }
        }
    }

    public static void changeSprint(boolean setState) {
        changeSprint(setState, true);
    }

    public static void changeTimer(float speed) {
        ((IAccessorMinecraft)mc).getTimer().field_74278_d = speed;
    }

    public static void setSprintSafely(boolean value) {
        EntityPlayer player = mc.field_71439_g;
        if (player != null && player.func_70051_ag() != value) {
            player.func_70031_b(value);
        }
    }

    public static void tryJump() {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            if (!mc.field_71474_y.field_74314_A.func_151470_d()) {
                player.func_70664_aZ();
            }
        }
    }

    public static boolean isInBadEnvironment() {
        EntityPlayer player = mc.field_71439_g;
        return player == null
            ? false
            : ((IAccessorEntity)player).getIsInWeb()
                || player.func_180799_ab()
                || player.func_70027_ad()
                || player.func_70090_H()
                || player.func_70115_ae();
    }

    public static boolean isMoving() {
        EntityPlayer player = mc.field_71439_g;
        return player != null && (player.field_70701_bs != 0.0F || player.field_70702_br != 0.0F);
    }

    public static boolean isMovingBackwards() {
        EntityPlayer player = mc.field_71439_g;
        if (player == null) {
            return false;
        }

        double motionX = player.field_70159_w;
        double motionZ = player.field_70179_y;
        if (Math.sqrt(motionX * motionX + motionZ * motionZ) < 0.1) {
            return true;
        }

        float moveAngle = normalizeAngle((float)Math.toDegrees(Math.atan2(motionX, motionZ)));
        float lookAngle = normalizeAngle(player.field_70177_z);
        float angleDiff = Math.min(Math.abs(moveAngle - lookAngle), 360.0F - Math.abs(moveAngle - lookAngle));
        return angleDiff >= 60.0F;
    }

    public static double getDirection() {
        EntityPlayer player = mc.field_71439_g;
        if (player == null) {
            return 0.0;
        }

        float moveYaw = player.field_70177_z;
        if (player.field_70701_bs != 0.0F && player.field_70702_br == 0.0F) {
            moveYaw += player.field_70701_bs > 0.0F ? 0.0F : 180.0F;
        } else if (player.field_70701_bs != 0.0F && player.field_70702_br != 0.0F) {
            if (player.field_70701_bs > 0.0F) {
                moveYaw += player.field_70702_br > 0.0F ? -45.0F : 45.0F;
            } else {
                moveYaw -= player.field_70702_br > 0.0F ? -45.0F : 45.0F;
            }

            moveYaw += player.field_70701_bs > 0.0F ? 0.0F : 180.0F;
        } else if (player.field_70702_br != 0.0F && player.field_70701_bs == 0.0F) {
            moveYaw += player.field_70702_br > 0.0F ? -90.0F : 90.0F;
        }

        return Math.floorMod((int)moveYaw, 360);
    }

    public static EntityLivingBase getNearestEntityInRange(float range) {
        EntityPlayer player = mc.field_71439_g;
        if (player != null && mc.field_71441_e != null) {
            EntityLivingBase best = null;
            double bestDist = Double.MAX_VALUE;

            for (Entity entity : mc.field_71441_e.field_73010_i) {
                if (entity instanceof EntityLivingBase && entity != player) {
                    double dist = player.func_70032_d(entity);
                    if (dist <= range && dist < bestDist) {
                        bestDist = dist;
                        best = (EntityLivingBase)entity;
                    }
                }
            }

            return best;
        } else {
            return null;
        }
    }

    public static double getSpeed() {
        EntityPlayer player = mc.field_71439_g;
        return player == null ? 0.0 : Math.hypot(player.field_70159_w, player.field_70179_y);
    }

    public static int randomInt(int startInclusive, int endExclusive) {
        return endExclusive - startInclusive <= 0
            ? startInclusive
            : startInclusive + (int)(Math.random() * (endExclusive - startInclusive));
    }

    public static float randomFloat(float startInclusive, float endInclusive) {
        return startInclusive != endInclusive && !(endInclusive - startInclusive <= 0.0F)
            ? startInclusive + (float)((endInclusive - startInclusive) * Math.random())
            : startInclusive;
    }

    public static boolean isLookingOnEntities(Entity entity, double maxAngle) {
        EntityPlayer player = mc.field_71439_g;
        if (player == null) {
            return false;
        }

        double dx = entity.field_70165_t - player.field_70165_t;
        double dz = entity.field_70161_v - player.field_70161_v;
        float yaw = normalizeAngle((float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float yawTo = normalizeAngle(player.field_70177_z);
        float diff = Math.min(Math.abs(yaw - yawTo), Math.abs(yaw - yawTo + 360.0F));
        diff = Math.min(diff, 360.0F - diff);
        return diff <= maxAngle;
    }
}
