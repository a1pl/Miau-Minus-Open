package com.viaversion.viaversion.data.entity;

import com.google.common.base.Preconditions;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.ClientEntityIdChangeListener;
import com.viaversion.viaversion.api.data.entity.DimensionData;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.data.entity.TrackedEntity;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.util.Key;
import java.util.Collections;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class EntityTrackerBase implements EntityTracker, ClientEntityIdChangeListener {
    private final Int2ObjectMap<TrackedEntity> entities = new Int2ObjectOpenHashMap<>();
    private final UserConnection connection;
    private final EntityType playerType;
    private int clientEntityId = -1;
    private int currentWorldSectionHeight = -1;
    private int currentMinY;
    private String currentWorld;
    private int biomesSent = -1;
    private Map<String, DimensionData> dimensions = Collections.emptyMap();

    public EntityTrackerBase(UserConnection connection, @Nullable EntityType playerType) {
        this.connection = connection;
        this.playerType = playerType;
    }

    @Override
    public UserConnection user() {
        return this.connection;
    }

    @Override
    public void addEntity(int id, EntityType type) {
        this.entities.put(id, new TrackedEntityImpl(type));
    }

    @Override
    public boolean hasEntity(int id) {
        return this.entities.containsKey(id);
    }

    @Override
    public @Nullable TrackedEntity entity(int entityId) {
        return this.entities.get(entityId);
    }

    @Override
    public @Nullable EntityType entityType(int id) {
        TrackedEntity entity = this.entities.get(id);
        return entity != null ? entity.entityType() : null;
    }

    @Override
    public @Nullable StoredEntityData entityData(int id) {
        TrackedEntity entity = this.entities.get(id);
        return entity != null ? entity.data() : null;
    }

    @Override
    public @Nullable StoredEntityData entityDataIfPresent(int id) {
        TrackedEntity entity = this.entities.get(id);
        return entity != null && entity.hasData() ? entity.data() : null;
    }

    @Override
    public void removeEntity(int id) {
        this.entities.remove(id);
    }

    @Override
    public void clearEntities() {
        this.entities.clear();
    }

    @Override
    public int clientEntityId() {
        return this.clientEntityId;
    }

    @Override
    public void setClientEntityId(int clientEntityId) {
        Preconditions.checkNotNull(this.playerType);
        TrackedEntity oldEntity;
        if (this.clientEntityId != -1 && (oldEntity = this.entities.remove(this.clientEntityId)) != null) {
            this.entities.put(clientEntityId, oldEntity);
        } else {
            this.entities.put(clientEntityId, new TrackedEntityImpl(this.playerType));
        }

        this.clientEntityId = clientEntityId;
    }

    @Override
    public boolean trackClientEntity() {
        if (this.clientEntityId != -1) {
            this.entities.put(this.clientEntityId, new TrackedEntityImpl(this.playerType));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int currentWorldSectionHeight() {
        return this.currentWorldSectionHeight;
    }

    @Override
    public void setCurrentWorldSectionHeight(int currentWorldSectionHeight) {
        this.currentWorldSectionHeight = currentWorldSectionHeight;
    }

    @Override
    public int currentMinY() {
        return this.currentMinY;
    }

    @Override
    public void setCurrentMinY(int currentMinY) {
        this.currentMinY = currentMinY;
    }

    @Override
    public @Nullable String currentWorld() {
        return this.currentWorld;
    }

    @Override
    public void setCurrentWorld(String currentWorld) {
        this.currentWorld = currentWorld;
    }

    @Override
    public int biomesSent() {
        return this.biomesSent;
    }

    @Override
    public void setBiomesSent(int biomesSent) {
        this.biomesSent = biomesSent;
    }

    @Override
    public EntityType playerType() {
        return this.playerType;
    }

    @Override
    public @Nullable DimensionData dimensionData(String dimension) {
        return this.dimensions.get(Key.stripMinecraftNamespace(dimension));
    }

    @Override
    public @Nullable DimensionData dimensionData(int dimensionId) {
        return this.dimensions.values().stream().filter(data -> data.id() == dimensionId).findFirst().orElse(null);
    }

    @Override
    public void setDimensions(Map<String, DimensionData> dimensions) {
        this.dimensions = dimensions;
    }
}
