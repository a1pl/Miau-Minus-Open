package com.viaversion.viaversion.api.minecraft;

import java.util.Arrays;

public final class TagData {
    private final String identifier;
    private final int[] entries;

    public TagData(String identifier, int[] entries) {
        this.identifier = identifier;
        this.entries = entries;
    }

    public String identifier() {
        return this.identifier;
    }

    public int[] entries() {
        return this.entries;
    }

    @Override
    public String toString() {
        return "TagData{identifier='" + this.identifier + '\'' + ", entries=" + Arrays.toString(this.entries) + '}';
    }
}
