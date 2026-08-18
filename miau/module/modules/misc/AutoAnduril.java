package miau.module.modules.misc;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.module.modules.player.InvWalk;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.ui.clickgui.ClickGui;
import miau.util.player.ItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class AutoAnduril extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private int previousSlot = -1;
    private int currentSlot = -1;
    private int intervalTick = -1;
    private int holdTick = -1;
    public final IntProperty interval = new IntProperty("interval", 40, 0, 100);
    public final IntProperty hold = new IntProperty("hold", 1, 0, 20);
    public final BooleanProperty speedCheck = new BooleanProperty("speed-check", false);
    public final IntProperty debug = new IntProperty("debug", 0, 0, 9);

    public AutoAnduril() {
        super("AutoAnduril", false);
    }

    public boolean canSwap() {
        if (mc.field_71476_x != null
            && mc.field_71476_x.field_72313_a == MovingObjectType.BLOCK
            && mc.field_71474_y.field_74312_F.func_151470_d()) {
            return false;
        }

        ItemStack currentItem = mc.field_71439_g
            .field_71071_by
            .func_70301_a(mc.field_71439_g.field_71071_by.field_70461_c);
        if (currentItem != null) {
            if (currentItem.func_77973_b() instanceof ItemBlock && mc.field_71474_y.field_74313_G.func_151470_d()) {
                return false;
            }

            if (!(currentItem.func_77973_b() instanceof ItemSword) && mc.field_71439_g.func_71039_bw()) {
                return false;
            }
        }

        InvWalk invWalk = (InvWalk)Miau.moduleManager.modules.get(InvWalk.class);
        return mc.field_71462_r == null
            || mc.field_71462_r instanceof ClickGui
            || invWalk.isEnabled() && invWalk.canInvWalk();
    }

    public boolean hasSpeed() {
        if (!this.speedCheck.getValue()) {
            return false;
        }

        PotionEffect potionEffect = mc.field_71439_g.func_70660_b(Potion.field_76424_c);
        return potionEffect == null ? false : potionEffect.func_76458_c() > 0;
    }

    @EventTarget(4)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.currentSlot != -1 && this.currentSlot != mc.field_71439_g.field_71071_by.field_70461_c) {
                this.currentSlot = -1;
                this.previousSlot = -1;
                this.intervalTick = this.interval.getValue();
                this.holdTick = -1;
            }

            if (this.intervalTick > 0) {
                this.intervalTick--;
            } else if (this.intervalTick == 0 && this.canSwap() && !this.hasSpeed()) {
                int slot = ItemUtil.findAndurilHotbarSlot(mc.field_71439_g.field_71071_by.field_70461_c);
                if (this.debug.getValue() != 0 && slot == -1) {
                    slot = this.debug.getValue() - 1;
                }

                if (slot != -1 && slot != mc.field_71439_g.field_71071_by.field_70461_c) {
                    this.previousSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                    this.currentSlot = mc.field_71439_g.field_71071_by.field_70461_c = slot;
                    this.intervalTick = -1;
                    this.holdTick = this.hold.getValue();
                    return;
                }

                this.intervalTick = this.interval.getValue();
                this.holdTick = -1;
            }

            if (this.holdTick > 0) {
                this.holdTick--;
            } else if (this.holdTick == 0 && this.previousSlot != -1 && this.canSwap()) {
                mc.field_71439_g.field_71071_by.field_70461_c = this.previousSlot;
                this.previousSlot = -1;
                this.holdTick = -1;
                this.intervalTick = this.interval.getValue();
            }
        }
    }

    @Override
    public void onEnabled() {
        this.previousSlot = -1;
        this.currentSlot = -1;
        this.intervalTick = this.interval.getValue();
        this.holdTick = -1;
    }

    @Override
    public void onDisabled() {
        this.previousSlot = -1;
        this.currentSlot = -1;
        this.intervalTick = -1;
        this.holdTick = -1;
    }
}
