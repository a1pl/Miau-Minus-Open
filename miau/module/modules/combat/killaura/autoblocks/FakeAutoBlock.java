package miau.module.modules.combat.killaura.autoblocks;

import miau.Miau;
import miau.enums.BlinkModules;
import miau.module.modules.combat.KillAura;
import miau.util.player.PlayerUtil;

public class FakeAutoBlock extends AutoBlockMode {
    public FakeAutoBlock(KillAura parent) {
        super("FAKE", parent);
    }

    @Override
    public boolean processBlock(boolean attack, boolean block) {
        boolean swap = false;
        Miau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
        this.parent.isBlocking = false;
        this.parent.fakeBlockState = this.parent.getTarget() != null;
        if (PlayerUtil.isUsingItem()
            && !this.parent.isPlayerBlocking()
            && !Miau.playerStateManager.digging
            && !Miau.playerStateManager.placing) {
            swap = true;
        }

        return swap;
    }
}
