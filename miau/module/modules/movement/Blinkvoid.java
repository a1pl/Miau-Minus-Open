package miau.module.modules.movement;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.LivingUpdateEvent;
import miau.module.Module;
import miau.module.modules.combat.KillAura;
import miau.module.modules.player.Scaffold;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.util.network.PacketUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

public class Blinkvoid extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty offOnScaffold = new BooleanProperty("Off on scaffold", true);
    public final FloatProperty scaffoldDelay = new FloatProperty("Scaffold Delay", 5.0F, 1.0F, 10.0F);
    public final FloatProperty fallDistance = new FloatProperty("Fall Distance", 3.0F, 1.0F, 8.0F);
    private int jumpticks = 0;
    private int scaffoldTimer = 0;
    private boolean falling = false;
    private boolean air = false;
    private boolean killaura = false;
    private boolean ljing = false;

    public Blinkvoid() {
        super("Blinkvoid", false);
    }

    @Override
    public void onEnabled() {
        this.jumpticks = 0;
        this.scaffoldTimer = 0;
        this.falling = false;
        this.air = false;
        this.killaura = false;
        this.ljing = false;
    }

    @Override
    public void onDisabled() {
        if (this.killaura) {
            this.enableKillAura();
        }

        this.falling = false;
        this.air = false;
        this.killaura = false;
        this.disableBlink();
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            double blinkDist = this.fallDistance.getValue().floatValue();
            int dist = this.fallDistance();
            if (this.scaffoldTimer > 0) {
                this.scaffoldTimer--;
            }

            if (mc.field_71439_g.field_70122_E) {
                this.ljing = true;
            }

            if (this.jumpticks-- <= 0
                && !mc.field_71439_g.field_71075_bZ.field_75100_b
                && !this.scaffoldDisable()
                && dist == -1
                && !this.falling
                && !mc.field_71439_g.field_70122_E
                && this.getKillAuraTarget() == null) {
                this.falling = true;
                this.killaura = this.isKillAuraEnabled();
                this.disableKillAura();
                this.enableBlink();
            } else if (this.falling && mc.field_71439_g.field_70143_R > blinkDist && dist == -1 && !this.air) {
                Vec3 pos = new Vec3(
                    mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v
                );
                this.air = true;
                PacketUtil.sendPacketNoEvent(
                    new C04PacketPlayerPosition(pos.field_72450_a, -420.0, pos.field_72449_c, false)
                );
                this.disableBlink();
            } else if (this.falling
                && (mc.field_71439_g.field_70122_E || dist != -1 || this.getKillAuraTarget() != null)) {
                if (this.killaura) {
                    this.enableKillAura();
                }

                this.falling = false;
                this.air = false;
                this.killaura = false;
                this.disableBlink();
            }
        }
    }

    private boolean scaffoldDisable() {
        int delay = (int)(this.scaffoldDelay.getValue() * 20.0F);
        if (this.offOnScaffold.getValue()) {
            Scaffold scaffold = (Scaffold)Miau.moduleManager.modules.get(Scaffold.class);
            if (scaffold != null && scaffold.isEnabled()) {
                this.scaffoldTimer = delay;
                return true;
            }

            if (this.scaffoldTimer > 0) {
                return true;
            }
        }

        return false;
    }

    private int fallDistance() {
        int fallDist = -1;
        double px = mc.field_71439_g.field_70165_t;
        double pz = mc.field_71439_g.field_70161_v;
        int y = (int)Math.floor(mc.field_71439_g.field_70163_u) - 1;

        for (int i = y; i > -1; i--) {
            Block block = mc.field_71441_e
                .func_180495_p(new BlockPos((int)Math.floor(px), i, (int)Math.floor(pz)))
                .func_177230_c();
            if (block != Blocks.field_150350_a
                && block != Blocks.field_150472_an
                && block != Blocks.field_150444_as
                && block != Blocks.field_150355_j
                && block != Blocks.field_150358_i
                && block != Blocks.field_150353_l
                && block != Blocks.field_150356_k) {
                fallDist = y - i;
                break;
            }
        }

        return fallDist;
    }

    private KillAura getKillAura() {
        return (KillAura)Miau.moduleManager.modules.get(KillAura.class);
    }

    private EntityLivingBase getKillAuraTarget() {
        KillAura ka = this.getKillAura();
        return ka != null ? ka.getTarget() : null;
    }

    private boolean isKillAuraEnabled() {
        KillAura ka = this.getKillAura();
        return ka != null && ka.isEnabled();
    }

    private void enableKillAura() {
        KillAura ka = this.getKillAura();
        if (ka != null && !ka.isEnabled()) {
            ka.setEnabled(true);
        }
    }

    private void disableKillAura() {
        KillAura ka = this.getKillAura();
        if (ka != null && ka.isEnabled()) {
            ka.setEnabled(false);
        }
    }

    private void enableBlink() {
        Blink blink = (Blink)Miau.moduleManager.modules.get(Blink.class);
        if (blink != null && !blink.isEnabled()) {
            blink.setEnabled(true);
        }
    }

    private void disableBlink() {
        Blink blink = (Blink)Miau.moduleManager.modules.get(Blink.class);
        if (blink != null && blink.isEnabled()) {
            blink.setEnabled(false);
        }
    }
}
