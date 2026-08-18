package com.viaversion.viaversion.bungee.providers;

import com.google.common.collect.Lists;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.protocols.base.BaseVersionProvider;
import com.viaversion.viaversion.util.ReflectionUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.protocol.ProtocolConstants;

public class BungeeVersionProvider extends BaseVersionProvider {
    @Override
    public ProtocolVersion getClosestServerProtocol(UserConnection user) throws Exception {
        List<Integer> list = ReflectionUtil.getStatic(ProtocolConstants.class, "SUPPORTED_VERSION_IDS", List.class);
        List<Integer> sorted = new ArrayList<>(list);
        Collections.sort(sorted);
        ProtocolInfo info = user.getProtocolInfo();
        ProtocolVersion clientProtocolVersion = info.protocolVersion();
        if (new HashSet<>(sorted).contains(clientProtocolVersion.getVersion())) {
            return clientProtocolVersion;
        }

        if (clientProtocolVersion.getVersion() < sorted.get(0)) {
            return getLowestSupportedVersion();
        }

        for (Integer protocol : Lists.reverse(sorted)) {
            if (clientProtocolVersion.getVersion() > protocol && ProtocolVersion.isRegistered(protocol)) {
                return ProtocolVersion.getProtocol(protocol);
            }
        }

        Via.getPlatform().getLogger().severe("Panic, no protocol id found for " + clientProtocolVersion);
        return clientProtocolVersion;
    }

    public static ProtocolVersion getLowestSupportedVersion() {
        try {
            List<Integer> list = ReflectionUtil.getStatic(ProtocolConstants.class, "SUPPORTED_VERSION_IDS", List.class);
            return ProtocolVersion.getProtocol(list.get(0));
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return ProtocolVersion.getProtocol(ProxyServer.getInstance().getProtocolVersion());
        }
    }
}
