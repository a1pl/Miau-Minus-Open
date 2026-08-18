package com.viaversion.viaversion.rewriter;

import com.google.common.base.Preconditions;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.Int2IntMapMappings;
import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.data.entity.DimensionData;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.data.entity.TrackedEntity;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.rewriter.RewriterBase;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.DimensionDataImpl;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.rewriter.meta.MetaFilter;
import com.viaversion.viaversion.rewriter.meta.MetaHandlerEvent;
import com.viaversion.viaversion.rewriter.meta.MetaHandlerEventImpl;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.TagUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class EntityRewriter<C extends ClientboundPacketType, T extends Protocol<C, ?, ?, ?>>
    extends RewriterBase<T>
    implements com.viaversion.viaversion.api.rewriter.EntityRewriter<T> {
    private static final Metadata[] EMPTY_ARRAY = new Metadata[0];
    protected final List<MetaFilter> metadataFilters = new ArrayList<>();
    protected final boolean trackMappedType;
    protected Mappings typeMappings;

    protected EntityRewriter(T protocol) {
        this(protocol, true);
    }

    protected EntityRewriter(T protocol, boolean trackMappedType) {
        super(protocol);
        this.trackMappedType = trackMappedType;
        protocol.put(this);
    }

    public MetaFilter.Builder filter() {
        return new MetaFilter.Builder(this);
    }

    public void registerFilter(MetaFilter filter) {
        Preconditions.checkArgument(!this.metadataFilters.contains(filter));
        this.metadataFilters.add(filter);
    }

    @Override
    public void handleMetadata(int entityId, List<Metadata> metadataList, UserConnection connection) {
        TrackedEntity entity = this.<EntityTracker>tracker(connection).entity(entityId);
        EntityType type = entity != null ? entity.entityType() : null;

        for (Metadata metadata : metadataList.toArray(EMPTY_ARRAY)) {
            MetaHandlerEvent event = null;

            for (MetaFilter filter : this.metadataFilters) {
                if (filter.isFiltered(type, metadata)) {
                    if (event == null) {
                        event = new MetaHandlerEventImpl(connection, entity, entityId, metadata, metadataList);
                    }

                    try {
                        filter.handler().handle(event, metadata);
                    } catch (Exception e) {
                        this.logException(e, type, metadataList, metadata);
                        metadataList.remove(metadata);
                        break;
                    }

                    if (event.cancelled()) {
                        metadataList.remove(metadata);
                        break;
                    }
                }
            }

            if (event != null && event.hasExtraMeta()) {
                metadataList.addAll(event.extraMeta());
            }
        }

        if (entity != null) {
            entity.sentMetadata(true);
        }
    }

    @Override
    public int newEntityId(int id) {
        return this.typeMappings != null ? this.typeMappings.getNewIdOrDefault(id, id) : id;
    }

    public void mapEntityType(EntityType type, EntityType mappedType) {
        Preconditions.checkArgument(
            type.getClass() != mappedType.getClass(), "EntityTypes should not be of the same class/enum"
        );
        this.mapEntityType(type.getId(), mappedType.getId());
    }

    protected void mapEntityType(int id, int mappedId) {
        if (this.typeMappings == null) {
            this.typeMappings = Int2IntMapMappings.of();
        }

        this.typeMappings.setNewId(id, mappedId);
    }

    public <E extends Enum<E> & EntityType> void mapTypes(EntityType[] oldTypes, Class<E> newTypeClass) {
        if (this.typeMappings == null) {
            this.typeMappings = Int2IntMapMappings.of();
        }

        for (EntityType oldType : oldTypes) {
            try {
                E newType = Enum.valueOf(newTypeClass, oldType.name());
                this.typeMappings.setNewId(oldType.getId(), newType.getId());
            } catch (IllegalArgumentException notFound) {
                if (!this.typeMappings.contains(oldType.getId())) {
                    Via.getPlatform()
                        .getLogger()
                        .warning(
                            "Could not find new entity type for "
                                + oldType
                                + "! Old type: "
                                + oldType.getClass().getEnclosingClass().getSimpleName()
                                + ", new type: "
                                + newTypeClass.getEnclosingClass().getSimpleName()
                        );
                }
            }
        }
    }

    public void mapTypes() {
        Preconditions.checkArgument(
            this.typeMappings == null,
            "Type mappings have already been set - manual type mappings should be set *after* this"
        );
        Preconditions.checkNotNull(
            this.protocol.getMappingData().getEntityMappings(), "Protocol does not have entity mappings"
        );
        this.typeMappings = this.protocol.getMappingData().getEntityMappings();
    }

    public void registerMetaTypeHandler(
        @Nullable MetaType itemType, @Nullable MetaType blockStateType, @Nullable MetaType particleType
    ) {
        this.registerMetaTypeHandler(itemType, null, blockStateType, particleType, null);
    }

    public void registerMetaTypeHandler(
        @Nullable MetaType itemType,
        @Nullable MetaType blockStateType,
        @Nullable MetaType optionalBlockStateType,
        @Nullable MetaType particleType,
        @Nullable MetaType particlesType
    ) {
        this.filter().handler((event, meta) -> {
            MetaType type = meta.metaType();
            if (type == itemType) {
                meta.setValue(this.protocol.getItemRewriter().handleItemToClient(event.user(), meta.value()));
            } else if (type == blockStateType) {
                int data = meta.<Integer>value();
                meta.setValue(this.protocol.getMappingData().getNewBlockStateId(data));
            } else if (type == optionalBlockStateType) {
                int data = meta.<Integer>value();
                if (data != 0) {
                    meta.setValue(this.protocol.getMappingData().getNewBlockStateId(data));
                }
            } else if (type == particleType) {
                this.rewriteParticle(event.user(), meta.value());
            } else if (type == particlesType) {
                Particle[] particles = meta.value();

                for (Particle particle : particles) {
                    this.rewriteParticle(event.user(), particle);
                }
            }
        });
    }

    public void registerTracker(C packetType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.map((Type<T>)Type.UUID);
                this.map(Type.VAR_INT);
                this.handler(EntityRewriter.this.trackerHandler());
            }
        });
    }

    public void registerTrackerWithData(C packetType, EntityType fallingBlockType) {
        this.protocol
            .registerClientbound(
                packetType,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.VAR_INT);
                        this.map((Type<T>)Type.UUID);
                        this.map(Type.VAR_INT);
                        this.map(Type.DOUBLE);
                        this.map(Type.DOUBLE);
                        this.map(Type.DOUBLE);
                        this.map(Type.BYTE);
                        this.map(Type.BYTE);
                        this.map(Type.INT);
                        this.handler(EntityRewriter.this.trackerHandler());
                        this.handler(
                            wrapper -> {
                                int entityId = wrapper.get(Type.VAR_INT, 0);
                                EntityType entityType = EntityRewriter.this.<EntityTracker>tracker(wrapper.user())
                                    .entityType(entityId);
                                if (entityType == fallingBlockType) {
                                    wrapper.set(
                                        Type.INT,
                                        0,
                                        EntityRewriter.this.protocol
                                            .getMappingData()
                                            .getNewBlockStateId(wrapper.get(Type.INT, 0))
                                    );
                                }
                            }
                        );
                    }
                }
            );
    }

    public void registerTrackerWithData1_19(C packetType, EntityType fallingBlockType) {
        this.protocol
            .registerClientbound(
                packetType,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.VAR_INT);
                        this.map((Type<T>)Type.UUID);
                        this.map(Type.VAR_INT);
                        this.map(Type.DOUBLE);
                        this.map(Type.DOUBLE);
                        this.map(Type.DOUBLE);
                        this.map(Type.BYTE);
                        this.map(Type.BYTE);
                        this.map(Type.BYTE);
                        this.map(Type.VAR_INT);
                        this.handler(EntityRewriter.this.trackerHandler());
                        this.handler(
                            wrapper -> {
                                if (EntityRewriter.this.protocol.getMappingData() != null) {
                                    int entityId = wrapper.get(Type.VAR_INT, 0);
                                    EntityType entityType = EntityRewriter.this.<EntityTracker>tracker(wrapper.user())
                                        .entityType(entityId);
                                    if (entityType == fallingBlockType) {
                                        wrapper.set(
                                            Type.VAR_INT,
                                            2,
                                            EntityRewriter.this.protocol
                                                .getMappingData()
                                                .getNewBlockStateId(wrapper.get(Type.VAR_INT, 2))
                                        );
                                    }
                                }
                            }
                        );
                    }
                }
            );
    }

    public void registerTracker(C packetType, EntityType entityType, Type<Integer> intType) {
        this.protocol.registerClientbound(packetType, wrapper -> {
            int entityId = wrapper.passthrough(intType);
            this.<EntityTracker>tracker(wrapper.user()).addEntity(entityId, entityType);
        });
    }

    public void registerTracker(C packetType, EntityType entityType) {
        this.registerTracker(packetType, entityType, Type.VAR_INT);
    }

    public void registerRemoveEntities(C packetType) {
        this.protocol.registerClientbound(packetType, wrapper -> {
            int[] entityIds = wrapper.passthrough(Type.VAR_INT_ARRAY_PRIMITIVE);
            EntityTracker entityTracker = this.tracker(wrapper.user());

            for (int entity : entityIds) {
                entityTracker.removeEntity(entity);
            }
        });
    }

    public void registerRemoveEntity(C packetType) {
        this.protocol.registerClientbound(packetType, wrapper -> {
            int entityId = wrapper.passthrough(Type.VAR_INT);
            this.<EntityTracker>tracker(wrapper.user()).removeEntity(entityId);
        });
    }

    public void registerMetadataRewriter(
        C packetType, @Nullable Type<List<Metadata>> oldMetaType, Type<List<Metadata>> newMetaType
    ) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                if (oldMetaType != null) {
                    this.map(oldMetaType, newMetaType);
                } else {
                    this.map(newMetaType);
                }

                this.handler(wrapper -> {
                    int entityId = wrapper.get(Type.VAR_INT, 0);
                    List<Metadata> metadata = wrapper.get(newMetaType, 0);
                    EntityRewriter.this.handleMetadata(entityId, metadata, wrapper.user());
                });
            }
        });
    }

    public void registerMetadataRewriter(C packetType, Type<List<Metadata>> metaType) {
        this.registerMetadataRewriter(packetType, null, metaType);
    }

    public PacketHandler trackerHandler() {
        return this.trackerAndRewriterHandler(null);
    }

    public PacketHandler playerTrackerHandler() {
        return wrapper -> {
            EntityTracker tracker = this.tracker(wrapper.user());
            int entityId = wrapper.get(Type.INT, 0);
            tracker.setClientEntityId(entityId);
            tracker.addEntity(entityId, tracker.playerType());
        };
    }

    public PacketHandler worldDataTrackerHandler(int nbtIndex) {
        return wrapper -> {
            EntityTracker tracker = this.tracker(wrapper.user());
            CompoundTag registryData = wrapper.get(Type.NAMED_COMPOUND_TAG, nbtIndex);
            NumberTag height = registryData.getNumberTag("height");
            if (height != null) {
                int blockHeight = height.asInt();
                tracker.setCurrentWorldSectionHeight(blockHeight >> 4);
            } else {
                Via.getPlatform().getLogger().warning("Height missing in dimension data: " + registryData);
            }

            NumberTag minY = registryData.getNumberTag("min_y");
            if (minY != null) {
                tracker.setCurrentMinY(minY.asInt());
            } else {
                Via.getPlatform().getLogger().warning("Min Y missing in dimension data: " + registryData);
            }

            String world = wrapper.get(Type.STRING, 0);
            if (tracker.currentWorld() != null && !tracker.currentWorld().equals(world)) {
                tracker.clearEntities();
                tracker.trackClientEntity();
            }

            tracker.setCurrentWorld(world);
        };
    }

    public PacketHandler worldDataTrackerHandlerByKey() {
        return wrapper -> {
            EntityTracker tracker = this.tracker(wrapper.user());
            String dimensionKey = wrapper.get(Type.STRING, 0);
            DimensionData dimensionData = tracker.dimensionData(dimensionKey);
            if (dimensionData == null) {
                Via.getPlatform()
                    .getLogger()
                    .severe("Dimension data missing for dimension: " + dimensionKey + ", falling back to overworld");
                dimensionData = tracker.dimensionData("minecraft:overworld");
                Preconditions.checkNotNull(dimensionData, "Overworld data missing");
            }

            tracker.setCurrentWorldSectionHeight(dimensionData.height() >> 4);
            tracker.setCurrentMinY(dimensionData.minY());
            String world = wrapper.get(Type.STRING, 1);
            if (tracker.currentWorld() != null && !tracker.currentWorld().equals(world)) {
                tracker.clearEntities();
                tracker.trackClientEntity();
            }

            tracker.setCurrentWorld(world);
        };
    }

    public PacketHandler worldDataTrackerHandlerByKey1_20_5(int dimensionIdIndex) {
        return wrapper -> {
            EntityTracker tracker = this.tracker(wrapper.user());
            int dimensionId = wrapper.get(Type.VAR_INT, dimensionIdIndex);
            DimensionData dimensionData = tracker.dimensionData(dimensionId);
            if (dimensionData == null) {
                Via.getPlatform()
                    .getLogger()
                    .severe("Dimension data missing for dimension: " + dimensionId + ", falling back to overworld");
                dimensionData = tracker.dimensionData("minecraft:overworld");
                Preconditions.checkNotNull(dimensionData, "Overworld data missing");
            }

            tracker.setCurrentWorldSectionHeight(dimensionData.height() >> 4);
            tracker.setCurrentMinY(dimensionData.minY());
            String world = wrapper.get(Type.STRING, 0);
            if (tracker.currentWorld() != null && !tracker.currentWorld().equals(world)) {
                tracker.clearEntities();
                tracker.trackClientEntity();
            }

            tracker.setCurrentWorld(world);
        };
    }

    public PacketHandler biomeSizeTracker() {
        return wrapper -> this.trackBiomeSize(wrapper.user(), wrapper.get(Type.NAMED_COMPOUND_TAG, 0));
    }

    public PacketHandler configurationBiomeSizeTracker() {
        return wrapper -> this.trackBiomeSize(wrapper.user(), wrapper.get(Type.COMPOUND_TAG, 0));
    }

    public void trackBiomeSize(UserConnection connection, CompoundTag registry) {
        ListTag<?> biomes = TagUtil.getRegistryEntries(registry, "worldgen/biome");
        this.<EntityTracker>tracker(connection).setBiomesSent(biomes.size());
    }

    public PacketHandler dimensionDataHandler() {
        return wrapper -> this.cacheDimensionData(wrapper.user(), wrapper.get(Type.NAMED_COMPOUND_TAG, 0));
    }

    public PacketHandler configurationDimensionDataHandler() {
        return wrapper -> this.cacheDimensionData(wrapper.user(), wrapper.get(Type.COMPOUND_TAG, 0));
    }

    public void cacheDimensionData(UserConnection connection, CompoundTag registry) {
        ListTag<CompoundTag> dimensions = TagUtil.getRegistryEntries(registry, "dimension_type");
        Map<String, DimensionData> dimensionDataMap = new HashMap<>(dimensions.size());

        for (CompoundTag dimension : dimensions) {
            NumberTag idTag = dimension.getNumberTag("id");
            CompoundTag element = dimension.getCompoundTag("element");
            String name = dimension.getStringTag("name").getValue();
            dimensionDataMap.put(Key.stripMinecraftNamespace(name), new DimensionDataImpl(idTag.asInt(), element));
        }

        this.<EntityTracker>tracker(connection).setDimensions(dimensionDataMap);
    }

    public PacketHandler registryDataHandler1_20_5() {
        return wrapper -> {
            String registryKey = Key.stripMinecraftNamespace(wrapper.get(Type.STRING, 0));
            if (registryKey.equals("worldgen/biome")) {
                RegistryEntry[] entries = wrapper.get(Type.REGISTRY_ENTRY_ARRAY, 0);
                this.<EntityTracker>tracker(wrapper.user()).setBiomesSent(entries.length);
            } else if (registryKey.equals("dimension_type")) {
                RegistryEntry[] entries = wrapper.get(Type.REGISTRY_ENTRY_ARRAY, 0);
                Map<String, DimensionData> dimensionDataMap = new HashMap<>(entries.length);

                for (int i = 0; i < entries.length; i++) {
                    RegistryEntry entry = entries[i];
                    dimensionDataMap.put(entry.key(), new DimensionDataImpl(i, (CompoundTag)entry.tag()));
                }

                this.<EntityTracker>tracker(wrapper.user()).setDimensions(dimensionDataMap);
            }
        };
    }

    public PacketHandler trackerAndRewriterHandler(@Nullable Type<List<Metadata>> metaType) {
        return wrapper -> {
            int entityId = wrapper.get(Type.VAR_INT, 0);
            int type = wrapper.get(Type.VAR_INT, 1);
            int newType = this.newEntityId(type);
            if (newType != type) {
                wrapper.set(Type.VAR_INT, 1, newType);
            }

            EntityType entType = this.typeFromId(this.trackMappedType ? newType : type);
            this.<EntityTracker>tracker(wrapper.user()).addEntity(entityId, entType);
            if (metaType != null) {
                this.handleMetadata(entityId, wrapper.get(metaType, 0), wrapper.user());
            }
        };
    }

    public PacketHandler trackerAndRewriterHandler(@Nullable Type<List<Metadata>> metaType, EntityType entityType) {
        return wrapper -> {
            int entityId = wrapper.get(Type.VAR_INT, 0);
            this.<EntityTracker>tracker(wrapper.user()).addEntity(entityId, entityType);
            if (metaType != null) {
                this.handleMetadata(entityId, wrapper.get(metaType, 0), wrapper.user());
            }
        };
    }

    public PacketHandler objectTrackerHandler() {
        return wrapper -> {
            int entityId = wrapper.get(Type.VAR_INT, 0);
            byte type = wrapper.get(Type.BYTE, 0);
            EntityType entType = this.objectTypeFromId(type);
            this.<EntityTracker>tracker(wrapper.user()).addEntity(entityId, entType);
        };
    }

    public void rewriteParticle(UserConnection connection, Particle particle) {
        ParticleMappings mappings = this.protocol.getMappingData().getParticleMappings();
        int id = particle.id();
        if (mappings.isBlockParticle(id)) {
            Particle.ParticleData<Integer> data = particle.getArgument(0);
            data.setValue(this.protocol.getMappingData().getNewBlockStateId(data.getValue()));
        } else if (mappings.isItemParticle(id) && this.protocol.getItemRewriter() != null) {
            Particle.ParticleData<Item> data = particle.getArgument(0);
            com.viaversion.viaversion.api.rewriter.ItemRewriter<?> itemRewriter = this.protocol.getItemRewriter();
            Item item = itemRewriter.handleItemToClient(connection, data.getValue());
            if (itemRewriter.mappedItemType() != null && itemRewriter.itemType() != itemRewriter.mappedItemType()) {
                particle.set(0, itemRewriter.mappedItemType(), item);
            } else {
                data.setValue(item);
            }
        }

        particle.setId(this.protocol.getMappingData().getNewParticleId(id));
    }

    public void rewriteParticle(PacketWrapper wrapper, Type<Particle> from, Type<Particle> to) throws Exception {
        Particle particle = wrapper.read(from);
        this.rewriteParticle(wrapper.user(), particle);
        wrapper.write(to, particle);
    }

    private void logException(Exception e, @Nullable EntityType type, List<Metadata> metadataList, Metadata metadata) {
        if (!Via.getConfig().isSuppressMetadataErrors() || Via.getManager().isDebug()) {
            Logger logger = Via.getPlatform().getLogger();
            logger.severe(
                "An error occurred in metadata handler "
                    + this.getClass().getSimpleName()
                    + " for "
                    + (type != null ? type.name() : "untracked")
                    + " entity type: "
                    + metadata
            );
            logger.severe(
                metadataList.stream()
                    .sorted(Comparator.comparingInt(Metadata::id))
                    .map(Metadata::toString)
                    .collect(Collectors.joining("\n", "Full metadata: ", ""))
            );
            logger.log(Level.SEVERE, "Error: ", e);
        }
    }
}
