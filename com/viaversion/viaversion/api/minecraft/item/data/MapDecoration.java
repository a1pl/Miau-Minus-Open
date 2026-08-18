package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public final class MapDecoration {
    public static final Type<MapDecoration> TYPE = new Type<MapDecoration>(MapDecoration.class) {
        public MapDecoration read(ByteBuf buffer) throws Exception {
            String type = Type.STRING.read(buffer);
            double x = Type.DOUBLE.readPrimitive(buffer);
            double z = Type.DOUBLE.readPrimitive(buffer);
            float rotation = Type.FLOAT.readPrimitive(buffer);
            return new MapDecoration(type, x, z, rotation);
        }

        public void write(ByteBuf buffer, MapDecoration value) throws Exception {
            Type.STRING.write(buffer, value.type);
            buffer.writeDouble(value.x);
            buffer.writeDouble(value.z);
            buffer.writeFloat(value.rotation);
        }
    };
    private final String type;
    private final double x;
    private final double z;
    private final float rotation;

    public MapDecoration(String type, double x, double z, float rotation) {
        this.type = type;
        this.x = x;
        this.z = z;
        this.rotation = rotation;
    }

    public String type() {
        return this.type;
    }

    public double x() {
        return this.x;
    }

    public double z() {
        return this.z;
    }

    public float rotation() {
        return this.rotation;
    }
}
