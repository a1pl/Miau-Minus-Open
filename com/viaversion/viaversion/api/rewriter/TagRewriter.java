package com.viaversion.viaversion.api.rewriter;

import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.TagData;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface TagRewriter extends MappingDataListener {
    void removeTags(String var1);

    void renameTag(RegistryType var1, String var2, String var3);

    void addEmptyTag(RegistryType var1, String var2);

    void addEmptyTags(RegistryType var1, String... var2);

    void addEntityTag(String var1, EntityType... var2);

    void addTag(RegistryType var1, String var2, int... var3);

    void addTagRaw(RegistryType var1, String var2, int... var3);

    @Nullable List<TagData> getNewTags(RegistryType var1);

    List<TagData> getOrComputeNewTags(RegistryType var1);
}
