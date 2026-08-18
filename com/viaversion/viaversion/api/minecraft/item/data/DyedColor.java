package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public final class DyedColor {
    public static final Type<DyedColor> TYPE = new Type<DyedColor>(DyedColor.class) {
        public DyedColor read(ByteBuf buffer) {
            int rgb = buffer.readInt();
            boolean showInTooltip = buffer.readBoolean();
            return new DyedColor(rgb, showInTooltip);
        }

        public void write(ByteBuf buffer, DyedColor value) {
            buffer.writeInt(value.rgb);
            buffer.writeBoolean(value.showInTooltip);
        }
    };
    private final int rgb;
    private final boolean showInTooltip;

    public DyedColor(int rgb, boolean showInTooltip) {
        this.rgb = rgb;
        this.showInTooltip = showInTooltip;
    }

    public int rgb() {
        return this.rgb;
    }

    public boolean showInTooltip() {
        return this.showInTooltip;
    }
}
