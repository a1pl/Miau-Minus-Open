package miau.module.modules.misc;

import miau.event.EventTarget;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.util.math.RandomUtil;
import miau.util.network.PacketUtil;
import miau.util.player.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S08PacketPlayerPosLook.EnumFlags;

public class NoRotate extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private boolean reset = false;

    public NoRotate() {
        super("NoRotate", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()
            && event.getType() == EventType.RECEIVE
            && !event.isCancelled()
            && mc.field_71439_g != null
            && mc.field_71441_e != null
            && (mc.field_71439_g.field_70177_z != -180.0F || mc.field_71439_g.field_70125_A != 0.0F)) {
            if (event.getPacket() instanceof S02PacketChat) {
                String msg = ((S02PacketChat)event.getPacket()).func_148915_c().func_150254_d();
                if (msg.contains("§e§lProtect your bed and destroy the enemy beds.")
                    || msg.contains("§eYou will respawn in §r§c1 §r§esecond!")) {
                    this.reset = true;
                }
            }

            if (event.getPacket() instanceof S08PacketPlayerPosLook) {
                if (this.reset) {
                    this.reset = false;
                    return;
                }

                S08PacketPlayerPosLook packet = (S08PacketPlayerPosLook)event.getPacket();
                event.setCancelled(true);
                double x = packet.func_148932_c();
                double y = packet.func_148928_d();
                double z = packet.func_148933_e();
                float yaw = packet.func_148931_f();
                float pitch = packet.func_148930_g();
                if (packet.func_179834_f().contains(EnumFlags.X)) {
                    x += mc.field_71439_g.field_70165_t;
                } else {
                    mc.field_71439_g.field_70159_w = 0.0;
                }

                if (packet.func_179834_f().contains(EnumFlags.Y)) {
                    y += mc.field_71439_g.field_70163_u;
                } else {
                    mc.field_71439_g.field_70181_x = 0.0;
                }

                if (packet.func_179834_f().contains(EnumFlags.Z)) {
                    z += mc.field_71439_g.field_70161_v;
                } else {
                    mc.field_71439_g.field_70179_y = 0.0;
                }

                if (packet.func_179834_f().contains(EnumFlags.X_ROT)) {
                    pitch += mc.field_71439_g.field_70125_A;
                }

                if (packet.func_179834_f().contains(EnumFlags.Y_ROT)) {
                    yaw += mc.field_71439_g.field_70177_z;
                }

                mc.field_71439_g
                    .func_70080_a(
                        x,
                        y,
                        z,
                        RotationUtil.quantizeAngle(mc.field_71439_g.field_70177_z + RandomUtil.nextFloat(-0.01F, 0.01F)),
                        RotationUtil.quantizeAngle(mc.field_71439_g.field_70125_A + RandomUtil.nextFloat(-0.01F, 0.01F))
                    );
                PacketUtil.sendPacketNoEvent(
                    new C06PacketPlayerPosLook(
                        mc.field_71439_g.field_70165_t,
                        mc.field_71439_g.func_174813_aQ().field_72338_b,
                        mc.field_71439_g.field_70161_v,
                        yaw % 360.0F,
                        pitch % 360.0F,
                        false
                    )
                );
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.reset = false;
    }

    @Override
    public void onDisabled() {
        this.reset = false;
    }
}
