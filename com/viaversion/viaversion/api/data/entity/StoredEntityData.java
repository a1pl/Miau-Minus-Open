package com.viaversion.viaversion.api.data.entity;

import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface StoredEntityData {
    EntityType type();

    boolean has(Class<?> var1);

    <T> @Nullable T get(Class<T> var1);

    <T> @Nullable T remove(Class<T> var1);

    void put(Object var1);
}
