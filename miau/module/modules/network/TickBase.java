package miau.module.modules.network;

import java.util.List;
import java.util.stream.Collectors;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorMinecraft;
import miau.module.Module;
import miau.module.modules.combat.KillAura;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.util.client.ChatUtil;
import miau.util.player.PlayerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.Timer;

public class TickBase extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty onlyKillaura = new BooleanProperty("Only KillAura", true);
    public final FloatProperty lagRange = new FloatProperty("Lag Range", 8.0F, 5.0F, 15.0F);
    public final FloatProperty targetRange = new FloatProperty("Target Range", 20.0F, 5.0F, 50.0F);
    public final FloatProperty minRange = new FloatProperty("Min Range", 3.0F, 1.0F, 10.0F);
    public final FloatProperty revertRange = new FloatProperty("Revert Range", 4.0F, 1.0F, 10.0F);
    public final IntProperty maxBalance = new IntProperty("Max Balance", 50, 10, 200);
    public final IntProperty timeMultiplier = new IntProperty("Time Multiplier", 25, 10, 100);
    public final IntProperty ticksToReduce = new IntProperty("Ticks To Reduce", 1, 1, 10);
    private TickBase.Mode mode = TickBase.Mode.NONE;
    private long time;
    private long balance;
    private double range;
    private double distance;
    private Entity target;

    public TickBase() {
        super("TickBase", false);
    }

    private EntityLivingBase getTarget(double range) {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        if (killAura == null) {
            return null;
        }

        if (this.onlyKillaura.getValue()) {
            if (!killAura.isEnabled()) {
                return null;
            }

            EntityLivingBase kTarget = killAura.getTarget();
            return kTarget != null && mc.field_71439_g.func_70032_d(kTarget) <= range ? kTarget : null;
        } else {
            List<EntityLivingBase> entities = mc.field_71441_e
                .field_72996_f
                .stream()
                .filter(entity -> entity instanceof EntityLivingBase)
                .map(entity -> (EntityLivingBase)entity)
                .filter(entity -> entity != mc.field_71439_g)
                .filter(entity -> !entity.field_70128_L && entity.func_110143_aJ() > 0.0F)
                .filter(
                    entity -> {
                        if (entity instanceof EntityPlayer && !killAura.targetPlayers.getValue()) {
                            return false;
                        } else if (entity.func_82150_aj() && !killAura.targetInvisibles.getValue()) {
                            return false;
                        } else {
                            return (entity instanceof EntityMob || entity instanceof EntitySlime)
                                    && !killAura.targetMobs.getValue()
                                ? false
                                : !(entity instanceof EntityAnimal)
                                        && !(entity instanceof EntitySquid)
                                        && !(entity instanceof EntityBat)
                                        && !(entity instanceof EntityVillager)
                                    || killAura.targetAnimals.getValue();
                        }
                    }
                )
                .filter(entity -> mc.field_71439_g.func_70032_d(entity) <= range)
                .collect(Collectors.toList());
            return entities.isEmpty() ? null : entities.get(0);
        }
    }

    @Override
    public void onDisabled() {
        this.mode = TickBase.Mode.NONE;
        ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.PRE) {
                if (this.target != null && mc.field_71439_g != null) {
                    this.distance = PlayerUtil.calculatePerfectRangeToEntity(this.target);
                }

                if (this.mode != TickBase.Mode.REDUCING) {
                    this.target = this.getTarget(this.targetRange.getValue().floatValue());
                    if (this.target != null) {
                        this.distance = PlayerUtil.calculatePerfectRangeToEntity(this.target);
                        double currentRange = this.distance;
                        if (currentRange > this.minRange.getValue().floatValue()
                            && this.balance >= this.maxBalance.getValue().intValue()
                            && this.mode == TickBase.Mode.BASING) {
                            this.balance = this.balance - this.maxBalance.getValue().intValue();
                            Timer var10000 = ((IAccessorMinecraft)mc).getTimer();
                            var10000.field_74280_b = var10000.field_74280_b + this.ticksToReduce.getValue();
                        } else {
                            if (this.balance != 0L) {
                                ChatUtil.display("Balance " + this.balance + " " + currentRange);
                            }

                            this.balance = 0L;
                            this.mode = TickBase.Mode.NONE;
                        }

                        if (currentRange < this.lagRange.getValue().floatValue()
                            && this.range >= this.lagRange.getValue().floatValue()
                            && this.mode == TickBase.Mode.NONE) {
                            this.mode = TickBase.Mode.REDUCING;
                            this.time = System.currentTimeMillis();
                            this.balance = 0L;
                        }

                        this.range = currentRange;
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.RECEIVE && this.mode == TickBase.Mode.REDUCING) {
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled()) {
            if (this.mode == TickBase.Mode.REDUCING && this.target != null) {
                if (!(this.distance <= this.revertRange.getValue().floatValue())
                    && !(
                        System.currentTimeMillis() - this.time
                            >= this.range
                                    / (mc.field_71439_g.func_70644_a(Potion.field_76424_c) ? 0.36 : 0.25)
                                    * this.timeMultiplier.getValue().intValue()
                                + this.timeMultiplier.getValue().intValue()
                    )) {
                    ((IAccessorMinecraft)mc).getTimer().field_74278_d = 0.0F;
                } else {
                    ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
                    this.mode = TickBase.Mode.BASING;
                    this.balance = System.currentTimeMillis() - this.time;
                }
            }
        }
    }

    private enum Mode {
        REDUCING,
        BASING,
        NONE;
    }
}
