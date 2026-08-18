package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.minecraft.GlobalPosition;
import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class LodestoneTracker {
    public static final Type<LodestoneTracker> TYPE = new Type<LodestoneTracker>(LodestoneTracker.class) {
        public LodestoneTracker read(ByteBuf buffer) throws Exception {
            GlobalPosition position = Type.OPTIONAL_GLOBAL_POSITION.read(buffer);
            boolean tracked = buffer.readBoolean();
            return new LodestoneTracker(position, tracked);
        }

        public void write(ByteBuf buffer, LodestoneTracker value) throws Exception {
            Type.OPTIONAL_GLOBAL_POSITION.write(buffer, value.position);
            buffer.writeBoolean(value.tracked);
        }
    };
    private final GlobalPosition position;
    private final boolean tracked;

    public LodestoneTracker(@Nullable GlobalPosition position, boolean tracked) {
        this.position = position;
        this.tracked = tracked;
    }

    public @Nullable GlobalPosition pos() {
        return this.position;
    }

    public boolean tracked() {
        return this.tracked;
    }
}
