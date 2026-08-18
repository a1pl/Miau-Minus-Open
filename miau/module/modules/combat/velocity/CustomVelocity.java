package miau.module.modules.combat.velocity;

import miau.event.impl.AttackEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.StrafeEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.client.ChatUtil;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class CustomVelocity extends VelocityMode {
    public final ModeProperty SprintControl = new ModeProperty(
        "sprint-control", 2, new String[]{"KeepSprint", "StopSprint", "NoControl"}
    );
    public final BooleanProperty tryForward = new BooleanProperty("try-forward", false);
    public final FloatProperty tryingForwardTime = new FloatProperty("try-forward-time", 50.0F, 0.0F, 1000.0F);
    public final BooleanProperty disableStrafeInput = new BooleanProperty("disable-strafe-input", false);
    public final FloatProperty disableStrafeInputTime = new FloatProperty(
        "disable-strafe-input-time", 50.0F, 0.0F, 1000.0F
    );
    public final ModeProperty progressivemode = new ModeProperty(
        "progressive-mode", 0, new String[]{"Decrease", "Increase"}
    );
    public final FloatProperty progressivestepfactor = new FloatProperty("progressive-step-factor", 0.02F, 0.0F, 1.0F);
    public final FloatProperty maxProgressiveFactor = new FloatProperty("max-progressive-factor", 1.0F, 0.0F, 2.0F);
    public final FloatProperty minProgressiveFactor = new FloatProperty("min-progressive-factor", 0.1F, 0.0F, 1.0F);
    public final BooleanProperty attackHelper = new BooleanProperty("attack-helper", false);
    public final BooleanProperty customAttackReduce = new BooleanProperty("custom-attack-reduce", false);
    public final BooleanProperty checkRotation = new BooleanProperty("check-rotation", false);
    public final IntProperty allowHurtTimeMin = new IntProperty("allow-hurt-time-min", 1, 0, 10);
    public final IntProperty allowHurtTimeMax = new IntProperty("allow-hurt-time-max", 10, 0, 10);
    public final FloatProperty activeRange = new FloatProperty("active-range", 3.0F, 1.0F, 6.0F);
    public final BooleanProperty specialReduce = new BooleanProperty("special-reduce", false);
    public final IntProperty specialReduceHurtTime = new IntProperty("special-reduce-hurt-time", 7, 1, 10);
    public final FloatProperty specialReduceFactor = new FloatProperty("special-reduce-factor", 0.5F, 0.0F, 1.0F);
    public final BooleanProperty specialMultiReduce = new BooleanProperty("special-multi-reduce", false);
    public final BooleanProperty randomizeXZ = new BooleanProperty("randomize-xz", false);
    public final FloatProperty minXZReduce = new FloatProperty("min-xz-reduce", 0.1F, 0.0F, 1.0F);
    public final FloatProperty maxXZReduce = new FloatProperty("max-xz-reduce", 1.0F, 0.0F, 1.0F);
    public final BooleanProperty randomizeY = new BooleanProperty("randomize-y", false);
    public final FloatProperty minYReduce = new FloatProperty("min-y-reduce", 0.1F, 0.0F, 1.0F);
    public final FloatProperty maxYReduce = new FloatProperty("max-y-reduce", 1.0F, 0.0F, 1.0F);
    public final BooleanProperty multiReduce = new BooleanProperty("multi-reduce", false);
    public final IntProperty maxTriggerTimes = new IntProperty("max-trigger-times", 3, 1, 10);
    public final FloatProperty attackReduceFactor = new FloatProperty("attack-reduce-factor", 0.5F, 0.0F, 1.0F);
    public final FloatProperty attackReduceYFactor = new FloatProperty("attack-reduce-y-factor", 1.0F, 0.0F, 1.0F);
    public final BooleanProperty doubleReduceWhenFirstReduce = new BooleanProperty("double-reduce-when-first", false);
    public final FloatProperty doubleReduceFactor = new FloatProperty("double-reduce-factor", 0.5F, 0.0F, 1.0F);
    public final IntProperty enableWhy = new IntProperty("trigger-times", 1, 1, 5);
    public final BooleanProperty progressiveFactor = new BooleanProperty("progressive-factor", false);
    public final IntProperty attackReduceMinHurtTime = new IntProperty("attack-reduce-min-hurt-time", 1, 0, 10);
    public final IntProperty attackReduceMaxHurtTime = new IntProperty("attack-reduce-max-hurt-time", 10, 0, 10);
    public final BooleanProperty attackReduceOnlyWhenBackward = new BooleanProperty(
        "attack-reduce-only-when-backward", false
    );
    public final BooleanProperty customTimer = new BooleanProperty("custom-timer", false);
    public final BooleanProperty customTimerOnlyWhenReceivedVelocity = new BooleanProperty(
        "custom-timer-only-when-received", false
    );
    public final ModeProperty customTimerTimeMode = new ModeProperty(
        "custom-timer-time-mode", 0, new String[]{"HurtTime", "MSTimer"}
    );
    public final IntProperty customTimerLowMinHurtTime = new IntProperty("custom-timer-low-min-hurt-time", 2, 0, 10);
    public final IntProperty customTimerMinWorkHurtTime = new IntProperty("custom-timer-min-work-hurt-time", 7, 0, 10);
    public final FloatProperty customTimerLowTimer = new FloatProperty("custom-timer-low-timer", 0.8F, 0.1F, 2.0F);
    public final FloatProperty customTimerMaxTimer = new FloatProperty("custom-timer-max-timer", 1.1F, 0.1F, 2.0F);
    public final FloatProperty customTimerLowMSTimer = new FloatProperty("custom-timer-low-ms", 200.0F, 0.0F, 2000.0F);
    public final FloatProperty customTimerMinWorkMSTimer = new FloatProperty(
        "custom-timer-min-work-ms", 500.0F, 0.0F, 2000.0F
    );
    public final BooleanProperty customTimerC03 = new BooleanProperty("custom-timer-c03", false);
    public final BooleanProperty customJumpReset = new BooleanProperty("custom-jump-reset", false);
    public final IntProperty customChance = new IntProperty("custom-chance", 50, 0, 100);
    public final IntProperty jumpResetMinHurtTime = new IntProperty("jump-reset-min-hurt-time", 1, 0, 10);
    public final IntProperty jumpResetMaxHurtTime = new IntProperty("jump-reset-max-hurt-time", 10, 0, 10);
    public final BooleanProperty customJumpResetSafe = new BooleanProperty("custom-jump-reset-safe", false);
    public final BooleanProperty jumpResetOnlyOnSwing = new BooleanProperty("jump-reset-only-on-swing", false);
    public final ModeProperty afterJumpSprintControl = new ModeProperty(
        "after-jump-sprint-control", 0, new String[]{"None", "Stop", "Sprint"}
    );
    public final BooleanProperty debugger = new BooleanProperty("debugger", false);
    private boolean hasReceivedVelocity = false;
    private boolean hasReceivedVelocity2 = false;
    private boolean hasReceivedVelocity3 = false;
    private boolean doubleReduce = false;
    private boolean triggerTimesSpecial = false;
    private int triggerTimes = 0;
    private float progressiveXZFactor = 0.0F;
    private long timerChangeTime = 0L;
    private long forwardTime = 0L;
    private long strafeControlTime = 0L;
    private boolean tryingForward = false;
    private boolean disablingStrafeInput = false;
    private int limitUntilJump = 0;

    public CustomVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.triggerTimes = 0;
        this.triggerTimesSpecial = false;
        this.progressiveXZFactor = this.attackReduceFactor.getValue();
        this.hasReceivedVelocity = false;
        this.hasReceivedVelocity2 = false;
        this.hasReceivedVelocity3 = false;
        this.doubleReduce = true;
        this.tryingForward = false;
        this.disablingStrafeInput = false;
        this.limitUntilJump = 0;
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (!event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (event.getType() == EventType.RECEIVE && event.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                    if (packet.func_149412_c() == player.func_145782_y()) {
                        this.triggerTimes = 0;
                        this.progressiveXZFactor = this.attackReduceFactor.getValue();
                        this.hasReceivedVelocity = true;
                        this.hasReceivedVelocity2 = true;
                        this.hasReceivedVelocity3 = true;
                        this.doubleReduce = true;
                        this.triggerTimesSpecial = false;
                        this.timerChangeTime = System.currentTimeMillis();
                    }
                }

                if (this.customTimerC03.getValue()
                    && event.getPacket() instanceof C03PacketPlayer
                    && !(event.getPacket() instanceof C04PacketPlayerPosition)
                    && !(event.getPacket() instanceof C05PacketPlayerLook)
                    && !(event.getPacket() instanceof C06PacketPlayerPosLook)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (this.progressiveXZFactor > this.maxProgressiveFactor.getValue()) {
                    this.progressiveXZFactor = this.maxProgressiveFactor.getValue();
                } else if (this.progressiveXZFactor < this.minProgressiveFactor.getValue()) {
                    this.progressiveXZFactor = this.minProgressiveFactor.getValue();
                }

                if (this.hasReceivedVelocity2
                    && player.field_70737_aN <= this.allowHurtTimeMax.getValue()
                    && player.field_70737_aN >= this.allowHurtTimeMin.getValue()
                    && this.attackHelper.getValue()
                    && this.customAttackReduce.getValue()
                    && player.field_70737_aN == this.specialReduceHurtTime.getValue()
                    && player.field_70737_aN != 0) {
                    Entity target = this.findTarget();
                    if (target != null && target instanceof EntityLivingBase) {
                        player.func_71038_i();
                        player.func_71059_n(target);
                        if (this.debugger.getValue()) {
                            ChatUtil.display("AttackHelper | Attacked");
                        }
                    }
                }

                if (player.field_70737_aN == 0 && this.attackHelper.getValue()) {
                    this.hasReceivedVelocity2 = false;
                }

                if (player.field_70737_aN == 0 && this.customTimer.getValue()) {
                    this.hasReceivedVelocity3 = false;
                }

                if (this.customTimer.getValue()) {
                    if (this.customTimerOnlyWhenReceivedVelocity.getValue()) {
                        if (this.customTimerTimeMode.getValue() == 0) {
                            if (player.field_70737_aN >= this.customTimerLowMinHurtTime.getValue()
                                && this.hasReceivedVelocity3) {
                                VelocityUtil.changeTimer(this.customTimerLowTimer.getValue());
                            } else if (!player.field_70122_E
                                && player.field_70737_aN >= this.customTimerMinWorkHurtTime.getValue()) {
                                VelocityUtil.changeTimer(this.customTimerMaxTimer.getValue());
                            } else if (player.field_70737_aN == 0) {
                                VelocityUtil.changeTimer(1.0F);
                            }
                        } else if (this.hasReceivedVelocity3) {
                            long elapsed = System.currentTimeMillis() - this.timerChangeTime;
                            if ((float)elapsed <= this.customTimerLowMSTimer.getValue() && player.field_70737_aN != 0) {
                                VelocityUtil.changeTimer(this.customTimerLowTimer.getValue());
                            } else if ((float)elapsed >= this.customTimerLowMSTimer.getValue()
                                && (float)elapsed <= this.customTimerMinWorkMSTimer.getValue()
                                && player.field_70737_aN != 0) {
                                VelocityUtil.changeTimer(this.customTimerMaxTimer.getValue());
                            } else if (player.field_70737_aN == 0) {
                                VelocityUtil.changeTimer(1.0F);
                            }
                        }
                    } else if (this.customTimerTimeMode.getValue() == 0) {
                        if (player.field_70737_aN >= this.customTimerLowMinHurtTime.getValue()) {
                            VelocityUtil.changeTimer(this.customTimerLowTimer.getValue());
                        } else if (!player.field_70122_E
                            && player.field_70737_aN >= this.customTimerMinWorkHurtTime.getValue()) {
                            VelocityUtil.changeTimer(this.customTimerMaxTimer.getValue());
                        } else if (player.field_70737_aN == 0) {
                            VelocityUtil.changeTimer(1.0F);
                        }
                    } else {
                        long elapsed = System.currentTimeMillis() - this.timerChangeTime;
                        if ((float)elapsed <= this.customTimerLowMSTimer.getValue() && player.field_70737_aN != 0) {
                            VelocityUtil.changeTimer(this.customTimerLowTimer.getValue());
                        } else if ((float)elapsed >= this.customTimerLowMSTimer.getValue()
                            && (float)elapsed <= this.customTimerMinWorkMSTimer.getValue()
                            && player.field_70737_aN != 0) {
                            VelocityUtil.changeTimer(this.customTimerMaxTimer.getValue());
                        } else if (player.field_70737_aN == 0) {
                            VelocityUtil.changeTimer(1.0F);
                        }
                    }

                    long elapsedForward = System.currentTimeMillis() - this.forwardTime;
                    if (this.tryForward.getValue() && this.tryingForward && player.field_70737_aN != 0) {
                        if ((float)elapsedForward <= this.tryingForwardTime.getValue()) {
                            ((EntityPlayerSP)player).field_71158_b.field_78900_b = 1.0F;
                            if (this.SprintControl.getValue() == 0) {
                                VelocityUtil.changeSprint(true);
                            } else if (this.SprintControl.getValue() == 1) {
                                VelocityUtil.changeSprint(false);
                            }
                        }
                    } else if (player.field_70737_aN == 0 || (float)elapsedForward >= this.tryingForwardTime.getValue()
                        )
                     {
                        this.tryingForward = false;
                    }

                    long elapsedStrafe = System.currentTimeMillis() - this.strafeControlTime;
                    if (this.disableStrafeInput.getValue() && this.disablingStrafeInput && player.field_70737_aN != 0) {
                        if ((float)elapsedStrafe <= this.disableStrafeInputTime.getValue()) {
                            ((EntityPlayerSP)player).field_71158_b.field_78902_a = 0.0F;
                        }
                    } else if (player.field_70737_aN == 0
                        || (float)elapsedStrafe >= this.disableStrafeInputTime.getValue()) {
                        this.disablingStrafeInput = false;
                    }
                }
            }
        }
    }

    @Override
    public void onAttack(AttackEvent event) {
        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player != null) {
            if (!this.attackReduceOnlyWhenBackward.getValue()
                || !this.customAttackReduce.getValue()
                || VelocityUtil.isMovingBackwards()) {
                if (player.field_70737_aN == this.specialReduceHurtTime.getValue()
                    && this.specialReduce.getValue()
                    && this.customAttackReduce.getValue()
                    && !this.triggerTimesSpecial) {
                    float finalXZ = this.specialReduceFactor.getValue();
                    if (this.randomizeXZ.getValue()) {
                        finalXZ *= VelocityUtil.randomFloat(this.minXZReduce.getValue(), this.maxXZReduce.getValue());
                    }

                    VelocityUtil.reduceXZ(finalXZ);
                    this.triggerTimesSpecial = !this.specialMultiReduce.getValue();
                    if (this.debugger.getValue()) {
                        ChatUtil.display("[SpecialReduce] XZ=" + finalXZ);
                    }

                    if (this.disableStrafeInput.getValue()) {
                        this.disablingStrafeInput = true;
                        this.strafeControlTime = System.currentTimeMillis();
                    }

                    if (this.tryForward.getValue()) {
                        this.tryingForward = true;
                        this.forwardTime = System.currentTimeMillis();
                    }
                }

                if (player.field_70737_aN <= this.attackReduceMaxHurtTime.getValue()
                    && player.field_70737_aN >= this.attackReduceMinHurtTime.getValue()
                    && this.hasReceivedVelocity
                    && this.customAttackReduce.getValue()) {
                    if (this.multiReduce.getValue()
                        && this.triggerTimes < this.maxTriggerTimes.getValue()
                        && !this.progressiveFactor.getValue()) {
                        float finalXZ = this.attackReduceFactor.getValue();
                        float finalY = this.attackReduceYFactor.getValue();
                        if (this.randomizeXZ.getValue()) {
                            finalXZ *= VelocityUtil.randomFloat(
                                this.minXZReduce.getValue(), this.maxXZReduce.getValue()
                            );
                        }

                        if (this.randomizeY.getValue()) {
                            finalY *= VelocityUtil.randomFloat(this.minYReduce.getValue(), this.maxYReduce.getValue());
                        }

                        VelocityUtil.reduceXZ(finalXZ);
                        VelocityUtil.reduceY(finalY);
                        this.triggerTimes++;
                        if (this.doubleReduceWhenFirstReduce.getValue() && this.doubleReduce) {
                            VelocityUtil.reduceXZ(finalXZ);
                            this.doubleReduce = false;
                        }

                        if (this.debugger.getValue()) {
                            ChatUtil.display(
                                "[AttackReduce] XZ="
                                    + finalXZ
                                    + " Trigs="
                                    + this.triggerTimes
                                    + "/"
                                    + this.maxTriggerTimes.getValue()
                            );
                        }

                        if (this.disableStrafeInput.getValue()) {
                            this.disablingStrafeInput = true;
                            this.strafeControlTime = System.currentTimeMillis();
                        }

                        if (this.tryForward.getValue()) {
                            this.tryingForward = true;
                            this.forwardTime = System.currentTimeMillis();
                        }

                        if (player.field_70737_aN < this.attackReduceMinHurtTime.getValue()) {
                            this.hasReceivedVelocity = false;
                            this.triggerTimes = 0;
                        }
                    } else if (!this.multiReduce.getValue()) {
                        float finalXZ = this.attackReduceFactor.getValue();
                        float finalY = this.attackReduceYFactor.getValue();
                        if (this.randomizeXZ.getValue()) {
                            finalXZ *= VelocityUtil.randomFloat(
                                this.minXZReduce.getValue(), this.maxXZReduce.getValue()
                            );
                        }

                        if (this.randomizeY.getValue()) {
                            finalY *= VelocityUtil.randomFloat(this.minYReduce.getValue(), this.maxYReduce.getValue());
                        }

                        VelocityUtil.reduceXZ(finalXZ);
                        VelocityUtil.reduceY(finalY);
                        if (this.doubleReduceWhenFirstReduce.getValue() && this.doubleReduce) {
                            VelocityUtil.reduceXZ(finalXZ);
                            this.doubleReduce = false;
                        }

                        if (this.debugger.getValue()) {
                            ChatUtil.display("[AttackReduce] XZ=" + finalXZ + " Y=" + finalY);
                        }

                        if (this.disableStrafeInput.getValue()) {
                            this.disablingStrafeInput = true;
                            this.strafeControlTime = System.currentTimeMillis();
                        }

                        if (this.tryForward.getValue()) {
                            this.tryingForward = true;
                            this.forwardTime = System.currentTimeMillis();
                        }

                        this.hasReceivedVelocity = false;
                    } else if (this.progressiveFactor.getValue()
                        && this.multiReduce.getValue()
                        && this.triggerTimes < this.maxTriggerTimes.getValue()) {
                        if (this.triggerTimes == 0) {
                            this.progressiveXZFactor = this.attackReduceFactor.getValue();
                        }

                        float finalProgressive = this.progressiveXZFactor;
                        float finalY = this.attackReduceYFactor.getValue();
                        if (this.randomizeXZ.getValue()) {
                            finalProgressive *= VelocityUtil.randomFloat(
                                this.minXZReduce.getValue(), this.maxXZReduce.getValue()
                            );
                        }

                        if (this.randomizeY.getValue()) {
                            finalY *= VelocityUtil.randomFloat(this.minYReduce.getValue(), this.maxYReduce.getValue());
                        }

                        VelocityUtil.reduceXZ(finalProgressive);
                        VelocityUtil.reduceY(finalY);
                        this.triggerTimes++;
                        if (this.doubleReduceWhenFirstReduce.getValue() && this.doubleReduce) {
                            VelocityUtil.reduceXZ(finalProgressive);
                            this.doubleReduce = false;
                        }

                        if (this.debugger.getValue()) {
                            ChatUtil.display(
                                "[ProgressiveReduce] XZ="
                                    + finalProgressive
                                    + " Trigs="
                                    + this.triggerTimes
                                    + "/"
                                    + this.maxTriggerTimes.getValue()
                            );
                        }

                        this.progressiveXZFactor = this.progressivemode.getValue() == 0
                            ? this.progressiveXZFactor - this.progressivestepfactor.getValue()
                            : this.progressiveXZFactor + this.progressivestepfactor.getValue();
                        this.progressiveXZFactor = Math.max(
                            this.minProgressiveFactor.getValue(),
                            Math.min(this.maxProgressiveFactor.getValue(), this.progressiveXZFactor)
                        );
                        if (this.disableStrafeInput.getValue()) {
                            this.disablingStrafeInput = true;
                            this.strafeControlTime = System.currentTimeMillis();
                        }

                        if (this.tryForward.getValue()) {
                            this.tryingForward = true;
                            this.forwardTime = System.currentTimeMillis();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onStrafe(StrafeEvent event) {
        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player != null) {
            if (this.customJumpReset.getValue() && this.hasReceivedVelocity) {
                boolean ready = VelocityUtil.randomInt(0, 100) < this.customChance.getValue()
                    && player.field_70122_E
                    && player.field_70737_aN <= this.jumpResetMaxHurtTime.getValue()
                    && player.field_70737_aN >= this.jumpResetMinHurtTime.getValue();
                if (ready) {
                    if (this.customJumpResetSafe.getValue() && VelocityUtil.isInBadEnvironment()) {
                        return;
                    }

                    if (this.jumpResetOnlyOnSwing.getValue() && !player.field_82175_bq) {
                        return;
                    }

                    VelocityUtil.tryJump();
                    this.limitUntilJump = 0;
                    if (this.afterJumpSprintControl.getValue() == 1) {
                        VelocityUtil.changeSprint(false);
                    } else if (this.afterJumpSprintControl.getValue() == 2) {
                        VelocityUtil.changeSprint(true);
                    }

                    if (this.debugger.getValue()) {
                        ChatUtil.display("[JumpReset] Jumped | HurtTime=" + player.field_70737_aN);
                    }
                }
            }
        }
    }

    private Entity findTarget() {
        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player == null) {
            return null;
        }

        Entity entity = Velocity.mc.field_71476_x != null ? Velocity.mc.field_71476_x.field_72308_g : null;
        return (Entity)(entity == null && !this.checkRotation.getValue()
            ? VelocityUtil.getNearestEntityInRange(this.activeRange.getValue())
            : entity);
    }

    @Override
    public void onDisable() {
        this.triggerTimes = 0;
        this.triggerTimesSpecial = false;
        this.hasReceivedVelocity = false;
        this.hasReceivedVelocity2 = false;
        this.hasReceivedVelocity3 = false;
        this.doubleReduce = true;
        this.tryingForward = false;
        this.disablingStrafeInput = false;
        this.limitUntilJump = 0;
        VelocityUtil.changeTimer(1.0F);
    }
}
