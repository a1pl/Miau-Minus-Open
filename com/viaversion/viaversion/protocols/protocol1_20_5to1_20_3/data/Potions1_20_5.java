package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data;

import com.viaversion.viaversion.util.KeyMappings;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Potions1_20_5 {
    private static final KeyMappings POTIONS = new KeyMappings(
        "water",
        "mundane",
        "thick",
        "awkward",
        "night_vision",
        "long_night_vision",
        "invisibility",
        "long_invisibility",
        "leaping",
        "long_leaping",
        "strong_leaping",
        "fire_resistance",
        "long_fire_resistance",
        "swiftness",
        "long_swiftness",
        "strong_swiftness",
        "slowness",
        "long_slowness",
        "strong_slowness",
        "turtle_master",
        "long_turtle_master",
        "strong_turtle_master",
        "water_breathing",
        "long_water_breathing",
        "healing",
        "strong_healing",
        "harming",
        "strong_harming",
        "poison",
        "long_poison",
        "strong_poison",
        "regeneration",
        "long_regeneration",
        "strong_regeneration",
        "strength",
        "long_strength",
        "strong_strength",
        "weakness",
        "long_weakness",
        "luck",
        "slow_falling",
        "long_slow_falling",
        "wind_charged",
        "weaving",
        "oozing",
        "infested"
    );

    public static @Nullable String idToKey(int id) {
        return POTIONS.idToKey(id);
    }

    public static int keyToId(String potion) {
        return POTIONS.keyToId(potion);
    }
}
