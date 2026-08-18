package miau.module.modules.misc.cheatdetector.impl;

import miau.module.modules.misc.cheatdetector.Check;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class NoSlowCheck extends Check {
    private int noSlowTicks = 0;
    private double lastPosX;
    private double lastPosZ;

    @Override
    public String getName() {
        return "No slow";
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        double deltaX = player.field_70165_t - this.lastPosX;
        double deltaZ = player.field_70161_v - this.lastPosZ;
        double speed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (player.func_70051_ag() && player.func_71039_bw() && !player.func_70115_ae()) {
            double baseThreshold = 0.05;
            PotionEffect speedEffect = player.func_70660_b(Potion.field_76424_c);
            if (speedEffect != null) {
                int amplifier = speedEffect.func_76458_c();
                baseThreshold *= 1.0 + 0.2 * (amplifier + 1);
            }

            if (speed > baseThreshold) {
                this.noSlowTicks++;
            } else {
                this.noSlowTicks = 0;
            }
        } else {
            this.noSlowTicks = 0;
        }

        if (this.noSlowTicks > 20) {
            this.flag(player, "speed: " + String.format("%.2f", speed) + ", ticks: " + this.noSlowTicks);
            this.noSlowTicks = 0;
        }

        this.lastPosX = player.field_70165_t;
        this.lastPosZ = player.field_70161_v;
    }
}
