package com.viaversion.viaversion.util;

import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TagUtil {
    public static ListTag<CompoundTag> getRegistryEntries(CompoundTag tag, String key) {
        CompoundTag registry = tag.getCompoundTag(Key.namespaced(key));
        if (registry == null) {
            registry = tag.getCompoundTag(Key.stripMinecraftNamespace(key));
        }

        return registry.getListTag("value", CompoundTag.class);
    }

    public static Tag handleDeep(Tag tag, TagUtil.TagUpdater consumer) {
        return handleDeep(null, tag, consumer);
    }

    private static Tag handleDeep(@Nullable String key, Tag tag, TagUtil.TagUpdater consumer) {
        if (tag instanceof CompoundTag) {
            CompoundTag compoundTag = (CompoundTag)tag;

            for (Entry<String, Tag> entry : compoundTag.entrySet()) {
                Tag updatedTag = handleDeep(entry.getKey(), entry.getValue(), consumer);
                entry.setValue(updatedTag);
            }
        } else if (tag instanceof ListTag) {
            handleListTag((ListTag)tag, consumer);
        }

        return consumer.update(key, tag);
    }

    private static <T extends Tag> void handleListTag(ListTag<T> listTag, TagUtil.TagUpdater consumer) {
        listTag.getValue().replaceAll(t -> handleDeep(null, t, consumer));
    }

    @FunctionalInterface
    public interface TagUpdater {
        Tag update(@Nullable String var1, Tag var2);
    }
}
