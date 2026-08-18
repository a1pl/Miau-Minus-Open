package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.fastutil.objects.Object2ObjectOpenHashMap;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.Map.Entry;

public final class BlockStateProperties {
    public static final Type<BlockStateProperties> TYPE = new Type<BlockStateProperties>(BlockStateProperties.class) {
        public BlockStateProperties read(ByteBuf buffer) throws Exception {
            int size = Type.VAR_INT.readPrimitive(buffer);
            Map<String, String> properties = new Object2ObjectOpenHashMap<>(size);

            for (int i = 0; i < size; i++) {
                properties.put(Type.STRING.read(buffer), Type.STRING.read(buffer));
            }

            return new BlockStateProperties(properties);
        }

        public void write(ByteBuf buffer, BlockStateProperties value) throws Exception {
            Type.VAR_INT.writePrimitive(buffer, value.properties.size());

            for (Entry<String, String> entry : value.properties.entrySet()) {
                Type.STRING.write(buffer, entry.getKey());
                Type.STRING.write(buffer, entry.getValue());
            }
        }
    };
    private final Map<String, String> properties;

    public BlockStateProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public Map<String, String> properties() {
        return this.properties;
    }
}
