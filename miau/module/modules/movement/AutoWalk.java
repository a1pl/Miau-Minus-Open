package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

public class AutoWalk extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty forward = new BooleanProperty("Forward", true);
    public final BooleanProperty backward = new BooleanProperty("Backward", false);
    public final BooleanProperty left = new BooleanProperty("Left", false);
    public final BooleanProperty right = new BooleanProperty("Right", false);
    public final BooleanProperty autoDisable = new BooleanProperty("AutoDisable", false);
    public final IntProperty disableTime = new IntProperty(
        "DisableTime", 1000, 0, 100000, () -> this.autoDisable.getValue()
    );
    private final TimerUtil disableTimer = new TimerUtil();

    public AutoWalk() {
        super("AutoWalk", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.forward.getValue()) {
                KeyBinding.func_74510_a(mc.field_71474_y.field_74351_w.func_151463_i(), true);
                if (!this.backward.getValue()) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74368_y.func_151463_i(), false);
                }

                if (!this.left.getValue()) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74370_x.func_151463_i(), false);
                }

                if (!this.right.getValue()) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74366_z.func_151463_i(), false);
                }
            }

            if (this.backward.getValue()) {
                KeyBinding.func_74510_a(mc.field_71474_y.field_74368_y.func_151463_i(), true);
                if (this.forward.getValue()) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74351_w.func_151463_i(), false);
                }

                if (!this.left.getValue()) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74370_x.func_151463_i(), false);
                }

                if (!this.right.getValue()) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74366_z.func_151463_i(), false);
                }
            }

            if (this.left.getValue()) {
                KeyBinding.func_74510_a(mc.field_71474_y.field_74370_x.func_151463_i(), true);
                if (this.forward.getValue()) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74351_w.func_151463_i(), false);
                }

                if (!this.backward.getValue()) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74368_y.func_151463_i(), false);
                }

                if (!this.right.getValue()) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74366_z.func_151463_i(), false);
                }
            }

            if (this.right.getValue()) {
                KeyBinding.func_74510_a(mc.field_71474_y.field_74366_z.func_151463_i(), true);
                if (this.forward.getValue()) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74351_w.func_151463_i(), false);
                }

                if (!this.backward.getValue()) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74368_y.func_151463_i(), false);
                }

                if (!this.left.getValue()) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74370_x.func_151463_i(), false);
                }
            }

            if (this.autoDisable.getValue() && this.disableTimer.hasTimeElapsed(this.disableTime.getValue().intValue())
                )
             {
                this.disableTimer.reset();
                this.setEnabled(false);
            }
        }
    }

    @Override
    public void onDisabled() {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            if (!GameSettings.func_100015_a(mc.field_71474_y.field_74351_w)) {
                KeyBinding.func_74510_a(mc.field_71474_y.field_74351_w.func_151463_i(), false);
            }

            if (!GameSettings.func_100015_a(mc.field_71474_y.field_74368_y)) {
                KeyBinding.func_74510_a(mc.field_71474_y.field_74368_y.func_151463_i(), false);
            }

            if (!GameSettings.func_100015_a(mc.field_71474_y.field_74370_x)) {
                KeyBinding.func_74510_a(mc.field_71474_y.field_74370_x.func_151463_i(), false);
            }

            if (!GameSettings.func_100015_a(mc.field_71474_y.field_74366_z)) {
                KeyBinding.func_74510_a(mc.field_71474_y.field_74366_z.func_151463_i(), false);
            }

            this.disableTimer.reset();
        }
    }

    @Override
    public void onEnabled() {
        this.disableTimer.reset();
    }
}
