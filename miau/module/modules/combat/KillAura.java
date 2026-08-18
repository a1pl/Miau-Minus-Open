package miau.module.modules.combat;

import com.google.common.base.CaseFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import miau.Miau;
import miau.enums.BlinkModules;
import miau.event.EventManager;
import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.CancelUseEvent;
import miau.event.impl.HitBlockEvent;
import miau.event.impl.JumpEvent;
import miau.event.impl.LeftClickMouseEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.PostRaytraceEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.RightClickMouseEvent;
import miau.event.impl.StrafeEvent;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.management.RotationState;
import miau.mixin.IAccessorPlayerControllerMP;
import miau.module.Module;
import miau.module.modules.combat.killaura.autoblocks.AutoBlockMode;
import miau.module.modules.combat.killaura.autoblocks.FakeAutoBlock;
import miau.module.modules.combat.killaura.autoblocks.GrimAC112AutoBlock;
import miau.module.modules.combat.killaura.autoblocks.GrimAC18AutoBlock;
import miau.module.modules.combat.killaura.autoblocks.InteractAutoBlock;
import miau.module.modules.combat.killaura.autoblocks.LegitAutoBlock;
import miau.module.modules.combat.killaura.autoblocks.NoneAutoBlock;
import miau.module.modules.combat.killaura.autoblocks.VanillaAutoBlock;
import miau.module.modules.combat.killaura.autoblocks.WatchdogAutoBlock;
import miau.module.modules.combat.killaura.render.TargetRenderer;
import miau.module.modules.combat.killaura.rotation.LegitRotation;
import miau.module.modules.combat.killaura.rotation.LockViewRotation;
import miau.module.modules.combat.killaura.rotation.PolarRotation;
import miau.module.modules.combat.killaura.rotation.RotationMode;
import miau.module.modules.combat.killaura.rotation.SilentRotation;
import miau.module.modules.combat.killaura.target.AttackData;
import miau.module.modules.combat.killaura.target.LastAttackData;
import miau.module.modules.combat.killaura.target.TargetManager;
import miau.module.modules.movement.NoSlow;
import miau.module.modules.player.AutoBlockIn;
import miau.module.modules.player.AutoHead;
import miau.module.modules.player.BedNuker;
import miau.module.modules.player.Scaffold;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.PercentProperty;
import miau.util.client.ChatUtil;
import miau.util.client.KeyBindUtil;
import miau.util.math.RandomUtil;
import miau.util.network.PacketUtil;
import miau.util.player.ItemUtil;
import miau.util.player.MoveUtil;
import miau.util.player.PlayerUtil;
import miau.util.player.RayCastUtil;
import miau.util.player.RotationUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;

