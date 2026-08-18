package miau.module.modules.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorEntity;
import miau.mixin.IAccessorMinecraft;
import miau.mixin.IAccessorS27PacketExplosion;
import miau.module.Module;
import miau.module.modules.combat.velocity.VelocityUtil;
import miau.module.modules.network.BackTrack;
import miau.notification.NotificationType;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.client.ChatUtil;
import miau.util.math.RandomUtil;
import miau.util.misc.BackTrackUtil;
import miau.util.misc.SomeUtil;
import miau.util.network.BlinkUtil;
import miau.util.player.RotationUtil;
import miau.util.player.SimulatedPlayer;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C12PacketUpdateSign;
import net.minecraft.network.play.client.C19PacketResourcePackStatus;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

public class TimerRange extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private int playerTicks = 0;
    private int smartTick = 0;
    private int cooldownTick = 0;
    private float randomRange = 0.0F;
    private boolean blinked = false;
    private boolean shouldReset = false;
    private boolean confirmTick = false;
    private boolean confirmStop = false;
    private boolean confirmAttack = false;
    public final ModeProperty timerBoostMode = new ModeProperty(
        "TimerMode", 2, new String[]{"Normal", "Smart", "Modern"}
    );
    public final IntProperty ticksValue = new IntProperty("Ticks", 10, 1, 20);
    public final FloatProperty timerBoostValue = new FloatProperty("TimerBoost", 1.5F, 0.01F, 35.0F);
    public final FloatProperty boostDelay = new FloatProperty("BoostDelay", 0.5F, 0.55F, 0.1F, 1.0F);
    public final FloatProperty timerChargedValue = new FloatProperty("TimerCharged", 0.45F, 0.05F, 5.0F);
    public final FloatProperty chargedDelay = new FloatProperty("ChargedDelay", 0.75F, 0.9F, 0.1F, 1.0F);
    public final FloatProperty rangeValue = new FloatProperty(
        "Range", 3.5F, 1.0F, 5.0F, () -> this.timerBoostMode.getModeString().equals("Normal")
    );
    public final IntProperty cooldownTickValue = new IntProperty(
        "CooldownTick", 10, 1, 50, () -> this.timerBoostMode.getModeString().equals("Normal")
    );
    public final FloatProperty range = new FloatProperty(
        "Range", 2.5F, 3.0F, 2.0F, 8.0F, () -> !this.timerBoostMode.getModeString().equals("Normal")
    );
    public final FloatProperty scanRange = new FloatProperty(
        "ScanRange", 8.0F, 2.0F, 12.0F, () -> !this.timerBoostMode.getModeString().equals("Normal")
    );
    public final FloatProperty tickDelay = new FloatProperty(
        "TickDelay", 30.0F, 60.0F, 1.0F, 200.0F, () -> !this.timerBoostMode.getModeString().equals("Normal")
    );
    public final BooleanProperty blink = new BooleanProperty("Blink", false);
    public final IntProperty predictClientMovement = new IntProperty("PredictClientMovement", 2, 0, 5);
    public final FloatProperty predictEnemyPosition = new FloatProperty("PredictEnemyPosition", 1.5F, -1.0F, 2.0F);
    public final FloatProperty maxAngleDifference = new FloatProperty(
        "MaxAngleDifference", 5.0F, 5.0F, 90.0F, () -> this.timerBoostMode.getModeString().equals("Modern")
    );
    public final ModeProperty markMode = new ModeProperty(
        "Mark", 0, new String[]{"Off", "Box", "Platform"}, () -> this.timerBoostMode.getModeString().equals("Modern")
    );
    public final BooleanProperty outline = new BooleanProperty(
        "Outline",
        false,
        () -> this.timerBoostMode.getModeString().equals("Modern") && this.markMode.getModeString().equals("Box")
    );
    public final BooleanProperty onWeb = new BooleanProperty("OnWeb", false);
    public final BooleanProperty onLiquid = new BooleanProperty("onLiquid", false);
    public final BooleanProperty onForwardOnly = new BooleanProperty("OnForwardOnly", true);
    public final BooleanProperty resetOnlagBack = new BooleanProperty("ResetOnLagback", false);
    public final BooleanProperty resetOnKnockback = new BooleanProperty("ResetOnKnockback", false);
    public final BooleanProperty chatDebug = new BooleanProperty(
        "ChatDebug", true, () -> this.resetOnlagBack.getValue() || this.resetOnKnockback.getValue()
    );
    public final BooleanProperty notificationDebug = new BooleanProperty(
        "NotificationDebug", false, () -> this.resetOnlagBack.getValue() || this.resetOnKnockback.getValue()
    );

    public TimerRange() {
        super("TimerRange", false);
    }

    @Override
    public void onDisabled() {
        this.shouldResetTimer();
        BlinkUtil.unblink();
        this.smartTick = 0;
        this.cooldownTick = 0;
        this.playerTicks = 0;
        this.shouldReset = false;
        this.blinked = false;
        this.confirmTick = false;
        this.confirmStop = false;
        this.confirmAttack = false;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        EntityPlayerSP player = mc.field_71439_g;
        if (player != null) {
            Entity targetEntity = event.getTarget();
            if (!(targetEntity instanceof EntityLivingBase) && this.playerTicks >= 1) {
                this.shouldResetTimer();
            } else {
                this.confirmAttack = true;
                if (targetEntity != null) {
                    double entityDistance = BackTrackUtil.getDistanceToEntityBox(targetEntity);
                    int randomTickDelay = randomInt(
                        this.tickDelay.getValue().intValue(), this.tickDelay.getSecondValue().intValue()
                    );
                    boolean shouldReturn = BackTrack.runWithNearestTrackedDistance(
                        targetEntity, () -> !this.updateDistance(targetEntity)
                    );
                    if (!shouldReturn
                        && (!isInWeb(player) || this.onWeb.getValue())
                        && (!isInLiquid(player) || this.onLiquid.getValue())) {
                        this.smartTick++;
                        this.cooldownTick++;
                        String mode = this.timerBoostMode.getModeString();
                        boolean shouldSlowed;
                        if (mode.equals("Normal")) {
                            shouldSlowed = this.cooldownTick >= this.cooldownTickValue.getValue()
                                && entityDistance <= this.rangeValue.getValue().floatValue();
                        } else if (mode.equals("Smart")) {
                            shouldSlowed = this.smartTick >= randomTickDelay && entityDistance <= this.randomRange;
                        } else {
                            shouldSlowed = false;
                        }

                        if (!shouldSlowed || !this.confirmAttack) {
                            this.shouldResetTimer();
                        } else if (this.updateDistance(targetEntity)) {
                            this.confirmAttack = false;
                            this.playerTicks = this.ticksValue.getValue();
                            this.cooldownTick = 0;
                            this.smartTick = 0;
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayerSP player = mc.field_71439_g;
            if (player != null) {
                if (this.timerBoostMode.getModeString().equals("Modern")) {
                    this.handleModernMove(player);
                }

                this.handleTimerUpdate();
            }
        }
    }

    @EventTarget
    public void onPostUpdate(UpdateEvent event) {
        if (event.getType() == EventType.POST) {
            if (this.blink.getValue()) {
                BlinkUtil.syncSent();
            }
        }
    }

    @EventTarget
    public void onWorld(LoadWorldEvent event) {
        if (this.blink.getValue()) {
            BlinkUtil.clear();
        }
    }

    private void handleModernMove(EntityPlayerSP player) {
        Entity nearbyEntity = this.getNearestEntityInRange();
        if (nearbyEntity != null) {
            int randomTickDelay = randomInt(
                this.tickDelay.getValue().intValue(), this.tickDelay.getSecondValue().intValue()
            );
            boolean shouldReturn = BackTrack.runWithNearestTrackedDistance(
                nearbyEntity, () -> !this.updateDistance(nearbyEntity)
            );
            if (!shouldReturn
                && (!isInWeb(player) || this.onWeb.getValue())
                && (!isInLiquid(player) || this.onLiquid.getValue())) {
                if (this.isPlayerMoving()) {
                    this.smartTick++;
                    if (this.smartTick >= randomTickDelay) {
                        this.confirmTick = true;
                        this.smartTick = 0;
                    }
                } else {
                    this.smartTick = 0;
                }

                if (!this.isPlayerMoving() || this.confirmStop) {
                    this.shouldResetTimer();
                } else if (VelocityUtil.isLookingOnEntities(
                    nearbyEntity, this.maxAngleDifference.getValue().floatValue()
                )) {
                    double entityDistance = BackTrackUtil.getDistanceToEntityBox(nearbyEntity);
                    if (this.confirmTick
                        && entityDistance >= this.randomRange
                        && entityDistance <= this.range.getSecondValue().floatValue()
                        && this.updateDistance(nearbyEntity)) {
                        this.playerTicks = this.ticksValue.getValue();
                        this.confirmTick = false;
                    }
                } else {
                    this.shouldResetTimer();
                }
            }
        }
    }

    private void handleTimerUpdate() {
        float timerBoost = randomFloat(this.boostDelay.getValue(), this.boostDelay.getSecondValue());
        float charged = randomFloat(this.chargedDelay.getValue(), this.chargedDelay.getSecondValue());
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            this.randomRange = randomFloat(this.range.getValue(), this.range.getSecondValue());
        }

        if (this.playerTicks > 0 && !this.confirmStop) {
            double tickProgress = (double)this.playerTicks / this.ticksValue.getValue().intValue();
            float playerSpeed;
            if (tickProgress < timerBoost) {
                playerSpeed = this.timerBoostValue.getValue();
            } else if (tickProgress < charged) {
                playerSpeed = this.timerChargedValue.getValue();
            } else {
                playerSpeed = 1.0F;
            }

            float speedAdjustment = playerSpeed >= 0.0F
                ? playerSpeed
                : 1.0F + this.ticksValue.getValue().intValue() - this.playerTicks;
            float adjustedTimerSpeed = Math.max(speedAdjustment, 0.0F);
            ((IAccessorMinecraft)mc).getTimer().field_74278_d = adjustedTimerSpeed;
            this.playerTicks--;
        } else {
            this.shouldResetTimer();
            if (this.blink.getValue() && this.blinked) {
                BlinkUtil.unblink();
                this.blinked = false;
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        EntityPlayerSP player = mc.field_71439_g;
        if (player != null) {
            if (this.timerBoostMode.getModeString().equals("Modern")) {
                Entity nearbyEntity = this.getNearestEntityInRange();
                if (nearbyEntity != null) {
                    double entityDistance = BackTrackUtil.getDistanceToEntityBox(nearbyEntity);
                    if (!(entityDistance > this.getScanRange())) {
                        Color color = VelocityUtil.isLookingOnEntities(
                                nearbyEntity, this.maxAngleDifference.getValue().floatValue()
                            )
                            ? new Color(37, 126, 255, 70)
                            : new Color(210, 60, 60, 70);
                        String mark = this.markMode.getModeString();
                        if (!mark.equals("Off")) {
                            if (mark.equals("Box")) {
                                this.drawBoxMark(nearbyEntity, color, this.outline.getValue());
                            } else if (mark.equals("Platform")) {
                                this.drawPlatformMark(nearbyEntity, color);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isPlayerMoving() {
        EntityPlayerSP player = mc.field_71439_g;
        if (player == null) {
            return false;
        } else {
            return !this.onForwardOnly.getValue()
                ? player.field_70701_bs != 0.0F || player.field_70702_br != 0.0F
                : player.field_70701_bs != 0.0F && player.field_70702_br == 0.0F;
        }
    }

    private Entity getNearestEntityInRange() {
        EntityPlayerSP player = mc.field_71439_g;
        if (player == null) {
            return null;
        }

        Entity best = null;
        double bestDist = Double.MAX_VALUE;

        for (EntityLivingBase entity : this.getTargets()) {
            double dist = BackTrackUtil.getDistanceToEntityBox(entity);
            if (dist < bestDist) {
                bestDist = dist;
                best = entity;
            }
        }

        return best;
    }

    private List<EntityLivingBase> getTargets() {
        List<EntityLivingBase> targets = new ArrayList<>();
        if (mc.field_71441_e != null && mc.field_71439_g != null) {
            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityLivingBase) {
                    EntityLivingBase living = (EntityLivingBase)entity;
                    if (SomeUtil.isSelected(living)) {
                        boolean inRange = BackTrack.runWithNearestTrackedDistance(
                            living,
                            () -> {
                                double dist = BackTrackUtil.getDistanceToEntityBox(living);
                                String mode = this.timerBoostMode.getModeString();
                                if (mode.equals("Normal")) {
                                    return dist <= this.rangeValue.getValue().floatValue();
                                } else {
                                    return !mode.equals("Smart") && !mode.equals("Modern")
                                        ? false
                                        : dist <= this.getScanRange() + this.randomRange;
                                }
                            }
                        );
                        if (inRange) {
                            targets.add(living);
                        }
                    }
                }
            }

            return targets;
        } else {
            return targets;
        }
    }

    private boolean updateDistance(Entity entity) {
        EntityPlayerSP player = mc.field_71439_g;
        if (player != null && entity != null) {
            Vec3 prediction = this.predictEntityPosition(entity);
            AxisAlignedBB boundingBox = entity.func_174813_aQ()
                .func_72317_d(prediction.field_72450_a, prediction.field_72448_b, prediction.field_72449_c);
            Vec3 currPos = new Vec3(player.field_70165_t, player.field_70163_u, player.field_70161_v);
            Vec3 oldPos = new Vec3(player.field_70169_q, player.field_70167_r, player.field_70166_s);
            SimulatedPlayer simPlayer = SimulatedPlayer.fromClientPlayer(player.field_71158_b);

            for (int i = 0; i < this.predictClientMovement.getValue() + 1; i++) {
                simPlayer.tick();
            }

            BackTrackUtil.setPositionAndPrevious(player, simPlayer.getPos());
            AxisAlignedBB originalBox = entity.func_174813_aQ();

            boolean result;
            try {
                entity.func_174826_a(boundingBox);
                String mode = this.timerBoostMode.getModeString();
                float lookRange;
                if (mode.equals("Normal")) {
                    lookRange = this.rangeValue.getValue();
                } else {
                    lookRange = this.randomRange;
                }

                Reach reach = (Reach)Miau.moduleManager.modules.get(Reach.class);
                float attackRange = reach != null && reach.isEnabled() ? reach.range.getValue() : 3.0F;
                result = RotationUtil.hasValidAimPoint(
                    entity, 50.0, 50.0, Math.max(lookRange, attackRange), false, false
                );
            } finally {
                entity.func_174826_a(originalBox);
                BackTrackUtil.setPositionAndPrevious(player, currPos, oldPos);
            }

            return result;
        } else {
            return false;
        }
    }

    private Vec3 predictEntityPosition(Entity entity) {
        double multiplier = 2.0F + this.predictEnemyPosition.getValue();
        return new Vec3(
            (entity.field_70165_t - entity.field_70169_q) * multiplier,
            (entity.field_70163_u - entity.field_70167_r) * multiplier,
            (entity.field_70161_v - entity.field_70166_s) * multiplier
        );
    }

    private void shouldResetTimer() {
        Entity nearestEntity = this.getNearestEntityInRange();
        if (nearestEntity != null && !nearestEntity.field_70128_L) {
            if (!this.shouldReset && ((IAccessorMinecraft)mc).getTimer().field_74278_d != 1.0F) {
                ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
                this.shouldReset = true;
            } else {
                this.shouldReset = false;
            }
        } else if (!this.shouldReset) {
            ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
            this.shouldReset = true;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.field_71439_g != null && !mc.field_71439_g.field_70128_L) {
            Packet<?> packet = event.getPacket();
            if (this.blink.getValue()) {
                if (this.playerTicks > 0 && !this.blinked) {
                    BlinkUtil.blink(event, false, true);
                    this.blinked = true;
                }

                if (this.blinked) {
                    if (packet instanceof S08PacketPlayerPosLook
                        || packet instanceof C07PacketPlayerDigging
                        || packet instanceof C12PacketUpdateSign
                        || packet instanceof C19PacketResourcePackStatus) {
                        BlinkUtil.unblink();
                        return;
                    }

                    if (packet instanceof S27PacketExplosion) {
                        S27PacketExplosion explosion = (S27PacketExplosion)packet;
                        IAccessorS27PacketExplosion accessor = (IAccessorS27PacketExplosion)explosion;
                        if (accessor.getMotionX() != 0.0F
                            || accessor.getMotionY() != 0.0F
                            || accessor.getMotionZ() != 0.0F) {
                            BlinkUtil.unblink();
                            return;
                        }
                    }

                    if (packet instanceof S06PacketUpdateHealth) {
                        S06PacketUpdateHealth health = (S06PacketUpdateHealth)packet;
                        if (health.func_149332_c() < mc.field_71439_g.func_110143_aJ()) {
                            BlinkUtil.unblink();
                            return;
                        }
                    }
                }
            }

            if (this.resetOnlagBack.getValue() && packet instanceof S08PacketPlayerPosLook) {
                this.shouldResetTimer();
                if (this.shouldReset) {
                    if (this.chatDebug.getValue()) {
                        ChatUtil.display("%s", "Lagback Received | Timer Reset");
                    }

                    if (this.notificationDebug.getValue()) {
                        Miau.notificationManager
                            .builder(NotificationType.INFO)
                            .duration(1000)
                            .title(this.getName())
                            .description("Lagback Received - Resetting Timer")
                            .buildAndPublish();
                    }

                    this.shouldReset = false;
                }
            }

            if (this.resetOnKnockback.getValue()
                && packet instanceof S12PacketEntityVelocity
                && mc.field_71439_g.func_145782_y() == ((S12PacketEntityVelocity)packet).func_149412_c()) {
                this.shouldResetTimer();
                if (this.shouldReset) {
                    if (this.chatDebug.getValue()) {
                        ChatUtil.display("%s", "Knockback Received | Timer Reset");
                    }

                    if (this.notificationDebug.getValue()) {
                        Miau.notificationManager
                            .builder(NotificationType.INFO)
                            .duration(1000)
                            .title(this.getName())
                            .description("Knockback Received - Resetting Timer")
                            .buildAndPublish();
                    }

                    this.shouldReset = false;
                }
            }
        }
    }

    private float getScanRange() {
        return Math.max(this.scanRange.getValue(), this.range.getSecondValue());
    }

    private void drawBoxMark(Entity entity, Color color, boolean outline) {
        RenderUtil.drawEntityBox(entity, color.getRed(), color.getGreen(), color.getBlue());
        if (outline) {
            RenderUtil.drawEntityBoundingBox(
                entity, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 2.0F, 0.1
            );
        }
    }

    private void drawPlatformMark(Entity entity, Color color) {
        AxisAlignedBB box = entity.func_174813_aQ();
        double vx = mc.func_175598_ae().field_78730_l;
        double vy = mc.func_175598_ae().field_78731_m;
        double vz = mc.func_175598_ae().field_78728_n;
        AxisAlignedBB renderBox = box.func_72317_d(-vx, -vy, -vz);
        AxisAlignedBB platform = new AxisAlignedBB(
            renderBox.field_72340_a,
            renderBox.field_72337_e + 0.2,
            renderBox.field_72339_c,
            renderBox.field_72336_d,
            renderBox.field_72337_e + 0.26,
            renderBox.field_72334_f
        );
        RenderUtil.drawFilledBox(platform, color.getRed(), color.getGreen(), color.getBlue());
    }

    private static boolean isInWeb(EntityPlayerSP player) {
        return ((IAccessorEntity)player).getIsInWeb();
    }

    private static boolean isInLiquid(EntityPlayerSP player) {
        return player.func_70090_H() || player.func_180799_ab();
    }

    private static int randomInt(int min, int max) {
        return max <= min ? min : RandomUtil.nextInt(min, max);
    }

    private static float randomFloat(float min, float max) {
        return max <= min ? min : RandomUtil.nextFloat(min, max);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.timerBoostMode.getModeString()};
    }
}
