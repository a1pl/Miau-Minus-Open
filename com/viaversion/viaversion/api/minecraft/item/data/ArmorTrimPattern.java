package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.misc.HolderType;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import io.netty.buffer.ByteBuf;

public final class ArmorTrimPattern {
    public static final HolderType<ArmorTrimPattern> TYPE = new HolderType<ArmorTrimPattern>() {
        public ArmorTrimPattern readDirect(ByteBuf buffer) throws Exception {
            String assetName = Type.STRING.read(buffer);
            int itemId = Type.VAR_INT.readPrimitive(buffer);
            Tag description = Type.TAG.read(buffer);
            boolean decal = buffer.readBoolean();
            return new ArmorTrimPattern(assetName, itemId, description, decal);
        }

        public void writeDirect(ByteBuf buffer, ArmorTrimPattern value) throws Exception {
            Type.STRING.write(buffer, value.assetName());
            Type.VAR_INT.writePrimitive(buffer, value.itemId());
            Type.TAG.write(buffer, value.description());
            buffer.writeBoolean(value.decal());
        }
    };
    private final String assetName;
    private final int itemId;
    private final Tag description;
    private final boolean decal;

    public ArmorTrimPattern(String assetName, int itemId, Tag description, boolean decal) {
        this.assetName = assetName;
        this.itemId = itemId;
        this.description = description;
        this.decal = decal;
    }

    public String assetName() {
        return this.assetName;
    }

    public int itemId() {
        return this.itemId;
    }

    public Tag description() {
        return this.description;
    }

    public boolean decal() {
        return this.decal;
    }
}
