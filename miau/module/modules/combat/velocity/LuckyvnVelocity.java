package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class LuckyvnVelocity extends VelocityMode {
    private boolean shouldProcess = false;
    private boolean doJumpReset = false;
    private double reducedX = 0.0;
    private double reducedZ = 0.0;

    public LuckyvnVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
            if (packet.func_149412_c() == mc.field_71439_g.func_145782_y()) {
                double velX = packet.func_149411_d() / 8000.0;
                double velZ = packet.func_149409_f() / 8000.0;
                this.reducedX = velX * 0.25;
                this.reducedZ = velZ * 0.25;
                this.doJumpReset = Math.random() < 0.75;
                this.shouldProcess = true;
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE && this.shouldProcess) {
            mc.field_71439_g.field_70159_w = this.reducedX;
            mc.field_71439_g.field_70179_y = this.reducedZ;
            if (this.doJumpReset && mc.field_71439_g.field_70122_E) {
                mc.field_71439_g.func_70664_aZ();
            }

            this.shouldProcess = false;
        }
    }
}
