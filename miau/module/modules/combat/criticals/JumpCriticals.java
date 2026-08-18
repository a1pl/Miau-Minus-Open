package miau.module.modules.combat.criticals;

import miau.Miau;
import miau.event.impl.AttackEvent;
import miau.module.modules.combat.Criticals;
import miau.module.modules.movement.Speed;
import miau.util.player.MoveUtil;

public class JumpCriticals extends CriticalsMode {
    public JumpCriticals(String name, Criticals parent) {
        super(name, parent);
    }

    @Override
    public void onAttack(AttackEvent event) {
        if (!mc.field_71439_g.func_70090_H()) {
            if (MoveUtil.isMoving()
                && mc.field_71439_g.field_70122_E
                && !Miau.moduleManager.modules.get(Speed.class).isEnabled()
                && !mc.field_71474_y.field_74314_A.func_151470_d()) {
                mc.field_71439_g.func_70664_aZ();
            }
        }
    }
}
