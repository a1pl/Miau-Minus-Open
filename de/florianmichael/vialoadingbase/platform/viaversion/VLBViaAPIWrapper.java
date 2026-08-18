package de.florianmichael.vialoadingbase.platform.viaversion;

import com.viaversion.viaversion.ViaAPIBase;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.buffer.ByteBuf;

public class VLBViaAPIWrapper extends ViaAPIBase<UserConnection> {
    public ProtocolVersion getPlayerProtocolVersion(UserConnection player) {
        return player.getProtocolInfo().protocolVersion();
    }

    public void sendRawPacket(UserConnection player, ByteBuf packet) {
        player.scheduleSendRawPacket(packet);
    }
}
