package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public final class Unbreakable {
    public static final Type<Unbreakable> TYPE = new Type<Unbreakable>(Unbreakable.class) {
        public Unbreakable read(ByteBuf buffer) {
            return new Unbreakable(buffer.readBoolean());
        }

        public void write(ByteBuf buffer, Unbreakable value) {
            buffer.writeBoolean(value.showInTooltip());
        }
    };
    private final boolean showInTooltip;

    public Unbreakable(boolean showInTooltip) {
        this.showInTooltip = showInTooltip;
    }

    public boolean showInTooltip() {
        return this.showInTooltip;
    }
}
