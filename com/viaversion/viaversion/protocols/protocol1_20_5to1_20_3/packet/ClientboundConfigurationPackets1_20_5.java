package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet;

import com.viaversion.viaversion.api.protocol.packet.State;

public enum ClientboundConfigurationPackets1_20_5 implements ClientboundPacket1_20_5 {
    COOKIE_REQUEST,
    CUSTOM_PAYLOAD,
    DISCONNECT,
    FINISH_CONFIGURATION,
    KEEP_ALIVE,
    PING,
    RESET_CHAT,
    REGISTRY_DATA,
    RESOURCE_PACK_POP,
    RESOURCE_PACK_PUSH,
    STORE_COOKIE,
    TRANSFER,
    UPDATE_ENABLED_FEATURES,
    UPDATE_TAGS,
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
