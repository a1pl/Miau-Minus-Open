package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers;

import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntOpenHashMap;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.blockconnections.ConnectionData;
import java.util.Map;
import java.util.StringJoiner;
import java.util.Map.Entry;

public class PistonHandler implements BackwardsBlockEntityProvider.BackwardsBlockEntityHandler {
    private final Object2IntMap<String> pistonIds = new Object2IntOpenHashMap<>();

    public PistonHandler() {
        this.pistonIds.defaultReturnValue(-1);
        if (Via.getConfig().isServersideBlockConnections()) {
            Map<String, Integer> keyToId = ConnectionData.getKeyToId();

            for (Entry<String, Integer> entry : keyToId.entrySet()) {
                if (entry.getKey().contains("piston")) {
                    this.addEntries(entry.getKey(), entry.getValue());
                }
            }
        } else {
            ListTag<StringTag> blockStates = MappingDataLoader.INSTANCE
                .loadNBT("blockstates-1.13.nbt")
                .getListTag("blockstates", StringTag.class);

            for (int id = 0; id < blockStates.size(); id++) {
                StringTag state = blockStates.get(id);
                String key = state.getValue();
                if (key.contains("piston")) {
                    this.addEntries(key, id);
                }
            }
        }
    }

    private void addEntries(String data, int id) {
        id = Protocol1_12_2To1_13.MAPPINGS.getNewBlockStateId(id);
        this.pistonIds.put(data, id);
        String substring = data.substring(10);
        if (substring.startsWith("piston") || substring.startsWith("sticky_piston")) {
            String[] split = data.substring(0, data.length() - 1).split("\\[");
            String[] properties = split[1].split(",");
            data = split[0] + "[" + properties[1] + "," + properties[0] + "]";
            this.pistonIds.put(data, id);
        }
    }

    @Override
    public CompoundTag transform(int blockId, CompoundTag tag) {
        CompoundTag blockState = tag.getCompoundTag("blockState");
        if (blockState == null) {
            return tag;
        }

        String dataFromTag = this.getDataFromTag(blockState);
        if (dataFromTag == null) {
            return tag;
        }

        int id = this.pistonIds.getInt(dataFromTag);
        if (id == -1) {
            return tag;
        }

        tag.putInt("blockId", id >> 4);
        tag.putInt("blockData", id & 15);
        return tag;
    }

    private String getDataFromTag(CompoundTag tag) {
        StringTag name = tag.getStringTag("Name");
        if (name == null) {
            return null;
        }

        CompoundTag properties = tag.getCompoundTag("Properties");
        if (properties == null) {
            return name.getValue();
        }

        StringJoiner joiner = new StringJoiner(",", name.getValue() + "[", "]");

        for (Entry<String, Tag> entry : properties) {
            if (entry.getValue() instanceof StringTag) {
                joiner.add(entry.getKey() + "=" + ((StringTag)entry.getValue()).getValue());
            }
        }

        return joiner.toString();
    }
}
