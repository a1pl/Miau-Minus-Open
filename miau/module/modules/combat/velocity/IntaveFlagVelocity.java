package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.util.network.PacketUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class IntaveFlagVelocity extends VelocityMode {
    private boolean intaFlag = false;

    public IntaveFlagVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (event.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                    if (packet.func_149412_c() == player.func_145782_y()) {
                        this.intaFlag = false;
                    }
                }
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (player.field_70737_aN >= 9 && !this.intaFlag) {
                    PacketUtil.sendPacket(
                        new C04PacketPlayerPosition(
                            player.field_70165_t + 6.0, player.field_70163_u + 1.0, player.field_70161_v + 6.0, false
                        )
                    );
                    this.intaFlag = true;
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.intaFlag = false;
    }
}
