package com.viaversion.viaversion.api.protocol.packet.provider;

import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.State;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface PacketTypesProvider<CU extends ClientboundPacketType, CM extends ClientboundPacketType, SM extends ServerboundPacketType, SU extends ServerboundPacketType> {
    Map<State, PacketTypeMap<CU>> unmappedClientboundPacketTypes();

    Map<State, PacketTypeMap<SU>> unmappedServerboundPacketTypes();

    Map<State, PacketTypeMap<CM>> mappedClientboundPacketTypes();

    Map<State, PacketTypeMap<SM>> mappedServerboundPacketTypes();

    default @Nullable CU unmappedClientboundType(State state, String typeName) {
        PacketTypeMap<CU> map = this.unmappedClientboundPacketTypes().get(state);
        return map != null ? map.typeByName(typeName) : null;
    }

    default @Nullable SU unmappedServerboundType(State state, String typeName) {
        PacketTypeMap<SU> map = this.unmappedServerboundPacketTypes().get(state);
        return map != null ? map.typeByName(typeName) : null;
    }

    default @Nullable CU unmappedClientboundType(State state, int packetId) {
        PacketTypeMap<CU> map = this.unmappedClientboundPacketTypes().get(state);
        return map != null ? map.typeById(packetId) : null;
    }

    default @Nullable SU unmappedServerboundType(State state, int packetId) {
        PacketTypeMap<SU> map = this.unmappedServerboundPacketTypes().get(state);
        return map != null ? map.typeById(packetId) : null;
    }
}
