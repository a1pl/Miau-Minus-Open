package com.viaversion.viaversion.velocity.platform;

import com.velocitypowered.api.proxy.Player;
import com.viaversion.viaversion.ViaAPIBase;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.buffer.ByteBuf;

public class VelocityViaAPI extends ViaAPIBase<Player> {
    public ProtocolVersion getPlayerProtocolVersion(Player player) {
        return this.getPlayerProtocolVersion(player.getUniqueId());
    }

    public void sendRawPacket(Player player, ByteBuf packet) throws IllegalArgumentException {
        this.sendRawPacket(player.getUniqueId(), packet);
    }
}
