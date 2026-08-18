package com.viaversion.viarewind.protocol.protocol1_8to1_9.metadata;

import com.viaversion.viarewind.ViaRewind;
import com.viaversion.viarewind.api.rewriter.VREntityRewriter;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.Protocol1_8To1_9;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.EntityTracker1_9;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.EulerAngle;
import com.viaversion.viaversion.api.minecraft.Vector;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_10;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_8;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_8;
import com.viaversion.viaversion.api.type.types.version.Types1_9;
import com.viaversion.viaversion.protocols.protocol1_8.ClientboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.metadata.MetaIndex;
import com.viaversion.viaversion.rewriter.meta.MetaHandlerEvent;
import com.viaversion.viaversion.util.IdAndData;
import java.util.UUID;
import java.util.logging.Level;

public class MetadataRewriter1_8To1_9 extends VREntityRewriter<ClientboundPackets1_9, Protocol1_8To1_9> {
    private static final byte HAND_ACTIVE_BIT = 0;
    private static final byte STATUS_USE_BIT = 4;

    public MetadataRewriter1_8To1_9(Protocol1_8To1_9 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        this.registerJoinGame1_8(ClientboundPackets1_9.JOIN_GAME);
        this.registerRemoveEntities(ClientboundPackets1_9.DESTROY_ENTITIES);
        this.registerMetadataRewriter(
            ClientboundPackets1_9.ENTITY_METADATA, Types1_9.METADATA_LIST, Types1_8.METADATA_LIST
        );
        this.protocol
            .registerClientbound(
                ClientboundPackets1_9.SPAWN_ENTITY,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.VAR_INT);
                        this.read(Type.UUID);
                        this.map(Type.BYTE);
                        this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                        this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                        this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                        this.map(Type.BYTE);
                        this.map(Type.BYTE);
                        this.map(Type.INT);
                        this.handler(MetadataRewriter1_8To1_9.this.getObjectTrackerHandler());
                        this.handler(
                            MetadataRewriter1_8To1_9.this.getObjectRewriter(
                                id -> EntityTypes1_10.ObjectType.findById(id).orElse(null)
                            )
                        );
                        this.handler(
                            wrapper -> {
                                int entityId = wrapper.get(Type.VAR_INT, 0);
                                int entityType = wrapper.get(Type.BYTE, 0);
                                EntityTypes1_10.EntityType type = EntityTypes1_10.getTypeFromId(entityType, true);
                                if (type != EntityTypes1_10.EntityType.AREA_EFFECT_CLOUD
                                    && type != EntityTypes1_10.EntityType.SPECTRAL_ARROW
                                    && type != EntityTypes1_10.EntityType.DRAGON_FIREBALL) {
                                    if (type.is(EntityTypes1_10.EntityType.BOAT)) {
                                        byte yaw = wrapper.get(Type.BYTE, 1);
                                        yaw = (byte)(yaw - 64);
                                        wrapper.set(Type.BYTE, 1, yaw);
                                        int y = wrapper.get(Type.INT, 1);
                                        y += 10;
                                        wrapper.set(Type.INT, 1, y);
                                    }

                                    int data = wrapper.get(Type.INT, 3);
                                    if (type.isOrHasParent(EntityTypes1_10.EntityType.ARROW) && data != 0) {
                                        wrapper.set(Type.INT, 3, --data);
                                    }

                                    if (type.is(EntityTypes1_10.EntityType.FALLING_BLOCK)) {
                                        int blockId = data & 4095;
                                        int blockData = data >> 12 & 15;
                                        IdAndData replace = MetadataRewriter1_8To1_9.this.protocol
                                            .getItemRewriter()
                                            .handleBlock(blockId, blockData);
                                        if (replace != null) {
                                            wrapper.set(Type.INT, 3, replace.getId() | replace.getData() << 12);
                                        }
                                    }

                                    if (data > 0) {
                                        wrapper.passthrough(Type.SHORT);
                                        wrapper.passthrough(Type.SHORT);
                                        wrapper.passthrough(Type.SHORT);
                                    } else {
                                        short velocityX = wrapper.read(Type.SHORT);
                                        short velocityY = wrapper.read(Type.SHORT);
                                        short velocityZ = wrapper.read(Type.SHORT);
                                        PacketWrapper velocityPacket = PacketWrapper.create(
                                            ClientboundPackets1_8.ENTITY_VELOCITY, wrapper.user()
                                        );
                                        velocityPacket.write(Type.VAR_INT, entityId);
                                        velocityPacket.write(Type.SHORT, velocityX);
                                        velocityPacket.write(Type.SHORT, velocityY);
                                        velocityPacket.write(Type.SHORT, velocityZ);
                                        velocityPacket.scheduleSend(Protocol1_8To1_9.class);
                                    }
                                } else {
                                    wrapper.cancel();
                                }
                            }
                        );
                    }
                }
            );
        this.protocol
            .registerClientbound(
                ClientboundPackets1_9.SPAWN_EXPERIENCE_ORB,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.VAR_INT);
                        this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                        this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                        this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                        this.map(Type.SHORT);
                        this.handler(
                            wrapper -> {
                                int entityId = wrapper.get(Type.VAR_INT, 0);
                                wrapper.user()
                                    .<EntityTracker>getEntityTracker(Protocol1_8To1_9.class)
                                    .addEntity(entityId, EntityTypes1_10.EntityType.EXPERIENCE_ORB);
                            }
                        );
                    }
                }
            );
        this.protocol
            .registerClientbound(
                ClientboundPackets1_9.SPAWN_GLOBAL_ENTITY,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.VAR_INT);
                        this.map(Type.BYTE);
                        this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                        this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                        this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                        this.handler(
                            wrapper -> {
                                int entityId = wrapper.get(Type.VAR_INT, 0);
                                wrapper.user()
                                    .<EntityTracker>getEntityTracker(Protocol1_8To1_9.class)
                                    .addEntity(entityId, EntityTypes1_10.EntityType.LIGHTNING);
                            }
                        );
                    }
                }
            );
        this.protocol.registerClientbound(ClientboundPackets1_9.SPAWN_MOB, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.read(Type.UUID);
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                this.map(Type.BYTE);
                this.map(Type.BYTE);
                this.map(Type.BYTE);
                this.map(Type.SHORT);
                this.map(Type.SHORT);
                this.map(Type.SHORT);
                this.map(Types1_9.METADATA_LIST, Types1_8.METADATA_LIST);
                this.handler(MetadataRewriter1_8To1_9.this.getTrackerHandler(Type.UNSIGNED_BYTE, 0));
                this.handler(MetadataRewriter1_8To1_9.this.getMobSpawnRewriter(Types1_8.METADATA_LIST));
            }
        });
        this.protocol
            .registerClientbound(
                ClientboundPackets1_9.SPAWN_PAINTING,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.VAR_INT);
                        this.read(Type.UUID);
                        this.map(Type.STRING);
                        this.map(Type.POSITION1_8);
                        this.map(Type.BYTE, Type.UNSIGNED_BYTE);
                        this.handler(
                            wrapper -> {
                                int entityId = wrapper.get(Type.VAR_INT, 0);
                                wrapper.user()
                                    .<EntityTracker>getEntityTracker(Protocol1_8To1_9.class)
                                    .addEntity(entityId, EntityTypes1_10.EntityType.PAINTING);
                            }
                        );
                    }
                }
            );
        this.protocol
            .registerClientbound(
                ClientboundPackets1_9.SPAWN_PLAYER,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.VAR_INT);
                        this.map(Type.UUID);
                        this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                        this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                        this.map(Type.DOUBLE, Protocol1_8To1_9.DOUBLE_TO_INT_TIMES_32);
                        this.map(Type.BYTE);
                        this.map(Type.BYTE);
                        this.create(Type.SHORT, (short)0);
                        this.map(Types1_9.METADATA_LIST, Types1_8.METADATA_LIST);
                        this.handler(
                            MetadataRewriter1_8To1_9.this.getTrackerAndMetaHandler(
                                Types1_8.METADATA_LIST, EntityTypes1_10.EntityType.PLAYER
                            )
                        );
                    }
                }
            );
    }

    @Override
    protected void registerRewrites() {
        this.mapEntityTypeWithData(EntityTypes1_10.EntityType.SHULKER, EntityTypes1_10.EntityType.MAGMA_CUBE)
            .plainName();
        this.mapEntityTypeWithData(EntityTypes1_10.EntityType.SHULKER_BULLET, EntityTypes1_10.EntityType.WITCH)
            .plainName();
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

    private void handleMetadata(MetaHandlerEvent event, Metadata metadata) {
        EntityTracker1_9 tracker = this.tracker(event.user());
        if (metadata.id() == MetaIndex.ENTITY_STATUS.getIndex()) {
            tracker.getStatus().put(event.entityId(), metadata.<Byte>value().byteValue());
        }

        MetaIndex metaIndex = MetaIndex1_8to1_9.searchIndex(event.entityType(), metadata.id());
        if (metaIndex == null) {
            event.cancel();
        } else if (metaIndex.getOldType() != null && metaIndex.getNewType() != null) {
            metadata.setId(metaIndex.getIndex());
            metadata.setMetaTypeUnsafe(metaIndex.getOldType());
            Object value = metadata.getValue();
            switch (metaIndex.getNewType()) {
                case Byte:
                    if (metaIndex.getOldType() == MetaType1_8.Byte) {
                        metadata.setValue(value);
                    }

                    if (metaIndex.getOldType() == MetaType1_8.Int) {
                        metadata.setValue(((Byte)value).intValue());
                    }
                    break;
                case OptUUID:
                    if (metaIndex.getOldType() != MetaType1_8.String) {
                        event.cancel();
                    } else {
                        UUID owner = (UUID)value;
                        metadata.setValue(owner != null ? owner.toString() : "");
                    }
                    break;
                case BlockID:
                    event.cancel();
                    event.createExtraMeta(
                        new Metadata(metaIndex.getIndex(), MetaType1_8.Short, ((Integer)value).shortValue())
                    );
                    break;
                case VarInt:
                    if (metaIndex.getOldType() == MetaType1_8.Byte) {
                        metadata.setValue(((Integer)value).byteValue());
                    }

                    if (metaIndex.getOldType() == MetaType1_8.Short) {
                        metadata.setValue(((Integer)value).shortValue());
                    }

                    if (metaIndex.getOldType() == MetaType1_8.Int) {
                        metadata.setValue(value);
                    }
                    break;
                case Float:
                case String:
                case Chat:
                    metadata.setValue(value);
                    break;
                case Boolean:
                    boolean bool = (Boolean)value;
                    if (metaIndex == MetaIndex.AGEABLE_CREATURE_AGE) {
                        metadata.setValue((byte)(bool ? -1 : 0));
                    } else {
                        metadata.setValue((byte)(bool ? 1 : 0));
                    }
                    break;
                case Slot:
                    metadata.setValue(this.protocol.getItemRewriter().handleItemToClient(event.user(), (Item)value));
                    break;
                case Position:
                    Vector vector = (Vector)value;
                    metadata.setValue(vector);
                    break;
                case Vector3F:
                    EulerAngle angle = (EulerAngle)value;
                    metadata.setValue(angle);
                    break;
                default:
                    event.cancel();
            }
        } else {
            if (metaIndex == MetaIndex.PLAYER_HAND) {
                byte status = (byte)tracker.getStatus().getOrDefault(event.entityId(), 0);
                if ((metadata.<Byte>value() & 1) != 0) {
                    status = (byte)(status | 16);
                } else {
                    status = (byte)(status & -17);
                }

                event.createExtraMeta(new Metadata(MetaIndex.ENTITY_STATUS.getIndex(), MetaType1_8.Byte, status));
            }

            event.cancel();
        }
    }

    public EntityTypes1_10.EntityType typeFromId(int type) {
        return EntityTypes1_10.getTypeFromId(type, false);
    }

    public EntityTypes1_10.EntityType objectTypeFromId(int type) {
        return EntityTypes1_10.getTypeFromId(type, true);
    }
}
