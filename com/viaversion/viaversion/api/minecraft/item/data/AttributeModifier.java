package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.ArrayType;
import io.netty.buffer.ByteBuf;

public final class AttributeModifier {
    public static final Type<AttributeModifier> TYPE = new Type<AttributeModifier>(AttributeModifier.class) {
        public AttributeModifier read(ByteBuf buffer) throws Exception {
            int attribute = Type.VAR_INT.readPrimitive(buffer);
            ModifierData modifier = ModifierData.TYPE.read(buffer);
            int slot = Type.VAR_INT.readPrimitive(buffer);
            return new AttributeModifier(attribute, modifier, slot);
        }

        public void write(ByteBuf buffer, AttributeModifier value) throws Exception {
            Type.VAR_INT.writePrimitive(buffer, value.attribute);
            ModifierData.TYPE.write(buffer, value.modifier);
            Type.VAR_INT.writePrimitive(buffer, value.slotType);
        }
    };
    public static final Type<AttributeModifier[]> ARRAY_TYPE = new ArrayType<>(TYPE);
    private final int attribute;
    private final ModifierData modifier;
    private final int slotType;

    public AttributeModifier(int attribute, ModifierData modifier, int slotType) {
        this.attribute = attribute;
        this.modifier = modifier;
        this.slotType = slotType;
    }

    public int attribute() {
        return this.attribute;
    }

    public ModifierData modifier() {
        return this.modifier;
    }

    public int slotType() {
        return this.slotType;
    }
}
