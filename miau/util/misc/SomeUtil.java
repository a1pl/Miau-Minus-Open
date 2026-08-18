package miau.util.misc;

import java.util.function.BooleanSupplier;
import miau.Miau;
import miau.event.impl.PacketEvent;
import miau.mixin.IAccessorEntity;
import miau.mixin.IAccessorMinecraft;
import miau.module.modules.combat.KillAura;
import miau.module.modules.combat.NewVelocity;
import miau.module.modules.movement.KeepSprint;
import miau.util.client.ChatUtil;
import miau.util.network.PacketUtil;
import miau.util.player.CombatTargeting;
import miau.util.player.PlayerUtil;
import miau.util.player.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public final class SomeUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();

    private SomeUtil() {
    }

    public static double roundToPlacesIfNeeded(double value) {
        return roundToPlacesIfNeeded(value, 5);
    }

    public static double roundToPlacesIfNeeded(double value, int places) {
        int scale = Math.max(0, Math.min(places, 15));
        if (!Double.isNaN(value) && !Double.isInfinite(value)) {
            if (Math.abs(value) < 1.0E-14) {
                return value;
            }

            if (Math.abs(value - 1.0) < 1.0E-14) {
                return value;
            }

            if (isAlreadyRounded(value, scale)) {
                return value;
            }

            double factor = Math.pow(10.0, scale);
            double scaled = value * factor;
            return Double.isFinite(scaled) ? Math.round(scaled) / factor : value;
        } else {
            return value;
        }
    }

    private static boolean isAlreadyRounded(double value, int scale) {
        if (!Double.isNaN(value) && !Double.isInfinite(value)) {
            double factor = Math.pow(10.0, scale);
            double scaled = value * factor;
            if (!Double.isFinite(scaled)) {
                return true;
            }

            double rounded = Math.round(scaled);
            return Math.abs(scaled - rounded) < 1.0E-8;
        } else {
            return true;
        }
    }

    public static void reduceXZ(double factor) {
        reduceXZ(factor, null, null, null);
    }

    public static void reduceXZ(double factor, Integer hurtTimeMin, Integer hurtTimeMax) {
        reduceXZ(factor, hurtTimeMin, hurtTimeMax, null);
    }

    public static void reduceXZ(double factor, Integer hurtTimeMin, Integer hurtTimeMax, Integer setScale) {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            if (hurtTimeMin == null
                || player.field_70737_aN >= hurtTimeMin
                    && (hurtTimeMax == null || player.field_70737_aN <= hurtTimeMax)) {
                double adjustedFactor = roundToPlacesIfNeeded(factor, setScale == null ? 5 : setScale);
                player.field_70159_w *= adjustedFactor;
                player.field_70179_y *= adjustedFactor;
            }
        }
    }

    public static void reduceY(double factor) {
        reduceY(factor, null, null, null);
    }

    public static void reduceY(double factor, Integer hurtTimeMin, Integer hurtTimeMax) {
        reduceY(factor, hurtTimeMin, hurtTimeMax, null);
    }

    public static void reduceY(double factor, Integer hurtTimeMin, Integer hurtTimeMax, Integer setScale) {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            if (hurtTimeMin == null
                || player.field_70737_aN >= hurtTimeMin
                    && (hurtTimeMax == null || player.field_70737_aN <= hurtTimeMax)) {
                double adjustedFactor = roundToPlacesIfNeeded(factor, setScale == null ? 5 : setScale);
                player.field_70181_x *= adjustedFactor;
            }
        }
    }

    public static void setMotion(Double xMotion, Double yMotion, Double zMotion) {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            if (xMotion != null) {
                player.field_70159_w = xMotion;
            }

            if (yMotion != null) {
                player.field_70181_x = yMotion;
            }

            if (zMotion != null) {
                player.field_70179_y = zMotion;
            }
        }
    }

    public static void changeSprint(boolean setState, boolean sendPacketToServer) {
        changeSprint(setState, sendPacketToServer, false);
    }

    public static void changeSprint(boolean setState, boolean sendPacketToServer, boolean forceChange) {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            if (forceChange) {
                if (sendPacketToServer) {
                    if (player.func_70051_ag() != setState) {
                        player.func_70031_b(setState);
                    }

                    PacketUtil.sendPacket(
                        new C0BPacketEntityAction(player, setState ? Action.START_SPRINTING : Action.STOP_SPRINTING)
                    );
                } else if (player.func_70051_ag() != setState) {
                    player.func_70031_b(setState);
                }
            } else {
                player.func_70031_b(setState);
                if (sendPacketToServer) {
                    PacketUtil.sendPacket(
                        new C0BPacketEntityAction(player, setState ? Action.START_SPRINTING : Action.STOP_SPRINTING)
                    );
                }
            }
        }
    }

    public static void changeTimer(float speed) {
        ((IAccessorMinecraft)mc).getTimer().field_74278_d = speed;
    }

    public static boolean keepingSprint() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        KeepSprint keepSprint = (KeepSprint)Miau.moduleManager.modules.get(KeepSprint.class);
        if (killAura != null && killAura.isEnabled()) {
            return true;
        } else if (NewVelocity.canCancelHitSlow) {
            return true;
        } else if (keepSprint != null && keepSprint.isEnabled()) {
            return true;
        } else if (mc.field_71439_g.field_70737_aN == 0) {
            return false;
        } else if (isInBadEnvironment()) {
            return false;
        } else if (mc.field_71439_g.func_70644_a(Potion.field_76421_d)) {
            return false;
        } else if (mc.field_71439_g.func_70644_a(Potion.field_76424_c)) {
            return false;
        } else {
            return mc.field_71439_g.func_70051_ag() ? true : true;
        }
    }

    public static boolean runAttack() {
        return runAttack(false, 3.0F, 1, null, true, "Packet", false, false, "Attacked", false, null, null, 1.0F);
    }

    public static boolean runAttack(boolean keepSprint) {
        return runAttack(keepSprint, 3.0F, 1, null, true, "Packet", false, false, "Attacked", false, null, null, 1.0F);
    }

    public static boolean runAttack(boolean keepSprint, boolean fakeSwing, int attackCount, boolean silentAttack) {
        return runAttack(
            keepSprint,
            3.0F,
            attackCount,
            null,
            true,
            "Packet",
            fakeSwing,
            false,
            "Attacked",
            silentAttack,
            null,
            null,
            1.0F
        );
    }

    public static boolean runAttack(
        boolean keepSprint,
        float maxDistance,
        int attackCount,
        Entity attackTarget,
        boolean ignoreBlocking,
        String swingMode,
        boolean fakeSwing,
        boolean debugMessage,
        String debugMessageString,
        boolean silentAttack,
        Double extraReduceXZ,
        Double extraReduceY,
        float attackChance
    ) {
        int trulyAttack = 0;
        boolean shouldSwingNormal = "Normal".equals(swingMode);
        boolean shouldSwingPacket = "Packet".equals(swingMode) || swingMode == null;
        boolean shouldSwingOff = "Off".equals(swingMode);
        Runnable swingAction = () -> {
            if (shouldSwingNormal) {
                if (mc.field_71439_g != null) {
                    mc.field_71439_g.func_71038_i();
                }
            } else if (shouldSwingPacket) {
                PacketUtil.sendPacket(new C0APacketAnimation());
            }
        };
        Entity target;
        if (attackTarget != null) {
            target = attackTarget;
        } else if (mc.field_71476_x != null) {
            target = mc.field_71476_x.field_72308_g;
        } else {
            target = null;
        }

        if (target == null) {
            if (fakeSwing) {
                swingAction.run();
            }

            return false;
        } else {
            double distance = RotationUtil.distanceFromEyeToClosestOnAABB(target);
            boolean withinRange = distance < maxDistance;
            boolean playerIsBlocking = mc.field_71439_g.func_70632_aY();
            if (playerIsBlocking && !ignoreBlocking) {
                return false;
            }

            boolean attackPerformed = false;
            if (withinRange) {
                for (int i = 0; i < attackCount; i++) {
                    if (!(Math.random() > attackChance)) {
                        if (silentAttack) {
                            SilentAttackManager.withSilentAttack(() -> {
                                attackEntityWithModifiedSprint(target, !keepSprint, swingAction);
                                if (extraReduceXZ != null) {
                                    reduceXZ(extraReduceXZ);
                                }

                                if (extraReduceY != null) {
                                    reduceY(extraReduceY);
                                }
                            });
                        } else {
                            attackEntityWithModifiedSprint(target, !keepSprint, swingAction);
                            if (extraReduceXZ != null) {
                                reduceXZ(extraReduceXZ);
                            }

                            if (extraReduceY != null) {
                                reduceY(extraReduceY);
                            }
                        }

                        CPSCounter.registerClick(CPSCounter.MouseButton.LEFT);
                        attackPerformed = true;
                        trulyAttack++;
                    }
                }

                if (debugMessage) {
                    ChatUtil.display("%s x%s", debugMessageString, trulyAttack);
                }

                return attackPerformed;
            } else {
                if (fakeSwing) {
                    swingAction.run();
                }

                return false;
            }
        }
    }

    private static void attackEntityWithModifiedSprint(Entity target, boolean cancelHitSlow, Runnable swingAction) {
        boolean wasSprinting = mc.field_71439_g.func_70051_ag();
        if (!cancelHitSlow && !keepingSprint()) {
            mc.field_71439_g.func_70031_b(false);
        } else {
            mc.field_71439_g.func_70031_b(true);
        }

        PlayerUtil.attackEntity(target);
        swingAction.run();
        if (wasSprinting) {
            mc.field_71439_g.func_70031_b(true);
        }
    }

    public static boolean isHurting() {
        return mc.field_71439_g != null && mc.field_71439_g.field_70737_aN > 0;
    }

    public static boolean isHurting(Boolean checkPacket, PacketEvent event) {
        if (mc.field_71439_g == null) {
            return false;
        } else {
            return Boolean.TRUE.equals(checkPacket) && event != null
                ? event.getPacket() instanceof S12PacketEntityVelocity
                    && ((S12PacketEntityVelocity)event.getPacket()).func_149412_c() == mc.field_71439_g.func_145782_y()
                    && mc.field_71439_g.field_70737_aN > 0
                : mc.field_71439_g.field_70737_aN > 0;
        }
    }

    public static boolean isFalling() {
        return mc.field_71439_g != null && !mc.field_71439_g.field_70122_E && mc.field_71439_g.field_70181_x < 0.0;
    }

    public static boolean isInBadEnvironment() {
        return ((IAccessorEntity)mc.field_71439_g).getIsInWeb()
            || mc.field_71439_g.func_180799_ab()
            || mc.field_71439_g.func_70027_ad()
            || mc.field_71439_g.func_70090_H()
            || mc.field_71439_g.func_70115_ae();
    }

    public static double bps() {
        return Math.sqrt(
                mc.field_71439_g.field_70159_w * mc.field_71439_g.field_70159_w
                    + mc.field_71439_g.field_70179_y * mc.field_71439_g.field_70179_y
            )
            * 20.0;
    }

    public static double bpt() {
        return Math.hypot(mc.field_71439_g.field_70159_w, mc.field_71439_g.field_70179_y);
    }

    public static double velocityX() {
        return mc.field_71439_g.field_70159_w;
    }

    public static double velocityY() {
        return mc.field_71439_g.field_70181_x;
    }

    public static double velocityZ() {
        return mc.field_71439_g.field_70179_y;
    }

    public static void setBPSTo(double targetBPS) {
        if (bps() != 0.0) {
            reduceXZ(targetBPS / bps());
        }
    }

    public static double getCurrentWeaponDamage(boolean isCritical) {
        EntityPlayer player = mc.field_71439_g;
        if (player == null) {
            return 1.0;
        }

        ItemStack heldItem = player.func_70694_bm();
        if (heldItem == null) {
            return 1.0;
        }

        double attackBonus = getAttackDamage(heldItem);
        double damage = isCritical ? 1.5 + attackBonus * 1.5 : 1.0 + attackBonus;
        PotionEffect strengthEffect = player.func_70660_b(Potion.field_76420_g);
        if (strengthEffect != null) {
            int amplifier = strengthEffect.func_76458_c();
            double strengthMultiplier = 1.0 + (amplifier + 1) * 1.3;
            damage *= strengthMultiplier;
        }

        PotionEffect weaknessEffect = player.func_70660_b(Potion.field_76437_t);
        if (weaknessEffect != null) {
            damage -= (weaknessEffect.func_76458_c() + 1) * 0.5;
        }

        return Math.max(0.5, damage);
    }

    private static double getAttackDamage(ItemStack itemStack) {
        if (itemStack == null || itemStack.func_77973_b() == null) {
            return 0.0;
        } else {
            return itemStack.func_77973_b() instanceof ItemSword
                ? 4.0 + ((ItemSword)itemStack.func_77973_b()).func_150931_i()
                : 1.0;
        }
    }

    public static void safeJump(double jumpStrength) {
        if (mc.field_71439_g != null) {
            if (mc.field_71439_g.field_70122_E && !mc.field_71474_y.field_74314_A.func_151470_d()) {
                mc.field_71439_g.field_70747_aH = (float)jumpStrength;
                mc.field_71439_g.func_70664_aZ();
                mc.field_71439_g.field_70747_aH = 0.2F;
            }
        }
    }

    public static double calculateAngleDifference() {
        return calculateAngleDifference(mc.field_71439_g);
    }

    public static double calculateAngleDifference(EntityLivingBase entity) {
        if (entity == null) {
            return 0.0;
        }

        double motionX = entity.field_70159_w;
        double motionZ = entity.field_70179_y;
        float playerYaw = entity.field_70177_z;
        double movementAngle = Math.toDegrees(Math.atan2(motionZ, motionX));
        double normalizedPlayerYaw = (playerYaw % 360.0F + 360.0F) % 360.0F;
        double angleDifference = Math.abs(movementAngle - normalizedPlayerYaw);
        if (angleDifference > 180.0) {
            angleDifference = 360.0 - angleDifference;
        }

        return angleDifference;
    }

    public static boolean isSelected(Entity entity) {
        if (entity == null || entity == mc.field_71439_g || entity.field_70128_L) {
            return false;
        } else if (!(entity instanceof EntityLivingBase)) {
            return false;
        } else {
            return entity instanceof EntityPlayer
                ? CombatTargeting.isTrackablePlayer((EntityPlayer)entity)
                : ((EntityLivingBase)entity).func_110143_aJ() > 0.0F;
        }
    }

    public static BooleanSupplier notNull() {
        return () -> true;
    }
}
