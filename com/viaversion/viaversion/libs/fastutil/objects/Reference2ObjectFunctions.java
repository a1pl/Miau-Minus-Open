package com.viaversion.viaversion.libs.fastutil.objects;

import com.viaversion.viaversion.libs.fastutil.Function;
import com.viaversion.viaversion.libs.fastutil.objects.Reference2ObjectFunctions.SynchronizedFunction;
import com.viaversion.viaversion.libs.fastutil.objects.Reference2ObjectFunctions.UnmodifiableFunction;
import java.io.Serializable;

public final class Reference2ObjectFunctions {
    public static final Reference2ObjectFunctions.EmptyFunction EMPTY_FUNCTION = new Reference2ObjectFunctions.EmptyFunction();

    private Reference2ObjectFunctions() {
    }

    public static <K, V> Reference2ObjectFunction<K, V> singleton(K key, V value) {
        return new Reference2ObjectFunctions.Singleton<>(key, value);
    }

    public static <K, V> Reference2ObjectFunction<K, V> synchronize(Reference2ObjectFunction<K, V> f) {
        return new SynchronizedFunction(f);
    }

    public static <K, V> Reference2ObjectFunction<K, V> synchronize(Reference2ObjectFunction<K, V> f, Object sync) {
        return new SynchronizedFunction(f, sync);
    }

    public static <K, V> Reference2ObjectFunction<K, V> unmodifiable(
        Reference2ObjectFunction<? extends K, ? extends V> f
    ) {
        return new UnmodifiableFunction(f);
    }

    public static class EmptyFunction<K, V>
        extends AbstractReference2ObjectFunction<K, V>
        implements Serializable,
        Cloneable {
        private static final long serialVersionUID = -7046029254386353129L;

        protected EmptyFunction() {
        }

        @Override
        public V get(Object k) {
            return null;
        }

        @Override
        public V getOrDefault(Object k, V defaultValue) {
            return defaultValue;
        }

        @Override
        public boolean containsKey(Object k) {
            return false;
        }

        @Override
        public V defaultReturnValue() {
            return null;
        }

        @Override
        public void defaultReturnValue(V defRetValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void clear() {
        }

        @Override
        public Object clone() {
            return Reference2ObjectFunctions.EMPTY_FUNCTION;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            return !(o instanceof Function) ? false : ((Function)o).size() == 0;
        }

        @Override
        public String toString() {
            return "{}";
        }

        private Object readResolve() {
            return Reference2ObjectFunctions.EMPTY_FUNCTION;
        }
    }

    public static class Singleton<K, V>
        extends AbstractReference2ObjectFunction<K, V>
        implements Serializable,
        Cloneable {
        private static final long serialVersionUID = -7046029254386353129L;
        protected final K key;
        protected final V value;

        protected Singleton(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean containsKey(Object k) {
            return this.key == k;
        }

        @Override
        public V get(Object k) {
            return this.key == k ? this.value : this.defRetValue;
        }

        @Override
        public V getOrDefault(Object k, V defaultValue) {
            return this.key == k ? this.value : defaultValue;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public Object clone() {
            return this;
        }
    }
}
