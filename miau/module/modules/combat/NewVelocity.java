package miau.module.modules.combat;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.JumpEvent;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorEntity;
import miau.mixin.IAccessorS12PacketEntityVelocity;
import miau.mixin.IAccessorS27PacketExplosion;
import miau.module.Module;
import miau.module.modules.movement.KeepSprint;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.TextProperty;
import miau.util.client.ChatUtil;
import miau.util.math.RandomUtil;
import miau.util.misc.BackTrackUtil;
import miau.util.misc.CPSCounter;
import miau.util.misc.SomeUtil;
import miau.util.network.BlinkUtil;
import miau.util.network.PacketUtil;
import miau.util.player.ItemUtil;
import miau.util.player.RayCastUtil;
import miau.util.time.TimerUtil;
import net.minecraft.block.BlockSoulSand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;

public class NewVelocity extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final String[] TAG_MODES = new String[]{"Normal", "Custom", "None"};
    private static final String[] MODES = new String[]{
        "AttackReduce",
        "Intave",
        "Intave2",
        "IntaveSafe",
        "OldIntave",
        "Matrix",
        "PolarJump",
        "Delay",
        "LegitClick",
        "LegitClick2",
        "OldGrim",
        "JumpReset",
        "AirJumpReset",
        "FakeJump",
        "MineBerryNew",
        "MineMenClub",
        "NoC0F",
        "GrimExempt117",
        "Prediction",
        "Tatako0.9.6.1To0.9.7.3-a2",
        "XZSwitch"
    };
    private static final String[] WORK_MODES = new String[]{"OnGround+InAir", "OnGround", "InAir", "OnlySprinting"};
    private static final String[] COOLDOWN_MODES = new String[]{"ReceivedHit", "Tick", "Both"};
    private static final String[] SWING_MODES = new String[]{"Off", "Normal", "Packet"};
    public static boolean canCancelHitSlow = false;
    public final ModeProperty tagMode = new ModeProperty("TagMode", 0, TAG_MODES);
    public final TextProperty customText = new TextProperty(
        "CustomText", "", () -> this.tagMode.getModeString().equals("Custom")
    );
    public final ModeProperty mode = new ModeProperty("Mode", 11, MODES);
    private final IntProperty blinkTicks = new IntProperty(
        "BlinkTicks", 5, 1, 10, () -> this.mode.getModeString().equals("Prediction")
    );
    private boolean preBlinking = false;
    private boolean preShouldBlink = false;
    private boolean preShouldAttack = false;
    private final IntProperty mineBerryMinWorkHurtTime = new IntProperty(
        "MinWorkHurtTime", 1, 1, 10, () -> this.mode.getModeString().equals("MineBerryNew")
    );
    private boolean mineBerryFirstReduce = false;
    private final FloatProperty grimrange = new FloatProperty(
        "OldGrimWorkRange", 3.5F, 0.0F, 6.0F, () -> this.mode.getModeString().equals("OldGrim")
    );
    private final IntProperty attackCountValue = new IntProperty(
        "Attack Counts", 12, 1, 16, () -> this.mode.getModeString().equals("OldGrim")
    );
    private final BooleanProperty fireCheckValue = new BooleanProperty(
        "FireCheck", false, () -> this.mode.getModeString().equals("OldGrim")
    );
    private final BooleanProperty waterCheckValue = new BooleanProperty(
        "WaterCheck", false, () -> this.mode.getModeString().equals("OldGrim")
    );
    private final BooleanProperty fallCheckValue = new BooleanProperty(
        "FallCheck", false, () -> this.mode.getModeString().equals("OldGrim")
    );
    private final BooleanProperty consumecheck = new BooleanProperty(
        "ConsumableCheck", false, () -> this.mode.getModeString().equals("OldGrim")
    );
    private final BooleanProperty raycastValue = new BooleanProperty(
        "Ray cast", false, () -> this.mode.getModeString().equals("OldGrim")
    );
    private Entity entity = null;
    private int velX = 0;
    private int velY = 0;
    private int velZ = 0;
    private final FloatProperty blinkWorkMaxDistance = new FloatProperty(
        "BlinkWorkMaxDistance", 3.5F, 0.0F, 6.0F, () -> this.mode.getModeString().equals("Intave")
    );
    private final IntProperty maxBlinkTicks = new IntProperty(
        "MaxBlinkTicks", 10, 0, 10, () -> this.mode.getModeString().equals("Intave")
    );
    private final BooleanProperty intaveJumpReset = new BooleanProperty(
        "IntaveJumpReset", true, () -> this.mode.getModeString().equals("Intave")
    );
    private final BooleanProperty intaveJumpResetSprint = new BooleanProperty(
        "ForceSprintJump", false, () -> this.mode.getModeString().equals("Intave") && this.intaveJumpReset.getValue()
    );
    private final BooleanProperty intaveJumpResetNeedForward = new BooleanProperty(
        "ForceSprintJumpNeedForward",
        true,
        () -> this.mode.getModeString().equals("Intave")
            && this.intaveJumpReset.getValue()
            && this.intaveJumpResetSprint.getValue()
    );
    private final BooleanProperty extraC0APerReduce = new BooleanProperty(
        "ExtraC0APerReduce", false, () -> this.mode.getModeString().equals("Intave")
    );
    private final IntProperty extraPacketCount = new IntProperty(
        "ExtraC0APacketCount",
        1,
        1,
        5,
        () -> this.mode.getModeString().equals("Intave") && this.extraC0APerReduce.getValue()
    );
    private final BooleanProperty moreReduce = new BooleanProperty(
        "MoreReduce", false, () -> this.mode.getModeString().equals("Intave")
    );
    private final IntProperty maxMoreReduce = new IntProperty(
        "MaxMoreReduceCount", 3, 1, 6, () -> this.moreReduce.getValue() && this.mode.getModeString().equals("Intave")
    );
    private final BooleanProperty onlyWhenNeed = new BooleanProperty(
        "OnlyWhenNeed", true, () -> this.mode.getModeString().equals("Intave")
    );
    private final BooleanProperty intaveSafe = new BooleanProperty(
        "IntaveSafe", true, () -> this.mode.getModeString().equals("Intave")
    );
    private boolean hasReceivedVelocity = false;
    private final Set<NewVelocity.IntavePhase> triggeredPhases = new HashSet<>();
    private int previousTimerState = 0;
    private boolean intaveReversed = false;
    private int timerState = 0;
    private boolean boosting = true;
    private boolean slowing = false;
    private int intaveClickTimes = 0;
    public int moreReduceTimes = 0;
    private boolean canOutPutMessage = false;
    private boolean shouldBlink = false;
    private boolean lastBlinkState = false;
    public int intaveReduceTimes = 0;
    private final FloatProperty minFactor = new FloatProperty(
        "MinFactor", 0.4F, 0.0F, 1.0F, () -> this.mode.getModeString().equals("Intave2")
    );
    private int intave2ReduceCounter = 0;
    private final BooleanProperty matrixBoost = new BooleanProperty(
        "BoostAfterReduce", false, () -> this.mode.getModeString().equals("Matrix")
    );
    private final FloatProperty matrixBoostFactor = new FloatProperty(
        "BoostFactor",
        0.33F,
        0.0F,
        5.0F,
        () -> this.mode.getModeString().equals("Matrix") && this.matrixBoost.getValue()
    );
    private final IntProperty matrixBoostDelay = new IntProperty(
        "BoostCooldown", 0, 0, 2000, () -> this.mode.getModeString().equals("Matrix") && this.matrixBoost.getValue()
    );
    private final TimerUtil matrixBoostTimer = new TimerUtil();
    private boolean matrixMotionYReduce = false;
    private final IntProperty clicks = new IntProperty(
        "Clicks", 1, 1, 20, () -> this.mode.getModeString().equals("LegitClick")
    );
    private final IntProperty durationHurtTime = new IntProperty(
        "DurationHurtTimes", 1, 1, 9, () -> this.mode.getModeString().equals("LegitClick")
    );
    private final IntProperty clickDelayTicks = new IntProperty(
        "ClickCooldownTicks", 0, 0, 10, () -> this.mode.getModeString().equals("LegitClick")
    );
    private final FloatProperty clickChancePerClick = new FloatProperty(
        "ClickChancePerClick", 1.0F, 0.0F, 1.0F, () -> this.mode.getModeString().equals("LegitClick")
    );
    private final BooleanProperty whenFacingEnemyOnly = new BooleanProperty(
        "WhenFacingEnemyOnly", true, () -> this.mode.getModeString().equals("LegitClick")
    );
    private final BooleanProperty ignoreBlocking = new BooleanProperty(
        "IgnoreBlocking", false, () -> this.mode.getModeString().equals("LegitClick")
    );
    private final FloatProperty clickRange = new FloatProperty(
        "ClickRange", 3.0F, 1.0F, 6.0F, () -> this.mode.getModeString().equals("LegitClick")
    );
    private final ModeProperty swingMode = new ModeProperty(
        "SwingMode", 1, SWING_MODES, () -> this.mode.getModeString().equals("LegitClick")
    );
    private final BooleanProperty modifyMotionWhenClick = new BooleanProperty(
        "ModifyMotionWhenClick", false, () -> this.mode.getModeString().equals("LegitClick")
    );
    private final BooleanProperty makeVanillaAttackNotStopSprint = new BooleanProperty(
        "MakeVanillaAttackNotStopSprint",
        false,
        () -> this.modifyMotionWhenClick.getValue() && this.mode.getModeString().equals("LegitClick")
    );
    private final FloatProperty modifyMotionFactor = new FloatProperty(
        "XZFactor",
        0.6F,
        -1.0F,
        1.0F,
        () -> this.modifyMotionWhenClick.getValue() && this.mode.getModeString().equals("LegitClick")
    );
    private int attackStartHurtTime = 0;
    private int clickDelayTick = 0;
    private final IntProperty click2MaxTimes = new IntProperty(
        "LegitClick2MaxClickTimes", 3, 1, 20, () -> this.mode.getModeString().equals("LegitClick2")
    );
    private final IntProperty addClicksPerUserClick = new IntProperty(
        "AddClicksPerUserClick", 1, 1, 20, () -> this.mode.getModeString().equals("LegitClick2")
    );
    private int legitClick2Times = 0;
    private final IntProperty mineMenClubDelay = new IntProperty(
        "PacketCancelDelay", 20, 0, 20, () -> this.mode.getModeString().equals("MineMenClub")
    );
    private int minemenClubCounter = 0;
    private final IntProperty delayTicks = new IntProperty(
        "DelayTicks", 3, 1, 20, () -> this.mode.getModeString().equals("Delay")
    );
    private final IntProperty delayChance = new IntProperty(
        "DelayChance", 100, 0, 100, () -> this.mode.getModeString().equals("Delay")
    );
    private final FloatProperty delayHorizontal = new FloatProperty(
        "DelayHorizontal", 0.0F, -1.0F, 1.0F, () -> this.mode.getModeString().equals("Delay")
    );
    private final FloatProperty delayVertical = new FloatProperty(
        "DelayVertical", 0.0F, -1.0F, 1.0F, () -> this.mode.getModeString().equals("Delay")
    );
    private final BooleanProperty delayAttackReduce = new BooleanProperty(
        "DelayAttackReduce", false, () -> this.mode.getModeString().equals("Delay")
    );
    private final BooleanProperty delayFakeCheck = new BooleanProperty(
        "DelayFakeCheck", true, () -> this.mode.getModeString().equals("Delay")
    );
    private int delayChanceCounter = 0;
    private boolean delayActive = false;
    private boolean delayReverseFlag = false;
    private boolean delayPendingExplosion = false;
    private boolean delayAllowNext = true;
    private final Map<Packet<?>, Long> delayedPackets = new LinkedHashMap<>();
    private final TimerUtil delayTimer = new TimerUtil();
    private int delayTickCounter = 0;
    private final FloatProperty attackReduceFactor = new FloatProperty(
        "AttackXZFactor", 0.6F, 0.0F, 1.0F, () -> this.mode.getModeString().equals("AttackReduce")
    );
    private final IntProperty attackHurtTime = new IntProperty(
        "AttackHurtTime", 9, 1, 10, () -> this.mode.getModeString().equals("AttackReduce")
    );
    private final BooleanProperty jumpReset = new BooleanProperty(
        "JumpReset",
        false,
        () -> !this.mode.getModeString().equals("Intave")
            && !this.mode.getModeString().equals("JumpReset")
            && !this.mode.getModeString().equals("AirJumpReset")
            && !this.mode.getModeString().equals("PolarJump")
            && !this.mode.getModeString().equals("IntaveSafe")
    );
    private final IntProperty jumpResetChance = new IntProperty(
        "JumpChance", 100, 0, 100, this::displayJumpResetChoices
    );
    private final ModeProperty jumpCooldownMode = new ModeProperty(
        "JumpCooldownMode", 0, COOLDOWN_MODES, this::displayJumpResetChoices
    );
    private final IntProperty jumpCooldownTick = new IntProperty(
        "JumpCooldownTicks",
        4,
        0,
        20,
        () -> this.displayJumpResetChoices()
            && (this.jumpCooldownMode.getValue() == 1 || this.jumpCooldownMode.getValue() == 2)
    );
    private final IntProperty jumpCooldownReceivedHit = new IntProperty(
        "JumpCooldownReceivedHit",
        1,
        0,
        5,
        () -> this.displayJumpResetChoices()
            && (this.jumpCooldownMode.getValue() == 0 || this.jumpCooldownMode.getValue() == 2)
    );
    private final BooleanProperty checkUserSprint = new BooleanProperty(
        "CheckUserIsSprinting", true, this::displayJumpResetChoices
    );
    private final BooleanProperty matrixJumpTest = new BooleanProperty(
        "MatrixJumpReset", false, this::displayJumpResetChoices
    );
    private int jumpCooldownTickCounter = 0;
    private int jumpCooldownReceivedHitCounter = 0;
    private int polarHurtTime = RandomUtil.nextInt(7, 9);
    private final BooleanProperty pauseOnExplosion = new BooleanProperty("PauseOnExplosion", false);
    private final IntProperty pauseTicksProp = new IntProperty(
        "PauseTicks", 20, 0, 100, () -> this.pauseOnExplosion.getValue()
    );
    private int pausedTicks = 0;
    private final ModeProperty allowWorkWhen = new ModeProperty("AllowWorkWhen", 0, WORK_MODES);
    public final BooleanProperty debugMessage = new BooleanProperty(
        "DebugMessage",
        false,
        () -> {
            String m = this.mode.getModeString();
            return m.equals("Intave")
                || m.equals("IntaveSafe")
                || m.equals("Intave2")
                || m.equals("LegitClick")
                || m.equals("JumpReset")
                || m.equals("AirJumpReset")
                || m.equals("OldGrim")
                || m.equals("Delay")
                || m.equals("MineBerryNew")
                || m.equals("FakeJump");
        }
    );
    private final BooleanProperty smartJumpReset = new BooleanProperty("SmartJumpReset", false);
    private boolean shouldCancelAttack = false;
    private int shouldAttackCount = 0;
    private boolean hasJumpReset = false;
    public double packetMotionX = 0.0;
    private double packetMotionY = 0.0;
    public double packetMotionZ = 0.0;
    private EntityLivingBase globalTarget = null;
    private final TimerUtil sprintTimer = new TimerUtil();
    private boolean serverSprintState = false;

    public NewVelocity() {
        super("NewVelocity", false);
    }

    private boolean modeIs(String... names) {
        String current = this.mode.getModeString();

        for (String name : names) {
            if (current.equals(name)) {
                return true;
            }
        }

        return false;
    }

    private boolean doNotNeedReduce() {
        return mc.field_71439_g == null
            || mc.field_71439_g.field_70737_aN == 0
            || this.knockBackIsNegated(this.packetMotionX, this.packetMotionZ);
    }

    @Override
    public String[] getSuffix() {
        String tagModeString = this.tagMode.getModeString();
        if (tagModeString.equals("None")) {
            return new String[0];
        } else {
            return tagModeString.equals("Custom")
                ? new String[]{this.customText.getValue()}
                : new String[]{this.mode.getModeString()};
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (event.getTarget() instanceof EntityLivingBase) {
            this.globalTarget = (EntityLivingBase)event.getTarget();
        } else {
            this.globalTarget = null;
        }

        if (this.canWorkNow()) {
            String currentMode = this.mode.getModeString();
            if (currentMode.equals("Intave")) {
                switch (mc.field_71439_g.field_70737_aN) {
                    case 1:
                    case 4:
                    case 7:
                        this.intaveReduce(3, this.intaveSafe.getValue());
                        break;
                    case 2:
                    case 5:
                    case 8:
                        this.intaveReduce(2, this.intaveSafe.getValue());
                        break;
                    case 3:
                    case 6:
                    case 9:
                        this.intaveReduce(1, this.intaveSafe.getValue());
                        break;
                    case 10:
                        this.intaveReduce(0, this.intaveSafe.getValue());
                }
            } else if (currentMode.equals("MineBerryNew")) {
                if (mc.field_71439_g.field_70737_aN >= this.mineBerryMinWorkHurtTime.getValue()) {
                    if (CPSCounter.getCPS(CPSCounter.MouseButton.LEFT) > 22) {
                        return;
                    }

                    int attackCount = this.mineBerryFirstReduce ? 3 : 1;
                    PacketUtil.sendPacket(new C0APacketAnimation());
                    SomeUtil.runAttack(
                        false,
                        3.0F,
                        attackCount,
                        null,
                        true,
                        "Packet",
                        false,
                        this.debugMessage.getValue(),
                        "Attacked",
                        true,
                        null,
                        null,
                        1.0F
                    );
                    if (this.mineBerryFirstReduce) {
                        this.mineBerryFirstReduce = false;
                    }

                    PacketUtil.sendPacket(new C0APacketAnimation());
                }
            } else if (currentMode.equals("AttackReduce")) {
                if (mc.field_71439_g.field_70737_aN == this.attackHurtTime.getValue()) {
                    SomeUtil.reduceXZ(this.attackReduceFactor.getValue().doubleValue());
                }
            } else if (currentMode.equals("Intave2")) {
                double reduceFactor = Math.max(
                    1.0 - this.intave2ReduceCounter * 0.1, this.minFactor.getValue().doubleValue()
                );
                SomeUtil.reduceXZ(reduceFactor);
                this.debugMessage("Intave2Reduce");
                this.intave2ReduceCounter++;
            } else if (currentMode.equals("OldIntave")) {
                if (mc.field_71439_g.field_70737_aN >= 2 && mc.field_71439_g.field_70737_aN <= 10) {
                    SomeUtil.reduceXZ(0.75);
                }

                if (mc.field_71439_g.field_70737_aN >= 1 && mc.field_71439_g.field_70737_aN <= 4) {
                    if (mc.field_71439_g.field_70181_x > 0.0) {
                        SomeUtil.reduceY(0.9);
                    } else {
                        SomeUtil.reduceY(1.1);
                    }
                }
            } else if (currentMode.equals("IntaveSafe")) {
                switch (mc.field_71439_g.field_70737_aN) {
                    case 8:
                        SomeUtil.reduceXZ(0.8);
                        this.debugMessage("IntaveSafeReduce");
                        break;
                    case 9:
                        SomeUtil.reduceXZ(0.6);
                        this.debugMessage("IntaveSafeReduce");
                }
            } else if (currentMode.equals("LegitClick2")) {
                this.extraJumpReset();
                if (mc.field_71439_g.field_70737_aN > 0 && this.legitClick2Times < this.click2MaxTimes.getValue()) {
                    PacketUtil.sendPacketNoEvent(new C02PacketUseEntity(mc.field_147125_j, Action.ATTACK));
                    mc.field_71439_g.func_71038_i();
                    this.legitClick2Times = this.legitClick2Times + this.addClicksPerUserClick.getValue();
                }
            } else if (currentMode.equals("OldGrim")) {
                mc.func_147114_u().func_147298_b().func_179290_a(new C0APacketAnimation());
                mc.func_147114_u()
                    .func_147298_b()
                    .func_179290_a(new C02PacketUseEntity(event.getTarget(), Action.ATTACK));
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null && mc.field_71439_g.field_70737_aN == 0 && this.hasReceivedVelocity) {
                this.hasReceivedVelocity = false;
            }

            if (this.clickDelayTick != 0) {
                this.clickDelayTick--;
            }

            if (this.canWorkNow()) {
                EntityPlayer player = mc.field_71439_g;
                if (player != null) {
                    this.shouldCancelAttack = this.shouldJumpReset(false, null, null, null, null, false);
                    this.updateJumpResetCooldown();
                    if (!this.shouldCancelAttack && this.shouldAttackCount != 0 && this.hasJumpReset) {
                        SomeUtil.runAttack(
                            false,
                            3.0F,
                            this.shouldAttackCount,
                            null,
                            true,
                            "Packet",
                            true,
                            false,
                            "Attacked",
                            true,
                            null,
                            null,
                            1.0F
                        );
                        this.shouldAttackCount = 0;
                    }

                    String currentMode = this.mode.getModeString();
                    if (currentMode.equals("Intave")) {
                        boolean shouldStop = mc.field_71439_g.field_70737_aN == 10 - this.maxBlinkTicks.getValue()
                            || this.maxBlinkTicks.getValue() == 0;
                        if (shouldStop && this.lastBlinkState) {
                            this.stopIntaveBlink();
                        }

                        boolean checkSprint = !this.intaveJumpResetSprint.getValue();
                        if (this.intaveJumpReset.getValue()
                            && this.shouldJumpReset(
                                checkSprint,
                                true,
                                this.intaveJumpResetNeedForward.getValue(),
                                true,
                                this.intaveJumpResetNeedForward.getValue(),
                                false
                            )) {
                            if (this.intaveJumpResetSprint.getValue()) {
                                SomeUtil.changeSprint(true, true, true);
                                this.serverSprintState = true;
                            }

                            player.func_70664_aZ();
                            this.debugMessage("Jump | " + mc.field_71439_g.field_70737_aN);
                        }

                        if (this.packetMotionValid()
                            && this.triggeredPhases.contains(NewVelocity.IntavePhase.PHASE_1)
                            && this.triggeredPhases.contains(NewVelocity.IntavePhase.PHASE_2)
                            && this.triggeredPhases.contains(NewVelocity.IntavePhase.PHASE_3)
                            && mc.field_71439_g.field_70737_aN != 0
                            && this.canOutPutMessage) {
                            this.debugMessage(
                                this.packetMotionX
                                    + " "
                                    + this.packetMotionZ
                                    + " -> "
                                    + this.playerNowMotionOutPut(true, false, true)
                            );
                            this.canOutPutMessage = false;
                        }
                    } else if (currentMode.equals("FakeJump")) {
                        double e = mc.field_71439_g.field_70181_x;
                        if (mc.field_71439_g.field_70737_aN == 9
                            && !mc.field_71474_y.field_74314_A.func_151470_d()
                            && mc.field_71439_g.field_70181_x != 0.42) {
                            mc.field_71439_g.func_70664_aZ();
                            this.debugMessage("Jump");
                            if (e != mc.field_71439_g.field_70181_x) {
                                mc.field_71439_g.field_70181_x = e;
                            }
                        }
                    } else if (currentMode.equals("Prediction")) {
                        this.preShouldBlink = mc.field_71439_g.field_70737_aN >= 10 - this.blinkTicks.getValue()
                            && mc.field_71439_g.field_70737_aN <= 10
                            && this.hasReceivedVelocity;
                    } else if (currentMode.equals("IntaveSafe")) {
                        if (this.shouldJumpReset(true, null, null, null, null, false)
                            && mc.field_71439_g.field_70173_aa % 2 == 0) {
                            player.func_70664_aZ();
                        }

                        this.debugMessage("Jump");
                    } else if (currentMode.equals("JumpReset")) {
                        if (this.shouldJumpReset(this.checkUserSprint.getValue(), false, null, null, null, false)) {
                            player.func_70664_aZ();
                            if (this.matrixJumpTest.getValue()) {
                                mc.field_71439_g.field_70181_x = this.packetMotionY;
                            }

                            this.debugMessage("Jump");
                            this.hasReceivedVelocity = false;
                            this.resetJumpCooldownCounter();
                        }
                    } else if (currentMode.equals("AirJumpReset")) {
                        if (this.shouldJumpReset(false, false, null, null, null, false)) {
                            player.func_70664_aZ();
                            this.debugMessage("Jump");
                            this.hasReceivedVelocity = false;
                            this.resetJumpCooldownCounter();
                        }
                    } else if (currentMode.equals("Matrix")
                        || currentMode.equals("LegitClick")
                        || currentMode.equals("LegitClick2")
                        || currentMode.equals("MineMenClub")
                        || currentMode.equals("NoC0F")
                        || currentMode.equals("GrimExempt117")
                        || currentMode.equals("XZSwitch")
                        || currentMode.equals("OldIntave")
                        || currentMode.equals("AttackReduce")
                        || currentMode.equals("MineBerryNew")) {
                        this.extraJumpReset();
                        if (currentMode.equals("LegitClick")) {
                            this.handleLegitClick();
                        }

                        if (currentMode.equals("MineMenClub")) {
                            this.minemenClubCounter++;
                        }
                    } else if (currentMode.equals("Delay")) {
                        if (this.delayReverseFlag
                            && (
                                this.canDelay()
                                    || this.isInLiquidOrWeb()
                                    || this.delayTickCounter >= this.delayTicks.getValue()
                            )) {
                            this.applyDelayedVelocity();
                            this.delayReverseFlag = false;
                            this.delayTickCounter = 0;
                            this.delayTimer.reset();
                        }

                        if (this.delayReverseFlag) {
                            this.delayTickCounter++;
                        }

                        if (this.delayActive) {
                            double speed = Math.sqrt(
                                player.field_70159_w * player.field_70159_w
                                    + player.field_70179_y * player.field_70179_y
                            );
                            if (speed > 0.1) {
                                double yaw = Math.toDegrees(Math.atan2(player.field_70179_y, player.field_70159_w))
                                    - 90.0;
                                player.field_70159_w = -Math.sin(Math.toRadians(yaw)) * speed;
                                player.field_70179_y = Math.cos(Math.toRadians(yaw)) * speed;
                            }

                            this.delayActive = false;
                        } else {
                            this.extraJumpReset();
                        }
                    } else if (currentMode.equals("PolarJump")) {
                        if (this.shouldJumpReset(true, true, true, true, null, true)) {
                            if (!mc.field_71474_y.field_74314_A.func_151470_d()) {
                                player.func_70664_aZ();
                            }

                            this.polarHurtTime = RandomUtil.nextInt(7, 9);
                            if (mc.field_71439_g.field_70737_aN == 0) {
                                this.hasReceivedVelocity = false;
                            }
                        }
                    } else if (currentMode.equals("OldGrim")
                        && mc.field_71439_g.field_70737_aN > 0
                        && mc.field_71439_g.field_70122_E) {
                        mc.field_71439_g.func_70024_g(-1.3E-10, -1.3E-10, -1.3E-10);
                        mc.field_71439_g.func_70031_b(false);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.canWorkNow()) {
            if (!event.isCancelled()) {
                EntityPlayer player = mc.field_71439_g;
                if (player != null) {
                    Packet<?> packet = event.getPacket();
                    if (isAttackPacketAndSwingPacket(packet)
                        && this.shouldCancelAttack
                        && this.smartJumpReset.getValue()
                        && !this.hasJumpReset) {
                        event.setCancelled(true);
                        if (isAttackPacket(packet)) {
                            this.shouldAttackCount++;
                        }
                    }

                    if (packet instanceof S27PacketExplosion && this.pauseOnExplosion.getValue()) {
                        this.pausedTicks = this.pauseTicksProp.getValue();
                    } else if (!this.mode.getModeString().equals("NoC0F")
                        || (!(packet instanceof C0FPacketConfirmTransaction) || mc.field_71439_g.field_70737_aN <= 0)
                            && (
                                !(packet instanceof S12PacketEntityVelocity)
                                    || !this.isValidS12Packet((S12PacketEntityVelocity)packet)
                            )
                            && !(packet instanceof S27PacketExplosion)) {
                        if (packet instanceof S12PacketEntityVelocity
                            && this.isValidS12Packet((S12PacketEntityVelocity)packet)) {
                            S12PacketEntityVelocity s12 = (S12PacketEntityVelocity)packet;
                            this.packetMotionX = SomeUtil.roundToPlacesIfNeeded(realMotionX(s12));
                            this.packetMotionY = SomeUtil.roundToPlacesIfNeeded(realMotionY(s12));
                            this.packetMotionZ = SomeUtil.roundToPlacesIfNeeded(realMotionZ(s12));
                            this.sprintTimer.reset();
                            String currentMode = this.mode.getModeString();
                            if (currentMode.equals("PolarJump")
                                || currentMode.equals("JumpReset")
                                || currentMode.equals("AirJumpReset")
                                || currentMode.equals("LegitClick")) {
                                this.hasReceivedVelocity = true;
                            } else if (currentMode.equals("MineBerryNew")) {
                                this.mineBerryFirstReduce = true;
                            } else if (currentMode.equals("OldGrim")) {
                                this.handleOldGrim(s12, event);
                            } else if (currentMode.equals("LegitClick2")) {
                                this.hasReceivedVelocity = true;
                                this.legitClick2Times = 0;
                            } else if (currentMode.equals("Intave")) {
                                this.hasReceivedVelocity = true;
                                this.triggeredPhases.clear();
                                this.intaveClickTimes = 0;
                                this.moreReduceTimes = 0;
                                this.canOutPutMessage = true;
                                this.shouldBlink = true;
                                this.intaveReversed = false;
                                this.timerState = 0;
                                this.boosting = false;
                                this.slowing = false;
                                this.intaveReduceTimes = 0;
                            } else if (currentMode.equals("Intave2")) {
                                this.intave2ReduceCounter = 0;
                            } else if (currentMode.equals("XZSwitch")) {
                                event.setCancelled(true);
                                SomeUtil.setMotion(realMotionZ(s12), realMotionY(s12), realMotionX(s12));
                            } else if (currentMode.equals("GrimExempt117")) {
                                PacketUtil.sendPacketNoEvent(
                                    new C06PacketPlayerPosLook(
                                        player.field_70165_t,
                                        player.field_70163_u,
                                        player.field_70161_v,
                                        player.field_70177_z,
                                        player.field_70125_A,
                                        player.field_70122_E
                                    )
                                );
                                PacketUtil.sendPacketNoEvent(
                                    new C07PacketPlayerDigging(
                                        net.minecraft.network.play.client.C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                                        player.func_180425_c(),
                                        EnumFacing.DOWN
                                    )
                                );
                                event.setCancelled(true);
                            } else if (currentMode.equals("Matrix")) {
                                this.handleMatrixVelocity(s12, event);
                            } else if (currentMode.equals("Delay")) {
                                this.handleDelayVelocity(s12, event);
                            } else if (currentMode.equals("MineMenClub")) {
                                if (this.minemenClubCounter > this.mineMenClubDelay.getValue()) {
                                    event.setCancelled(true);
                                    this.minemenClubCounter = 0;
                                } else {
                                    this.hasReceivedVelocity = true;
                                }
                            }
                        }

                        if (packet instanceof S27PacketExplosion) {
                            String currentMode = this.mode.getModeString();
                            if (currentMode.equals("Delay")) {
                                this.handleExplosionDelay((S27PacketExplosion)packet, event);
                            } else if (currentMode.equals("MineMenClub")
                                && this.minemenClubCounter > this.mineMenClubDelay.getValue()) {
                                event.setCancelled(true);
                                this.minemenClubCounter = 0;
                            }
                        }

                        if (packet instanceof C0BPacketEntityAction) {
                            this.serverSprintState = ((C0BPacketEntityAction)packet).func_180764_b()
                                == net.minecraft.network.play.client.C0BPacketEntityAction.Action.START_SPRINTING;
                        }

                        String currentMode = this.mode.getModeString();
                        if (currentMode.equals("Intave")) {
                            boolean blinkDistanceCheck;
                            if (this.blinkWorkMaxDistance.getValue() == 0.0F) {
                                blinkDistanceCheck = true;
                            } else {
                                KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
                                EntityLivingBase target = killAura != null ? killAura.getTarget() : null;
                                blinkDistanceCheck = target != null
                                    && this.getDistance(target) <= this.blinkWorkMaxDistance.getValue().floatValue();
                            }

                            if (this.shouldBlink
                                && mc.field_71439_g.field_70737_aN > 0
                                && blinkDistanceCheck
                                && this.maxBlinkTicks.getValue() > 0) {
                                BlinkUtil.blink(
                                    event,
                                    true,
                                    true,
                                    p -> p instanceof S12PacketEntityVelocity
                                        || p instanceof C03PacketPlayer
                                        || p instanceof C02PacketUseEntity
                                        || p instanceof C08PacketPlayerBlockPlacement
                                        || p instanceof C07PacketPlayerDigging,
                                    Integer.MAX_VALUE,
                                    null
                                );
                                if (!this.lastBlinkState) {
                                    this.debugMessage("Blinking");
                                }

                                this.lastBlinkState = true;
                            } else if (!blinkDistanceCheck && BlinkUtil.isBlinking()) {
                                BlinkUtil.unblink();
                                if (this.lastBlinkState) {
                                    this.debugMessage("Out of range,stop blink");
                                }

                                this.lastBlinkState = false;
                            }
                        } else if (currentMode.equals("Prediction")) {
                            if (packet instanceof S12PacketEntityVelocity
                                && ((S12PacketEntityVelocity)packet).func_149412_c()
                                    == mc.field_71439_g.func_145782_y()
                                && this.isValidS12Packet((S12PacketEntityVelocity)packet)) {
                                this.preShouldAttack = true;
                                this.hasReceivedVelocity = true;
                            }

                            if (packet instanceof S12PacketEntityVelocity
                                && ((S12PacketEntityVelocity)packet).func_149412_c()
                                    == mc.field_71439_g.func_145782_y()
                                && mc.field_71439_g.field_70122_E
                                && mc.field_71439_g.func_70051_ag()
                                && !mc.field_71474_y.field_74314_A.func_151470_d()) {
                                mc.field_71439_g.func_70664_aZ();
                            }

                            if (this.preShouldBlink) {
                                BlinkUtil.blink(event, true, false);
                                this.preBlinking = true;
                                if (this.preShouldAttack) {
                                    SomeUtil.runAttack(
                                        false,
                                        3.0F,
                                        this.blinkTicks.getValue(),
                                        null,
                                        true,
                                        "Packet",
                                        true,
                                        false,
                                        "Attacked",
                                        true,
                                        null,
                                        null,
                                        1.0F
                                    );
                                    this.preShouldAttack = false;
                                }
                            }

                            if (!this.preShouldBlink && this.preBlinking) {
                                BlinkUtil.unblink();
                            }
                        }
                    } else {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.pausedTicks > 0) {
                this.pausedTicks--;
            } else if (this.canWorkNow()) {
                EntityPlayer player = mc.field_71439_g;
                if (player != null) {
                    if (this.mode.getModeString().equals("Delay")) {
                        if (this.delayReverseFlag
                            && this.delayTimer.hasTimeElapsed(50L * this.delayTicks.getValue().intValue())) {
                            this.applyDelayedVelocity();
                            this.delayReverseFlag = false;
                            this.delayTickCounter = 0;
                            this.delayTimer.reset();
                        }

                        if (player.field_70737_aN == 0) {
                            this.delayPendingExplosion = false;
                            this.delayAllowNext = true;
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onJump(JumpEvent event) {
        this.hasJumpReset = mc.field_71439_g != null
            && mc.field_71439_g.field_70737_aN == 9
            && mc.field_71439_g.func_70051_ag();
    }

    @EventTarget
    public void onWorld(LoadWorldEvent event) {
        this.pausedTicks = 0;
    }

    @Override
    public void onDisabled() {
        this.hasReceivedVelocity = false;
        this.matrixBoostTimer.reset();
        this.jumpCooldownTickCounter = 0;
        this.jumpCooldownReceivedHitCounter = 0;
        if (this.mode.getModeString().equals("Delay")) {
            this.resetDelayState();
        }

        this.delayedPackets.clear();
    }

    @Override
    public void onEnabled() {
        this.hasReceivedVelocity = false;
        this.pausedTicks = 0;
        this.resetDelayState();
        this.triggeredPhases.clear();
        this.intaveClickTimes = 0;
        this.moreReduceTimes = 0;
        this.timerState = 0;
        this.boosting = false;
        this.slowing = false;
        this.previousTimerState = 0;
        this.matrixBoostTimer.reset();
        this.minemenClubCounter = 0;
        this.legitClick2Times = 0;
        this.attackStartHurtTime = 0;
        this.polarHurtTime = RandomUtil.nextInt(7, 9);
        this.jumpCooldownTickCounter = 0;
        this.jumpCooldownReceivedHitCounter = 0;
        this.delayedPackets.clear();
        this.serverSprintState = mc.field_71439_g != null && mc.field_71439_g.func_70051_ag();
    }

    private void extraJumpReset() {
        if (this.jumpReset.getValue()
            && this.shouldJumpReset(this.checkUserSprint.getValue(), true, null, false, null, false)
            && RandomUtil.nextInt(0, 99) <= this.jumpResetChance.getValue()
            && this.passedJumpCooldown()) {
            if (this.matrixJumpTest.getValue()) {
                SomeUtil.changeSprint(true, false, true);
            }

            mc.field_71439_g.func_70664_aZ();
            if (this.matrixJumpTest.getValue()) {
                mc.field_71439_g.field_70181_x = this.packetMotionY;
            }

            if (this.hasReceivedVelocity) {
                this.hasReceivedVelocity = false;
            }

            this.resetJumpCooldownCounter();
        }
    }

    private void handleLegitClick() {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            if (player.field_70737_aN == 0) {
                this.attackStartHurtTime = 0;
            } else {
                KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
                boolean blocking = player.func_70632_aY() || killAura != null && killAura.isBlocking();
                if (!this.ignoreBlocking.getValue() || !blocking) {
                    if (this.attackStartHurtTime == 0 && player.field_70737_aN > 0) {
                        this.attackStartHurtTime = player.field_70737_aN;
                    }

                    int currentHurtTimeOffset = this.attackStartHurtTime - player.field_70737_aN;
                    if (currentHurtTimeOffset < this.durationHurtTime.getValue()) {
                        if (this.clickDelayTick <= 0) {
                            Entity target = mc.field_71476_x != null ? mc.field_71476_x.field_72308_g : null;
                            if (target == null) {
                                if (this.whenFacingEnemyOnly.getValue()) {
                                    MovingObjectPosition result = RayCastUtil.rayCast(
                                        player.field_70177_z,
                                        player.field_70125_A,
                                        this.clickRange.getValue().floatValue(),
                                        0.0F
                                    );
                                    if (result != null
                                        && result.field_72308_g != null
                                        && SomeUtil.isSelected(result.field_72308_g)) {
                                        target = result.field_72308_g;
                                    }
                                } else {
                                    Entity nearest = this.getNearestEntityInRange(this.clickRange.getValue());
                                    if (nearest != null && SomeUtil.isSelected(nearest)) {
                                        target = nearest;
                                    }
                                }
                            }

                            if (target != null) {
                                int hits = RandomUtil.nextInt(1, 20);
                                hits = Math.min(hits, 2);
                                boolean keepSprint = this.modifyMotionWhenClick.getValue()
                                    && this.makeVanillaAttackNotStopSprint.getValue();
                                SomeUtil.runAttack(
                                    keepSprint,
                                    this.clickRange.getValue(),
                                    hits,
                                    target,
                                    true,
                                    this.swingMode.getModeString(),
                                    true,
                                    this.debugMessage.getValue(),
                                    "Attacked",
                                    false,
                                    null,
                                    null,
                                    this.clickChancePerClick.getValue()
                                );
                                if (this.clickDelayTicks.getValue() > 0) {
                                    this.clickDelayTick = this.clickDelayTicks.getValue();
                                }

                                if (this.modifyMotionWhenClick.getValue()) {
                                    SomeUtil.reduceXZ(this.modifyMotionFactor.getValue().doubleValue());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateJumpResetCooldown() {
        if (this.jumpCooldownTickCounter > 0) {
            this.jumpCooldownTickCounter--;
        }

        boolean receivedHitSelected = this.jumpCooldownMode.getValue() == 0 || this.jumpCooldownMode.getValue() == 2;
        if (receivedHitSelected
            && (
                this.mode.getModeString().equals("JumpReset")
                    || !this.mode.getModeString().equals("Intave") && this.jumpReset.getValue()
            )
            && this.jumpCooldownReceivedHitCounter < this.jumpCooldownReceivedHit.getValue()
            && mc.field_71439_g != null
            && mc.field_71439_g.field_70737_aN > 0) {
            this.jumpCooldownReceivedHitCounter++;
        }
    }

    private boolean shouldJumpReset(
        Boolean checkSprint,
        Boolean checkOnGround,
        Boolean checkMoving,
        Boolean needReceivedS12,
        Boolean needForward,
        boolean polarMode
    ) {
        if (mc.field_71439_g == null) {
            return false;
        }

        int jumpHurtTime = polarMode ? this.polarHurtTime : 9;
        return mc.field_71439_g.field_70737_aN == jumpHurtTime
            && (!Boolean.TRUE.equals(needReceivedS12) || this.hasReceivedVelocity)
            && (!Boolean.TRUE.equals(checkSprint) || mc.field_71439_g.func_70051_ag())
            && (!Boolean.TRUE.equals(checkOnGround) || mc.field_71439_g.field_70122_E)
            && (!Boolean.TRUE.equals(checkMoving) || this.isMoving())
            && (!Boolean.TRUE.equals(needForward) || mc.field_71439_g.field_70701_bs > 0.707F)
            && !mc.field_71474_y.field_74314_A.func_151470_d();
    }

    private boolean passedJumpCooldown() {
        boolean tickPassed = true;
        boolean receivedHitPassed = true;
        boolean tickSelected = this.jumpCooldownMode.getValue() == 1 || this.jumpCooldownMode.getValue() == 2;
        boolean receivedHitSelected = this.jumpCooldownMode.getValue() == 0 || this.jumpCooldownMode.getValue() == 2;
        if (tickSelected) {
            tickPassed = this.jumpCooldownTickCounter == 0;
        }

        if (receivedHitSelected) {
            receivedHitPassed = this.jumpCooldownReceivedHitCounter >= this.jumpCooldownReceivedHit.getValue();
        }

        return tickPassed && receivedHitPassed;
    }

    private void resetJumpCooldownCounter() {
        boolean tickSelected = this.jumpCooldownMode.getValue() == 1 || this.jumpCooldownMode.getValue() == 2;
        boolean receivedHitSelected = this.jumpCooldownMode.getValue() == 0 || this.jumpCooldownMode.getValue() == 2;
        if (tickSelected) {
            this.jumpCooldownTickCounter = this.jumpCooldownTick.getValue();
        }

        if (receivedHitSelected) {
            this.jumpCooldownReceivedHitCounter = 0;
        }
    }

    private Entity getNearestEntityInRange(float range) {
        EntityPlayer player = mc.field_71439_g;
        if (player != null && mc.field_71441_e != null) {
            Entity best = null;
            double bestDist = Double.MAX_VALUE;

            for (Object o : mc.field_71441_e.field_72996_f) {
                if (o instanceof Entity) {
                    Entity e = (Entity)o;
                    if (SomeUtil.isSelected(e)) {
                        double d = BackTrackUtil.getDistanceToEntityBox(e);
                        if (d <= range && d < bestDist) {
                            bestDist = d;
                            best = e;
                        }
                    }
                }
            }

            return best;
        } else {
            return null;
        }
    }

    private void handleOldGrim(S12PacketEntityVelocity packet, PacketEvent event) {
        if (!mc.field_71439_g.field_70128_L) {
            if (!(mc.field_71462_r instanceof GuiGameOver)) {
                if (mc.field_71442_b.func_178889_l() != GameType.SPECTATOR) {
                    if (!mc.field_71439_g.func_70617_f_()) {
                        if (!mc.field_71439_g.func_70027_ad() || !this.fireCheckValue.getValue()) {
                            if (!mc.field_71439_g.func_70090_H() || !this.waterCheckValue.getValue()) {
                                if (!(mc.field_71439_g.field_70143_R > 1.5) || !this.fallCheckValue.getValue()) {
                                    if (!ItemUtil.isEating() || !this.consumecheck.getValue()) {
                                        if (!this.soulSandCheck()) {
                                            if (packet.func_149412_c() == mc.field_71439_g.func_145782_y()) {
                                                double horizontalStrength = Math.sqrt(
                                                    Math.pow(
                                                            ((IAccessorS12PacketEntityVelocity)packet).getMotionX(),
                                                            2.0
                                                        )
                                                        + Math.pow(
                                                            ((IAccessorS12PacketEntityVelocity)packet).getMotionZ(),
                                                            2.0
                                                        )
                                                );
                                                if (horizontalStrength <= 1000.0) {
                                                    return;
                                                }

                                                MovingObjectPosition mouse = mc.field_71476_x;
                                                Entity targetEntity = null;
                                                if (mouse != null
                                                    && mouse.field_72313_a == MovingObjectType.ENTITY
                                                    && mouse.field_72308_g instanceof EntityLivingBase
                                                    && BackTrackUtil.getDistanceToEntityBox(mouse.field_72308_g)
                                                        <= this.killAuraRange()) {
                                                    targetEntity = mouse.field_72308_g;
                                                }

                                                if (targetEntity == null && !this.raycastValue.getValue()) {
                                                    KillAura killAura = (KillAura)Miau.moduleManager
                                                        .modules
                                                        .get(KillAura.class);
                                                    EntityLivingBase target = killAura != null
                                                        ? killAura.getTarget()
                                                        : null;
                                                    if (target != null
                                                        && BackTrackUtil.getDistanceToEntityBox(target)
                                                            <= this.grimrange.getValue().floatValue()) {
                                                        targetEntity = target;
                                                    }
                                                }

                                                boolean state = this.serverSprintState;
                                                if (targetEntity != null) {
                                                    if (!state) {
                                                        PacketUtil.sendPacketNoEvent(
                                                            new C0BPacketEntityAction(
                                                                mc.field_71439_g,
                                                                net.minecraft.network.play.client.C0BPacketEntityAction.Action.START_SPRINTING
                                                            )
                                                        );
                                                    }

                                                    for (int i = 0; i < this.attackCountValue.getValue(); i++) {
                                                        mc.func_147114_u()
                                                            .func_147298_b()
                                                            .func_179290_a(new C0APacketAnimation());
                                                        mc.func_147114_u()
                                                            .func_147298_b()
                                                            .func_179290_a(
                                                                new C02PacketUseEntity(targetEntity, Action.ATTACK)
                                                            );
                                                    }

                                                    if (!state) {
                                                        PacketUtil.sendPacketNoEvent(
                                                            new C0BPacketEntityAction(
                                                                mc.field_71439_g,
                                                                net.minecraft.network.play.client.C0BPacketEntityAction.Action.STOP_SPRINTING
                                                            )
                                                        );
                                                    }

                                                    this.velX = ((IAccessorS12PacketEntityVelocity)packet).getMotionX();
                                                    this.velY = ((IAccessorS12PacketEntityVelocity)packet).getMotionY();
                                                    this.velZ = ((IAccessorS12PacketEntityVelocity)packet).getMotionZ();
                                                    event.setCancelled(true);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private float killAuraRange() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        return killAura != null ? killAura.attackRange.getValue() : 3.0F;
    }

    private void applyDelayedVelocity() {
        boolean shouldPerformSpecialJumpReset = false;
        Iterator<Entry<Packet<?>, Long>> it = this.delayedPackets.entrySet().iterator();

        while (it.hasNext()) {
            Entry<Packet<?>, Long> entry = it.next();
            if (entry.getKey() instanceof S12PacketEntityVelocity) {
                this.applyVelocityReduction((S12PacketEntityVelocity)entry.getKey());
                shouldPerformSpecialJumpReset = true;
                it.remove();
            }
        }

        if (shouldPerformSpecialJumpReset
            && mc.field_71439_g != null
            && mc.field_71439_g.field_70122_E
            && mc.field_71439_g.func_70051_ag()
            && this.jumpReset.getValue()
            && this.shouldJumpReset(this.checkUserSprint.getValue(), true, null, false, null, false)
            && RandomUtil.nextInt(0, 99) <= this.jumpResetChance.getValue()
            && this.passedJumpCooldown()) {
            mc.field_71439_g.func_70664_aZ();
            if (this.matrixJumpTest.getValue()) {
                mc.field_71439_g.field_70181_x = this.packetMotionY;
            }

            if (this.hasReceivedVelocity) {
                this.hasReceivedVelocity = false;
            }

            this.resetJumpCooldownCounter();
            this.debugMessage("Special Jump Reset triggered after delayed velocity");
        }

        if (this.delayAttackReduce.getValue()) {
            SomeUtil.runAttack(
                false,
                3.0F,
                5,
                null,
                true,
                "Packet",
                false,
                this.debugMessage.getValue(),
                "DelayAttackReduced",
                false,
                null,
                null,
                1.0F
            );
        }
    }

    private void applyVelocityReduction(S12PacketEntityVelocity packet) {
        EntityPlayer thePlayer = mc.field_71439_g;
        if (thePlayer != null) {
            double motionX = realMotionX(packet);
            double motionZ = realMotionZ(packet);
            double motionY = realMotionY(packet);
            if (this.delayHorizontal.getValue() != 0.0F) {
                motionX *= this.delayHorizontal.getValue().floatValue();
                motionZ *= this.delayHorizontal.getValue().floatValue();
            }

            if (this.delayVertical.getValue() != 0.0F) {
                motionY *= this.delayVertical.getValue().floatValue();
            }

            thePlayer.field_70159_w = motionX;
            thePlayer.field_70179_y = motionZ;
            thePlayer.field_70181_x = motionY;
        }
    }

    private void handleMatrixVelocity(S12PacketEntityVelocity packet, PacketEvent event) {
        this.hasReceivedVelocity = true;
        event.setCancelled(true);
        if (Math.abs(realMotionY(packet)) >= 0.1F) {
            mc.field_71439_g.field_70181_x = realMotionY(packet);
            this.matrixMotionYReduce = true;
            if (!this.isMoving()) {
                double reducedSpeed = Math.max(packetBpt(packet) * 0.1, SomeUtil.bpt());
                if (packetBpt(packet) > 0.0) {
                    mc.field_71439_g.field_70159_w = realMotionX(packet) / packetBpt(packet) * reducedSpeed;
                    mc.field_71439_g.field_70179_y = realMotionZ(packet) / packetBpt(packet) * reducedSpeed;
                }
            } else if (this.matrixBoost.getValue()
                && this.matrixBoostTimer.hasTimeElapsed(this.matrixBoostDelay.getValue().intValue())) {
                SomeUtil.reduceXZ(this.matrixBoostFactor.getValue().doubleValue() + 1.0);
                this.matrixBoostTimer.reset();
            }
        }
    }

    private void handleDelayVelocity(S12PacketEntityVelocity packet, PacketEvent event) {
        if (!this.delayReverseFlag
            && !this.canDelay()
            && !this.isInLiquidOrWeb()
            && !this.delayPendingExplosion
            && (!this.delayAllowNext || !this.delayFakeCheck.getValue())) {
            this.delayChanceCounter = this.delayChanceCounter % 100 + this.delayChance.getValue();
            if (this.delayChanceCounter >= 100) {
                this.delayedPackets.put(packet, System.currentTimeMillis());
                event.setCancelled(true);
                this.delayReverseFlag = true;
                this.delayActive = true;
                this.delayTimer.reset();
                return;
            }
        }

        this.applyVelocityReduction(packet);
        event.setCancelled(true);
    }

    private void handleExplosionDelay(S27PacketExplosion packet, PacketEvent event) {
        this.delayPendingExplosion = true;
        if (this.delayHorizontal.getValue() != 0.0F && this.delayVertical.getValue() != 0.0F) {
            IAccessorS27PacketExplosion explosion = (IAccessorS27PacketExplosion)packet;
            explosion.setMotionX(explosion.getMotionX() * this.delayHorizontal.getValue());
            explosion.setMotionY(explosion.getMotionY() * this.delayVertical.getValue());
            explosion.setMotionZ(explosion.getMotionZ() * this.delayHorizontal.getValue());
        } else {
            event.setCancelled(true);
        }
    }

    private void resetDelayState() {
        this.delayChanceCounter = 0;
        this.delayActive = false;
        this.delayReverseFlag = false;
        this.delayPendingExplosion = false;
        this.delayAllowNext = true;
        this.delayTickCounter = 0;
        this.delayTimer.reset();
    }

    private boolean canDelay() {
        EntityPlayer thePlayer = mc.field_71439_g;
        if (thePlayer == null) {
            return false;
        }

        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        boolean killAuraBlocking = killAura != null && killAura.isEnabled() && killAura.isBlocking();
        return thePlayer.field_70122_E && !killAuraBlocking;
    }

    private boolean isInLiquidOrWeb() {
        EntityPlayer thePlayer = mc.field_71439_g;
        return thePlayer == null
            ? false
            : thePlayer.func_70090_H() || thePlayer.func_180799_ab() || ((IAccessorEntity)thePlayer).getIsInWeb();
    }

    private boolean isValidS12Packet(S12PacketEntityVelocity packet) {
        return realMotionX(packet) != 0.0
            && realMotionY(packet) != 0.0
            && realMotionZ(packet) != 0.0
            && packet.func_149412_c() == mc.field_71439_g.func_145782_y();
    }

    private boolean canWorkNow() {
        EntityPlayer player = mc.field_71439_g;
        if (player == null) {
            return false;
        } else {
            boolean onGround = player.field_70122_E;
            boolean inAir = !onGround;
            String work = this.allowWorkWhen.getModeString();
            if (work.equals("OnlySprinting") && !mc.field_71439_g.func_70051_ag()) {
                return false;
            } else if (this.pausedTicks > 0) {
                return false;
            } else if (work.equals("OnGround+InAir")) {
                return true;
            } else if (work.equals("OnGround")) {
                return onGround;
            } else {
                return work.equals("InAir") ? inAir : true;
            }
        }
    }

    private boolean displayJumpResetChoices() {
        String currentMode = this.mode.getModeString();
        return currentMode.equals("JumpReset")
            || !currentMode.equals("Intave")
                && !currentMode.equals("IntaveSafe")
                && !currentMode.equals("PolarJump")
                && !currentMode.equals("AirJumpReset")
                && !currentMode.equals("Prediction")
                && this.jumpReset.getValue();
    }

    private boolean soulSandCheck() {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            AxisAlignedBB box = mc.field_71439_g.func_174813_aQ().func_72331_e(0.001, 0.001, 0.001);
            int minX = MathHelper.func_76128_c(box.field_72340_a);
            int maxX = MathHelper.func_76128_c(box.field_72336_d + 1.0);
            int minY = MathHelper.func_76128_c(box.field_72338_b);
            int maxY = MathHelper.func_76128_c(box.field_72337_e + 1.0);
            int minZ = MathHelper.func_76128_c(box.field_72339_c);
            int maxZ = MathHelper.func_76128_c(box.field_72334_f + 1.0);

            for (int x = minX; x < maxX; x++) {
                for (int y = minY; y < maxY; y++) {
                    for (int z = minZ; z < maxZ; z++) {
                        if (mc.field_71441_e.func_180495_p(new BlockPos(x, y, z)).func_177230_c() instanceof BlockSoulSand
                            )
                         {
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

    private void debugMessage(String message) {
        if (this.debugMessage.getValue()) {
            if (message != null) {
                ChatUtil.display("%s", message);
            }
        }
    }

    private boolean packetMotionValid() {
        return this.packetMotionX != 0.0 && this.packetMotionZ != 0.0 && this.packetMotionY != 0.0;
    }

    private String playerNowMotionOutPut(boolean x, boolean y, boolean z) {
        StringBuilder sb = new StringBuilder();
        double mx = SomeUtil.roundToPlacesIfNeeded(mc.field_71439_g.field_70159_w);
        double my = SomeUtil.roundToPlacesIfNeeded(mc.field_71439_g.field_70181_x);
        double mz = SomeUtil.roundToPlacesIfNeeded(mc.field_71439_g.field_70179_y);
        if (x) {
            sb.append(mx);
        }

        if (y) {
            if (sb.length() > 0) {
                sb.append(" ");
            }

            sb.append(my);
        }

        if (z) {
            if (sb.length() > 0) {
                sb.append(" ");
            }

            sb.append(mz);
        }

        return sb.toString();
    }

    private boolean knockBackIsNegated(double xMotion, double zMotion) {
        double motionX = mc.field_71439_g.field_70159_w;
        double motionZ = mc.field_71439_g.field_70179_y;
        boolean isXNegated = Math.signum(motionX) != Math.signum(xMotion);
        boolean isZNegated = Math.signum(motionZ) != Math.signum(zMotion);
        return isXNegated && isZNegated;
    }

    private void stopIntaveBlink() {
        if (this.shouldBlink && BlinkUtil.isBlinking()) {
            SomeUtil.runAttack(false, 3.0F, 1, null, true, "Packet", true, false, "Attacked", true, null, null, 1.0F);
            BlinkUtil.unblink();
            this.shouldBlink = false;
            this.lastBlinkState = false;
            this.debugMessage("Unblink | " + mc.field_71439_g.field_70737_aN);
        }
    }

    private double getDistance(EntityLivingBase target) {
        return BackTrackUtil.getDistanceToEntityBox(target);
    }

    private void intaveReduce(int phase, boolean safe) {
        if (!safe || this.globalTarget instanceof EntityPlayer) {
            this.stopIntaveBlink();
            if (!this.knockBackIsNegated(this.packetMotionX, this.packetMotionZ) || !this.onlyWhenNeed.getValue()) {
                canCancelHitSlow = false;
                if (!this.intaveReversed
                    && this.intaveReduceTimes == 1
                    && mc.field_71439_g.field_70737_aN >= 1
                    && mc.field_71439_g.field_70737_aN <= 9) {
                    if (this.knockBackIsNegated(this.packetMotionX, this.packetMotionZ)) {
                        return;
                    }

                    if (SomeUtil.bps() > 2.805) {
                        if (SomeUtil.calculateAngleDifference() > 15.0) {
                            SomeUtil.setBPSTo(-Math.min(5.612, SomeUtil.bps() * 0.6));
                        }

                        this.intaveReversed = true;
                        this.debugMessage("IntaveReverse | " + mc.field_71439_g.field_70737_aN);
                    }
                } else if (mc.field_71439_g.field_70737_aN != 10) {
                    this.intaveReduceTimes++;
                } else {
                    this.intaveReduceTimes = 1;
                }

                if (this.intaveReduceTimes <= 5 || !this.intaveReversed) {
                    switch (phase) {
                        case 0:
                            SomeUtil.runAttack(
                                false, 3.0F, 1, null, true, "Packet", false, false, "Attacked", true, null, null, 1.0F
                            );
                            this.debugMessage("IntaveReduce | " + mc.field_71439_g.field_70737_aN);
                            break;
                        case 1:
                            if (!this.getTriggeredPhase(1)) {
                                if (this.extraC0APerReduce.getValue()) {
                                    for (int i = 0; i < this.extraPacketCount.getValue(); i++) {
                                        PacketUtil.sendPacket(new C0APacketAnimation());
                                    }
                                }

                                SomeUtil.reduceXZ(0.6);
                                this.debugMessage("IntaveReduce | " + mc.field_71439_g.field_70737_aN);
                                this.intaveReduceTrigger(1);
                                return;
                            }

                            if (!this.getTriggeredPhase(4)) {
                                if (this.extraC0APerReduce.getValue()) {
                                    for (int i = 0; i < this.extraPacketCount.getValue(); i++) {
                                        PacketUtil.sendPacket(new C0APacketAnimation());
                                    }
                                }

                                if (SomeUtil.calculateAngleDifference() < 15.0) {
                                    SomeUtil.reduceXZ(1.5);
                                } else {
                                    SomeUtil.reduceXZ(0.6);
                                }

                                this.debugMessage("IntaveReduce | " + mc.field_71439_g.field_70737_aN);
                                this.intaveReduceTrigger(4);
                                return;
                            }

                            if (this.moreReduceTimes < this.maxMoreReduce.getValue() && this.moreReduce.getValue()) {
                                this.moreReduceTimes++;
                                SomeUtil.reduceXZ(this.getMoreReduceFactor(this.moreReduceTimes));
                            }
                            break;
                        case 2:
                            if (!this.getTriggeredPhase(2)) {
                                if (this.extraC0APerReduce.getValue()) {
                                    for (int i = 0; i < this.extraPacketCount.getValue(); i++) {
                                        PacketUtil.sendPacket(new C0APacketAnimation());
                                    }
                                }

                                SomeUtil.reduceXZ(0.36);
                                this.debugMessage("IntaveReduce | " + mc.field_71439_g.field_70737_aN);
                                this.intaveReduceTrigger(2);
                                return;
                            }

                            if (!this.getTriggeredPhase(5)) {
                                if (this.extraC0APerReduce.getValue()) {
                                    for (int i = 0; i < this.extraPacketCount.getValue(); i++) {
                                        PacketUtil.sendPacket(new C0APacketAnimation());
                                    }
                                }

                                if (SomeUtil.calculateAngleDifference() < 15.0) {
                                    SomeUtil.reduceXZ(1.5);
                                } else {
                                    SomeUtil.reduceXZ(0.6);
                                }

                                this.debugMessage("IntaveReduce | " + mc.field_71439_g.field_70737_aN);
                                this.intaveReduceTrigger(5);
                                return;
                            }

                            if (this.moreReduceTimes < this.maxMoreReduce.getValue() && this.moreReduce.getValue()) {
                                this.moreReduceTimes++;
                                SomeUtil.reduceXZ(this.getMoreReduceFactor(this.moreReduceTimes));
                            }
                            break;
                        case 3:
                            if (!this.getTriggeredPhase(3)) {
                                if (this.extraC0APerReduce.getValue()) {
                                    for (int i = 0; i < this.extraPacketCount.getValue(); i++) {
                                        PacketUtil.sendPacket(new C0APacketAnimation());
                                    }
                                }

                                SomeUtil.reduceXZ(0.216);
                                this.debugMessage("IntaveReduce | " + mc.field_71439_g.field_70737_aN);
                                this.intaveReduceTrigger(3);
                                return;
                            }

                            if (!this.getTriggeredPhase(6)) {
                                if (this.extraC0APerReduce.getValue()) {
                                    for (int i = 0; i < this.extraPacketCount.getValue(); i++) {
                                        PacketUtil.sendPacket(new C0APacketAnimation());
                                    }
                                }

                                if (SomeUtil.calculateAngleDifference() < 15.0) {
                                    SomeUtil.reduceXZ(1.5);
                                } else {
                                    SomeUtil.reduceXZ(0.6);
                                }

                                this.debugMessage("IntaveReduce | " + mc.field_71439_g.field_70737_aN);
                                this.intaveReduceTrigger(6);
                                return;
                            }

                            if (this.moreReduceTimes < this.maxMoreReduce.getValue() && this.moreReduce.getValue()) {
                                this.moreReduceTimes++;
                                SomeUtil.reduceXZ(this.getMoreReduceFactor(this.moreReduceTimes));
                            }
                    }
                }
            } else if (this.intaveReversed && this.intaveReduceTimes >= 2 && this.intaveReduceTimes <= 4) {
                canCancelHitSlow = false;
                SomeUtil.reduceXZ(0.6);
            } else {
                KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
                KeepSprint keepSprint = (KeepSprint)Miau.moduleManager.modules.get(KeepSprint.class);
                boolean keepSprintModule = keepSprint != null && keepSprint.isEnabled();
                boolean killAuraKeepSprint = killAura != null && killAura.isEnabled();
                if (!keepSprintModule && !killAuraKeepSprint) {
                    canCancelHitSlow = true;
                }
            }
        }
    }

    private double getMoreReduceFactor(int reduceCount) {
        switch (reduceCount) {
            case 1:
                return 0.8333333333333334;
            case 2:
                return 0.75;
            default:
                double baseFactor = 0.7;
                double reduction = (reduceCount - 3) * 0.05;
                return Math.max(0.0, baseFactor - reduction);
        }
    }

    private boolean getTriggeredPhase(int phase) {
        switch (phase) {
            case 1:
                return this.triggeredPhases.contains(NewVelocity.IntavePhase.PHASE_1);
            case 2:
                return this.triggeredPhases.contains(NewVelocity.IntavePhase.PHASE_2);
            case 3:
                return this.triggeredPhases.contains(NewVelocity.IntavePhase.PHASE_3);
            case 4:
                return this.triggeredPhases.contains(NewVelocity.IntavePhase.PHASE_4);
            case 5:
                return this.triggeredPhases.contains(NewVelocity.IntavePhase.PHASE_5);
            case 6:
                return this.triggeredPhases.contains(NewVelocity.IntavePhase.PHASE_6);
            default:
                return false;
        }
    }

    private void intaveReduceTrigger(int phase) {
        switch (phase) {
            case 1:
                this.triggeredPhases.add(NewVelocity.IntavePhase.PHASE_1);
                break;
            case 2:
                this.triggeredPhases.add(NewVelocity.IntavePhase.PHASE_2);
                break;
            case 3:
                this.triggeredPhases.add(NewVelocity.IntavePhase.PHASE_3);
                break;
            case 4:
                this.triggeredPhases.add(NewVelocity.IntavePhase.PHASE_4);
                break;
            case 5:
                this.triggeredPhases.add(NewVelocity.IntavePhase.PHASE_5);
                break;
            case 6:
                this.triggeredPhases.add(NewVelocity.IntavePhase.PHASE_6);
        }
    }

    private boolean isMoving() {
        return mc.field_71439_g != null
            && (mc.field_71439_g.field_70701_bs != 0.0F || mc.field_71439_g.field_70702_br != 0.0F);
    }

    private static boolean isAttackPacket(Packet<?> packet) {
        return packet instanceof C02PacketUseEntity && ((C02PacketUseEntity)packet).func_149565_c() == Action.ATTACK;
    }

    private static boolean isSwingPacket(Packet<?> packet) {
        return packet instanceof C0APacketAnimation;
    }

    private static boolean isAttackPacketAndSwingPacket(Packet<?> packet) {
        return isAttackPacket(packet) || isSwingPacket(packet);
    }

    private static double realMotionX(S12PacketEntityVelocity packet) {
        return ((IAccessorS12PacketEntityVelocity)packet).getMotionX() / 8000.0;
    }

    private static double realMotionY(S12PacketEntityVelocity packet) {
        return ((IAccessorS12PacketEntityVelocity)packet).getMotionY() / 8000.0;
    }

    private static double realMotionZ(S12PacketEntityVelocity packet) {
        return ((IAccessorS12PacketEntityVelocity)packet).getMotionZ() / 8000.0;
    }

    private static double packetBpt(S12PacketEntityVelocity packet) {
        return Math.hypot(realMotionX(packet), realMotionZ(packet));
    }

    private enum IntavePhase {
        PHASE_1,
        PHASE_2,
        PHASE_3,
        PHASE_4,
        PHASE_5,
        PHASE_6;
    }
}
