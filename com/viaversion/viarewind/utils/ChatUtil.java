package com.viaversion.viarewind.utils;

import com.viaversion.viarewind.ViaRewind;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonParser;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viaversion.rewriter.ComponentRewriter;
import com.viaversion.viaversion.util.ComponentUtil;
import java.util.logging.Level;
import java.util.regex.Pattern;

@Deprecated
public class ChatUtil {
    private static final Pattern UNUSED_COLOR_PATTERN = Pattern.compile(
        "(?>(?>§[0-fk-or])*(§r|\\Z))|(?>(?>§[0-f])*(§[0-f]))"
    );
    private static final ComponentRewriter<ClientboundPacketType> LEGACY_REWRITER = new ComponentRewriter<ClientboundPacketType>(
        null, ComponentRewriter.ReadType.JSON
    ) {
        @Override
        protected void handleTranslate(JsonObject object, String translate) {
            String text = Protocol1_13To1_12_2.MAPPINGS.getMojangTranslation().get(translate);
            if (text != null) {
                object.addProperty("translate", text);
            }
        }
    };

    public static String jsonToLegacy(UserConnection connection, String json) {
        if (json != null && !json.equals("null") && !json.isEmpty()) {
            try {
                return jsonToLegacy(connection, JsonParser.parseString(json));
            } catch (Exception e) {
                ViaRewind.getPlatform()
                    .getLogger()
                    .log(Level.WARNING, "Could not convert component to legacy text: " + json, e);
                return "";
            }
        } else {
            return "";
        }
    }

    public static String jsonToLegacy(UserConnection connection, JsonElement component) {
        if (!component.isJsonNull()
            && (!component.isJsonArray() || !component.getAsJsonArray().isEmpty())
            && (!component.isJsonObject() || !component.getAsJsonObject().isEmpty())) {
            if (component.isJsonPrimitive()) {
                return component.getAsString();
            }

            try {
                LEGACY_REWRITER.processText(connection, component);
                String legacy = ComponentUtil.jsonToLegacy(component);

                while (legacy.startsWith("§f")) {
                    legacy = legacy.substring(2);
                }

                return legacy;
            } catch (Exception ex) {
                ViaRewind.getPlatform()
                    .getLogger()
                    .log(Level.WARNING, "Could not convert component to legacy text: " + component, ex);
                return "";
            }
        } else {
            return "";
        }
    }

    public static String removeUnusedColor(String legacy, char last) {
        if (legacy == null) {
            return null;
        }

        legacy = UNUSED_COLOR_PATTERN.matcher(legacy).replaceAll("$1$2");
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < legacy.length(); i++) {
            char current = legacy.charAt(i);
            if (current == 167 && i != legacy.length() - 1) {
                current = legacy.charAt(++i);
                if (current != last) {
                    builder.append('§').append(current);
                    last = current;
                }
            } else {
                builder.append(current);
            }
        }

        return builder.toString();
    }
}
