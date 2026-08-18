package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;

public class AirJump extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"VanillaJump", "Motion"});
    public final IntProperty cooldown = new IntProperty("CoolDown", 5, 0, 20);
    private int cooldownCounter = 0;

    public AirJump() {
        super("AirJump", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null && mc.field_71441_e != null) {
                if (mc.field_71474_y.field_74314_A.func_151470_d() || mc.field_71474_y.field_74314_A.func_151468_f()) {
                    if (this.cooldownCounter != 0) {
                        return;
                    }

                    if (this.mode.getModeString().equals("VanillaJump")) {
                        mc.field_71439_g.func_70664_aZ();
                    } else {
                        mc.field_71439_g.field_70181_x = 0.42;
                    }

                    this.cooldownCounter = this.cooldown.getValue();
                }

                if (this.cooldownCounter != 0) {
                    this.cooldownCounter--;
                }
            }
        }
    }
}
