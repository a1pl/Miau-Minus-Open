package miau.module.modules.combat.velocity;

import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import net.minecraft.entity.player.EntityPlayer;

public class Intave1433Velocity extends VelocityMode {
    public Intave1433Velocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (player.field_70737_aN == 10) {
                    VelocityUtil.reduceXZ(-1.0);
                } else if (player.field_70737_aN == 9 && player.field_70122_E) {
                    VelocityUtil.reduceXZ(0.9);
                }
            }
        }
    }
}
