package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.ArrayType;
import io.netty.buffer.ByteBuf;

public final class PotionEffect {
    public static final Type<PotionEffect> TYPE = new Type<PotionEffect>(PotionEffect.class) {
        public PotionEffect read(ByteBuf buffer) throws Exception {
            int effect = Type.VAR_INT.readPrimitive(buffer);
            PotionEffectData effectData = PotionEffectData.TYPE.read(buffer);
            return new PotionEffect(effect, effectData);
        }

        public void write(ByteBuf buffer, PotionEffect value) throws Exception {
            Type.VAR_INT.writePrimitive(buffer, value.effect);
            PotionEffectData.TYPE.write(buffer, value.effectData);
        }
    };
    public static final Type<PotionEffect[]> ARRAY_TYPE = new ArrayType<>(TYPE);
    private final int effect;
    private final PotionEffectData effectData;

    public PotionEffect(int effect, PotionEffectData effectData) {
        this.effect = effect;
        this.effectData = effectData;
    }

    public int effect() {
        return this.effect;
    }

    public PotionEffectData effectData() {
        return this.effectData;
    }
}
