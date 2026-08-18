package com.viaversion.viabackwards.api.data;

import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.util.IdAndData;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MappedLegacyBlockItem {
    private final int id;
    private final short data;
    private final String name;
    private final IdAndData block;
    private final MappedLegacyBlockItem.Type type;
    private MappedLegacyBlockItem.BlockEntityHandler blockEntityHandler;

    public MappedLegacyBlockItem(int id) {
        this(id, (short)-1, null, MappedLegacyBlockItem.Type.ITEM);
    }

    public MappedLegacyBlockItem(int id, short data, @Nullable String name, MappedLegacyBlockItem.Type type) {
        this.id = id;
        this.data = data;
        this.name = name != null ? "§f" + name : null;
        this.block = type != MappedLegacyBlockItem.Type.ITEM
            ? (data != -1 ? new IdAndData(id, data) : new IdAndData(id))
            : null;
        this.type = type;
    }

    public int getId() {
        return this.id;
    }

    public short getData() {
        return this.data;
    }

    public String getName() {
        return this.name;
    }

    public MappedLegacyBlockItem.Type getType() {
        return this.type;
    }

    public IdAndData getBlock() {
        return this.block;
    }

    public boolean hasBlockEntityHandler() {
        return this.blockEntityHandler != null;
    }

    public MappedLegacyBlockItem.@Nullable BlockEntityHandler getBlockEntityHandler() {
        return this.blockEntityHandler;
    }

    public void setBlockEntityHandler(MappedLegacyBlockItem.@Nullable BlockEntityHandler blockEntityHandler) {
        this.blockEntityHandler = blockEntityHandler;
    }

    @FunctionalInterface
    public interface BlockEntityHandler {
        CompoundTag handleOrNewCompoundTag(int var1, CompoundTag var2);
    }

    public enum Type {
        ITEM("items"),
        BLOCK_ITEM("block-items"),
        BLOCK("blocks");

        final String name;

        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }
}
