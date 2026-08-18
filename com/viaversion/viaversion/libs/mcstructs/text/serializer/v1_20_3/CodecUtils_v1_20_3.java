package com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_3;

import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface CodecUtils_v1_20_3 {
    default List<Tag> unwrapMarkers(ListTag<?> list) {
        List<Tag> tags = new ArrayList<>((Collection<? extends Tag>)list.getValue());
        if (CompoundTag.class == list.getElementType()) {
            for (int i = 0; i < tags.size(); i++) {
                CompoundTag compound = (CompoundTag)tags.get(i);
                if (compound.size() == 1) {
                    Tag wrapped = compound.get("");
                    if (wrapped != null) {
                        tags.set(i, wrapped);
                    }
                }
            }
        }

        return tags;
    }

    default boolean isString(@Nullable JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }

    default boolean isNumber(@Nullable JsonElement element) {
        return element != null
            && element.isJsonPrimitive()
            && (element.getAsJsonPrimitive().isNumber() || element.getAsJsonPrimitive().isBoolean());
    }

    default boolean isObject(@Nullable JsonElement element) {
        return element != null && element.isJsonObject();
    }

    default boolean containsString(JsonObject obj, String name) {
        return obj.has(name) && this.isString(obj.get(name));
    }

    default boolean containsArray(JsonObject obj, String name) {
        return obj.has(name) && obj.get(name).isJsonArray();
    }

    default boolean containsObject(JsonObject obj, String name) {
        return obj.has(name) && this.isObject(obj.get(name));
    }

    @Nullable
    default Boolean optionalBoolean(CompoundTag tag, String name) {
        return !tag.contains(name) ? null : this.requiredBoolean(tag, name);
    }

    @Nullable
    default Boolean optionalBoolean(JsonObject obj, String name) {
        return !obj.has(name) ? null : this.requiredBoolean(obj, name);
    }

    @Nullable
    default Integer optionalInt(CompoundTag tag, String name) {
        return !tag.contains(name) ? null : this.requiredInt(tag, name);
    }

    @Nullable
    default Integer optionalInt(JsonObject obj, String name) {
        return !obj.has(name) ? null : this.requiredInt(obj, name);
    }

    @Nullable
    default String optionalString(CompoundTag tag, String name) {
        return !tag.contains(name) ? null : this.requiredString(tag, name);
    }

    @Nullable
    default String optionalString(JsonObject obj, String name) {
        return !obj.has(name) ? null : this.requiredString(obj, name);
    }

    @Nullable
    default CompoundTag optionalCompound(CompoundTag tag, String name) {
        return !tag.contains(name) ? null : this.requiredCompound(tag, name);
    }

    @Nullable
    default JsonObject optionalObject(JsonObject obj, String name) {
        return !obj.has(name) ? null : this.requiredObject(obj, name);
    }

    default boolean requiredBoolean(CompoundTag tag, String name) {
        if (!(tag.get(name) instanceof ByteTag)) {
            throw new IllegalArgumentException("Expected byte tag for '" + name + "' tag");
        } else {
            return tag.get(name) instanceof ByteTag ? tag.<ByteTag>get(name).asBoolean() : false;
        }
    }

    default boolean requiredBoolean(JsonObject obj, String name) {
        if (!obj.has(name)) {
            throw new IllegalArgumentException("Missing boolean for '" + name + "' tag");
        } else {
            JsonElement element = obj.get(name);
            if (!element.isJsonPrimitive()) {
                throw new IllegalArgumentException("Expected boolean for '" + name + "' tag");
            } else {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isBoolean()) {
                    return primitive.getAsBoolean();
                } else if (primitive.isNumber()) {
                    return primitive.getAsInt() != 0;
                } else {
                    throw new IllegalArgumentException("Expected boolean for '" + name + "' tag");
                }
            }
        }
    }

    default int requiredInt(CompoundTag tag, String name) {
        if (!(tag.get(name) instanceof IntTag)) {
            throw new IllegalArgumentException("Expected int tag for '" + name + "' tag");
        } else {
            return tag.get(name) instanceof IntTag ? tag.<IntTag>get(name).asInt() : 0;
        }
    }

    default int requiredInt(JsonObject obj, String name) {
        if (!obj.has(name)) {
            throw new IllegalArgumentException("Missing int for '" + name + "' tag");
        } else {
            JsonElement element = obj.get(name);
            if (!element.isJsonPrimitive()) {
                throw new IllegalArgumentException("Expected int for '" + name + "' tag");
            } else {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isNumber()) {
                    return primitive.getAsInt();
                } else if (primitive.isBoolean()) {
                    return primitive.getAsBoolean() ? 1 : 0;
                } else {
                    throw new IllegalArgumentException("Expected int for '" + name + "' tag");
                }
            }
        }
    }

    default String requiredString(CompoundTag tag, String name) {
        if (!(tag.get(name) instanceof StringTag)) {
            throw new IllegalArgumentException("Expected string tag for '" + name + "' tag");
        } else {
            return tag.get(name) instanceof StringTag ? tag.<StringTag>get(name).getValue() : "";
        }
    }

    default String requiredString(JsonObject obj, String name) {
        if (!obj.has(name)) {
            throw new IllegalArgumentException("Missing string for '" + name + "' tag");
        } else {
            JsonElement element = obj.get(name);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                return element.getAsString();
            } else {
                throw new IllegalArgumentException("Expected string for '" + name + "' tag");
            }
        }
    }

    default CompoundTag requiredCompound(CompoundTag tag, String name) {
        if (!(tag.get(name) instanceof CompoundTag)) {
            throw new IllegalArgumentException("Expected compound tag for '" + name + "' tag");
        } else {
            return tag.get(name) instanceof CompoundTag ? tag.get(name) : new CompoundTag();
        }
    }

    default JsonObject requiredObject(JsonObject obj, String name) {
        if (!obj.has(name)) {
            throw new IllegalArgumentException("Missing object for '" + name + "' tag");
        } else {
            JsonElement element = obj.get(name);
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("Expected object for '" + name + "' tag");
            } else {
                return element.getAsJsonObject();
            }
        }
    }
}
