package miau.module.modules.combat.velocity;

import miau.event.impl.AttackEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class BuzzReverseVelocity extends VelocityMode {
    public final BooleanProperty needAttack = new BooleanProperty("need-attack", false);
    private boolean hasReceivedVelocity = false;

    public BuzzReverseVelocity(String name, Velocity parent) {
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
                if (player.field_70737_aN == 7 && this.hasReceivedVelocity) {
                    if (this.needAttack.getValue()) {
                        return;
                    }

                    VelocityUtil.reduceXZ(-1.0);
                    this.hasReceivedVelocity = false;
                }

                if (player.field_70737_aN == 0 && this.hasReceivedVelocity) {
                    this.hasReceivedVelocity = false;
                }
            }
        }
    }

    @Override
    public void onAttack(AttackEvent event) {
        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player != null) {
            if (this.needAttack.getValue() && player.field_70737_aN == 7 && this.hasReceivedVelocity) {
                VelocityUtil.reduceXZ(-1.0);
                this.hasReceivedVelocity = false;
            }
        }
    }

    @Override
    public void onDisable() {
        this.hasReceivedVelocity = false;
    }
}
