package miau.module.modules.movement.noslow;

import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.movement.NoSlow;
import miau.util.network.PacketUtil;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class OMOldIntaveNoSlow extends NoSlowMode {
    private int ticks = 0;

    public OMOldIntaveNoSlow(String name, NoSlow parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.ticks = 0;
    }

    @Override
    public void onDisable() {
        this.ticks = 0;
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            this.ticks++;
            if (this.ticks >= 10) {
                if (this.getParent().isAnyActive()) {
                    int currentSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                    PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot % 8 + 1));
                    PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot));
                    PacketUtil.sendPacket(
                        new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.field_177992_a, EnumFacing.DOWN)
                    );
                    PacketUtil.sendPacket(
                        new C08PacketPlayerBlockPlacement(
                            new BlockPos(-1, -1, -1), 255, mc.field_71439_g.func_70694_bm(), 0.0F, 0.0F, 0.0F
                        )
                    );
                    float multiplier = this.getParent().getMotionMultiplier();
                    mc.field_71439_g.field_71158_b.field_78900_b *= multiplier;
                    mc.field_71439_g.field_71158_b.field_78902_a *= multiplier;
                }
            }
        }
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging digging = (C07PacketPlayerDigging)event.getPacket();
            if (digging.func_180762_c() == Action.RELEASE_USE_ITEM) {
                event.setCancelled(true);
            }
        }
    }
}
