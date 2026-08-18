package miau.module.modules.combat.killaura.autoblocks;

import miau.Miau;
import miau.enums.BlinkModules;
import miau.module.modules.combat.KillAura;
import net.minecraft.client.Minecraft;

public class LegitAutoBlock extends AutoBlockMode {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public LegitAutoBlock(KillAura parent) {
        super("LEGIT", parent);
    }

    @Override
    public boolean processBlock(boolean attack, boolean block) {
        boolean swap = false;
        if (this.parent.getTarget() != null) {
            if (!Miau.playerStateManager.digging && !Miau.playerStateManager.placing) {
                switch (this.parent.blockTick) {
                    case 0:
                        if (!this.parent.isPlayerBlocking()) {
                            swap = true;
                        }

                        this.parent.blockTick = 1;
                        break;
                    case 1:
                        if (this.parent.isPlayerBlocking()) {
                            this.parent.stopBlock();
                            this.parent.cancelAttack = true;
                        }

                        if (this.parent.attackDelayMS <= 50L) {
                            this.parent.blockTick = 0;
                        }
                        break;
                    default:
                        this.parent.blockTick = 0;
                }
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
