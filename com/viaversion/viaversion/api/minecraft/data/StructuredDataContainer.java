package com.viaversion.viaversion.api.minecraft.data;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.FullMappings;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.libs.fastutil.objects.Reference2ObjectOpenHashMap;
import com.viaversion.viaversion.util.Unit;
import java.util.Map;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class StructuredDataContainer {
    private final Map<StructuredDataKey<?>, StructuredData<?>> data;
    private FullMappings lookup;
    private boolean mappedNames;

    public StructuredDataContainer(Map<StructuredDataKey<?>, StructuredData<?>> data) {
        this.data = data;
    }

    public StructuredDataContainer(StructuredData<?>[] dataArray) {
        this(new Reference2ObjectOpenHashMap<>(dataArray.length));

        for (StructuredData<?> data : dataArray) {
            this.add(data);
        }
    }

    public StructuredDataContainer() {
        this(new Reference2ObjectOpenHashMap<>());
    }

    public <T> @Nullable StructuredData<T> get(StructuredDataKey<T> key) {
        return (StructuredData<T>)this.data.get(key);
    }

    public <T> @Nullable StructuredData<T> getNonEmpty(StructuredDataKey<T> key) {
        StructuredData<T> data = (StructuredData<T>)this.data.get(key);
        return data != null && data.isPresent() ? data : null;
    }

    public <T> StructuredData<T> computeIfAbsent(
        StructuredDataKey<T> key, Function<StructuredDataKey<T>, T> mappingFunction
    ) {
        StructuredData<T> data = this.getNonEmpty(key);
        if (data != null) {
            return data;
        }

        int id = this.serializerId(key);
        StructuredData<T> empty = StructuredData.of(key, mappingFunction.apply(key), id);
        this.data.put(key, empty);
        return empty;
    }

    public <T> void set(StructuredDataKey<T> key, T value) {
        int id = this.serializerId(key);
        if (id != -1) {
            this.data.put(key, StructuredData.of(key, value, id));
        }
    }

    public void set(StructuredDataKey<Unit> key) {
        this.set(key, Unit.INSTANCE);
    }

    public void addEmpty(StructuredDataKey<?> key) {
        this.data.put(key, StructuredData.empty(key, this.serializerId(key)));
    }

    public <T> @Nullable StructuredData<T> remove(StructuredDataKey<T> key) {
        StructuredData<?> data = this.data.remove(key);
        return (StructuredData<T>)(data != null ? data : null);
    }

    public boolean contains(StructuredDataKey<?> key) {
        return this.data.containsKey(key);
    }

    public void setIdLookup(Protocol<?, ?, ?, ?> protocol, boolean mappedNames) {
        this.lookup = protocol.getMappingData().getDataComponentSerializerMappings();
        this.mappedNames = mappedNames;
    }

    public StructuredDataContainer copy() {
        StructuredDataContainer copy = new StructuredDataContainer(new Reference2ObjectOpenHashMap<>(this.data));
        copy.lookup = this.lookup;
        return copy;
    }

    private int serializerId(StructuredDataKey<?> key) {
        int id = this.mappedNames ? this.lookup.mappedId(key.identifier()) : this.lookup.id(key.identifier());
        if (id == -1) {
            Via.getPlatform().getLogger().severe("Could not find item data serializer for type " + key);
        }

        return id;
    }

    public Map<StructuredDataKey<?>, StructuredData<?>> data() {
        return this.data;
    }

    private <T> void add(StructuredData<T> data) {
        this.set(data.key(), data.value());
    }

    @Override
    public String toString() {
        return "StructuredDataContainer{data=" + this.data + '}';
    }
}
