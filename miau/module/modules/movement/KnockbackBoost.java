package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.LivingUpdateEvent;
import miau.module.Module;
import miau.util.player.MoveUtil;
import net.minecraft.client.Minecraft;

public class KnockbackBoost extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private boolean start = false;
    private int ticks = 0;

    public KnockbackBoost() {
        super("KnockbackBoost", false);
    }

    @Override
    public void onEnabled() {
        this.start = false;
        this.ticks = 0;
    }

    @Override
    public void onDisabled() {
        this.start = false;
        this.ticks = 0;
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            if (mc.field_71439_g.field_70737_aN >= 3) {
                this.start = true;
            }

            if (this.start) {
                this.ticks++;
            }

            if (!mc.field_71439_g.field_70122_E) {
                if (this.ticks == 1) {
                    mc.field_71439_g.field_70181_x += 0.061;
                } else if (this.ticks > 1) {
                    mc.field_71439_g.field_70181_x += 0.0283;
                }
            } else if (this.ticks > 1) {
                this.setEnabled(false);
                return;
            }

            if (this.ticks > 0 && this.ticks < 30) {
                mc.field_71439_g.field_70181_x = 0.29;
            }

            if (this.start && mc.field_71439_g.field_70737_aN == 9 && MoveUtil.isForwardPressed()) {
                MoveUtil.setSpeed(1.94);
            }
        }
    }
}
