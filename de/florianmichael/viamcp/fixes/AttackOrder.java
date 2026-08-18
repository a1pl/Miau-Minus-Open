package de.florianmichael.viamcp.fixes;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class AttackOrder {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public static void sendConditionalSwing(MovingObjectPosition mop) {
        if (mop != null && mop.field_72313_a != MovingObjectType.ENTITY) {
            mc.field_71439_g.func_71038_i();
        }
    }

    public static void sendFixedAttack(EntityPlayer entityIn, Entity target) {
        if (ViaLoadingBase.getInstance().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            mc.field_71439_g.func_71038_i();
            mc.field_71442_b.func_78764_a(entityIn, target);
        } else {
            mc.field_71442_b.func_78764_a(entityIn, target);
            mc.field_71439_g.func_71038_i();
        }
    }
}
