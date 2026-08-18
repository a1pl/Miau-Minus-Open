package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorS12PacketEntityVelocity;
import miau.module.modules.combat.Velocity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class HypixelVelocity extends VelocityMode {
    private boolean hasReceivedVelocity = false;
    private boolean absorbedVelocity = false;

    public HypixelVelocity(String name, Velocity parent) {
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
                        this.hasReceivedVelocity = true;
                        if (!player.field_70122_E && !this.absorbedVelocity) {
                            event.setCancelled(true);
                            this.absorbedVelocity = true;
                        } else {
                            IAccessorS12PacketEntityVelocity accessor = (IAccessorS12PacketEntityVelocity)packet;
                            accessor.setMotionX((int)(player.field_70159_w * 8000.0));
                            accessor.setMotionZ((int)(player.field_70179_y * 8000.0));
                        }
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
                if (this.hasReceivedVelocity && player.field_70122_E) {
                    this.absorbedVelocity = false;
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.hasReceivedVelocity = false;
        this.absorbedVelocity = false;
    }
}
