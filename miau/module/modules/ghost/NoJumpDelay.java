package miau.module.modules.ghost;

import miau.event.EventTarget;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorEntityLivingBase;
import miau.module.Module;
import miau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;

public class NoJumpDelay extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final IntProperty delay = new IntProperty("delay", 3, 0, 8);

    public NoJumpDelay() {
        super("NoJumpDelay", false);
    }

    @EventTarget(0)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            ((IAccessorEntityLivingBase)mc.field_71439_g)
                .setJumpTicks(
                    Math.min(((IAccessorEntityLivingBase)mc.field_71439_g).getJumpTicks(), this.delay.getValue() + 1)
                );
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.delay.getValue().toString()};
    }
}
