package miau.module.modules.combat;

import com.google.common.base.CaseFormat;
import miau.event.EventTarget;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;

public class Refill extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final IntProperty delay = new IntProperty("delay", 1, 0, 20);
    public final ModeProperty mode = new ModeProperty("mode", 1, new String[]{"SOUP", "POT"});
    private final TimerUtil time = new TimerUtil();

    public Refill() {
        super("Refill", false);
    }

    @EventTarget
    public void onUpdate(TickEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && event.getType() == EventType.PRE) {
            if (this.mode.getValue() == 0) {
                this.refill(Items.field_151009_A);
            } else if (this.mode.getValue() == 1) {
                this.refill(ItemPotion.func_150899_d(373));
            }
        }
    }

    private void refill(Item targetItem) {
        if (mc.field_71462_r instanceof GuiInventory
            && !isHotbarFull()
            && this.time.hasTimeElapsed(this.delay.getValue() * 50)) {
            for (int i = 9; i < 36; i++) {
                ItemStack itemstack = mc.field_71439_g.field_71069_bz.func_75139_a(i).func_75211_c();
                if (itemstack != null && itemstack.func_77973_b() == targetItem) {
                    mc.field_71442_b.func_78753_a(0, i, 0, 1, mc.field_71439_g);
                    break;
                }
            }

            this.time.reset();
        }
    }

    public static boolean isHotbarFull() {
        for (int i = 0; i <= 36; i++) {
            ItemStack itemstack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (itemstack == null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
