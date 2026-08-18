package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data;

import com.viaversion.viaversion.util.KeyMappings;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TrimPatterns1_20_3 {
    private static final KeyMappings PATTERNS = new KeyMappings(
        "coast",
        "dune",
        "eye",
        "host",
        "raiser",
        "rib",
        "sentry",
        "shaper",
        "silence",
        "snout",
        "spire",
        "tide",
        "vex",
        "ward",
        "wayfinder",
        "wild"
    );

    public static @Nullable String idToKey(int id) {
        return PATTERNS.idToKey(id);
    }

    public static int keyToId(String pattern) {
        return PATTERNS.keyToId(pattern);
    }
}
