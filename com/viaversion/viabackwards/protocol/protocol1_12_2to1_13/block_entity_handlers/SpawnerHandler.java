package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers;

import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.data.EntityNameRewrites;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;

public class SpawnerHandler implements BackwardsBlockEntityProvider.BackwardsBlockEntityHandler {
    @Override
    public CompoundTag transform(int blockId, CompoundTag tag) {
        CompoundTag dataTag = tag.getCompoundTag("SpawnData");
        if (dataTag != null) {
            StringTag idTag = dataTag.getStringTag("id");
            if (idTag != null) {
                idTag.setValue(EntityNameRewrites.rewrite(idTag.getValue()));
            }
        }

        return tag;
    }
}
