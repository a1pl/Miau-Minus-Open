package com.viaversion.viaversion.protocols.base;

import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.protocols.base.packet.BaseClientboundPacket;

public enum ClientboundLoginPackets implements BaseClientboundPacket {
    LOGIN_DISCONNECT,
    HELLO,
    GAME_PROFILE,
    LOGIN_COMPRESSION,
    CUSTOM_QUERY,
    COOKIE_REQUEST;

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
        return State.LOGIN;
    }
}
