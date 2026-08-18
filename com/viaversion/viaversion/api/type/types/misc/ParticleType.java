package com.viaversion.viaversion.api.type.types.misc;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.FullMappings;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.util.Key;
import io.netty.buffer.ByteBuf;

public class ParticleType extends DynamicType<Particle> {
    public ParticleType() {
        super(Particle.class);
    }

    public void write(ByteBuf buffer, Particle object) throws Exception {
        Type.VAR_INT.writePrimitive(buffer, object.id());

        for (Particle.ParticleData<?> data : object.getArguments()) {
            data.write(buffer);
        }
    }

    public Particle read(ByteBuf buffer) throws Exception {
        int type = Type.VAR_INT.readPrimitive(buffer);
        Particle particle = new Particle(type);
        this.readData(buffer, particle);
        return particle;
    }

    @Override
    protected FullMappings mappings(Protocol<?, ?, ?, ?> protocol) {
        return protocol.getMappingData().getParticleMappings();
    }

    public static DynamicType.DataReader<Particle> itemHandler(Type<Item> itemType) {
        return (buf, particle) -> particle.add(itemType, itemType.read(buf));
    }

    public static final class Readers {
        public static final DynamicType.DataReader<Particle> BLOCK = (buf, particle) -> particle.add(
            Type.VAR_INT, Type.VAR_INT.readPrimitive(buf)
        );
        public static final DynamicType.DataReader<Particle> ITEM1_13 = ParticleType.itemHandler(Type.ITEM1_13);
        public static final DynamicType.DataReader<Particle> ITEM1_13_2 = ParticleType.itemHandler(Type.ITEM1_13_2);
        public static final DynamicType.DataReader<Particle> ITEM1_20_2 = ParticleType.itemHandler(Type.ITEM1_20_2);
        public static final DynamicType.DataReader<Particle> ITEM1_20_5 = ParticleType.itemHandler(Types1_20_5.ITEM);
        public static final DynamicType.DataReader<Particle> DUST = (buf, particle) -> {
            particle.add(Type.FLOAT, Type.FLOAT.readPrimitive(buf));
            particle.add(Type.FLOAT, Type.FLOAT.readPrimitive(buf));
            particle.add(Type.FLOAT, Type.FLOAT.readPrimitive(buf));
            particle.add(Type.FLOAT, Type.FLOAT.readPrimitive(buf));
        };
        public static final DynamicType.DataReader<Particle> DUST_TRANSITION = (buf, particle) -> {
            particle.add(Type.FLOAT, Type.FLOAT.readPrimitive(buf));
            particle.add(Type.FLOAT, Type.FLOAT.readPrimitive(buf));
            particle.add(Type.FLOAT, Type.FLOAT.readPrimitive(buf));
            particle.add(Type.FLOAT, Type.FLOAT.readPrimitive(buf));
            particle.add(Type.FLOAT, Type.FLOAT.readPrimitive(buf));
            particle.add(Type.FLOAT, Type.FLOAT.readPrimitive(buf));
            particle.add(Type.FLOAT, Type.FLOAT.readPrimitive(buf));
        };
        public static final DynamicType.DataReader<Particle> VIBRATION = (buf, particle) -> {
            particle.add(Type.POSITION1_14, Type.POSITION1_14.read(buf));
            String resourceLocation = Type.STRING.read(buf);
            particle.add(Type.STRING, resourceLocation);
            resourceLocation = Key.stripMinecraftNamespace(resourceLocation);
            if (resourceLocation.equals("block")) {
                particle.add(Type.POSITION1_14, Type.POSITION1_14.read(buf));
            } else if (resourceLocation.equals("entity")) {
                particle.add(Type.VAR_INT, Type.VAR_INT.readPrimitive(buf));
            } else {
                Via.getPlatform()
                    .getLogger()
                    .warning("Unknown vibration path position source type: " + resourceLocation);
            }

            particle.add(Type.VAR_INT, Type.VAR_INT.readPrimitive(buf));
        };
        public static final DynamicType.DataReader<Particle> VIBRATION1_19 = (buf, particle) -> {
            String resourceLocation = Type.STRING.read(buf);
            particle.add(Type.STRING, resourceLocation);
            resourceLocation = Key.stripMinecraftNamespace(resourceLocation);
            if (resourceLocation.equals("block")) {
                particle.add(Type.POSITION1_14, Type.POSITION1_14.read(buf));
            } else if (resourceLocation.equals("entity")) {
                particle.add(Type.VAR_INT, Type.VAR_INT.readPrimitive(buf));
                particle.add(Type.FLOAT, Type.FLOAT.readPrimitive(buf));
            } else {
                Via.getPlatform()
                    .getLogger()
                    .warning("Unknown vibration path position source type: " + resourceLocation);
            }

            particle.add(Type.VAR_INT, Type.VAR_INT.readPrimitive(buf));
        };
        public static final DynamicType.DataReader<Particle> VIBRATION1_20_3 = (buf, particle) -> {
            int sourceTypeId = Type.VAR_INT.readPrimitive(buf);
            particle.add(Type.VAR_INT, sourceTypeId);
            if (sourceTypeId == 0) {
                particle.add(Type.POSITION1_14, Type.POSITION1_14.read(buf));
            } else if (sourceTypeId == 1) {
                particle.add(Type.VAR_INT, Type.VAR_INT.readPrimitive(buf));
                particle.add(Type.FLOAT, Type.FLOAT.readPrimitive(buf));
            } else {
                Via.getPlatform().getLogger().warning("Unknown vibration path position source type: " + sourceTypeId);
            }

            particle.add(Type.VAR_INT, Type.VAR_INT.readPrimitive(buf));
        };
        public static final DynamicType.DataReader<Particle> SCULK_CHARGE = (buf, particle) -> particle.add(
            Type.FLOAT, Type.FLOAT.readPrimitive(buf)
        );
        public static final DynamicType.DataReader<Particle> SHRIEK = (buf, particle) -> particle.add(
            Type.VAR_INT, Type.VAR_INT.readPrimitive(buf)
        );
        public static final DynamicType.DataReader<Particle> COLOR = (buf, particle) -> particle.add(
            Type.INT, buf.readInt()
        );
    }
}
