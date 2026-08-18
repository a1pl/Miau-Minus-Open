package com.viaversion.viabackwards.api.data;

import com.google.common.base.Preconditions;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.BiMappings;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.data.MappingDataBase;
import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectArrayMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.util.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BackwardsMappings extends MappingDataBase {
    private final Class<? extends Protocol<?, ?, ?, ?>> vvProtocolClass;
    protected Int2ObjectMap<MappedItem> backwardsItemMappings;
    private Map<String, String> backwardsSoundMappings;
    private Map<String, String> entityNames;
    private Int2ObjectMap<String> enchantmentNames;

    public BackwardsMappings(String unmappedVersion, String mappedVersion) {
        this(unmappedVersion, mappedVersion, null);
    }

    public BackwardsMappings(
        String unmappedVersion, String mappedVersion, @Nullable Class<? extends Protocol<?, ?, ?, ?>> vvProtocolClass
    ) {
        super(unmappedVersion, mappedVersion);
        Preconditions.checkArgument(
            vvProtocolClass == null || !vvProtocolClass.isAssignableFrom(BackwardsProtocol.class)
        );
        this.vvProtocolClass = vvProtocolClass;
    }

    @Override
    protected void loadExtras(CompoundTag data) {
        CompoundTag itemNames = data.getCompoundTag("itemnames");
        if (itemNames != null) {
            Preconditions.checkNotNull(this.itemMappings);
            this.backwardsItemMappings = new Int2ObjectOpenHashMap<>(itemNames.size());
            CompoundTag extraItemData = data.getCompoundTag("itemdata");

            for (Entry<String, Tag> entry : itemNames.entrySet()) {
                StringTag name = (StringTag)entry.getValue();
                int id = Integer.parseInt(entry.getKey());
                Integer customModelData = null;
                if (extraItemData != null && extraItemData.contains(entry.getKey())) {
                    CompoundTag entryTag = extraItemData.getCompoundTag(entry.getKey());
                    NumberTag customModelDataTag = entryTag.getNumberTag("custom_model_data");
                    customModelData = customModelDataTag != null ? customModelDataTag.asInt() : null;
                }

                this.backwardsItemMappings
                    .put(id, new MappedItem(this.getNewItemId(id), name.getValue(), customModelData));
            }
        }

        this.entityNames = this.loadNameByStringMappings(data, "entitynames");
        this.enchantmentNames = this.loadNameByIdMappings(data, "enchantmentnames");
        this.backwardsSoundMappings = this.loadNameByStringMappings(data, "soundnames");
    }

    private @Nullable Map<String, String> loadNameByStringMappings(CompoundTag data, String key) {
        CompoundTag nameMappings = data.getCompoundTag(key);
        if (nameMappings == null) {
            return null;
        }

        Map<String, String> map = new HashMap<>(nameMappings.size());

        for (Entry<String, Tag> entry : nameMappings.entrySet()) {
            StringTag mappedTag = (StringTag)entry.getValue();
            map.put(entry.getKey(), mappedTag.getValue());
        }

        return map;
    }

    private @Nullable Int2ObjectMap<String> loadNameByIdMappings(CompoundTag data, String key) {
        CompoundTag nameMappings = data.getCompoundTag(key);
        if (nameMappings == null) {
            return null;
        }

        Int2ObjectMap<String> map = new Int2ObjectArrayMap<>(nameMappings.size());

        for (Entry<String, Tag> entry : nameMappings.entrySet()) {
            StringTag mappedTag = (StringTag)entry.getValue();
            map.put(Integer.parseInt(entry.getKey()), mappedTag.getValue());
        }

        return map;
    }

    @Override
    protected @Nullable BiMappings loadBiMappings(CompoundTag data, String key) {
        if (key.equals("items") && this.vvProtocolClass != null) {
            Mappings mappings = super.loadMappings(data, key);
            MappingData mappingData = Via.getManager()
                .getProtocolManager()
                .getProtocol(this.vvProtocolClass)
                .getMappingData();
            if (mappingData != null && mappingData.getItemMappings() != null) {
                return ItemMappings.of(mappings, mappingData.getItemMappings());
            }
        }

        return super.loadBiMappings(data, key);
    }

    @Override
    public int getNewItemId(int id) {
        return this.itemMappings.getNewId(id);
    }

    @Override
    public int getNewBlockId(int id) {
        return this.blockMappings.getNewId(id);
    }

    @Override
    public int getOldItemId(int id) {
        return this.checkValidity(id, this.itemMappings.inverse().getNewId(id), "item");
    }

    public @Nullable MappedItem getMappedItem(int id) {
        return this.backwardsItemMappings != null ? this.backwardsItemMappings.get(id) : null;
    }

    public @Nullable String getMappedNamedSound(String id) {
        return this.backwardsSoundMappings == null
            ? null
            : this.backwardsSoundMappings.get(Key.stripMinecraftNamespace(id));
    }

    public @Nullable String mappedEntityName(String entityName) {
        if (this.entityNames == null) {
            this.getLogger()
                .log(
                    Level.SEVERE,
                    "No entity mappings found when requesting them for " + entityName,
                    new RuntimeException()
                );
            return null;
        } else {
            return this.entityNames.get(entityName);
        }
    }

    public @Nullable String mappedEnchantmentName(int enchantmentId) {
        if (this.enchantmentNames == null) {
            ViaBackwards.getPlatform()
                .getLogger()
                .log(
                    Level.SEVERE,
                    "No enchantment name mappings found when requesting " + enchantmentId,
                    new RuntimeException()
                );
            return null;
        } else {
            return this.enchantmentNames.get(enchantmentId);
        }
    }

    public @Nullable Int2ObjectMap<MappedItem> getBackwardsItemMappings() {
        return this.backwardsItemMappings;
    }

    public @Nullable Map<String, String> getBackwardsSoundMappings() {
        return this.backwardsSoundMappings;
    }

    public @Nullable Class<? extends Protocol<?, ?, ?, ?>> getViaVersionProtocolClass() {
        return this.vvProtocolClass;
    }

    @Override
    protected Logger getLogger() {
        return ViaBackwards.getPlatform().getLogger();
    }

    @Override
    protected @Nullable CompoundTag readMappingsFile(String name) {
        return BackwardsMappingDataLoader.INSTANCE.loadNBTFromDir(name);
    }
}
