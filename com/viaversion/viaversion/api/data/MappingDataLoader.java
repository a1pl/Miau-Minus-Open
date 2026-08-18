package com.viaversion.viaversion.api.data;

import com.google.common.annotations.Beta;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntOpenHashMap;
import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonIOException;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonSyntaxException;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntArrayTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.libs.opennbt.tag.io.NBTIO;
import com.viaversion.viaversion.libs.opennbt.tag.io.TagReader;
import com.viaversion.viaversion.util.GsonUtil;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MappingDataLoader {
    public static final MappingDataLoader INSTANCE = new MappingDataLoader(
        MappingDataLoader.class, "assets/viaversion/data/"
    );
    public static final TagReader<CompoundTag> MAPPINGS_READER = NBTIO.reader(CompoundTag.class).named();
    private static final Map<String, String[]> GLOBAL_IDENTIFIER_INDEXES = new HashMap<>();
    private static final byte DIRECT_ID = 0;
    private static final byte SHIFTS_ID = 1;
    private static final byte CHANGES_ID = 2;
    private static final byte IDENTITY_ID = 3;
    private final Map<String, CompoundTag> mappingsCache = new HashMap<>();
    private final Class<?> dataLoaderClass;
    private final String dataPath;
    private boolean cacheValid = true;

    public MappingDataLoader(Class<?> dataLoaderClass, String dataPath) {
        this.dataLoaderClass = dataLoaderClass;
        this.dataPath = dataPath;
    }

    public static void loadGlobalIdentifiers() {
        CompoundTag globalIdentifiers = INSTANCE.loadNBT("identifier-table.nbt");

        for (Entry<String, Tag> entry : globalIdentifiers.entrySet()) {
            ListTag<StringTag> value = (ListTag<StringTag>)entry.getValue();
            String[] array = new String[value.size()];
            int i = 0;

            for (int size = value.size(); i < size; i++) {
                array[i] = value.get(i).getValue();
            }

            GLOBAL_IDENTIFIER_INDEXES.put(entry.getKey(), array);
        }
    }

    public @Nullable String identifierFromGlobalId(String registry, int globalId) {
        String[] array = GLOBAL_IDENTIFIER_INDEXES.get(registry);
        if (array == null) {
            throw new IllegalArgumentException("Unknown global identifier key: " + registry);
        } else if (globalId >= 0 && globalId < array.length) {
            return array[globalId];
        } else {
            throw new IllegalArgumentException("Unknown global identifier index: " + globalId);
        }
    }

    public void clearCache() {
        this.mappingsCache.clear();
        this.cacheValid = false;
    }

    public @Nullable JsonObject loadFromDataDir(String name) {
        File file = new File(this.getDataFolder(), name);
        if (!file.exists()) {
            return this.loadData(name);
        }

        try (FileReader reader = new FileReader(file)) {
            return GsonUtil.getGson().fromJson(reader, JsonObject.class);
        } catch (JsonSyntaxException e) {
            this.getLogger().warning(name + " is badly formatted!");
            throw new RuntimeException(e);
        } catch (IOException | JsonIOException e) {
            throw new RuntimeException(e);
        }
    }

    public @Nullable JsonObject loadData(String name) {
        InputStream stream = this.getResource(name);
        if (stream == null) {
            return null;
        }

        try (InputStreamReader reader = new InputStreamReader(stream)) {
            return GsonUtil.getGson().fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public @Nullable CompoundTag loadNBT(String name, boolean cache) {
        if (!this.cacheValid) {
            return this.loadNBTFromFile(name);
        }

        CompoundTag data = this.mappingsCache.get(name);
        if (data != null) {
            return data;
        }

        data = this.loadNBTFromFile(name);
        if (cache && data != null) {
            this.mappingsCache.put(name, data);
        }

        return data;
    }

    public @Nullable CompoundTag loadNBT(String name) {
        return this.loadNBT(name, false);
    }

    public @Nullable CompoundTag loadNBTFromFile(String name) {
        InputStream resource = this.getResource(name);
        if (resource == null) {
            return null;
        }

        try (InputStream stream = new BufferedInputStream(resource)) {
            return MAPPINGS_READER.read(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public @Nullable Mappings loadMappings(CompoundTag mappingsTag, String key) {
        return this.loadMappings(mappingsTag, key, size -> {
            int[] array = new int[size];
            Arrays.fill(array, -1);
            return array;
        }, (array, id, mappedId) -> array[id] = mappedId, IntArrayMappings::of);
    }

    @Beta
    public <M extends Mappings, V> @Nullable Mappings loadMappings(
        CompoundTag mappingsTag,
        String key,
        MappingDataLoader.MappingHolderSupplier<V> holderSupplier,
        MappingDataLoader.AddConsumer<V> addConsumer,
        MappingDataLoader.MappingsSupplier<M, V> mappingsSupplier
    ) {
        CompoundTag tag = mappingsTag.getCompoundTag(key);
        if (tag == null) {
            return null;
        }

        ByteTag serializationStragetyTag = tag.getUnchecked("id");
        IntTag mappedSizeTag = tag.getUnchecked("mappedSize");
        byte strategy = serializationStragetyTag.asByte();
        if (strategy == 0) {
            IntArrayTag valuesTag = tag.getIntArrayTag("val");
            return IntArrayMappings.of(valuesTag.getValue(), mappedSizeTag.asInt());
        }

        V mappings;
        if (strategy == 1) {
            IntArrayTag shiftsAtTag = tag.getIntArrayTag("at");
            IntArrayTag shiftsTag = tag.getIntArrayTag("to");
            IntTag sizeTag = tag.getUnchecked("size");
            int[] shiftsAt = shiftsAtTag.getValue();
            int[] shiftsTo = shiftsTag.getValue();
            int size = sizeTag.asInt();
            mappings = holderSupplier.get(size);
            if (shiftsAt[0] != 0) {
                int to = shiftsAt[0];

                for (int id = 0; id < to; id++) {
                    addConsumer.addTo(mappings, id, id);
                }
            }

            for (int i = 0; i < shiftsAt.length; i++) {
                int from = shiftsAt[i];
                int to = i == shiftsAt.length - 1 ? size : shiftsAt[i + 1];
                int mappedId = shiftsTo[i];

                for (int id = from; id < to; id++) {
                    addConsumer.addTo(mappings, id, mappedId++);
                }
            }
        } else {
            if (strategy != 2) {
                if (strategy == 3) {
                    IntTag sizeTag = tag.getUnchecked("size");
                    return new IdentityMappings(sizeTag.asInt(), mappedSizeTag.asInt());
                }

                throw new IllegalArgumentException("Unknown serialization strategy: " + strategy);
            }

            IntArrayTag changesAtTag = tag.getIntArrayTag("at");
            IntArrayTag valuesTag = tag.getIntArrayTag("val");
            IntTag sizeTag = tag.getUnchecked("size");
            boolean fillBetween = tag.get("nofill") == null;
            int[] changesAt = changesAtTag.getValue();
            int[] values = valuesTag.getValue();
            mappings = holderSupplier.get(sizeTag.asInt());

            for (int i = 0; i < changesAt.length; i++) {
                int id = changesAt[i];
                if (fillBetween) {
                    int previousId = i != 0 ? changesAt[i - 1] + 1 : 0;

                    for (int identity = previousId; identity < id; identity++) {
                        addConsumer.addTo(mappings, identity, identity);
                    }
                }

                addConsumer.addTo(mappings, id, values[i]);
            }
        }

        return mappingsSupplier.create(mappings, mappedSizeTag.asInt());
    }

    public FullMappings loadFullMappings(
        CompoundTag mappingsTag, CompoundTag unmappedIdentifiersTag, CompoundTag mappedIdentifiersTag, String key
    ) {
        if (unmappedIdentifiersTag.contains(key) && mappedIdentifiersTag.contains(key)) {
            List<String> unmappedIdentifiers = this.identifiersFromGlobalIds(unmappedIdentifiersTag, key);
            List<String> mappedIdentifiers = this.identifiersFromGlobalIds(mappedIdentifiersTag, key);
            Mappings mappings = this.loadMappings(mappingsTag, key);
            if (mappings == null) {
                mappings = new IdentityMappings(unmappedIdentifiers.size(), mappedIdentifiers.size());
            }

            return new FullMappingsBase(unmappedIdentifiers, mappedIdentifiers, mappings);
        } else {
            return null;
        }
    }

    public @Nullable List<String> identifiersFromGlobalIds(CompoundTag mappingsTag, String key) {
        Mappings mappings = this.loadMappings(mappingsTag, key);
        if (mappings == null) {
            return null;
        }

        List<String> identifiers = new ArrayList<>(mappings.size());

        for (int i = 0; i < mappings.size(); i++) {
            identifiers.add(this.identifierFromGlobalId(key, mappings.getNewId(i)));
        }

        return identifiers;
    }

    public Object2IntMap<String> indexedObjectToMap(JsonObject object) {
        Object2IntMap<String> map = new Object2IntOpenHashMap<>(object.size(), 0.99F);
        map.defaultReturnValue(-1);

        for (Entry<String, JsonElement> entry : object.entrySet()) {
            map.put(entry.getValue().getAsString(), Integer.parseInt(entry.getKey()));
        }

        return map;
    }

    public Object2IntMap<String> arrayToMap(JsonArray array) {
        Object2IntMap<String> map = new Object2IntOpenHashMap<>(array.size(), 0.99F);
        map.defaultReturnValue(-1);

        for (int i = 0; i < array.size(); i++) {
            map.put(array.get(i).getAsString(), i);
        }

        return map;
    }

    public Logger getLogger() {
        return Via.getPlatform().getLogger();
    }

    public File getDataFolder() {
        return Via.getPlatform().getDataFolder();
    }

    public @Nullable InputStream getResource(String name) {
        return this.dataLoaderClass.getClassLoader().getResourceAsStream(this.dataPath + name);
    }

    @FunctionalInterface
    public interface AddConsumer<T> {
        void addTo(T var1, int var2, int var3);
    }

    @FunctionalInterface
    public interface MappingHolderSupplier<T> {
        T get(int var1);
    }

    @FunctionalInterface
    public interface MappingsSupplier<T extends Mappings, V> {
        T create(V var1, int var2);
    }
}
