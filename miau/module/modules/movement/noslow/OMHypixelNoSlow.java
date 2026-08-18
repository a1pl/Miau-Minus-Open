package miau.module.modules.movement.noslow;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.movement.NoSlow;
import miau.util.network.PacketUtil;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;

public class OMHypixelNoSlow extends NoSlowMode {
    private boolean sentSprintStart = false;

    public OMHypixelNoSlow(String name, NoSlow parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.sentSprintStart = false;
    }

    @Override
    public void onDisable() {
        this.sentSprintStart = false;
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.getParent().isAnyActive()) {
                int currentSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot % 8 + 1));
                PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot));
                if (!this.getParent().hypixelJump.getValue() && mc.field_71439_g.func_70051_ag()) {
                    PacketUtil.sendPacket(new C0BPacketEntityAction(mc.field_71439_g, Action.STOP_SPRINTING));
                    this.sentSprintStart = false;
                }

                float multiplier = this.getParent().getMotionMultiplier();
                mc.field_71439_g.field_71158_b.field_78900_b *= multiplier;
                mc.field_71439_g.field_71158_b.field_78902_a *= multiplier;
            } else if (mc.field_71439_g.func_70051_ag()
                && !this.getParent().hypixelJump.getValue()
                && !this.sentSprintStart) {
                PacketUtil.sendPacket(new C0BPacketEntityAction(mc.field_71439_g, Action.START_SPRINTING));
                this.sentSprintStart = true;
            }
        }
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND) {
            this.sentSprintStart = false;
        }
    }
}
