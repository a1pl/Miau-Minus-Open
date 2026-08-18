package miau.module.modules.combat.killaura.autoblocks;

import miau.Miau;
import miau.enums.BlinkModules;
import miau.module.modules.combat.KillAura;

public class InteractAutoBlock extends AutoBlockMode {
    public InteractAutoBlock(KillAura parent) {
        super("INTERACT", parent);
    }

    @Override
    public boolean processBlock(boolean attack, boolean block) {
        boolean swap = false;
        if (this.parent.getTarget() != null) {
            if (!this.parent.isPlayerBlocking() && !Miau.playerStateManager.digging && !Miau.playerStateManager.placing
                )
             {
                swap = true;
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
}
