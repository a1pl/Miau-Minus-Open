package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class PotionContents {
    public static final Type<PotionContents> TYPE = new Type<PotionContents>(PotionContents.class) {
        public PotionContents read(ByteBuf buffer) throws Exception {
            Integer potion = buffer.readBoolean() ? Type.VAR_INT.readPrimitive(buffer) : null;
            Integer customColor = buffer.readBoolean() ? buffer.readInt() : null;
            PotionEffect[] customEffects = PotionEffect.ARRAY_TYPE.read(buffer);
            return new PotionContents(potion, customColor, customEffects);
        }

        public void write(ByteBuf buffer, PotionContents value) throws Exception {
            buffer.writeBoolean(value.potion != null);
            if (value.potion != null) {
                Type.VAR_INT.writePrimitive(buffer, value.potion);
            }

            buffer.writeBoolean(value.customColor != null);
            if (value.customColor != null) {
                buffer.writeInt(value.customColor);
            }

            PotionEffect.ARRAY_TYPE.write(buffer, value.customEffects);
        }
    };
    private final Integer potion;
    private final Integer customColor;
    private final PotionEffect[] customEffects;

    public PotionContents(@Nullable Integer potion, @Nullable Integer customColor, PotionEffect[] customEffects) {
        this.potion = potion;
        this.customColor = customColor;
        this.customEffects = customEffects;
    }

    public @Nullable Integer potion() {
        return this.potion;
    }

    public @Nullable Integer customColor() {
        return this.customColor;
    }

    public PotionEffect[] customEffects() {
        return this.customEffects;
    }
}
