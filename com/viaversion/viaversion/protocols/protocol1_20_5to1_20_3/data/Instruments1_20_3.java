package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data;

import com.viaversion.viaversion.util.KeyMappings;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Instruments1_20_3 {
    private static final KeyMappings MAPPINGS = new KeyMappings(
        "ponder_goat_horn",
        "sing_goat_horn",
        "seek_goat_horn",
        "feel_goat_horn",
        "admire_goat_horn",
        "call_goat_horn",
        "yearn_goat_horn",
        "dream_goat_horn"
    );

    public static @Nullable String idToKey(int id) {
        return MAPPINGS.idToKey(id);
    }

    public static int keyToId(String name) {
        return MAPPINGS.keyToId(name);
    }
}
