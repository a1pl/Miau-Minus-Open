package miau.component;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorPlayerControllerMP;
import miau.util.player.IInventoryPlayerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public final class SlotComponent {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static boolean render = true;
    public static boolean finished = true;

    public void setSlot(int slot) {
        this.setSlot(slot, true);
    }

    public void setSlot(int slot, boolean renderEffect) {
        if (slot >= 0 && slot < 9) {
            IInventoryPlayerAccessor inv = (IInventoryPlayerAccessor)mc.field_71439_g.field_71071_by;
            inv.miau$setAlternativeCurrentItem(slot);
            inv.miau$setAlternativeSlot(true);
            render = renderEffect;
            finished = false;
            ((IAccessorPlayerControllerMP)mc.field_71442_b).callSyncCurrentPlayItem();
        }
    }

    public void setSlotDelayed(int slot, boolean force) {
        this.setSlotDelayed(slot, force, true);
    }

    public void setSlotDelayed(int slot, boolean force, boolean renderEffect) {
        if (!(Math.random() * Math.random() > 0.25) && !force) {
            this.setSlot(slot, renderEffect);
        } else {
            this.setSlot(((IAccessorPlayerControllerMP)mc.field_71442_b).getCurrentPlayerItem(), renderEffect);
        }
    }

    public int getItemIndex() {
        if (mc.field_71439_g == null) {
            return 0;
        }

        IInventoryPlayerAccessor inv = (IInventoryPlayerAccessor)mc.field_71439_g.field_71071_by;
        return inv.miau$getAlternativeSlot()
            ? inv.miau$getAlternativeCurrentItem()
            : mc.field_71439_g.field_71071_by.field_70461_c;
    }

    public ItemStack getItemStack() {
        if (mc.field_71439_g != null && mc.field_71439_g.field_71069_bz != null) {
            int index = this.getItemIndex();
            return mc.field_71439_g.field_71069_bz.func_75139_a(index + 36).func_75211_c();
        } else {
            return null;
        }
    }

    public Item getItem() {
        ItemStack stack = this.getItemStack();
        return stack == null ? null : stack.func_77973_b();
    }

    public boolean isHoldingBlock() {
        return this.getItem() instanceof ItemBlock;
    }

    @EventTarget(0)
    public void onPreUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null) {
                IInventoryPlayerAccessor inv = (IInventoryPlayerAccessor)mc.field_71439_g.field_71071_by;
                inv.miau$setAlternativeSlot(false);
                inv.miau$setAlternativeCurrentItem(mc.field_71439_g.field_71071_by.field_70461_c);
            }
        }
    }
}
