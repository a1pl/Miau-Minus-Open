package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data;

import com.viaversion.viaversion.util.KeyMappings;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TrimMaterials1_20_3 {
    private static final KeyMappings MATERIALS = new KeyMappings(
        "amethyst", "copper", "diamond", "emerald", "gold", "iron", "lapis", "netherite", "quartz", "redstone"
    );

    public static @Nullable String idToKey(int id) {
        return MATERIALS.idToKey(id);
    }

    public static int keyToId(String material) {
        return MATERIALS.keyToId(material);
    }
}
