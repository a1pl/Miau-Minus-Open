package miau.module.modules.combat.velocity;

import miau.event.impl.AttackEvent;
import miau.module.modules.combat.Velocity;
import net.minecraft.entity.player.EntityPlayer;

public class IntaveStrongVelocity extends VelocityMode {
    public IntaveStrongVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onAttack(AttackEvent event) {
        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player != null) {
            if (player.field_70737_aN > 0) {
                VelocityUtil.reduceXZ(0.6);
            }
        }
    }
}
