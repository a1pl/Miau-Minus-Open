package miau.module.modules.combat.velocity;

import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.property.properties.FloatProperty;
import net.minecraft.entity.player.EntityPlayer;

public class AACv4Velocity extends VelocityMode {
    public final FloatProperty aacv4MotionReducer = new FloatProperty("motion-reducer", 0.62F, 0.0F, 1.0F);

    public AACv4Velocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (player.field_70737_aN > 0 && !player.field_70122_E) {
                    VelocityUtil.reduceXZ(this.aacv4MotionReducer.getValue().floatValue());
                }
            }
        }
    }
}
