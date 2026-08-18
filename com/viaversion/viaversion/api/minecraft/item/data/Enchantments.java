package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntOpenHashMap;
import io.netty.buffer.ByteBuf;

public final class Enchantments {
    public static final Type<Enchantments> TYPE = new Type<Enchantments>(Enchantments.class) {
        public Enchantments read(ByteBuf buffer) {
            Int2IntMap enchantments = new Int2IntOpenHashMap();
            int size = Type.VAR_INT.readPrimitive(buffer);

            for (int i = 0; i < size; i++) {
                int id = Type.VAR_INT.readPrimitive(buffer);
                int level = Type.VAR_INT.readPrimitive(buffer);
                enchantments.put(id, level);
            }

            return new Enchantments(enchantments, buffer.readBoolean());
        }

        public void write(ByteBuf buffer, Enchantments value) {
            Type.VAR_INT.writePrimitive(buffer, value.enchantments.size());

            for (Int2IntMap.Entry entry : value.enchantments.int2IntEntrySet()) {
                Type.VAR_INT.writePrimitive(buffer, entry.getIntKey());
                Type.VAR_INT.writePrimitive(buffer, entry.getIntValue());
            }

            buffer.writeBoolean(value.showInTooltip());
        }
    };
    private final Int2IntMap enchantments;
    private final boolean showInTooltip;

    public Enchantments(Int2IntMap enchantments, boolean showInTooltip) {
        this.enchantments = enchantments;
        this.showInTooltip = showInTooltip;
    }

    public Enchantments(boolean showInTooltip) {
        this(new Int2IntOpenHashMap(), showInTooltip);
    }

    public Int2IntMap enchantments() {
        return this.enchantments;
    }

    public int size() {
        return this.enchantments.size();
    }

    public boolean showInTooltip() {
        return this.showInTooltip;
    }

    public void add(int id, int level) {
        this.enchantments.put(id, level);
    }

    public void remove(int id) {
        this.enchantments.remove(id);
    }

    public void clear() {
        this.enchantments.clear();
    }

    public int getLevel(int id) {
        return this.enchantments.getOrDefault(id, -1);
    }
}
