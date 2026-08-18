package com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.rewriter;

import com.google.common.base.Preconditions;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.Protocol1_20_3To1_20_5;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage.RegistryDataStorage;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage.SecureChatStorage;
import com.viaversion.viaversion.api.data.entity.DimensionData;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_20_3;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.data.entity.DimensionDataImpl;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.FloatTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Attributes1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.storage.BannerPatternStorage;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.MathUtil;
import java.util.HashMap;
import java.util.Map;

public final class EntityPacketRewriter1_20_5 extends EntityRewriter<ClientboundPacket1_20_5, Protocol1_20_3To1_20_5> {
    public EntityPacketRewriter1_20_5(Protocol1_20_3To1_20_5 protocol) {
        super(protocol, Types1_20_3.META_TYPES.optionalComponentType, Types1_20_3.META_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        this.registerTrackerWithData1_19(ClientboundPackets1_20_5.SPAWN_ENTITY, EntityTypes1_20_5.FALLING_BLOCK);
        this.registerMetadataRewriter(
            ClientboundPackets1_20_5.ENTITY_METADATA, Types1_20_5.METADATA_LIST, Types1_20_3.METADATA_LIST
        );
        this.registerRemoveEntities(ClientboundPackets1_20_5.REMOVE_ENTITIES);
        this.protocol
            .registerClientbound(
                ClientboundPackets1_20_5.ENTITY_EQUIPMENT,
                wrapper -> {
                    wrapper.passthrough(Type.VAR_INT);

                    byte slot;
                    do {
                        slot = wrapper.read(Type.BYTE);
                        if (slot == 6) {
                            slot = 2;
                        }

                        wrapper.write(Type.BYTE, slot);
                        Item item = this.protocol
                            .getItemRewriter()
                            .handleItemToClient(wrapper.user(), wrapper.read(Types1_20_5.ITEM));
                        wrapper.write(Type.ITEM1_20_2, item);
                    } while (slot < 0);
                }
            );
        this.protocol
            .registerClientbound(
                ClientboundConfigurationPackets1_20_5.REGISTRY_DATA,
                wrapper -> {
                    wrapper.cancel();
                    String registryKey = Key.stripMinecraftNamespace(wrapper.read(Type.STRING));
                    if (!registryKey.equals("wolf_variant")) {
                        RegistryDataStorage registryDataStorage = wrapper.user().get(RegistryDataStorage.class);
                        RegistryEntry[] entries = wrapper.read(Type.REGISTRY_ENTRY_ARRAY);
                        if (registryKey.equals("banner_pattern")) {
                            BannerPatternStorage bannerStorage = new BannerPatternStorage();
                            wrapper.user().put(bannerStorage);

                            for (int i = 0; i < entries.length; i++) {
                                bannerStorage.bannerPatterns().put(i, entries[i].key());
                            }
                        } else {
                            if (registryKey.equals("worldgen/biome")) {
                                this.<EntityTracker>tracker(wrapper.user()).setBiomesSent(entries.length);

                                for (RegistryEntry entry : entries) {
                                    if (entry.tag() != null) {
                                        CompoundTag effects = ((CompoundTag)entry.tag()).getCompoundTag("effects");
                                        CompoundTag particle = effects.getCompoundTag("particle");
                                        if (particle != null) {
                                            CompoundTag particleOptions = particle.getCompoundTag("options");
                                            String particleType = particleOptions.getString("type");
                                            this.updateParticleFormat(
                                                particleOptions, Key.stripMinecraftNamespace(particleType)
                                            );
                                        }
                                    }
                                }
                            } else if (registryKey.equals("dimension_type")) {
                                Map<String, DimensionData> dimensionDataMap = new HashMap<>(entries.length);
                                String[] keys = new String[entries.length];

                                for (int i = 0; i < entries.length; i++) {
                                    RegistryEntry entry = entries[i];
                                    Preconditions.checkNotNull(
                                        entry.tag(), "Server unexpectedly sent null dimension data for " + entry.key()
                                    );
                                    String dimensionKey = Key.stripMinecraftNamespace(entry.key());
                                    CompoundTag tag = (CompoundTag)entry.tag();
                                    this.updateDimensionTypeData(tag);
                                    dimensionDataMap.put(dimensionKey, new DimensionDataImpl(i, tag));
                                    keys[i] = dimensionKey;
                                }

                                registryDataStorage.setDimensionKeys(keys);
                                this.<EntityTracker>tracker(wrapper.user()).setDimensions(dimensionDataMap);
                            }

                            boolean isTrimPattern = registryKey.equals("trim_pattern");
                            CompoundTag registryTag = new CompoundTag();
                            ListTag<CompoundTag> entriesTag = new ListTag<>(CompoundTag.class);
                            registryTag.putString("type", registryKey);
                            registryTag.put("value", entriesTag);

                            for (int i = 0; i < entries.length; i++) {
                                RegistryEntry entry = entries[i];
                                Preconditions.checkNotNull(
                                    entry.tag(), "Server unexpectedly sent null registry data entry for " + entry.key()
                                );
                                if (isTrimPattern) {
                                    CompoundTag patternTag = (CompoundTag)entry.tag();
                                    StringTag templateItem = patternTag.getStringTag("template_item");
                                    if (Protocol1_20_5To1_20_3.MAPPINGS
                                            .getFullItemMappings()
                                            .id(templateItem.getValue())
                                        == -1) {
                                        continue;
                                    }
                                }

                                CompoundTag entryCompoundTag = new CompoundTag();
                                entryCompoundTag.putString("name", entry.key());
                                entryCompoundTag.putInt("id", i);
                                entryCompoundTag.put("element", entry.tag());
                                entriesTag.add(entryCompoundTag);
                            }

                            registryDataStorage.registryData().put(registryKey, registryTag);
                        }
                    }
                }
            );
        this.protocol.registerClientbound(ClientboundPackets1_20_5.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.INT);
                this.map(Type.BOOLEAN);
                this.map(Type.STRING_ARRAY);
                this.map(Type.VAR_INT);
                this.map(Type.VAR_INT);
                this.map(Type.VAR_INT);
                this.map(Type.BOOLEAN);
                this.map(Type.BOOLEAN);
                this.map(Type.BOOLEAN);
                this.handler(wrapper -> {
                    int dimensionId = wrapper.read(Type.VAR_INT);
                    RegistryDataStorage storage = wrapper.user().get(RegistryDataStorage.class);
                    wrapper.write(Type.STRING, storage.dimensionKeys()[dimensionId]);
                });
                this.map(Type.STRING);
                this.map(Type.LONG);
                this.map(Type.BYTE);
                this.map(Type.BYTE);
                this.map(Type.BOOLEAN);
                this.map(Type.BOOLEAN);
                this.map(Type.OPTIONAL_GLOBAL_POSITION);
                this.map(Type.VAR_INT);
                this.handler(wrapper -> {
                    boolean enforcesSecureChat = wrapper.read(Type.BOOLEAN);
                    wrapper.user().get(SecureChatStorage.class).setEnforcesSecureChat(enforcesSecureChat);
                });
                this.handler(EntityPacketRewriter1_20_5.this.worldDataTrackerHandlerByKey());
                this.handler(EntityPacketRewriter1_20_5.this.playerTrackerHandler());
            }
        });
        this.protocol.registerClientbound(ClientboundPackets1_20_5.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                this.handler(wrapper -> {
                    int dimensionId = wrapper.read(Type.VAR_INT);
                    RegistryDataStorage storage = wrapper.user().get(RegistryDataStorage.class);
                    wrapper.write(Type.STRING, storage.dimensionKeys()[dimensionId]);
                });
                this.map(Type.STRING);
                this.handler(EntityPacketRewriter1_20_5.this.worldDataTrackerHandlerByKey());
            }
        });
        this.protocol.registerClientbound(ClientboundPackets1_20_5.ENTITY_EFFECT, wrapper -> {
            wrapper.passthrough(Type.VAR_INT);
            wrapper.passthrough(Type.VAR_INT);
            int amplifier = wrapper.read(Type.VAR_INT);
            wrapper.write(Type.BYTE, (byte)MathUtil.clamp(amplifier, -128, 127));
            wrapper.passthrough(Type.VAR_INT);
            wrapper.passthrough(Type.BYTE);
            wrapper.write(Type.OPTIONAL_COMPOUND_TAG, null);
        });
        this.protocol.registerClientbound(ClientboundPackets1_20_5.ENTITY_PROPERTIES, wrapper -> {
            int entityId = wrapper.passthrough(Type.VAR_INT);
            int size = wrapper.passthrough(Type.VAR_INT);
            int newSize = size;

            for (int i = 0; i < size; i++) {
                int attributeId = wrapper.read(Type.VAR_INT);
                String attribute = Attributes1_20_5.idToKey(attributeId);
                int mappedId = this.protocol.getMappingData().getAttributeMappings().getNewId(attributeId);
                if ("generic.jump_strength".equals(attribute)) {
                    EntityType type = this.<EntityTracker>tracker(wrapper.user()).entityType(entityId);
                    if (type == null || !type.isOrHasParent(EntityTypes1_20_5.HORSE)) {
                        mappedId = -1;
                    }
                }

                if (mappedId == -1) {
                    newSize--;
                    wrapper.read(Type.DOUBLE);
                    int modifierSize = wrapper.read(Type.VAR_INT);

                    for (int j = 0; j < modifierSize; j++) {
                        wrapper.read(Type.UUID);
                        wrapper.read(Type.DOUBLE);
                        wrapper.read(Type.BYTE);
                    }
                } else {
                    wrapper.write(Type.STRING, attribute);
                    wrapper.passthrough(Type.DOUBLE);
                    int modifierSize = wrapper.passthrough(Type.VAR_INT);

                    for (int j = 0; j < modifierSize; j++) {
                        wrapper.passthrough(Type.UUID);
                        wrapper.passthrough(Type.DOUBLE);
                        wrapper.passthrough(Type.BYTE);
                    }
                }
            }

            wrapper.set(Type.VAR_INT, 1, newSize);
        });
    }

    private void updateParticleFormat(CompoundTag options, String particleType) {
        if ("block".equals(particleType)
            || "block_marker".equals(particleType)
            || "falling_dust".equals(particleType)
            || "dust_pillar".equals(particleType)) {
            this.moveTag(options, "block_state", "value");
        } else if ("item".equals(particleType)) {
            Tag item = options.remove("item");
            if (item instanceof StringTag) {
                CompoundTag compoundTag = new CompoundTag();
                compoundTag.put("id", item);
                item = compoundTag;
            }

            options.put("value", item);
        } else if ("dust_color_transition".equals(particleType)) {
            this.moveTag(options, "from_color", "fromColor");
            this.moveTag(options, "to_color", "toColor");
        } else if ("entity_effect".equals(particleType)) {
            Tag color = options.remove("color");
            if (color instanceof ListTag) {
                ListTag<? extends NumberTag> colorParts = (ListTag<? extends NumberTag>)color;
                color = new FloatTag(
                    this.encodeARGB(
                        colorParts.get(0).getValue().floatValue(),
                        colorParts.get(1).getValue().floatValue(),
                        colorParts.get(2).getValue().floatValue(),
                        colorParts.get(3).getValue().floatValue()
                    )
                );
            }

            options.put("value", color);
        }
    }

    private int encodeARGB(float a, float r, float g, float b) {
        int encodedAlpha = this.encodeColorPart(a);
        int encodedRed = this.encodeColorPart(r);
        int encodedGreen = this.encodeColorPart(g);
        int encodedBlue = this.encodeColorPart(b);
        return encodedAlpha << 24 | encodedRed << 16 | encodedGreen << 8 | encodedBlue;
    }

    private int encodeColorPart(float part) {
        return (int)Math.floor(part * 255.0F);
    }

    private void moveTag(CompoundTag compoundTag, String from, String to) {
        Tag tag = compoundTag.remove(from);
        if (tag != null) {
            compoundTag.put(to, tag);
        }
    }

    private void updateDimensionTypeData(CompoundTag elementTag) {
        CompoundTag monsterSpawnLightLevel = elementTag.getCompoundTag("monster_spawn_light_level");
        if (monsterSpawnLightLevel != null) {
            CompoundTag value = new CompoundTag();
            monsterSpawnLightLevel.put("value", value);
            value.putInt("min_inclusive", monsterSpawnLightLevel.getInt("min_inclusive"));
            value.putInt("max_inclusive", monsterSpawnLightLevel.getInt("max_inclusive"));
        }
    }

    @Override
    protected void registerRewrites() {
        this.filter().mapMetaType(typeId -> {
            if (typeId == Types1_20_5.META_TYPES.particlesType.typeId()) {
                return Types1_20_5.META_TYPES.particlesType;
            }

            int id = typeId;
            if (typeId >= Types1_20_5.META_TYPES.wolfVariantType.typeId()) {
                id--;
            }

            if (typeId >= Types1_20_5.META_TYPES.armadilloState.typeId()) {
                id--;
            }

            if (typeId >= Types1_20_5.META_TYPES.particlesType.typeId()) {
                id--;
            }

            return Types1_20_3.META_TYPES.byId(id);
        });
        this.registerMetaTypeHandler1_20_3(
            Types1_20_3.META_TYPES.itemType,
            Types1_20_3.META_TYPES.blockStateType,
            Types1_20_3.META_TYPES.optionalBlockStateType,
            Types1_20_3.META_TYPES.particleType,
            null,
            Types1_20_3.META_TYPES.componentType,
            Types1_20_3.META_TYPES.optionalComponentType
        );
        this.filter().type(EntityTypes1_20_5.LIVINGENTITY).index(10).handler((event, meta) -> {
            Particle[] particles = meta.value();
            int color = 0;

            for (Particle particle : particles) {
                if (particle.id() == this.protocol.getMappingData().getParticleMappings().id("entity_effect")) {
                    color = particle.<Integer>removeArgument(0).getValue();
                }
            }

            meta.setTypeAndValue(Types1_20_3.META_TYPES.varIntType, color);
        });
        this.filter().type(EntityTypes1_20_5.AREA_EFFECT_CLOUD).addIndex(9);
        this.filter().type(EntityTypes1_20_5.AREA_EFFECT_CLOUD).index(11).handler((event, meta) -> {
            Particle particle = meta.value();
            if (particle.id() == this.protocol.getMappingData().getParticleMappings().mappedId("entity_effect")) {
                int color = particle.<Integer>removeArgument(0).getValue();
                event.createExtraMeta(new Metadata(9, Types1_20_3.META_TYPES.varIntType, color));
            }
        });
        this.filter().type(EntityTypes1_20_5.MINECART_ABSTRACT).index(11).handler((event, meta) -> {
            int blockState = meta.<Integer>value();
            meta.setValue(this.protocol.getMappingData().getNewBlockStateId(blockState));
        });
        this.filter().type(EntityTypes1_20_5.LLAMA).addIndex(20);
        this.filter().type(EntityTypes1_20_5.ARMADILLO).removeIndex(17);
        this.filter().type(EntityTypes1_20_5.WOLF).removeIndex(22);
        this.filter().type(EntityTypes1_20_5.OMINOUS_ITEM_SPAWNER).removeIndex(8);
    }

    @Override
    public void onMappingDataLoaded() {
        this.mapTypes();
        this.mapEntityTypeWithData(EntityTypes1_20_5.ARMADILLO, EntityTypes1_20_5.COW).tagName();
        this.mapEntityTypeWithData(EntityTypes1_20_5.BOGGED, EntityTypes1_20_5.STRAY).tagName();
        this.mapEntityTypeWithData(EntityTypes1_20_5.BREEZE_WIND_CHARGE, EntityTypes1_20_5.WIND_CHARGE);
        this.mapEntityTypeWithData(EntityTypes1_20_5.OMINOUS_ITEM_SPAWNER, EntityTypes1_20_5.TEXT_DISPLAY);
    }

    @Override
    public EntityType typeFromId(int type) {
        return EntityTypes1_20_5.getTypeFromId(type);
    }
}
