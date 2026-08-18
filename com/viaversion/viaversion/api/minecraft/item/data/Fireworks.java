package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public final class Fireworks {
    public static final Type<Fireworks> TYPE = new Type<Fireworks>(Fireworks.class) {
        public Fireworks read(ByteBuf buffer) throws Exception {
            int flightDuration = Type.VAR_INT.readPrimitive(buffer);
            FireworkExplosion[] explosions = FireworkExplosion.ARRAY_TYPE.read(buffer);
            return new Fireworks(flightDuration, explosions);
        }

        public void write(ByteBuf buffer, Fireworks value) throws Exception {
            Type.VAR_INT.writePrimitive(buffer, value.flightDuration);
            FireworkExplosion.ARRAY_TYPE.write(buffer, value.explosions);
        }
    };
    private final FireworkExplosion[] explosions;
    private final int flightDuration;

    public Fireworks(int flightDuration, FireworkExplosion[] explosions) {
        this.flightDuration = flightDuration;
        this.explosions = explosions;
    }

    public int flightDuration() {
        return this.flightDuration;
    }

    public FireworkExplosion[] explosions() {
        return this.explosions;
    }
}
