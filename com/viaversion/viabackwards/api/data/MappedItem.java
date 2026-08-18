package com.viaversion.viabackwards.api.data;

import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.util.ComponentUtil;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MappedItem {
    private final int id;
    private final String jsonName;
    private final Tag tagName;
    private final Integer customModelData;

    public MappedItem(int id, String name) {
        this(id, name, null);
    }

    public MappedItem(int id, String name, @Nullable Integer customModelData) {
        this.id = id;
        this.jsonName = ComponentUtil.legacyToJsonString("§f" + name, true);
        this.tagName = ComponentUtil.jsonStringToTag(this.jsonName);
        this.customModelData = customModelData;
    }

    public int id() {
        return this.id;
    }

    public String jsonName() {
        return this.jsonName;
    }

    public Tag tagName() {
        return this.tagName;
    }

    public @Nullable Integer customModelData() {
        return this.customModelData;
    }
}
