package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.HitSlowDownEvent;
import miau.event.impl.StrafeEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.PercentProperty;
import miau.util.player.MoveUtil;
import net.minecraft.client.Minecraft;

public class KeepSprint extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"VANILLA", "CANCEL", "GRIM", "VULCAN"});
    public final PercentProperty slowdown = new PercentProperty("slowdown", 0);
    public final BooleanProperty groundOnly = new BooleanProperty("ground-only", false);
    public final BooleanProperty reachOnly = new BooleanProperty("reach-only", false);
    public final IntProperty delayTicks = new IntProperty(
        "delay-ticks", 2, 0, 10, () -> this.mode.getValue() == 2 || this.mode.getValue() == 3
    );
    public final IntProperty resendInterval = new IntProperty(
        "resend-interval", 10, 1, 40, () -> this.mode.getValue() == 3
    );
    private int hitTicks = 0;
    private int packetTicks = 0;
    private boolean wasHit = false;

    public KeepSprint() {
        super("KeepSprint", false);
    }

    @Override
    public void onDisabled() {
        this.hitTicks = 0;
        this.packetTicks = 0;
        this.wasHit = false;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled() && this.shouldKeepSprint()) {
            this.hitTicks = 0;
            this.wasHit = true;
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled() && this.shouldKeepSprint()) {
            if (this.mode.getValue() != 0 && (this.mode.getValue() != 1 || this.wasHit)) {
                if (this.wasHit) {
                    this.hitTicks++;
                    if (this.hitTicks > this.delayTicks.getValue() + 2) {
                        this.wasHit = false;
                    }
                }

                if (this.wasHit && this.hitTicks <= this.delayTicks.getValue() + 1) {
                    float speed = MoveUtil.getSpeed() > 0.0 ? 1.0F : 0.0F;
                    event.setForward(speed);
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.isEnabled() && this.shouldKeepSprint()) {
                switch (this.mode.getValue()) {
                    case 0:
                        this.vanillaTick();
                    case 1:
                    default:
                        break;
                    case 2:
                        this.grimTick();
                        break;
                    case 3:
                        this.vulcanTick();
                }
            }
        }
    }

    private void vanillaTick() {
        if (!mc.field_71439_g.func_70051_ag()) {
            mc.field_71439_g.func_70031_b(true);
        }
    }

    private void grimTick() {
        if (!this.wasHit && !mc.field_71439_g.func_70051_ag()) {
            mc.field_71439_g.func_70031_b(true);
        }
    }

    private void vulcanTick() {
        this.packetTicks++;
        if (this.packetTicks >= this.resendInterval.getValue()) {
            if (!mc.field_71439_g.func_70051_ag()) {
                mc.field_71439_g.func_70031_b(true);
            }

            this.packetTicks = 0;
        }
    }

    @EventTarget
    public void onHitSlowDown(HitSlowDownEvent event) {
        if (this.isEnabled() && this.shouldKeepSprint()) {
            switch (this.mode.getValue()) {
                case 0:
                    event.setSprint(true);
                    double mult = 1.0 - this.slowdown.getValue().doubleValue() / 100.0;
                    event.setSlowDown(0.6 + 0.4 * mult);
                    break;
                case 1:
                case 2:
                case 3:
                    event.setCancelled(true);
            }
        }
    }

    public boolean shouldKeepSprint() {
        return this.groundOnly.getValue() && !mc.field_71439_g.field_70122_E
            ? false
            : !this.reachOnly.getValue()
                || mc.field_71476_x.field_72307_f.func_72438_d(mc.func_175606_aa().func_174824_e(1.0F)) > 3.0;
    }
}
