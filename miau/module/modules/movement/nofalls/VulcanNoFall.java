package miau.module.modules.movement.nofalls;

import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorC03PacketPlayer;
import miau.module.modules.movement.NoFall;
import net.minecraft.network.play.client.C03PacketPlayer;

public class VulcanNoFall extends NoFallMode {
    public VulcanNoFall(String name, NoFall parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND
            && event.getPacket() instanceof C03PacketPlayer
            && mc.field_71439_g.field_70143_R > 7.0F) {
            ((IAccessorC03PacketPlayer)event.getPacket()).setOnGround(true);
            mc.field_71439_g.field_70143_R = 0.0F;
            mc.field_71439_g.field_70181_x = 0.0;
        }
    }
}
