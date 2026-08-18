package miau.module.modules.player;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.impl.WindowClickEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorItemSword;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.util.client.ChatUtil;
import miau.util.player.ItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.world.WorldSettings.GameType;
import org.apache.commons.lang3.RandomUtils;

public class ChestStealer extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private int clickDelay = 0;
    private int oDelay = 0;
    private boolean inChest = false;
    private boolean warnedFull = false;
    public final FloatProperty delayMs = new FloatProperty("delay", 1.0F, 2.0F, 0.0F, 20.0F);
    public final IntProperty openDelay = new IntProperty("open-delay", 1, 0, 20);
    public final BooleanProperty autoClose = new BooleanProperty("auto-close", false);
    public final BooleanProperty nameCheck = new BooleanProperty("name-check", true);
    public final BooleanProperty skipTrash = new BooleanProperty("skip-trash", true);
    public final BooleanProperty moreArmor = new BooleanProperty("more-armor", false);
    public final BooleanProperty moreSword = new BooleanProperty("more-sword", false);

    private boolean isValidGameMode() {
        GameType gameType = mc.field_71442_b.func_178889_l();
        return gameType == GameType.SURVIVAL || gameType == GameType.ADVENTURE;
    }

    private boolean isMoreArmor(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        if (!this.moreArmor.getValue()) {
            return false;
        }

        if (!(itemStack.func_77973_b() instanceof ItemArmor)) {
            return false;
        }

        ArmorMaterial armorMaterial = ((ItemArmor)itemStack.func_77973_b()).func_82812_d();
        return armorMaterial == ArmorMaterial.DIAMOND
            ? true
            : armorMaterial == ArmorMaterial.IRON && itemStack.func_77948_v();
    }

    private boolean isMoreSword(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        } else if (!this.moreSword.getValue()) {
            return false;
        } else if (!(itemStack.func_77973_b() instanceof ItemSword)) {
            return false;
        } else {
            ToolMaterial swordMaterial = ((IAccessorItemSword)itemStack.func_77973_b()).getMaterial();
            if (swordMaterial == ToolMaterial.EMERALD) {
                return true;
            } else {
                return EnchantmentHelper.func_77506_a(Enchantment.field_77334_n.field_77352_x, itemStack) != 0
                    ? true
                    : swordMaterial == ToolMaterial.IRON && itemStack.func_77948_v();
            }
        }
    }

    private boolean isInvManagerRequire(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        } else {
            InvManager invManager = (InvManager)Miau.moduleManager.modules.get(InvManager.class);
            if (ItemUtil.ItemType.Block.contains(itemStack)) {
                return !invManager.isEnabled()
                    || ItemUtil.findInventorySlot(ItemUtil.ItemType.Block) < invManager.blocks.getValue();
            } else if (ItemUtil.ItemType.Projectile.contains(itemStack)) {
                return !invManager.isEnabled()
                    || ItemUtil.findInventorySlot(ItemUtil.ItemType.Projectile) < invManager.projectiles.getValue();
            } else if (ItemUtil.ItemType.FishRod.contains(itemStack)) {
                return ItemUtil.findInventorySlot(ItemUtil.ItemType.Projectile) == 0;
            } else {
                return !ItemUtil.ItemType.Arrow.contains(itemStack)
                    ? false
                    : !invManager.isEnabled()
                        || ItemUtil.findInventorySlot(ItemUtil.ItemType.Arrow) < invManager.arrow.getValue();
            }
        }
    }

    private void shiftClick(int windowId, int slotId) {
        mc.field_71442_b.func_78753_a(windowId, slotId, 0, 1, mc.field_71439_g);
    }

    public ChestStealer() {
        super("ChestStealer", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.clickDelay > 0) {
                this.clickDelay--;
            }

            if (this.oDelay > 0) {
                this.oDelay--;
            }

            if (!(mc.field_71462_r instanceof GuiChest)) {
                this.inChest = false;
            } else {
                Container container = ((GuiChest)mc.field_71462_r).field_147002_h;
                if (!(container instanceof ContainerChest)) {
                    this.inChest = false;
                } else {
                    if (!this.inChest) {
                        this.inChest = true;
                        this.warnedFull = false;
                        this.oDelay = this.openDelay.getValue() + 1;
                    }

                    if (this.oDelay <= 0 && this.clickDelay <= 0 && this.isEnabled() && this.isValidGameMode()) {
                        IInventory inventory = ((ContainerChest)container).func_85151_d();
                        if (this.nameCheck.getValue()) {
                            String inventoryName = inventory.func_70005_c_();
                            if (!inventoryName.equals(I18n.func_135052_a("container.chest", new Object[0]))
                                && !inventoryName.equals(I18n.func_135052_a("container.chestDouble", new Object[0]))) {
                                return;
                            }
                        }

                        if (mc.field_71439_g.field_71071_by.func_70447_i() == -1) {
                            if (!this.warnedFull) {
                                ChatUtil.display("%s%s: &cYour inventory is full!&r", this.getName());
                                this.warnedFull = true;
                            }

                            if (this.autoClose.getValue()) {
                                mc.field_71439_g.func_71053_j();
                            }
                        } else {
                            if (this.skipTrash.getValue()) {
                                int bestSword = -1;
                                double bestDamage = 0.0;
                                int[] bestArmorSlots = new int[]{-1, -1, -1, -1};
                                double[] bestArmorProtection = new double[]{0.0, 0.0, 0.0, 0.0};
                                int bestPickaxeSlot = -1;
                                float bestPickaxeEfficiency = 1.0F;
                                int bestShovelSlot = -1;
                                float bestShovelEfficiency = 1.0F;
                                int bestAxeSlot = -1;
                                float bestAxeEfficiency = 1.0F;
                                int bestBow = -1;
                                double bestBowDamage = 0.0;

                                for (int i = 0; i < inventory.func_70302_i_(); i++) {
                                    if (container.func_75139_a(i).func_75216_d()) {
                                        ItemStack stack = container.func_75139_a(i).func_75211_c();
                                        Item item = stack.func_77973_b();
                                        if (item instanceof ItemSword) {
                                            double damage = ItemUtil.getAttackBonus(stack);
                                            if (bestSword == -1 || damage > bestDamage) {
                                                bestSword = i;
                                                bestDamage = damage;
                                            }
                                        } else if (item instanceof ItemArmor) {
                                            int armorType = ((ItemArmor)item).field_77881_a;
                                            double protectionLevel = ItemUtil.getArmorProtection(stack);
                                            if (bestArmorSlots[armorType] == -1
                                                || protectionLevel > bestArmorProtection[armorType]) {
                                                bestArmorSlots[armorType] = i;
                                                bestArmorProtection[armorType] = protectionLevel;
                                            }
                                        } else if (item instanceof ItemPickaxe) {
                                            float efficiency = ItemUtil.getToolEfficiency(stack);
                                            if (bestPickaxeSlot == -1 || efficiency > bestPickaxeEfficiency) {
                                                bestPickaxeSlot = i;
                                                bestPickaxeEfficiency = efficiency;
                                            }
                                        } else if (item instanceof ItemSpade) {
                                            float efficiency = ItemUtil.getToolEfficiency(stack);
                                            if (bestShovelSlot == -1 || efficiency > bestShovelEfficiency) {
                                                bestShovelSlot = i;
                                                bestShovelEfficiency = efficiency;
                                            }
                                        } else if (item instanceof ItemAxe) {
                                            float efficiency = ItemUtil.getToolEfficiency(stack);
                                            if (bestAxeSlot == -1 || efficiency > bestAxeEfficiency) {
                                                bestAxeSlot = i;
                                                bestAxeEfficiency = efficiency;
                                            }
                                        } else if (item instanceof ItemBow) {
                                            double damage = ItemUtil.getBowAttackBonus(stack);
                                            if (bestBow == -1 || damage > bestBowDamage) {
                                                bestBow = i;
                                                bestBowDamage = damage;
                                            }
                                        }
                                    }
                                }

                                int swordInInventorySlot = ItemUtil.findSwordInInventorySlot(0, true);
                                double damage = swordInInventorySlot != -1
                                    ? ItemUtil.getAttackBonus(
                                        mc.field_71439_g.field_71071_by.func_70301_a(swordInInventorySlot)
                                    )
                                    : 0.0;
                                if (bestDamage > damage) {
                                    this.shiftClick(container.field_75152_c, bestSword);
                                    return;
                                }

                                for (int i = 0; i < 4; i++) {
                                    int slot = ItemUtil.findArmorInventorySlot(i, true);
                                    double protectionLevel = slot != -1
                                        ? ItemUtil.getArmorProtection(
                                            mc.field_71439_g.field_71071_by.func_70301_a(slot)
                                        )
                                        : 0.0;
                                    if (bestArmorProtection[i] > protectionLevel) {
                                        this.shiftClick(container.field_75152_c, bestArmorSlots[i]);
                                        return;
                                    }
                                }

                                int pickaxeSlot = ItemUtil.findInventorySlot("pickaxe", 0, true);
                                float pickaxeEfficiency = pickaxeSlot != -1
                                    ? ItemUtil.getToolEfficiency(
                                        mc.field_71439_g.field_71071_by.func_70301_a(pickaxeSlot)
                                    )
                                    : 1.0F;
                                if (bestPickaxeEfficiency > pickaxeEfficiency) {
                                    this.shiftClick(container.field_75152_c, bestPickaxeSlot);
                                    return;
                                }

                                int shovelSlot = ItemUtil.findInventorySlot("shovel", 0, true);
                                float shovelEfficiency = shovelSlot != -1
                                    ? ItemUtil.getToolEfficiency(
                                        mc.field_71439_g.field_71071_by.func_70301_a(shovelSlot)
                                    )
                                    : 1.0F;
                                if (bestShovelEfficiency > shovelEfficiency) {
                                    this.shiftClick(container.field_75152_c, bestShovelSlot);
                                    return;
                                }

                                int axeSlot = ItemUtil.findInventorySlot("axe", 0, true);
                                float efficiency = axeSlot != -1
                                    ? ItemUtil.getToolEfficiency(mc.field_71439_g.field_71071_by.func_70301_a(axeSlot))
                                    : 1.0F;
                                if (bestAxeEfficiency > efficiency) {
                                    this.shiftClick(container.field_75152_c, bestAxeSlot);
                                    return;
                                }

                                int bowSlot = ItemUtil.findBowInventorySlot(0, true);
                                double bowDamage = bowSlot != -1
                                    ? ItemUtil.getBowAttackBonus(mc.field_71439_g.field_71071_by.func_70301_a(bowSlot))
                                    : 0.0;
                                if (bestBowDamage > bowDamage) {
                                    this.shiftClick(container.field_75152_c, bestBow);
                                    return;
                                }
                            }

                            for (int i = 0; i < inventory.func_70302_i_(); i++) {
                                if (container.func_75139_a(i).func_75216_d()) {
                                    ItemStack stack = container.func_75139_a(i).func_75211_c();
                                    if (!this.skipTrash.getValue()
                                        || !ItemUtil.isNotSpecialItem(stack)
                                        || this.isMoreArmor(stack)
                                        || this.isMoreSword(stack)
                                        || this.isInvManagerRequire(stack)) {
                                        this.shiftClick(container.field_75152_c, i);
                                        return;
                                    }
                                }
                            }

                            if (this.autoClose.getValue()) {
                                mc.field_71439_g.func_71053_j();
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onWindowClick(WindowClickEvent event) {
        this.clickDelay = RandomUtils.nextInt(
            this.delayMs.getValue().intValue() + 1, this.delayMs.getSecondValue().intValue() + 2
        );
    }
}
