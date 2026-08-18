package miau.module.modules.movement.noslow;

import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.movement.NoSlow;
import miau.util.network.PacketUtil;
import net.minecraft.network.play.client.C09PacketHeldItemChange;

public class OMLuckyvnNoSlow extends NoSlowMode {
    public OMLuckyvnNoSlow(String name, NoSlow parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (this.getParent().isAnyActive()) {
            if (event.getType() == EventType.PRE) {
                int currentSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot == 8 ? 0 : currentSlot + 1));
                PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot));
            }

            float multiplier = this.getParent().getMotionMultiplier();
            mc.field_71439_g.field_71158_b.field_78900_b *= multiplier;
            mc.field_71439_g.field_71158_b.field_78902_a *= multiplier;
            if (!this.getParent().canSprint()) {
                mc.field_71439_g.func_70031_b(false);
            }
        }
    }
}
