package com.viaversion.viaversion.api.protocol.remapper;

import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;

@FunctionalInterface
public interface PacketHandler {
    void handle(PacketWrapper var1) throws Exception;

    default PacketHandler then(PacketHandler handler) {
        return wrapper -> {
            this.handle(wrapper);
            handler.handle(wrapper);
        };
    }
}
