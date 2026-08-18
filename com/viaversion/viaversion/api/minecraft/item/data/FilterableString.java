package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.ArrayType;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class FilterableString extends Filterable<String> {
    public static final Type<FilterableString> TYPE = new Filterable.FilterableType<String, FilterableString>(
        Type.STRING, Type.OPTIONAL_STRING, FilterableString.class
    ) {
        protected FilterableString create(String raw, String filtered) {
            return new FilterableString(raw, filtered);
        }
    };
    public static final Type<FilterableString[]> ARRAY_TYPE = new ArrayType<>(TYPE);

    public FilterableString(String raw, @Nullable String filtered) {
        super(raw, filtered);
    }
}
