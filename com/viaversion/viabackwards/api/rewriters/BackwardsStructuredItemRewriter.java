package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viabackwards.api.data.MappedItem;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BackwardsStructuredItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType, T extends BackwardsProtocol<C, ?, ?, S>>
    extends BackwardsItemRewriter<C, S, T> {
    protected final StructuredEnchantmentRewriter enchantmentRewriter = new StructuredEnchantmentRewriter(this);

    public BackwardsStructuredItemRewriter(T protocol, Type<Item> itemType, Type<Item[]> itemArrayType) {
        super(protocol, itemType, itemArrayType);
    }

    public BackwardsStructuredItemRewriter(
        T protocol,
        Type<Item> itemType,
        Type<Item[]> itemArrayType,
        Type<Item> mappedItemType,
        Type<Item[]> mappedItemArrayType
    ) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType);
    }

    @Override
    public @Nullable Item handleItemToClient(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        StructuredDataContainer data = item.structuredData();
        data.setIdLookup(this.protocol, true);
        if (this.protocol.getMappingData().getEnchantmentMappings() != null) {
            this.enchantmentRewriter.handleToClient(item);
        }

        if (this.protocol.getTranslatableRewriter() != null) {
            StructuredData<Tag> customNameData = data.getNonEmpty(StructuredDataKey.CUSTOM_NAME);
            if (customNameData != null) {
                Tag originalName = customNameData.value().copy();
                this.protocol.getTranslatableRewriter().processTag(connection, customNameData.value());
                if (!customNameData.value().equals(originalName)) {
                    this.saveTag(this.createCustomTag(item), originalName, "Name");
                }
            }

            StructuredData<Tag[]> loreData = data.getNonEmpty(StructuredDataKey.LORE);
            if (loreData != null) {
                for (Tag tag : loreData.value()) {
                    this.protocol.getTranslatableRewriter().processTag(connection, tag);
                }
            }
        }

        BackwardsMappings mappingData = this.protocol.getMappingData();
        MappedItem mappedItem = mappingData != null ? mappingData.getMappedItem(item.identifier()) : null;
        if (mappedItem == null) {
            if (mappingData != null && mappingData.getItemMappings() != null) {
                item.setIdentifier(mappingData.getNewItemId(item.identifier()));
            }

            return item;
        } else {
            CompoundTag tag = this.createCustomTag(item);
            tag.putInt(this.nbtTagName("id"), item.identifier());
            item.setIdentifier(mappedItem.id());
            if (mappedItem.customModelData() != null && !data.contains(StructuredDataKey.CUSTOM_MODEL_DATA)) {
                data.set(StructuredDataKey.CUSTOM_MODEL_DATA, mappedItem.customModelData());
            }

            if (!data.contains(StructuredDataKey.CUSTOM_NAME)) {
                data.set(StructuredDataKey.CUSTOM_NAME, mappedItem.tagName());
                tag.putBoolean(this.nbtTagName("customName"), true);
            }

            return item;
        }
    }

    @Override
    public @Nullable Item handleItemToServer(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        BackwardsMappings mappingData = this.protocol.getMappingData();
        if (mappingData != null && mappingData.getItemMappings() != null) {
            item.setIdentifier(mappingData.getOldItemId(item.identifier()));
        }

        StructuredDataContainer data = item.structuredData();
        data.setIdLookup(this.protocol, false);
        if (this.protocol.getMappingData().getEnchantmentMappings() != null) {
            this.enchantmentRewriter.handleToServer(item);
        }

        CompoundTag tag = this.customTag(item);
        if (tag != null) {
            Tag originalId = tag.remove(this.nbtTagName("id"));
            if (originalId instanceof IntTag) {
                item.setIdentifier(((NumberTag)originalId).asInt());
            }
        }

        this.restoreDisplayTag(item);
        return item;
    }

    protected @Nullable CompoundTag customTag(Item item) {
        StructuredData<CompoundTag> customData = item.structuredData().getNonEmpty(StructuredDataKey.CUSTOM_DATA);
        return customData != null ? customData.value() : null;
    }

    protected CompoundTag createCustomTag(Item item) {
        StructuredDataContainer data = item.structuredData();
        StructuredData<CompoundTag> customData = data.getNonEmpty(StructuredDataKey.CUSTOM_DATA);
        if (customData != null) {
            return customData.value();
        }

        CompoundTag tag = new CompoundTag();
        data.set(StructuredDataKey.CUSTOM_DATA, tag);
        return tag;
    }

    @Override
    protected void restoreDisplayTag(Item item) {
        StructuredDataContainer data = item.structuredData();
        StructuredData<CompoundTag> customData = data.getNonEmpty(StructuredDataKey.CUSTOM_DATA);
        if (customData != null) {
            if (customData.value().remove(this.nbtTagName("customName")) != null) {
                data.remove(StructuredDataKey.CUSTOM_NAME);
            } else {
                Tag name = this.removeBackupTag(customData.value(), "Name");
                if (name != null) {
                    data.set(StructuredDataKey.CUSTOM_NAME, name);
                }
            }
        }
    }

    protected void saveTag(CompoundTag customData, Tag tag, String name) {
        String backupName = this.nbtTagName(name);
        if (!customData.contains(backupName)) {
            customData.put(backupName, tag);
        }
    }

    protected @Nullable Tag removeBackupTag(CompoundTag customData, String tagName) {
        return customData.remove(this.nbtTagName(tagName));
    }
}
