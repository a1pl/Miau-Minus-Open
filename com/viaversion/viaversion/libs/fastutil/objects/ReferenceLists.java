package com.viaversion.viaversion.libs.fastutil.objects;

import com.viaversion.viaversion.libs.fastutil.objects.ReferenceLists.SynchronizedList;
import com.viaversion.viaversion.libs.fastutil.objects.ReferenceLists.SynchronizedRandomAccessList;
import com.viaversion.viaversion.libs.fastutil.objects.ReferenceLists.UnmodifiableList;
import com.viaversion.viaversion.libs.fastutil.objects.ReferenceLists.UnmodifiableRandomAccessList;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class ReferenceLists {
    public static final ReferenceLists.EmptyList EMPTY_LIST = new ReferenceLists.EmptyList();

    private ReferenceLists() {
    }

    public static <K> ReferenceList<K> shuffle(ReferenceList<K> l, Random random) {
        int i = l.size();

        while (i-- != 0) {
            int p = random.nextInt(i + 1);
            K t = l.get(i);
            l.set(i, l.get(p));
            l.set(p, t);
        }

        return l;
    }

    public static <K> ReferenceList<K> emptyList() {
        return EMPTY_LIST;
    }

    public static <K> ReferenceList<K> singleton(K element) {
        return new ReferenceLists.Singleton<>(element);
    }

    public static <K> ReferenceList<K> synchronize(ReferenceList<K> l) {
        return (ReferenceList<K>)(l instanceof RandomAccess
            ? new SynchronizedRandomAccessList(l)
            : new SynchronizedList(l));
    }

    public static <K> ReferenceList<K> synchronize(ReferenceList<K> l, Object sync) {
        return (ReferenceList<K>)(l instanceof RandomAccess
            ? new SynchronizedRandomAccessList(l, sync)
            : new SynchronizedList(l, sync));
    }

    public static <K> ReferenceList<K> unmodifiable(ReferenceList<? extends K> l) {
        return (ReferenceList<K>)(l instanceof RandomAccess
            ? new UnmodifiableRandomAccessList(l)
            : new UnmodifiableList(l));
    }

    public static class EmptyList<K>
        extends ReferenceCollections.EmptyCollection<K>
        implements ReferenceList<K>,
        RandomAccess,
        Serializable,
        Cloneable {
        private static final long serialVersionUID = -7046029254386353129L;

        protected EmptyList() {
        }

        @Override
        public K get(int i) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public boolean remove(Object k) {
            throw new UnsupportedOperationException();
        }

        @Override
        public K remove(int i) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int index, K k) {
            throw new UnsupportedOperationException();
        }

        @Override
        public K set(int index, K k) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int indexOf(Object k) {
            return -1;
        }

        @Override
        public int lastIndexOf(Object k) {
            return -1;
        }

        @Override
        public boolean addAll(int i, Collection<? extends K> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceAll(UnaryOperator<K> operator) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sort(Comparator<? super K> comparator) {
        }

        @Override
        public void unstableSort(Comparator<? super K> comparator) {
        }

        @Override
        public ObjectListIterator<K> listIterator() {
            return ObjectIterators.EMPTY_ITERATOR;
        }

        @Override
        public ObjectListIterator<K> iterator() {
            return ObjectIterators.EMPTY_ITERATOR;
        }

        @Override
        public ObjectListIterator<K> listIterator(int i) {
            if (i == 0) {
                return ObjectIterators.EMPTY_ITERATOR;
            } else {
                throw new IndexOutOfBoundsException(String.valueOf(i));
            }
        }

        @Override
        public ReferenceList<K> subList(int from, int to) {
            if (from == 0 && to == 0) {
                return this;
            } else {
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public void getElements(int from, Object[] a, int offset, int length) {
            if (from != 0 || length != 0 || offset < 0 || offset > a.length) {
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public void removeElements(int from, int to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addElements(int index, K[] a, int offset, int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addElements(int index, K[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setElements(K[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setElements(int index, K[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setElements(int index, K[] a, int offset, int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void size(int s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object clone() {
            return ReferenceLists.EMPTY_LIST;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof List && ((List)o).isEmpty();
        }

        @Override
        public String toString() {
            return "[]";
        }

        private Object readResolve() {
            return ReferenceLists.EMPTY_LIST;
        }
    }

    abstract static class ImmutableListBase<K> extends AbstractReferenceList<K> implements ReferenceList<K> {
        @Deprecated
        @Override
        public final void add(int index, K k) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final boolean add(K k) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final boolean addAll(Collection<? extends K> c) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final boolean addAll(int index, Collection<? extends K> c) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final K remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final boolean remove(Object k) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final boolean removeIf(Predicate<? super K> c) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final void replaceAll(UnaryOperator<K> operator) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final K set(int index, K k) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final void clear() {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final void size(int size) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final void removeElements(int from, int to) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final void addElements(int index, K[] a, int offset, int length) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final void setElements(int index, K[] a, int offset, int length) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final void sort(Comparator<? super K> comparator) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public final void unstableSort(Comparator<? super K> comparator) {
            throw new UnsupportedOperationException();
        }
    }

    public static class Singleton<K> extends AbstractReferenceList<K> implements RandomAccess, Serializable, Cloneable {
        private static final long serialVersionUID = -7046029254386353129L;
        private final K element;

        protected Singleton(K element) {
            this.element = element;
        }

        @Override
        public K get(int i) {
            if (i == 0) {
                return this.element;
            } else {
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public boolean remove(Object k) {
            throw new UnsupportedOperationException();
        }

        @Override
        public K remove(int i) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(Object k) {
            return k == this.element;
        }

        @Override
        public int indexOf(Object k) {
            return k == this.element ? 0 : -1;
        }

        @Override
        public Object[] toArray() {
            return new Object[]{this.element};
        }

        @Override
        public ObjectListIterator<K> listIterator() {
            return ObjectIterators.singleton(this.element);
        }

        @Override
        public ObjectListIterator<K> iterator() {
            return this.listIterator();
        }

        @Override
        public ObjectSpliterator<K> spliterator() {
            return ObjectSpliterators.singleton(this.element);
        }

        @Override
        public ObjectListIterator<K> listIterator(int i) {
            if (i <= 1 && i >= 0) {
                ObjectListIterator<K> l = this.listIterator();
                if (i == 1) {
                    l.next();
                }

                return l;
            } else {
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public ReferenceList<K> subList(int from, int to) {
            this.ensureIndex(from);
            this.ensureIndex(to);
            if (from > to) {
                throw new IndexOutOfBoundsException("Start index (" + from + ") is greater than end index (" + to + ")");
            } else {
                return from == 0 && to == 1 ? this : ReferenceLists.EMPTY_LIST;
            }
        }

        @Override
        public void forEach(Consumer<? super K> action) {
            action.accept(this.element);
        }

        @Override
        public boolean addAll(int i, Collection<? extends K> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends K> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeIf(Predicate<? super K> filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceAll(UnaryOperator<K> operator) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sort(Comparator<? super K> comparator) {
        }

        @Override
        public void unstableSort(Comparator<? super K> comparator) {
        }

        @Override
        public void getElements(int from, Object[] a, int offset, int length) {
            if (offset < 0) {
                throw new ArrayIndexOutOfBoundsException("Offset (" + offset + ") is negative");
            }

            if (offset + length > a.length) {
                throw new ArrayIndexOutOfBoundsException(
                    "End index (" + (offset + length) + ") is greater than array length (" + a.length + ")"
                );
            }

            if (from + length > this.size()) {
                throw new IndexOutOfBoundsException(
                    "End index (" + (from + length) + ") is greater than list size (" + this.size() + ")"
                );
            }

            if (length > 0) {
                a[offset] = this.element;
            }
        }

        @Override
        public void removeElements(int from, int to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addElements(int index, K[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addElements(int index, K[] a, int offset, int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setElements(K[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setElements(int index, K[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setElements(int index, K[] a, int offset, int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public void size(int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object clone() {
            return this;
        }
    }
}
