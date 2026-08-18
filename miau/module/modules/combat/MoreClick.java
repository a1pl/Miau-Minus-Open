package miau.module.modules.combat;

import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.util.client.ChatUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

public class MoreClick extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final IntProperty extraPacket = new IntProperty("ExtraClickPacket", 1, 1, 20);
    public final BooleanProperty keepSprint = new BooleanProperty("KeepSprint", false);
    public final IntProperty sendPacketDelay = new IntProperty("SendPacketDelay", 50, 0, 1000);
    public final BooleanProperty debugger = new BooleanProperty("Debugger", false);
    private final TimerUtil packetDelay = new TimerUtil();
    private boolean silentAttack = false;

    public MoreClick() {
        super("MoreClick", false);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && !this.silentAttack) {
            if (this.packetDelay.hasTimeElapsed(this.sendPacketDelay.getValue().intValue())) {
                this.packetDelay.reset();
                Entity target = event.getTarget();
                if (target != null) {
                    this.silentAttack = true;

                    try {
                        for (int i = 0; i < this.extraPacket.getValue(); i++) {
                            if (mc.field_71439_g.func_70032_d(target) < 3.0F) {
                                mc.field_71439_g.func_71038_i();
                                mc.field_71442_b.func_78764_a(mc.field_71439_g, target);
                                if (this.keepSprint.getValue()) {
                                    mc.field_71439_g.func_70031_b(true);
                                }
                            }
                        }
                    } finally {
                        this.silentAttack = false;
                    }

                    if (this.debugger.getValue()) {
                        ChatUtil.display("&7Attacked x" + this.extraPacket.getValue());
                    }
                }
            }
        }
    }

    @Override
    public void onEnabled() {
        this.packetDelay.reset();
    }

    @Override
    public void onDisabled() {
        this.packetDelay.reset();
    }
}
