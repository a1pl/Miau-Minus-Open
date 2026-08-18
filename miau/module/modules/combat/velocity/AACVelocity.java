package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.property.properties.FloatProperty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class AACVelocity extends VelocityMode {
    private long velocityTimer = 0L;
    private boolean hasReceivedVelocity = false;
    public final FloatProperty horizontal = new FloatProperty("horizontal", 0.0F, -1.0F, 1.0F);

    public AACVelocity(String name, Velocity parent) {
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
                        this.velocityTimer = System.currentTimeMillis();
                        this.hasReceivedVelocity = true;
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
                if (this.hasReceivedVelocity && System.currentTimeMillis() - this.velocityTimer >= 80L) {
                    VelocityUtil.reduceXZ(this.horizontal.getValue().floatValue());
                    this.hasReceivedVelocity = false;
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.hasReceivedVelocity = false;
    }
}
