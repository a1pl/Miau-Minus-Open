package com.viaversion.viaversion.api.debug;

import com.google.common.annotations.Beta;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.packet.Direction;
import com.viaversion.viaversion.api.protocol.packet.PacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import java.util.logging.Level;

@Beta
public interface DebugHandler {
    boolean enabled();

    void setEnabled(boolean var1);

    void addPacketTypeNameToLog(String var1);

    void addPacketTypeToLog(PacketType var1);

    boolean removePacketTypeNameToLog(String var1);

    void clearPacketTypesToLog();

    boolean logPostPacketTransform();

    void setLogPostPacketTransform(boolean var1);

    boolean shouldLog(PacketWrapper var1, Direction var2);

    default void enableAndLogIds(PacketType... packetTypes) {
        this.setEnabled(true);

        for (PacketType packetType : packetTypes) {
            this.addPacketTypeToLog(packetType);
        }
    }

    default void error(String error, Throwable t) {
        if (!Via.getConfig().isSuppressConversionWarnings() || this.enabled()) {
            Via.getPlatform().getLogger().log(Level.SEVERE, error, t);
        }
    }
}
