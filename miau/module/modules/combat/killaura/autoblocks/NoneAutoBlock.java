package miau.module.modules.combat.killaura.autoblocks;

import miau.Miau;
import miau.enums.BlinkModules;
import miau.module.modules.combat.KillAura;
import miau.util.player.PlayerUtil;

public class NoneAutoBlock extends AutoBlockMode {
    public NoneAutoBlock(KillAura parent) {
        super("NONE", parent);
    }

    @Override
    public boolean processBlock(boolean attack, boolean block) {
        boolean swap = false;
        if (PlayerUtil.isUsingItem()) {
            this.parent.isBlocking = true;
            if (!this.parent.isPlayerBlocking() && !Miau.playerStateManager.digging && !Miau.playerStateManager.placing
                )
             {
                swap = true;
            }
        } else {
            this.parent.isBlocking = false;
            if (this.parent.isPlayerBlocking() && !Miau.playerStateManager.digging && !Miau.playerStateManager.placing) {
                this.parent.stopBlock();
            }
        }

        Miau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
        this.parent.fakeBlockState = false;
        return swap;
    }
}
