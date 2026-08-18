package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class Filterable<T> {
    private final T raw;
    private final T filtered;

    protected Filterable(T raw, @Nullable T filtered) {
        this.raw = raw;
        this.filtered = filtered;
    }

    public T raw() {
        return this.raw;
    }

    public boolean isFiltered() {
        return this.filtered != null;
    }

    public @Nullable T filtered() {
        return this.filtered;
    }

    public T get() {
        return this.filtered != null ? this.filtered : this.raw;
    }

    public abstract static class FilterableType<T, F extends Filterable<T>> extends Type<F> {
        private final Type<T> elementType;
        private final Type<T> optionalElementType;

        protected FilterableType(Type<T> elementType, Type<T> optionalElementType, Class<F> outputClass) {
            super(outputClass);
            this.elementType = elementType;
            this.optionalElementType = optionalElementType;
        }

        public F read(ByteBuf buffer) throws Exception {
            T raw = this.elementType.read(buffer);
            T filtered = this.optionalElementType.read(buffer);
            return this.create(raw, filtered);
        }

        public void write(ByteBuf buffer, F value) throws Exception {
            this.elementType.write(buffer, value.raw());
            this.optionalElementType.write(buffer, value.filtered());
        }

        protected abstract F create(T var1, T var2);
    }
}
