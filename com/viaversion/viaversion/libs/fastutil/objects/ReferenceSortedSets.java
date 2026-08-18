package com.viaversion.viaversion.libs.fastutil.objects;

import com.viaversion.viaversion.libs.fastutil.objects.ReferenceSortedSets.SynchronizedSortedSet;
import com.viaversion.viaversion.libs.fastutil.objects.ReferenceSortedSets.UnmodifiableSortedSet;
import java.io.Serializable;
import java.util.Comparator;
import java.util.NoSuchElementException;

public final class ReferenceSortedSets {
    public static final ReferenceSortedSets.EmptySet EMPTY_SET = new ReferenceSortedSets.EmptySet();

    private ReferenceSortedSets() {
    }

    public static <K> ReferenceSet<K> emptySet() {
        return EMPTY_SET;
    }

    public static <K> ReferenceSortedSet<K> singleton(K element) {
        return new ReferenceSortedSets.Singleton<>(element);
    }

    public static <K> ReferenceSortedSet<K> singleton(K element, Comparator<? super K> comparator) {
        return new ReferenceSortedSets.Singleton<>(element, comparator);
    }

    public static <K> ReferenceSortedSet<K> synchronize(ReferenceSortedSet<K> s) {
        return new SynchronizedSortedSet(s);
    }

    public static <K> ReferenceSortedSet<K> synchronize(ReferenceSortedSet<K> s, Object sync) {
        return new SynchronizedSortedSet(s, sync);
    }

    public static <K> ReferenceSortedSet<K> unmodifiable(ReferenceSortedSet<K> s) {
        return new UnmodifiableSortedSet(s);
    }

    public static class EmptySet<K>
        extends ReferenceSets.EmptySet<K>
        implements ReferenceSortedSet<K>,
        Serializable,
        Cloneable {
        private static final long serialVersionUID = -7046029254386353129L;

        protected EmptySet() {
        }

        @Override
        public ObjectBidirectionalIterator<K> iterator(K from) {
            return ObjectIterators.EMPTY_ITERATOR;
        }

        @Override
        public ReferenceSortedSet<K> subSet(K from, K to) {
            return ReferenceSortedSets.EMPTY_SET;
        }

        @Override
        public ReferenceSortedSet<K> headSet(K from) {
            return ReferenceSortedSets.EMPTY_SET;
        }

        @Override
        public ReferenceSortedSet<K> tailSet(K to) {
            return ReferenceSortedSets.EMPTY_SET;
        }

        @Override
        public K first() {
            throw new NoSuchElementException();
        }

        @Override
        public K last() {
            throw new NoSuchElementException();
        }

        @Override
        public Comparator<? super K> comparator() {
            return null;
        }

        @Override
        public Object clone() {
            return ReferenceSortedSets.EMPTY_SET;
        }

        private Object readResolve() {
            return ReferenceSortedSets.EMPTY_SET;
        }
    }

    public static class Singleton<K>
        extends ReferenceSets.Singleton<K>
        implements ReferenceSortedSet<K>,
        Serializable,
        Cloneable {
        private static final long serialVersionUID = -7046029254386353129L;
        final Comparator<? super K> comparator;

        protected Singleton(K element, Comparator<? super K> comparator) {
            super(element);
            this.comparator = comparator;
        }

        Singleton(K element) {
            this(element, null);
        }

        final int compare(K k1, K k2) {
            return this.comparator == null ? ((Comparable)k1).compareTo(k2) : this.comparator.compare(k1, k2);
        }

        @Override
        public ObjectBidirectionalIterator<K> iterator(K from) {
            ObjectBidirectionalIterator<K> i = this.iterator();
            if (this.compare(this.element, from) <= 0) {
                i.next();
            }

            return i;
        }

        @Override
        public Comparator<? super K> comparator() {
            return this.comparator;
        }

        @Override
        public ObjectSpliterator<K> spliterator() {
            return ObjectSpliterators.singleton(this.element, this.comparator);
        }

        @Override
        public ReferenceSortedSet<K> subSet(K from, K to) {
            return this.compare(from, this.element) <= 0 && this.compare(this.element, to) < 0
                ? this
                : ReferenceSortedSets.EMPTY_SET;
        }

        @Override
        public ReferenceSortedSet<K> headSet(K to) {
            return this.compare(this.element, to) < 0 ? this : ReferenceSortedSets.EMPTY_SET;
        }

        @Override
        public ReferenceSortedSet<K> tailSet(K from) {
            return this.compare(from, this.element) <= 0 ? this : ReferenceSortedSets.EMPTY_SET;
        }

        @Override
        public K first() {
            return this.element;
        }

        @Override
        public K last() {
            return this.element;
        }
    }
}
