package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data;

import com.viaversion.viaversion.util.KeyMappings;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class EquipmentSlots1_20_5 {
    public static final KeyMappings SLOTS = new KeyMappings(
        "any", "mainhand", "offhand", "hand", "feet", "legs", "chest", "head", "armor", "body"
    );

    public static @Nullable String idToKey(int id) {
        return SLOTS.idToKey(id);
    }

    public static int keyToId(String enchantment) {
        return SLOTS.keyToId(enchantment);
    }
}
