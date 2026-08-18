package miau.module.modules.combat.killaura.autoblocks;

import miau.module.modules.combat.KillAura;

public abstract class AutoBlockMode {
    protected final String name;
    protected final KillAura parent;

    public AutoBlockMode(String name, KillAura parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return this.name;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onPreUpdate() {
    }

    public void onPostUpdate() {
    }

    public void onAttack() {
    }

    public abstract boolean processBlock(boolean var1, boolean var2);
}
