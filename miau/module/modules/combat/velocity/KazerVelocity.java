package miau.module.modules.combat.velocity;

import miau.event.impl.AttackEvent;
import miau.module.modules.combat.Velocity;
import net.minecraft.entity.player.EntityPlayer;

public class KazerVelocity extends VelocityMode {
    public KazerVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onAttack(AttackEvent event) {
        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player != null) {
            VelocityUtil.reduceXZ(0.078, 9, 10);
        }
    }
}
