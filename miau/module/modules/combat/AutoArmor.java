package miau.module.modules.combat;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class AutoArmor extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final IntProperty delay = new IntProperty("Delay", 50, 0, 1000);
    public final IntProperty minItemAge = new IntProperty("MinItemAge", 0, 0, 2000);
    public final BooleanProperty smartSwap = new BooleanProperty("SmartSwap", true);
    private final TimerUtil timer = new TimerUtil();
    private int armorType = 0;

    public AutoArmor() {
        super("AutoArmor", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && event.getType() == EventType.PRE) {
            if (mc.field_71462_r instanceof GuiInventory) {
                if (mc.field_71439_g.field_71070_bA.field_75152_c == 0) {
                    if (this.timer.hasTimeElapsed(this.delay.getValue().intValue())) {
                        for (int attempt = 0; attempt < 4; attempt++) {
                            int type = this.armorType;
                            this.armorType = (this.armorType + 1) % 4;
                            if (this.tryEquip(type)) {
                                this.timer.reset();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean tryEquip(int type) {
        int armorSlot = 5 + type;
        ItemStack current = mc.field_71439_g.field_71071_by.field_70460_b[type];
        double bestScore = current == null ? -1.0 : this.getScore(current);
        int bestSlot = -1;

        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.field_71439_g.field_71069_bz.func_75139_a(i).func_75211_c();
            if (stack != null
                && stack.func_77973_b() instanceof ItemArmor
                && ((ItemArmor)stack.func_77973_b()).field_77881_a == 3 - type
                && (this.minItemAge.getValue() <= 0 || stack.field_77992_b <= 0)) {
                double score = this.getScore(stack);
                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot == -1) {
            return false;
        }

        mc.field_71442_b.func_78753_a(0, bestSlot, 0, 1, mc.field_71439_g);
        return true;
    }

    private double getScore(ItemStack stack) {
        ItemArmor armor = (ItemArmor)stack.func_77973_b();
        return armor.field_77879_b * 100.0
            + EnchantmentHelper.func_77506_a(Enchantment.field_180310_c.field_77352_x, stack) * 5.0
            + armor.func_77612_l() / 100.0;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.delay.getValue() + "ms"};
    }
}
