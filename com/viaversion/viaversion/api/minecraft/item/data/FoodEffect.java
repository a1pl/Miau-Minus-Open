package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.ArrayType;
import io.netty.buffer.ByteBuf;

public final class FoodEffect {
    public static final Type<FoodEffect> TYPE = new Type<FoodEffect>(FoodEffect.class) {
        public FoodEffect read(ByteBuf buffer) throws Exception {
            PotionEffect effect = PotionEffect.TYPE.read(buffer);
            float probability = buffer.readFloat();
            return new FoodEffect(effect, probability);
        }

        public void write(ByteBuf buffer, FoodEffect value) throws Exception {
            PotionEffect.TYPE.write(buffer, value.effect);
            buffer.writeFloat(value.probability);
        }
    };
    public static final Type<FoodEffect[]> ARRAY_TYPE = new ArrayType<>(TYPE);
    private final PotionEffect effect;
    private final float probability;

    public FoodEffect(PotionEffect effect, float probability) {
        this.effect = effect;
        this.probability = probability;
    }

    public PotionEffect effect() {
        return this.effect;
    }

    public float probability() {
        return this.probability;
    }
}
