package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet;

import com.viaversion.viaversion.api.protocol.packet.State;

public enum ServerboundConfigurationPackets1_20_5 implements ServerboundPacket1_20_5 {
    CLIENT_INFORMATION,
    COOKIE_RESPONSE,
    CUSTOM_PAYLOAD,
    FINISH_CONFIGURATION,
    KEEP_ALIVE,
    PONG,
    RESOURCE_PACK,
    SELECT_KNOWN_PACKS;

    @Override
    public int getId() {
        return this.ordinal();
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public State state() {
        return State.CONFIGURATION;
    }
}
