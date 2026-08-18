package com.viaversion.viaversion.api.protocol.packet.provider;

import com.google.common.base.Preconditions;
import com.viaversion.viaversion.api.protocol.packet.PacketType;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface PacketTypeMap<P extends PacketType> {
    @Nullable P typeByName(String var1);

    @Nullable P typeById(int var1);

    Collection<P> types();

    static <T extends PacketType, E extends T> PacketTypeMap<T> of(Class<E> enumClass) {
        T[] types = (T[])enumClass.getEnumConstants();
        Preconditions.checkArgument(types != null, "%s is not an enum", new Object[]{enumClass});
        Map<String, T> byName = new HashMap<>(types.length);

        for (T type : types) {
            byName.put(type.getName(), type);
        }

        return of(byName, types);
    }

    static <T extends PacketType> PacketTypeMap<T> of(Map<String, T> packetsByName, Int2ObjectMap<T> packetsById) {
        return new PacketTypeMapMap<>(packetsByName, packetsById);
    }

    static <T extends PacketType> PacketTypeMap<T> of(Map<String, T> packetsByName, T[] packets) {
        return new PacketTypeArrayMap<>(packetsByName, packets);
    }
}
