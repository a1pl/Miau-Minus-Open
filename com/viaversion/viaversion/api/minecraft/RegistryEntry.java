package com.viaversion.viaversion.api.minecraft;

import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class RegistryEntry {
    private final String key;
    private final Tag tag;

    public RegistryEntry(String key, @Nullable Tag tag) {
        this.key = key;
        this.tag = tag;
    }

    public String key() {
        return this.key;
    }

    public @Nullable Tag tag() {
        return this.tag;
    }

    public RegistryEntry withKey(String key) {
        return new RegistryEntry(key, this.tag != null ? this.tag.copy() : null);
    }
}
