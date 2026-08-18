package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.util.network.PacketUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

public class VulcanVelocity extends VelocityMode {
    private boolean transaction = false;

    public VulcanVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (event.getPacket() instanceof S32PacketConfirmTransaction) {
                    event.setCancelled(true);
                    PacketUtil.sendPacketNoEvent(
                        new C0FPacketConfirmTransaction(
                            this.transaction ? 1 : -1, (short)(this.transaction ? -1 : 1), this.transaction
                        )
                    );
                    this.transaction = !this.transaction;
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.transaction = false;
    }
}
