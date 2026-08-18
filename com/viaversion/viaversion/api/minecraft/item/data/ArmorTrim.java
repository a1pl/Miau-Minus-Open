package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public final class ArmorTrim {
    public static final Type<ArmorTrim> TYPE = new Type<ArmorTrim>(ArmorTrim.class) {
        public ArmorTrim read(ByteBuf buffer) throws Exception {
            Holder<ArmorTrimMaterial> material = ArmorTrimMaterial.TYPE.read(buffer);
            Holder<ArmorTrimPattern> pattern = ArmorTrimPattern.TYPE.read(buffer);
            boolean showInTooltip = buffer.readBoolean();
            return new ArmorTrim(material, pattern, showInTooltip);
        }

        public void write(ByteBuf buffer, ArmorTrim value) throws Exception {
            ArmorTrimMaterial.TYPE.write(buffer, value.material);
            ArmorTrimPattern.TYPE.write(buffer, value.pattern);
            buffer.writeBoolean(value.showInTooltip);
        }
    };
    private final Holder<ArmorTrimMaterial> material;
    private final Holder<ArmorTrimPattern> pattern;
    private final boolean showInTooltip;

    public ArmorTrim(Holder<ArmorTrimMaterial> material, Holder<ArmorTrimPattern> pattern, boolean showInTooltip) {
        this.material = material;
        this.pattern = pattern;
        this.showInTooltip = showInTooltip;
    }

    public Holder<ArmorTrimMaterial> material() {
        return this.material;
    }

    public Holder<ArmorTrimPattern> pattern() {
        return this.pattern;
    }

    public boolean showInTooltip() {
        return this.showInTooltip;
    }
}
