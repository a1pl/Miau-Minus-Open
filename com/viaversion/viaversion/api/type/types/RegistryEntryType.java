package com.viaversion.viaversion.api.type.types;

import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public class RegistryEntryType extends Type<RegistryEntry> {
    public RegistryEntryType() {
        super(RegistryEntry.class);
    }

    public RegistryEntry read(ByteBuf buffer) throws Exception {
        return new RegistryEntry(Type.STRING.read(buffer), Type.OPTIONAL_TAG.read(buffer));
    }

    public void write(ByteBuf buffer, RegistryEntry entry) throws Exception {
        Type.STRING.write(buffer, entry.key());
        Type.OPTIONAL_TAG.write(buffer, entry.tag());
    }
}
