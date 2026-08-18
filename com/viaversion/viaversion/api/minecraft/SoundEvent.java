package com.viaversion.viaversion.api.minecraft;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class SoundEvent {
    private final String identifier;
    private final Float fixedRange;

    public SoundEvent(String identifier, @Nullable Float fixedRange) {
        this.identifier = identifier;
        this.fixedRange = fixedRange;
    }

    public String identifier() {
        return this.identifier;
    }

    public @Nullable Float fixedRange() {
        return this.fixedRange;
    }

    public SoundEvent withIdentifier(String identifier) {
        return new SoundEvent(identifier, this.fixedRange);
    }
}
