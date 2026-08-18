package miau.module.modules.combat.velocity;

import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.util.network.PacketUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C0APacketAnimation;

public class GrimReduceVelocity extends VelocityMode {
    public GrimReduceVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.parent.onSwing.getValue() && !mc.field_71439_g.field_82175_bq
                || mc.field_71439_g.field_70173_aa <= 20) {
                return;
            }

            EntityLivingBase target = null;

            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityLivingBase
                    && entity != mc.field_71439_g
                    && mc.field_71439_g.func_70032_d(entity) <= 7.0F) {
                    target = (EntityLivingBase)entity;
                    break;
                }
            }

            if (target == null) {
                return;
            }

            if (mc.field_71439_g.field_70737_aN > 0) {
                PacketUtil.sendPacketNoEvent(new C0APacketAnimation());
                mc.field_71442_b.func_78764_a(mc.field_71439_g, target);
            }
        }
    }
}
