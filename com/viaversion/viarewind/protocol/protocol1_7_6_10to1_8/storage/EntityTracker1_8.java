package com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.storage;

import com.viaversion.viarewind.ViaRewind;
import com.viaversion.viarewind.protocol.protocol1_7_2_5to1_7_6_10.ServerboundPackets1_7_2_5;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.Protocol1_7_6_10To1_8;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.data.VirtualHologramEntity;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_10;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntArrayMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectArrayMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntOpenHashMap;
import com.viaversion.viaversion.protocols.protocol1_8.ClientboundPackets1_8;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;

public class EntityTracker1_8 extends EntityTrackerBase {
    private final Int2ObjectMap<VirtualHologramEntity> holograms = new Int2ObjectArrayMap<>();
    private final Int2IntMap vehicles = new Int2IntArrayMap();
    private final Int2ObjectMap<UUID> entityIdToUUID = new Int2ObjectArrayMap<>();
    private final Object2IntMap<UUID> entityUUIDToId = new Object2IntOpenHashMap<>();
    public int spectatingClientEntityId = -1;
    private int clientEntityGameMode;

    public EntityTracker1_8(UserConnection connection) {
        super(connection, EntityTypes1_10.EntityType.ENTITY_HUMAN);
    }

    @Override
    public void addEntity(int id, EntityType type) {
        super.addEntity(id, type);
        if (type == EntityTypes1_10.EntityType.ARMOR_STAND) {
            this.holograms.put(id, new VirtualHologramEntity(this.user(), id));
        }
    }

    @Override
    public void removeEntity(int entityId) {
        super.removeEntity(entityId);
        if (this.entityIdToUUID.containsKey(entityId)) {
            UUID playerId = this.entityIdToUUID.remove(entityId);
            this.entityUUIDToId.removeInt(playerId);
            this.user().get(PlayerSessionStorage.class).getPlayerEquipment().remove(playerId);
        }
    }

    @Override
    public void clearEntities() {
        super.clearEntities();
        this.vehicles.clear();
    }

    @Override
    public void setClientEntityId(int entityId) {
        if (this.spectatingClientEntityId == this.clientEntityId()) {
            this.spectatingClientEntityId = entityId;
        }

        super.setClientEntityId(entityId);
    }

    public void addPlayer(int entityId, UUID uuid) {
        this.entityUUIDToId.put(uuid, entityId);
        this.entityIdToUUID.put(entityId, uuid);
    }

    public UUID getPlayerUUID(int entityId) {
        return this.entityIdToUUID.get(entityId);
    }

    public int getPlayerEntityId(UUID uuid) {
        return this.entityUUIDToId.getOrDefault(uuid, -1);
    }

    public int getVehicle(int passengerId) {
        for (Entry<Integer, Integer> vehicle : this.vehicles.entrySet()) {
            if (vehicle.getValue() == passengerId) {
                return vehicle.getValue();
            }
        }

        return -1;
    }

    public int getPassenger(int vehicleId) {
        return this.vehicles.getOrDefault(vehicleId, -1);
    }

    protected void startSneaking() {
        try {
            PacketWrapper entityAction = PacketWrapper.create(ServerboundPackets1_7_2_5.ENTITY_ACTION, this.user());
            entityAction.write(Type.VAR_INT, this.clientEntityId());
            entityAction.write(Type.VAR_INT, 0);
            entityAction.write(Type.VAR_INT, 0);
            entityAction.sendToServer(Protocol1_7_6_10To1_8.class);
        } catch (Exception e) {
            ViaRewind.getPlatform().getLogger().log(Level.SEVERE, "Failed to send sneak packet", e);
        }
    }

    public void setPassenger(int vehicleId, int passengerId) {
        if (vehicleId == this.spectatingClientEntityId && this.spectatingClientEntityId != this.clientEntityId()) {
            this.startSneaking();
            this.setSpectating(this.clientEntityId());
        }

        if (vehicleId == -1) {
            this.vehicles.remove(this.getVehicle(passengerId));
        } else if (passengerId == -1) {
            this.vehicles.remove(vehicleId);
        } else {
            this.vehicles.put(vehicleId, passengerId);
        }
    }

    protected void attachEntity(int target) {
        try {
            PacketWrapper attachEntity = PacketWrapper.create(ClientboundPackets1_8.ATTACH_ENTITY, this.user());
            attachEntity.write(Type.INT, this.clientEntityId());
            attachEntity.write(Type.INT, target);
            attachEntity.write(Type.BOOLEAN, false);
            attachEntity.scheduleSend(Protocol1_7_6_10To1_8.class);
        } catch (Exception e) {
            ViaRewind.getPlatform().getLogger().log(Level.SEVERE, "Failed to send attach packet", e);
        }
    }

    public void setSpectating(int spectating) {
        if (spectating != this.clientEntityId() && this.getPassenger(spectating) != -1) {
            this.startSneaking();
            this.setSpectating(this.clientEntityId());
        } else {
            if (this.spectatingClientEntityId != spectating && this.spectatingClientEntityId != this.clientEntityId()) {
                this.attachEntity(-1);
            }

            this.spectatingClientEntityId = spectating;
            if (spectating != this.clientEntityId()) {
                this.attachEntity(this.spectatingClientEntityId);
            }
        }
    }

    public Int2ObjectMap<VirtualHologramEntity> getHolograms() {
        return this.holograms;
    }

    public boolean isSpectator() {
        return this.clientEntityGameMode == 3;
    }

    public void setClientEntityGameMode(int clientEntityGameMode) {
        this.clientEntityGameMode = clientEntityGameMode;
    }
}
