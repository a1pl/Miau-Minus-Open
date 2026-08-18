package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

public final class BannerPatternStorage implements StorableObject {
    private final Int2ObjectMap<String> bannerPatterns = new Int2ObjectOpenHashMap<>();

    public Int2ObjectMap<String> bannerPatterns() {
        return this.bannerPatterns;
    }
}
