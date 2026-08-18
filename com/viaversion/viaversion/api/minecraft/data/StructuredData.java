package com.viaversion.viaversion.api.minecraft.data;

import com.viaversion.viaversion.util.IdHolder;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface StructuredData<T> extends IdHolder {
    static <T> StructuredData<T> of(StructuredDataKey<T> key, T value, int id) {
        return new FilledStructuredData<>(key, value, id);
    }

    static <T> StructuredData<T> empty(StructuredDataKey<T> key, int id) {
        return new EmptyStructuredData<>(key, id);
    }

    void setValue(T var1);

    void write(ByteBuf var1) throws Exception;

    void setId(int var1);

    StructuredDataKey<T> key();

    @Nullable T value();

    default boolean isPresent() {
        return !this.isEmpty();
    }

    boolean isEmpty();
}
