package com.viaversion.viaversion.api.type.types;

import com.google.common.base.Preconditions;
import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public class IntArrayType extends Type<int[]> {
    private final int length;

    public IntArrayType(int length) {
        super(int[].class);
        this.length = length;
    }

    public IntArrayType() {
        super(int[].class);
        this.length = -1;
    }

    public void write(ByteBuf buffer, int[] object) throws Exception {
        if (this.length != -1) {
            Preconditions.checkArgument(this.length == object.length, "Length does not match expected length");
        } else {
            Type.VAR_INT.writePrimitive(buffer, object.length);
        }

        for (int i : object) {
            buffer.writeInt(i);
        }
    }

    public int[] read(ByteBuf buffer) throws Exception {
        int length = this.length == -1 ? Type.VAR_INT.readPrimitive(buffer) : this.length;
        Preconditions.checkArgument(buffer.isReadable(length), "Length is fewer than readable bytes");
        int[] array = new int[length];

        for (int i = 0; i < length; i++) {
            array[i] = buffer.readInt();
        }

        return array;
    }
}
