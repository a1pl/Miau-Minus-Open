package com.viaversion.viaversion.api.minecraft.data;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.Objects;

final class FilledStructuredData<T> implements StructuredData<T> {
    private final StructuredDataKey<T> key;
    private T value;
    private int id;

    FilledStructuredData(StructuredDataKey<T> key, T value, int id) {
        Preconditions.checkNotNull(key);
        this.key = key;
        this.value = value;
        this.id = id;
    }

    @Override
    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public void write(ByteBuf buffer) throws Exception {
        this.key.type().write(buffer, this.value);
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public StructuredDataKey<T> key() {
        return this.key;
    }

    @Override
    public T value() {
        return this.value;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o != null && this.getClass() == o.getClass()) {
            FilledStructuredData<?> that = (FilledStructuredData<?>)o;
            if (this.id != that.id) {
                return false;
            } else {
                return !this.key.equals(that.key) ? false : Objects.equals(this.value, that.value);
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = this.key.hashCode();
        result = 31 * result + (this.value != null ? this.value.hashCode() : 0);
        return 31 * result + this.id;
    }

    @Override
    public String toString() {
        return "FilledStructuredData{key=" + this.key + ", value=" + this.value + ", id=" + this.id + '}';
    }
}
