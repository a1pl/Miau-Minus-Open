package miau.util.player;

import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;

public class PlayerTracker {
    public static float fallDistance = 0.0F;
    public static int onGroundTicks = 0;
    public static int ticksSinceVelocity = 0;
    private static final Minecraft mc = Minecraft.func_71410_x();

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            ticksSinceVelocity++;
            if (mc.field_71439_g != null) {
                double fallDist = mc.field_71439_g.field_70137_T - mc.field_71439_g.field_70163_u;
                if (fallDist > 0.0) {
                    fallDistance = (float)(fallDistance + fallDist);
                }

                if (mc.field_71439_g.field_70122_E) {
                    fallDistance = 0.0F;
                    onGroundTicks++;
                } else {
                    onGroundTicks = 0;
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                if (mc.field_71439_g != null && packet.func_149412_c() == mc.field_71439_g.func_145782_y()) {
                    ticksSinceVelocity = 0;
                }
            } else if (event.getPacket() instanceof S27PacketExplosion) {
                ticksSinceVelocity = 0;
            }
        }
    }
}
