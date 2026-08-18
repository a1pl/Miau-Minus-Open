package com.viaversion.viaversion.api.type.types.misc;

import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.type.OptionalType;
import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public class HolderSetType extends Type<HolderSet> {
    public HolderSetType() {
        super(HolderSet.class);
    }

    public HolderSet read(ByteBuf buffer) throws Exception {
        int size = Type.VAR_INT.readPrimitive(buffer) - 1;
        if (size == -1) {
            String tag = Type.STRING.read(buffer);
            return HolderSet.of(tag);
        }

        int[] values = new int[size];

        for (int i = 0; i < size; i++) {
            values[i] = Type.VAR_INT.readPrimitive(buffer);
        }

        return HolderSet.of(values);
    }

    public void write(ByteBuf buffer, HolderSet object) throws Exception {
        if (object.hasTagKey()) {
            Type.VAR_INT.writePrimitive(buffer, 0);
            Type.STRING.write(buffer, object.tagKey());
        } else {
            int[] values = object.ids();
            Type.VAR_INT.writePrimitive(buffer, values.length + 1);

            for (int value : values) {
                Type.VAR_INT.writePrimitive(buffer, value);
            }
        }
    }

    public static final class OptionalHolderSetType extends OptionalType<HolderSet> {
        public OptionalHolderSetType() {
            super(Type.HOLDER_SET);
        }
    }
}
