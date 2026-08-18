package com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl;

import com.viaversion.viaversion.libs.mcstructs.core.utils.ToString;
import com.viaversion.viaversion.libs.mcstructs.snbt.SNbtSerializer;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.AHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.HoverEventAction;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.TextComponentSerializer;
import java.util.Objects;

public class TextHoverEvent extends AHoverEvent {
    private ATextComponent text;

    public TextHoverEvent(HoverEventAction action, ATextComponent text) {
        super(action);
        this.text = text;
    }

    public TextHoverEvent setAction(HoverEventAction action) {
        this.action = action;
        return this;
    }

    public ATextComponent getText() {
        return this.text;
    }

    public TextHoverEvent setText(ATextComponent text) {
        this.text = text;
        return this;
    }

    @Override
    public TextHoverEvent toLegacy(TextComponentSerializer textComponentSerializer, SNbtSerializer<?> sNbtSerializer) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            TextHoverEvent that = (TextHoverEvent)o;
            return Objects.equals(this.text, that.text);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.text);
    }

    @Override
    public String toString() {
        return ToString.of(this).add("text", this.text).toString();
    }
}
