package com.viaversion.viaversion.protocols.protocol1_19_1to1_19.data;

import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;

public class ChatRegistry {
    private static final CompoundTag chatRegistry = MappingDataLoader.INSTANCE
        .loadNBTFromFile("chat-registry-1.19.1.nbt");

    public static CompoundTag chatRegistry() {
        return chatRegistry.copy();
    }
}
