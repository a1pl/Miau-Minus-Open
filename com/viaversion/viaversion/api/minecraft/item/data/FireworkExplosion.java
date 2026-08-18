package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.ArrayType;
import io.netty.buffer.ByteBuf;

public final class FireworkExplosion {
    public static final Type<FireworkExplosion> TYPE = new Type<FireworkExplosion>(FireworkExplosion.class) {
        public FireworkExplosion read(ByteBuf buffer) throws Exception {
            int shape = Type.VAR_INT.readPrimitive(buffer);
            int[] colors = Type.INT_ARRAY_PRIMITIVE.read(buffer);
            int[] fadeColors = Type.INT_ARRAY_PRIMITIVE.read(buffer);
            boolean hasTrail = buffer.readBoolean();
            boolean hasTwinkle = buffer.readBoolean();
            return new FireworkExplosion(shape, colors, fadeColors, hasTrail, hasTwinkle);
        }

        public void write(ByteBuf buffer, FireworkExplosion value) throws Exception {
            Type.VAR_INT.writePrimitive(buffer, value.shape);
            Type.INT_ARRAY_PRIMITIVE.write(buffer, value.colors);
            Type.INT_ARRAY_PRIMITIVE.write(buffer, value.fadeColors);
            buffer.writeBoolean(value.hasTrail);
            buffer.writeBoolean(value.hasTwinkle);
        }
    };
    public static final Type<FireworkExplosion[]> ARRAY_TYPE = new ArrayType<>(TYPE);
    private final int shape;
    private final int[] colors;
    private final int[] fadeColors;
    private final boolean hasTrail;
    private final boolean hasTwinkle;

    public FireworkExplosion(int shape, int[] colors, int[] fadeColors, boolean hasTrail, boolean hasTwinkle) {
        this.shape = shape;
        this.colors = colors;
        this.fadeColors = fadeColors;
        this.hasTrail = hasTrail;
        this.hasTwinkle = hasTwinkle;
    }

    public int shape() {
        return this.shape;
    }

    public int[] colors() {
        return this.colors;
    }

    public int[] fadeColors() {
        return this.fadeColors;
    }

    public boolean hasTrail() {
        return this.hasTrail;
    }

    public boolean hasTwinkle() {
        return this.hasTwinkle;
    }
}
