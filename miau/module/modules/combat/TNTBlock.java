package miau.module.modules.combat;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

public class TNTBlock extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final IntProperty fuse = new IntProperty("Fuse", 10, 0, 80);
    public final FloatProperty range = new FloatProperty("Range", 9.0F, 1.0F, 20.0F);
    public final BooleanProperty autoSword = new BooleanProperty("AutoSword", true);
    private boolean blocked = false;

    public TNTBlock() {
        super("TNTBlock", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            if (event.getType() == EventType.PRE) {
                for (Object o : mc.field_71441_e.field_72996_f) {
                    if (o instanceof EntityTNTPrimed) {
                        EntityTNTPrimed entity = (EntityTNTPrimed)o;
                        if (entity.field_70516_a <= this.fuse.getValue()
                            && !(mc.field_71439_g.func_70068_e(entity) > this.range.getValue() * this.range.getValue())
                            )
                         {
                            if (this.autoSword.getValue()) {
                                int slot = -1;
                                float bestDamage = 1.0F;

                                for (int i = 0; i < 9; i++) {
                                    ItemStack itemStack = mc.field_71439_g.field_71071_by.func_70301_a(i);
                                    if (itemStack != null && itemStack.func_77973_b() instanceof ItemSword) {
                                        float itemDamage = ((ItemSword)itemStack.func_77973_b()).func_150931_i() + 4.0F;
                                        if (itemDamage > bestDamage) {
                                            bestDamage = itemDamage;
                                            slot = i;
                                        }
                                    }
                                }

                                if (slot != -1 && slot != mc.field_71439_g.field_71071_by.field_70461_c) {
                                    mc.field_71439_g.field_71071_by.field_70461_c = slot;
                                    mc.field_71442_b.func_78765_e();
                                }
                            }

                            if (mc.field_71439_g.func_70694_bm() != null
                                && mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemSword) {
                                KeyBinding.func_74510_a(mc.field_71474_y.field_74313_G.func_151463_i(), true);
                                this.blocked = true;
                            }

                            return;
                        }
                    }
                }

                if (this.blocked && !GameSettings.func_100015_a(mc.field_71474_y.field_74313_G)) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74313_G.func_151463_i(), false);
                    this.blocked = false;
                }
            }
        }
    }
}
