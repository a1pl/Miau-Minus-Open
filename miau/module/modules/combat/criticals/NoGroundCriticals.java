package miau.module.modules.combat.criticals;

import miau.event.impl.AttackEvent;
import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorC03PacketPlayer;
import miau.module.modules.combat.Criticals;
import net.minecraft.network.play.client.C03PacketPlayer;

public class NoGroundCriticals extends CriticalsMode {
    private boolean attacked;

    public NoGroundCriticals(String name, Criticals parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.attacked = false;
    }

    @Override
    public void onDisable() {
        this.attacked = false;
    }

    @Override
    public void onAttack(AttackEvent event) {
        if (!mc.field_71439_g.func_70090_H()) {
            this.attacked = true;
        }
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (this.attacked && event.getType() == EventType.SEND && event.getPacket() instanceof C03PacketPlayer) {
            ((IAccessorC03PacketPlayer)event.getPacket()).setOnGround(false);
            this.attacked = false;
        }
    }
}
