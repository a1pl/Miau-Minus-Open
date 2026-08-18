package com.viaversion.viaversion.util;

import com.google.common.base.Preconditions;
import com.viaversion.viaversion.api.protocol.packet.PacketType;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypeMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ProtocolUtil {
    @SafeVarargs
    public static <P extends PacketType> Map<State, PacketTypeMap<P>> packetTypeMap(
        @Nullable Class<P> parent, Class<? extends P>... packetTypeClasses
    ) {
        if (parent == null) {
            return Collections.emptyMap();
        }

        Map<State, PacketTypeMap<P>> map = new EnumMap<>(State.class);

        for (Class<? extends P> packetTypeClass : packetTypeClasses) {
            P[] types = (P[])packetTypeClass.getEnumConstants();
            Preconditions.checkArgument(types != null, "%s not an enum", new Object[]{packetTypeClass});
            Preconditions.checkArgument(types.length > 0, "Enum %s has no types", new Object[]{packetTypeClass});
            State state = types[0].state();
            map.put(state, PacketTypeMap.of(packetTypeClass));
        }

        return map;
    }

    public static String toNiceHex(int id) {
        String hex = Integer.toHexString(id).toUpperCase();
        return (hex.length() == 1 ? "0x0" : "0x") + hex;
    }
}
