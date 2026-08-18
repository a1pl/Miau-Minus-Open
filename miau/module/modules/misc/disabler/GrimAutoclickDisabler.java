package miau.module.modules.misc.disabler;

import miau.Miau;
import miau.event.impl.LivingUpdateEvent;
import miau.module.modules.combat.KillAura;
import miau.module.modules.ghost.AutoClicker;
import miau.module.modules.misc.Disabler;
import miau.module.modules.player.Scaffold;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class GrimAutoclickDisabler extends DisablerMode {
    public GrimAutoclickDisabler(String name, Disabler parent) {
        super(name, parent);
    }

    @Override
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (mc.field_71439_g != null && mc.func_147114_u() != null) {
            boolean isScaffActive = false;
            Scaffold scaffold = (Scaffold)Miau.moduleManager.modules.get(Scaffold.class);
            if (scaffold != null && scaffold.isEnabled()) {
                isScaffActive = true;
            }

            boolean isKuraActive = false;
            KillAura aura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
            if (aura != null && aura.isEnabled()) {
                isKuraActive = true;
            }

            boolean isAClickActive = false;
            AutoClicker autoclicker = (AutoClicker)Miau.moduleManager.modules.get(AutoClicker.class);
            if (autoclicker != null && autoclicker.isEnabled()) {
                isAClickActive = true;
            }

            if ((isScaffActive || isKuraActive || isAClickActive) && mc.field_71439_g.field_70173_aa % 20 == 0) {
                mc.func_147114_u()
                    .func_147297_a(
                        new C07PacketPlayerDigging(
                            Action.START_DESTROY_BLOCK,
                            new BlockPos(
                                mc.field_71439_g.field_70165_t,
                                mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(),
                                mc.field_71439_g.field_70161_v
                            ),
                            EnumFacing.DOWN
                        )
                    );
            }
        }
    }
}
