package miau.module.modules.combat.killaura.autoblocks;

import miau.Miau;
import miau.enums.BlinkModules;
import miau.module.modules.combat.KillAura;
import miau.util.math.RandomUtil;
import miau.util.network.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;

public class GrimAC112AutoBlock extends AutoBlockMode {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public GrimAC112AutoBlock(KillAura parent) {
        super("GRIMAC-1.12", parent);
    }

    @Override
    public boolean processBlock(boolean attack, boolean block) {
        boolean swap = false;
        if (this.parent.getTarget() != null) {
            if (mc.field_71439_g.func_70694_bm() != null
                && mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemSword) {
                PacketUtil.sendPacket(
                    new C0FPacketConfirmTransaction(
                        RandomUtil.nextInt(0, Integer.MAX_VALUE), (short)RandomUtil.nextInt(-32767, 0), true
                    )
                );
                PacketUtil.sendPacket(new C0APacketAnimation());
                mc.field_71442_b.func_78769_a(mc.field_71439_g, mc.field_71441_e, mc.field_71439_g.func_70694_bm());
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
        this.parent.setRightHold(false);
        mc.field_71439_g.func_71034_by();
    }
}
