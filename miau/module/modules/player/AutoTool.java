package miau.module.modules.player;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.BlockDamageEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;

public class AutoTool extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty switchBack = new BooleanProperty("switch-back", false);
    public final BooleanProperty onlySneaking = new BooleanProperty("only-sneaking", false);
    private int previousSlot = -1;

    public AutoTool() {
        super("AutoTool", false);
    }

    @EventTarget(0)
    public void onPreUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE && this.isEnabled()) {
            if (this.switchBack.getValue() && !mc.field_71474_y.field_74312_F.func_151470_d()) {
                this.resetSlot();
            }
        }
    }

    @EventTarget(0)
    public void onBlockDamage(BlockDamageEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            if (event.getPlayer() == mc.field_71439_g) {
                BlockPos pos = event.getBlockPos();
                if (pos != null) {
                    Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
                    if (block != null) {
                        if ((!this.onlySneaking.getValue() || mc.field_71439_g.func_70093_af())
                            && block.func_176195_g(mc.field_71441_e, pos) != 0.0F) {
                            float fastest = 1.0F;
                            int slot = -1;

                            for (int i = 0; i < 9; i++) {
                                ItemStack item = mc.field_71439_g.field_71071_by.func_70301_a(i);
                                if (item != null) {
                                    float speed = item.func_150997_a(block);
                                    if (speed > fastest) {
                                        fastest = speed;
                                        slot = i;
                                    }
                                }
                            }

                            if (slot != -1) {
                                ItemStack equipped = mc.field_71439_g.func_70694_bm();
                                if (equipped == null || equipped.func_150997_a(block) != fastest) {
                                    this.selectSlot(slot);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void selectSlot(int slot) {
        if (this.previousSlot == -1 && slot != Miau.slotComponent.getItemIndex()) {
            this.previousSlot = Miau.slotComponent.getItemIndex();
        }

        Miau.slotComponent.setSlot(slot, false);
    }

    private void resetSlot() {
        if (this.previousSlot != -1) {
            Miau.slotComponent.setSlot(this.previousSlot, false);
        }

        this.previousSlot = -1;
    }

    @Override
    public void onDisabled() {
        this.resetSlot();
    }
}
