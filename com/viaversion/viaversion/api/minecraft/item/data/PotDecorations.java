package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public final class PotDecorations {
    public static final Type<PotDecorations> TYPE = new Type<PotDecorations>(PotDecorations.class) {
        public PotDecorations read(ByteBuf buffer) throws Exception {
            return new PotDecorations(Type.VAR_INT_ARRAY_PRIMITIVE.read(buffer));
        }

        public void write(ByteBuf buffer, PotDecorations value) throws Exception {
            Type.VAR_INT_ARRAY_PRIMITIVE.write(buffer, value.itemIds());
        }
    };
    private final int[] itemIds;

    public PotDecorations(int[] itemIds) {
        this.itemIds = itemIds;
    }

    public PotDecorations(int backItem, int leftItem, int rightItem, int frontItem) {
        this.itemIds = new int[]{backItem, leftItem, rightItem, frontItem};
    }

    public int[] itemIds() {
        return this.itemIds;
    }

    public int backItem() {
        return this.item(0);
    }

    public int leftItem() {
        return this.item(1);
    }

    public int rightItem() {
        return this.item(2);
    }

    public int frontItem() {
        return this.item(3);
    }

    private int item(int index) {
        return index >= 0 && index < this.itemIds.length ? this.itemIds[index] : -1;
    }
}
