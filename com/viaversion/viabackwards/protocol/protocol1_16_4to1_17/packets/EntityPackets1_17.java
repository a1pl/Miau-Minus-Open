package com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.packets;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.Protocol1_16_4To1_17;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_16_2;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_17;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_16;
import com.viaversion.viaversion.api.type.types.version.Types1_17;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ClientboundPackets1_17;
import com.viaversion.viaversion.util.TagUtil;

public final class EntityPackets1_17 extends EntityRewriter<ClientboundPackets1_17, Protocol1_16_4To1_17> {
    private boolean warned;

    public EntityPackets1_17(Protocol1_16_4To1_17 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        this.registerTrackerWithData(ClientboundPackets1_17.SPAWN_ENTITY, EntityTypes1_17.FALLING_BLOCK);
        this.registerSpawnTracker(ClientboundPackets1_17.SPAWN_MOB);
        this.registerTracker(ClientboundPackets1_17.SPAWN_EXPERIENCE_ORB, EntityTypes1_17.EXPERIENCE_ORB);
        this.registerTracker(ClientboundPackets1_17.SPAWN_PAINTING, EntityTypes1_17.PAINTING);
        this.registerTracker(ClientboundPackets1_17.SPAWN_PLAYER, EntityTypes1_17.PLAYER);
        this.registerMetadataRewriter(
            ClientboundPackets1_17.ENTITY_METADATA, Types1_17.METADATA_LIST, Types1_16.METADATA_LIST
        );
        this.protocol
            .registerClientbound(
                ClientboundPackets1_17.REMOVE_ENTITY, ClientboundPackets1_16_2.DESTROY_ENTITIES, wrapper -> {
                    int entityId = wrapper.read(Type.VAR_INT);
                    this.<EntityTracker>tracker(wrapper.user()).removeEntity(entityId);
                    int[] array = new int[]{entityId};
                    wrapper.write(Type.VAR_INT_ARRAY_PRIMITIVE, array);
                }
            );
        this.protocol.registerClientbound(ClientboundPackets1_17.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.INT);
                this.map(Type.BOOLEAN);
                this.map(Type.BYTE);
                this.map(Type.BYTE);
                this.map(Type.STRING_ARRAY);
                this.map(Type.NAMED_COMPOUND_TAG);
                this.map(Type.NAMED_COMPOUND_TAG);
                this.map(Type.STRING);
                this.handler(wrapper -> {
                    byte previousGamemode = wrapper.get(Type.BYTE, 1);
                    if (previousGamemode == -1) {
                        wrapper.set(Type.BYTE, 1, (byte)0);
                    }
                });
                this.handler(EntityPackets1_17.this.getTrackerHandler(EntityTypes1_17.PLAYER, Type.INT));
                this.handler(EntityPackets1_17.this.worldDataTrackerHandler(1));
                this.handler(wrapper -> {
                    CompoundTag registry = wrapper.get(Type.NAMED_COMPOUND_TAG, 0);

                    for (CompoundTag biome : TagUtil.getRegistryEntries(registry, "worldgen/biome")) {
                        CompoundTag biomeCompound = biome.getCompoundTag("element");
                        StringTag category = biomeCompound.getStringTag("category");
                        if (category.getValue().equalsIgnoreCase("underground")) {
                            category.setValue("none");
                        }
                    }

                    for (CompoundTag dimension : TagUtil.getRegistryEntries(registry, "dimension_type")) {
                        CompoundTag dimensionCompound = dimension.getCompoundTag("element");
                        EntityPackets1_17.this.reduceExtendedHeight(dimensionCompound, false);
                    }

                    EntityPackets1_17.this.reduceExtendedHeight(wrapper.get(Type.NAMED_COMPOUND_TAG, 1), true);
                });
            }
        });
        this.protocol
            .registerClientbound(
                ClientboundPackets1_17.RESPAWN,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.NAMED_COMPOUND_TAG);
                        this.map(Type.STRING);
                        this.handler(EntityPackets1_17.this.worldDataTrackerHandler(0));
                        this.handler(
                            wrapper -> EntityPackets1_17.this.reduceExtendedHeight(
                                wrapper.get(Type.NAMED_COMPOUND_TAG, 0), true
                            )
                        );
                    }
                }
            );
        this.protocol.registerClientbound(ClientboundPackets1_17.PLAYER_POSITION, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.DOUBLE);
                this.map(Type.DOUBLE);
                this.map(Type.DOUBLE);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.BYTE);
                this.map(Type.VAR_INT);
                this.handler(wrapper -> wrapper.read(Type.BOOLEAN));
            }
        });
        this.protocol.registerClientbound(ClientboundPackets1_17.ENTITY_PROPERTIES, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.handler(wrapper -> wrapper.write(Type.INT, wrapper.read(Type.VAR_INT)));
            }
        });
        this.protocol.mergePacket(ClientboundPackets1_17.COMBAT_ENTER, ClientboundPackets1_16_2.COMBAT_EVENT, 0);
        this.protocol.mergePacket(ClientboundPackets1_17.COMBAT_END, ClientboundPackets1_16_2.COMBAT_EVENT, 1);
        this.protocol.mergePacket(ClientboundPackets1_17.COMBAT_KILL, ClientboundPackets1_16_2.COMBAT_EVENT, 2);
    }

    @Override
    protected void registerRewrites() {
        this.filter().handler((event, meta) -> {
            meta.setMetaType(Types1_16.META_TYPES.byId(meta.metaType().typeId()));
            MetaType type = meta.metaType();
            if (type == Types1_16.META_TYPES.particleType) {
                Particle particle = (Particle)meta.getValue();
                if (particle.id() == 16) {
                    particle.getArguments().subList(4, 7).clear();
                } else if (particle.id() == 37) {
                    particle.setId(0);
                    particle.getArguments().clear();
                    return;
                }

                this.rewriteParticle(event.user(), particle);
            } else if (type == Types1_16.META_TYPES.poseType) {
                int pose = meta.<Integer>value();
                if (pose == 6) {
                    meta.setValue(1);
                } else if (pose > 6) {
                    meta.setValue(pose - 1);
                }
            }
        });
        this.registerMetaTypeHandler(
            Types1_16.META_TYPES.itemType,
            Types1_16.META_TYPES.blockStateType,
            null,
            null,
            Types1_16.META_TYPES.componentType,
            Types1_16.META_TYPES.optionalComponentType
        );
        this.mapTypes(EntityTypes1_17.values(), EntityTypes1_16_2.class);
        this.filter().type(EntityTypes1_17.AXOLOTL).cancel(17);
        this.filter().type(EntityTypes1_17.AXOLOTL).cancel(18);
        this.filter().type(EntityTypes1_17.AXOLOTL).cancel(19);
        this.filter().type(EntityTypes1_17.GLOW_SQUID).cancel(16);
        this.filter().type(EntityTypes1_17.GOAT).cancel(17);
        this.mapEntityTypeWithData(EntityTypes1_17.AXOLOTL, EntityTypes1_17.TROPICAL_FISH).jsonName();
        this.mapEntityTypeWithData(EntityTypes1_17.GOAT, EntityTypes1_17.SHEEP).jsonName();
        this.mapEntityTypeWithData(EntityTypes1_17.GLOW_SQUID, EntityTypes1_17.SQUID).jsonName();
        this.mapEntityTypeWithData(EntityTypes1_17.GLOW_ITEM_FRAME, EntityTypes1_17.ITEM_FRAME);
        this.filter().type(EntityTypes1_17.SHULKER).addIndex(17);
        this.filter().removeIndex(7);
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return EntityTypes1_17.getTypeFromId(typeId);
    }

    private void reduceExtendedHeight(CompoundTag tag, boolean warn) {
        NumberTag minY = tag.getNumberTag("min_y");
        NumberTag height = tag.getNumberTag("height");
        NumberTag logicalHeight = tag.getNumberTag("logical_height");
        if (minY.asInt() != 0 || height.asInt() > 256 || logicalHeight.asInt() > 256) {
            if (warn && !this.warned) {
                ViaBackwards.getPlatform()
                    .getLogger()
                    .warning(
                        "Increased world height is NOT SUPPORTED for 1.16 players and below. They will see a void below y 0 and above 256"
                    );
                this.warned = true;
            }

            tag.putInt("height", Math.min(256, height.asInt()));
            tag.putInt("logical_height", Math.min(256, logicalHeight.asInt()));
        }
    }
}
