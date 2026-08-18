package com.viaversion.viaversion.protocols.protocol1_16to1_15_2.data;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public final class AttributeMappings {
    private static final BiMap<String, String> ATTRIBUTE_MAPPINGS = HashBiMap.create();

    public static BiMap<String, String> attributeIdentifierMappings() {
        return ATTRIBUTE_MAPPINGS;
    }

    static {
        ATTRIBUTE_MAPPINGS.put("generic.maxHealth", "minecraft:generic.max_health");
        ATTRIBUTE_MAPPINGS.put("zombie.spawnReinforcements", "minecraft:zombie.spawn_reinforcements");
        ATTRIBUTE_MAPPINGS.put("horse.jumpStrength", "minecraft:horse.jump_strength");
        ATTRIBUTE_MAPPINGS.put("generic.followRange", "minecraft:generic.follow_range");
        ATTRIBUTE_MAPPINGS.put("generic.knockbackResistance", "minecraft:generic.knockback_resistance");
        ATTRIBUTE_MAPPINGS.put("generic.movementSpeed", "minecraft:generic.movement_speed");
        ATTRIBUTE_MAPPINGS.put("generic.flyingSpeed", "minecraft:generic.flying_speed");
        ATTRIBUTE_MAPPINGS.put("generic.attackDamage", "minecraft:generic.attack_damage");
        ATTRIBUTE_MAPPINGS.put("generic.attackKnockback", "minecraft:generic.attack_knockback");
        ATTRIBUTE_MAPPINGS.put("generic.attackSpeed", "minecraft:generic.attack_speed");
        ATTRIBUTE_MAPPINGS.put("generic.armorToughness", "minecraft:generic.armor_toughness");
    }
}
