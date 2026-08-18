package miau.module.modules.ghost;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.LeftClickMouseEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.module.modules.combat.KillAura;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.util.player.SimulatedPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;

public class SmartClicking extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private EntityLivingBase lastTarget = null;
    private long lastAttackTime = 0L;
    private boolean shouldClick = true;
    private double lastDamage = 0.0;
    private long lastTimePlayerHurt = 0L;
    public final FloatProperty attackRange = new FloatProperty("attack-range", 3.0F, 2.0F, 6.0F);
    public final FloatProperty searchRange = new FloatProperty("search-range", 3.5F, 2.0F, 8.0F);
    public final IntProperty selfPredictionTicks = new IntProperty("self-pred-ticks", 1, 0, 5);
    public final IntProperty targetPredictionTicks = new IntProperty("target-pred-ticks", 3, 0, 5);
    public final BooleanProperty baseHurtTimeOnPing = new BooleanProperty("base-hurttime-on-ping", true);
    public final IntProperty validHurtTimeStart = new IntProperty("valid-hurttime-start", 2, 0, 10);
    public final IntProperty tradeTimeoutTicks = new IntProperty("trade-timeout-ticks", 5, 0, 20);
    public final IntProperty midTradeHurtTimeStart = new IntProperty("mid-trade-start", 1, 0, 10);
    public final IntProperty midTradeHurtTimeEnd = new IntProperty("mid-trade-end", 2, 0, 10);

    public SmartClicking() {
        super("SmartClicking", false);
    }

    @Override
    public void onEnabled() {
        this.lastTarget = null;
        this.shouldClick = true;
        this.lastDamage = 0.0;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled()) {
            if (event.getTarget() instanceof EntityLivingBase) {
                this.lastTarget = (EntityLivingBase)event.getTarget();
                this.lastAttackTime = System.currentTimeMillis();
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
            EntityLivingBase currentTarget = null;
            if (killAura != null && killAura.isEnabled()) {
                currentTarget = killAura.getTarget();
            }

            if (currentTarget == null) {
                currentTarget = this.lastTarget;
            }

            if (currentTarget != null) {
                double dist = mc.field_71439_g.func_70032_d(currentTarget);
                if (dist > this.searchRange.getValue().floatValue()) {
                    currentTarget = null;
                    this.lastTarget = null;
                }
            }

            if (mc.field_71439_g.field_70737_aN > 0) {
                this.lastTimePlayerHurt = System.currentTimeMillis();
            }

            if (currentTarget != null) {
                this.shouldClick = this.calculateShouldClick(currentTarget);
            } else {
                this.shouldClick = true;
            }
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled()) {
            if (!this.shouldClick) {
                double currentDamage = this.getPlayerDamage();
                if (currentDamage > this.lastDamage) {
                    this.lastDamage = currentDamage;
                } else {
                    event.setCancelled(true);
                }
            } else {
                this.lastDamage = this.getPlayerDamage();
            }
        }
    }

    private double getPlayerDamage() {
        if (mc.field_71439_g == null) {
            return 0.0;
        }

        double baseDamage = 1.0;
        if (mc.field_71439_g.func_110148_a(SharedMonsterAttributes.field_111264_e) != null) {
            baseDamage = mc.field_71439_g.func_110148_a(SharedMonsterAttributes.field_111264_e).func_111126_e();
        }

        ItemStack held = mc.field_71439_g.func_70694_bm();
        float enchantDamage = 0.0F;
        if (held != null) {
            enchantDamage = EnchantmentHelper.func_152377_a(held, EnumCreatureAttribute.UNDEFINED);
        }

        boolean isCrit = mc.field_71439_g.field_70143_R > 0.0F
            && !mc.field_71439_g.field_70122_E
            && !mc.field_71439_g.func_70617_f_()
            && !mc.field_71439_g.func_70090_H()
            && !mc.field_71439_g.func_70644_a(Potion.field_76440_q)
            && mc.field_71439_g.field_70154_o == null;
        return (baseDamage + enchantDamage) * (isCrit ? 1.5 : 1.0);
    }

    private boolean calculateShouldClick(EntityLivingBase target) {
        SimulatedPlayer sim = SimulatedPlayer.fromClientPlayer(mc.field_71439_g.field_71158_b);
        int simHurtTime = mc.field_71439_g.field_70737_aN;
        int selfPred = this.selfPredictionTicks.getValue();

        for (int i = 0; i < selfPred; i++) {
            sim.tick();
            if (simHurtTime > 0) {
                simHurtTime--;
            }
        }

        if (simHurtTime <= 0) {
            ItemStack targetHeld = target.func_70694_bm();
            int knockbackLevel = 0;
            if (targetHeld != null) {
                knockbackLevel = EnchantmentHelper.func_77506_a(Enchantment.field_180313_o.field_77352_x, targetHeld);
            }

            double kb = knockbackLevel + (target.func_70051_ag() ? 1.0 : 0.0);
            float yawHead = target.field_70759_as;
            sim.motionX = sim.motionX + -MathHelper.func_76126_a(yawHead * (float) Math.PI / 180.0F) * kb * 0.5;
            sim.motionZ = sim.motionZ + MathHelper.func_76134_b(yawHead * (float) Math.PI / 180.0F) * kb * 0.5;
            sim.motionY += 0.1;
        }

        int pingTicks = 0;
        if (this.baseHurtTimeOnPing.getValue()) {
            int ping = 0;
            if (mc.func_147114_u() != null && mc.field_71439_g != null) {
                NetworkPlayerInfo info = mc.func_147114_u().func_175102_a(mc.field_71439_g.func_110124_au());
                if (info != null) {
                    ping = info.func_178853_c();
                }
            }

            pingTicks = ping / 50;
        }

        int optimalHurtTime = this.validHurtTimeStart.getValue() + pingTicks;
        boolean targetHittable = target.field_70737_aN <= optimalHurtTime;
        long timeSinceHurt = System.currentTimeMillis() - this.lastTimePlayerHurt;
        if (timeSinceHurt <= this.tradeTimeoutTicks.getValue() * 50) {
            int ticksForGround = 5;
            SimulatedPlayer landSim = SimulatedPlayer.fromClientPlayer(mc.field_71439_g.field_71158_b);

            for (int i = 1; i <= 5; i++) {
                landSim.tick();
                if (landSim.onGround) {
                    ticksForGround = i;
                    break;
                }
            }

            double start = this.midTradeHurtTimeStart.getValue().intValue();
            double end = this.midTradeHurtTimeEnd.getValue().intValue();
            double midTradeHurtTime = start + (end - start) * (ticksForGround / 5.0);
            if (target.field_70737_aN > midTradeHurtTime && target.field_70737_aN < 10) {
                return false;
            }
        }

        boolean targetOnFire = target.func_70027_ad();
        return targetHittable || targetOnFire;
    }
}
