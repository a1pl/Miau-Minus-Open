package miau.module.modules.player;

import miau.event.EventTarget;
import miau.event.impl.LivingUpdateEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.StrafeEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;

public class Freeze extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private double savedMotionX;
    private double savedMotionY;
    private double savedMotionZ;
    private int tickCounter;
    private int phase;
    private static final int STASIS_TICKS = 45;
    private static final int RELEASE_TICKS = 1;

    public Freeze() {
        super("Freeze", false);
    }

    @Override
    public void onEnabled() {
        if (mc.field_71439_g != null) {
            this.savedMotionX = mc.field_71439_g.field_70159_w;
            this.savedMotionY = mc.field_71439_g.field_70181_x;
            this.savedMotionZ = mc.field_71439_g.field_70179_y;
        }

        this.tickCounter = 0;
        this.phase = 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()) {
            this.tickCounter++;
            if (this.phase == 0 && this.tickCounter >= 45) {
                this.phase = 1;
                this.tickCounter = 0;
                mc.field_71439_g.field_70159_w = this.savedMotionX;
                mc.field_71439_g.field_70181_x = this.savedMotionY;
                mc.field_71439_g.field_70179_y = this.savedMotionZ;
            } else if (this.phase == 1 && this.tickCounter >= 1) {
                this.phase = 0;
                this.tickCounter = 0;
                this.savedMotionX = mc.field_71439_g.field_70159_w;
                this.savedMotionY = mc.field_71439_g.field_70181_x;
                this.savedMotionZ = mc.field_71439_g.field_70179_y;
            }

            if (this.phase == 0) {
                mc.field_71439_g.field_70159_w = 0.0;
                mc.field_71439_g.field_70179_y = 0.0;
                mc.field_71439_g.field_70181_x = 0.0;
            }

            if (mc.field_71439_g != null && mc.field_71439_g.field_70122_E) {
                this.setEnabled(false);
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled() && this.phase == 0) {
            mc.field_71439_g.field_71158_b.field_78900_b = 0.0F;
            mc.field_71439_g.field_71158_b.field_78902_a = 0.0F;
            mc.field_71439_g.field_71158_b.field_78901_c = false;
            mc.field_71439_g.field_71158_b.field_78899_d = false;
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && this.phase == 0) {
            mc.field_71439_g.field_70159_w = 0.0;
            mc.field_71439_g.field_70181_x = 0.0;
            mc.field_71439_g.field_70179_y = 0.0;
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled() && this.phase == 0) {
            event.setForward(0.0F);
            event.setStrafe(0.0F);
            event.setFriction(0.0F);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.SEND && !event.isCancelled()) {
            if (event.getPacket() instanceof C03PacketPlayer) {
                if (this.phase != 1) {
                    if (mc.field_71439_g != null && mc.field_71439_g.field_70737_aN == 0) {
                        if (!(event.getPacket() instanceof C05PacketPlayerLook)) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisabled() {
        if (mc.field_71439_g != null) {
            mc.field_71439_g.field_70159_w = this.savedMotionX;
            mc.field_71439_g.field_70181_x = this.savedMotionY;
            mc.field_71439_g.field_70179_y = this.savedMotionZ;
        }

        this.tickCounter = 0;
        this.phase = 0;
    }
}
