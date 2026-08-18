package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.property.properties.FloatProperty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class GhostBlockVelocity extends VelocityMode {
    public final FloatProperty minHurtTime = new FloatProperty("min-hurt-time", 1.0F, 1.0F, 10.0F);
    public final FloatProperty maxHurtTime = new FloatProperty("max-hurt-time", 9.0F, 1.0F, 10.0F);
    private boolean hasReceivedVelocity = false;

    public GhostBlockVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player == null) {
                return;
            }

            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                if (packet.func_149412_c() == player.func_145782_y()) {
                    this.hasReceivedVelocity = true;
                }
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (this.hasReceivedVelocity
                    && player.field_70737_aN >= this.minHurtTime.getValue().intValue()
                    && player.field_70737_aN <= this.maxHurtTime.getValue().intValue()) {
                    player.field_70145_X = true;
                } else if (player.field_70737_aN == 0) {
                    this.hasReceivedVelocity = false;
                    player.field_70145_X = false;
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.hasReceivedVelocity = false;
    }
}
