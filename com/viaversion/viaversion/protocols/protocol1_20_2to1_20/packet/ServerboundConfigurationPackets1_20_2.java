package com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet;

import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ServerboundPacket1_20_3;

public enum ServerboundConfigurationPackets1_20_2 implements ServerboundPacket1_20_2, ServerboundPacket1_20_3 {
    CLIENT_INFORMATION,
    CUSTOM_PAYLOAD,
    FINISH_CONFIGURATION,
    KEEP_ALIVE,
    PONG,
    RESOURCE_PACK;

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
