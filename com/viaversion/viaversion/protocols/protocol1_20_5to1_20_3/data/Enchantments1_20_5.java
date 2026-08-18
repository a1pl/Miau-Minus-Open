package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data;

import com.viaversion.viaversion.util.KeyMappings;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Enchantments1_20_5 {
    public static final KeyMappings ENCHANTMENTS = new KeyMappings(
        "protection",
        "fire_protection",
        "feather_falling",
        "blast_protection",
        "projectile_protection",
        "respiration",
        "aqua_affinity",
        "thorns",
        "depth_strider",
        "frost_walker",
        "binding_curse",
        "soul_speed",
        "swift_sneak",
        "sharpness",
        "smite",
        "bane_of_arthropods",
        "knockback",
        "fire_aspect",
        "looting",
        "sweeping_edge",
        "efficiency",
        "silk_touch",
        "unbreaking",
        "fortune",
        "power",
        "punch",
        "flame",
        "infinity",
        "luck_of_the_sea",
        "lure",
        "loyalty",
        "impaling",
        "riptide",
        "channeling",
        "multishot",
        "quick_charge",
        "piercing",
        "density",
        "breach",
        "wind_burst",
        "mending",
        "vanishing_curse"
    );

    public static @Nullable String idToKey(int id) {
        return ENCHANTMENTS.idToKey(id);
    }

    public static int keyToId(String enchantment) {
        return ENCHANTMENTS.keyToId(enchantment);
    }
}
