package miau.module.modules.combat;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.management.RotationState;
import miau.mixin.IAccessorEntityPlayer;
import miau.module.Module;
import miau.property.properties.IntProperty;
import miau.util.network.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class FastBow extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final IntProperty packets = new IntProperty("Packets", 20, 3, 20);

    public FastBow() {
        super("FastBow", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && event.getType() == EventType.PRE) {
            if (mc.field_71439_g.func_71039_bw()) {
                ItemStack currentItem = mc.field_71439_g.field_71071_by.func_70448_g();
                if (currentItem != null && currentItem.func_77973_b() instanceof ItemBow) {
                    PacketUtil.sendPacket(
                        new C08PacketPlayerBlockPlacement(
                            BlockPos.field_177992_a, 255, mc.field_71439_g.func_71045_bC(), 0.0F, 0.0F, 0.0F
                        )
                    );
                    float yaw;
                    float pitch;
                    if (RotationState.isActived()) {
                        yaw = RotationState.getSmoothedYaw();
                        pitch = RotationState.getRotationPitch();
                    } else {
                        yaw = mc.field_71439_g.field_70177_z;
                        pitch = mc.field_71439_g.field_70125_A;
                    }

                    for (int i = 0; i < this.packets.getValue(); i++) {
                        PacketUtil.sendPacket(new C05PacketPlayerLook(yaw, pitch, true));
                    }

                    PacketUtil.sendPacket(
                        new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.field_177992_a, EnumFacing.DOWN)
                    );
                    ((IAccessorEntityPlayer)mc.field_71439_g).setItemInUseCount(currentItem.func_77988_m() - 1);
                }
            }
        }
    }
}
