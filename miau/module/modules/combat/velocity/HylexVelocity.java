package miau.module.modules.combat.velocity;

import miau.event.impl.AttackEvent;
import miau.module.modules.combat.Velocity;
import net.minecraft.entity.player.EntityPlayer;

public class HylexVelocity extends VelocityMode {
    private long lastAttackTime = 0L;

    public HylexVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onAttack(AttackEvent event) {
        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player != null) {
            switch (player.field_70737_aN) {
                case 4:
                    VelocityUtil.reduceXZ(0.37);
                case 5:
                case 6:
                default:
                    break;
                case 7:
                    VelocityUtil.reduceXZ(0.4);
                    break;
                case 8:
                    VelocityUtil.reduceXZ(0.11);
                    break;
                case 9:
                    VelocityUtil.reduceXZ(0.8);
            }

            this.lastAttackTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onEnable() {
        this.lastAttackTime = 0L;
    }

    @Override
    public void onDisable() {
        this.lastAttackTime = 0L;
    }
}
