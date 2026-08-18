package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorS12PacketEntityVelocity;
import miau.mixin.IAccessorS27PacketExplosion;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.util.client.ChatUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;

public class BufferAbuseVelocity extends VelocityMode {
    public final IntProperty bufferPacket = new IntProperty("buffer-packet", 3, 1, 5);
    public final FloatProperty bufferHorizontal = new FloatProperty("buffer-horizontal", 1.0F, 0.0F, 1.0F);
    public final FloatProperty bufferVertical = new FloatProperty("buffer-vertical", 1.0F, 0.0F, 1.0F);
    public final BooleanProperty bufferDebugger = new BooleanProperty("buffer-debugger", false);
    private int bufferAmount = 0;

    public BufferAbuseVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.bufferAmount = 0;
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

                    if (this.bufferAmount < this.bufferPacket.getValue()) {
                        event.setCancelled(true);
                        this.bufferAmount++;
                        if (this.bufferDebugger.getValue()) {
                            ChatUtil.display(
                                "&7[BufferAbuse] Cancelled packet "
                                    + this.bufferAmount
                                    + "/"
                                    + this.bufferPacket.getValue()
                            );
                        }

                        return;
                    }

                    IAccessorS12PacketEntityVelocity accessor = (IAccessorS12PacketEntityVelocity)packet;
                    accessor.setMotionX((int)(packet.func_149411_d() * this.bufferHorizontal.getValue()));
                    accessor.setMotionY((int)(packet.func_149410_e() * this.bufferVertical.getValue()));
                    accessor.setMotionZ((int)(packet.func_149409_f() * this.bufferHorizontal.getValue()));
                    this.bufferAmount = 0;
                    if (this.bufferDebugger.getValue()) {
                        ChatUtil.display(
                            "&7[BufferAbuse] Applied reduction: H="
                                + this.bufferHorizontal.getValue()
                                + ", V="
                                + this.bufferVertical.getValue()
                        );
                    }
                } else if (event.getPacket() instanceof S27PacketExplosion) {
                    if (this.bufferAmount < this.bufferPacket.getValue()) {
                        event.setCancelled(true);
                        this.bufferAmount++;
                        if (this.bufferDebugger.getValue()) {
                            ChatUtil.display(
                                "&7[BufferAbuse] Cancelled explosion "
                                    + this.bufferAmount
                                    + "/"
                                    + this.bufferPacket.getValue()
                            );
                        }

                        return;
                    }

                    IAccessorS27PacketExplosion accessor = (IAccessorS27PacketExplosion)event.getPacket();
                    accessor.setMotionX(accessor.getMotionX() * this.bufferHorizontal.getValue());
                    accessor.setMotionY(accessor.getMotionY() * this.bufferVertical.getValue());
                    accessor.setMotionZ(accessor.getMotionZ() * this.bufferHorizontal.getValue());
                    this.bufferAmount = 0;
                    if (this.bufferDebugger.getValue()) {
                        ChatUtil.display("&7[BufferAbuse] Applied explosion reduction");
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.bufferAmount = 0;
    }
}
