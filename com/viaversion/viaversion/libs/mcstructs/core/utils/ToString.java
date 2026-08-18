package com.viaversion.viaversion.libs.mcstructs.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class ToString {
    private final Class<?> clazz;
    private final List<String> fields;

    public static ToString of(Object object) {
        return of(object.getClass());
    }

    public static ToString of(Class<?> clazz) {
        return new ToString(clazz);
    }

    private ToString(Class<?> clazz) {
        this.clazz = clazz;
        this.fields = new ArrayList<>();
    }

    public <T> ToString add(String name, T value) {
        return this.add(name, value, v -> true);
    }

    public <T> ToString add(String name, T value, Predicate<T> condition) {
        return this.add(name, value, condition, String::valueOf);
    }

    public <T> ToString add(String name, T value, Predicate<T> condition, Function<T, String> mapper) {
        if (condition.test(value)) {
            String val = mapper.apply(value);
            if (value instanceof String) {
                val = "'" + val + "'";
            }

            this.fields.add(name + "=" + val);
        }

        return this;
    }

    @Override
    public String toString() {
        return this.clazz.getSimpleName() + "{" + String.join(", ", this.fields) + "}";
    }
}
