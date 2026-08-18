package miau.module.modules.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.module.modules.misc.AntiBot;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.client.KeyBindUtil;
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
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class KillAuraV2 extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private int cps;
    private int targetIndex;
    private float lastYaw;
    private float lastPitch;
    private boolean aiming;
    private boolean blocking;
    private boolean wasBlocking;
    private EntityLivingBase target;
    private final TimerUtil attackTimer = new TimerUtil();
    private final TimerUtil switchTimer = new TimerUtil();
    private final List<EntityLivingBase> targets = new ArrayList<>();
    private int autoBlockWatchdogBlockingTime;
    public final BooleanProperty targetPlayers = new BooleanProperty("Target players", true);
    public final BooleanProperty targetAnimals = new BooleanProperty("Target animals", false);
    public final BooleanProperty targetMobs = new BooleanProperty("Target mobs", false);
    public final BooleanProperty targetInvisible = new BooleanProperty("Target invisible", false);
    public final BooleanProperty targetBosses = new BooleanProperty("Target bosses", false);
    public final BooleanProperty targetGolems = new BooleanProperty("Target golems", false);
    public final BooleanProperty targetSilverfish = new BooleanProperty("Target silverfish", false);
    public final BooleanProperty targetTeams = new BooleanProperty("Target teams", true);
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Single", "Switch"});
    public final IntProperty switchDelay = new IntProperty(
        "Switch delay", 200, 0, 1000, () -> this.mode.getValue() == 1
    );
    public final FloatProperty rotationSpeed = new FloatProperty("Rotation speed", 20.0F, 2.0F, 20.0F);
    public final ModeProperty rotationMode = new ModeProperty("Rotation mode", 0, new String[]{"Instant", "Nearest"});
    public final ModeProperty moveFixMode = new ModeProperty(
        "Move fix mode", 2, new String[]{"Off", "Normal", "Silent"}
    );
    public final BooleanProperty autoBlock = new BooleanProperty("Auto block", false);
    public final ModeProperty autoBlockMode = new ModeProperty(
        "Auto block mode", 0, new String[]{"Fake", "Watchdog", "GrimAC 1.8", "GrimAC 1.12"}, this.autoBlock::getValue
    );
    public final BooleanProperty fixNoSlowFlag = new BooleanProperty(
        "Fix no slow flag", false, () -> this.autoBlock.getValue() && this.autoBlockMode.getValue() == 1
    );
    public final ModeProperty sortMode = new ModeProperty(
        "Sort mode", 0, new String[]{"Distance", "Hurt Time", "Health", "Armor"}
    );
    public final FloatProperty minCPS = new FloatProperty("Min CPS", 10.0F, 1.0F, 20.0F);
    public final FloatProperty maxCPS = new FloatProperty("Max CPS", 20.0F, 1.0F, 20.0F);
    public final FloatProperty preAimRange = new FloatProperty("Pre aim range", 3.5F, 3.0F, 10.0F);
    public final FloatProperty attackRange = new FloatProperty("Attack range", 3.2F, 3.0F, 6.0F);
    public final BooleanProperty throughWalls = new BooleanProperty("Through walls", false);
    public final BooleanProperty rayCast = new BooleanProperty("Ray cast", true);

    public KillAuraV2() {
        super("KillAuraV2", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }

    @Override
    public void onDisabled() {
        this.target = null;
        this.targets.clear();
        this.aiming = false;
        this.blocking = false;
        if (this.wasBlocking) {
            int autoBlock = this.autoBlockMode.getValue();
            switch (autoBlock) {
                case 1:
                    PacketUtil.sendPacket(
                        new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.field_177992_a, EnumFacing.DOWN)
                    );
                    break;
                case 2:
                case 3:
                    KeyBindUtil.setKeyBindState(mc.field_71474_y.field_74313_G.func_151463_i(), false);
            }
        }

        this.wasBlocking = false;
        this.autoBlockWatchdogBlockingTime = 0;
        super.onDisabled();
    }

    private float rotMove(float target, float current, float speed) {
        float delta = MathHelper.func_76142_g(target - current);
        delta = MathHelper.func_76131_a(delta, -speed, speed);
        return current + delta;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.minCPS.getValue() > this.maxCPS.getValue()) {
                float min = this.minCPS.getValue();
                float max = this.maxCPS.getValue();
                this.minCPS.setValue(Math.min(min, max));
                this.maxCPS.setValue(Math.max(min, max));
            }

            if (this.attackRange.getValue() > this.preAimRange.getValue()) {
                this.preAimRange.setValue(this.attackRange.getValue());
            }

            this.sortTargets();
            if (this.target == null) {
                this.lastYaw = event.getYaw();
                this.lastPitch = event.getPitch();
            }

            this.aiming = !this.targets.isEmpty();
            this.blocking = this.autoBlock.getValue() && this.aiming && ItemUtil.isHoldingSword();
            if (this.aiming) {
                switch (this.mode.getValue()) {
                    case 0:
                        if (!this.targets.isEmpty()) {
                            this.target = this.targets.get(0);
                        } else {
                            this.target = null;
                        }
                        break;
                    case 1:
                        if (this.switchTimer.hasTimeElapsed(this.switchDelay.getValue().intValue(), true)) {
                            this.targetIndex = (this.targetIndex + 1) % this.targets.size();
                        }

                        if (this.targetIndex < this.targets.size()) {
                            this.target = this.targets.get(this.targetIndex);
                        } else {
                            this.target = null;
                        }
                }

                float yaw = this.lastYaw;
                float pitch = this.lastPitch;
                float rotSpeed = (float)RandomUtil.nextDouble(
                    this.rotationSpeed.getValue().floatValue(), this.rotationSpeed.getValue().floatValue()
                );
                switch (this.rotationMode.getValue()) {
                    case 0:
                        if (this.target != null) {
                            Vec3 eyePos = new Vec3(
                                this.target.field_70165_t,
                                this.target.field_70163_u + this.target.func_70047_e(),
                                this.target.field_70161_v
                            );
                            float[] rots = RotationUtil.calculate(eyePos);
                            yaw = rots[0];
                            pitch = rots[1];
                        }
                        break;
                    case 1:
                        if (this.target != null) {
                            float[] rots = RotationUtil.calculate(this.target);
                            yaw = rots[0];
                            pitch = rots[1];
                        }
                }

                this.lastYaw = this.rotMove(yaw, this.lastYaw, rotSpeed);
                this.lastPitch = this.rotMove(pitch, this.lastPitch, rotSpeed);
                event.setRotation(this.lastYaw, this.lastPitch, 1);
                if (this.rayCast.getValue() && this.target != null) {
                    MovingObjectPosition mop = RayCastUtil.getEntityIntercept(
                        this.target, this.lastYaw, this.lastPitch, this.attackRange.getValue().floatValue()
                    );
                    if ((
                            mop != null
                                || RotationUtil.distanceToEntity(this.target)
                                    <= this.attackRange.getValue().floatValue()
                        )
                        && this.attackTimer.hasTimeElapsed(this.cps)) {
                        int maxValue = (int)((this.minCPS.getMaximum() - this.maxCPS.getValue()) * 5.0);
                        int minValue = (int)((this.minCPS.getMaximum() - this.minCPS.getValue()) * 5.0);
                        this.cps = RandomUtil.nextInt(Math.min(minValue, maxValue), Math.max(minValue, maxValue));
                        this.attack();
                    }
                } else if (this.target != null && this.attackTimer.hasTimeElapsed(this.cps)) {
                    int maxValue = (int)((this.minCPS.getMaximum() - this.maxCPS.getValue()) * 5.0);
                    int minValue = (int)((this.minCPS.getMaximum() - this.minCPS.getValue()) * 5.0);
                    this.cps = RandomUtil.nextInt(Math.min(minValue, maxValue), Math.max(minValue, maxValue));
                    this.attack();
                }
            } else {
                this.attackTimer.reset();
                this.target = null;
            }

            int autoBlock = this.autoBlockMode.getValue();
            if (this.blocking) {
                switch (autoBlock) {
                    case 0:
                    case 2:
                    default:
                        break;
                    case 1:
                        if (this.autoBlockWatchdogBlockingTime >= 10 && this.fixNoSlowFlag.getValue()) {
                            if (this.wasBlocking) {
                                PacketUtil.sendPacket(
                                    new C07PacketPlayerDigging(
                                        Action.RELEASE_USE_ITEM, BlockPos.field_177992_a, EnumFacing.DOWN
                                    )
                                );
                            }

                            this.wasBlocking = false;
                            this.autoBlockWatchdogBlockingTime = 0;
                        } else {
                            PacketUtil.sendPacket(
                                new C02PacketUseEntity(
                                    this.target, net.minecraft.network.play.client.C02PacketUseEntity.Action.INTERACT
                                )
                            );
                            PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.field_71439_g.func_70694_bm()));
                            this.wasBlocking = true;
                            this.autoBlockWatchdogBlockingTime++;
                        }
                        break;
                    case 3:
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
                            this.wasBlocking = true;
                        }
                }
            } else if (!this.wasBlocking || autoBlock != 2 && autoBlock != 3) {
                if (this.wasBlocking && autoBlock == 1) {
                    PacketUtil.sendPacketNoEvent(
                        new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.field_177992_a, EnumFacing.DOWN)
                    );
                    this.wasBlocking = false;
                }
            } else {
                KeyBindUtil.setKeyBindState(mc.field_71474_y.field_74313_G.func_151463_i(), false);
                this.wasBlocking = false;
            }
        } else if (event.getType() == EventType.POST && this.blocking && this.autoBlockMode.getValue() == 2) {
            mc.field_71442_b.func_78769_a(mc.field_71439_g, mc.field_71441_e, mc.field_71439_g.func_70694_bm());
        }
    }

    private void attack() {
        if (this.target != null) {
            MovingObjectPosition mop = RayCastUtil.getEntityIntercept(
                this.target, this.lastYaw, this.lastPitch, this.attackRange.getValue().floatValue()
            );
            if (mop != null || RotationUtil.distanceToEntity(this.target) <= this.attackRange.getValue().floatValue()) {
                if (mc.field_71439_g.field_70143_R > 0.0F
                    && !mc.field_71439_g.field_70122_E
                    && !mc.field_71439_g.func_70617_f_()
                    && !mc.field_71439_g.func_70090_H()
                    && !mc.field_71439_g.func_70644_a(Potion.field_76440_q)
                    && mc.field_71439_g.field_70154_o == null) {
                    mc.field_71439_g.func_71009_b(this.target);
                }

                if (EnchantmentHelper.func_152377_a(mc.field_71439_g.func_70694_bm(), this.target.func_70668_bt())
                    > 0.0F) {
                    mc.field_71439_g.func_71047_c(this.target);
                    PacketUtil.sendPacket(new C0APacketAnimation());
                }

                PlayerUtil.attackEntity(this.target);
                this.attackTimer.reset();
            }
        }
    }

    private void sortTargets() {
        this.targets.clear();

        for (Entity entity : mc.field_71441_e.func_72910_y()) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase e = (EntityLivingBase)entity;
                if (mc.field_71439_g.func_70032_d(entity) <= this.preAimRange.getValue()
                    && this.isValid(entity)
                    && mc.field_71439_g != e) {
                    this.targets.add(e);
                }
            }
        }

        switch (this.sortMode.getValue()) {
            case 0:
                this.targets.sort(Comparator.comparingDouble(mc.field_71439_g::func_70032_d));
                break;
            case 1:
                this.targets.sort(Comparator.comparingInt(entityx -> entityx.field_70737_aN));
                break;
            case 2:
                this.targets.sort(Comparator.comparingDouble(EntityLivingBase::func_110143_aJ));
                break;
            case 3:
                this.targets.sort(Comparator.comparingInt(EntityLivingBase::func_70658_aO));
        }
    }

    private boolean isValid(Entity entity) {
        if (entity instanceof EntityLivingBase && mc.field_71441_e != null && mc.field_71439_g != null) {
            EntityLivingBase e = (EntityLivingBase)entity;
            if (!mc.field_71441_e.func_72910_y().contains(e)) {
                return false;
            }

            if (e == mc.field_71439_g || e == mc.field_71439_g.field_70154_o) {
                return false;
            }

            if (e == mc.func_175606_aa() || e == mc.func_175606_aa().field_70154_o) {
                return false;
            }

            if (e.field_70725_aQ > 0) {
                return false;
            }

            if (e instanceof EntityPlayer) {
                return this.isValidPlayer((EntityPlayer)e);
            }

            if (e instanceof EntityDragon || e instanceof EntityWither) {
                return this.targetBosses.getValue();
            }

            if (!(e instanceof EntityMob) && !(e instanceof EntitySlime)) {
                if (e instanceof EntityAnimal
                    || e instanceof EntityBat
                    || e instanceof EntitySquid
                    || e instanceof EntityVillager) {
                    return this.targetAnimals.getValue();
                } else {
                    return !(e instanceof EntityIronGolem)
                        ? false
                        : this.targetGolems.getValue() && this.allowTeamColor(e);
                }
            } else {
                return !(e instanceof EntitySilverfish)
                    ? this.targetMobs.getValue()
                    : this.targetSilverfish.getValue() && this.allowTeamColor(e);
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
}
