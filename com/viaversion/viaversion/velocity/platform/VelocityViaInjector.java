package com.viaversion.viaversion.velocity.platform;

import com.viaversion.viaversion.VelocityPlugin;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.platform.ViaInjector;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.fastutil.objects.ObjectLinkedOpenHashSet;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.util.ReflectionUtil;
import com.viaversion.viaversion.velocity.handlers.VelocityChannelInitializer;
import io.netty.channel.ChannelInitializer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.SortedSet;
import java.util.logging.Level;
import org.jetbrains.annotations.Nullable;

public class VelocityViaInjector implements ViaInjector {
    public static final Method GET_PLAYER_INFO_FORWARDING_MODE = getPlayerInfoForwardingModeMethod();

    @Nullable
    private static Method getPlayerInfoForwardingModeMethod() {
        try {
            return Class.forName("com.velocitypowered.proxy.config.VelocityConfiguration")
                .getMethod("getPlayerInfoForwardingMode");
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            Via.getPlatform()
                .getLogger()
                .log(
                    Level.SEVERE,
                    "Failed to get getPlayerInfoForwardingMode method from Velocity, please report this issue on our GitHub.",
                    e
                );
            return null;
        }
    }

    private ChannelInitializer getInitializer() throws Exception {
        Object connectionManager = ReflectionUtil.get(VelocityPlugin.PROXY, "cm", Object.class);
        Object channelInitializerHolder = ReflectionUtil.invoke(connectionManager, "getServerChannelInitializer");
        return (ChannelInitializer)ReflectionUtil.invoke(channelInitializerHolder, "get");
    }

    private ChannelInitializer getBackendInitializer() throws Exception {
        Object connectionManager = ReflectionUtil.get(VelocityPlugin.PROXY, "cm", Object.class);
        Object channelInitializerHolder = ReflectionUtil.invoke(connectionManager, "getBackendChannelInitializer");
        return (ChannelInitializer)ReflectionUtil.invoke(channelInitializerHolder, "get");
    }

    @Override
    public void inject() throws Exception {
        Via.getPlatform()
            .getLogger()
            .info("Replacing channel initializers; you can safely ignore the following two warnings.");
        Object connectionManager = ReflectionUtil.get(VelocityPlugin.PROXY, "cm", Object.class);
        Object channelInitializerHolder = ReflectionUtil.invoke(connectionManager, "getServerChannelInitializer");
        ChannelInitializer originalInitializer = this.getInitializer();
        channelInitializerHolder.getClass()
            .getMethod("set", ChannelInitializer.class)
            .invoke(channelInitializerHolder, new VelocityChannelInitializer(originalInitializer, false));
        Object backendInitializerHolder = ReflectionUtil.invoke(connectionManager, "getBackendChannelInitializer");
        ChannelInitializer backendInitializer = this.getBackendInitializer();
        backendInitializerHolder.getClass()
            .getMethod("set", ChannelInitializer.class)
            .invoke(backendInitializerHolder, new VelocityChannelInitializer(backendInitializer, true));
    }

    @Override
    public void uninject() {
        Via.getPlatform().getLogger().severe("ViaVersion cannot remove itself from Velocity without a reboot!");
    }

    @Override
    public ProtocolVersion getServerProtocolVersion() {
        return ProtocolVersion.getProtocol(getLowestSupportedProtocolVersion());
    }

    @Override
    public SortedSet<ProtocolVersion> getServerProtocolVersions() {
        int lowestSupportedProtocolVersion = getLowestSupportedProtocolVersion();
        SortedSet<ProtocolVersion> set = new ObjectLinkedOpenHashSet<>();

        for (com.velocitypowered.api.network.ProtocolVersion version : com.velocitypowered.api.network.ProtocolVersion.SUPPORTED_VERSIONS) {
            if (version.getProtocol() >= lowestSupportedProtocolVersion) {
                set.add(ProtocolVersion.getProtocol(version.getProtocol()));
            }
        }

        return set;
    }

    public static int getLowestSupportedProtocolVersion() {
        try {
            if (GET_PLAYER_INFO_FORWARDING_MODE != null
                && ((Enum)GET_PLAYER_INFO_FORWARDING_MODE.invoke(VelocityPlugin.PROXY.getConfiguration()))
                    .name()
                    .equals("MODERN")) {
                return ProtocolVersion.v1_13.getVersion();
            }
        } catch (IllegalAccessException | InvocationTargetException var1) {
        }

        return com.velocitypowered.api.network.ProtocolVersion.MINIMUM_VERSION.getProtocol();
    }

    @Override
    public JsonObject getDump() {
        JsonObject data = new JsonObject();

        try {
            data.addProperty("currentInitializer", this.getInitializer().getClass().getName());
        } catch (Exception var3) {
        }

        return data;
    }
}
