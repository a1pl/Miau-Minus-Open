package miau.module.modules.combat;

import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.module.Module;
import miau.property.properties.IntProperty;
import miau.util.network.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;

public class ComboOneHit extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final IntProperty attackPackets = new IntProperty("AttackPackets", 50, 1, 1000);

    public ComboOneHit() {
        super("ComboOneHit", false);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null) {
            Entity target = event.getTarget();
            if (target != null) {
                for (int i = 0; i < this.attackPackets.getValue(); i++) {
                    PacketUtil.sendPacket(new C02PacketUseEntity(target, Action.ATTACK));
                    mc.field_71439_g.func_71038_i();
                }
            }
        }
    }
}
