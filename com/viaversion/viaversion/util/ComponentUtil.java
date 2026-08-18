package com.viaversion.viaversion.util;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.Style;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.AHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.TextHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.LegacyStringDeserializer;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.TextComponentSerializer;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ComponentUtil {
    public static JsonObject emptyJsonComponent() {
        return plainToJson("");
    }

    public static String emptyJsonComponentString() {
        return "{\"text\":\"\"}";
    }

    public static JsonObject plainToJson(String message) {
        JsonObject object = new JsonObject();
        object.addProperty("text", message);
        return object;
    }

    public static @Nullable JsonElement tagToJson(@Nullable Tag tag) {
        try {
            ATextComponent component = SerializerVersion.V1_20_3.toComponent(tag);
            return component != null ? SerializerVersion.V1_19_4.toJson(component) : null;
        } catch (Exception e) {
            Via.getPlatform().getLogger().log(Level.SEVERE, "Error converting tag: " + tag, e);
            return plainToJson("<error>");
        }
    }

    public static @Nullable Tag jsonToTag(@Nullable JsonElement element) {
        if (element != null && !element.isJsonNull()) {
            try {
                ATextComponent component = SerializerVersion.V1_19_4.toComponent(element);
                return trimStrings(SerializerVersion.V1_20_3.toTag(component));
            } catch (Exception e) {
                Via.getPlatform().getLogger().log(Level.SEVERE, "Error converting component: " + element, e);
                return new StringTag("<error>");
            }
        } else {
            return null;
        }
    }

    private static Tag trimStrings(Tag input) {
        return input == null ? null : TagUtil.handleDeep(input, (key, tag) -> {
            if (tag instanceof StringTag) {
                String value = ((StringTag)tag).getValue();
                if (value.length() > 32767) {
                    ((StringTag)tag).setValue("{}");
                }
            }

            return tag;
        });
    }

    public static @Nullable String tagToJsonString(@Nullable Tag tag) {
        try {
            ATextComponent component = SerializerVersion.V1_20_5.toComponent(tag);
            return component != null ? SerializerVersion.V1_20_3.toString(component) : null;
        } catch (Exception e) {
            Via.getPlatform().getLogger().log(Level.SEVERE, "Error converting tag: " + tag, e);
            return plainToJson("<error>").toString();
        }
    }

    public static @Nullable Tag jsonStringToTag(@Nullable String json) {
        return jsonStringToTag(json, SerializerVersion.V1_20_3, SerializerVersion.V1_20_5);
    }

    public static @Nullable Tag jsonStringToTag(@Nullable String json, SerializerVersion from, SerializerVersion to) {
        return json == null ? null : to.toTag(from.jsonSerializer.deserialize(json));
    }

    public static @Nullable JsonElement convertJson(
        @Nullable JsonElement element, SerializerVersion from, SerializerVersion to
    ) {
        return element != null ? convert(from, to, from.toComponent(element)) : null;
    }

    public static @Nullable JsonElement convertJson(@Nullable String json, SerializerVersion from, SerializerVersion to) {
        return json != null ? convert(from, to, from.toComponent(json)) : null;
    }

    public static @Nullable JsonElement convertJsonOrEmpty(
        @Nullable String json, SerializerVersion from, SerializerVersion to
    ) {
        ATextComponent component = from.toComponent(json);
        return component == null ? emptyJsonComponent() : to.toJson(component);
    }

    private static JsonElement convert(SerializerVersion from, SerializerVersion to, ATextComponent component) {
        if (from.ordinal() >= SerializerVersion.V1_16.ordinal() && to.ordinal() < SerializerVersion.V1_16.ordinal()) {
            Style style = component.getStyle();
            AHoverEvent hoverEvent = style.getHoverEvent();
            if (hoverEvent != null && !(hoverEvent instanceof TextHoverEvent)) {
                style.setHoverEvent(hoverEvent.toLegacy(to.jsonSerializer, to.snbtSerializer));
            }
        }

        return to.toJson(component);
    }

    public static JsonElement legacyToJson(String message) {
        return SerializerVersion.V1_12.toJson(LegacyStringDeserializer.parse(message, true));
    }

    public static String legacyToJsonString(String message) {
        return legacyToJsonString(message, false);
    }

    public static String legacyToJsonString(String message, boolean itemData) {
        ATextComponent component = LegacyStringDeserializer.parse(message, true);
        if (itemData) {
            component.setParentStyle(new Style().setItalic(false));
        }

        return SerializerVersion.V1_12.toString(component);
    }

    public static String jsonToLegacy(String value) {
        return TextComponentSerializer.V1_12.deserializeReader(value).asLegacyFormatString();
    }

    public static String jsonToLegacy(JsonElement value) {
        return SerializerVersion.V1_12.toComponent(value).asLegacyFormatString();
    }

    public static CompoundTag deserializeLegacyShowItem(JsonElement element, SerializerVersion version) {
        return (CompoundTag)version.toTag(version.toComponent(element).asUnformattedString());
    }

    public static CompoundTag deserializeShowItem(Tag value, SerializerVersion version) {
        return (CompoundTag)version.toTag(version.toComponent(value).asUnformattedString());
    }
}
