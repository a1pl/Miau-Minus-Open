package com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.data;

import com.viaversion.viabackwards.api.data.BackwardsMappingDataLoader;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.Protocol1_19To1_18_2;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BackwardsMappings extends com.viaversion.viabackwards.api.data.BackwardsMappings {
    private final Int2ObjectMap<CompoundTag> defaultChatTypes = new Int2ObjectOpenHashMap<>();

    public BackwardsMappings() {
        super("1.19", "1.18", Protocol1_19To1_18_2.class);
    }

    @Override
    protected void loadExtras(CompoundTag data) {
        super.loadExtras(data);

        for (CompoundTag chatType : BackwardsMappingDataLoader.INSTANCE
            .loadNBT("chat-types-1.19.1.nbt")
            .getListTag("values", CompoundTag.class)) {
            NumberTag idTag = chatType.getNumberTag("id");
            this.defaultChatTypes.put(idTag.asInt(), chatType);
        }
    }

    public @Nullable CompoundTag chatType(int id) {
        return this.defaultChatTypes.get(id);
    }
}
