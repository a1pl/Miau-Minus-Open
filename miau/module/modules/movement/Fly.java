package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.StrafeEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.FloatProperty;
import miau.util.client.KeyBindUtil;
import miau.util.player.MoveUtil;
import net.minecraft.client.Minecraft;

public class Fly extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private double verticalMotion = 0.0;
    public final FloatProperty hSpeed = new FloatProperty("horizontal-speed", 1.0F, 0.0F, 100.0F);
    public final FloatProperty vSpeed = new FloatProperty("vertical-speed", 1.0F, 0.0F, 100.0F);

    public Fly() {
        super("Fly", false);
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled()) {
            if (mc.field_71439_g.field_70163_u % 1.0 != 0.0) {
                mc.field_71439_g.field_70181_x = this.verticalMotion;
            }

            MoveUtil.setSpeed(0.0);
            event.setFriction((float)MoveUtil.getBaseMoveSpeed() * this.hSpeed.getValue());
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            this.verticalMotion = 0.0;
            if (mc.field_71462_r == null) {
                if (KeyBindUtil.isKeyDown(mc.field_71474_y.field_74314_A.func_151463_i())) {
                    this.verticalMotion = this.verticalMotion + this.vSpeed.getValue().doubleValue() * 0.42F;
                }

                if (KeyBindUtil.isKeyDown(mc.field_71474_y.field_74311_E.func_151463_i())) {
                    this.verticalMotion = this.verticalMotion - this.vSpeed.getValue().doubleValue() * 0.42F;
                }

                KeyBindUtil.setKeyBindState(mc.field_71474_y.field_74311_E.func_151463_i(), false);
            }
        }
    }

    @Override
    public void onDisabled() {
        mc.field_71439_g.field_70181_x = 0.0;
        MoveUtil.setSpeed(0.0);
        KeyBindUtil.updateKeyState(mc.field_71474_y.field_74311_E.func_151463_i());
    }
}
