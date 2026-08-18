package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorEntityPlayerSP;
import miau.module.Module;
import net.minecraft.client.Minecraft;

public class PerfectHorseJump extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public PerfectHorseJump() {
        super("PerfectHorseJump", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g == null || mc.field_71441_e == null) {
                return;
            }

            ((IAccessorEntityPlayerSP)mc.field_71439_g).setHorseJumpPowerCounter(9);
            ((IAccessorEntityPlayerSP)mc.field_71439_g).setHorseJumpPower(1.0F);
        }
    }
}
