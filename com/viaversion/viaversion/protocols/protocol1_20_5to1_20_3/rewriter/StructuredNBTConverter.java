package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.rewriter;

import com.google.common.base.Preconditions;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class StructuredNBTConverter {
    private static final Map<String, StructuredNBTConverter.DataConverter<?>> rewriters = new HashMap<>();

    public static List<StructuredData<?>> toData(CompoundTag tag) {
        List<StructuredData<?>> data = new ArrayList<>();

        for (Entry<String, Tag> entry : tag.entrySet()) {
            StructuredData<?> structuredData = readFromTag(entry.getKey(), entry.getValue());
            data.add(structuredData);
        }

        return data;
    }

    public static <T> StructuredData<T> readFromTag(String identifier, Tag tag) {
        StructuredNBTConverter.DataConverter<T> converter = (StructuredNBTConverter.DataConverter<T>)rewriters.get(
            identifier
        );
        Preconditions.checkNotNull(converter, "No converter for %s found", new Object[]{identifier});
        return (StructuredData<T>)converter.convert(tag);
    }

    private static <T> void register(StructuredDataKey<T> key, StructuredNBTConverter.DataConverter<T> converter) {
        rewriters.put(key.identifier(), converter);
    }

    static {
        register(StructuredDataKey.CUSTOM_DATA, tag -> (CompoundTag)tag);
    }

    @FunctionalInterface
    interface DataConverter<T> {
        T convert(Tag var1);
    }
}
