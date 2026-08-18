package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorMinecraft;
import miau.module.modules.combat.Velocity;
import miau.util.network.PacketUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class GrimC03Velocity extends VelocityMode {
    private boolean hasReceivedVelocity = false;
    private int timerTicks = 0;

    public GrimC03Velocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (event.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                    if (packet.func_149412_c() == player.func_145782_y() && VelocityUtil.isMoving()) {
                        this.hasReceivedVelocity = true;
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @Override
    public void onTick(TickEvent event) {
        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player != null) {
            float speed = 0.8F + 0.2F * (20 - this.timerTicks) / 20.0F;
            VelocityUtil.changeTimer(Math.min(speed, 1.0F));
            if (this.timerTicks > 0) {
                this.timerTicks--;
            } else if (((IAccessorMinecraft)mc).getTimer().field_74278_d <= 1.0F) {
                VelocityUtil.changeTimer(1.0F);
            }

            if (this.hasReceivedVelocity) {
                BlockPos pos = new BlockPos(player.field_70165_t, player.field_70163_u, player.field_70161_v);
                if (this.checkAir(pos)) {
                    this.hasReceivedVelocity = false;
                }
            }
        }
    }

    private boolean checkAir(BlockPos blockPos) {
        if (mc.field_71441_e == null) {
            return false;
        }

        if (!mc.field_71441_e.func_175623_d(blockPos)) {
            return false;
        }

        this.timerTicks = 20;
        PacketUtil.sendPacketNoEvent(new C03PacketPlayer(true));
        PacketUtil.sendPacketNoEvent(new C07PacketPlayerDigging(Action.STOP_DESTROY_BLOCK, blockPos, EnumFacing.DOWN));
        mc.field_71441_e.func_175698_g(blockPos);
        return true;
    }

    @Override
    public void onDisable() {
        this.hasReceivedVelocity = false;
        this.timerTicks = 0;
        VelocityUtil.changeTimer(1.0F);
    }
}
