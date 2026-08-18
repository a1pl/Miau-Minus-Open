package miau.module.modules.combat;

import java.util.concurrent.ThreadLocalRandom;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.misc.BackTrackUtil;
import miau.util.network.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;

public class AutoRod2 extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final ModeProperty rodMode = new ModeProperty("RodMode", 0, new String[]{"Legit", "Packet", "NewPacket"});
    private final FloatProperty maxRange = new FloatProperty("MaxRange", 6.0F, 0.0F, 16.0F);
    private final IntProperty maxDelayValue = new IntProperty("MaxDelay", 200, 0, 1000);
    private final IntProperty minDelay = new IntProperty("MinDelay", 100, 0, 1000);
    private final BooleanProperty smartDelay = new BooleanProperty("SmartDelay", false);
    private final BooleanProperty smartRodTiming = new BooleanProperty(
        "SmartRodTiming", false, () -> this.smartDelay.getValue()
    );
    private final BooleanProperty perfectTiming = new BooleanProperty("PerfectTiming", false);
    private final IntProperty perfectHurtTime = new IntProperty(
        "PerfectHurtTime", 9, 0, 10, () -> this.perfectTiming.getValue()
    );
    private final ModeProperty predictMode = new ModeProperty(
        "PredictMode", 0, new String[]{"Custom", "ExperimentalFitting"}
    );
    private final FloatProperty predictSize = new FloatProperty(
        "PredictSize", 3.5F, 0.0F, 10.0F, () -> this.predictMode.getModeString().equals("Custom")
    );
    private int currentItem = -1;
    private ItemStack itemStack = null;
    private boolean resetting = false;
    private int pauseTick = 0;
    private boolean rodActionState = false;
    private boolean itemState = false;
    private boolean hasThrownRod = false;

    public AutoRod2() {
        super("AutoRod2", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.rodMode.getModeString()};
    }

    @Override
    public void onDisabled() {
        this.reset();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayer player = mc.field_71439_g;
            if (player != null) {
                int rod = this.getRod();
                int lastCurrentItem = this.currentItem;
                ItemStack lastItemStack = this.itemStack;
                if (this.cancelRun()) {
                    this.resetting = true;
                    switch (this.rodMode.getModeString()) {
                        case "Legit":
                            if (this.rodActionState) {
                                KeyBinding.func_74510_a(mc.field_71474_y.field_74313_G.func_151463_i(), false);
                            }

                            if (this.itemState) {
                                this.swapItem(lastCurrentItem);
                            }
                            break;
                        case "Packet":
                            if (this.rodActionState) {
                                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(lastItemStack));
                            }

                            if (this.itemState) {
                                PacketUtil.sendPacket(new C09PacketHeldItemChange(lastCurrentItem));
                            }
                            break;
                        case "NewPacket":
                            if (this.itemState) {
                                PacketUtil.sendPacket(new C09PacketHeldItemChange(lastCurrentItem));
                            }
                    }

                    this.resetting = false;
                    this.reset();
                } else {
                    this.currentItem = player.field_71071_by.field_70461_c;
                    if (rod >= 0) {
                        this.itemStack = player.field_71069_bz.func_75139_a(rod).func_75211_c();
                    }

                    EntityLivingBase target = this.getKillAuraTarget();
                    boolean shouldPullRod = this.perfectTiming.getValue()
                        && target != null
                        && target.field_70737_aN == this.perfectHurtTime.getValue();
                    switch (this.rodMode.getModeString()) {
                        case "Legit":
                            if (this.perfectTiming.getValue()) {
                                if (!this.hasThrownRod && this.shouldThrowRod()) {
                                    this.swapItem(rod - 36);
                                    this.itemState = true;
                                    KeyBinding.func_74510_a(mc.field_71474_y.field_74313_G.func_151463_i(), true);
                                    this.rodActionState = true;
                                    this.hasThrownRod = true;
                                }

                                if (this.hasThrownRod && (shouldPullRod || this.cancelRun())) {
                                    KeyBinding.func_74510_a(mc.field_71474_y.field_74313_G.func_151463_i(), false);
                                    this.rodActionState = false;
                                    this.swapItem(this.currentItem);
                                    this.itemState = true;
                                    this.hasThrownRod = false;
                                    this.pauseTick = 0;
                                }
                            } else {
                                this.pauseTick++;
                                if (this.pauseTick == 1) {
                                    this.swapItem(rod - 36);
                                    this.itemState = true;
                                    KeyBinding.func_74510_a(mc.field_71474_y.field_74313_G.func_151463_i(), true);
                                    this.rodActionState = true;
                                }

                                if (this.pauseTick >= this.tickDelay()) {
                                    KeyBinding.func_74510_a(mc.field_71474_y.field_74313_G.func_151463_i(), false);
                                    this.rodActionState = false;
                                    this.swapItem(this.currentItem);
                                    this.itemState = true;
                                    this.pauseTick = 0;
                                }
                            }
                            break;
                        case "Packet":
                            if (this.perfectTiming.getValue()) {
                                if (!this.hasThrownRod && this.shouldThrowRod()) {
                                    PacketUtil.sendPacket(new C09PacketHeldItemChange(rod - 36));
                                    this.itemState = true;
                                    PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(this.itemStack));
                                    this.rodActionState = true;
                                    this.hasThrownRod = true;
                                }

                                if (this.hasThrownRod && (shouldPullRod || this.cancelRun())) {
                                    PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(this.itemStack));
                                    this.rodActionState = false;
                                    PacketUtil.sendPacket(new C09PacketHeldItemChange(this.currentItem));
                                    this.itemState = false;
                                    this.hasThrownRod = false;
                                    this.pauseTick = 0;
                                }
                            } else {
                                this.pauseTick++;
                                if (this.pauseTick == 1) {
                                    PacketUtil.sendPacket(new C09PacketHeldItemChange(rod - 36));
                                    this.itemState = true;
                                    PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(this.itemStack));
                                    this.rodActionState = true;
                                }

                                if (this.pauseTick >= this.tickDelay()) {
                                    PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(this.itemStack));
                                    this.rodActionState = false;
                                    PacketUtil.sendPacket(new C09PacketHeldItemChange(this.currentItem));
                                    this.itemState = false;
                                    this.pauseTick = 0;
                                }
                            }
                            break;
                        case "NewPacket":
                            if (this.perfectTiming.getValue()) {
                                if (!this.hasThrownRod && this.shouldThrowRod()) {
                                    PacketUtil.sendPacket(new C09PacketHeldItemChange(rod - 36));
                                    this.itemState = true;
                                    PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(this.itemStack));
                                    this.hasThrownRod = true;
                                }

                                if (this.hasThrownRod && (shouldPullRod || this.cancelRun())) {
                                    PacketUtil.sendPacket(new C09PacketHeldItemChange(this.currentItem));
                                    this.itemState = false;
                                    this.hasThrownRod = false;
                                    this.pauseTick = 0;
                                }
                            } else {
                                this.pauseTick++;
                                if (this.pauseTick == 1) {
                                    PacketUtil.sendPacket(new C09PacketHeldItemChange(rod - 36));
                                    this.itemState = true;
                                    PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(this.itemStack));
                                }

                                if (this.pauseTick >= this.tickDelay()) {
                                    PacketUtil.sendPacket(new C09PacketHeldItemChange(this.currentItem));
                                    this.itemState = false;
                                    this.pauseTick = 0;
                                }
                            }
                    }
                }
            }
        }
    }

    private double[] predictedPoint() {
        EntityLivingBase target = this.getKillAuraTarget();
        if (target == null) {
            return new double[]{0.0, 0.0};
        } else if (this.predictMode.getModeString().equals("Custom")) {
            double motionX = target.field_70165_t - target.field_70169_q;
            double motionZ = target.field_70161_v - target.field_70166_s;
            return new double[]{
                motionX * this.predictSize.getValue().floatValue(), motionZ * this.predictSize.getValue().floatValue()
            };
        } else if (this.predictMode.getModeString().equals("ExperimentalFitting")) {
            double motionX = target.field_70165_t - target.field_70169_q;
            double motionZ = target.field_70161_v - target.field_70166_s;
            double bpsX = motionX * 20.0;
            double bpsZ = motionZ * 20.0;
            double fittedX = motionX * rangeFrom(f(bpsX), f(1.0), f(9.8));
            double fittedZ = motionZ * rangeFrom(f(bpsZ), f(1.0), f(9.8));
            return new double[]{fittedX, fittedZ};
        } else {
            return new double[]{0.0, 0.0};
        }
    }

    private static double f(double x) {
        return 0.00428696 * Math.pow(x, 5.0)
            - 0.1235 * Math.pow(x, 4.0)
            + 1.32092 * Math.pow(x, 3.0)
            - 6.35726 * Math.pow(x, 2.0)
            + 12.732 * x;
    }

    private static double rangeFrom(double value, double min, double max) {
        return min > max ? Math.max(max, Math.min(min, value)) : Math.max(min, Math.min(max, value));
    }

    private double distance() {
        EntityLivingBase target = this.getKillAuraTarget();
        return mc.field_71439_g != null && target != null ? BackTrackUtil.getDistanceToEntityBox(target) : 0.0;
    }

    private int delay() {
        if (this.smartDelay.getValue()) {
            double dist = this.distance();
            double calculated = 1880.0 / (1.0 + 18.71F * Math.pow(2.7182818285, -0.2076F * dist)) / 100.0;
            int rounded = (int)Math.round(calculated) * 100;
            return Math.max(200, Math.min(650, rounded));
        } else {
            int min = Math.min(this.minDelay.getValue(), this.maxDelayValue.getValue());
            int max = Math.max(this.minDelay.getValue(), this.maxDelayValue.getValue());
            return ThreadLocalRandom.current().nextInt(min, max + 1);
        }
    }

    private int tickDelay() {
        return (int)Math.ceil(this.delay() / 50.0);
    }

    private boolean rodTiming() {
        EntityLivingBase target = this.getKillAuraTarget();
        return target != null && target.field_70737_aN <= 3 + this.tickDelay();
    }

    private boolean isInRodRange() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        float killAuraRange = killAura == null ? 0.0F : killAura.attackRange.getValue();
        return this.distance() > killAuraRange && this.distance() <= this.maxRange.getValue().floatValue();
    }

    private void reset() {
        this.pauseTick = 0;
        this.rodActionState = false;
        this.itemState = false;
        this.hasThrownRod = false;
    }

    private int getRod() {
        for (int i = 36; i < 45; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.field_70462_a[i - 36];
            if (stack != null && stack.func_77973_b() == Items.field_151112_aM) {
                return i - 36;
            }
        }

        return -1;
    }

    private boolean cancelRun() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        EntityLivingBase target = killAura == null ? null : killAura.getTarget();
        if (killAura != null && killAura.isEnabled() && target != null) {
            if (this.getRod() == -1) {
                return true;
            } else {
                return !this.isInRodRange()
                    ? true
                    : this.smartDelay.getValue() && this.smartRodTiming.getValue() && !this.rodTiming();
            }
        } else {
            return true;
        }
    }

    private boolean shouldThrowRod() {
        if (this.cancelRun()) {
            return false;
        }

        if (!this.perfectTiming.getValue()) {
            return true;
        }

        EntityLivingBase target = this.getKillAuraTarget();
        return target != null && target.field_70737_aN == 9;
    }

    private EntityLivingBase getKillAuraTarget() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        return killAura == null ? null : killAura.getTarget();
    }

    private void swapItem(int slot) {
        mc.field_71439_g.field_71071_by.field_70461_c = Math.max(0, Math.min(8, slot));
    }
}