public class KillAura extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final DecimalFormat df = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));
    private final TimerUtil timer = new TimerUtil();
    private AttackData target = null;
    private TargetRenderer targetRenderer;
    private final TargetManager targetManager = new TargetManager(this);
    public final List<RotationMode> rotationModes = new ArrayList<>();
    public int switchTick = 0;
    public boolean hitRegistered = false;
    public boolean blockingState = false;
    public boolean isBlocking = false;
    public boolean fakeBlockState = false;
    public boolean blinkReset = false;
    public boolean rightHoldActive = false;
    public long attackDelayMS = 0L;
    public int blockTick = 0;
    public int watchdogTick = 0;
    public boolean cancelAttack = false;
    public int ticksSinceVelocity = 0;
    public double expandRange = 0.0;
    public final ModeProperty mode;
    public final IntProperty switchDelay;
    public final ModeProperty autoBlock;
    public final ModeProperty rotations;
    public final ModeProperty debugLog;
    public final ModeProperty moveFix;
    public final BooleanProperty rayCast;
    public final FloatProperty cps;
    public final FloatProperty autoBlockCps;
    public final FloatProperty swingRange;
    public final PercentProperty smoothing;
    public final IntProperty angleStep;
    public int lastTickProcessed = 0;
    public final List<AutoBlockMode> autoBlockModes = new ArrayList<>();
    public final BooleanProperty autoBlockRequirePress;
    public final BooleanProperty preventServersideBlocking;
    public final BooleanProperty fixNoSlowFlag;
    public final ModeProperty sort;
    public final FloatProperty attackRange;
    public final BooleanProperty throughWalls;
    public final BooleanProperty whileScaffold;
    public final BooleanProperty requirePress;
    public final BooleanProperty allowMining;
    public final ModeProperty showTarget;
    public final BooleanProperty targetPlayers = new BooleanProperty("target-players", true);
    public final BooleanProperty targetInvisibles = new BooleanProperty(
        "target-invisibles", false, this.targetPlayers::getValue
    );
    public final BooleanProperty targetBosses = new BooleanProperty("target-bosses", false);
    public final BooleanProperty targetMobs = new BooleanProperty("target-mobs", false);
    public final BooleanProperty targetAnimals = new BooleanProperty("target-animals", false);
    public final BooleanProperty targetGolems = new BooleanProperty("target-golems", false);
    public final BooleanProperty targetSilverfish = new BooleanProperty("target-silverfish", false);
    public final BooleanProperty targetTeams = new BooleanProperty("target-teams", true);
    public final Map<Integer, LastAttackData> targetMap = new HashMap<>();

    private long getAttackDelay() {
        float min = this.cps.getValue();
        float max = this.cps.getSecondValue();
        if (this.isBlocking) {
            min = this.autoBlockCps.getValue();
            max = this.autoBlockCps.getSecondValue();
        }

        return 1000L / RandomUtil.nextLong((int)min, (int)max);
    }

    private boolean performAttack(float yaw, float pitch) {
        if (!Miau.playerStateManager.digging && !Miau.playerStateManager.placing) {
            if (this.isPlayerBlocking() && this.autoBlock.getValue() != 1) {
                return false;
            }

            if (this.shouldDelayHit()) {
                return false;
            }

            if (this.attackDelayMS > 0L) {
                return false;
            }

            mc.field_71439_g.func_71038_i();
            MovingObjectPosition rayCastPos = null;
            boolean rayCastHit = false;
            boolean useRaycast = this.rayCast.getValue();
            if (this.rotations.getValue() != 0) {
                if (this.throughWalls.getValue()) {
                    rayCastPos = RayCastUtil.getEntityIntercept(
                        this.target.getEntity(), yaw, pitch, this.attackRange.getValue().floatValue()
                    );
                } else {
                    rayCastPos = RayCastUtil.rayCast(yaw, pitch, this.attackRange.getValue().floatValue());
                }

                if (rayCastPos != null && rayCastPos.field_72308_g == this.target.getEntity()) {
                    rayCastHit = true;
                } else if (rayCastPos != null && rayCastPos.field_72313_a == MovingObjectType.ENTITY) {
                    if (!(rayCastPos.field_72308_g instanceof EntityFireball)
                        && !(rayCastPos.field_72308_g instanceof EntityItemFrame)
                        && rayCastPos.field_72308_g instanceof EntityLivingBase
                        && this.targetManager.isValid((EntityLivingBase)rayCastPos.field_72308_g)) {
                        this.target = new AttackData((EntityLivingBase)rayCastPos.field_72308_g);
                    }

                    rayCastHit = true;
                }
            } else if (useRaycast) {
                if (this.throughWalls.getValue()) {
                    rayCastPos = RayCastUtil.getEntityIntercept(
                        this.target.getEntity(), yaw, pitch, this.attackRange.getValue().floatValue()
                    );
                } else {
                    rayCastPos = RayCastUtil.rayCast(yaw, pitch, this.attackRange.getValue().floatValue());
                }

                if (rayCastPos != null && rayCastPos.field_72308_g == this.target.getEntity()) {
                    rayCastHit = true;
                } else if (rayCastPos != null && rayCastPos.field_72313_a == MovingObjectType.ENTITY) {
                    if (!(rayCastPos.field_72308_g instanceof EntityFireball)
                        && !(rayCastPos.field_72308_g instanceof EntityItemFrame)
                        && rayCastPos.field_72308_g instanceof EntityLivingBase
                        && this.targetManager.isValid((EntityLivingBase)rayCastPos.field_72308_g)) {
                        this.target = new AttackData((EntityLivingBase)rayCastPos.field_72308_g);
                    }

                    rayCastHit = true;
                }
            } else if (mc.field_71439_g.func_70032_d(this.target.getEntity()) <= this.attackRange.getValue()) {
                rayCastHit = true;
            }

            if ((this.rotations.getValue() != 0 || !this.targetManager.isBoxInAttackRange(this.target.getBox()))
                && !rayCastHit) {
                return false;
            }

            boolean wasBlockingBefore = this.isPlayerBlocking();
            if (wasBlockingBefore) {
                this.stopBlock();
            }

            AttackEvent event = new AttackEvent(this.target.getEntity());
            EventManager.call(event);
            ((IAccessorPlayerControllerMP)mc.field_71442_b).callSyncCurrentPlayItem();
            PacketUtil.sendPacket(new C02PacketUseEntity(this.target.getEntity(), Action.ATTACK));
            if (mc.field_71442_b.func_178889_l() != GameType.SPECTATOR) {
                PlayerUtil.attackEntity(this.target.getEntity());
            }

            if (wasBlockingBefore) {
                this.sendUseItem();
            }

            LastAttackData lastAttack = this.targetMap.get(this.target.getEntity().func_145782_y());
            if (lastAttack == null) {
                this.targetMap
                    .put(
                        this.target.getEntity().func_145782_y(),
                        new LastAttackData(this.getDamage(this.target.getEntity()))
                    );
            } else {
                lastAttack.reset(true, this.getDamage(this.target.getEntity()));
            }

            this.hitRegistered = true;
            return true;
        } else {
            return false;
        }
    }

    public void sendUseItem() {
        ((IAccessorPlayerControllerMP)mc.field_71442_b).callSyncCurrentPlayItem();
        this.startBlock(mc.field_71439_g.func_70694_bm());
    }

    public void startBlock(ItemStack itemStack) {
        PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(itemStack));
        mc.field_71439_g.func_71008_a(itemStack, itemStack.func_77988_m());
        this.blockingState = true;
    }

    public void stopBlock() {
        PacketUtil.sendPacket(
            new C07PacketPlayerDigging(
                net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                BlockPos.field_177992_a,
                EnumFacing.DOWN
            )
        );
        mc.field_71439_g.func_71034_by();
        this.blockingState = false;
    }

    public void setRightHold(boolean pressed) {
        int useKey = mc.field_71474_y.field_74313_G.func_151463_i();
        if (pressed) {
            KeyBindUtil.setKeyBindState(useKey, true);
            this.rightHoldActive = true;
        } else if (this.rightHoldActive) {
            KeyBindUtil.updateKeyState(useKey);
            this.rightHoldActive = false;
        }
    }

    private void interactAttack(float yaw, float pitch) {
        this.interactAttack(yaw, pitch, true);
    }

    private void interactAttack(float yaw, float pitch, boolean sendInteractAt) {
        if (this.target != null) {
            Vec3 eyePos = mc.field_71439_g.func_174824_e(1.0F);
            Vec3 lookVec = RayCastUtil.getVectorForRotation(pitch, yaw);
            Vec3 targetPos = eyePos.func_72441_c(
                lookVec.field_72450_a * 8.0, lookVec.field_72448_b * 8.0, lookVec.field_72449_c * 8.0
            );
            MovingObjectPosition mop = this.target.getBox().func_72327_a(eyePos, targetPos);
            if (mop != null) {
                ((IAccessorPlayerControllerMP)mc.field_71442_b).callSyncCurrentPlayItem();
                if (sendInteractAt) {
                    PacketUtil.sendPacket(
                        new C02PacketUseEntity(
                            this.target.getEntity(),
                            new Vec3(
                                mop.field_72307_f.field_72450_a - this.target.getX(),
                                mop.field_72307_f.field_72448_b - this.target.getY(),
                                mop.field_72307_f.field_72449_c - this.target.getZ()
                            )
                        )
                    );
                }

                PacketUtil.sendPacket(new C02PacketUseEntity(this.target.getEntity(), Action.INTERACT));
                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.field_71439_g.func_70694_bm()));
                mc.field_71439_g
                    .func_71008_a(mc.field_71439_g.func_70694_bm(), mc.field_71439_g.func_70694_bm().func_77988_m());
                this.blockingState = true;
            }
        }
    }

    private boolean canAttack() {
        if (mc.field_71462_r instanceof GuiContainer) {
            return false;
        } else if (((IAccessorPlayerControllerMP)mc.field_71442_b).getIsHittingBlock()) {
            return false;
        } else if ((ItemUtil.isEating() || ItemUtil.isUsingBow()) && PlayerUtil.isUsingItem()) {
            return false;
        } else {
            AutoHead autoHead = (AutoHead)Miau.moduleManager.modules.get(AutoHead.class);
            if (autoHead.isEnabled() && autoHead.isHealing()) {
                return false;
            } else {
                BedNuker bedNuker = (BedNuker)Miau.moduleManager.modules.get(BedNuker.class);
                AutoBlockIn autoBlockIn = (AutoBlockIn)Miau.moduleManager.modules.get(AutoBlockIn.class);
                if (bedNuker.isEnabled() && bedNuker.isReady()) {
                    return false;
                } else if (!this.whileScaffold.getValue() && Miau.moduleManager.modules.get(Scaffold.class).isEnabled()
                    )
                 {
                    return false;
                } else if (autoBlockIn.isEnabled()) {
                    return false;
                } else {
                    return this.requirePress.getValue()
                        ? PlayerUtil.isAttacking()
                        : !this.allowMining.getValue()
                            || !mc.field_71476_x.field_72313_a.equals(MovingObjectType.BLOCK)
                            || !PlayerUtil.isAttacking();
                }
            }
        }
    }

    private boolean canAutoBlock() {
        return !ItemUtil.isHoldingSword() ? false : !this.autoBlockRequirePress.getValue() || PlayerUtil.isUsingItem();
    }

    public AttackData getAttackData() {
        return this.target;
    }

    public int findEmptySlot(int currentSlot) {
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot && mc.field_71439_g.field_71071_by.func_70301_a(i) == null) {
                return i;
            }
        }

        for (int i = 0; i < 9; i++) {
            if (i != currentSlot) {
                ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
                if (stack != null && !stack.func_82837_s()) {
                    return i;
                }
            }
        }

        return Math.floorMod(currentSlot - 1, 9);
    }

    public int findSwordSlot(int currentSlot) {
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot) {
                ItemStack item = mc.field_71439_g.field_71071_by.func_70301_a(i);
                if (item != null && item.func_77973_b() instanceof ItemSword) {
                    return i;
                }
            }
        }

        return -1;
    }

    public KillAura() {
        super("KillAura", false);
        this.autoBlockModes.add(new NoneAutoBlock(this));
        this.autoBlockModes.add(new VanillaAutoBlock(this));
        this.autoBlockModes.add(new InteractAutoBlock(this));
        this.autoBlockModes.add(new LegitAutoBlock(this));
        this.autoBlockModes.add(new FakeAutoBlock(this));
        this.autoBlockModes.add(new WatchdogAutoBlock(this));
        this.autoBlockModes.add(new GrimAC18AutoBlock(this));
        this.autoBlockModes.add(new GrimAC112AutoBlock(this));
        this.rotationModes.add(new LegitRotation(this));
        this.rotationModes.add(new SilentRotation(this));
        this.rotationModes.add(new LockViewRotation(this));
        this.rotationModes.add(new PolarRotation(this));
        this.lastTickProcessed = 0;
        this.mode = new ModeProperty("Mode", 0, new String[]{"SINGLE", "SWITCH"});
        this.switchDelay = new IntProperty("switch-delay", 150, 0, 1000);
        this.sort = new ModeProperty("sort", 0, new String[]{"HEALTH", "HURT-TIME"});
        this.attackRange = new FloatProperty("attack-range", 3.0F, 3.0F, 6.0F);
        this.swingRange = new FloatProperty("swing-range", 3.5F, 3.0F, 6.0F);
        this.cps = new FloatProperty("aps", 14.0F, 14.0F, 1.0F, 20.0F);
        this.rayCast = new BooleanProperty("ray-cast", false);
        this.throughWalls = new BooleanProperty("through-walls", true);
        this.whileScaffold = new BooleanProperty("while-scaffold", false);
        this.requirePress = new BooleanProperty("require-press", false);
        this.allowMining = new BooleanProperty("allow-mining", true);
        this.autoBlock = new ModeProperty(
            "auto-block",
            0,
            new String[]{"NONE", "VANILLA", "INTERACT", "LEGIT", "FAKE", "WATCHDOG", "GRIMAC-1.8", "GRIMAC-1.12"}
        );
        this.autoBlockRequirePress = new BooleanProperty("autoblock-require-press", false);
        this.preventServersideBlocking = new BooleanProperty("prevent-serverside-blocking", false);
        this.fixNoSlowFlag = new BooleanProperty("fix-noslow-flag", false, () -> this.autoBlock.getValue() == 5);
        this.autoBlockCps = new FloatProperty("autoblock-aps", 8.0F, 10.0F, 1.0F, 10.0F);
        this.rotations = new ModeProperty("rotations", 2, new String[]{"NONE", "LEGIT", "SILENT", "LOCK_VIEW", "POLAR"});
        this.smoothing = new PercentProperty("smoothing", 0);
        this.angleStep = new IntProperty("angle-step", 90, 30, 180);
        this.moveFix = new ModeProperty("move-fix", 0, new String[]{"OFF", "Normal"});
        this.showTarget = new ModeProperty("show-target", 0, new String[]{"NONE", "BOX", "SIGMA_RING"});
        this.debugLog = new ModeProperty("debug-log", 0, new String[]{"NONE", "HEALTH"});
    }

    public EntityLivingBase getTarget() {
        return this.target != null ? this.target.getEntity() : null;
    }

    public boolean isInRange(EntityLivingBase entity) {
        return this.targetManager.isInRange(entity);
    }

    private boolean shouldDelayHit() {
        return false;
    }

    public boolean isAttackAllowed() {
        Scaffold scaffold = (Scaffold)Miau.moduleManager.modules.get(Scaffold.class);
        if (!this.whileScaffold.getValue() && scaffold.isEnabled()) {
            return false;
        } else {
            return this.shouldDelayHit()
                ? false
                : !this.requirePress.getValue()
                    || KeyBindUtil.isKeyDown(mc.field_71474_y.field_74312_F.func_151463_i());
        }
    }

    public boolean shouldAutoBlock() {
        return this.isPlayerBlocking() && this.isBlocking
            ? (
                    this.autoBlock.getValue() == 2
                        || this.autoBlock.getValue() == 3
                        || this.autoBlock.getValue() == 4
                        || this.autoBlock.getValue() == 5
                        || this.autoBlock.getValue() == 6
                        || this.autoBlock.getValue() == 7
                )
                && !mc.field_71439_g.func_70090_H()
                && !mc.field_71439_g.func_180799_ab()
            : false;
    }

    public boolean isBlocking() {
        return this.fakeBlockState && ItemUtil.isHoldingSword();
    }

    public boolean isPlayerBlocking() {
        return (mc.field_71439_g.func_71039_bw() || this.blockingState) && ItemUtil.isHoldingSword();
    }

    public boolean isNoSlowAntiSwitchActive() {
        NoSlow noSlow = (NoSlow)Miau.moduleManager.modules.get(NoSlow.class);
        return noSlow.isEnabled() && noSlow.mode.getValue() == 3 && this.isPlayerBlocking();
    }

    @EventTarget(3)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.POST && this.blinkReset) {
            this.blinkReset = false;
            Miau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
            Miau.blinkManager.setBlinkState(true, BlinkModules.AUTO_BLOCK);
        }

        if (this.isEnabled() && event.getType() == EventType.PRE) {
            this.ticksSinceVelocity++;
            if (mc.field_71439_g.field_70173_aa % 20 == 0) {
                this.expandRange = 3.0 + Math.random() * 0.5;
            }

            if (this.attackDelayMS > 0L) {
                this.attackDelayMS -= 50L;
            }

            boolean attack = this.target != null && this.canAttack();
            boolean block = attack && this.canAutoBlock();
            if (!block) {
                Miau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                this.isBlocking = false;
                this.fakeBlockState = false;
                this.blockTick = 0;
            }

            if (attack) {
                boolean swap = false;
                boolean blocked = false;
                if (block) {
                    this.cancelAttack = false;
                    AutoBlockMode mode = this.autoBlockModes.get(this.autoBlock.getValue());
                    if (mode != null) {
                        swap = mode.processBlock(attack, block);
                    }

                    if (this.cancelAttack) {
                        attack = false;
                    }
                }

                boolean attacked = false;
                if (this.targetManager.isBoxInSwingRange(this.target.getBox())) {
                    if (this.rotations.getValue() != 0) {
                        float[] targetRots = RotationUtil.getRotationsWithBackup(
                            this.target.getEntity(),
                            100.0,
                            100.0,
                            event.getYaw(),
                            event.getPitch(),
                            this.attackRange.getValue().floatValue(),
                            this.throughWalls.getValue(),
                            true
                        );
                        if (targetRots == null) {
                            targetRots = RotationUtil.calculate(
                                this.target.getEntity(), true, this.attackRange.getValue().floatValue()
                            );
                        }

                        float[] lastRots = new float[]{event.getYaw(), event.getPitch()};
                        double rotSpeed = this.angleStep.getValue().intValue() + RandomUtil.nextFloat(-5.0F, 5.0F);
                        float[] rotations = lastRots;
                        RotationMode currentMode = this.rotations.getValue() > 0
                                && this.rotations.getValue() <= this.rotationModes.size()
                            ? this.rotationModes.get(this.rotations.getValue() - 1)
                            : null;
                        if (currentMode != null) {
                            rotations = currentMode.processRotations(targetRots, lastRots, rotSpeed, event);
                        }

                        float[] quantized = RotationUtil.flexRotation(
                            rotations[0], rotations[1], lastRots[0], lastRots[1]
                        );
                        event.setRotation(quantized[0], quantized[1], 1);
                        if (this.rotations.getValue() != 2) {
                            event.setPervRotation(quantized[0], 1);
                        }
                    }

                    if (attack) {
                        attacked = this.performAttack(event.getNewYaw(), event.getNewPitch());
                    }
                }

                if (swap) {
                    if (attacked) {
                        this.interactAttack(event.getNewYaw(), event.getNewPitch());
                    } else {
                        this.sendUseItem();
                    }
                }

                if (blocked) {
                    Miau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    Miau.blinkManager.setBlinkState(true, BlinkModules.AUTO_BLOCK);
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            switch (event.getType()) {
                case PRE:
                    if (this.target == null
                        || !this.targetManager.isValidTarget(this.target.getEntity())
                        || !this.targetManager.isBoxInAttackRange(this.target.getBox())
                        || !this.targetManager.isBoxInSwingRange(this.target.getBox())
                        || this.timer.hasTimeElapsed(this.switchDelay.getValue().longValue())) {
                        this.timer.reset();
                        List<EntityLivingBase> validTargets = this.targetManager.getValidTargets();
                        if (validTargets.isEmpty()) {
                            this.target = null;
                        } else {
                            this.target = this.targetManager.findBestTarget(validTargets);
                        }
                    }

                    if (this.target != null) {
                        this.target = new AttackData(this.target.getEntity());
                    }
                    break;
                case POST:
                    for (AutoBlockMode mode : this.autoBlockModes) {
                        mode.onPostUpdate();
                    }

                    if (this.isPlayerBlocking() && !mc.field_71439_g.func_70632_aY()) {
                        mc.field_71439_g
                            .func_71008_a(
                                mc.field_71439_g.func_70694_bm(), mc.field_71439_g.func_70694_bm().func_77988_m()
                            );
                    }
            }
        }
    }

    @EventTarget(4)
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND) {
            if (event.getPacket() instanceof C0BPacketEntityAction) {
                C0BPacketEntityAction packet = (C0BPacketEntityAction)event.getPacket();
                switch (packet.func_180764_b()) {
                }
            }

            if (this.preventServersideBlocking.getValue() && this.isPlayerBlocking()) {
                if (event.getPacket() instanceof C08PacketPlayerBlockPlacement) {
                    C08PacketPlayerBlockPlacement wrapper = (C08PacketPlayerBlockPlacement)event.getPacket();
                    if (wrapper.func_149574_g() != null && wrapper.func_149574_g().func_77973_b() instanceof ItemSword) {
                        event.setCancelled(true);
                    }
                } else if (event.getPacket() instanceof C07PacketPlayerDigging) {
                    C07PacketPlayerDigging wrapper = (C07PacketPlayerDigging)event.getPacket();
                    if (wrapper.func_180762_c()
                        == net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                        event.setCancelled(true);
                    }
                }
            }
        }

        if (this.isEnabled() && !event.isCancelled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            if (event.getPacket() instanceof C07PacketPlayerDigging) {
                C07PacketPlayerDigging packet = (C07PacketPlayerDigging)event.getPacket();
                if (packet.func_180762_c()
                    == net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                    this.blockingState = false;
                }
            }

            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                if (packet.func_149412_c() == mc.field_71439_g.func_145782_y()) {
                    this.ticksSinceVelocity = 0;
                }
            }

            if (event.getPacket() instanceof C09PacketHeldItemChange) {
                this.blockingState = false;
                if (this.isBlocking) {
                    mc.field_71439_g.func_71034_by();
                }
            }

            if (this.debugLog.getValue() == 1
                && mc.field_71439_g != null
                && event.getPacket() instanceof S06PacketUpdateHealth) {
                float packet = ((S06PacketUpdateHealth)event.getPacket()).func_149332_c()
                    - mc.field_71439_g.func_110143_aJ();
                if (packet != 0.0F && this.lastTickProcessed != mc.field_71439_g.field_70173_aa) {
                    this.lastTickProcessed = mc.field_71439_g.field_70173_aa;
                    ChatUtil.sendFormatted(
                        String.format(
                            "%sHealth: %s&l%s&r (&otick: %d&r)&r",
                            Miau.clientName,
                            packet > 0.0F ? "&a" : "&c",
                            df.format(packet),
                            mc.field_71439_g.field_70173_aa
                        )
                    );
                }
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (this.isEnabled()) {
            if (this.rotations.getValue() != 0 && RotationState.isActived()) {
                MoveUtil.fixMovement(RotationState.getRotationYawHead());
            }

            if (this.shouldAutoBlock()) {
                mc.field_71439_g.field_71158_b.field_78901_c = false;
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled() && RotationState.isActived() && this.target != null && this.rotations.getValue() != 0) {
            event.setYaw(RotationState.getRotationYawHead());
        }
    }

    @EventTarget
    public void onJump(JumpEvent event) {
        if (this.isEnabled() && RotationState.isActived() && this.target != null && this.rotations.getValue() != 0) {
            event.setYaw(RotationState.getRotationYawHead());
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.targetRenderer == null) {
            this.targetRenderer = new TargetRenderer(this);
        }

        this.targetRenderer.onRender(event);
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isBlocking) {
            event.setCancelled(true);
        } else if (this.isEnabled() && this.target != null && this.canAttack()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isBlocking) {
            event.setCancelled(true);
        } else if (this.isEnabled() && this.target != null && this.canAttack()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isBlocking) {
            event.setCancelled(true);
        } else if (this.isEnabled() && this.target != null && this.canAttack()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onCancelUse(CancelUseEvent event) {
        if (this.isBlocking) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onPostRaytrace(PostRaytraceEvent event) {
        if (this.shouldOverrideMouseOver()) {
            this.modifyMouseOverFromGetMouseOver(event.partialTicks);
        }
    }

    public boolean shouldOverrideMouseOver() {
        if (!this.isEnabled()) {
            return false;
        }

        if (mc.field_71439_g == null || mc.field_71441_e == null) {
            return false;
        }

        if (this.target == null || this.target.getEntity() == null) {
            return false;
        }

        if (this.target.getEntity().field_70128_L) {
            return false;
        }

        double dist = RotationUtil.distanceToEntity(this.target.getEntity());
        return dist > this.attackRange.getValue().floatValue() ? false : !(mc.field_71462_r instanceof GuiContainer);
    }

    public void modifyMouseOverFromGetMouseOver(float partialTicks) {
        if (this.target != null && this.target.getEntity() != null) {
            Entity targetEntity = this.target.getEntity();
            Entity viewEntity = mc.func_175606_aa();
            if (viewEntity != null) {
                Vec3 eyes = viewEntity.func_174824_e(partialTicks);
                Vec3 look = viewEntity.func_70676_i(partialTicks);
                double reach = this.attackRange.getValue().floatValue();
                Vec3 rayEnd = eyes.func_72441_c(
                    look.field_72450_a * reach, look.field_72448_b * reach, look.field_72449_c * reach
                );
                float border = targetEntity.func_70111_Y();
                AxisAlignedBB bb = targetEntity.func_174813_aQ().func_72314_b(border, border, border);
                MovingObjectPosition intercept = bb.func_72327_a(eyes, rayEnd);
                boolean inside = bb.func_72318_a(eyes);
                if (inside || intercept != null) {
                    Vec3 hitVec = inside
                        ? (intercept == null ? eyes : intercept.field_72307_f)
                        : intercept.field_72307_f;
                    if (!this.throughWalls.getValue()) {
                        MovingObjectPosition blockHit = mc.field_71441_e
                            .func_147447_a(eyes, hitVec, false, false, true);
                        if (blockHit != null && blockHit.field_72313_a == MovingObjectType.BLOCK) {
                            return;
                        }
                    }

                    mc.field_71476_x = new MovingObjectPosition(targetEntity, hitVec);
                    mc.field_147125_j = targetEntity;
                }
            }
        }
    }

    @Override
    public void onEnabled() {
        this.target = null;
        this.switchTick = 0;
        this.hitRegistered = false;
        this.attackDelayMS = 0L;
        this.blockTick = 0;
        this.watchdogTick = 0;
        this.rightHoldActive = false;
    }

    @Override
    public void onDisabled() {
        for (AutoBlockMode mode : this.autoBlockModes) {
            mode.onDisable();
        }

        this.targetMap.clear();
        Miau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
        this.blockingState = false;
        this.isBlocking = false;
        this.fakeBlockState = false;
    }

    @Override
    public void verifyValue(String value) {
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }

    private double getDamage(EntityLivingBase target) {
        float baseDamage = 1.0F;
        if (mc.field_71439_g.func_110148_a(SharedMonsterAttributes.field_111264_e) != null) {
            baseDamage = (float)mc.field_71439_g.func_110148_a(SharedMonsterAttributes.field_111264_e).func_111126_e();
        }

        float enchantmentBonus = 0.0F;
        if (mc.field_71439_g.func_70694_bm() != null) {
            enchantmentBonus = EnchantmentHelper.func_152377_a(
                mc.field_71439_g.func_70694_bm(),
                target != null ? target.func_70668_bt() : EnumCreatureAttribute.UNDEFINED
            );
        }

        boolean isCritical = mc.field_71439_g.field_70143_R > 0.0F
            && !mc.field_71439_g.field_70122_E
            && !mc.field_71439_g.func_70617_f_()
            && !mc.field_71439_g.func_70090_H()
            && !mc.field_71439_g.func_70644_a(Potion.field_76440_q)
            && mc.field_71439_g.field_70154_o == null;
        if (isCritical && baseDamage > 0.0F) {
            baseDamage *= 1.5F;
        }

        baseDamage += enchantmentBonus;
        return baseDamage;
    }
}
