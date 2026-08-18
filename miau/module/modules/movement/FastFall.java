package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorMinecraft;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;

public class FastFall extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty boostMode = new ModeProperty(
        "BoostMode", 1, new String[]{"Number", "Factor", "SetMotion"}
    );
    public final FloatProperty boostNumber = new FloatProperty(
        "BoostNumber", 1.0F, 0.01F, 10.0F, () -> this.boostMode.getModeString().equals("Number")
    );
    public final FloatProperty boostFactor = new FloatProperty(
        "BoostFactor", 2.0F, 1.0F, 10.0F, () -> this.boostMode.getModeString().equals("Factor")
    );
    public final FloatProperty setMotionY = new FloatProperty(
        "SetMotionNumber", 0.8F, 0.01F, 10.0F, () -> this.boostMode.getModeString().equals("SetMotion")
    );
    public final BooleanProperty changeTimer = new BooleanProperty("ChangeTimer", false);
    public final IntProperty timers = new IntProperty("Times", 1, 1, 2, () -> this.changeTimer.getValue());
    public final FloatProperty timer1Factor = new FloatProperty(
        "Timer1Factor", 0.5F, 0.01F, 2.0F, () -> this.timers.getValue() >= 1 && this.changeTimer.getValue()
    );
    public final IntProperty timer1Ticks = new IntProperty(
        "Timer1Ticks", 3, 1, 20, () -> this.timers.getValue() >= 1 && this.changeTimer.getValue()
    );
    public final FloatProperty timer2Factor = new FloatProperty(
        "Timer2Factor", 0.5F, 0.01F, 150.0F, () -> this.timers.getValue() >= 2 && this.changeTimer.getValue()
    );
    public final IntProperty timer2Ticks = new IntProperty(
        "Timer2Ticks", 3, 1, 20, () -> this.timers.getValue() >= 2 && this.changeTimer.getValue()
    );
    public final BooleanProperty autoDisable = new BooleanProperty("AutoDisable", false);
    private boolean boosted = false;
    private boolean tick1Start = false;
    private int timer1Tick = 0;
    private boolean tick2Start = false;
    private int timer2Tick = 0;
    private boolean changingTimer = false;

    public FastFall() {
        super("FastFall", false);
    }

    private boolean isFalling() {
        return !mc.field_71439_g.field_70122_E && mc.field_71439_g.field_70181_x < 0.0;
    }

    private void changeTimer(float speed) {
        ((IAccessorMinecraft)mc).getTimer().field_74278_d = speed;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null && mc.field_71441_e != null) {
                if (this.isFalling()) {
                    if (this.boostMode.getModeString().equals("Number")) {
                        if (!this.boosted) {
                            mc.field_71439_g.field_70181_x = mc.field_71439_g.field_70181_x
                                - this.boostNumber.getValue().floatValue();
                        }
                    } else if (this.boostMode.getModeString().equals("Factor")) {
                        if (!this.boosted) {
                            mc.field_71439_g.field_70181_x = mc.field_71439_g.field_70181_x
                                * this.boostFactor.getValue().floatValue();
                        }
                    } else if (this.boostMode.getModeString().equals("SetMotion") && !this.boosted) {
                        mc.field_71439_g.field_70181_x = -this.setMotionY.getValue();
                    }

                    this.boosted = true;
                }

                if (!this.isFalling() || mc.field_71439_g.field_70122_E && this.boosted) {
                    if (this.autoDisable.getValue()) {
                        this.setEnabled(false);
                    }

                    if (this.changeTimer.getValue()) {
                        this.changeTimer(1.0F);
                    }

                    this.tick1Start = false;
                    this.tick2Start = false;
                    this.timer1Tick = 0;
                    this.timer2Tick = 0;
                    this.changingTimer = false;
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null && mc.field_71441_e != null) {
                if (this.boosted
                    && this.changeTimer.getValue()
                    && !mc.field_71439_g.field_70122_E
                    && !this.changingTimer) {
                    this.changingTimer = true;
                }

                if (this.boosted && this.changeTimer.getValue() && this.changingTimer) {
                    if (mc.field_71439_g.field_70122_E) {
                        this.changeTimer(1.0F);
                        return;
                    }

                    if (!this.tick1Start && !this.tick2Start) {
                        if (this.timer1Tick > this.timer1Ticks.getValue() && this.changingTimer && this.tick1Start) {
                            this.tick1Start = false;
                            this.tick2Start = true;
                            return;
                        }

                        this.tick1Start = true;
                        this.changeTimer(this.timer1Factor.getValue());
                        this.timer1Tick++;
                    }

                    if (!this.tick1Start && this.tick2Start) {
                        if (this.timer2Tick > this.timer2Ticks.getValue() && this.changingTimer && this.tick2Start) {
                            this.tick2Start = false;
                            this.changingTimer = false;
                            return;
                        }

                        this.changeTimer(this.timer2Factor.getValue());
                        this.timer2Tick++;
                    }
                }
            }
        }
    }

    @Override
    public void onEnabled() {
        this.boosted = false;
        this.tick1Start = false;
        this.tick2Start = false;
        this.timer1Tick = 0;
        this.timer2Tick = 0;
        this.changingTimer = false;
    }

    @Override
    public void onDisabled() {
        if (this.changeTimer.getValue()) {
            this.changeTimer(1.0F);
        }
    }
}
