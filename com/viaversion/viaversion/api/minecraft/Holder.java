package com.viaversion.viaversion.api.minecraft;

public interface Holder<T> {
    static <T> Holder<T> of(int id) {
        return new HolderImpl<>(id);
    }

    static <T> Holder<T> of(T value) {
        return new HolderImpl<>(value);
    }

    boolean isDirect();

    boolean hasId();

    T value();

    int id();
}
