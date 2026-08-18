package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.fastutil.objects.Object2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2ObjectOpenHashMap;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.Map.Entry;

public final class MapDecorations {
    public static final Type<MapDecorations> TYPE = new Type<MapDecorations>(MapDecorations.class) {
        public MapDecorations read(ByteBuf buffer) throws Exception {
            Object2ObjectMap<String, MapDecoration> decorations = new Object2ObjectOpenHashMap<>();
            int size = Type.VAR_INT.readPrimitive(buffer);

            for (int i = 0; i < size; i++) {
                String id = Type.STRING.read(buffer);
                MapDecoration decoration = MapDecoration.TYPE.read(buffer);
                decorations.put(id, decoration);
            }

            return new MapDecorations(decorations);
        }

        public void write(ByteBuf buffer, MapDecorations value) throws Exception {
            Type.VAR_INT.writePrimitive(buffer, value.decorations.size());

            for (Entry<String, MapDecoration> entry : value.decorations.entrySet()) {
                Type.STRING.write(buffer, entry.getKey());
                MapDecoration.TYPE.write(buffer, entry.getValue());
            }
        }
    };
    private final Map<String, MapDecoration> decorations;

    public MapDecorations(Map<String, MapDecoration> decorations) {
        this.decorations = decorations;
    }

    public Map<String, MapDecoration> decorations() {
        return this.decorations;
    }
}
