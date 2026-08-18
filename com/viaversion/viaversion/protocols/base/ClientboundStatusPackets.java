package com.viaversion.viaversion.protocols.base;

import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.protocols.base.packet.BaseClientboundPacket;

public enum ClientboundStatusPackets implements BaseClientboundPacket {
    STATUS_RESPONSE,
    PONG_RESPONSE;

    @Override
    public final int getId() {
        return this.ordinal();
    }

    @Override
    public final String getName() {
        return this.name();
    }

    @Override
    public final State state() {
        return State.STATUS;
    }
}
