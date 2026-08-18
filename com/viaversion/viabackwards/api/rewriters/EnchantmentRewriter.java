package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EnchantmentRewriter {
    protected final Map<String, String> enchantmentMappings = new HashMap<>();
    protected final BackwardsItemRewriter<?, ?, ?> itemRewriter;
    private final boolean jsonFormat;

    public EnchantmentRewriter(BackwardsItemRewriter<?, ?, ?> itemRewriter, boolean jsonFormat) {
        this.itemRewriter = itemRewriter;
        this.jsonFormat = jsonFormat;
    }

    public EnchantmentRewriter(BackwardsItemRewriter<?, ?, ?> itemRewriter) {
        this(itemRewriter, true);
    }

    public void registerEnchantment(String key, String replacementLore) {
        this.enchantmentMappings.put(Key.stripMinecraftNamespace(key), replacementLore);
    }

    public void handleToClient(Item item) {
        CompoundTag tag = item.tag();
        if (tag != null) {
            if (tag.getListTag("Enchantments") != null) {
                this.rewriteEnchantmentsToClient(tag, false);
            }

            if (tag.getListTag("StoredEnchantments") != null) {
                this.rewriteEnchantmentsToClient(tag, true);
            }
        }
    }

    public void handleToServer(Item item) {
        CompoundTag tag = item.tag();
        if (tag != null) {
            if (tag.contains(this.itemRewriter.nbtTagName("Enchantments"))) {
                this.rewriteEnchantmentsToServer(tag, false);
            }

            if (tag.contains(this.itemRewriter.nbtTagName("StoredEnchantments"))) {
                this.rewriteEnchantmentsToServer(tag, true);
            }
        }
    }

    public void rewriteEnchantmentsToClient(CompoundTag tag, boolean storedEnchant) {
        String key = storedEnchant ? "StoredEnchantments" : "Enchantments";
        ListTag<CompoundTag> enchantments = tag.getListTag(key, CompoundTag.class);
        List<StringTag> loreToAdd = new ArrayList<>();
        boolean changed = false;
        Iterator<CompoundTag> iterator = enchantments.iterator();

        while (iterator.hasNext()) {
            CompoundTag enchantmentEntry = iterator.next();
            StringTag idTag = enchantmentEntry.getStringTag("id");
            if (idTag != null) {
                String enchantmentId = Key.stripMinecraftNamespace(idTag.getValue());
                String remappedName = this.enchantmentMappings.get(enchantmentId);
                if (remappedName != null) {
                    if (!changed) {
                        this.itemRewriter.saveListTag(tag, enchantments, key);
                        changed = true;
                    }

                    iterator.remove();
                    NumberTag levelTag = enchantmentEntry.getNumberTag("lvl");
                    int level = levelTag != null ? levelTag.asInt() : 1;
                    String loreValue = remappedName + " " + getRomanNumber(level);
                    if (this.jsonFormat) {
                        loreValue = ComponentUtil.legacyToJsonString(loreValue);
                    }

                    loreToAdd.add(new StringTag(loreValue));
                }
            }
        }

        if (!loreToAdd.isEmpty()) {
            if (!storedEnchant && enchantments.isEmpty()) {
                CompoundTag dummyEnchantment = new CompoundTag();
                dummyEnchantment.putString("id", "");
                dummyEnchantment.putShort("lvl", (short)0);
                enchantments.add(dummyEnchantment);
            }

            CompoundTag display = tag.getCompoundTag("display");
            if (display == null) {
                tag.put("display", display = new CompoundTag());
            }

            ListTag<StringTag> loreTag = display.getListTag("Lore", StringTag.class);
            if (loreTag == null) {
                display.put("Lore", loreTag = new ListTag<>(StringTag.class));
            } else {
                this.itemRewriter.saveListTag(display, loreTag, "Lore");
            }

            loreToAdd.addAll(loreTag.getValue());
            loreTag.setValue(loreToAdd);
        }
    }

    public void rewriteEnchantmentsToServer(CompoundTag tag, boolean storedEnchant) {
        String key = storedEnchant ? "StoredEnchantments" : "Enchantments";
        this.itemRewriter.restoreListTag(tag, key);
    }

    public static String getRomanNumber(int number) {
        switch (number) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            case 6:
                return "VI";
            case 7:
                return "VII";
            case 8:
                return "VIII";
            case 9:
                return "IX";
            case 10:
                return "X";
            default:
                return Integer.toString(number);
        }
    }
}
