package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.network.PacketUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class GrimVerticalVelocity extends VelocityMode {
    public final ModeProperty mode = new ModeProperty("reveal-mode", 0, new String[]{"Vertical", "1.17", "Reduce"});
    public final BooleanProperty callEvent = new BooleanProperty("call-event", false);
    public final BooleanProperty via = new BooleanProperty("via", false);
    public final BooleanProperty smartVelo = new BooleanProperty("smart-velo", false);
    public final FloatProperty motionXZ = new FloatProperty("motion-xz", 0.05F, 0.01F, 0.2F);
    public final IntProperty c0fPacketAmount = new IntProperty("c0f-packet-amount", 2, 1, 10);
    public final BooleanProperty sendC0FValue = new BooleanProperty("send-c0f", true);
    private boolean canCancel = false;
    private boolean canSpoof = false;
    private boolean attack = false;
    private boolean velocityInput = false;
    private float savedMotionXZ = 0.05F;

    public GrimVerticalVelocity(String name, Velocity parent) {
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

                    String m = this.mode.getModeString().toLowerCase();
                    if (m.equals("reduce")) {
                        double velocityX = packet.func_149411_d() / 8000.0;
                        double velocityZ = packet.func_149409_f() / 8000.0;
                        player.field_70159_w = velocityX * 0.078;
                        player.field_70179_y = velocityZ * 0.078;
                    } else if (m.equals("1.17")) {
                        this.canCancel = true;
                        this.canSpoof = true;
                        event.setCancelled(true);
                    } else if (m.equals("vertical")) {
                        if (packet.func_149411_d() == 0 && packet.func_149409_f() == 0) {
                            return;
                        }

                        this.velocityInput = true;
                        this.savedMotionXZ = this.getMotionNoXZ(packet);
                        if (player.func_70051_ag() && VelocityUtil.isMoving()) {
                            if (this.sendC0FValue.getValue()) {
                                for (int i = 0; i < this.c0fPacketAmount.getValue(); i++) {
                                    PacketUtil.sendPacket(
                                        new C0FPacketConfirmTransaction(
                                            VelocityUtil.randomInt(102, 1000024123),
                                            (short)VelocityUtil.randomInt(102, 1000024123),
                                            true
                                        )
                                    );
                                }
                            }

                            this.attack = true;
                        }

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
                String m = this.mode.getModeString().toLowerCase();
                if (m.equals("1.17")) {
                    if (this.canSpoof) {
                        PacketUtil.sendPacket(
                            new C06PacketPlayerPosLook(
                                player.field_70165_t,
                                player.field_70163_u,
                                player.field_70161_v,
                                player.field_70177_z,
                                player.field_70125_A,
                                player.field_70122_E
                            )
                        );
                        PacketUtil.sendPacket(
                            new C07PacketPlayerDigging(
                                Action.STOP_DESTROY_BLOCK, new BlockPos(player).func_177977_b(), EnumFacing.DOWN
                            )
                        );
                        this.canSpoof = false;
                    }
                } else if (m.equals("vertical") && this.attack) {
                    float reduce = this.smartVelo.getValue() && player.field_70122_E ? this.savedMotionXZ : 0.07776F;
                    VelocityUtil.reduceXZ(reduce);
                    this.velocityInput = false;
                    this.attack = false;
                }
            }
        }
    }

    private float getMotionNoXZ(S12PacketEntityVelocity packet) {
        double x = packet.func_149411_d();
        double y = packet.func_149410_e();
        double z = packet.func_149409_f();
        double strength = Math.sqrt(x * x + y * y + z * z);
        double motionNoXZ;
        if (strength >= 20000.0) {
            motionNoXZ = Velocity.mc.field_71439_g.field_70122_E ? 0.06425 : 0.075;
        } else if (strength >= 5000.0) {
            motionNoXZ = Velocity.mc.field_71439_g.field_70122_E ? 0.02625 : 0.0552;
        } else {
            motionNoXZ = 0.0175;
        }

        return (float)motionNoXZ;
    }

    @Override
    public void onDisable() {
        this.canCancel = false;
        this.canSpoof = false;
        this.attack = false;
        this.velocityInput = false;
    }
}
