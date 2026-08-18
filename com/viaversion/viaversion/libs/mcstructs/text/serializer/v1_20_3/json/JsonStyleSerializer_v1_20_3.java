package com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_3.json;

import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.mcstructs.core.Identifier;
import com.viaversion.viaversion.libs.mcstructs.core.TextFormatting;
import com.viaversion.viaversion.libs.mcstructs.text.Style;
import com.viaversion.viaversion.libs.mcstructs.text.events.click.ClickEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.click.ClickEventAction;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.AHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.ITypedSerializer;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_3.CodecUtils_v1_20_3;
import java.util.function.Function;

public class JsonStyleSerializer_v1_20_3 implements ITypedSerializer<JsonElement, Style>, CodecUtils_v1_20_3 {
    private final ITypedSerializer<JsonElement, AHoverEvent> hoverEventSerializer;

    public JsonStyleSerializer_v1_20_3(
        Function<JsonStyleSerializer_v1_20_3, ITypedSerializer<JsonElement, AHoverEvent>> hoverEventSerializer
    ) {
        this.hoverEventSerializer = hoverEventSerializer.apply(this);
    }

    public JsonElement serialize(Style object) {
        JsonObject out = new JsonObject();
        if (object.getColor() != null) {
            out.addProperty("color", object.getColor().serialize());
        }

        if (object.getBold() != null) {
            out.addProperty("bold", object.isBold());
        }

        if (object.getItalic() != null) {
            out.addProperty("italic", object.isItalic());
        }

        if (object.getUnderlined() != null) {
            out.addProperty("underlined", object.isUnderlined());
        }

        if (object.getStrikethrough() != null) {
            out.addProperty("strikethrough", object.isStrikethrough());
        }

        if (object.getObfuscated() != null) {
            out.addProperty("obfuscated", object.isObfuscated());
        }

        if (object.getClickEvent() != null) {
            JsonObject clickEvent = new JsonObject();
            clickEvent.addProperty("action", object.getClickEvent().getAction().getName());
            clickEvent.addProperty("value", object.getClickEvent().getValue());
            out.add("clickEvent", clickEvent);
        }

        if (object.getHoverEvent() != null) {
            out.add("hoverEvent", this.hoverEventSerializer.serialize(object.getHoverEvent()));
        }

        if (object.getInsertion() != null) {
            out.addProperty("insertion", object.getInsertion());
        }

        if (object.getFont() != null) {
            out.addProperty("font", object.getFont().get());
        }

        return out;
    }

    public Style deserialize(JsonElement object) {
        if (!object.isJsonObject()) {
            throw new IllegalArgumentException("Json element is not a json object");
        }

        JsonObject obj = object.getAsJsonObject();
        Style style = new Style();
        if (obj.has("color")) {
            String color = this.requiredString(obj, "color");
            TextFormatting formatting = TextFormatting.parse(color);
            if (formatting == null) {
                throw new IllegalArgumentException("Unknown color: " + color);
            }

            if (formatting.isRGBColor() && (formatting.getRgbValue() < 0 || formatting.getRgbValue() > 16777215)) {
                throw new IllegalArgumentException("Out of bounds RGB color: " + formatting.getRgbValue());
            }

            style.setFormatting(formatting);
        }

        style.setBold(this.optionalBoolean(obj, "bold"));
        style.setItalic(this.optionalBoolean(obj, "italic"));
        style.setUnderlined(this.optionalBoolean(obj, "underlined"));
        style.setStrikethrough(this.optionalBoolean(obj, "strikethrough"));
        style.setObfuscated(this.optionalBoolean(obj, "obfuscated"));
        if (obj.has("clickEvent")) {
            JsonObject clickEvent = this.requiredObject(obj, "clickEvent");
            ClickEventAction action = ClickEventAction.getByName(this.requiredString(clickEvent, "action"), false);
            if (action == null || ClickEventAction.TWITCH_USER_INFO.equals(action)) {
                throw new IllegalArgumentException(
                    "Unknown click event action: " + clickEvent.get("action").getAsString()
                );
            }

            if (!action.isUserDefinable()) {
                throw new IllegalArgumentException("Click event action is not user definable: " + action);
            }

            style.setClickEvent(new ClickEvent(action, this.requiredString(clickEvent, "value")));
        }

        if (obj.has("hoverEvent")) {
            style.setHoverEvent(this.hoverEventSerializer.deserialize(this.requiredObject(obj, "hoverEvent")));
        }

        style.setInsertion(this.optionalString(obj, "insertion"));
        if (obj.has("font")) {
            style.setFont(Identifier.of(this.requiredString(obj, "font")));
        }

        return style;
    }
}
