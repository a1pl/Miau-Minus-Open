package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public final class FoodProperties {
    public static final Type<FoodProperties> TYPE = new Type<FoodProperties>(FoodProperties.class) {
        public FoodProperties read(ByteBuf buffer) throws Exception {
            int nutrition = Type.VAR_INT.readPrimitive(buffer);
            float saturationModifier = buffer.readFloat();
            boolean canAlwaysEat = buffer.readBoolean();
            float eatSeconds = buffer.readFloat();
            FoodEffect[] possibleEffects = FoodEffect.ARRAY_TYPE.read(buffer);
            return new FoodProperties(nutrition, saturationModifier, canAlwaysEat, eatSeconds, possibleEffects);
        }

        public void write(ByteBuf buffer, FoodProperties value) throws Exception {
            Type.VAR_INT.writePrimitive(buffer, value.nutrition);
            buffer.writeFloat(value.saturationModifier);
            buffer.writeBoolean(value.canAlwaysEat);
            buffer.writeFloat(value.eatSeconds);
            FoodEffect.ARRAY_TYPE.write(buffer, value.possibleEffects);
        }
    };
    private final int nutrition;
    private final float saturationModifier;
    private final boolean canAlwaysEat;
    private final float eatSeconds;
    private final FoodEffect[] possibleEffects;

    public FoodProperties(
        int nutrition, float saturationModifier, boolean canAlwaysEat, float eatSeconds, FoodEffect[] possibleEffects
    ) {
        this.nutrition = nutrition;
        this.saturationModifier = saturationModifier;
        this.canAlwaysEat = canAlwaysEat;
        this.eatSeconds = eatSeconds;
        this.possibleEffects = possibleEffects;
    }

    public int nutrition() {
        return this.nutrition;
    }

    public float saturationModifier() {
        return this.saturationModifier;
    }

    public boolean canAlwaysEat() {
        return this.canAlwaysEat;
    }

    public float eatSeconds() {
        return this.eatSeconds;
    }

    public FoodEffect[] possibleEffects() {
        return this.possibleEffects;
    }
}
