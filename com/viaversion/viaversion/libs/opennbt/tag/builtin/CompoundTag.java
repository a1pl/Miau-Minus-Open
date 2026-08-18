package com.viaversion.viaversion.libs.opennbt.tag.builtin;

import com.viaversion.viaversion.libs.opennbt.tag.TagRegistry;
import com.viaversion.viaversion.libs.opennbt.tag.limiter.TagLimiter;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;

public final class CompoundTag extends Tag implements Iterable<Entry<String, Tag>> {
    public static final int ID = 10;
    private Map<String, Tag> value;

    public CompoundTag() {
        this(new LinkedHashMap<>());
    }

    public CompoundTag(Map<String, Tag> value) {
        this.value = new LinkedHashMap<>(value);
    }

    public CompoundTag(LinkedHashMap<String, Tag> value) {
        if (value == null) {
            throw new NullPointerException("value cannot be null");
        }

        this.value = value;
    }

    public static CompoundTag read(DataInput in, TagLimiter tagLimiter, int nestingLevel) throws IOException {
        tagLimiter.checkLevel(nestingLevel);
        int newNestingLevel = nestingLevel + 1;
        CompoundTag compoundTag = new CompoundTag();

        while (true) {
            tagLimiter.countByte();
            int id = in.readByte();
            if (id == 0) {
                return compoundTag;
            }

            String name = in.readUTF();
            tagLimiter.countBytes(2 * name.length());

            Tag tag;
            try {
                tag = TagRegistry.read(id, in, tagLimiter, newNestingLevel);
            } catch (IllegalArgumentException e) {
                throw new IOException("Failed to create tag.", e);
            }

            compoundTag.value.put(name, tag);
        }
    }

    public Map<String, Tag> getValue() {
        return this.value;
    }

    @Override
    public String asRawString() {
        return this.value.toString();
    }

    public void setValue(Map<String, Tag> value) {
        if (value == null) {
            throw new NullPointerException("value cannot be null");
        }

        this.value = new LinkedHashMap<>(value);
    }

    public void setValue(LinkedHashMap<String, Tag> value) {
        if (value == null) {
            throw new NullPointerException("value cannot be null");
        }

        this.value = value;
    }

    public boolean isEmpty() {
        return this.value.isEmpty();
    }

    public boolean contains(String tagName) {
        return this.value.containsKey(tagName);
    }

    @Nullable
    public <T extends Tag> T get(String tagName) {
        return (T)this.value.get(tagName);
    }

    @Nullable
    public <T extends Tag> T getUnchecked(String tagName) {
        return (T)this.value.get(tagName);
    }

    @Nullable
    public StringTag getStringTag(String tagName) {
        Tag tag = this.value.get(tagName);
        return tag instanceof StringTag ? (StringTag)tag : null;
    }

    @Nullable
    public CompoundTag getCompoundTag(String tagName) {
        Tag tag = this.value.get(tagName);
        return tag instanceof CompoundTag ? (CompoundTag)tag : null;
    }

    @Nullable
    public ListTag<?> getListTag(String tagName) {
        Tag tag = this.value.get(tagName);
        return tag instanceof ListTag ? (ListTag)tag : null;
    }

    @Nullable
    public <T extends Tag> ListTag<T> getListTag(String tagName, Class<T> type) {
        Tag tag = this.value.get(tagName);
        if (!(tag instanceof ListTag)) {
            return null;
        }

        Class<? extends Tag> elementType = ((ListTag)tag).getElementType();
        return elementType != type && elementType != null ? null : (ListTag)tag;
    }

    @Nullable
    public ListTag<? extends NumberTag> getNumberListTag(String tagName) {
        Tag tag = this.value.get(tagName);
        if (!(tag instanceof ListTag)) {
            return null;
        }

        Class<? extends Tag> elementType = ((ListTag)tag).getElementType();
        return elementType != null && !NumberTag.class.isAssignableFrom(elementType) ? null : (ListTag)tag;
    }

    @Nullable
    public IntTag getIntTag(String tagName) {
        Tag tag = this.value.get(tagName);
        return tag instanceof IntTag ? (IntTag)tag : null;
    }

    @Nullable
    public LongTag getLongTag(String tagName) {
        Tag tag = this.value.get(tagName);
        return tag instanceof LongTag ? (LongTag)tag : null;
    }

    @Nullable
    public ShortTag getShortTag(String tagName) {
        Tag tag = this.value.get(tagName);
        return tag instanceof ShortTag ? (ShortTag)tag : null;
    }

    @Nullable
    public ByteTag getByteTag(String tagName) {
        Tag tag = this.value.get(tagName);
        return tag instanceof ByteTag ? (ByteTag)tag : null;
    }

    @Nullable
    public FloatTag getFloatTag(String tagName) {
        Tag tag = this.value.get(tagName);
        return tag instanceof FloatTag ? (FloatTag)tag : null;
    }

    @Nullable
    public DoubleTag getDoubleTag(String tagName) {
        Tag tag = this.value.get(tagName);
        return tag instanceof DoubleTag ? (DoubleTag)tag : null;
    }

    @Nullable
    public NumberTag getNumberTag(String tagName) {
        Tag tag = this.value.get(tagName);
        return tag instanceof NumberTag ? (NumberTag)tag : null;
    }

