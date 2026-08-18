package miau.module.modules.misc.disabler;

import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.module.modules.misc.Disabler;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;

public class TransactionDisabler extends DisablerMode {
    public TransactionDisabler(String name, Disabler parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C0FPacketConfirmTransaction) {
            event.setCancelled(true);
        }
    }
}
