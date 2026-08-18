package miau.module.modules.misc;

import miau.event.EventTarget;
import miau.event.impl.MoveInputEvent;
import miau.management.RotationState;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.util.player.MoveUtil;
import net.minecraft.client.Minecraft;

public class MoveFix extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty groundOnly = new BooleanProperty("ground-only", false);

    public MoveFix() {
        super("MoveFix", false, false);
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null) {
            if (!this.groundOnly.getValue() || mc.field_71439_g.field_70122_E) {
                if (RotationState.isActived() && MoveUtil.isForwardPressed()) {
                    MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
                }
            }
        }
    }
}
