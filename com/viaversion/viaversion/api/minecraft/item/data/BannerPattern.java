package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.misc.HolderType;
import io.netty.buffer.ByteBuf;

public final class BannerPattern {
    public static final HolderType<BannerPattern> TYPE = new HolderType<BannerPattern>() {
        public BannerPattern readDirect(ByteBuf buffer) throws Exception {
            String assetId = Type.STRING.read(buffer);
            String translationKey = Type.STRING.read(buffer);
            return new BannerPattern(assetId, translationKey);
        }

        public void writeDirect(ByteBuf buffer, BannerPattern value) throws Exception {
            Type.STRING.write(buffer, value.assetId);
            Type.STRING.write(buffer, value.translationKey);
        }
    };
    private final String assetId;
    private final String translationKey;

    public BannerPattern(String assetId, String translationKey) {
        this.assetId = assetId;
        this.translationKey = translationKey;
    }

    public String assetId() {
        return this.assetId;
    }

    public String translationKey() {
        return this.translationKey;
    }
}
