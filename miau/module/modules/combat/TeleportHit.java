package miau.module.modules.combat;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.util.network.PacketUtil;
import miau.util.player.RayCastUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class TeleportHit extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private EntityLivingBase targetEntity = null;
    private boolean shouldHit = false;

    public TeleportHit() {
        super("TeleportHit", false);
    }

    private boolean isSelected(EntityLivingBase entity) {
        return entity != null && entity != mc.field_71439_g && !entity.field_70128_L && entity.func_70089_S();
    }

    private void sendPath(double tx, double ty, double tz) {
        double sx = mc.field_71439_g.field_70165_t;
        double sy = mc.field_71439_g.field_70163_u;
        double sz = mc.field_71439_g.field_70161_v;
        double dx = tx - sx;
        double dy = ty - sy;
        double dz = tz - sz;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double steps = Math.max(1.0, Math.ceil(distance / 0.9));

        for (int i = 1; i <= steps; i++) {
            double t = i / steps;
            PacketUtil.sendPacket(new C04PacketPlayerPosition(sx + dx * t, sy + dy * t, sz + dz * t, false));
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null && mc.field_71441_e != null) {
                MovingObjectPosition mop = RayCastUtil.rayCast(
                    mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A, 100.0, 0.2F
                );
                EntityLivingBase facedEntity = mop != null
                        && mop.field_72313_a == MovingObjectType.ENTITY
                        && mop.field_72308_g instanceof EntityLivingBase
                    ? (EntityLivingBase)mop.field_72308_g
                    : null;
                if (mc.field_71474_y.field_74312_F.func_151470_d()
                    && this.isSelected(facedEntity)
                    && facedEntity.func_70068_e(mc.field_71439_g) >= 1.0) {
                    this.targetEntity = facedEntity;
                }

                if (this.targetEntity != null) {
                    if (!this.shouldHit) {
                        this.shouldHit = true;
                        return;
                    }

                    if (mc.field_71439_g.field_70143_R > 0.0F) {
                        Vec3 rotationVector = RayCastUtil.getVectorForRotation(0.0F, mc.field_71439_g.field_70177_z);
                        double x = mc.field_71439_g.field_70165_t
                            + rotationVector.field_72450_a * (mc.field_71439_g.func_70032_d(this.targetEntity) - 1.0F);
                        double z = mc.field_71439_g.field_70161_v
                            + rotationVector.field_72449_c * (mc.field_71439_g.func_70032_d(this.targetEntity) - 1.0F);
                        double y = this.targetEntity.field_70163_u + 0.25;
                        this.sendPath(x, y + 1.0, z);
                        mc.field_71439_g.func_71038_i();
                        PacketUtil.sendPacket(new C02PacketUseEntity(this.targetEntity, Action.ATTACK));
                        mc.field_71439_g.func_71009_b(this.targetEntity);
                        this.shouldHit = false;
                        this.targetEntity = null;
                    } else if (mc.field_71439_g.field_70122_E) {
                        mc.field_71439_g.func_70664_aZ();
                    }
                } else {
                    this.shouldHit = false;
                }
            }
        }
    }
}
