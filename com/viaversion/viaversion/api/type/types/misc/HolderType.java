package com.viaversion.viaversion.api.type.types.misc;

import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public abstract class HolderType<T> extends Type<Holder<T>> {
    protected HolderType() {
        super(Holder.class);
    }

    public Holder<T> read(ByteBuf buffer) throws Exception {
        int id = Type.VAR_INT.readPrimitive(buffer) - 1;
        return id == -1 ? Holder.of(this.readDirect(buffer)) : Holder.of(id);
    }

    public void write(ByteBuf buffer, Holder<T> object) throws Exception {
        if (object.hasId()) {
            Type.VAR_INT.writePrimitive(buffer, object.id() + 1);
        } else {
            Type.VAR_INT.writePrimitive(buffer, 0);
            this.writeDirect(buffer, object.value());
        }
    }

    public abstract T readDirect(ByteBuf var1) throws Exception;

    public abstract void writeDirect(ByteBuf var1, T var2) throws Exception;
}
