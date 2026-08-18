package com.viaversion.viaversion.api.minecraft;

import com.google.common.base.Preconditions;

final class HolderImpl<T> implements Holder<T> {
    private final T value;
    private final int id;

    HolderImpl(int id) {
        Preconditions.checkArgument(id >= 0, "id cannot be negative");
        this.value = null;
        this.id = id;
    }

    HolderImpl(T value) {
        this.value = value;
        this.id = -1;
    }

    @Override
    public boolean isDirect() {
        return this.id == -1;
    }

    @Override
    public boolean hasId() {
        return this.id != -1;
    }

    @Override
    public T value() {
        Preconditions.checkArgument(this.isDirect(), "Holder is not direct");
        return this.value;
    }

    @Override
    public int id() {
        return this.id;
    }
}
