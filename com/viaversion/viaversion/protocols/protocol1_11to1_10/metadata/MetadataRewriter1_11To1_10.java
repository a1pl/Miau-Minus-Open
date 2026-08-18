package com.viaversion.viaversion.protocols.protocol1_11to1_10.metadata;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_11;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_9;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_11to1_10.Protocol1_11To1_10;
import com.viaversion.viaversion.protocols.protocol1_11to1_10.rewriter.EntityIdRewriter;
import com.viaversion.viaversion.protocols.protocol1_11to1_10.storage.EntityTracker1_11;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import com.viaversion.viaversion.rewriter.EntityRewriter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class MetadataRewriter1_11To1_10 extends EntityRewriter<ClientboundPackets1_9_3, Protocol1_11To1_10> {
    public MetadataRewriter1_11To1_10(Protocol1_11To1_10 protocol) {
        super(protocol);
    }

    @Override
    protected void registerRewrites() {
        this.filter().handler((event, meta) -> {
            if (meta.getValue() instanceof DataItem) {
                EntityIdRewriter.toClientItem(meta.value());
            }
        });
        this.filter().type(EntityTypes1_11.EntityType.GUARDIAN).index(12).handler((event, meta) -> {
            boolean value = ((Byte)meta.getValue() & 2) == 2;
            meta.setTypeAndValue(MetaType1_9.Boolean, value);
        });
        this.filter().type(EntityTypes1_11.EntityType.ABSTRACT_SKELETON).removeIndex(12);
        this.filter()
            .type(EntityTypes1_11.EntityType.ZOMBIE)
            .handler(
                (event, meta) -> {
                    if ((
                            event.entityType() == EntityTypes1_11.EntityType.ZOMBIE
                                || event.entityType() == EntityTypes1_11.EntityType.HUSK
                        )
                        && meta.id() == 14) {
                        event.cancel();
                    } else if (meta.id() == 15) {
                        meta.setId(14);
                    }
                }
            );
        this.filter()
            .type(EntityTypes1_11.EntityType.ABSTRACT_HORSE)
            .handler(
                (event, metadata) -> {
                    EntityType type = event.entityType();
                    int id = metadata.id();
                    if (id == 14) {
                        event.cancel();
                    } else {
                        if (id == 16) {
                            metadata.setId(14);
                        } else if (id == 17) {
                            metadata.setId(16);
                        }

                        if ((type.is(EntityTypes1_11.EntityType.HORSE) || metadata.id() != 15) && metadata.id() != 16) {
                            if ((type == EntityTypes1_11.EntityType.DONKEY || type == EntityTypes1_11.EntityType.MULE)
                                && metadata.id() == 13) {
                                if (((Byte)metadata.getValue() & 8) == 8) {
                                    event.createExtraMeta(new Metadata(15, MetaType1_9.Boolean, true));
                                } else {
                                    event.createExtraMeta(new Metadata(15, MetaType1_9.Boolean, false));
                                }
                            }
                        } else {
                            event.cancel();
                        }
                    }
                }
            );
        this.filter()
            .type(EntityTypes1_11.EntityType.ARMOR_STAND)
            .index(0)
            .handler(
                (event, meta) -> {
                    if (Via.getConfig().isHologramPatch()) {
                        Metadata flags = event.metaAtIndex(11);
                        Metadata customName = event.metaAtIndex(2);
                        Metadata customNameVisible = event.metaAtIndex(3);
                        if (flags != null && customName != null && customNameVisible != null) {
                            byte data = meta.<Byte>value();
                            if ((data & 32) == 32
                                && ((Byte)flags.getValue() & 1) == 1
                                && !((String)customName.getValue()).isEmpty()
                                && (Boolean)customNameVisible.getValue()) {
                                EntityTracker1_11 tracker = this.tracker(event.user());
                                int entityId = event.entityId();
                                if (tracker.addHologram(entityId)) {
                                    try {
                                        PacketWrapper wrapper = PacketWrapper.create(
                                            ClientboundPackets1_9_3.ENTITY_POSITION, null, event.user()
                                        );
                                        wrapper.write(Type.VAR_INT, entityId);
                                        wrapper.write(Type.SHORT, (short)0);
                                        wrapper.write(
                                            Type.SHORT, (short)(128.0 * (-Via.getConfig().getHologramYOffset() * 32.0))
                                        );
                                        wrapper.write(Type.SHORT, (short)0);
                                        wrapper.write(Type.BOOLEAN, true);
                                        wrapper.send(Protocol1_11To1_10.class);
                                    } catch (Exception e) {
                                        Via.getPlatform()
                                            .getLogger()
                                            .log(Level.WARNING, "Failed to update hologram position", e);
                                    }
                                }
                            }
                        }
                    }
                }
            );
    }

    @Override
    public EntityType typeFromId(int type) {
        return EntityTypes1_11.getTypeFromId(type, false);
    }

    @Override
    public EntityType objectTypeFromId(int type) {
        return EntityTypes1_11.getTypeFromId(type, true);
    }

    public static EntityTypes1_11.EntityType rewriteEntityType(int numType, List<Metadata> metadata) {
        Optional<EntityTypes1_11.EntityType> optType = EntityTypes1_11.EntityType.findById(numType);
        if (!optType.isPresent()) {
            Via.getManager()
                .getPlatform()
                .getLogger()
                .severe("Error: could not find Entity type " + numType + " with metadata: " + metadata);
            return null;
        }

        EntityTypes1_11.EntityType type = optType.get();

        try {
            if (type.is(EntityTypes1_11.EntityType.GUARDIAN)) {
                Optional<Metadata> options = getById(metadata, 12);
                if (options.isPresent() && ((Byte)options.get().getValue() & 4) == 4) {
                    return EntityTypes1_11.EntityType.ELDER_GUARDIAN;
                }
            }

            if (type.is(EntityTypes1_11.EntityType.SKELETON)) {
                Optional<Metadata> options = getById(metadata, 12);
                if (options.isPresent()) {
                    if ((Integer)options.get().getValue() == 1) {
                        return EntityTypes1_11.EntityType.WITHER_SKELETON;
                    }

                    if ((Integer)options.get().getValue() == 2) {
                        return EntityTypes1_11.EntityType.STRAY;
                    }
                }
            }

            if (type.is(EntityTypes1_11.EntityType.ZOMBIE)) {
                Optional<Metadata> options = getById(metadata, 13);
                if (options.isPresent()) {
                    int value = (Integer)options.get().getValue();
                    if (value > 0 && value < 6) {
                        metadata.add(new Metadata(16, MetaType1_9.VarInt, value - 1));
                        return EntityTypes1_11.EntityType.ZOMBIE_VILLAGER;
                    }

                    if (value == 6) {
                        return EntityTypes1_11.EntityType.HUSK;
                    }
                }
            }

            if (type.is(EntityTypes1_11.EntityType.HORSE)) {
                Optional<Metadata> options = getById(metadata, 14);
                if (options.isPresent()) {
                    if ((Integer)options.get().getValue() == 0) {
                        return EntityTypes1_11.EntityType.HORSE;
                    }

                    if ((Integer)options.get().getValue() == 1) {
                        return EntityTypes1_11.EntityType.DONKEY;
                    }

                    if ((Integer)options.get().getValue() == 2) {
                        return EntityTypes1_11.EntityType.MULE;
                    }

                    if ((Integer)options.get().getValue() == 3) {
                        return EntityTypes1_11.EntityType.ZOMBIE_HORSE;
                    }

                    if ((Integer)options.get().getValue() == 4) {
                        return EntityTypes1_11.EntityType.SKELETON_HORSE;
                    }
                }
            }
        } catch (Exception e) {
            if (!Via.getConfig().isSuppressMetadataErrors() || Via.getManager().isDebug()) {
                Via.getPlatform().getLogger().warning("An error occurred with entity type rewriter");
                Via.getPlatform().getLogger().warning("Metadata: " + metadata);
                Via.getPlatform().getLogger().log(Level.WARNING, "Error: ", e);
            }
        }

        return type;
    }

    public static Optional<Metadata> getById(List<Metadata> metadatas, int id) {
        for (Metadata metadata : metadatas) {
            if (metadata.id() == id) {
                return Optional.of(metadata);
            }
        }

        return Optional.empty();
    }
}
