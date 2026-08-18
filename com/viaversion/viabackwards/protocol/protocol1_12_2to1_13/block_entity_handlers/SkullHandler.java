package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers;

import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;

public class SkullHandler implements BackwardsBlockEntityProvider.BackwardsBlockEntityHandler {
    private static final int SKULL_START = 5447;

    @Override
    public CompoundTag transform(int blockId, CompoundTag tag) {
        int diff = blockId - 5447;
        int pos = diff % 20;
        byte type = (byte)Math.floor(diff / 20.0F);
        tag.putByte("SkullType", type);
        if (pos < 4) {
            return tag;
        }

        tag.putByte("Rot", (byte)(pos - 4 & 0xFF));
        return tag;
    }
}
