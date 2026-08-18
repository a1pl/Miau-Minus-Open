package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class MatrixReduce3Velocity extends VelocityMode {
    private long boostTimer = 0L;
    public final BooleanProperty boostAfterReduce = new BooleanProperty("boost-after-reduce", false);
    public final FloatProperty boostFactor = new FloatProperty(
        "boost-factor", 0.33F, 0.0F, 5.0F, () -> this.boostAfterReduce.getValue()
    );
    public final IntProperty boostCooldown = new IntProperty(
        "boost-cooldown", 0, 0, 2000, () -> this.boostAfterReduce.getValue()
    );

    public MatrixReduce3Velocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.boostTimer = 0L;
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
                    double realMotionY = packet.func_149410_e() / 8000.0;
                    if (Math.abs(realMotionY) >= 0.1F) {
                        player.field_70181_x = realMotionY;
                        double currentSpeed = Math.hypot(player.field_70159_w, player.field_70179_y);
                        double knockbackX = packet.func_149411_d() / 8000.0;
                        double knockbackZ = packet.func_149409_f() / 8000.0;
                        double knockbackSpeed = Math.hypot(knockbackX, knockbackZ);
                        if (!VelocityUtil.isMoving()) {
                            double reducedSpeed = Math.max(knockbackSpeed * 0.1, currentSpeed);
                            if (knockbackSpeed > 0.0) {
                                player.field_70159_w = knockbackX / knockbackSpeed * reducedSpeed;
                                player.field_70179_y = knockbackZ / knockbackSpeed * reducedSpeed;
                            }
                        } else if (this.boostAfterReduce.getValue()
                            && System.currentTimeMillis() - this.boostTimer >= this.boostCooldown.getValue().intValue()
                            )
                         {
                            VelocityUtil.reduceXZ(this.boostFactor.getValue() + 1.0F);
                            this.boostTimer = System.currentTimeMillis();
                        }
                    }
                }
            }
        }
    }
}
