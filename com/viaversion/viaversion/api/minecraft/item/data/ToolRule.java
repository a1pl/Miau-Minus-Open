package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.ArrayType;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ToolRule {
    public static final Type<ToolRule> TYPE = new Type<ToolRule>(ToolRule.class) {
        public ToolRule read(ByteBuf buffer) throws Exception {
            HolderSet blocks = Type.HOLDER_SET.read(buffer);
            Float speed = Type.OPTIONAL_FLOAT.read(buffer);
            Boolean correctForDrops = Type.OPTIONAL_BOOLEAN.read(buffer);
            return new ToolRule(blocks, speed, correctForDrops);
        }

        public void write(ByteBuf buffer, ToolRule value) throws Exception {
            Type.HOLDER_SET.write(buffer, value.blocks);
            Type.OPTIONAL_FLOAT.write(buffer, value.speed);
            Type.OPTIONAL_BOOLEAN.write(buffer, value.correctForDrops);
        }
    };
    public static final Type<ToolRule[]> ARRAY_TYPE = new ArrayType<>(TYPE);
    private final HolderSet blocks;
    private final Float speed;
    private final Boolean correctForDrops;

    public ToolRule(HolderSet blocks, @Nullable Float speed, @Nullable Boolean correctForDrops) {
        this.blocks = blocks;
        this.speed = speed;
        this.correctForDrops = correctForDrops;
    }

    public HolderSet blocks() {
        return this.blocks;
    }

    public @Nullable Float speed() {
        return this.speed;
    }

    public @Nullable Boolean correctForDrops() {
        return this.correctForDrops;
    }
}
