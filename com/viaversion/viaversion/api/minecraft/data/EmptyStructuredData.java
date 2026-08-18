package com.viaversion.viaversion.api.minecraft.data;

import io.netty.buffer.ByteBuf;

final class EmptyStructuredData<T> implements StructuredData<T> {
    private final StructuredDataKey<T> key;
    private int id;

    EmptyStructuredData(StructuredDataKey<T> key, int id) {
        this.key = key;
        this.id = id;
    }

    @Override
    public void setValue(T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(ByteBuf buffer) {
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
        return null;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            EmptyStructuredData<?> that = (EmptyStructuredData<?>)o;
            return this.id != that.id ? false : this.key.equals(that.key);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = this.key.hashCode();
        return 31 * result + this.id;
    }

    @Override
    public String toString() {
        return "EmptyStructuredData{key=" + this.key + ", id=" + this.id + '}';
    }
}
