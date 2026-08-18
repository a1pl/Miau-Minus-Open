package com.viaversion.viarewind.protocol.protocol1_8to1_9.storage;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Vector;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_10;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntOpenHashMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.fastutil.ints.IntArrayList;
import com.viaversion.viaversion.libs.fastutil.ints.IntList;
import java.util.List;
import java.util.Map.Entry;

public class EntityTracker1_9 extends EntityTrackerBase {
    private final Int2ObjectMap<IntList> vehicles = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Vector> offsets = new Int2ObjectOpenHashMap<>();
    private final Int2IntMap status = new Int2IntOpenHashMap();

    public EntityTracker1_9(UserConnection connection) {
        super(connection, EntityTypes1_10.EntityType.ENTITY_HUMAN);
    }

    @Override
    public void removeEntity(int id) {
        this.vehicles.remove(id);
        this.offsets.remove(id);
        this.status.remove(id);
        this.vehicles.forEach((vehicle, passengers) -> passengers.rem(id));
        this.vehicles.int2ObjectEntrySet().removeIf(entry -> entry.getValue().isEmpty());
        super.removeEntity(id);
    }

    public void resetEntityOffset(int id) {
        this.offsets.remove(id);
    }

    public Vector getEntityOffset(int id) {
        return this.offsets.get(id);
    }

    public void setEntityOffset(int id, Vector offset) {
        this.offsets.put(id, offset);
    }

    public IntList getPassengers(int id) {
        return this.vehicles.getOrDefault(id, new IntArrayList());
    }

    public void setPassengers(int id, IntList passengers) {
        this.vehicles.put(id, passengers);
    }

    public boolean isInsideVehicle(int id) {
        for (List<Integer> vehicle : this.vehicles.values()) {
            if (vehicle.contains(id)) {
                return true;
            }
        }

        return false;
    }

    public int getVehicle(int passenger) {
        for (Entry<Integer, IntList> vehicle : this.vehicles.int2ObjectEntrySet()) {
            if (vehicle.getValue().contains(passenger)) {
                return vehicle.getKey();
            }
        }

        return -1;
    }

    public Int2IntMap getStatus() {
        return this.status;
    }
}
