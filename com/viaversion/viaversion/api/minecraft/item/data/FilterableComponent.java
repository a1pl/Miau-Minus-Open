package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.ArrayType;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class FilterableComponent extends Filterable<Tag> {
    public static final Type<FilterableComponent> TYPE = new Filterable.FilterableType<Tag, FilterableComponent>(
        Type.TAG, Type.OPTIONAL_TAG, FilterableComponent.class
    ) {
        protected FilterableComponent create(Tag raw, Tag filtered) {
            return new FilterableComponent(raw, filtered);
        }
    };
    public static final Type<FilterableComponent[]> ARRAY_TYPE = new ArrayType<>(TYPE);

    public FilterableComponent(Tag raw, @Nullable Tag filtered) {
        super(raw, filtered);
    }
}
