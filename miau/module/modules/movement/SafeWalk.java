package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.SafeWalkEvent;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;

public class SafeWalk extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty onlyGround = new BooleanProperty("only-ground", false);
    public final BooleanProperty pitchLimit = new BooleanProperty("pitch", false);
    public final FloatProperty pitchBound = new FloatProperty(
        "pitch-bound", 0.0F, 90.0F, 0.0F, 90.0F, this.pitchLimit::getValue
    );

    public SafeWalk() {
        super("SafeWalk", false);
    }

    private boolean canSafeWalk() {
        if (mc.field_71439_g == null) {
            return false;
        } else {
            return this.onlyGround.getValue() && !mc.field_71439_g.field_70122_E
                ? false
                : !this.pitchLimit.getValue()
                    || mc.field_71439_g.field_70125_A < this.pitchBound.getSecondValue()
                        && mc.field_71439_g.field_70125_A > this.pitchBound.getValue();
        }
    }

    @EventTarget
    public void onMove(SafeWalkEvent event) {
        if (this.isEnabled() && this.canSafeWalk()) {
            event.setSafeWalk(true);
        }
    }
}
