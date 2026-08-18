package com.viaversion.viaversion.api.type.types.item;

import com.google.common.base.Preconditions;
import com.viaversion.viaversion.api.data.FullMappings;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class StructuredDataType extends Type<StructuredData<?>> {
    private StructuredDataKey<?>[] types;

    public StructuredDataType() {
        super(StructuredData.class);
    }

    public void write(ByteBuf buffer, StructuredData<?> object) throws Exception {
        Type.VAR_INT.writePrimitive(buffer, object.id());
        object.write(buffer);
    }

    public StructuredData<?> read(ByteBuf buffer) throws Exception {
        Preconditions.checkNotNull(this.types, "StructuredDataType has not been initialized");
        int id = Type.VAR_INT.readPrimitive(buffer);
        StructuredDataKey<?> key = this.types[id];
        if (key == null) {
            throw new IllegalArgumentException("No data component serializer found for id " + id);
        } else {
            return this.readData(buffer, key, id);
        }
    }

    public @Nullable StructuredDataKey<?> key(int id) {
        return id >= 0 && id < this.types.length ? this.types[id] : null;
    }

    private <T> StructuredData<T> readData(ByteBuf buffer, StructuredDataKey<T> key, int id) throws Exception {
        return StructuredData.of(key, key.type().read(buffer), id);
    }

    public StructuredDataType.DataFiller filler(Protocol<?, ?, ?, ?> protocol) {
        return new StructuredDataType.DataFiller(protocol);
    }

    public final class DataFiller {
        private final FullMappings mappings;

        private DataFiller(Protocol<?, ?, ?, ?> protocol) {
            this.mappings = protocol.getMappingData().getDataComponentSerializerMappings();
            Preconditions.checkArgument(
                this.mappings != null, "No mappings found for protocol %s", new Object[]{protocol.getClass()}
            );
            Preconditions.checkArgument(
                StructuredDataType.this.types == null, "StructuredDataType has already been initialized"
            );
            StructuredDataType.this.types = new StructuredDataKey[this.mappings.mappedSize()];
        }

        public StructuredDataType.DataFiller add(StructuredDataKey<?> reader) {
            int id = this.mappings.mappedId(reader.identifier());
            Preconditions.checkArgument(id != -1, "No mapped id found for %s", new Object[]{reader.identifier()});
            StructuredDataType.this.types[id] = reader;
            return this;
        }
    }
}
