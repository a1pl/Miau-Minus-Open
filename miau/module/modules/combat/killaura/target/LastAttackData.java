package miau.module.modules.combat.killaura.target;

public class LastAttackData {
    private long time = System.currentTimeMillis();
    private double damage;

    public LastAttackData(double damage) {
        this.damage = damage;
    }

    public void reset(boolean reset, double damage) {
        if (reset) {
            this.time = System.currentTimeMillis();
        }

        this.damage = damage;
    }

    public long getTime() {
        return System.currentTimeMillis() - this.time;
    }

    public double getDamage() {
        return this.damage;
    }
}
