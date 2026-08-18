package com.viaversion.viaversion.api.type.types.misc;

import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public final class SoundEventType extends HolderType<SoundEvent> {
    public SoundEvent readDirect(ByteBuf buffer) throws Exception {
        String resourceLocation = Type.STRING.read(buffer);
        Float fixedRange = Type.OPTIONAL_FLOAT.read(buffer);
        return new SoundEvent(resourceLocation, fixedRange);
    }

    public void writeDirect(ByteBuf buffer, SoundEvent value) throws Exception {
        Type.STRING.write(buffer, value.identifier());
        Type.OPTIONAL_FLOAT.write(buffer, value.fixedRange());
    }
}
