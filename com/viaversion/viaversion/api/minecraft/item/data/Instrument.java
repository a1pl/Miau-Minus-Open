package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.misc.HolderType;
import io.netty.buffer.ByteBuf;

public final class Instrument {
    public static final HolderType<Instrument> TYPE = new HolderType<Instrument>() {
        public Instrument readDirect(ByteBuf buffer) throws Exception {
            Holder<SoundEvent> soundEvent = Type.SOUND_EVENT.read(buffer);
            int useDuration = Type.VAR_INT.readPrimitive(buffer);
            float range = buffer.readFloat();
            return new Instrument(soundEvent, useDuration, range);
        }

        public void writeDirect(ByteBuf buffer, Instrument value) throws Exception {
            Type.SOUND_EVENT.write(buffer, value.soundEvent());
            Type.VAR_INT.writePrimitive(buffer, value.useDuration());
            buffer.writeFloat(value.range());
        }
    };
    private final Holder<SoundEvent> soundEvent;
    private final int useDuration;
    private final float range;

    public Instrument(Holder<SoundEvent> soundEvent, int useDuration, float range) {
        this.soundEvent = soundEvent;
        this.useDuration = useDuration;
        this.range = range;
    }

    public Holder<SoundEvent> soundEvent() {
        return this.soundEvent;
    }

    public int useDuration() {
        return this.useDuration;
    }

    public float range() {
        return this.range;
    }
}
