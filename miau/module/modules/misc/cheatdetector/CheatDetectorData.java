package miau.module.modules.misc.cheatdetector;

import miau.Miau;
import miau.event.impl.PacketEvent;
import miau.module.modules.misc.CheatDetector;
import miau.module.modules.misc.cheatdetector.impl.AutoBlockCheck;
import miau.module.modules.misc.cheatdetector.impl.KillauraCheck;
import miau.module.modules.misc.cheatdetector.impl.LegitScaffoldCheck;
import miau.module.modules.misc.cheatdetector.impl.NoSlowCheck;
import net.minecraft.entity.player.EntityPlayer;

public class CheatDetectorData {
    public AutoBlockCheck autoBlockCheck = new AutoBlockCheck();
    public KillauraCheck killauraCheck = new KillauraCheck();
    public NoSlowCheck noSlowCheck = new NoSlowCheck();
    public LegitScaffoldCheck legitScaffoldCheck = new LegitScaffoldCheck();

    public void onUpdate(EntityPlayer player) {
        CheatDetector cd = (CheatDetector)Miau.moduleManager.getModule(CheatDetector.class);
        if (cd != null) {
            if (cd.isCheckEnabled(this.autoBlockCheck.getName())) {
                this.autoBlockCheck.onUpdate(player);
            }

            if (cd.isCheckEnabled(this.killauraCheck.getName())) {
                this.killauraCheck.onUpdate(player);
            }

            if (cd.isCheckEnabled(this.noSlowCheck.getName())) {
                this.noSlowCheck.onUpdate(player);
            }

            if (cd.isCheckEnabled(this.legitScaffoldCheck.getName())) {
                this.legitScaffoldCheck.onUpdate(player);
            }
        }
    }

    public void onPacket(PacketEvent e, EntityPlayer player) {
        CheatDetector cd = (CheatDetector)Miau.moduleManager.getModule(CheatDetector.class);
        if (cd != null) {
            if (cd.isCheckEnabled(this.autoBlockCheck.getName())) {
                this.autoBlockCheck.onPacket(e, player);
            }

            if (cd.isCheckEnabled(this.killauraCheck.getName())) {
                this.killauraCheck.onPacket(e, player);
            }

            if (cd.isCheckEnabled(this.noSlowCheck.getName())) {
                this.noSlowCheck.onPacket(e, player);
            }

            if (cd.isCheckEnabled(this.legitScaffoldCheck.getName())) {
                this.legitScaffoldCheck.onPacket(e, player);
            }
        }
    }
}
