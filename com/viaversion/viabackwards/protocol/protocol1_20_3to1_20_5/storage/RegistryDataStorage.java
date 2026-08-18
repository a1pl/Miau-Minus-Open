package com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class RegistryDataStorage implements StorableObject {
    private final CompoundTag registryData = new CompoundTag();
    private String[] dimensionKeys;

    public CompoundTag registryData() {
        return this.registryData;
    }

    public String @Nullable [] dimensionKeys() {
        return this.dimensionKeys;
    }

    public void setDimensionKeys(String[] dimensionKeys) {
        this.dimensionKeys = dimensionKeys;
    }

    public void clear() {
        this.registryData.clear();
        this.dimensionKeys = null;
    }
}
