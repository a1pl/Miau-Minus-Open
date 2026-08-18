package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public final class AttributeModifiers {
    public static final Type<AttributeModifiers> TYPE = new Type<AttributeModifiers>(AttributeModifiers.class) {
        public AttributeModifiers read(ByteBuf buffer) throws Exception {
            AttributeModifier[] modifiers = AttributeModifier.ARRAY_TYPE.read(buffer);
            boolean showInTooltip = buffer.readBoolean();
            return new AttributeModifiers(modifiers, showInTooltip);
        }

        public void write(ByteBuf buffer, AttributeModifiers value) throws Exception {
            AttributeModifier.ARRAY_TYPE.write(buffer, value.modifiers());
            buffer.writeBoolean(value.showInTooltip());
        }
    };
    private final AttributeModifier[] modifiers;
    private final boolean showInTooltip;

    public AttributeModifiers(AttributeModifier[] modifiers, boolean showInTooltip) {
        this.modifiers = modifiers;
        this.showInTooltip = showInTooltip;
    }

    public AttributeModifier[] modifiers() {
        return this.modifiers;
    }

    public boolean showInTooltip() {
        return this.showInTooltip;
    }
}
