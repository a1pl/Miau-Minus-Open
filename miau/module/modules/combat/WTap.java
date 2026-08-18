package miau.module.modules.combat;

import java.util.Random;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class WTap extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Normal", "Simple"});
    public final IntProperty chance = new IntProperty("Chance", 100, 0, 100, () -> this.mode.getValue() == 0);
    public final IntProperty delay = new IntProperty("Delay", 0, 0, 500, () -> this.mode.getValue() == 0);
    public final IntProperty targetHurtTime = new IntProperty(
        "TargetHurtTime", 0, 0, 10, () -> this.mode.getValue() == 0
    );
    public final IntProperty ownHurtTime = new IntProperty("OwnHurtTime", 0, 0, 10, () -> this.mode.getValue() == 0);
    public final IntProperty ticksUntilBlock = new IntProperty(
        "TicksUntilBlock", 0, 0, 2, () -> this.mode.getValue() == 0
    );
    public final IntProperty reSprintTicks = new IntProperty("ReSprintTicks", 1, 1, 2, () -> this.mode.getValue() == 0);
    public final IntProperty targetDistance = new IntProperty(
        "TargetDistance", 3, 0, 5, () -> this.mode.getValue() == 0
    );
    public final BooleanProperty AllowJump = new BooleanProperty("AllowJump", false, () -> this.mode.getValue() == 0);
    public final BooleanProperty ADStrafe = new BooleanProperty("ADStrafe", false, () -> this.mode.getValue() == 0);
    public final IntProperty DurationTime = new IntProperty(
        "ADStrafeDurationTick", 3, 1, 10, () -> this.ADStrafe.getValue() && this.mode.getValue() == 0
    );
    public final BooleanProperty restartForwardWhenBlockStop = new BooleanProperty("RestartForwardWhenBlockStop", false);
    public final IntProperty forwardTick = new IntProperty(
        "ForwardTick", 1, 0, 10, () -> this.restartForwardWhenBlockStop.getValue()
    );
    public final FloatProperty minEnemyRotDiffToIgnore = new FloatProperty(
        "MinRotationDiffFromEnemyToIgnore", 180.0F, 0.0F, 180.0F, () -> this.mode.getValue() == 0
    );
    public final IntProperty stopDuration = new IntProperty("StopDuration", 1, 0, 10, () -> this.mode.getValue() == 1);
    public final BooleanProperty onlyGround = new BooleanProperty("OnlyGround", false);
    public final BooleanProperty onlyMove = new BooleanProperty("OnlyMove", true);
    public final BooleanProperty onlyMoveForward = new BooleanProperty("OnlyMoveForward", true);
    public final BooleanProperty onlyWhenTargetGoesBack = new BooleanProperty("OnlyWhenTargetGoesBack", false);
    public final BooleanProperty onlyWhenNotBlocking = new BooleanProperty("OnlyWhenNotBlocking", false);
    private final Random random = new Random();
    private final TimerUtil strafeTimer = new TimerUtil();
    public int forwardTicks = 0;
    private int blockInputTicks;
    private int blockTicksElapsed = 0;
    private boolean startWaiting = false;
    private boolean blockInput = false;
    private int allowInputTicks;
    private int ticksElapsed = 0;
    private int strafeDuration = 0;
    private boolean randomSide;
    private int simpleModeTicks = 0;
    private boolean wasBlockingInput = false;

    public WTap() {
        super("WTap", false);
        this.blockInputTicks = this.randomInRange(this.ticksUntilBlock);
        this.allowInputTicks = this.randomInRange(this.reSprintTicks);
        this.randomSide = this.random.nextBoolean();
    }

    @Override
    public void onEnabled() {
        this.resetState();
    }

    @Override
    public void onDisabled() {
        this.resetState();
    }

    private void resetState() {
        this.blockInput = false;
        this.startWaiting = false;
        this.blockTicksElapsed = 0;
        this.ticksElapsed = 0;
        this.forwardTicks = 0;
        this.wasBlockingInput = false;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null) {
            if (event.getTarget() instanceof EntityLivingBase) {
                EntityLivingBase target = (EntityLivingBase)event.getTarget();
                double distance = this.getDistanceToEntityBox(target);
                if (this.shouldActivateWTap(mc.field_71439_g, target, this.mode.getModeString())) {
                    if (mc.field_71439_g.func_70051_ag() && !this.blockInput && !this.startWaiting) {
                        double delayMultiplier = 1.0
                            / (Math.abs(this.targetDistance.getValue().intValue() - distance) + 1.0);
                        this.randomSide = this.random.nextBoolean();
                        this.blockInputTicks = (int)(this.randomInRange(this.ticksUntilBlock) * delayMultiplier);
                        this.blockInput = this.blockInputTicks == 0;
                        if (!this.blockInput) {
                            this.startWaiting = true;
                        }

                        this.allowInputTicks = (int)(this.randomInRange(this.reSprintTicks) * delayMultiplier);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && event.getType() == EventType.PRE) {
            if (mc.field_71439_g.field_70737_aN >= this.ownHurtTime.getMinimum()
                && mc.field_71439_g.field_70737_aN <= this.ownHurtTime.getMaximum()) {
                KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
                EntityLivingBase auraTarget = killAura != null ? killAura.getTarget() : null;
                int currentTargetHurtTime = auraTarget == null ? -1 : auraTarget.field_70737_aN;
                EntityLivingBase pointedEntity = mc.field_147125_j instanceof EntityLivingBase
                    ? (EntityLivingBase)mc.field_147125_j
                    : null;
                if (this.wasBlockingInput && !this.blockInput && this.restartForwardWhenBlockStop.getValue()) {
                    this.forwardTicks = this.forwardTick.getValue();
                }

                this.wasBlockingInput = this.blockInput;
                if (this.forwardTicks > 0) {
                    this.forwardTicks--;
                }

                if (this.blockInput) {
                    if (this.ticksElapsed++ >= this.allowInputTicks) {
                        this.blockInput = false;
                        this.ticksElapsed = 0;
                    }
                } else if (this.startWaiting) {
                    this.blockInput = this.blockTicksElapsed++ >= this.blockInputTicks;
                    if (this.blockInput) {
                        this.startWaiting = false;
                        this.blockTicksElapsed = 0;
                    }
                }

                int hurtTimeToCheck;
                if (currentTargetHurtTime >= 0) {
                    hurtTimeToCheck = currentTargetHurtTime;
                } else {
                    if (pointedEntity == null) {
                        return;
                    }

                    hurtTimeToCheck = pointedEntity.field_70737_aN;
                }

                if (hurtTimeToCheck == 10) {
                    this.simpleModeTicks = this.stopDuration.getValue();
                }
            }
        }
    }

    private boolean shouldActivateWTap(EntityPlayerSP player, EntityLivingBase target, String mode) {
        if (this.onlyGround.getValue() && !player.field_70122_E) {
            return false;
        }

        if (!this.onlyMove.getValue()
            || MoveUtil.isMoving() && (!this.onlyMoveForward.getValue() || player.field_71158_b.field_78902_a == 0.0F)) {
            if (mode.equals("Normal")) {
                if (target.field_70737_aN < this.targetHurtTime.getMinimum()
                    || target.field_70737_aN > this.targetHurtTime.getMaximum()) {
                    return false;
                }

                if (player.field_70737_aN < this.ownHurtTime.getMinimum()
                    || player.field_70737_aN > this.ownHurtTime.getMaximum()) {
                    return false;
                }

                TimerUtil delayTimer = new TimerUtil();
                delayTimer.reset();
                if (!delayTimer.hasTimeElapsed(this.randomInRange(this.delay))) {
                    return false;
                }

                if (this.random.nextInt(100) > this.chance.getValue()) {
                    return false;
                }

                float rotationToPlayer = this.getYawToTarget(target);
                float angleDifferenceToPlayer = Math.abs(this.angleDifference(rotationToPlayer, target.field_70177_z));
                if (angleDifferenceToPlayer > this.minEnemyRotDiffToIgnore.getValue()
                    && !target.func_174813_aQ().func_72318_a(mc.field_71439_g.func_174824_e(1.0F))) {
                    return false;
                }

                if (this.onlyWhenTargetGoesBack.getValue()) {
                    Vec3 pos = new Vec3(
                        target.field_70165_t - target.field_70142_S,
                        target.field_70163_u - target.field_70137_T,
                        target.field_70161_v - target.field_70136_U
                    );
                    AxisAlignedBB box = target.func_174813_aQ()
                        .func_72317_d(pos.field_72450_a, pos.field_72448_b, pos.field_72449_c);
                    double distanceBasedOnMotion = this.getDistanceToBox(box);
                    if (distanceBasedOnMotion >= this.getDistanceToEntityBox(target)) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean shouldBlockInput() {
        if (this.onlyWhenNotBlocking.getValue() && mc.field_71439_g != null && mc.field_71439_g.func_70632_aY()) {
            return false;
        }

        if (this.mode.getModeString().equals("Normal")) {
            if (this.isEnabled() && this.blockInput) {
                EntityPlayerSP player = mc.field_71439_g;
                if (player == null) {
                    return false;
                }

                if (this.strafeDuration == 0 && this.ADStrafe.getValue()) {
                    this.strafeDuration = this.DurationTime.getValue();
                    this.strafeTimer.reset();
                    player.field_71158_b.field_78902_a = 0.0F;
                }

                if (this.strafeTimer.hasTimeElapsed(this.strafeDuration) && this.ADStrafe.getValue()) {
                    player.field_71158_b.field_78902_a = 0.0F;
                    this.strafeDuration = 0;
                } else if ((
                        mc.field_71474_y.field_74370_x.func_151470_d()
                            || mc.field_71474_y.field_74366_z.func_151470_d()
                    )
                    && this.ADStrafe.getValue()) {
                    if (mc.field_71474_y.field_74370_x.func_151470_d() && this.ADStrafe.getValue()) {
                        player.field_71158_b.field_78902_a = -1.0F;
                    } else if (mc.field_71474_y.field_74366_z.func_151470_d() && this.ADStrafe.getValue()) {
                        player.field_71158_b.field_78902_a = 1.0F;
                    }
                } else if (this.randomSide && this.ADStrafe.getValue()) {
                    player.field_71158_b.field_78902_a = -1.0F;
                } else if (this.ADStrafe.getValue()) {
                    player.field_71158_b.field_78902_a = 1.0F;
                }

                if (this.AllowJump.getValue() && player.field_70122_E) {
                    player.func_70664_aZ();
                }

                return true;
            }
        } else if (this.mode.getModeString().equals("Simple") && this.simpleModeTicks != 0) {
            this.simpleModeTicks--;
            return true;
        }

        return false;
    }

    private int randomInRange(IntProperty property) {
        int min = property.getMinimum();
        int max = property.getMaximum();
        return max <= min ? min : min + this.random.nextInt(max - min + 1);
    }

    private double getDistanceToEntityBox(Entity entity) {
        return this.getDistanceToBox(entity.func_174813_aQ());
    }

    private double getDistanceToBox(AxisAlignedBB box) {
        if (mc.field_71439_g == null) {
            return Double.MAX_VALUE;
        }

        double x = MathHelper.func_151237_a(mc.field_71439_g.field_70165_t, box.field_72340_a, box.field_72336_d);
        double y = MathHelper.func_151237_a(mc.field_71439_g.field_70163_u, box.field_72338_b, box.field_72337_e);
        double z = MathHelper.func_151237_a(mc.field_71439_g.field_70161_v, box.field_72339_c, box.field_72334_f);
        return Math.sqrt(mc.field_71439_g.func_70092_e(x, y, z));
    }

    private float angleDifference(float a, float b) {
        return MathHelper.func_76142_g(a - b);
    }

    private float getYawToTarget(EntityLivingBase target) {
        AxisAlignedBB box = target.func_174813_aQ();
        double cx = (box.field_72340_a + box.field_72336_d) / 2.0;
        double cz = (box.field_72339_c + box.field_72334_f) / 2.0;
        double dx = cx - mc.field_71439_g.field_70165_t;
        double dz = cz - mc.field_71439_g.field_70161_v;
        return (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
    }
}
