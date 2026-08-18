package com.viaversion.viaversion.libs.fastutil.objects;

import com.viaversion.viaversion.libs.fastutil.Pair;

public interface ReferenceObjectPair<K, V> extends Pair<K, V> {
    static <K, V> ReferenceObjectPair<K, V> of(K left, V right) {
        return new ReferenceObjectImmutablePair<>(left, right);
    }
}
