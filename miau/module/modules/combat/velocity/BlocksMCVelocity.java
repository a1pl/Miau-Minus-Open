package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.util.network.PacketUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class BlocksMCVelocity extends VelocityMode {
    private boolean hasReceivedVelocity = false;

    public BlocksMCVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (!event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (event.getPacket() instanceof C0BPacketEntityAction && this.hasReceivedVelocity) {
                    this.hasReceivedVelocity = false;
                    event.setCancelled(true);
                }

                if (event.getType() == EventType.RECEIVE) {
                    if (event.getPacket() instanceof S12PacketEntityVelocity) {
                        S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                        if (packet.func_149412_c() == player.func_145782_y()) {
                            this.hasReceivedVelocity = true;
                            event.setCancelled(true);
                            PacketUtil.sendPacket(new C0BPacketEntityAction(player, Action.START_SNEAKING));
                            PacketUtil.sendPacket(new C0BPacketEntityAction(player, Action.STOP_SNEAKING));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.hasReceivedVelocity = false;
    }
}
