package com.viaversion.viaversion.protocols.protocol1_13to1_12_2.providers.blockentities;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.providers.BlockEntityProvider;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.storage.BlockStorage;
import com.viaversion.viaversion.util.ComponentUtil;

public class BannerHandler implements BlockEntityProvider.BlockEntityHandler {
    private static final int WALL_BANNER_START = 7110;
    private static final int WALL_BANNER_STOP = 7173;
    private static final int BANNER_START = 6854;
    private static final int BANNER_STOP = 7109;

    @Override
    public int transform(UserConnection user, CompoundTag tag) {
        BlockStorage storage = user.get(BlockStorage.class);
        Position position = new Position(
            tag.getNumberTag("x").asInt(), tag.getNumberTag("y").asShort(), tag.getNumberTag("z").asInt()
        );
        if (!storage.contains(position)) {
            Via.getPlatform()
                .getLogger()
                .warning("Received an banner color update packet, but there is no banner! O_o " + tag);
            return -1;
        }

        int blockId = storage.get(position).getOriginal();
        NumberTag base = tag.getNumberTag("Base");
        int color = base != null ? base.asInt() : 0;
        if (blockId >= 6854 && blockId <= 7109) {
            blockId += (15 - color) * 16;
        } else if (blockId >= 7110 && blockId <= 7173) {
            blockId += (15 - color) * 4;
        } else {
            Via.getPlatform().getLogger().warning("Why does this block have the banner block entity? :(" + tag);
        }

        ListTag<CompoundTag> patterns = tag.getListTag("Patterns", CompoundTag.class);
        if (patterns != null) {
            for (CompoundTag pattern : patterns) {
                NumberTag colorTag = pattern.getNumberTag("Color");
                if (colorTag != null) {
                    pattern.putInt("Color", 15 - colorTag.asInt());
                }
            }
        }

        StringTag name = tag.getStringTag("CustomName");
        if (name != null) {
            name.setValue(ComponentUtil.legacyToJsonString(name.getValue()));
        }

        return blockId;
    }
}
