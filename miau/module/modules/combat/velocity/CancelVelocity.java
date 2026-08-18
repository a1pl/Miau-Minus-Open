package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class CancelVelocity extends VelocityMode {
    public final BooleanProperty cancelHorizontal = new BooleanProperty("cancel-horizontal", true);
    public final BooleanProperty cancelVertical = new BooleanProperty("cancel-vertical", true);
    public final BooleanProperty cancelVerticalOnlyInAir = new BooleanProperty("cancel-vertical-only-in-air", false);

    public CancelVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (event.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                    if (packet.func_149412_c() != player.func_145782_y()) {
                        return;
                    }

                    event.setCancelled(true);
                    boolean hCancel = this.cancelHorizontal.getValue();
                    boolean vCancel = this.cancelVertical.getValue();
                    boolean vOnlyInAir = this.cancelVerticalOnlyInAir.getValue();
                    if (hCancel && vCancel && !vOnlyInAir) {
                        return;
                    }

                    if (!hCancel) {
                        player.field_70159_w = packet.func_149411_d() / 8000.0;
                        player.field_70179_y = packet.func_149409_f() / 8000.0;
                    }

                    boolean shouldCancelVertical = vCancel || vOnlyInAir && !player.field_70122_E;
                    if (!shouldCancelVertical) {
                        player.field_70181_x = packet.func_149410_e() / 8000.0;
                    }
                }
            }
        }
    }
}
