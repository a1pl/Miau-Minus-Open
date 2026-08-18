package com.viaversion.viaversion.libs.fastutil.objects;

import com.viaversion.viaversion.libs.fastutil.Pair;

public interface ReferenceReferencePair<K, V> extends Pair<K, V> {
    static <K, V> ReferenceReferencePair<K, V> of(K left, V right) {
        return new ReferenceReferenceImmutablePair<>(left, right);
    }
}
