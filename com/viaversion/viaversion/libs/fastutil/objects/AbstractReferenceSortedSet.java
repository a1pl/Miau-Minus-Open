package com.viaversion.viaversion.libs.fastutil.objects;

public abstract class AbstractReferenceSortedSet<K> extends AbstractReferenceSet<K> implements ReferenceSortedSet<K> {
    protected AbstractReferenceSortedSet() {
    }

    @Override
    public abstract ObjectBidirectionalIterator<K> iterator();
}
