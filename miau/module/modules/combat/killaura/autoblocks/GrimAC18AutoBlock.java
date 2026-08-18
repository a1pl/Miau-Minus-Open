package miau.module.modules.combat.killaura.autoblocks;

import miau.Miau;
import miau.enums.BlinkModules;
import miau.module.modules.combat.KillAura;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemSword;

public class GrimAC18AutoBlock extends AutoBlockMode {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public GrimAC18AutoBlock(KillAura parent) {
        super("GRIMAC-1.8", parent);
    }

    @Override
    public boolean processBlock(boolean attack, boolean block) {
        boolean swap = false;
        if (this.parent.getTarget() != null) {
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
    public void onPostUpdate() {
        if (this.parent.isBlocking
            && this.parent.getTarget() != null
            && mc.field_71439_g.func_70694_bm() != null
            && mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemSword) {
            mc.field_71442_b.func_78769_a(mc.field_71439_g, mc.field_71441_e, mc.field_71439_g.func_70694_bm());
        }
    }

    @Override
    public void onDisable() {
        this.parent.setRightHold(false);
        mc.field_71439_g.func_71034_by();
    }
}
