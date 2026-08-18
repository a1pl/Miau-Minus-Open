package com.viaversion.viaversion.api.minecraft;

import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class GameProfile {
    private final String name;
    private final UUID id;
    private final GameProfile.Property[] properties;

    public GameProfile(@Nullable String name, @Nullable UUID id, GameProfile.Property[] properties) {
        this.name = name;
        this.id = id;
        this.properties = properties;
    }

    public @Nullable String name() {
        return this.name;
    }

    public @Nullable UUID id() {
        return this.id;
    }

    public GameProfile.Property[] properties() {
        return this.properties;
    }

    public static final class Property {
        private final String name;
        private final String value;
        private final String signature;

        public Property(String name, String value, @Nullable String signature) {
            this.name = name;
            this.value = value;
            this.signature = signature;
        }

        public String name() {
            return this.name;
        }

        public String value() {
            return this.value;
        }

        public @Nullable String signature() {
            return this.signature;
        }
    }
}
