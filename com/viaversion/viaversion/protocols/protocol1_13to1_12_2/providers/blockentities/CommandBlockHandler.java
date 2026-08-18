package com.viaversion.viaversion.protocols.protocol1_13to1_12_2.providers.blockentities;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonParser;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.providers.BlockEntityProvider;
import com.viaversion.viaversion.util.ComponentUtil;

public class CommandBlockHandler implements BlockEntityProvider.BlockEntityHandler {
    private final Protocol1_13To1_12_2 protocol = Via.getManager()
        .getProtocolManager()
        .getProtocol(Protocol1_13To1_12_2.class);

    @Override
    public int transform(UserConnection user, CompoundTag tag) {
        StringTag name = tag.getStringTag("CustomName");
        if (name != null) {
            name.setValue(ComponentUtil.legacyToJsonString(name.getValue()));
        }

        StringTag out = tag.getStringTag("LastOutput");
        if (out != null) {
            JsonElement value = JsonParser.parseString(out.getValue());
            this.protocol.getComponentRewriter().processText(user, value);
            out.setValue(value.toString());
        }

        return -1;
    }
}
