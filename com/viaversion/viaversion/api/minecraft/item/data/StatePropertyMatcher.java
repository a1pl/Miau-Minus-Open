package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.ArrayType;
import com.viaversion.viaversion.util.Either;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class StatePropertyMatcher {
    public static final Type<StatePropertyMatcher> TYPE = new Type<StatePropertyMatcher>(StatePropertyMatcher.class) {
        public StatePropertyMatcher read(ByteBuf buffer) throws Exception {
            String name = Type.STRING.read(buffer);
            if (buffer.readBoolean()) {
                String value = Type.STRING.read(buffer);
                return new StatePropertyMatcher(name, Either.left(value));
            } else {
                String minValue = Type.OPTIONAL_STRING.read(buffer);
                String maxValue = Type.OPTIONAL_STRING.read(buffer);
                return new StatePropertyMatcher(
                    name, Either.right(new StatePropertyMatcher.RangedMatcher(minValue, maxValue))
                );
            }
        }

        public void write(ByteBuf buffer, StatePropertyMatcher value) throws Exception {
            Type.STRING.write(buffer, value.name);
            if (value.matcher.isLeft()) {
                buffer.writeBoolean(true);
                Type.STRING.write(buffer, value.matcher.left());
            } else {
                buffer.writeBoolean(false);
                Type.OPTIONAL_STRING.write(buffer, value.matcher.right().minValue());
                Type.OPTIONAL_STRING.write(buffer, value.matcher.right().maxValue());
            }
        }
    };
    public static final Type<StatePropertyMatcher[]> ARRAY_TYPE = new ArrayType<>(TYPE);
    private final String name;
    private final Either<String, StatePropertyMatcher.RangedMatcher> matcher;

    public StatePropertyMatcher(String name, Either<String, StatePropertyMatcher.RangedMatcher> matcher) {
        this.name = name;
        this.matcher = matcher;
    }

    public String name() {
        return this.name;
    }

    public Either<String, StatePropertyMatcher.RangedMatcher> matcher() {
        return this.matcher;
    }

    public static final class RangedMatcher {
        private final String minValue;
        private final String maxValue;

        public RangedMatcher(@Nullable String minValue, @Nullable String maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        public @Nullable String minValue() {
            return this.minValue;
        }

        public @Nullable String maxValue() {
            return this.maxValue;
        }
    }
}
