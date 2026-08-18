package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data;

import com.viaversion.viaversion.api.data.MappingDataBase;
import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.util.KeyMappings;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MappingData extends MappingDataBase {
    private KeyMappings blocks;
    private KeyMappings sounds;

    public MappingData() {
        super("1.20.3", "1.20.5");
    }

    @Override
    protected void loadExtras(CompoundTag data) {
        super.loadExtras(data);
        CompoundTag extraMappings = MappingDataLoader.INSTANCE.loadNBT("extra-identifiers-1.20.3.nbt");
        this.blocks = new KeyMappings(extraMappings.getListTag("blocks", StringTag.class));
        this.sounds = new KeyMappings(extraMappings.getListTag("sounds", StringTag.class));
    }

    public int blockId(String name) {
        return this.blocks.keyToId(name);
    }

    public @Nullable String blockName(int id) {
        return this.blocks.idToKey(id);
    }

    public int soundId(String name) {
        return this.sounds.keyToId(name);
    }

    public @Nullable String soundName(int id) {
        return this.sounds.idToKey(id);
    }
}
