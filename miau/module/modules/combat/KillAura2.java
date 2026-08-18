package miau.module.modules.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.module.modules.misc.AntiBot;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.math.RandomUtil;
import miau.util.network.PacketUtil;
import miau.util.player.ItemUtil;
import miau.util.player.PlayerUtil;
import miau.util.player.RayCastUtil;
import miau.util.player.RotationUtil;
import miau.util.player.TeamUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;

public class KillAura2 extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final TimerUtil attackTimer = new TimerUtil();
    private final TimerUtil switchTimer = new TimerUtil();
    private EntityLivingBase target;
    private int targetIndex;
    private float lastYaw;
    private float lastPitch;
    private boolean blocking;
    private boolean wasBlocking;
    private int blockingTime;
    private int cpsValue;
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"SINGLE", "SWITCH"});
    public final IntProperty switchDelay = new IntProperty("switch-delay", 150, 0, 1000);
    public final ModeProperty sortMode = new ModeProperty(
        "sort", 0, new String[]{"HEALTH", "HURT-TIME", "DISTANCE", "YAW"}
    );
    public final FloatProperty attackRange = new FloatProperty("attack-range", 3.2F, 3.0F, 6.0F);
    public final FloatProperty swingRange = new FloatProperty("swing-range", 3.5F, 3.0F, 8.0F);
    public final FloatProperty preAimRange = new FloatProperty("preaim-range", 6.0F, 3.0F, 12.0F);
    public final FloatProperty minCps = new FloatProperty("min-cps", 10.0F, 1.0F, 20.0F);
    public final FloatProperty maxCps = new FloatProperty("max-cps", 10.0F, 1.0F, 20.0F);
    public final ModeProperty rotationMode = new ModeProperty("rotation", 1, new String[]{"NONE", "SILENT"});
    public final ModeProperty moveFix = new ModeProperty("move-fix", 0, new String[]{"OFF", "NORMAL"});
    public final FloatProperty rotationSpeed = new FloatProperty("rotation-speed", 5.0F, 0.0F, 5.0F);
    public final ModeProperty autoBlock = new ModeProperty(
        "auto-block",
        1,
        new String[]{
            "MANUAL",
            "VANILLA",
            "POST",
            "SWAP",
            "INTERACT_A",
            "INTERACT_B",
            "FAKE",
            "PARTIAL",
            "WATCHDOG",
            "GRIMAC-1.8",
            "GRIMAC-1.12"
        }
    );
    public final BooleanProperty fixNoSlowFlag = new BooleanProperty("fix-noslow-flag", true);
    public final IntProperty postDelay = new IntProperty("post-delay", 10, 1, 20);
    public final BooleanProperty hitThroughWalls = new BooleanProperty("through-walls", true);
    public final BooleanProperty rayCast = new BooleanProperty("ray-cast", true);
    public final BooleanProperty weaponOnly = new BooleanProperty("weapon-only", false);
    public final BooleanProperty targetPlayers = new BooleanProperty("target-players", true);
    public final BooleanProperty targetMobs = new BooleanProperty("target-mobs", false);
    public final BooleanProperty targetAnimals = new BooleanProperty("target-animals", false);
    public final BooleanProperty targetInvisible = new BooleanProperty("target-invisible", false);
    public final BooleanProperty targetBosses = new BooleanProperty("target-bosses", false);
    public final BooleanProperty targetGolems = new BooleanProperty("target-golems", false);
    public final BooleanProperty targetSilverfish = new BooleanProperty("target-silverfish", false);
    public final BooleanProperty targetTeams = new BooleanProperty("target-teams", true);
    public final BooleanProperty silentSwing = new BooleanProperty("silent-swing", false);

    public KillAura2() {
        super("KillAura2", false);
    }

    @Override
    public void onEnabled() {
        this.target = null;
        this.targetIndex = 0;
        this.blocking = false;
        this.wasBlocking = false;
        this.blockingTime = 0;
    }

    @Override
    public void onDisabled() {
        this.target = null;
        if (this.wasBlocking) {
            PacketUtil.sendPacket(
                new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.field_177992_a, EnumFacing.DOWN)
            );
            this.wasBlocking = false;
            this.blocking = false;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            List<EntityLivingBase> targets = this.getTargets();
            if (targets.isEmpty()) {
                this.target = null;
            } else {
                switch (this.mode.getValue()) {
                    case 0:
                        this.target = targets.get(0);
                        break;
                    case 1:
                        if (this.switchTimer.hasTimeElapsed(this.switchDelay.getValue().longValue(), true)) {
                            this.targetIndex = (this.targetIndex + 1) % targets.size();
                        }

                        if (this.targetIndex < targets.size()) {
                            this.target = targets.get(this.targetIndex);
                        } else {
                            this.target = null;
                        }
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.POST) {
                if (this.blocking
                    && this.autoBlock.getValue() == 9
                    && mc.field_71439_g.func_70694_bm() != null
                    && mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemSword) {
                    mc.field_71442_b.func_78769_a(mc.field_71439_g, mc.field_71441_e, mc.field_71439_g.func_70694_bm());
                }
            } else if (event.getType() == EventType.PRE) {
                if (this.target != null) {
                    if (!this.weaponOnly.getValue() || ItemUtil.isHoldingSword()) {
                        boolean canAutoBlock = ItemUtil.isHoldingSword() && this.target != null;
                        this.blocking = canAutoBlock && this.autoBlock.getValue() > 0;
                        if (this.rotationMode.getValue() == 1) {
                            float[] rots = RotationUtil.calculate(
                                this.target, true, this.attackRange.getValue().floatValue()
                            );
                            if (rots != null) {
                                float speed = this.rotationSpeed.getValue();
                                if (speed > 0.0F) {
                                    rots[0] = this.rotMove(rots[0], this.lastYaw, speed);
                                    rots[1] = this.rotMove(rots[1], this.lastPitch, speed);
                                }

                                event.setRotation(rots[0], rots[1], 1);
                                this.lastYaw = rots[0];
                                this.lastPitch = rots[1];
                            }
                        }

                        boolean shouldRayCast = this.rayCast.getValue();
                        if (shouldRayCast) {
                            MovingObjectPosition mop;
                            if (this.hitThroughWalls.getValue()) {
                                mop = RayCastUtil.getEntityIntercept(
                                    this.target, this.lastYaw, this.lastPitch, this.attackRange.getValue().floatValue()
                                );
                            } else {
                                mop = RayCastUtil.rayCast(
                                    this.lastYaw, this.lastPitch, this.attackRange.getValue().floatValue()
                                );
                            }

                            if (mop == null || mop.field_72308_g != this.target) {
                                return;
                            }
                        }

                        if (this.attackTimer.hasTimeElapsed(this.getCpsDelay(), true)) {
                            this.performAttack();
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.SEND) {
            if (event.getPacket() instanceof C07PacketPlayerDigging) {
                C07PacketPlayerDigging packet = (C07PacketPlayerDigging)event.getPacket();
                if (packet.func_180762_c() == Action.RELEASE_USE_ITEM) {
                    this.blocking = false;
                    this.wasBlocking = false;
                }
            }
        }
    }

    private void performAttack() {
        if (this.target != null) {
            boolean silentSwingActive = this.silentSwing.getValue() && this.blocking;
            if (!silentSwingActive) {
                mc.field_71439_g.func_71038_i();
            } else {
                PacketUtil.sendPacket(new C0APacketAnimation());
            }

            PacketUtil.sendPacket(
                new C02PacketUseEntity(this.target, net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK)
            );
            PlayerUtil.attackEntity(this.target);
            if (mc.field_71439_g.field_70143_R > 0.0F
                && !mc.field_71439_g.field_70122_E
                && !mc.field_71439_g.func_70617_f_()
                && !mc.field_71439_g.func_70090_H()
                && !mc.field_71439_g.func_70644_a(Potion.field_76440_q)
                && mc.field_71439_g.field_70154_o == null) {
                mc.field_71439_g.func_71009_b(this.target);
            }

            if (mc.field_71439_g.func_70694_bm() != null
                && EnchantmentHelper.func_152377_a(mc.field_71439_g.func_70694_bm(), this.target.func_70668_bt())
                    > 0.0F) {
                mc.field_71439_g.func_71047_c(this.target);
            }

            this.handleAutoBlock();
        }
    }

    private void handleAutoBlock() {
        if (this.blocking && ItemUtil.isHoldingSword()) {
            int mode = this.autoBlock.getValue();
            if (this.fixNoSlowFlag.getValue() && this.blockingTime > this.postDelay.getValue()) {
                this.unBlock();
                this.blockingTime = 0;
            } else {
                switch (mode) {
                    case 0:
                    case 7:
                    default:
                        break;
                    case 1:
                        this.sendBlock();
                        this.wasBlocking = true;
                        break;
                    case 2:
                        PacketUtil.sendPacket(
                            new C09PacketHeldItemChange(mc.field_71439_g.field_71071_by.field_70461_c % 8 + 1)
                        );
                        PacketUtil.sendPacket(
                            new C09PacketHeldItemChange(mc.field_71439_g.field_71071_by.field_70461_c)
                        );
                        this.wasBlocking = true;
                        break;
                    case 3:
                    case 4:
                    case 5:
                    case 8:
                        PacketUtil.sendPacket(
                            new C02PacketUseEntity(
                                this.target, net.minecraft.network.play.client.C02PacketUseEntity.Action.INTERACT
                            )
                        );
                        this.sendBlock();
                        this.wasBlocking = true;
                        break;
                    case 6:
                        this.wasBlocking = true;
                        break;
                    case 9:
                        this.wasBlocking = true;
                        break;
                    case 10:
                        if (mc.field_71439_g.func_70694_bm() != null
                            && mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemSword) {
                            PacketUtil.sendPacket(
                                new C0FPacketConfirmTransaction(
                                    RandomUtil.nextInt(0, Integer.MAX_VALUE),
                                    (short)RandomUtil.nextInt(-32767, 0),
                                    true
                                )
                            );
                            PacketUtil.sendPacket(new C0APacketAnimation());
                            mc.field_71442_b
                                .func_78769_a(mc.field_71439_g, mc.field_71441_e, mc.field_71439_g.func_70694_bm());
                        }

                        this.wasBlocking = true;
                }

                this.blockingTime++;
            }
        }
    }

    private void sendBlock() {
        PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.field_71439_g.func_70694_bm()));
    }

    private void unBlock() {
        if (ItemUtil.isHoldingSword()) {
            PacketUtil.sendPacket(
                new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.field_177992_a, EnumFacing.DOWN)
            );
            this.wasBlocking = false;
            this.blocking = false;
        }
    }

    private List<EntityLivingBase> getTargets() {
        List<EntityLivingBase> list = new ArrayList<>();

        for (Entity entity : mc.field_71441_e.func_72910_y()) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase e = (EntityLivingBase)entity;
                if (e != mc.field_71439_g && !e.field_70128_L) {
                    double dist = mc.field_71439_g.func_70032_d(e);
                    if (!(dist > this.preAimRange.getValue().floatValue()) && this.isValidTarget(e)) {
                        list.add(e);
                    }
                }
            }
        }

        switch (this.sortMode.getValue()) {
            case 0:
                list.sort(Comparator.comparingDouble(EntityLivingBase::func_110143_aJ));
                break;
            case 1:
                list.sort(Comparator.comparingInt(ex -> ex.field_70737_aN));
                break;
            case 2:
                list.sort(Comparator.comparingDouble(mc.field_71439_g::func_70032_d));
                break;
            case 3:
                list.sort((a, b) -> Float.compare(Math.abs(a.field_70177_z), Math.abs(b.field_70177_z)));
        }

        return list;
    }

    private boolean isValidTarget(EntityLivingBase entity) {
        if (entity != null && mc.field_71441_e != null && mc.field_71439_g != null) {
            if (!mc.field_71441_e.func_72910_y().contains(entity)) {
                return false;
            }

            if (entity == mc.field_71439_g || entity == mc.field_71439_g.field_70154_o) {
                return false;
            }

            if (entity == mc.func_175606_aa() || entity == mc.func_175606_aa().field_70154_o) {
                return false;
            }

            if (entity.field_70725_aQ > 0) {
                return false;
            }

            if (entity instanceof EntityPlayer) {
                return this.isValidPlayer((EntityPlayer)entity);
            }

            if (entity instanceof EntityDragon || entity instanceof EntityWither) {
                return this.targetBosses.getValue();
            }

            if (!(entity instanceof EntityMob) && !(entity instanceof EntitySlime)) {
                if (entity instanceof EntityAnimal
                    || entity instanceof EntityBat
                    || entity instanceof EntitySquid
                    || entity instanceof EntityVillager) {
                    return this.targetAnimals.getValue();
                } else {
                    return !(entity instanceof EntityIronGolem)
                        ? false
                        : this.targetGolems.getValue() && this.allowTeamColor(entity);
                }
            } else {
                return !(entity instanceof EntitySilverfish)
                    ? this.targetMobs.getValue()
                    : this.targetSilverfish.getValue() && this.allowTeamColor(entity);
            }
        } else {
            return false;
        }
    }

    private boolean isValidPlayer(EntityPlayer player) {
        if (!this.targetPlayers.getValue()) {
            return false;
        } else {
            boolean isInvisible = player.func_82150_aj();
            if (isInvisible && !this.targetInvisible.getValue()) {
                return false;
            } else {
                return TeamUtil.isFriend(player)
                    ? false
                    : this.allowSameTeam(player) && (isInvisible || !AntiBot.isBot(player));
            }
        }
    }

    private boolean allowTeamColor(EntityLivingBase entityLivingBase) {
        return this.targetTeams.getValue() || !TeamUtil.hasTeamColor(entityLivingBase);
    }

    private boolean allowSameTeam(EntityPlayer player) {
        return this.targetTeams.getValue() || !TeamUtil.isSameTeam(player);
    }

    private long getCpsDelay() {
        float min = this.minCps.getValue();
        float max = this.maxCps.getValue();
        if (min > max) {
            float tmp = min;
            min = max;
            max = tmp;
        }

        return (long)(1000.0 / RandomUtil.nextDouble(min, max));
    }

    private float rotMove(float target, float current, float speed) {
        float delta = MathHelper.func_76142_g(target - current);
        if (speed >= 5.0F) {
            return target;
        }

        float maxStep = speed * 10.0F;
        return Math.abs(delta) <= maxStep ? target : current + (delta > 0.0F ? maxStep : -maxStep);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
