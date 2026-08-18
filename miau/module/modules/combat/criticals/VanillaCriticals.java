package miau.module.modules.combat.criticals;

import miau.event.impl.AttackEvent;
import miau.module.modules.combat.Criticals;

public class VanillaCriticals extends CriticalsMode {
    public VanillaCriticals(String name, Criticals parent) {
        super(name, parent);
    }

    @Override
    public void onAttack(AttackEvent event) {
        if (!mc.field_71439_g.func_70090_H()) {
            mc.field_71439_g.func_71009_b(event.getTarget());
        }
    }
}
