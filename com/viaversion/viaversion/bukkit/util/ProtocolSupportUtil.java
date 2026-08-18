package com.viaversion.viaversion.bukkit.util;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import org.bukkit.entity.Player;

public final class ProtocolSupportUtil {
    private static final Method PROTOCOL_VERSION_METHOD;
    private static final Method GET_ID_METHOD;

    public static ProtocolVersion getProtocolVersion(Player player) {
        if (PROTOCOL_VERSION_METHOD == null) {
            return ProtocolVersion.unknown;
        }

        try {
            Object version = PROTOCOL_VERSION_METHOD.invoke(null, player);
            int id = (Integer)GET_ID_METHOD.invoke(version);
            boolean preNetty = id == 78 || id == 74 || id == 73 || id == 61 || id == 60 || id == 51;
            return ProtocolVersion.getProtocol(preNetty ? VersionType.RELEASE_INITIAL : VersionType.RELEASE, id);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Via.getPlatform().getLogger().log(Level.SEVERE, "Failed to get ProtocolSupport version", e);
            return ProtocolVersion.unknown;
        }
    }

    static {
        Method protocolVersionMethod = null;
        Method getIdMethod = null;

        try {
            protocolVersionMethod = Class.forName("protocolsupport.api.ProtocolSupportAPI")
                .getMethod("getProtocolVersion", Player.class);
            getIdMethod = Class.forName("protocolsupport.api.ProtocolVersion").getMethod("getId");
        } catch (ReflectiveOperationException var3) {
        }

        PROTOCOL_VERSION_METHOD = protocolVersionMethod;
        GET_ID_METHOD = getIdMethod;
    }
}