    @Nullable
    public ByteArrayTag getByteArrayTag(String tagName) {
        Tag tag = this.value.get(tagName);
        return tag instanceof ByteArrayTag ? (ByteArrayTag)tag : null;
    }

    @Nullable
    public IntArrayTag getIntArrayTag(String tagName) {
        Tag tag = this.value.get(tagName);
        return tag instanceof IntArrayTag ? (IntArrayTag)tag : null;
    }

    @Nullable
    public LongArrayTag getLongArrayTag(String tagName) {
        Tag tag = this.value.get(tagName);
        return tag instanceof LongArrayTag ? (LongArrayTag)tag : null;
    }

    @Nullable
    public String getString(String tagName) {
        return this.getString(tagName, null);
    }

    @Nullable
    public String getString(String tagName, @Nullable String def) {
        Tag tag = this.value.get(tagName);
        return tag instanceof StringTag ? ((StringTag)tag).getValue() : def;
    }

    public int getInt(String tagName) {
        return this.getInt(tagName, 0);
    }

    public int getInt(String tagName, int def) {
        Tag tag = this.value.get(tagName);
        return tag instanceof NumberTag ? ((NumberTag)tag).asInt() : def;
    }

    public long getLong(String tagName) {
        return this.getLong(tagName, 0L);
    }

    public long getLong(String tagName, long def) {
        Tag tag = this.value.get(tagName);
        return tag instanceof NumberTag ? ((NumberTag)tag).asLong() : def;
    }

    public short getShort(String tagName) {
        return this.getShort(tagName, (short)0);
    }

    public short getShort(String tagName, short def) {
        Tag tag = this.value.get(tagName);
        return tag instanceof NumberTag ? ((NumberTag)tag).asShort() : def;
    }

    public byte getByte(String tagName) {
        return this.getByte(tagName, (byte)0);
    }

    public byte getByte(String tagName, byte def) {
        Tag tag = this.value.get(tagName);
        return tag instanceof NumberTag ? ((NumberTag)tag).asByte() : def;
    }

    public float getFloat(String tagName) {
        return this.getFloat(tagName, 0.0F);
    }

    public float getFloat(String tagName, float def) {
        Tag tag = this.value.get(tagName);
        return tag instanceof NumberTag ? ((NumberTag)tag).asFloat() : def;
    }

    public double getDouble(String tagName) {
        return this.getDouble(tagName, 0.0);
    }

    public double getDouble(String tagName, double def) {
        Tag tag = this.value.get(tagName);
        return tag instanceof NumberTag ? ((NumberTag)tag).asDouble() : def;
    }

    public boolean getBoolean(String tagName) {
        return this.getBoolean(tagName, false);
    }

    public boolean getBoolean(String tagName, boolean def) {
        Tag tag = this.value.get(tagName);
        return tag instanceof NumberTag ? ((NumberTag)tag).asBoolean() : def;
    }

    @Nullable
    public <T extends Tag> T put(String tagName, T tag) {
        if (tag == this) {
            throw new IllegalArgumentException("Cannot add a tag to itself");
        } else {
            return (T)this.value.put(tagName, tag);
        }
    }

    public void putString(String tagName, String value) {
        this.value.put(tagName, new StringTag(value));
    }

    public void putByte(String tagName, byte value) {
        this.value.put(tagName, new ByteTag(value));
    }

    public void putInt(String tagName, int value) {
        this.value.put(tagName, new IntTag(value));
    }

    public void putShort(String tagName, short value) {
        this.value.put(tagName, new ShortTag(value));
    }

    public void putLong(String tagName, long value) {
        this.value.put(tagName, new LongTag(value));
    }

    public void putFloat(String tagName, float value) {
        this.value.put(tagName, new FloatTag(value));
    }

    public void putDouble(String tagName, double value) {
        this.value.put(tagName, new DoubleTag(value));
    }

    public void putBoolean(String tagName, boolean value) {
        this.value.put(tagName, new ByteTag((byte)(value ? 1 : 0)));
    }

    public void putAll(CompoundTag compoundTag) {
        this.value.putAll(compoundTag.value);
    }

    @Nullable
    public <T extends Tag> T remove(String tagName) {
        return (T)this.value.remove(tagName);
    }

    @Nullable
    public <T extends Tag> T removeUnchecked(String tagName) {
        return (T)this.value.remove(tagName);
    }

    public Set<String> keySet() {
        return this.value.keySet();
    }

    public Collection<Tag> values() {
        return this.value.values();
    }

    public Set<Entry<String, Tag>> entrySet() {
        return this.value.entrySet();
    }

    public int size() {
        return this.value.size();
    }

    public void clear() {
        this.value.clear();
    }

    @Override
    public Iterator<Entry<String, Tag>> iterator() {
        return this.value.entrySet().iterator();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        for (Entry<String, Tag> entry : this.value.entrySet()) {
            Tag tag = entry.getValue();
            out.writeByte(tag.getTagId());
            out.writeUTF(entry.getKey());
            tag.write(out);
        }

        out.writeByte(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            CompoundTag tags = (CompoundTag)o;
            return this.value.equals(tags.value);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    public CompoundTag copy() {
        LinkedHashMap<String, Tag> newMap = new LinkedHashMap<>();

        for (Entry<String, Tag> entry : this.value.entrySet()) {
            newMap.put(entry.getKey(), entry.getValue().copy());
        }

        return new CompoundTag(newMap);
    }

    @Override
    public int getTagId() {
        return 10;
    }
}
