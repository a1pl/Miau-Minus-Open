package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorKeyBinding;
import miau.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;

public class AntiAFK extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private int lastInput;

    public AntiAFK() {
        super("AntiAFK", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE && this.isEnabled()) {
            GameSettings gameSettings = mc.field_71474_y;
            if (gameSettings.field_74314_A.func_151468_f()
                || gameSettings.field_74366_z.func_151468_f()
                || gameSettings.field_74351_w.func_151468_f()
                || gameSettings.field_74370_x.func_151468_f()
                || gameSettings.field_74368_y.func_151468_f()) {
                this.lastInput = 0;
            }

            this.lastInput++;
            if (this.lastInput < 200) {
                return;
            }

            if (mc.field_71439_g.field_70173_aa % 5 == 0) {
                ((IAccessorKeyBinding)mc.field_71474_y.field_74366_z).setPressed(false);
                ((IAccessorKeyBinding)mc.field_71474_y.field_74370_x).setPressed(false);
                ((IAccessorKeyBinding)mc.field_71474_y.field_74314_A).setPressed(false);
            }

            if (mc.field_71439_g.field_70173_aa % 20 == 0) {
                if (mc.field_71439_g.field_70173_aa % 40 == 0) {
                    ((IAccessorKeyBinding)mc.field_71474_y.field_74366_z).setPressed(true);
                } else {
                    ((IAccessorKeyBinding)mc.field_71474_y.field_74370_x).setPressed(true);
                }
            }

            if (mc.field_71439_g.field_70173_aa % 100 == 0) {
                ((IAccessorKeyBinding)mc.field_71474_y.field_74314_A).setPressed(true);
            }
        }
    }
}
