package com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.metadata;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.viaversion.viarewind.ViaRewind;
import com.viaversion.viarewind.api.rewriter.VREntityRewriter;
import com.viaversion.viarewind.api.type.Types1_7_6_10;
import com.viaversion.viarewind.api.type.metadata.MetaType1_7_6_10;
import com.viaversion.viarewind.protocol.protocol1_7_2_5to1_7_6_10.ClientboundPackets1_7_2_5;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.Protocol1_7_6_10To1_8;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.data.VirtualHologramEntity;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.storage.EntityTracker1_8;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.storage.GameProfileStorage;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.storage.Scoreboard;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_10;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_8;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_8;
import com.viaversion.viaversion.protocols.protocol1_8.ClientboundPackets1_8;
import com.viaversion.viaversion.rewriter.meta.MetaHandlerEvent;
import com.viaversion.viaversion.util.IdAndData;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class MetadataRewriter1_7_6_10To1_8 extends VREntityRewriter<ClientboundPackets1_8, Protocol1_7_6_10To1_8> {
    public MetadataRewriter1_7_6_10To1_8(Protocol1_7_6_10To1_8 protocol) {
        super(protocol, MetaType1_7_6_10.String, MetaType1_7_6_10.Byte);
    }

    @Override
    protected void registerPackets() {
        this.protocol.registerClientbound(ClientboundPackets1_8.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.INT);
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.BYTE);
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.STRING);
                this.read(Type.BOOLEAN);
                this.handler(MetadataRewriter1_7_6_10To1_8.this.playerTrackerHandler());
                this.handler(wrapper -> {
                    int entityId = wrapper.get(Type.INT, 0);
                    if (ViaRewind.getConfig().isReplaceAdventureMode() && wrapper.get(Type.UNSIGNED_BYTE, 0) == 2) {
                        wrapper.set(Type.UNSIGNED_BYTE, 0, (short)0);
                    }

                    EntityTracker1_8 tracker = wrapper.user().getEntityTracker(Protocol1_7_6_10To1_8.class);
                    tracker.addPlayer(entityId, wrapper.user().getProtocolInfo().getUuid());
                    tracker.setClientEntityGameMode(wrapper.get(Type.UNSIGNED_BYTE, 0));
                    wrapper.user().get(ClientWorld.class).setEnvironment(wrapper.get(Type.BYTE, 0));
                    wrapper.user().put(new Scoreboard(wrapper.user()));
                });
            }
        });
        this.protocol
            .registerClientbound(
                ClientboundPackets1_8.DESTROY_ENTITIES,
                wrapper -> {
                    int[] entities = wrapper.read(Type.VAR_INT_ARRAY_PRIMITIVE);
                    this.untrackEntities(wrapper.user(), entities);
                    wrapper.cancel();

                    for (List<Integer> part : Lists.partition(Ints.asList(entities), 127)) {
                        PacketWrapper destroy = PacketWrapper.create(
                            ClientboundPackets1_7_2_5.DESTROY_ENTITIES, wrapper.user()
                        );
                        destroy.write(Types1_7_6_10.BYTE_INT_ARRAY, part.stream().mapToInt(Integer::intValue).toArray());
                        destroy.scheduleSend(Protocol1_7_6_10To1_8.class);
                    }
                }
            );
        this.protocol.registerClientbound(ClientboundPackets1_8.ENTITY_METADATA, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT, Type.INT);
                this.map(Types1_8.METADATA_LIST, Types1_7_6_10.METADATA_LIST);
                this.handler(wrapper -> {
                    int entityId = wrapper.get(Type.INT, 0);
                    List<Metadata> metadata = wrapper.get(Types1_7_6_10.METADATA_LIST, 0);
                    MetadataRewriter1_7_6_10To1_8.this.handleMetadata(entityId, metadata, wrapper.user());
                });
            }
        });
        this.protocol
            .registerClientbound(
                ClientboundPackets1_8.SPAWN_ENTITY,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.VAR_INT);
                        this.map(Type.BYTE);
                        this.map(Type.INT);
                        this.map(Type.INT);
                        this.map(Type.INT);
                        this.map(Type.BYTE);
                        this.map(Type.BYTE);
                        this.map(Type.INT);
                        this.handler(MetadataRewriter1_7_6_10To1_8.this.getObjectTrackerHandler());
                        this.handler(
                            MetadataRewriter1_7_6_10To1_8.this.getObjectRewriter(
                                id -> EntityTypes1_10.ObjectType.findById(id).orElse(null)
                            )
                        );
                        this.handler(
                            wrapper -> {
                                int entityId = wrapper.get(Type.VAR_INT, 0);
                                EntityTypes1_10.EntityType type = EntityTypes1_10.getTypeFromId(
                                    wrapper.get(Type.BYTE, 0), true
                                );
                                int x = wrapper.get(Type.INT, 0);
                                int y = wrapper.get(Type.INT, 1);
                                int z = wrapper.get(Type.INT, 2);
                                byte pitch = wrapper.get(Type.BYTE, 1);
                                byte yaw = wrapper.get(Type.BYTE, 2);
                                int data = wrapper.get(Type.INT, 3);
                                if (type == EntityTypes1_10.ObjectType.ITEM_FRAME.getType()) {
                                    switch (yaw) {
                                        case -128:
                                            z += 32;
                                            yaw = 0;
                                            break;
                                        case -64:
                                            x -= 32;
                                            yaw = -64;
                                            break;
                                        case 0:
                                            z -= 32;
                                            yaw = -128;
                                            break;
                                        case 64:
                                            x += 32;
                                            yaw = 64;
                                    }
                                } else if (type == EntityTypes1_10.ObjectType.ARMOR_STAND.getType()) {
                                    wrapper.cancel();
                                    EntityTracker1_8 tracker = MetadataRewriter1_7_6_10To1_8.this.tracker(
                                        wrapper.user()
                                    );
                                    VirtualHologramEntity hologram = tracker.getHolograms().get(entityId);
                                    hologram.setPosition(x / 32.0, y / 32.0, z / 32.0);
                                    hologram.setRotation(yaw * 360.0F / 256.0F, pitch * 360.0F / 256.0F);
                                    hologram.setHeadYaw(yaw * 360.0F / 256.0F);
                                } else if (type != null && type.isOrHasParent(EntityTypes1_10.EntityType.FALLING_BLOCK)
                                    )
                                 {
                                    int blockId = data & 4095;
                                    int blockData = data >> 12 & 15;
                                    IdAndData replace = MetadataRewriter1_7_6_10To1_8.this.protocol
                                        .getItemRewriter()
                                        .handleBlock(blockId, blockData);
                                    if (replace != null) {
                                        blockId = replace.getId();
                                        blockData = replace.getData();
                                    }

                                    wrapper.set(Type.INT, 3, data = blockId | blockData << 16);
                                }

                                wrapper.set(Type.INT, 0, x);
                                wrapper.set(Type.INT, 1, y);
                                wrapper.set(Type.INT, 2, z);
                                wrapper.set(Type.BYTE, 2, yaw);
                                if (data > 0) {
                                    wrapper.passthrough(Type.SHORT);
                                    wrapper.passthrough(Type.SHORT);
                                    wrapper.passthrough(Type.SHORT);
                                }
                            }
                        );
                    }
                }
            );
        this.registerTracker(ClientboundPackets1_8.SPAWN_EXPERIENCE_ORB, EntityTypes1_10.EntityType.EXPERIENCE_ORB);
        this.registerTracker(ClientboundPackets1_8.SPAWN_GLOBAL_ENTITY, EntityTypes1_10.EntityType.LIGHTNING);
        this.protocol
            .registerClientbound(
                ClientboundPackets1_8.SPAWN_PAINTING,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.VAR_INT);
                        this.map(Type.STRING);
                        this.map(Type.POSITION1_8, Types1_7_6_10.INT_POSITION);
                        this.map(Type.UNSIGNED_BYTE, Type.INT);
                        this.handler(
                            wrapper -> {
                                int entityId = wrapper.get(Type.VAR_INT, 0);
                                Position position = wrapper.get(Types1_7_6_10.INT_POSITION, 0);
                                int rotation = wrapper.get(Type.INT, 0);
                                int modX = 0;
                                int modZ = 0;
                                switch (rotation) {
                                    case 0:
                                        modZ = -1;
                                        break;
                                    case 1:
                                        modX = 1;
                                        break;
                                    case 2:
                                        modZ = 1;
                                        break;
                                    case 3:
                                        modX = -1;
                                }

                                wrapper.set(
                                    Types1_7_6_10.INT_POSITION,
                                    0,
                                    new Position(position.x() + modX, position.y(), position.z() + modZ)
                                );
                                MetadataRewriter1_7_6_10To1_8.this.addTrackedEntity(
                                    wrapper, entityId, EntityTypes1_10.EntityType.PAINTING
                                );
                            }
                        );
                    }
                }
            );
        this.protocol
            .registerClientbound(
                ClientboundPackets1_8.SPAWN_MOB,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.VAR_INT);
                        this.map(Type.UNSIGNED_BYTE);
                        this.map(Type.INT);
                        this.map(Type.INT);
                        this.map(Type.INT);
                        this.map(Type.BYTE);
                        this.map(Type.BYTE);
                        this.map(Type.BYTE);
                        this.map(Type.SHORT);
                        this.map(Type.SHORT);
                        this.map(Type.SHORT);
                        this.map(Types1_8.METADATA_LIST, Types1_7_6_10.METADATA_LIST);
                        this.handler(MetadataRewriter1_7_6_10To1_8.this.getTrackerHandler(Type.UNSIGNED_BYTE, 0));
                        this.handler(
                            MetadataRewriter1_7_6_10To1_8.this.getMobSpawnRewriter(Types1_7_6_10.METADATA_LIST)
                        );
                        this.handler(
                            wrapper -> {
                                short typeId = wrapper.get(Type.UNSIGNED_BYTE, 0);
                                EntityTypes1_10.EntityType type = EntityTypes1_10.getTypeFromId(typeId, false);
                                if (type == EntityTypes1_10.EntityType.ARMOR_STAND) {
                                    wrapper.cancel();
                                    int entityId = wrapper.get(Type.VAR_INT, 0);
                                    int x = wrapper.get(Type.INT, 0);
                                    int y = wrapper.get(Type.INT, 1);
                                    int z = wrapper.get(Type.INT, 2);
                                    byte pitch = wrapper.get(Type.BYTE, 1);
                                    byte yaw = wrapper.get(Type.BYTE, 0);
                                    byte headYaw = wrapper.get(Type.BYTE, 2);
                                    EntityTracker1_8 tracker = wrapper.user()
                                        .getEntityTracker(Protocol1_7_6_10To1_8.class);
                                    VirtualHologramEntity hologram = tracker.getHolograms().get(entityId);
                                    hologram.setPosition(x / 32.0, y / 32.0, z / 32.0);
                                    hologram.setRotation(yaw * 360.0F / 256.0F, pitch * 360.0F / 256.0F);
                                    hologram.setHeadYaw(headYaw * 360.0F / 256.0F);
                                    hologram.syncState(
                                        MetadataRewriter1_7_6_10To1_8.this.protocol().getEntityRewriter(),
                                        wrapper.get(Types1_7_6_10.METADATA_LIST, 0)
                                    );
                                }
                            }
                        );
                    }
                }
            );
        this.protocol
            .registerClientbound(
                ClientboundPackets1_8.SPAWN_PLAYER,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.VAR_INT);
                        this.handler(
                            wrapper -> {
                                UUID uuid = wrapper.read(Type.UUID);
                                wrapper.write(Type.STRING, uuid.toString());
                                GameProfileStorage gameProfileStorage = wrapper.user().get(GameProfileStorage.class);
                                GameProfileStorage.GameProfile gameProfile = gameProfileStorage.get(uuid);
                                if (gameProfile == null) {
                                    wrapper.write(Type.STRING, "");
                                    wrapper.write(Type.VAR_INT, 0);
                                } else {
                                    wrapper.write(
                                        Type.STRING,
                                        gameProfile.name.length() > 16
                                            ? gameProfile.name.substring(0, 16)
                                            : gameProfile.name
                                    );
                                    wrapper.write(Type.VAR_INT, gameProfile.properties.size());

                                    for (GameProfileStorage.Property property : gameProfile.properties) {
                                        wrapper.write(Type.STRING, property.name);
                                        wrapper.write(Type.STRING, property.value);
                                        wrapper.write(Type.STRING, property.signature == null ? "" : property.signature);
                                    }
                                }

                                int entityId = wrapper.get(Type.VAR_INT, 0);
                                EntityTracker1_8 tracker = wrapper.user().getEntityTracker(Protocol1_7_6_10To1_8.class);
                                if (gameProfile != null && gameProfile.gamemode == 3) {
                                    for (short i = 0; i < 5; i++) {
                                        PacketWrapper entityEquipment = PacketWrapper.create(
                                            ClientboundPackets1_7_2_5.ENTITY_EQUIPMENT, wrapper.user()
                                        );
                                        entityEquipment.write(Type.INT, entityId);
                                        entityEquipment.write(Type.SHORT, i);
                                        entityEquipment.write(
                                            Types1_7_6_10.COMPRESSED_NBT_ITEM, i == 4 ? gameProfile.getSkull() : null
                                        );
                                        entityEquipment.scheduleSend(Protocol1_7_6_10To1_8.class);
                                    }
                                }

                                tracker.addPlayer(entityId, uuid);
                            }
                        );
                        this.map(Type.INT);
                        this.map(Type.INT);
                        this.map(Type.INT);
                        this.map(Type.BYTE);
                        this.map(Type.BYTE);
                        this.map(Type.SHORT);
                        this.map(Types1_8.METADATA_LIST, Types1_7_6_10.METADATA_LIST);
                        this.handler(
                            MetadataRewriter1_7_6_10To1_8.this.getTrackerAndMetaHandler(
                                Types1_7_6_10.METADATA_LIST, EntityTypes1_10.EntityType.PLAYER
                            )
                        );
                    }
                }
            );
    }

    @Override
    protected void registerRewrites() {
        this.mapEntityTypeWithData(EntityTypes1_10.EntityType.GUARDIAN, EntityTypes1_10.EntityType.SQUID).plainName();
        this.mapEntityTypeWithData(EntityTypes1_10.EntityType.ENDERMITE, EntityTypes1_10.EntityType.SQUID).plainName();
        this.mapEntityTypeWithData(EntityTypes1_10.EntityType.RABBIT, EntityTypes1_10.EntityType.CHICKEN).plainName();
        this.filter()
            .handler(
                (event, meta) -> {
                    try {
                        this.handleMetadata(event, meta);
                    } catch (Exception e) {
                        if (Via.getManager().isDebug()) {
                            ViaRewind.getPlatform()
                                .getLogger()
                                .log(Level.SEVERE, "An error occurred with entity metadata: " + meta, e);
                        }

                        event.cancel();
                    }
                }
            );
    }

    public void handleMetadata(MetaHandlerEvent event, Metadata metadata) throws Exception {
        if (event.entityType() == EntityTypes1_10.EntityType.ARMOR_STAND) {
            EntityTracker1_8 tracker = this.tracker(event.user());
            tracker.getHolograms().get(event.entityId()).syncState(this, event.metadataList());
            event.cancel();
        } else {
            MetaIndex metaIndex = MetaIndex.searchIndex(event.entityType(), metadata.id());
            if (metaIndex == null) {
                event.cancel();
            } else if (metaIndex.getOldType() == null) {
                event.cancel();
            } else {
                Object value = metadata.getValue();
                metadata.setTypeAndValue(metaIndex.getNewType(), value);
                metadata.setMetaTypeUnsafe(metaIndex.getOldType());
                metadata.setId(metaIndex.getIndex());
                switch (metaIndex.getOldType()) {
                    case Int:
                        if (metaIndex.getNewType() == MetaType1_8.Byte) {
                            metadata.setValue(((Byte)value).intValue());
                            if (metaIndex == MetaIndex.ENTITY_AGEABLE_AGE && (Integer)metadata.getValue() < 0) {
                                metadata.setValue(-25000);
                            }
                        }

                        if (metaIndex.getNewType() == MetaType1_8.Short) {
                            metadata.setValue(((Short)value).intValue());
                        }

                        if (metaIndex.getNewType() == MetaType1_8.Int) {
                            metadata.setValue(value);
                        }
                        break;
                    case Byte:
                        if (metaIndex.getNewType() == MetaType1_8.Int) {
                            metadata.setValue(((Integer)value).byteValue());
                        }

                        if (metaIndex.getNewType() == MetaType1_8.Byte) {
                            if (metaIndex == MetaIndex.ITEM_FRAME_ROTATION) {
                                metadata.setValue(Integer.valueOf((Byte)value % 4).byteValue());
                            } else {
                                metadata.setValue(value);
                            }
                        }

                        if (metaIndex == MetaIndex.HUMAN_SKIN_FLAGS) {
                            byte flags = (Byte)value;
                            boolean cape = (flags & 1) != 0;
                            flags = (byte)(cape ? 0 : 2);
                            metadata.setValue(flags);
                        }
                        break;
                    case Slot:
                        metadata.setValue(this.protocol.getItemRewriter().handleItemToClient(event.user(), (Item)value));
                        break;
                    case Float:
                    case String:
                    case Short:
                    case Position:
                        metadata.setValue(value);
                        break;
                    default:
                        event.cancel();
                }
            }
        }
    }

    public EntityTypes1_10.EntityType typeFromId(int type) {
        return EntityTypes1_10.getTypeFromId(type, false);
    }

    public EntityTypes1_10.EntityType objectTypeFromId(int type) {
        return EntityTypes1_10.getTypeFromId(type, true);
    }
}
