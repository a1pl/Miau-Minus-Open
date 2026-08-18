package miau.module.modules.misc.cheatdetector.impl;

import java.util.ArrayList;
import java.util.List;
import miau.module.modules.misc.cheatdetector.Check;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;

public class LegitScaffoldCheck extends Check {
    private long lastCrouchStart = 0L;
    private long lastCrouchEnd = 0L;
    private boolean wasSneaking = false;
    private long lastSwingTick = Long.MIN_VALUE;
    private final List<Integer> crouchDurations = new ArrayList<>();
    private long lastFlagTick = 0L;

    @Override
    public String getName() {
        return "Legit scaffold";
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        long tick = mc.field_71441_e.func_82737_E();
        this.trackCrouch(tick, player.func_70093_af());
        this.trackSwing(tick, player.field_110158_av);
        if (this.isScaffold(player)) {
            this.evaluate(player, tick);
        }
    }

    private void trackCrouch(long tick, boolean currSneak) {
        if (currSneak && !this.wasSneaking) {
            this.lastCrouchStart = tick;
        } else if (!currSneak && this.wasSneaking) {
            int duration = (int)(tick - this.lastCrouchStart);
            this.lastCrouchEnd = tick;
            this.crouchDurations.add(0, duration);
            if (this.crouchDurations.size() > 5) {
                this.crouchDurations.remove(5);
            }
        }

        this.wasSneaking = currSneak;
    }

    private void trackSwing(long tick, int swingProgressInt) {
        if (swingProgressInt == 1) {
            this.lastSwingTick = tick;
        }
    }

    private boolean isScaffold(EntityPlayer player) {
        return player.field_70125_A >= 60.0F
            && player.field_70122_E
            && player.func_70694_bm() != null
            && player.func_70694_bm().func_77973_b() instanceof ItemBlock;
    }

    private void evaluate(EntityPlayer player, long tick) {
        if (this.lastCrouchStart != 0L && this.lastCrouchEnd != 0L) {
            int crouchDuration = (int)(this.lastCrouchEnd - this.lastCrouchStart);
            boolean quickCrouch = crouchDuration >= 1 && crouchDuration <= 2;
            boolean swingTiming = this.lastSwingTick >= this.lastCrouchEnd
                && this.lastSwingTick <= this.lastCrouchEnd + 3L
                && tick - this.lastSwingTick <= 10L;
            boolean consistent = this.crouchDurations.size() >= 3
                && this.crouchDurations.get(0) <= 3
                && this.crouchDurations.get(1) <= 3
                && this.crouchDurations.get(2) <= 3;
            if (quickCrouch && swingTiming && consistent && tick - this.lastFlagTick >= 60L) {
                this.flag(player, "crouch: " + crouchDuration + "t");
                this.lastFlagTick = tick;
            }
        }
    }
}
