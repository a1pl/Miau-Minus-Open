package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.ArrayType;
import io.netty.buffer.ByteBuf;

public final class BannerPatternLayer {
    public static final Type<BannerPatternLayer> TYPE = new Type<BannerPatternLayer>(BannerPatternLayer.class) {
        public BannerPatternLayer read(ByteBuf buffer) throws Exception {
            Holder<BannerPattern> pattern = BannerPattern.TYPE.read(buffer);
            int color = Type.VAR_INT.readPrimitive(buffer);
            return new BannerPatternLayer(pattern, color);
        }

        public void write(ByteBuf buffer, BannerPatternLayer value) throws Exception {
            BannerPattern.TYPE.write(buffer, value.pattern);
            Type.VAR_INT.writePrimitive(buffer, value.dyeColor);
        }
    };
    public static final Type<BannerPatternLayer[]> ARRAY_TYPE = new ArrayType<>(TYPE);
    private final Holder<BannerPattern> pattern;
    private final int dyeColor;

    public BannerPatternLayer(Holder<BannerPattern> pattern, int dyeColor) {
        this.pattern = pattern;
        this.dyeColor = dyeColor;
    }

    public Holder<BannerPattern> pattern() {
        return this.pattern;
    }

    public int dyeColor() {
        return this.dyeColor;
    }
}
