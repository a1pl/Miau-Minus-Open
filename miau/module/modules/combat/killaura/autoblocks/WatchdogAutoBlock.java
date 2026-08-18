package miau.module.modules.combat.killaura.autoblocks;

import miau.Miau;
import miau.enums.BlinkModules;
import miau.module.modules.combat.KillAura;

public class WatchdogAutoBlock extends AutoBlockMode {
    public WatchdogAutoBlock(KillAura parent) {
        super("WATCHDOG", parent);
    }

    @Override
    public boolean processBlock(boolean attack, boolean block) {
        boolean swap = false;
        if (this.parent.getTarget() != null) {
            if (this.parent.fixNoSlowFlag.getValue() && this.parent.watchdogTick >= 10) {
                this.parent.stopBlock();
                this.parent.watchdogTick = 0;
            } else {
                if (!this.parent.isPlayerBlocking()
                    && !Miau.playerStateManager.digging
                    && !Miau.playerStateManager.placing) {
                    swap = true;
                }

                this.parent.watchdogTick++;
            }

            Miau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
            this.parent.isBlocking = true;
            this.parent.fakeBlockState = false;
        } else {
            Miau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
            this.parent.isBlocking = false;
            this.parent.fakeBlockState = false;
        }

        return swap;
    }

    @Override
    public void onDisable() {
        if (this.parent.isPlayerBlocking()) {
            this.parent.stopBlock();
        }
    }
}
