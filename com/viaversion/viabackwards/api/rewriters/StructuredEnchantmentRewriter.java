package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.Enchantments;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.fastutil.ints.IntIntPair;
import com.viaversion.viaversion.libs.fastutil.objects.ObjectIterator;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.util.ComponentUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StructuredEnchantmentRewriter {
    protected final BackwardsItemRewriter<?, ?, ?> itemRewriter;
    private boolean rewriteIds = true;

    public StructuredEnchantmentRewriter(BackwardsItemRewriter<?, ?, ?> itemRewriter) {
        this.itemRewriter = itemRewriter;
    }

    public void handleToClient(Item item) {
        StructuredDataContainer data = item.structuredData();
        this.rewriteEnchantmentsToClient(data, StructuredDataKey.ENCHANTMENTS, false);
        this.rewriteEnchantmentsToClient(data, StructuredDataKey.STORED_ENCHANTMENTS, true);
    }

    public void handleToServer(Item item) {
        StructuredDataContainer data = item.structuredData();
        StructuredData<CompoundTag> customData = data.getNonEmpty(StructuredDataKey.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.value();
            if (tag.contains(this.itemRewriter.nbtTagName("enchantments"))) {
                this.rewriteEnchantmentsToServer(data, tag, StructuredDataKey.ENCHANTMENTS, false);
            }

            if (tag.contains(this.itemRewriter.nbtTagName("stored_enchantments"))) {
                this.rewriteEnchantmentsToServer(data, tag, StructuredDataKey.STORED_ENCHANTMENTS, true);
            }
        }
    }

    public void rewriteEnchantmentsToClient(
        StructuredDataContainer data, StructuredDataKey<Enchantments> key, boolean storedEnchant
    ) {
        StructuredData<Enchantments> enchantmentsData = data.getNonEmpty(key);
        if (enchantmentsData != null) {
            CompoundTag tag = data.<CompoundTag>computeIfAbsent(StructuredDataKey.CUSTOM_DATA, $ -> new CompoundTag())
                .value();
            Enchantments enchantments = enchantmentsData.value();
            List<Tag> loreToAdd = new ArrayList<>();
            boolean changed = false;
            ObjectIterator<Int2IntMap.Entry> iterator = enchantments.enchantments().int2IntEntrySet().iterator();
            List<IntIntPair> updatedIds = new ArrayList<>();

            while (iterator.hasNext()) {
                Int2IntMap.Entry entry = iterator.next();
                BackwardsMappings mappingData = this.itemRewriter.protocol().getMappingData();
                Mappings mappings = mappingData.getEnchantmentMappings();
                int mappedId = mappings.getNewId(entry.getIntKey());
                if (mappedId != -1) {
                    if (this.rewriteIds) {
                        updatedIds.add(IntIntPair.of(entry.getIntKey(), mappedId));
                    }
                } else {
                    String remappedName = mappingData.mappedEnchantmentName(entry.getIntKey());
                    if (remappedName != null) {
                        if (!changed) {
                            this.itemRewriter.saveListTag(tag, this.asTag(enchantments), key.identifier());
                            changed = true;
                        }

                        int level = entry.getIntValue();
                        loreToAdd.add(
                            ComponentUtil.jsonStringToTag(
                                ComponentUtil.legacyToJsonString(
                                    "§7" + remappedName + " " + EnchantmentRewriter.getRomanNumber(level), true
                                )
                            )
                        );
                        iterator.remove();
                    }
                }
            }

            for (IntIntPair pair : updatedIds) {
                enchantments.add(pair.firstInt(), pair.secondInt());
            }

            if (!loreToAdd.isEmpty()) {
                if (!storedEnchant && enchantments.size() == 0) {
                    StructuredData<Boolean> glintOverride = data.getNonEmpty(
                        StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE
                    );
                    if (glintOverride != null) {
                        tag.putBoolean(this.itemRewriter.nbtTagName("glint"), glintOverride.value());
                    } else {
                        tag.putBoolean(this.itemRewriter.nbtTagName("noglint"), true);
                    }

                    data.set(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE, true);
                }

                StructuredData<Tag[]> loreData = data.getNonEmpty(StructuredDataKey.LORE);
                if (loreData != null) {
                    List<Tag> loreList = Arrays.asList(loreData.value());
                    this.itemRewriter.saveGenericTagList(tag, loreList, "lore");
                    loreToAdd.addAll(loreList);
                } else {
                    tag.putBoolean(this.itemRewriter.nbtTagName("nolore"), true);
                }

                if (enchantments.showInTooltip()) {
                    tag.putBoolean(this.itemRewriter.nbtTagName("show_" + key.identifier()), true);
                }

                data.set(StructuredDataKey.LORE, loreToAdd.toArray(new Tag[0]));
            }
        }
    }

    private ListTag<CompoundTag> asTag(Enchantments enchantments) {
        ListTag<CompoundTag> listTag = new ListTag<>(CompoundTag.class);

        for (Int2IntMap.Entry entry : enchantments.enchantments().int2IntEntrySet()) {
            CompoundTag enchantment = new CompoundTag();
            enchantment.putInt("id", entry.getIntKey());
            enchantment.putInt("lvl", entry.getIntValue());
            listTag.add(enchantment);
        }

        return listTag;
    }

    public void rewriteEnchantmentsToServer(
        StructuredDataContainer data, CompoundTag tag, StructuredDataKey<Enchantments> key, boolean storedEnchant
    ) {
        ListTag<CompoundTag> enchantmentsTag = this.itemRewriter
            .removeListTag(tag, key.identifier(), CompoundTag.class);
        if (enchantmentsTag != null) {
            Tag glintTag = tag.remove(this.itemRewriter.nbtTagName("glint"));
            if (glintTag instanceof ByteTag) {
                data.set(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE, ((NumberTag)glintTag).asBoolean());
            } else if (tag.remove(this.itemRewriter.nbtTagName("noglint")) != null) {
                data.remove(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE);
            }

            List<Tag> lore = this.itemRewriter.removeGenericTagList(tag, "lore");
            if (lore != null) {
                data.set(StructuredDataKey.LORE, lore.toArray(new Tag[0]));
            } else if (tag.remove(this.itemRewriter.nbtTagName("nolore")) != null) {
                data.remove(StructuredDataKey.LORE);
            }

            Enchantments enchantments = new Enchantments(
                tag.remove(this.itemRewriter.nbtTagName("show_" + key.identifier())) != null
            );

            for (CompoundTag enchantment : enchantmentsTag) {
                enchantments.add(enchantment.getInt("id"), enchantment.getInt("lvl"));
            }

            data.set(key, enchantments);
        }
    }

    public void setRewriteIds(boolean rewriteIds) {
        this.rewriteIds = rewriteIds;
    }
}
