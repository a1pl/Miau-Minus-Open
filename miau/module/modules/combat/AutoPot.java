package miau.module.modules.combat;

import java.util.List;
import java.util.Random;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.network.PacketUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class AutoPot extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final FloatProperty health = new FloatProperty(
        "Health", 15.0F, 1.0F, 20.0F, () -> this.healPotion.getValue() || this.regenerationPotion.getValue()
    );
    private final IntProperty delay = new IntProperty("Delay", 500, 500, 1000);
    private final BooleanProperty healPotion = new BooleanProperty("HealPotion", true);
    private final BooleanProperty regenerationPotion = new BooleanProperty("RegenPotion", true);
    private final BooleanProperty fireResistancePotion = new BooleanProperty("FireResPotion", true);
    private final BooleanProperty strengthPotion = new BooleanProperty("StrengthPotion", true);
    private final BooleanProperty jumpPotion = new BooleanProperty("JumpPotion", true);
    private final BooleanProperty speedPotion = new BooleanProperty("SpeedPotion", true);
    private final BooleanProperty openInventory = new BooleanProperty("OpenInv", false);
    private final BooleanProperty simulateInventory = new BooleanProperty(
        "SimulateInventory", true, () -> !this.openInventory.getValue()
    );
    private final FloatProperty groundDistance = new FloatProperty("GroundDistance", 2.0F, 0.0F, 5.0F);
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Normal", "Jump", "Port"});
    private final TimerUtil msTimer = new TimerUtil();
    private final Random random = new Random();

    public AutoPot() {
        super("AutoPot", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null) {
            if (event.getType() == EventType.PRE) {
                if (this.msTimer.hasTimeElapsed(this.delay.getValue().intValue())) {
                    if (!mc.field_71442_b.func_78758_h()) {
                        int potionInHotbar = this.findPotion(0, 8);
                        if (potionInHotbar != -1) {
                            if (mc.field_71439_g.field_70122_E) {
                                switch (this.mode.getModeString()) {
                                    case "Jump":
                                        if (!mc.field_71474_y.field_74314_A.func_151470_d()) {
                                            mc.field_71439_g.func_70664_aZ();
                                        }
                                        break;
                                    case "Port":
                                        mc.field_71439_g.func_70091_d(0.0, 0.42, 0.0);
                                }
                            }

                            Miau.slotComponent.setSlot(potionInHotbar, false);
                            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(potionInHotbar);
                            if (stack != null) {
                                if (mc.field_71439_g.field_70125_A <= 80.0F) {
                                    float pitch = 80.0F + this.random.nextFloat() * 10.0F;
                                    Miau.rotationManager.setSilentRotation(mc.field_71439_g.field_70177_z, pitch, 3);
                                }

                                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(stack));
                                this.msTimer.reset();
                            }
                        } else {
                            int potionInInventory = this.findPotion(9, 35);
                            if (potionInInventory != -1) {
                                if (this.hasSpaceInHotbar()) {
                                    mc.field_71442_b.func_78753_a(0, potionInInventory, 0, 1, mc.field_71439_g);
                                    this.msTimer.reset();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int findPotion(int startSlot, int endSlot) {
        for (int i = startSlot; i <= endSlot; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (stack != null
                && stack.func_77973_b() instanceof ItemPotion
                && ItemPotion.func_77831_g(stack.func_77960_j())) {
                ItemPotion itemPotion = (ItemPotion)stack.func_77973_b();
                List<PotionEffect> effects = itemPotion.func_77832_l(stack);
                if (effects != null) {
                    for (PotionEffect potionEffect : effects) {
                        if (this.healPotion.getValue()
                            && potionEffect.func_76456_a() == Potion.field_76432_h.func_76396_c()
                            && mc.field_71439_g.func_110143_aJ() <= this.health.getValue()) {
                            return i;
                        }
                    }

                    if (!mc.field_71439_g.func_70644_a(Potion.field_76428_l)) {
                        for (PotionEffect potionEffect : effects) {
                            if (this.regenerationPotion.getValue()
                                && potionEffect.func_76456_a() == Potion.field_76428_l.func_76396_c()
                                && mc.field_71439_g.func_110143_aJ() <= this.health.getValue()) {
                                return i;
                            }
                        }
                    }

                    if (!mc.field_71439_g.func_70644_a(Potion.field_76426_n)) {
                        for (PotionEffect potionEffect : effects) {
                            if (this.fireResistancePotion.getValue()
                                && potionEffect.func_76456_a() == Potion.field_76426_n.func_76396_c()) {
                                return i;
                            }
                        }
                    }

                    if (!mc.field_71439_g.func_70644_a(Potion.field_76424_c)) {
                        for (PotionEffect potionEffect : effects) {
                            if (this.speedPotion.getValue()
                                && potionEffect.func_76456_a() == Potion.field_76424_c.func_76396_c()) {
                                return i;
                            }
                        }
                    }

                    if (!mc.field_71439_g.func_70644_a(Potion.field_76430_j)) {
                        for (PotionEffect potionEffect : effects) {
                            if (this.jumpPotion.getValue()
                                && potionEffect.func_76456_a() == Potion.field_76430_j.func_76396_c()) {
                                return i;
                            }
                        }
                    }

                    if (!mc.field_71439_g.func_70644_a(Potion.field_76420_g)) {
                        for (PotionEffect potionEffect : effects) {
                            if (this.strengthPotion.getValue()
                                && potionEffect.func_76456_a() == Potion.field_76420_g.func_76396_c()) {
                                return i;
                            }
                        }
                    }
                }
            }
        }

        return -1;
    }

    private boolean hasSpaceInHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.field_71439_g.field_71071_by.func_70301_a(i) == null) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
