package com.viaversion.viaversion.sponge.platform;

import com.viaversion.viaversion.ViaAPIBase;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import org.spongepowered.api.entity.living.player.Player;

public class SpongeViaAPI extends ViaAPIBase<Player> {
    public ProtocolVersion getPlayerProtocolVersion(Player player) {
        return this.getPlayerProtocolVersion(player.uniqueId());
    }

    public void sendRawPacket(Player player, ByteBuf packet) throws IllegalArgumentException {
        this.sendRawPacket(player.uniqueId(), packet);
    }
}
