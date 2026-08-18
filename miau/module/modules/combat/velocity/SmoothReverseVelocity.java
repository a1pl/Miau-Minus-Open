package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorEntityPlayer;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class SmoothReverseVelocity extends VelocityMode {
    private long velocityTimer = 0L;
    private boolean hasReceivedVelocity = false;
    private boolean reverseHurt = false;
    public final FloatProperty reverse2Strength = new FloatProperty("smooth-reverse-strength", 0.05F, 0.02F, 0.1F);
    public final BooleanProperty onLook = new BooleanProperty("on-look", false);
    public final FloatProperty range = new FloatProperty("range", 3.0F, 1.0F, 5.0F, () -> this.onLook.getValue());
    public final FloatProperty maxAngleDifference = new FloatProperty(
        "max-angle-difference", 45.0F, 5.0F, 90.0F, () -> this.onLook.getValue()
    );

    public SmoothReverseVelocity(String name, Velocity parent) {
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
                    IAccessorEntityPlayer acc = (IAccessorEntityPlayer)player;
                    if (nearby == null) {
                        acc.setSpeedInAir(0.02F);
                        this.reverseHurt = false;
                    } else if (this.onLook.getValue()
                        && !VelocityUtil.isLookingOnEntities(nearby, this.maxAngleDifference.getValue().floatValue())) {
                        this.hasReceivedVelocity = false;
                        acc.setSpeedInAir(0.02F);
                        this.reverseHurt = false;
                    } else {
                        if (player.field_70737_aN > 0) {
                            this.reverseHurt = true;
                        }

                        if (!player.field_70122_E) {
                            acc.setSpeedInAir(this.reverseHurt ? this.reverse2Strength.getValue() : 0.02F);
                        } else if (System.currentTimeMillis() - this.velocityTimer >= 80L) {
                            this.hasReceivedVelocity = false;
                            acc.setSpeedInAir(0.02F);
                            this.reverseHurt = false;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.hasReceivedVelocity = false;
        this.reverseHurt = false;
        if (Velocity.mc.field_71439_g != null) {
            ((IAccessorEntityPlayer)Velocity.mc.field_71439_g).setSpeedInAir(0.02F);
        }
    }
}
