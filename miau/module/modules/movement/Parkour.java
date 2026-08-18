package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.MoveInputEvent;
import miau.module.Module;
import miau.util.player.MoveUtil;
import miau.util.player.SimulatedPlayer;
import net.minecraft.client.Minecraft;

public class Parkour extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public Parkour() {
        super("Parkour", false);
    }

    @EventTarget
    public void onMovementInput(MoveInputEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            SimulatedPlayer simPlayer = SimulatedPlayer.fromClientPlayer(mc.field_71439_g.field_71158_b);
            simPlayer.tick();
            if (MoveUtil.isMoving()
                && mc.field_71439_g.field_70122_E
                && !mc.field_71439_g.func_70093_af()
                && !mc.field_71474_y.field_74311_E.func_151470_d()
                && !simPlayer.onGround) {
                mc.field_71439_g.field_71158_b.field_78901_c = true;
            }
        }
    }
}
