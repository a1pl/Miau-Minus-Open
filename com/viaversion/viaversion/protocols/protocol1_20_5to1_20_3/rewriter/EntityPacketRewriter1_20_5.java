package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.rewriter;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.DimensionData;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_20_3;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundConfigurationPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPacket1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Attributes1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.BannerPatterns1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.storage.AcknowledgedMessagesStorage;
import com.viaversion.viaversion.rewriter.EntityRewriter;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.TagUtil;
import java.util.Arrays;
import java.util.UUID;
import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class EntityPacketRewriter1_20_5 extends EntityRewriter<ClientboundPacket1_20_3, Protocol1_20_5To1_20_3> {
    private static final UUID CREATIVE_BLOCK_INTERACTION_RANGE = UUID.fromString("736565d2-e1a7-403d-a3f8-1aeb3e302542");
    private static final UUID CREATIVE_ENTITY_INTERACTION_RANGE = UUID.fromString(
        "98491ef6-97b1-4584-ae82-71a8cc85cf73"
    );
    private static final int CREATIVE_MODE_ID = 1;

    public EntityPacketRewriter1_20_5(Protocol1_20_5To1_20_3 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        this.registerTrackerWithData1_19(ClientboundPackets1_20_3.SPAWN_ENTITY, EntityTypes1_20_5.FALLING_BLOCK);
        this.registerMetadataRewriter(
            ClientboundPackets1_20_3.ENTITY_METADATA, Types1_20_3.METADATA_LIST, Types1_20_5.METADATA_LIST
        );
        this.registerRemoveEntities(ClientboundPackets1_20_3.REMOVE_ENTITIES);
        this.protocol
            .registerClientbound(
                ClientboundConfigurationPackets1_20_3.REGISTRY_DATA,
                wrapper -> {
                    PacketWrapper knownPacksPacket = wrapper.create(
                        ClientboundConfigurationPackets1_20_5.SELECT_KNOWN_PACKS
                    );
                    knownPacksPacket.write(Type.VAR_INT, 0);
                    knownPacksPacket.send(Protocol1_20_5To1_20_3.class);
                    CompoundTag registryData = wrapper.read(Type.COMPOUND_TAG);
                    this.cacheDimensionData(wrapper.user(), registryData);
                    this.trackBiomeSize(wrapper.user(), registryData);

                    for (CompoundTag dimensionType : TagUtil.getRegistryEntries(registryData, "dimension_type")) {
                        CompoundTag elementTag = dimensionType.getCompoundTag("element");
                        CompoundTag monsterSpawnLightLevel = elementTag.getCompoundTag("monster_spawn_light_level");
                        if (monsterSpawnLightLevel != null) {
                            CompoundTag value = monsterSpawnLightLevel.removeUnchecked("value");
                            monsterSpawnLightLevel.putInt("min_inclusive", value.getInt("min_inclusive"));
                            monsterSpawnLightLevel.putInt("max_inclusive", value.getInt("max_inclusive"));
                        }
                    }

                    for (CompoundTag biome : TagUtil.getRegistryEntries(registryData, "worldgen/biome")) {
                        CompoundTag effects = biome.getCompoundTag("element").getCompoundTag("effects");
                        this.checkSoundTag(effects.getCompoundTag("mood_sound"), "sound");
                        this.checkSoundTag(effects.getCompoundTag("additions_sound"), "sound");
                        this.checkSoundTag(effects.getCompoundTag("music"), "sound");
                        this.checkSoundTag(effects, "ambient_sound");
                        CompoundTag particle = effects.getCompoundTag("particle");
                        if (particle != null) {
                            CompoundTag particleOptions = particle.getCompoundTag("options");
                            String particleType = particleOptions.getString("type");
                            this.updateParticleFormat(particleOptions, Key.stripMinecraftNamespace(particleType));
                        }
                    }

                    for (Entry<String, Tag> entry : registryData.entrySet()) {
                        CompoundTag entryTag = (CompoundTag)entry.getValue();
                        String type = entryTag.getString("type");
                        ListTag<CompoundTag> valueTag = entryTag.getListTag("value", CompoundTag.class);
                        RegistryEntry[] registryEntries = new RegistryEntry[valueTag.size()];
                        boolean requiresDummyValues = false;
                        int entriesLength = registryEntries.length;

                        for (CompoundTag tag : valueTag) {
                            String name = tag.getString("name");
                            int id = tag.getInt("id");
                            entriesLength = Math.max(entriesLength, id + 1);
                            if (id >= registryEntries.length) {
                                registryEntries = Arrays.copyOf(
                                    registryEntries, Math.max(registryEntries.length * 2, id + 1)
                                );
                                requiresDummyValues = true;
                            }

                            registryEntries[id] = new RegistryEntry(name, tag.get("element"));
                        }

                        if (Key.stripMinecraftNamespace(type).equals("damage_type")) {
                            int length = registryEntries.length;
                            registryEntries = Arrays.copyOf(registryEntries, length + 1);
                            CompoundTag spitData = new CompoundTag();
                            spitData.putString("scaling", "when_caused_by_living_non_player");
                            spitData.putString("message_id", "mob");
                            spitData.putFloat("exhaustion", 0.1F);
                            registryEntries[length] = new RegistryEntry("minecraft:spit", spitData);
                        }

                        if (requiresDummyValues) {
                            if (registryEntries.length != entriesLength) {
                                registryEntries = Arrays.copyOf(registryEntries, entriesLength);
                            }

                            this.replaceNullValues(registryEntries);
                        }

                        PacketWrapper registryPacket = wrapper.create(
                            ClientboundConfigurationPackets1_20_5.REGISTRY_DATA
                        );
                        registryPacket.write(Type.STRING, type);
                        registryPacket.write(Type.REGISTRY_ENTRY_ARRAY, registryEntries);
                        registryPacket.send(Protocol1_20_5To1_20_3.class);
                    }

                    wrapper.cancel();
                    PacketWrapper wolfVariantsPacket = wrapper.create(
                        ClientboundConfigurationPackets1_20_5.REGISTRY_DATA
                    );
                    wolfVariantsPacket.write(Type.STRING, "minecraft:wolf_variant");
                    CompoundTag paleWolf = new CompoundTag();
                    paleWolf.putString("wild_texture", "entity/wolf/wolf");
                    paleWolf.putString("tame_texture", "entity/wolf/wolf_tame");
                    paleWolf.putString("angry_texture", "entity/wolf/wolf_angry");
                    paleWolf.put("biomes", new ListTag<>(StringTag.class));
                    wolfVariantsPacket.write(
                        Type.REGISTRY_ENTRY_ARRAY, new RegistryEntry[]{new RegistryEntry("minecraft:pale", paleWolf)}
                    );
                    wolfVariantsPacket.send(Protocol1_20_5To1_20_3.class);
                    PacketWrapper bannerPatternsPacket = wrapper.create(
                        ClientboundConfigurationPackets1_20_5.REGISTRY_DATA
                    );
                    bannerPatternsPacket.write(Type.STRING, "minecraft:banner_pattern");
                    RegistryEntry[] patternEntries = new RegistryEntry[BannerPatterns1_20_5.keys().length];
                    String[] keys = BannerPatterns1_20_5.keys();

                    for (int i = 0; i < keys.length; i++) {
                        CompoundTag pattern = new CompoundTag();
                        String key = keys[i];
                        String resourceLocation = "minecraft:" + key;
                        pattern.putString("asset_id", key);
                        pattern.putString("translation_key", "block.minecraft.banner." + key);
                        patternEntries[i] = new RegistryEntry(resourceLocation, pattern);
                    }

                    bannerPatternsPacket.write(Type.REGISTRY_ENTRY_ARRAY, patternEntries);
                    bannerPatternsPacket.send(Protocol1_20_5To1_20_3.class);
                }
            );
        this.protocol
            .registerClientbound(
                ClientboundPackets1_20_3.JOIN_GAME,
                new PacketHandlers() {
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
                        this.handler(
                            wrapper -> {
                                String dimensionKey = wrapper.read(Type.STRING);
                                DimensionData data = EntityPacketRewriter1_20_5.this.<EntityTracker>tracker(
                                        wrapper.user()
                                    )
                                    .dimensionData(dimensionKey);
                                wrapper.write(Type.VAR_INT, data.id());
                            }
                        );
                        this.map(Type.STRING);
                        this.map(Type.LONG);
                        this.map(Type.BYTE);
                        this.map(Type.BYTE);
                        this.map(Type.BOOLEAN);
                        this.map(Type.BOOLEAN);
                        this.map(Type.OPTIONAL_GLOBAL_POSITION);
                        this.map(Type.VAR_INT);
                        this.handler(EntityPacketRewriter1_20_5.this.worldDataTrackerHandlerByKey1_20_5(3));
                        this.handler(EntityPacketRewriter1_20_5.this.playerTrackerHandler());
                        this.handler(wrapper -> {
                            AcknowledgedMessagesStorage storage = wrapper.user().get(AcknowledgedMessagesStorage.class);
                            if (storage.secureChatEnforced() != null) {
                                wrapper.write(Type.BOOLEAN, storage.isSecureChatEnforced());
                            } else {
                                wrapper.write(Type.BOOLEAN, Via.getConfig().enforceSecureChat());
                            }

                            storage.clear();
                            byte gamemode = wrapper.get(Type.BYTE, 0);
                            if (gamemode == 1) {
                                EntityPacketRewriter1_20_5.this.sendRangeAttributes(wrapper.user(), true);
                            }
                        });
                    }
                }
            );
        this.protocol.registerClientbound(ClientboundPackets1_20_3.RESPAWN, wrapper -> {
            String dimensionKey = wrapper.read(Type.STRING);
            DimensionData data = this.<EntityTracker>tracker(wrapper.user()).dimensionData(dimensionKey);
            wrapper.write(Type.VAR_INT, data.id());
            wrapper.passthrough(Type.STRING);
            this.worldDataTrackerHandlerByKey1_20_5(0).handle(wrapper);
            wrapper.passthrough(Type.LONG);
            byte gamemode = wrapper.passthrough(Type.BYTE);
            this.sendRangeAttributes(wrapper.user(), gamemode == 1);
        });
        this.protocol.registerClientbound(ClientboundPackets1_20_3.ENTITY_EFFECT, wrapper -> {
            wrapper.passthrough(Type.VAR_INT);
            wrapper.passthrough(Type.VAR_INT);
            byte amplifier = wrapper.read(Type.BYTE);
            wrapper.write(Type.VAR_INT, Integer.valueOf(amplifier));
            wrapper.passthrough(Type.VAR_INT);
            wrapper.passthrough(Type.BYTE);
            wrapper.read(Type.OPTIONAL_COMPOUND_TAG);
        });
        this.protocol.registerClientbound(ClientboundPackets1_20_3.ENTITY_PROPERTIES, wrapper -> {
            wrapper.passthrough(Type.VAR_INT);
            int size = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < size; i++) {
                String attributeIdentifier = wrapper.read(Type.STRING);
                int mappedId = Attributes1_20_5.keyToId(attributeIdentifier);
                wrapper.write(Type.VAR_INT, mappedId != -1 ? mappedId : 0);
                wrapper.passthrough(Type.DOUBLE);
                int modifierSize = wrapper.passthrough(Type.VAR_INT);

                for (int j = 0; j < modifierSize; j++) {
                    wrapper.passthrough(Type.UUID);
                    wrapper.passthrough(Type.DOUBLE);
                    wrapper.passthrough(Type.BYTE);
                }
            }
        });
        this.protocol.registerClientbound(ClientboundPackets1_20_3.GAME_EVENT, wrapper -> {
            short event = wrapper.passthrough(Type.UNSIGNED_BYTE);
            if (event == 3) {
                float value = wrapper.passthrough(Type.FLOAT);
                this.sendRangeAttributes(wrapper.user(), value == 1.0F);
            }
        });
    }

    private void updateParticleFormat(CompoundTag options, String particleType) {
        if ("block".equals(particleType)
            || "block_marker".equals(particleType)
            || "falling_dust".equals(particleType)
            || "dust_pillar".equals(particleType)) {
            this.moveTag(options, "value", "block_state");
        } else if ("item".equals(particleType)) {
            this.moveTag(options, "value", "item");
        } else if ("dust_color_transition".equals(particleType)) {
            this.moveTag(options, "fromColor", "from_color");
            this.moveTag(options, "toColor", "to_color");
        } else if ("entity_effect".equals(particleType)) {
            this.moveTag(options, "value", "color");
        }
    }

    private void moveTag(CompoundTag compoundTag, String from, String to) {
        Tag tag = compoundTag.remove(from);
        if (tag != null) {
            compoundTag.put(to, tag);
        }
    }

    private void checkSoundTag(@Nullable CompoundTag tag, String key) {
        if (tag != null) {
            String sound = tag.getString(key);
            if (sound != null && this.protocol.getMappingData().soundId(sound) == -1) {
                CompoundTag directSoundValue = new CompoundTag();
                directSoundValue.putString("sound_id", sound);
                tag.put(key, directSoundValue);
            }
        }
    }

    private void replaceNullValues(RegistryEntry[] entries) {
        RegistryEntry first = null;

        for (RegistryEntry registryEntry : entries) {
            if (registryEntry != null) {
                first = registryEntry;
                break;
            }
        }

        for (int i = 0; i < entries.length; i++) {
            if (entries[i] == null) {
                entries[i] = first.withKey(UUID.randomUUID().toString());
            }
        }
    }

    private void sendRangeAttributes(UserConnection connection, boolean creativeMode) throws Exception {
        PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_20_5.ENTITY_PROPERTIES, connection);
        wrapper.write(Type.VAR_INT, this.<EntityTracker>tracker(connection).clientEntityId());
        wrapper.write(Type.VAR_INT, 2);
        this.writeAttribute(
            wrapper, "player.block_interaction_range", 4.5, creativeMode ? CREATIVE_BLOCK_INTERACTION_RANGE : null, 0.5
        );
        this.writeAttribute(
            wrapper,
            "player.entity_interaction_range",
            3.0,
            creativeMode ? CREATIVE_ENTITY_INTERACTION_RANGE : null,
            2.0
        );
        wrapper.scheduleSend(Protocol1_20_5To1_20_3.class);
    }

    private void writeAttribute(
        PacketWrapper wrapper, String attributeId, double base, @Nullable UUID modifierId, double amount
    ) {
        wrapper.write(Type.VAR_INT, Attributes1_20_5.keyToId(attributeId));
        wrapper.write(Type.DOUBLE, base);
        if (modifierId != null) {
            wrapper.write(Type.VAR_INT, 1);
            wrapper.write(Type.UUID, modifierId);
            wrapper.write(Type.DOUBLE, amount);
            wrapper.write(Type.BYTE, (byte)0);
        } else {
            wrapper.write(Type.VAR_INT, 0);
        }
    }

    @Override
    protected void registerRewrites() {
        this.filter().mapMetaType(typeId -> {
            int id = typeId;
            if (id >= Types1_20_5.META_TYPES.particlesType.typeId()) {
                id++;
            }

            if (id >= Types1_20_5.META_TYPES.wolfVariantType.typeId()) {
                id++;
            }

            if (id >= Types1_20_5.META_TYPES.armadilloState.typeId()) {
                id++;
            }

            return Types1_20_5.META_TYPES.byId(id);
        });
        this.registerMetaTypeHandler(
            Types1_20_5.META_TYPES.itemType,
            Types1_20_5.META_TYPES.blockStateType,
            Types1_20_5.META_TYPES.optionalBlockStateType,
            Types1_20_5.META_TYPES.particleType,
            null
        );
        this.filter()
            .type(EntityTypes1_20_5.LIVINGENTITY)
            .index(10)
            .handler(
                (event, meta) -> {
                    int effectColor = meta.<Integer>value();
                    Particle particle = new Particle(
                        this.protocol.getMappingData().getParticleMappings().mappedId("entity_effect")
                    );
                    particle.add(Type.INT, effectColor);
                    meta.setTypeAndValue(Types1_20_5.META_TYPES.particlesType, new Particle[]{particle});
                }
            );
        this.filter().type(EntityTypes1_20_5.LLAMA).removeIndex(20);
        this.filter().type(EntityTypes1_20_5.AREA_EFFECT_CLOUD).handler((event, meta) -> {
            int metaIndex = event.index();
            if (metaIndex == 9) {
                Metadata particleData = event.metaAtIndex(11);
                this.addColor(particleData, meta.<Integer>value());
                event.cancel();
            } else {
                if (metaIndex > 9) {
                    event.setIndex(metaIndex - 1);
                }

                if (metaIndex == 11) {
                    Metadata colorData = event.metaAtIndex(9);
                    if (colorData != null && colorData.metaType() == Types1_20_5.META_TYPES.varIntType) {
                        this.addColor(meta, colorData.<Integer>value());
                    }
                }
            }
        });
        this.filter().type(EntityTypes1_20_5.MINECART_ABSTRACT).index(11).handler((event, meta) -> {
            int blockState = meta.<Integer>value();
            meta.setValue(this.protocol.getMappingData().getNewBlockStateId(blockState));
        });
    }

    private void addColor(@Nullable Metadata particleMeta, int color) {
        if (particleMeta != null) {
            Particle particle = particleMeta.value();
            if (particle.id() == this.protocol.getMappingData().getParticleMappings().mappedId("entity_effect")) {
                particle.<Integer>getArgument(0).setValue(color);
            }
        }
    }

    @Override
    public void rewriteParticle(UserConnection connection, Particle particle) {
        super.rewriteParticle(connection, particle);
        if (particle.id() == this.protocol.getMappingData().getParticleMappings().mappedId("entity_effect")) {
            particle.add(Type.INT, 0);
        }
    }

    @Override
    public void onMappingDataLoaded() {
        this.mapTypes();
    }

    @Override
    public EntityType typeFromId(int type) {
        return EntityTypes1_20_5.getTypeFromId(type);
    }
}
