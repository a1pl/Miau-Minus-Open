package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public final class AdventureModePredicate {
    public static final Type<AdventureModePredicate> TYPE = new Type<AdventureModePredicate>(
        AdventureModePredicate.class
    ) {
        public AdventureModePredicate read(ByteBuf buffer) throws Exception {
            BlockPredicate[] predicates = BlockPredicate.ARRAY_TYPE.read(buffer);
            boolean showInTooltip = buffer.readBoolean();
            return new AdventureModePredicate(predicates, showInTooltip);
        }

        public void write(ByteBuf buffer, AdventureModePredicate value) throws Exception {
            BlockPredicate.ARRAY_TYPE.write(buffer, value.predicates);
            buffer.writeBoolean(value.showInTooltip);
        }
    };
    private final BlockPredicate[] predicates;
    private final boolean showInTooltip;

    public AdventureModePredicate(BlockPredicate[] predicates, boolean showInTooltip) {
        this.predicates = predicates;
        this.showInTooltip = showInTooltip;
    }

    public BlockPredicate[] predicates() {
        return this.predicates;
    }

    public boolean showInTooltip() {
        return this.showInTooltip;
    }
}
