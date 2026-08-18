package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.ArrayType;
import io.netty.buffer.ByteBuf;

public final class SuspiciousStewEffect {
    public static final Type<SuspiciousStewEffect> TYPE = new Type<SuspiciousStewEffect>(SuspiciousStewEffect.class) {
        public SuspiciousStewEffect read(ByteBuf buffer) {
            int effect = Type.VAR_INT.readPrimitive(buffer);
            int duration = Type.VAR_INT.readPrimitive(buffer);
            return new SuspiciousStewEffect(effect, duration);
        }

        public void write(ByteBuf buffer, SuspiciousStewEffect value) {
            Type.VAR_INT.writePrimitive(buffer, value.effect);
            Type.VAR_INT.writePrimitive(buffer, value.duration);
        }
    };
    public static final Type<SuspiciousStewEffect[]> ARRAY_TYPE = new ArrayType<>(TYPE);
    private final int effect;
    private final int duration;

    public SuspiciousStewEffect(int effect, int duration) {
        this.effect = effect;
        this.duration = duration;
    }

    public int mobEffect() {
        return this.effect;
    }

    public int duration() {
        return this.duration;
    }
}
