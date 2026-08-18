package com.viaversion.viaversion.util;

import java.util.regex.Pattern;

public final class Key {
    private static final Pattern PATTERN = Pattern.compile("([0-9a-z_.-]*:)?[0-9a-z_/.-]*");
    private static final int MINECRAFT_NAMESPACE_LENGTH = "minecraft:".length();

    public static String stripNamespace(String identifier) {
        int index = identifier.indexOf(58);
        return index == -1 ? identifier : identifier.substring(index + 1);
    }

    public static String stripMinecraftNamespace(String identifier) {
        if (identifier.startsWith("minecraft:")) {
            return identifier.substring(MINECRAFT_NAMESPACE_LENGTH);
        } else {
            return !identifier.isEmpty() && identifier.charAt(0) == ':' ? identifier.substring(1) : identifier;
        }
    }

    public static boolean equals(String firstIdentifier, String secondIdentifier) {
        return stripNamespace(firstIdentifier).equals(stripNamespace(secondIdentifier));
    }

    public static String namespaced(String identifier) {
        int index = identifier.indexOf(58);
        if (index == -1) {
            return "minecraft:" + identifier;
        } else {
            return index == 0 ? "minecraft" + identifier : identifier;
        }
    }

    public static boolean isValid(String identifier) {
        return PATTERN.matcher(identifier).matches();
    }
}
