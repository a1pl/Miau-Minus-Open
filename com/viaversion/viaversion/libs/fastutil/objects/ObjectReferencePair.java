package com.viaversion.viaversion.libs.fastutil.objects;

import com.viaversion.viaversion.libs.fastutil.Pair;

public interface ObjectReferencePair<K, V> extends Pair<K, V> {
    static <K, V> ObjectReferencePair<K, V> of(K left, V right) {
        return new ObjectReferenceImmutablePair<>(left, right);
    }
}
