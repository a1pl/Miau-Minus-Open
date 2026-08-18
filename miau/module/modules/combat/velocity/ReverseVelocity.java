package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class ReverseVelocity extends VelocityMode {
    private long velocityTimer = 0L;
    private boolean hasReceivedVelocity = false;
    public final FloatProperty reverseStrength = new FloatProperty("reverse-strength", 1.0F, 0.1F, 1.0F);
    public final BooleanProperty onLook = new BooleanProperty("on-look", false);
    public final FloatProperty range = new FloatProperty("range", 3.0F, 1.0F, 5.0F, () -> this.onLook.getValue());
    public final FloatProperty maxAngleDifference = new FloatProperty(
        "max-angle-difference", 45.0F, 5.0F, 90.0F, () -> this.onLook.getValue()
    );

    public ReverseVelocity(String name, Velocity parent) {
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
                    this.velocityTimer = System.currentTimeMillis();
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
                if (this.hasReceivedVelocity) {
                    EntityLivingBase nearby = VelocityUtil.getNearestEntityInRange(this.range.getValue());
                    if (nearby != null) {
                        if (!player.field_70122_E) {
                            if (this.onLook.getValue()
                                && !VelocityUtil.isLookingOnEntities(
                                    nearby, this.maxAngleDifference.getValue().floatValue()
                                )) {
                                return;
                            }

                            double speed = VelocityUtil.getSpeed();
                            double yaw = Math.atan2(player.field_70179_y, player.field_70159_w) * 180.0 / Math.PI
                                - 90.0;
                            if (speed > 0.0) {
                                player.field_70159_w = -Math.sin(Math.toRadians(yaw))
                                    * (speed * this.reverseStrength.getValue().floatValue());
                                player.field_70179_y = Math.cos(Math.toRadians(yaw))
                                    * (speed * this.reverseStrength.getValue().floatValue());
                            } else {
                                VelocityUtil.reduceXZ(this.reverseStrength.getValue().floatValue());
                            }
                        } else if (System.currentTimeMillis() - this.velocityTimer >= 80L) {
                            this.hasReceivedVelocity = false;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.hasReceivedVelocity = false;
    }
}
