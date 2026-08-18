package com.viaversion.viaversion.protocols.protocol1_19to1_18_2.data;

import com.viaversion.viaversion.api.data.MappingDataBase;
import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class MappingData extends MappingDataBase {
    private final Int2ObjectMap<CompoundTag> defaultChatTypes = new Int2ObjectOpenHashMap<>();
    private CompoundTag chatRegistry;

    public MappingData() {
        super("1.18", "1.19");
    }

    @Override
    protected void loadExtras(CompoundTag daata) {
        for (CompoundTag chatType : MappingDataLoader.INSTANCE
            .loadNBTFromFile("chat-types-1.19.nbt")
            .getListTag("values", CompoundTag.class)) {
            NumberTag idTag = chatType.getNumberTag("id");
            this.defaultChatTypes.put(idTag.asInt(), chatType);
        }

        this.chatRegistry = MappingDataLoader.INSTANCE.loadNBTFromFile("chat-registry-1.19.nbt");
    }

    public @Nullable CompoundTag chatType(int id) {
        return this.defaultChatTypes.get(id);
    }

    public CompoundTag chatRegistry() {
        return this.chatRegistry.copy();
    }
}
