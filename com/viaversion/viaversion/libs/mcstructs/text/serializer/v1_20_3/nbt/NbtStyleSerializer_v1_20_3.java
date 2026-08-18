package com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_3.nbt;

import com.viaversion.viaversion.libs.mcstructs.core.Identifier;
import com.viaversion.viaversion.libs.mcstructs.core.TextFormatting;
import com.viaversion.viaversion.libs.mcstructs.text.Style;
import com.viaversion.viaversion.libs.mcstructs.text.events.click.ClickEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.click.ClickEventAction;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.AHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.ITypedSerializer;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_3.CodecUtils_v1_20_3;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import java.util.function.Function;

public class NbtStyleSerializer_v1_20_3 implements ITypedSerializer<Tag, Style>, CodecUtils_v1_20_3 {
    private final ITypedSerializer<Tag, AHoverEvent> hoverEventSerializer;

    public NbtStyleSerializer_v1_20_3(
        Function<NbtStyleSerializer_v1_20_3, ITypedSerializer<Tag, AHoverEvent>> hoverEventSerializer
    ) {
        this.hoverEventSerializer = hoverEventSerializer.apply(this);
    }

    public Tag serialize(Style object) {
        CompoundTag out = new CompoundTag();
        if (object.getColor() != null) {
            out.putString("color", object.getColor().serialize());
        }

        if (object.getBold() != null) {
            out.putBoolean("bold", object.isBold());
        }

        if (object.getItalic() != null) {
            out.putBoolean("italic", object.isItalic());
        }

        if (object.getUnderlined() != null) {
            out.putBoolean("underlined", object.isUnderlined());
        }

        if (object.getStrikethrough() != null) {
            out.putBoolean("strikethrough", object.isStrikethrough());
        }

        if (object.getObfuscated() != null) {
            out.putBoolean("obfuscated", object.isObfuscated());
        }

        if (object.getClickEvent() != null) {
            CompoundTag clickEvent = new CompoundTag();
            clickEvent.putString("action", object.getClickEvent().getAction().getName());
            clickEvent.putString("value", object.getClickEvent().getValue());
            out.put("clickEvent", clickEvent);
        }

        if (object.getHoverEvent() != null) {
            out.put("hoverEvent", this.hoverEventSerializer.serialize(object.getHoverEvent()));
        }

        if (object.getInsertion() != null) {
            out.putString("insertion", object.getInsertion());
        }

        if (object.getFont() != null) {
            out.putString("font", object.getFont().get());
        }

        return out;
    }

    public Style deserialize(Tag object) {
        if (!(object instanceof CompoundTag)) {
            throw new IllegalArgumentException("Nbt tag is not a compound tag");
        }

        CompoundTag tag = (CompoundTag)object;
        Style style = new Style();
        if (tag.contains("color")) {
            String color = this.requiredString(tag, "color");
            TextFormatting formatting = TextFormatting.parse(color);
            if (formatting == null) {
                throw new IllegalArgumentException("Unknown color: " + color);
            }

            if (formatting.isRGBColor() && (formatting.getRgbValue() < 0 || formatting.getRgbValue() > 16777215)) {
                throw new IllegalArgumentException("Out of bounds RGB color: " + formatting.getRgbValue());
            }

            style.setFormatting(formatting);
        }

        style.setBold(this.optionalBoolean(tag, "bold"));
        style.setItalic(this.optionalBoolean(tag, "italic"));
        style.setUnderlined(this.optionalBoolean(tag, "underlined"));
        style.setStrikethrough(this.optionalBoolean(tag, "strikethrough"));
        style.setObfuscated(this.optionalBoolean(tag, "obfuscated"));
        if (tag.contains("clickEvent")) {
            CompoundTag clickEvent = this.requiredCompound(tag, "clickEvent");
            ClickEventAction action = ClickEventAction.getByName(this.requiredString(clickEvent, "action"), false);
            if (action == null || ClickEventAction.TWITCH_USER_INFO.equals(action)) {
                throw new IllegalArgumentException(
                    "Unknown click event action: "
                        + (
                            clickEvent.get("action") instanceof StringTag
                                ? clickEvent.<StringTag>get("action").getValue()
                                : ""
                        )
                );
            }

            if (!action.isUserDefinable()) {
                throw new IllegalArgumentException("Click event action is not user definable: " + action);
            }

            style.setClickEvent(new ClickEvent(action, this.requiredString(clickEvent, "value")));
        }

        if (tag.contains("hoverEvent")) {
            style.setHoverEvent(this.hoverEventSerializer.deserialize(this.requiredCompound(tag, "hoverEvent")));
        }

        style.setInsertion(this.optionalString(tag, "insertion"));
        if (tag.contains("font")) {
            style.setFont(Identifier.of(this.requiredString(tag, "font")));
        }

        return style;
    }
}
