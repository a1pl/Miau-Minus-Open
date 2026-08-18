package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;

public class BannerHandler implements BackwardsBlockEntityProvider.BackwardsBlockEntityHandler {
    private static final int WALL_BANNER_START = 7110;
    private static final int WALL_BANNER_STOP = 7173;
    private static final int BANNER_START = 6854;
    private static final int BANNER_STOP = 7109;

    @Override
    public CompoundTag transform(int blockId, CompoundTag tag) {
        if (blockId >= 6854 && blockId <= 7109) {
            int color = blockId - 6854 >> 4;
            tag.putInt("Base", 15 - color);
        } else if (blockId >= 7110 && blockId <= 7173) {
            int color = blockId - 7110 >> 2;
            tag.putInt("Base", 15 - color);
        } else {
            ViaBackwards.getPlatform()
                .getLogger()
                .warning("Why does this block have the banner block entity? :(" + tag);
        }

        ListTag<CompoundTag> patternsTag = tag.getListTag("Patterns", CompoundTag.class);
        if (patternsTag != null) {
            for (CompoundTag pattern : patternsTag) {
                NumberTag colorTag = pattern.getNumberTag("Color");
                pattern.putInt("Color", 15 - colorTag.asInt());
            }
        }

        return tag;
    }
}
