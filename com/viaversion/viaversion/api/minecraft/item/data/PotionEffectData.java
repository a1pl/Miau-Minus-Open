package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.OptionalType;
import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class PotionEffectData {
    public static final Type<PotionEffectData> TYPE = new Type<PotionEffectData>(PotionEffectData.class) {
        public PotionEffectData read(ByteBuf buffer) throws Exception {
            int amplifier = Type.VAR_INT.readPrimitive(buffer);
            int duration = Type.VAR_INT.readPrimitive(buffer);
            boolean ambient = buffer.readBoolean();
            boolean showParticles = buffer.readBoolean();
            boolean showIcon = buffer.readBoolean();
            PotionEffectData hiddenEffect = PotionEffectData.OPTIONAL_TYPE.read(buffer);
            return new PotionEffectData(amplifier, duration, ambient, showParticles, showIcon, hiddenEffect);
        }

        public void write(ByteBuf buffer, PotionEffectData value) throws Exception {
            Type.VAR_INT.writePrimitive(buffer, value.amplifier);
            Type.VAR_INT.writePrimitive(buffer, value.duration);
            buffer.writeBoolean(value.ambient);
            buffer.writeBoolean(value.showParticles);
            buffer.writeBoolean(value.showIcon);
            PotionEffectData.OPTIONAL_TYPE.write(buffer, value.hiddenEffect);
        }
    };
    public static final Type<PotionEffectData> OPTIONAL_TYPE = new OptionalType<PotionEffectData>(TYPE) {};
    private final int amplifier;
    private final int duration;
    private final boolean ambient;
    private final boolean showParticles;
    private final boolean showIcon;
    private final PotionEffectData hiddenEffect;

    public PotionEffectData(
        int amplifier,
        int duration,
        boolean ambient,
        boolean showParticles,
        boolean showIcon,
        @Nullable PotionEffectData hiddenEffect
    ) {
        this.amplifier = amplifier;
        this.duration = duration;
        this.ambient = ambient;
        this.showParticles = showParticles;
        this.showIcon = showIcon;
        this.hiddenEffect = hiddenEffect;
    }

    public int amplifier() {
        return this.amplifier;
    }

    public int duration() {
        return this.duration;
    }

    public boolean ambient() {
        return this.ambient;
    }

    public boolean showParticles() {
        return this.showParticles;
    }

    public boolean showIcon() {
        return this.showIcon;
    }

    public @Nullable PotionEffectData hiddenEffect() {
        return this.hiddenEffect;
    }
}
