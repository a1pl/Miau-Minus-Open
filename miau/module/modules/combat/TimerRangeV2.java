package miau.module.modules.combat;

import java.awt.Color;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.UpdateEvent;
import miau.mixin.IAccessorMinecraft;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ColorProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.TextProperty;
import miau.util.client.ChatUtil;
import miau.util.misc.BackTrackUtil;
import miau.util.misc.CPSCounter;
import miau.util.misc.SomeUtil;
import miau.util.network.BlinkUtil;
import miau.util.render.RenderUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

public class TimerRangeV2 extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final ModeProperty tagMode = new ModeProperty(
        "TagMode", 0, new String[]{"WorkRange", "IsWorking", "MinSpeed-MaxSpeed", "Custom"}
    );
    private final TextProperty customText = new TextProperty(
        "CustomTagText", "", () -> this.tagMode.getModeString().equals("Custom")
    );
    private final ModeProperty workMode = new ModeProperty("Mode", 1, new String[]{"SlowFirst", "BoostFirst"});
    private final FloatProperty maxRange = new FloatProperty("MaxRange", 4.0F, 0.0F, 8.0F);
    private final FloatProperty minRange = new FloatProperty("MinRange", 3.0F, 0.0F, 8.0F);
    private final FloatProperty boostTimer = new FloatProperty("BoostTimerSpeed", 2.0F, 1.0F, 10.0F);
    private final IntProperty boostTime = new IntProperty("BoostTime", 100, 0, 3000);
    private final FloatProperty slowTimer = new FloatProperty("SlowTimerSpeed", 0.5F, 0.01F, 1.0F);
    private final IntProperty slowTime = new IntProperty("SlowTime", 100, 0, 3000);
    private final IntProperty cooldownTime = new IntProperty("CooldownTime", 100, 0, 3000);
    private final BooleanProperty attackWhenBoosting = new BooleanProperty("AttackWhenChangingTimer", false);
    private final BooleanProperty attackOnBoosting = new BooleanProperty(
        "AttackOnBoosting", false, () -> this.attackWhenBoosting.getValue()
    );
    private final BooleanProperty attackOnSlowing = new BooleanProperty(
        "AttackOnSlowing", true, () -> this.attackWhenBoosting.getValue()
    );
    private final IntProperty attackCount = new IntProperty(
        "TotalAttackCount", 1, 1, 5, () -> this.attackWhenBoosting.getValue()
    );
    private final BooleanProperty onlyAttackWhenNotReachedCPSLimit = new BooleanProperty(
        "OnlyAttackWhenNotReachedCPSLimit", false, () -> this.attackWhenBoosting.getValue()
    );
    private final IntProperty CPSLimit = new IntProperty(
        "CPSLimit",
        20,
        1,
        100,
        () -> this.attackWhenBoosting.getValue() && this.onlyAttackWhenNotReachedCPSLimit.getValue()
    );
    private final FloatProperty attackMaxRange = new FloatProperty(
        "AttackMaxRange", 3.0F, 0.0F, 8.0F, () -> this.attackWhenBoosting.getValue()
    );
    private final ModeProperty swingMode = new ModeProperty(
        "AttackSwingMode", 0, new String[]{"Normal", "Packet"}, () -> this.attackWhenBoosting.getValue()
    );
    private final BooleanProperty keepSprint = new BooleanProperty(
        "KeepSprint", false, () -> this.attackWhenBoosting.getValue()
    );
    private final FloatProperty allowKeepSprintHurtTime = new FloatProperty(
        "AllowKeepSprintHurtTime",
        0.0F,
        10.0F,
        0.0F,
        10.0F,
        () -> this.attackWhenBoosting.getValue() && this.keepSprint.getValue()
    );
    private final BooleanProperty boostMotion = new BooleanProperty("BoostMotion", false);
    private final BooleanProperty boostOnBoosting = new BooleanProperty(
        "BoostOnBoosting", false, () -> this.boostMotion.getValue()
    );
    private final BooleanProperty boostOnSlowing = new BooleanProperty(
        "BoostOnSlowing", true, () -> this.boostMotion.getValue()
    );
    private final FloatProperty boostBoostingFactor = new FloatProperty(
        "BoostFactorBoosting", 0.0F, 0.0F, 2.0F, () -> this.boostOnBoosting.getValue() && this.boostMotion.getValue()
    );
    private final FloatProperty boostSlowingFactor = new FloatProperty(
        "BoostFactorSlowing", 0.0F, 0.0F, 2.0F, () -> this.boostOnSlowing.getValue() && this.boostMotion.getValue()
    );
    private final BooleanProperty stopBoostingWhenHurting = new BooleanProperty("StopBoostingWhenHurt", false);
    private final BooleanProperty blinkOnWorking = new BooleanProperty("BlinkOnBoosting", false);
    private final BooleanProperty cancelC03 = new BooleanProperty(
        "CancelC03WhenWorking", false, () -> this.blinkOnWorking.getValue()
    );
    private final BooleanProperty onlyForward = new BooleanProperty("OnlyForward", false);
    private final BooleanProperty debugMessage = new BooleanProperty("DebugMessage", false);
    private final BooleanProperty safe = new BooleanProperty("Safe", false);
    private final BooleanProperty visualPrediction = new BooleanProperty("VisualPrediction", false);
    private final BooleanProperty predictionBox = new BooleanProperty(
        "PredictionBox", true, () -> this.visualPrediction.getValue()
    );
    private final ColorProperty predictionBoxColor = new ColorProperty(
        "PredictionBoxColor", new Color(255, 0, 0).getRGB(), () -> this.predictionBox.getValue()
    );
    private final BooleanProperty predictionLine = new BooleanProperty(
        "PredictionLine", true, () -> this.visualPrediction.getValue()
    );
    private final ColorProperty predictionLineColor = new ColorProperty(
        "PredictionLineColor", new Color(255, 255, 0).getRGB(), () -> this.predictionLine.getValue()
    );
    private final FloatProperty predictionLineWidth = new FloatProperty(
        "PredictionLineWidth", 2.0F, 0.5F, 5.0F, () -> this.predictionLine.getValue()
    );
    private final BooleanProperty showCurrentPos = new BooleanProperty(
        "ShowCurrentPos", true, () -> this.visualPrediction.getValue()
    );
    private final ColorProperty currentPosColor = new ColorProperty(
        "CurrentPosColor", new Color(0, 255, 0).getRGB(), () -> this.showCurrentPos.getValue()
    );
    private final IntProperty predictionDuration = new IntProperty(
        "PredictionDuration", 200, 50, 1000, () -> this.visualPrediction.getValue()
    );
    private boolean isBoosting = false;
    private final TimerUtil boostedTime = new TimerUtil();
    private final TimerUtil slowedTime = new TimerUtil();
    private final TimerUtil cooldownTimer = new TimerUtil();
    private int attackCounter = 0;
    private boolean hasSlowed = false;
    private boolean hasBoosted = false;
    private boolean shouldBlink = false;
    private boolean hasBlink = false;
    private Vec3 predictedPlayerPosition = null;
    private boolean shouldShowPrediction = false;

    public TimerRangeV2() {
        super("TimerRangeV2", false);
    }

    private float effectiveMaxRange() {
        return Math.max(this.maxRange.getValue(), this.minRange.getValue());
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            if (mc.field_71441_e != null) {
                boolean timerChanged = ((IAccessorMinecraft)mc).getTimer().field_74278_d == this.boostTimer.getValue()
                    || ((IAccessorMinecraft)mc).getTimer().field_74278_d == this.slowTimer.getValue();
                KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
                Entity target = killAura == null ? null : killAura.getTarget();
                if (target == null) {
                    if (timerChanged) {
                        ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
                    }

                    this.predictedPlayerPosition = null;
                    this.shouldShowPrediction = false;
                } else if (this.onlyForward.getValue() && !mc.field_71474_y.field_74351_w.func_151470_d()) {
                    this.predictedPlayerPosition = null;
                    this.shouldShowPrediction = false;
                } else if (this.stopBoostingWhenHurting.getValue()
                    && SomeUtil.isHurting()
                    && ((IAccessorMinecraft)mc).getTimer().field_74278_d == this.boostTimer.getValue()) {
                    ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
                    if (this.debugMessage.getValue()) {
                        this.debugMessage("CancelledTimerChange");
                    }

                    this.predictedPlayerPosition = null;
                    this.shouldShowPrediction = false;
                } else if (killAura != null && killAura.isEnabled()) {
                    double distance = BackTrackUtil.getDistanceToEntityBox(target);
                    if (!(distance < this.minRange.getValue().floatValue()) && !(distance > this.effectiveMaxRange())) {
                        boolean justStartedBoosting = !this.isBoosting
                            && this.cooldownTimer.hasTimeElapsed(this.cooldownTime.getValue().intValue());
                        if (justStartedBoosting) {
                            this.isBoosting = true;
                            this.boostedTime.reset();
                            this.slowedTime.reset();
                            this.attackCounter = 0;
                            this.hasSlowed = false;
                            this.hasBoosted = false;
                            if (this.visualPrediction.getValue()) {
                                this.calculateBoostPrediction(player, target);
                                this.shouldShowPrediction = true;
                            }
                        }

                        if (this.isBoosting) {
                            if (this.workMode.getModeString().equals("BoostFirst")) {
                                if (!this.boostedTime.hasTimeElapsed(this.boostTime.getValue().intValue())) {
                                    if (this.blinkOnWorking.getValue()) {
                                        this.shouldBlink = true;
                                    }

                                    ((IAccessorMinecraft)mc).getTimer().field_74278_d = this.boostTimer.getValue();
                                    this.hasBoosted = true;
                                    this.slowedTime.reset();
                                    this.debugMessage("Boosting");
                                    if (this.attackWhenBoosting.getValue()
                                        && this.attackOnBoosting.getValue()
                                        && this.attackCounter < this.attackCount.getValue()) {
                                        this.runAttack();
                                    }

                                    if (this.boostMotion.getValue() && this.boostOnBoosting.getValue()) {
                                        SomeUtil.reduceXZ(this.boostBoostingFactor.getValue().floatValue() + 1.0);
                                    }
                                } else if (!this.slowedTime.hasTimeElapsed(this.slowTime.getValue().intValue())) {
                                    if (this.blinkOnWorking.getValue() && this.shouldBlink) {
                                        this.shouldBlink = false;
                                    }

                                    ((IAccessorMinecraft)mc).getTimer().field_74278_d = this.slowTimer.getValue();
                                    this.hasSlowed = true;
                                    this.debugMessage("Slowing");
                                    if (this.attackWhenBoosting.getValue()
                                        && this.attackCounter < this.attackCount.getValue()
                                        && this.attackOnSlowing.getValue()) {
                                        this.runAttack();
                                    }

                                    if (this.boostMotion.getValue() && this.boostOnSlowing.getValue()) {
                                        SomeUtil.reduceXZ(this.boostSlowingFactor.getValue().floatValue() + 1.0);
                                    }

                                    this.shouldShowPrediction = false;
                                } else if (this.safe.getValue() && this.hasBoosted && !this.hasSlowed) {
                                    if (this.blinkOnWorking.getValue() && this.shouldBlink) {
                                        this.shouldBlink = false;
                                    }

                                    this.slowedTime.reset();
                                    ((IAccessorMinecraft)mc).getTimer().field_74278_d = this.slowTimer.getValue();
                                    this.hasSlowed = true;
                                    this.debugMessage("Safe mode: Forcing slow timer");
                                } else {
                                    this.isBoosting = false;
                                    ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
                                    this.cooldownTimer.reset();
                                    this.attackCounter = 0;
                                    this.predictedPlayerPosition = null;
                                    this.shouldShowPrediction = false;
                                }
                            } else if (!this.slowedTime.hasTimeElapsed(this.slowTime.getValue().intValue())) {
                                if (this.blinkOnWorking.getValue() && this.shouldBlink) {
                                    this.shouldBlink = false;
                                }

                                ((IAccessorMinecraft)mc).getTimer().field_74278_d = this.slowTimer.getValue();
                                this.hasSlowed = true;
                                this.boostedTime.reset();
                                this.debugMessage("Slowing");
                                if (this.attackWhenBoosting.getValue()
                                    && this.attackCounter < this.attackCount.getValue()
                                    && this.attackOnSlowing.getValue()) {
                                    this.runAttack();
                                }

                                if (this.boostMotion.getValue() && this.boostOnSlowing.getValue()) {
                                    SomeUtil.reduceXZ(this.boostSlowingFactor.getValue().floatValue() + 1.0);
                                }
                            } else if (!this.boostedTime.hasTimeElapsed(this.boostTime.getValue().intValue())) {
                                if (this.blinkOnWorking.getValue()) {
                                    this.shouldBlink = true;
                                }

                                ((IAccessorMinecraft)mc).getTimer().field_74278_d = this.boostTimer.getValue();
                                this.hasBoosted = true;
                                this.debugMessage("Boosting");
                                if (this.visualPrediction.getValue() && !this.shouldShowPrediction) {
                                    this.calculateBoostPrediction(player, target);
                                    this.shouldShowPrediction = true;
                                }

                                if (this.attackWhenBoosting.getValue()
                                    && this.attackCounter < this.attackCount.getValue()
                                    && this.attackOnBoosting.getValue()) {
                                    this.runAttack();
                                }

                                if (this.boostMotion.getValue() && this.boostOnBoosting.getValue()) {
                                    SomeUtil.reduceXZ(this.boostBoostingFactor.getValue().floatValue() + 1.0);
                                }
                            } else if (this.safe.getValue() && this.hasBoosted && !this.hasSlowed) {
                                this.slowedTime.reset();
                                ((IAccessorMinecraft)mc).getTimer().field_74278_d = this.slowTimer.getValue();
                                this.hasSlowed = true;
                                this.debugMessage("Safe mode: Forcing slow timer");
                            } else {
                                this.isBoosting = false;
                                ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
                                this.cooldownTimer.reset();
                                this.attackCounter = 0;
                                this.predictedPlayerPosition = null;
                                this.shouldShowPrediction = false;
                            }
                        } else {
                            if (((IAccessorMinecraft)mc).getTimer().field_74278_d != 1.0F) {
                                ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
                            }

                            this.predictedPlayerPosition = null;
                            this.shouldShowPrediction = false;
                        }

                        if (this.shouldShowPrediction
                            && this.boostedTime.hasTimeElapsed(this.predictionDuration.getValue().intValue())) {
                            this.shouldShowPrediction = false;
                        }
                    } else {
                        if (timerChanged) {
                            ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
                        }

                        this.predictedPlayerPosition = null;
                        this.shouldShowPrediction = false;
                    }
                } else {
                    if (timerChanged) {
                        ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
                    }

                    this.predictedPlayerPosition = null;
                    this.shouldShowPrediction = false;
                }
            }
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.visualPrediction.getValue() && this.shouldShowPrediction) {
            EntityPlayer player = mc.field_71439_g;
            if (player != null) {
                Vec3 predictedPos = this.predictedPlayerPosition;
                if (predictedPos != null) {
                    GL11.glPushMatrix();
                    GL11.glDisable(3553);
                    GL11.glDisable(2929);
                    GL11.glEnable(3042);
                    GL11.glBlendFunc(770, 771);
                    double viewerX = mc.func_175598_ae().field_78730_l;
                    double viewerY = mc.func_175598_ae().field_78731_m;
                    double viewerZ = mc.func_175598_ae().field_78728_n;
                    if (this.showCurrentPos.getValue()) {
                        Vec3 currentPos = new Vec3(player.field_70165_t, player.field_70163_u, player.field_70161_v);
                        this.drawPlayerBox(
                            currentPos, viewerX, viewerY, viewerZ, this.currentPosColor.getValue(), 100, "Current"
                        );
                    }

                    if (this.predictionLine.getValue()) {
                        this.drawPredictionLine(player, predictedPos, viewerX, viewerY, viewerZ);
                    }

                    if (this.predictionBox.getValue()) {
                        this.drawPlayerBox(
                            predictedPos, viewerX, viewerY, viewerZ, this.predictionBoxColor.getValue(), 100, "Boosted"
                        );
                    }

                    GL11.glEnable(2929);
                    GL11.glEnable(3553);
                    GL11.glDisable(3042);
                    GL11.glPopMatrix();
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof C03PacketPlayer && this.cancelC03.getValue()) {
            if (this.cancelC03.getValue() && this.isBoosting) {
                event.setCancelled(true);
            }
        } else if (this.blinkOnWorking.getValue()) {
            if (this.shouldBlink && this.isBoosting && !this.hasBlink) {
                BlinkUtil.blink(event, true, false, null, Integer.MAX_VALUE, null);
                if (!this.hasBlink) {
                    this.debugMessage("StartBlink");
                }

                this.hasBlink = true;
            } else if (this.hasBlink && !this.isBoosting) {
                this.debugMessage("StopBlink");
                BlinkUtil.unblink();
                this.hasBlink = false;
            }
        }
    }

    @Override
    public void onEnabled() {
        ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
        this.isBoosting = false;
        this.boostedTime.reset();
        this.slowedTime.reset();
        this.cooldownTimer.reset();
        this.hasSlowed = false;
        this.hasBoosted = false;
        this.predictedPlayerPosition = null;
        this.shouldShowPrediction = false;
    }

    @Override
    public void onDisabled() {
        float current = ((IAccessorMinecraft)mc).getTimer().field_74278_d;
        if (current == this.boostTimer.getValue() || current == this.slowTimer.getValue()) {
            ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
        }

        this.predictedPlayerPosition = null;
        this.shouldShowPrediction = false;
    }

    @Override
    public String[] getSuffix() {
        switch (this.tagMode.getModeString()) {
            case "WorkRange":
                return new String[]{String.format("%s - %s", this.minRange.getValue(), this.effectiveMaxRange())};
            case "IsWorking":
                return new String[]{this.isBoosting ? "Working" : "Idle"};
            case "MinSpeed-MaxSpeed":
                return new String[]{String.format("%sx - %sx", this.slowTimer.getValue(), this.boostTimer.getValue())};
            case "Custom":
                return new String[]{this.customText.getValue()};
            default:
                return new String[0];
        }
    }

    private void debugMessage(String message) {
        if (this.debugMessage.getValue()) {
            ChatUtil.display(message);
        }
    }

    private void runAttack() {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            Entity aimedTarget = mc.field_71476_x == null ? null : mc.field_71476_x.field_72308_g;
            KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
            Entity target = (Entity)(aimedTarget != null
                ? aimedTarget
                : (killAura == null ? null : killAura.getTarget()));
            if (target != null) {
                double dist = BackTrackUtil.getDistanceToEntityBox(target);
                if (!(dist > this.attackMaxRange.getValue().floatValue())) {
                    if (!this.onlyAttackWhenNotReachedCPSLimit.getValue()
                        || CPSCounter.getCPS(CPSCounter.MouseButton.LEFT) < this.CPSLimit.getValue()) {
                        if (this.attackCounter < this.attackCount.getValue()) {
                            boolean shouldKeepSprint = this.keepSprint.getValue()
                                && player.field_70737_aN >= this.allowKeepSprintHurtTime.getValue()
                                && player.field_70737_aN <= this.allowKeepSprintHurtTime.getSecondValue();
                            SomeUtil.runAttack(
                                shouldKeepSprint,
                                this.attackMaxRange.getValue(),
                                1,
                                target,
                                true,
                                this.swingMode.getModeString(),
                                false,
                                false,
                                "Attacked",
                                false,
                                null,
                                null,
                                1.0F
                            );
                            this.attackCounter++;
                            this.debugMessage("Attacked");
                        }
                    }
                }
            }
        }
    }

    private void calculateBoostPrediction(EntityPlayer player, Entity target) {
        double toTargetX = target.field_70165_t - player.field_70165_t;
        double toTargetZ = target.field_70161_v - player.field_70161_v;
        double distance = Math.sqrt(toTargetX * toTargetX + toTargetZ * toTargetZ);
        double dirX = toTargetX / distance;
        double dirZ = toTargetZ / distance;
        double ticks = this.boostTime.getValue().intValue() / 50.0;
        double totalMoveDistance = 0.1 * this.boostTimer.getValue().floatValue() * ticks;
        double maxMoveDistance = Math.min(distance, totalMoveDistance);
        double predictedX = player.field_70165_t + dirX * maxMoveDistance;
        double predictedY = player.field_70163_u;
        double predictedZ = player.field_70161_v + dirZ * maxMoveDistance;
        this.predictedPlayerPosition = new Vec3(predictedX, predictedY, predictedZ);
    }

    private void drawPredictionLine(
        EntityPlayer player, Vec3 predictedPos, double viewerX, double viewerY, double viewerZ
    ) {
        Vec3 currentPos = new Vec3(player.field_70165_t, player.field_70163_u, player.field_70161_v);
        GL11.glLineWidth(this.predictionLineWidth.getValue());
        Color color = new Color(this.predictionLineColor.getValue());
        GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 0.5882353F);
        GL11.glBegin(1);
        GL11.glVertex3d(
            currentPos.field_72450_a - viewerX, currentPos.field_72448_b - viewerY, currentPos.field_72449_c - viewerZ
        );
        GL11.glVertex3d(
            predictedPos.field_72450_a - viewerX,
            predictedPos.field_72448_b - viewerY,
            predictedPos.field_72449_c - viewerZ
        );
        GL11.glEnd();
    }

    private void drawPlayerBox(
        Vec3 position, double viewerX, double viewerY, double viewerZ, int colorInt, int alpha, String label
    ) {
        double x = position.field_72450_a - viewerX;
        double y = position.field_72448_b - viewerY;
        double z = position.field_72449_c - viewerZ;
        Color color = new Color(colorInt);
        GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, alpha / 255.0F);
        double width = 0.3;
        double height = 1.8;
        RenderUtil.drawBoundingBox(
            new AxisAlignedBB(x - width, y, z - width, x + width, y + height, z + width),
            color.getRed(),
            color.getGreen(),
            color.getBlue(),
            color.getAlpha(),
            1.0F
        );
    }
}
