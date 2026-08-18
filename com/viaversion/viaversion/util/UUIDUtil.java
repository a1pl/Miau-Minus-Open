package com.viaversion.viaversion.util;

import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class UUIDUtil {
    public static UUID fromIntArray(int[] parts) {
        return parts.length != 4
            ? new UUID(0L, 0L)
            : new UUID((long)parts[0] << 32 | parts[1] & 4294967295L, (long)parts[2] << 32 | parts[3] & 4294967295L);
    }

    public static int[] toIntArray(UUID uuid) {
        return toIntArray(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    public static int[] toIntArray(long msb, long lsb) {
        return new int[]{(int)(msb >> 32), (int)msb, (int)(lsb >> 32), (int)lsb};
    }

    public static @Nullable UUID parseUUID(String uuidString) {
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
