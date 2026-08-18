package com.viaversion.viaversion.libs.opennbt.tag.builtin;

import com.viaversion.viaversion.libs.opennbt.tag.TagRegistry;
import com.viaversion.viaversion.libs.opennbt.tag.limiter.TagLimiter;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public final class ListTag<T extends Tag> extends Tag implements Iterable<T> {
    public static final int ID = 9;
    private Class<T> type;
    private List<T> value;

    @Deprecated
    public ListTag() {
        this.value = new ArrayList<>();
    }

    public ListTag(Class<T> type) {
        this.type = type;
        this.value = new ArrayList<>();
    }

    public ListTag(List<T> value) {
        this.setValue(value);
    }

    public static ListTag<?> read(DataInput in, TagLimiter tagLimiter, int nestingLevel) throws IOException {
        tagLimiter.checkLevel(nestingLevel);
        tagLimiter.countBytes(5);
        int id = in.readByte();
        Class<? extends Tag> type = null;
        if (id != 0) {
            type = TagRegistry.getClassFor(id);
            if (type == null) {
                throw new IOException("Unknown tag ID in ListTag: " + id);
            }
        }

        return read(in, id, type, tagLimiter, nestingLevel);
    }

    private static <T extends Tag> ListTag<T> read(
        DataInput in, int id, Class<T> type, TagLimiter tagLimiter, int nestingLevel
    ) throws IOException {
        ListTag<T> listTag = new ListTag<>(type);
        int count = in.readInt();
        int newNestingLevel = nestingLevel + 1;

        for (int index = 0; index < count; index++) {
            T tag;
            try {
                tag = (T)TagRegistry.read(id, in, tagLimiter, newNestingLevel);
            } catch (IllegalArgumentException e) {
                throw new IOException("Failed to create tag.", e);
            }

            listTag.add(tag);
        }

        return listTag;
    }

    public List<T> getValue() {
        return this.value;
    }

    @Override
    public String asRawString() {
        return this.value.toString();
    }

    public void setValue(List<T> value) {
        this.value = new ArrayList<>(value);
        if (!value.isEmpty()) {
            if (this.type == null) {
                this.type = (Class<T>)value.get(0).getClass();
            }

            for (T t : value) {
                this.checkType(t);
            }
        }
    }

    @Nullable
    public Class<? extends Tag> getElementType() {
        return this.type;
    }

    public boolean add(T tag) throws IllegalArgumentException {
        this.checkAddedTag(tag);
        return this.value.add(tag);
    }

    private void checkAddedTag(T tag) {
        if (this.type == null) {
            this.type = (Class<T>)tag.getClass();
        } else {
            this.checkType(tag);
        }
    }

    private void checkType(Tag tag) {
        if (tag.getClass() != this.type) {
            throw new IllegalArgumentException(
                "Tag type " + tag.getClass().getSimpleName() + " differs from list type " + this.type.getSimpleName()
            );
        }
    }

    public boolean remove(T tag) {
        return this.value.remove(tag);
    }

    public T get(int index) {
        return this.value.get(index);
    }

    public T set(int index, T tag) {
        this.checkAddedTag(tag);
        return this.value.set(index, tag);
    }

    public T remove(int index) {
        return this.value.remove(index);
    }

    public int size() {
        return this.value.size();
    }

    public boolean isEmpty() {
        return this.value.isEmpty();
    }

    public Stream<T> stream() {
        return this.value.stream();
    }

    @Override
    public Iterator<T> iterator() {
        return this.value.iterator();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        if (this.value.isEmpty()) {
            out.writeByte(0);
        } else {
            int id = TagRegistry.getIdFor(this.type);
            if (id == -1) {
                throw new IOException("ListTag contains unregistered tag class.");
            }

            out.writeByte(id);
        }

        out.writeInt(this.value.size());

        for (Tag tag : this.value) {
            tag.write(out);
        }
    }

    public ListTag<T> copy() {
        ListTag<T> copy = new ListTag<>(this.type);
        copy.value = new ArrayList<>(this.value.size());

        for (T value : this.value) {
            copy.add((T)value.copy());
        }

        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            ListTag<?> tags = (ListTag<?>)o;
            return !Objects.equals(this.type, tags.type) ? false : this.value.equals(tags.value);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = this.type != null ? this.type.hashCode() : 0;
        return 31 * result + this.value.hashCode();
    }

    @Override
    public int getTagId() {
        return 9;
    }
}
