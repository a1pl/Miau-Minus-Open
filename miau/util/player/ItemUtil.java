package miau.util.player;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Iterator;
import miau.mixin.IAccessorItemSword;
import miau.util.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;

public class ItemUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final ArrayList<Integer> specialItems = new ItemUtil.SpecialItems();

    public static boolean isNotSpecialItem(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        } else {
            Item item = itemStack.func_77973_b();
            if (item instanceof ItemPotion) {
                return ((ItemPotion)item)
                    .func_77832_l(itemStack)
                    .stream()
                    .<Integer>map(PotionEffect::func_76456_a)
                    .noneMatch(specialItems::contains);
            } else if (item instanceof ItemEnderPearl) {
                return false;
            } else if (item instanceof ItemFood && item != Items.field_151070_bp) {
                return false;
            } else {
                return item instanceof ItemMonsterPlacer ? false : item != Items.field_151156_bN;
            }
        }
    }

    public static boolean isBlock(ItemStack itemStack) {
        if (itemStack != null && itemStack.field_77994_a >= 1) {
            Item item = itemStack.func_77973_b();
            return item instanceof ItemBlock ? isContainerBlock((ItemBlock)item) : false;
        } else {
            return false;
        }
    }

    public static boolean isProjectile(ItemStack itemStack) {
        if (itemStack != null && itemStack.field_77994_a >= 1) {
            Item item = itemStack.func_77973_b();
            return item instanceof ItemEgg ? true : item instanceof ItemSnowball;
        } else {
            return false;
        }
    }

    public static boolean isContainerBlock(ItemBlock itemBlock) {
        Block block = itemBlock.func_179223_d();
        return BlockUtil.isInteractable(block) ? false : BlockUtil.isSolid(block);
    }

    public static double getAttackBonus(ItemStack itemStack) {
        double attackBonus = 0.0;
        if (itemStack == null) {
            return 0.0;
        }

        Multimap<String, AttributeModifier> multimap = itemStack.func_111283_C();

        for (String attributeName : multimap.keySet()) {
            if (attributeName.equals("generic.attackDamage")) {
                Iterator<AttributeModifier> iterator = multimap.get(attributeName).iterator();
                if (iterator.hasNext()) {
                    attackBonus += iterator.next().func_111164_d();
                }
                break;
            }
        }

        if (itemStack.func_77948_v()) {
            attackBonus = attackBonus
                + EnchantmentHelper.func_77506_a(Enchantment.field_77334_n.field_77352_x, itemStack)
                + EnchantmentHelper.func_77506_a(Enchantment.field_180314_l.field_77352_x, itemStack) * 1.25;
        }

        return attackBonus;
    }

    public static float getToolEfficiency(ItemStack itemStack) {
        float efficiency = 1.0F;
        if (itemStack != null && itemStack.func_77973_b() instanceof ItemTool) {
            efficiency = ((ItemTool)itemStack.func_77973_b()).func_150913_i().func_77998_b();
            int enchantLevel;
            if (efficiency > 1.0F
                && (enchantLevel = EnchantmentHelper.func_77506_a(Enchantment.field_77349_p.field_77352_x, itemStack))
                    > 0) {
                efficiency += enchantLevel * enchantLevel + 1;
            }
        }

        return efficiency;
    }

    public static float getToolEfficiency(ItemStack itemStack, Block block) {
        float efficiency = 1.0F;
        if (itemStack != null) {
            efficiency = !itemStack.func_150998_b(block) && itemStack.func_77973_b() instanceof ItemPickaxe
                ? 1.0F
                : itemStack.func_150997_a(block);
            int enchantLevel;
            if (itemStack.func_77973_b() instanceof ItemTool
                && efficiency > 1.0F
                && (enchantLevel = EnchantmentHelper.func_77506_a(Enchantment.field_77349_p.field_77352_x, itemStack))
                    > 0) {
                efficiency += enchantLevel * enchantLevel + 1;
            }
        }

        return efficiency;
    }

    public static double getArmorProtection(ItemStack itemStack) {
        double protection = 0.0;
        if (itemStack != null && itemStack.func_77973_b() instanceof ItemArmor) {
            protection = 0.0 + ((ItemArmor)itemStack.func_77973_b()).field_77879_b;
            if (itemStack.func_77948_v()) {
                protection += EnchantmentHelper.func_77506_a(Enchantment.field_180310_c.field_77352_x, itemStack) * 0.8;
                protection += EnchantmentHelper.func_77506_a(Enchantment.field_180309_e.field_77352_x, itemStack)
                    * 0.05;
                protection += EnchantmentHelper.func_77506_a(Enchantment.field_180308_g.field_77352_x, itemStack)
                    * 0.01;
            }
        }

        return protection;
    }

    public static double getBowAttackBonus(ItemStack itemStack) {
        double attackBonus = 0.0;
        if (itemStack != null && itemStack.func_77973_b() instanceof ItemBow) {
            attackBonus = 2.0;
            if (itemStack.func_77948_v()) {
                int power = EnchantmentHelper.func_77506_a(Enchantment.field_77345_t.field_77352_x, itemStack);
                if (power > 0) {
                    attackBonus += (power + 1) * 0.25;
                }

                attackBonus += EnchantmentHelper.func_77506_a(Enchantment.field_77343_v.field_77352_x, itemStack)
                    * 0.25;
                attackBonus += EnchantmentHelper.func_77506_a(Enchantment.field_77342_w.field_77352_x, itemStack)
                    * 0.05;
            }
        }

        return attackBonus;
    }

    public static int findSwordInInventorySlot(int startSlot, boolean checkDurability) {
        int bestSlot = -1;
        double bestAttackBonus = 0.0;
        if (startSlot < 0) {
            return bestSlot;
        }

        for (int i = 0; i < 36; i++) {
            int currentSlot = (startSlot + i) % 36;
            ItemStack itemStack = mc.field_71439_g.field_71071_by.func_70301_a(currentSlot);
            if (itemStack != null
                && itemStack.func_77973_b() instanceof ItemSword
                && (
                    !checkDurability
                        || !itemStack.func_77951_h()
                        || itemStack.func_77958_k() - itemStack.func_77952_i() >= 30
                )) {
                double attackBonus = getAttackBonus(itemStack);
                if (attackBonus > bestAttackBonus) {
                    bestSlot = currentSlot;
                    bestAttackBonus = attackBonus;
                }
            }
        }

        return bestSlot;
    }

    public static int findBowInventorySlot(int startSlot, boolean checkDurability) {
        int bestSlot = -1;
        double bestAttackBonus = 0.0;
        if (startSlot < 0) {
            return bestSlot;
        }

        for (int i = 0; i < 36; i++) {
            int currentSlot = (startSlot + i) % 36;
            ItemStack itemStack = mc.field_71439_g.field_71071_by.func_70301_a(currentSlot);
            if (itemStack != null
                && itemStack.func_77973_b() instanceof ItemBow
                && (
                    !checkDurability
                        || !itemStack.func_77951_h()
                        || itemStack.func_77958_k() - itemStack.func_77952_i() >= 30
                )) {
                double attackBonus = getBowAttackBonus(itemStack);
                if (attackBonus > bestAttackBonus) {
                    bestSlot = currentSlot;
                    bestAttackBonus = attackBonus;
                }
            }
        }

        return bestSlot;
    }

    public static int findInventorySlot(String toolClass, int startSlot, boolean checkDurability) {
        int bestSlot = -1;
        float bestEfficiency = 1.0F;
        if (startSlot < 0) {
            return bestSlot;
        }

        for (int i = 0; i < 36; i++) {
            int currentSlot = (startSlot + i) % 36;
            ItemStack itemStack = mc.field_71439_g.field_71071_by.func_70301_a(currentSlot);
            if (itemStack != null
                && itemStack.func_77973_b() instanceof ItemTool
                && itemStack.func_77973_b().getToolClasses(itemStack).contains(toolClass)
                && (
                    !checkDurability
                        || !itemStack.func_77951_h()
                        || itemStack.func_77958_k() - itemStack.func_77952_i() >= 30
                )) {
                float efficiency = getToolEfficiency(itemStack);
                if (efficiency > bestEfficiency) {
                    bestSlot = currentSlot;
                    bestEfficiency = efficiency;
                }
            }
        }

        return bestSlot;
    }

    public static int findInventorySlot(int currentSlot, Block block) {
        ItemStack currentItem = mc.field_71439_g.field_71071_by.func_70301_a(currentSlot);
        int bestSlot = currentSlot;
        float bestStrength = getToolEfficiency(currentItem, block);

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (itemStack != null) {
                float strength = getToolEfficiency(itemStack, block);
                if (strength > bestStrength) {
                    bestSlot = i;
                    bestStrength = strength;
                }
            }
        }

        return bestSlot;
    }

    public static int findAndurilHotbarSlot(int currentSlot) {
        for (int i = currentSlot; i < currentSlot + 9; i++) {
            ItemStack itemStack = mc.field_71439_g.field_71071_by.func_70301_a(i % 9);
            if (itemStack != null && itemStack.func_77973_b() instanceof ItemSword && itemStack.func_77942_o()) {
                IAccessorItemSword itemSword = (IAccessorItemSword)itemStack.func_77973_b();
                if (itemSword.getMaterial() == ToolMaterial.IRON
                    && itemStack.func_77978_p().func_150297_b("display", 10)) {
                    NBTTagList nbttaglist = itemStack.func_77978_p().func_74775_l("display").func_150295_c("Lore", 8);

                    for (int j = 0; j < nbttaglist.func_74745_c(); j++) {
                        if (nbttaglist.func_150307_f(j).contains("§9Justice")) {
                            return i % 9;
                        }
                    }
                }
            }
        }

        return -1;
    }

    public static int findArmorInventorySlot(int armorType, boolean checkDurability) {
        int bestSlot = -1;
        double bestProtection = 0.0;

        for (int i = 0; i < 40; i++) {
            ItemStack itemStack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (itemStack != null
                && itemStack.func_77973_b() instanceof ItemArmor
                && ((ItemArmor)itemStack.func_77973_b()).field_77881_a == armorType
                && (
                    !checkDurability
                        || !itemStack.func_77951_h()
                        || itemStack.func_77958_k() - itemStack.func_77952_i() >= 30
                )) {
                double protection = getArmorProtection(itemStack);
                if (protection >= bestProtection) {
                    bestSlot = i;
                    bestProtection = protection;
                }
            }
        }

        return bestSlot;
    }

    public static int findInventorySlot(int startSlot, ItemUtil.ItemType itemType) {
        int bestSlot = -1;
        int maxStackSize = 0;
        if (startSlot < 0) {
            startSlot = 0;
        }

        for (int i = 0; i < 36; i++) {
            int currentSlot = (startSlot + i) % 36;
            ItemStack itemStack = mc.field_71439_g.field_71071_by.func_70301_a(currentSlot);
            if (itemStack != null && itemType.contains(itemStack) && maxStackSize < itemStack.field_77994_a) {
                bestSlot = currentSlot;
                maxStackSize = itemStack.field_77994_a;
            }
        }

        return bestSlot;
    }

    public static int findInventorySlot(ItemUtil.ItemType itemType) {
        int stackSize = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (itemStack != null && itemType.contains(itemStack)) {
                stackSize += itemStack.field_77994_a;
            }
        }

        return stackSize;
    }

    public static boolean hasRawUnbreakingEnchant() {
        ItemStack itemStack = mc.field_71439_g.func_70694_bm();
        if (itemStack == null) {
            return false;
        }

        if (itemStack.func_77942_o()) {
            NBTTagCompound tag = itemStack.func_77978_p();
            if (tag.func_74764_b("ExtraAttributes")) {
                NBTTagCompound extra = tag.func_74775_l("ExtraAttributes");
                if (extra.func_74764_b("UHCid")) {
                    long id = extra.func_74763_f("UHCid");
                    if (id == 50006L || id == 50009L) {
                        return true;
                    }
                }
            }

            if (tag.func_74764_b("HideFlags")
                && itemStack.func_77973_b() instanceof ItemSpade
                && ((ItemSpade)itemStack.func_77973_b()).func_150913_i() == ToolMaterial.EMERALD) {
                return true;
            }
        }

        if (itemStack.func_77973_b() instanceof ItemEnchantedBook) {
            return false;
        } else {
            return EnchantmentHelper.func_82781_a(itemStack).containsKey(19)
                ? true
                : itemStack.func_77973_b() instanceof ItemSword;
        }
    }

    public static boolean isHoldingSword() {
        ItemStack itemStack = mc.field_71439_g.func_70694_bm();
        return itemStack == null ? false : itemStack.func_77973_b() instanceof ItemSword;
    }

    public static boolean isHoldingTool() {
        ItemStack itemStack = mc.field_71439_g.func_70694_bm();
        return itemStack == null ? false : itemStack.func_77973_b() instanceof ItemTool;
    }

    public static boolean isEating() {
        ItemStack itemStack = mc.field_71439_g.func_70694_bm();
        if (itemStack == null) {
            return false;
        } else {
            return ItemPotion.func_77831_g(itemStack.func_77973_b().getMetadata(itemStack))
                ? false
                : itemStack.func_77975_n() == EnumAction.EAT || itemStack.func_77975_n() == EnumAction.DRINK;
        }
    }

    public static boolean isUsingBow() {
        ItemStack itemStack = mc.field_71439_g.func_70694_bm();
        return itemStack == null ? false : itemStack.func_77973_b() instanceof ItemBow;
    }

    public static boolean isHoldingNonEmpty() {
        ItemStack itemStack = mc.field_71439_g.func_70694_bm();
        return itemStack != null && itemStack.field_77994_a >= 1
            ? itemStack.func_77973_b() instanceof ItemBlock
            : false;
    }

    public static boolean isHoldingBlock() {
        return isBlock(mc.field_71439_g.func_70694_bm());
    }

    public static boolean hasHoldItem() {
        ItemStack itemStack = mc.field_71439_g.func_70694_bm();
        return itemStack != null && itemStack.field_77994_a >= 1
            ? itemStack.func_77973_b() instanceof ItemFireball
            : false;
    }

    public enum ItemType {
        Block {
            @Override
            public boolean contains(ItemStack itemStack) {
                return ItemUtil.isBlock(itemStack);
            }
        },
        Projectile {
            @Override
            public boolean contains(ItemStack itemStack) {
                return ItemUtil.isProjectile(itemStack);
            }
        },
        FishRod {
            @Override
            public boolean contains(ItemStack itemStack) {
                return itemStack.func_77973_b() instanceof ItemFishingRod;
            }
        },
        GoldApple {
            @Override
            public boolean contains(ItemStack itemStack) {
                return itemStack.func_77973_b() instanceof ItemAppleGold;
            }
        },
        Arrow {
            @Override
            public boolean contains(ItemStack itemStack) {
                return itemStack.func_77973_b() == Items.field_151032_g;
            }
        };

        ItemType() {
        }

        public abstract boolean contains(ItemStack var1);
    }

    static final class SpecialItems extends ArrayList<Integer> {
        SpecialItems() {
            this.add(1);
            this.add(3);
            this.add(5);
            this.add(6);
            this.add(8);
            this.add(10);
            this.add(11);
            this.add(12);
            this.add(14);
            this.add(21);
            this.add(22);
        }
    }
}
