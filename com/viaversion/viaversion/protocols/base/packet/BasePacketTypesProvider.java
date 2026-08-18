package com.viaversion.viaversion.protocols.base.packet;

import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypeMap;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ClientboundStatusPackets;
import com.viaversion.viaversion.protocols.base.ServerboundHandshakePackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundStatusPackets;
import java.util.EnumMap;
import java.util.Map;

public final class BasePacketTypesProvider
    implements PacketTypesProvider<BaseClientboundPacket, BaseClientboundPacket, BaseServerboundPacket, BaseServerboundPacket> {
    public static final PacketTypesProvider<BaseClientboundPacket, BaseClientboundPacket, BaseServerboundPacket, BaseServerboundPacket> INSTANCE = new BasePacketTypesProvider();
    private final Map<State, PacketTypeMap<BaseClientboundPacket>> clientboundPacketTypes = new EnumMap<>(State.class);
    private final Map<State, PacketTypeMap<BaseServerboundPacket>> serverboundPacketTypes = new EnumMap<>(State.class);

    private BasePacketTypesProvider() {
        this.clientboundPacketTypes.put(State.STATUS, PacketTypeMap.of(ClientboundStatusPackets.class));
        this.clientboundPacketTypes.put(State.LOGIN, PacketTypeMap.of(ClientboundLoginPackets.class));
        this.serverboundPacketTypes.put(State.STATUS, PacketTypeMap.of(ServerboundStatusPackets.class));
        this.serverboundPacketTypes.put(State.HANDSHAKE, PacketTypeMap.of(ServerboundHandshakePackets.class));
        this.serverboundPacketTypes.put(State.LOGIN, PacketTypeMap.of(ServerboundLoginPackets.class));
    }

    @Override
    public Map<State, PacketTypeMap<BaseClientboundPacket>> unmappedClientboundPacketTypes() {
        return this.clientboundPacketTypes;
    }

    @Override
    public Map<State, PacketTypeMap<BaseServerboundPacket>> unmappedServerboundPacketTypes() {
        return this.serverboundPacketTypes;
    }

    @Override
    public Map<State, PacketTypeMap<BaseClientboundPacket>> mappedClientboundPacketTypes() {
        return this.unmappedClientboundPacketTypes();
    }

    @Override
    public Map<State, PacketTypeMap<BaseServerboundPacket>> mappedServerboundPacketTypes() {
        return this.unmappedServerboundPacketTypes();
    }
}
