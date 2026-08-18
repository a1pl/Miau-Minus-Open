package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.ArrayType;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BlockPredicate {
    public static final Type<BlockPredicate> TYPE = new Type<BlockPredicate>(BlockPredicate.class) {
        public BlockPredicate read(ByteBuf buffer) throws Exception {
            HolderSet holders = Type.OPTIONAL_HOLDER_SET.read(buffer);
            StatePropertyMatcher[] propertyMatchers = buffer.readBoolean()
                ? StatePropertyMatcher.ARRAY_TYPE.read(buffer)
                : null;
            CompoundTag tag = Type.OPTIONAL_COMPOUND_TAG.read(buffer);
            return new BlockPredicate(holders, propertyMatchers, tag);
        }

        public void write(ByteBuf buffer, BlockPredicate value) throws Exception {
            Type.OPTIONAL_HOLDER_SET.write(buffer, value.holderSet);
            buffer.writeBoolean(value.propertyMatchers != null);
            if (value.propertyMatchers != null) {
                StatePropertyMatcher.ARRAY_TYPE.write(buffer, value.propertyMatchers);
            }

            Type.OPTIONAL_COMPOUND_TAG.write(buffer, value.tag);
        }
    };
    public static final Type<BlockPredicate[]> ARRAY_TYPE = new ArrayType<>(TYPE);
    private final HolderSet holderSet;
    private final StatePropertyMatcher[] propertyMatchers;
    private final CompoundTag tag;

    public BlockPredicate(
        @Nullable HolderSet holderSet, StatePropertyMatcher @Nullable [] propertyMatchers, @Nullable CompoundTag tag
    ) {
        this.holderSet = holderSet;
        this.propertyMatchers = propertyMatchers;
        this.tag = tag;
    }

    public @Nullable HolderSet holderSet() {
        return this.holderSet;
    }

    public StatePropertyMatcher @Nullable [] propertyMatchers() {
        return this.propertyMatchers;
    }

    public @Nullable CompoundTag tag() {
        return this.tag;
    }
}
