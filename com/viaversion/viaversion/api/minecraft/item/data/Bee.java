package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.ArrayType;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import io.netty.buffer.ByteBuf;

public final class Bee {
    public static final Type<Bee> TYPE = new Type<Bee>(Bee.class) {
        public Bee read(ByteBuf buffer) throws Exception {
            CompoundTag entityData = Type.COMPOUND_TAG.read(buffer);
            int ticksInHive = Type.VAR_INT.readPrimitive(buffer);
            int minTicksInHive = Type.VAR_INT.readPrimitive(buffer);
            return new Bee(entityData, ticksInHive, minTicksInHive);
        }

        public void write(ByteBuf buffer, Bee value) throws Exception {
            Type.COMPOUND_TAG.write(buffer, value.entityData);
            Type.VAR_INT.writePrimitive(buffer, value.ticksInHive);
            Type.VAR_INT.writePrimitive(buffer, value.minTicksInHive);
        }
    };
    public static final Type<Bee[]> ARRAY_TYPE = new ArrayType<>(TYPE);
    private final CompoundTag entityData;
    private final int ticksInHive;
    private final int minTicksInHive;

    public Bee(CompoundTag entityData, int ticksInHive, int minTicksInHive) {
        this.entityData = entityData;
        this.ticksInHive = ticksInHive;
        this.minTicksInHive = minTicksInHive;
    }

    public CompoundTag entityData() {
        return this.entityData;
    }

    public int ticksInHive() {
        return this.ticksInHive;
    }

    public int minTicksInHive() {
        return this.minTicksInHive;
    }
}
