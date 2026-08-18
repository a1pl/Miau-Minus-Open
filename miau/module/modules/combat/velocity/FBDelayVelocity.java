package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorEntity;
import miau.mixin.IAccessorS27PacketExplosion;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;

public class FBDelayVelocity extends VelocityMode {
    public final IntProperty delayTicks = new IntProperty("delay-ticks", 3, 1, 20);
    public final IntProperty delayChance = new IntProperty("delay-chance", 100, 0, 100);
    public final FloatProperty delayHorizontal = new FloatProperty("delay-horizontal", 0.0F, -1.0F, 1.0F);
    public final FloatProperty delayVertical = new FloatProperty("delay-vertical", 0.0F, -1.0F, 1.0F);
    public final BooleanProperty delayFakeCheck = new BooleanProperty("fake-check", true);
    private int delayChanceCounter = 0;
    private boolean delayActive = false;
    private boolean delayReverseFlag = false;
    private boolean delayPendingExplosion = false;
    private boolean delayAllowNext = true;
    private int delayTickCounter = 0;
    private long delayTimer = 0L;

    public FBDelayVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.delayChanceCounter = 0;
        this.delayActive = false;
        this.delayReverseFlag = false;
        this.delayPendingExplosion = false;
        this.delayAllowNext = true;
        this.delayTickCounter = 0;
        this.delayTimer = 0L;
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                S12PacketEntityVelocity velocityPacket = null;
                if (event.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                    if (packet.func_149412_c() == player.func_145782_y()) {
                        if (!this.delayReverseFlag
                            && !this.canDelay()
                            && !VelocityUtil.isInBadEnvironment()
                            && !this.delayPendingExplosion
                            && (!this.delayAllowNext || !this.delayFakeCheck.getValue())) {
                            this.delayChanceCounter = this.delayChanceCounter % 100 + this.delayChance.getValue();
                            if (this.delayChanceCounter >= 100) {
                                event.setCancelled(true);
                                this.delayReverseFlag = true;
                                this.delayActive = true;
                                this.delayTimer = System.currentTimeMillis();
                                return;
                            }
                        }

                        this.applyVelocityReduction(packet);
                        event.setCancelled(true);
                    }
                } else if (event.getPacket() instanceof S27PacketExplosion) {
                    this.delayPendingExplosion = true;
                    S27PacketExplosion explosion = (S27PacketExplosion)event.getPacket();
                    if (this.delayHorizontal.getValue() != 0.0F && this.delayVertical.getValue() != 0.0F) {
                        ((IAccessorS27PacketExplosion)explosion)
                            .setMotionX(
                                ((IAccessorS27PacketExplosion)explosion).getMotionX() * this.delayHorizontal.getValue()
                            );
                        ((IAccessorS27PacketExplosion)explosion)
                            .setMotionY(
                                ((IAccessorS27PacketExplosion)explosion).getMotionY() * this.delayVertical.getValue()
                            );
                        ((IAccessorS27PacketExplosion)explosion)
                            .setMotionZ(
                                ((IAccessorS27PacketExplosion)explosion).getMotionZ() * this.delayHorizontal.getValue()
                            );
                    } else {
                        event.setCancelled(true);
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
                if (this.delayReverseFlag
                    && (
                        this.canDelay()
                            || player.func_70090_H()
                            || player.func_180799_ab()
                            || ((IAccessorEntity)player).getIsInWeb()
                            || this.delayTickCounter >= this.delayTicks.getValue()
                    )) {
                    this.delayReverseFlag = false;
                    this.delayTickCounter = 0;
                    this.delayTimer = 0L;
                }

                if (this.delayReverseFlag) {
                    this.delayTickCounter++;
                }

                if (this.delayActive) {
                    double speed = Math.sqrt(
                        player.field_70159_w * player.field_70159_w + player.field_70179_y * player.field_70179_y
                    );
                    if (speed > 0.1) {
                        double yaw = Math.toDegrees(Math.atan2(player.field_70179_y, player.field_70159_w)) - 90.0;
                        player.field_70159_w = -Math.sin(Math.toRadians(yaw)) * speed;
                        player.field_70179_y = Math.cos(Math.toRadians(yaw)) * speed;
                    }

                    this.delayActive = false;
                }
            }
        }
    }

    @Override
    public void onTick(TickEvent event) {
        if (this.delayReverseFlag
            && System.currentTimeMillis() - this.delayTimer >= 50L * this.delayTicks.getValue().intValue()) {
            this.delayReverseFlag = false;
            this.delayTickCounter = 0;
            this.delayTimer = 0L;
        }

        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player != null && player.field_70737_aN == 0) {
            this.delayPendingExplosion = false;
            this.delayAllowNext = true;
        }
    }

    @Override
    public void onDisable() {
        this.delayChanceCounter = 0;
        this.delayActive = false;
        this.delayReverseFlag = false;
        this.delayPendingExplosion = false;
        this.delayAllowNext = true;
        this.delayTickCounter = 0;
        this.delayTimer = 0L;
    }

    private boolean canDelay() {
        EntityPlayer player = Velocity.mc.field_71439_g;
        return player == null ? false : player.field_70122_E;
    }

    private void applyVelocityReduction(S12PacketEntityVelocity packet) {
        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player != null) {
            double motionX = packet.func_149411_d() / 8000.0;
            double motionZ = packet.func_149409_f() / 8000.0;
            double motionY = packet.func_149410_e() / 8000.0;
            if (this.delayHorizontal.getValue() != 0.0F) {
                motionX *= this.delayHorizontal.getValue().floatValue();
                motionZ *= this.delayHorizontal.getValue().floatValue();
            }

            if (this.delayVertical.getValue() != 0.0F) {
                motionY *= this.delayVertical.getValue().floatValue();
            }

            player.field_70159_w = motionX;
            player.field_70179_y = motionZ;
            player.field_70181_x = motionY;
        }
    }
}
