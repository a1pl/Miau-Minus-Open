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
import miau.util.client.ChatUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class Intave14Velocity extends VelocityMode {
    public final BooleanProperty onlyWhenBackward = new BooleanProperty("only-when-backward", false);
    public final BooleanProperty finalReverse = new BooleanProperty("final-reverse", false);
    public final FloatProperty finalReverseFactor = new FloatProperty("final-reverse-factor", 0.9F, 0.0F, 2.0F);
    public final BooleanProperty finalReverseStrict = new BooleanProperty("final-reverse-strict", false);
    public final BooleanProperty yReduceTest = new BooleanProperty("y-reduce-test", false);
    public final FloatProperty yReduceCount = new FloatProperty("y-reduce-count", 0.2F, 0.0F, 1.0F);
    public final IntProperty yReduceMaxTimes = new IntProperty("y-reduce-max-times", 1, 1, 20);
    public final IntProperty firstReduce = new IntProperty("first-reduce", 8, 1, 10);
    public final IntProperty secondReduce = new IntProperty("second-reduce", 7, 1, 10);
    public final IntProperty thirdReduce = new IntProperty("third-reduce", 6, 1, 10);
    public final BooleanProperty applyDiffFactorOnGroundOrInAir = new BooleanProperty("apply-diff-factor", false);
    public final IntProperty triggerTimes = new IntProperty("trigger-times", 2, 1, 3);
    public final BooleanProperty intaveMoreReduce = new BooleanProperty("more-reduce", false);
    public final FloatProperty intaveMoreReduceFactor = new FloatProperty("more-reduce-factor", 0.4F, 0.0F, 1.0F);
    public final FloatProperty intaveMoreReduceAnotherFactor = new FloatProperty(
        "more-reduce-another-factor", 0.8F, 0.0F, 1.0F
    );
    public final IntProperty intaveMoreReduceMaxTimes = new IntProperty("more-reduce-max-times", 1, 1, 20);
    public final BooleanProperty intaveMoreReduceExtraReduce = new BooleanProperty("more-reduce-extra-reduce", false);
    public final BooleanProperty intaveTimerTest = new BooleanProperty("timer-test", false);
    public final BooleanProperty intave14Debugger = new BooleanProperty("debug", false);
    private boolean hasReceivedVelocity = false;
    private boolean onGroundTri = false;
    private boolean notTriggered1 = true;
    private boolean notTriggered2 = true;
    private boolean notTriggered3 = true;
    private boolean notTriggeredA = true;
    private boolean finalReverseTriggered = false;
    private int yReduceTriggeredTimes = 0;
    private int finalReverseCondition = 0;
    private int intaveMoreReduceTimes = 0;
    private String reduceCondition = "";

    public Intave14Velocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (event.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                    if (packet.func_149412_c() == player.func_145782_y()) {
                        this.finalReverseTriggered = false;
                        this.hasReceivedVelocity = true;
                        this.notTriggered1 = true;
                        this.notTriggered2 = true;
                        this.notTriggered3 = true;
                        this.notTriggeredA = true;
                        this.yReduceTriggeredTimes = 0;
                        this.finalReverseCondition = 0;
                        this.intaveMoreReduceTimes = 0;
                    }
                }
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (player.field_70737_aN >= 9 && this.hasReceivedVelocity) {
                    this.onGroundTri = player.field_70122_E;
                }

                if (player.field_70737_aN == 0 && this.hasReceivedVelocity) {
                    this.hasReceivedVelocity = false;
                }

                int finalReverseHurtTime = this.triggerTimes.getValue() == 3
                    ? this.thirdReduce.getValue() - 1
                    : (
                        this.triggerTimes.getValue() == 2
                            ? this.secondReduce.getValue() - 1
                            : this.firstReduce.getValue() - 1
                    );
                if (this.finalReverse.getValue()
                    && finalReverseHurtTime == player.field_70737_aN
                    && player.field_70737_aN != 0) {
                    if (!VelocityUtil.isMovingBackwards()) {
                        return;
                    }

                    if (!this.hasReceivedVelocity) {
                        return;
                    }

                    if (this.finalReverseStrict.getValue() && this.triggerTimes.getValue() == 2) {
                        if (this.finalReverseCondition < 2) {
                            return;
                        }
                    } else if (this.finalReverseStrict.getValue()
                        && this.triggerTimes.getValue() == 3
                        && this.finalReverseCondition < 3) {
                        return;
                    }

                    VelocityUtil.reduceXZ(-this.finalReverseFactor.getValue());
                    if (this.intave14Debugger.getValue()) {
                        ChatUtil.display(
                            "FinalReversed [" + this.finalReverseCondition + "/" + this.triggerTimes.getValue() + "]"
                        );
                    }

                    this.finalReverseTriggered = true;
                }

                if (this.intaveTimerTest.getValue()) {
                    if (player.field_70737_aN >= 8) {
                        VelocityUtil.changeTimer(0.3F);
                    } else if (player.field_70737_aN > 2) {
                        VelocityUtil.changeTimer(5.0F);
                    } else if (player.field_70737_aN == 2) {
                        VelocityUtil.changeTimer(1.0F);
                    } else if (player.field_70737_aN == 0) {
                        VelocityUtil.changeTimer(1.0F);
                    }
                }
            }
        }
    }

    @Override
    public void onAttack(AttackEvent event) {
        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player != null) {
            if (!this.onlyWhenBackward.getValue() || VelocityUtil.isMovingBackwards()) {
                if (this.hasReceivedVelocity) {
                    int finalReverseHurtTime = this.triggerTimes.getValue() == 3
                        ? this.thirdReduce.getValue() - 1
                        : (
                            this.triggerTimes.getValue() == 2
                                ? this.secondReduce.getValue() - 1
                                : this.firstReduce.getValue() - 1
                        );
                    if (player.field_70737_aN == this.firstReduce.getValue()
                        && this.triggerTimes.getValue() >= 1
                        && this.notTriggered1) {
                        VelocityUtil.reduceXZ(0.6);
                        this.reduceCondition = this.onGroundTri ? "OnGround" : "InAir";
                        this.yReduce();
                        this.notTriggered1 = false;
                        this.finalReverseCondition++;
                        this.notTriggeredA = false;
                        if (this.intave14Debugger.getValue()) {
                            ChatUtil.display("Reduce | Phase1 | " + this.reduceCondition + " | 60%");
                        }
                    } else if (player.field_70737_aN == this.secondReduce.getValue()
                        && this.triggerTimes.getValue() >= 2
                        && this.notTriggered2) {
                        if (this.notTriggeredA) {
                            VelocityUtil.reduceXZ(0.6);
                            this.notTriggeredA = false;
                        } else {
                            VelocityUtil.reduceXZ(0.35);
                        }

                        this.yReduce();
                        this.reduceCondition = this.onGroundTri ? "OnGround" : "InAir";
                        this.notTriggered2 = false;
                        this.finalReverseCondition++;
                        if (this.intave14Debugger.getValue()) {
                            ChatUtil.display(
                                "Reduce | Phase2 | "
                                    + this.reduceCondition
                                    + " | "
                                    + (this.notTriggeredA ? "60%" : "35%")
                            );
                        }
                    } else if (player.field_70737_aN == this.thirdReduce.getValue()
                        && this.triggerTimes.getValue() >= 3
                        && this.notTriggered3) {
                        if (this.notTriggeredA) {
                            VelocityUtil.reduceXZ(0.6);
                            this.notTriggeredA = false;
                        } else {
                            double factor = this.applyDiffFactorOnGroundOrInAir.getValue() && !this.onGroundTri
                                ? 0.5
                                : 0.15;
                            VelocityUtil.reduceXZ(factor);
                        }

                        this.yReduce();
                        this.reduceCondition = this.onGroundTri ? "OnGround" : "InAir";
                        this.notTriggered3 = false;
                        this.finalReverseCondition++;
                        if (this.intave14Debugger.getValue()) {
                            ChatUtil.display(
                                "Reduce | Phase3 | "
                                    + this.reduceCondition
                                    + " | "
                                    + (
                                        this.notTriggeredA
                                            ? "60%"
                                            : (this.applyDiffFactorOnGroundOrInAir.getValue() ? "50%" : "15%")
                                    )
                            );
                        }
                    } else if (player.field_70737_aN == finalReverseHurtTime) {
                        if (!this.onlyWhenBackward.getValue() || VelocityUtil.isMovingBackwards()) {
                            if (this.finalReverse.getValue()) {
                                if (this.finalReverseStrict.getValue() && this.triggerTimes.getValue() == 2) {
                                    if (this.finalReverseCondition < 2) {
                                        return;
                                    }
                                } else if (this.finalReverseStrict.getValue()
                                    && this.triggerTimes.getValue() == 3
                                    && this.finalReverseCondition < 3) {
                                    return;
                                }

                                if (player.field_70737_aN != 0) {
                                    VelocityUtil.reduceXZ(-this.finalReverseFactor.getValue());
                                    if (this.intave14Debugger.getValue()) {
                                        ChatUtil.display(
                                            "FinalReversed ["
                                                + this.finalReverseCondition
                                                + "/"
                                                + this.triggerTimes.getValue()
                                                + "]"
                                        );
                                    }

                                    this.finalReverseTriggered = true;
                                }
                            }
                        }
                    } else {
                        if (this.intaveMoreReduce.getValue()) {
                            int moreReduceHurtTime = this.triggerTimes.getValue() == 3
                                ? this.thirdReduce.getValue() - 1
                                : (
                                    this.triggerTimes.getValue() == 2
                                        ? this.secondReduce.getValue() - 1
                                        : this.firstReduce.getValue() - 1
                                );
                            if (player.field_70737_aN <= moreReduceHurtTime
                                && player.field_70737_aN > 0
                                && this.intaveMoreReduceTimes < this.intaveMoreReduceMaxTimes.getValue()
                                && !this.finalReverseTriggered) {
                                double factor = (this.intaveMoreReduceExtraReduce.getValue()
                                            && this.notTriggered1
                                            && this.notTriggered2
                                            && this.notTriggered3
                                        ? this.intaveMoreReduceAnotherFactor.getValue()
                                        : this.intaveMoreReduceFactor.getValue())
                                    .floatValue();
                                VelocityUtil.reduceXZ(factor);
                                this.intaveMoreReduceTimes++;
                                if (this.intave14Debugger.getValue()) {
                                    ChatUtil.display("IntaveMoreReduce");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onStrafe(StrafeEvent event) {
    }

    private void yReduce() {
        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player != null
            && this.yReduceTest.getValue()
            && this.yReduceTriggeredTimes < this.yReduceMaxTimes.getValue()) {
            player.field_70181_x = player.field_70181_x - this.yReduceCount.getValue().floatValue();
            this.yReduceTriggeredTimes++;
            if (this.intave14Debugger.getValue()) {
                ChatUtil.display("YReduced");
            }
        }
    }

    @Override
    public void onDisable() {
        this.hasReceivedVelocity = false;
        this.onGroundTri = false;
        this.notTriggered1 = true;
        this.notTriggered2 = true;
        this.notTriggered3 = true;
        this.notTriggeredA = true;
        this.finalReverseTriggered = false;
        this.yReduceTriggeredTimes = 0;
        this.finalReverseCondition = 0;
        this.intaveMoreReduceTimes = 0;
        VelocityUtil.changeTimer(1.0F);
    }
}
