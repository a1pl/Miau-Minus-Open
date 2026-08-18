package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.util.client.ChatUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class PolarJumpVelocity extends VelocityMode {
    public final IntProperty forceChangeHurtTimeCount = new IntProperty("force-change-hurt-time-count", 3, 1, 20);
    public final BooleanProperty polarJumpDebugger = new BooleanProperty("debug", false);
    private int polarHurtTime = 0;
    private int polarHurtCount = 0;

    public PolarJumpVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.polarHurtTime = VelocityUtil.randomInt(7, 10);
        this.polarHurtCount = 0;
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (event.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                    if (packet.func_149412_c() == player.func_145782_y()) {
                        this.polarHurtCount++;
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
                if (this.polarHurtTime == player.field_70737_aN && player.field_70122_E) {
                    VelocityUtil.tryJump();
                    if (this.polarJumpDebugger.getValue()) {
                        ChatUtil.display("[PolarJump] Jumped");
                    }

                    this.polarHurtTime = VelocityUtil.randomInt(7, 10);
                    if (this.polarJumpDebugger.getValue()) {
                        ChatUtil.display("[PolarJump] NextJumpHurtTime: " + this.polarHurtTime);
                    }
                }

                if (this.polarHurtCount >= this.forceChangeHurtTimeCount.getValue()) {
                    this.polarHurtCount = 0;
                    this.polarHurtTime = VelocityUtil.randomInt(7, 10);
                    if (this.polarJumpDebugger.getValue()) {
                        ChatUtil.display("[PolarJump] ForceChangeJumpHurtTime-NextJumpHurtTime: " + this.polarHurtTime);
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.polarHurtTime = 0;
        this.polarHurtCount = 0;
    }
}
