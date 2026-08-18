package miau.util.player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import miau.module.modules.misc.AntiBot;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

public class CombatTargeting {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public static List<EntityLivingBase> getTargets(
        boolean targetPlayers,
        boolean targetMobs,
        boolean targetAnimals,
        boolean targetInvis,
        boolean ignoreTeammates,
        boolean ignoreFriends,
        double range,
        CombatTargeting.SortMode sortMode
    ) {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            List<EntityLivingBase> targets = mc.field_71441_e
                .field_72996_f
                .stream()
                .filter(entity -> entity instanceof EntityLivingBase)
                .map(entity -> (EntityLivingBase)entity)
                .filter(
                    entity -> isValidTarget(
                        entity,
                        targetPlayers,
                        targetMobs,
                        targetAnimals,
                        targetInvis,
                        ignoreTeammates,
                        ignoreFriends,
                        range
                    )
                )
                .collect(Collectors.toList());
            switch (sortMode) {
                case DISTANCE:
                    targets.sort(Comparator.comparingDouble(RotationUtil::distanceSqFromEyeToClosestOnAABB));
                    break;
                case HEALTH:
                    targets.sort(Comparator.comparingDouble(EntityLivingBase::func_110143_aJ));
                    break;
                case HURT_TIME:
                    targets.sort(Comparator.comparingInt(entity -> entity.field_70737_aN));
                    break;
                case CROSSHAIR:
                    targets.sort(Comparator.comparingDouble(RotationUtil::angleToEntity));
            }

            return targets;
        } else {
            return new ArrayList<>();
        }
    }

    public static boolean isValidTarget(
        EntityLivingBase entity,
        boolean targetPlayers,
        boolean targetMobs,
        boolean targetAnimals,
        boolean targetInvis,
        boolean ignoreTeammates,
        boolean ignoreFriends,
        double range
    ) {
        if (entity == null
            || entity == mc.field_71439_g
            || entity.field_70128_L
            || entity.func_110143_aJ() <= 0.0F
            || entity instanceof EntityArmorStand) {
            return false;
        }

        if (AntiBot.isBot(entity)) {
            return false;
        }

        if (RotationUtil.distanceSqFromEyeToClosestOnAABB(entity) > range * range) {
            return false;
        }

        if (entity.func_82150_aj() && !targetInvis) {
            return false;
        }

        if (entity instanceof EntityPlayer) {
            if (!targetPlayers) {
                return false;
            }

            if (entity instanceof EntityPlayer) {
                if (ignoreTeammates && TeamUtil.isSameTeam((EntityPlayer)entity)) {
                    return false;
                }

                if (ignoreFriends && TeamUtil.isFriend((EntityPlayer)entity)) {
                    return false;
                }
            }

            return true;
        } else if (entity instanceof EntityMob || entity instanceof EntitySlime) {
            return targetMobs;
        } else {
            return !(entity instanceof EntityAnimal)
                    && !(entity instanceof EntitySquid)
                    && !(entity instanceof EntityVillager)
                ? false
                : targetAnimals;
        }
    }

    public static EntityLivingBase getTarget(
        boolean targetPlayers,
        boolean targetMobs,
        boolean targetAnimals,
        boolean targetInvis,
        boolean ignoreTeammates,
        boolean ignoreFriends,
        double range,
        CombatTargeting.SortMode sortMode
    ) {
        List<EntityLivingBase> targets = getTargets(
            targetPlayers, targetMobs, targetAnimals, targetInvis, ignoreTeammates, ignoreFriends, range, sortMode
        );
        return targets.isEmpty() ? null : targets.get(0);
    }

    public static EntityPlayer findTarget(double maxDistanceSq) {
        return findTarget(maxDistanceSq, true);
    }

    public static EntityPlayer findTarget(double maxDistanceSq, boolean ignoreTeammates) {
        EntityPlayer mouseOverTarget = getMouseOverTarget(maxDistanceSq, ignoreTeammates);
        return mouseOverTarget != null ? mouseOverTarget : findClosestTarget(maxDistanceSq, ignoreTeammates);
    }

    public static EntityPlayer findClosestTarget(double maxDistanceSq) {
        return findClosestTarget(maxDistanceSq, true);
    }

    public static EntityPlayer findClosestTarget(double maxDistanceSq, boolean ignoreTeammates) {
        if (mc != null && mc.field_71441_e != null) {
            EntityPlayer closest = null;
            double closestDistanceSq = Double.MAX_VALUE;

            for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                if (isValidPlayer(player, maxDistanceSq, ignoreTeammates)) {
                    double distanceSq = RotationUtil.distanceSqFromEyeToClosestOnAABB(player);
                    if (distanceSq < closestDistanceSq) {
                        closestDistanceSq = distanceSq;
                        closest = player;
                    }
                }
            }

            return closest;
        } else {
            return null;
        }
    }

    public static EntityPlayer getMouseOverTarget(double maxDistanceSq) {
        return getMouseOverTarget(maxDistanceSq, true);
    }

    public static EntityPlayer getMouseOverTarget(double maxDistanceSq, boolean ignoreTeammates) {
        if (mc != null && mc.field_71476_x != null) {
            MovingObjectPosition objectMouseOver = mc.field_71476_x;
            return asValidPlayer(objectMouseOver.field_72308_g, maxDistanceSq, ignoreTeammates);
        } else {
            return null;
        }
    }

    public static EntityPlayer asValidPlayer(Entity entity, double maxDistanceSq) {
        return asValidPlayer(entity, maxDistanceSq, true);
    }

    public static EntityPlayer asValidPlayer(Entity entity, double maxDistanceSq, boolean ignoreTeammates) {
        if (!(entity instanceof EntityPlayer)) {
            return null;
        }

        EntityPlayer player = (EntityPlayer)entity;
        return isValidPlayer(player, maxDistanceSq, ignoreTeammates) ? player : null;
    }

    public static boolean isValidPlayer(EntityPlayer player, double maxDistanceSq) {
        return isValidPlayer(player, maxDistanceSq, true);
    }

    public static boolean isValidPlayer(EntityPlayer player, double maxDistanceSq, boolean ignoreTeammates) {
        return isTrackablePlayer(player, ignoreTeammates) && isWithinRange(player, maxDistanceSq);
    }

    public static boolean isTrackablePlayer(EntityPlayer player) {
        return isTrackablePlayer(player, true);
    }

    public static boolean isTrackablePlayer(EntityPlayer player, boolean ignoreTeammates) {
        if (player == null || player == mc.field_71439_g || player.field_70128_L || player.field_70725_aQ != 0) {
            return false;
        } else {
            return TeamUtil.isFriend(player) || AntiBot.isBot(player)
                ? false
                : !ignoreTeammates || !TeamUtil.isSameTeam(player);
        }
    }

    public static boolean isWithinRange(EntityPlayer player, double maxDistanceSq) {
        return player == null ? false : RotationUtil.distanceSqFromEyeToClosestOnAABB(player) <= maxDistanceSq;
    }

    public enum SortMode {
        DISTANCE,
        HEALTH,
        HURT_TIME,
        CROSSHAIR;
    }
}
