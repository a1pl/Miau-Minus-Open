package com.viaversion.viaversion.protocols.protocol1_12to1_11_1.rewriter;

import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.SerializerVersion;

public final class ChatItemRewriter {
    public static void toClient(JsonElement element) {
        if (element instanceof JsonObject) {
            JsonObject obj = (JsonObject)element;
            if (obj.has("hoverEvent")) {
                if (!(obj.get("hoverEvent") instanceof JsonObject)) {
                    return;
                }

                JsonObject hoverEvent = (JsonObject)obj.get("hoverEvent");
                if (!hoverEvent.has("action") || !hoverEvent.has("value")) {
                    return;
                }

                String type = hoverEvent.get("action").getAsString();
                JsonElement value = hoverEvent.get("value");
                if (type.equals("show_item")) {
                    CompoundTag compound = ComponentUtil.deserializeLegacyShowItem(value, SerializerVersion.V1_8);
                    hoverEvent.addProperty("value", SerializerVersion.V1_12.toSNBT(compound));
                }
            } else if (obj.has("extra")) {
                toClient(obj.get("extra"));
            }
        } else if (element instanceof JsonArray) {
            for (JsonElement value : (JsonArray)element) {
                toClient(value);
            }
        }
    }
}
