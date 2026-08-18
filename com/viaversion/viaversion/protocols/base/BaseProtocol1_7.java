package com.viaversion.viaversion.protocols.base;

import com.google.common.base.Joiner;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonParseException;
import com.viaversion.viaversion.protocol.ProtocolManagerImpl;
import com.viaversion.viaversion.protocol.ServerProtocolVersionSingleton;
import com.viaversion.viaversion.protocols.base.packet.BaseClientboundPacket;
import com.viaversion.viaversion.protocols.base.packet.BasePacketTypesProvider;
import com.viaversion.viaversion.protocols.base.packet.BaseServerboundPacket;
import com.viaversion.viaversion.util.ChatColorUtil;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.GsonUtil;
import io.netty.channel.ChannelFuture;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class BaseProtocol1_7
    extends AbstractProtocol<BaseClientboundPacket, BaseClientboundPacket, BaseServerboundPacket, BaseServerboundPacket> {
    public BaseProtocol1_7() {
        super(
            BaseClientboundPacket.class,
            BaseClientboundPacket.class,
            BaseServerboundPacket.class,
            BaseServerboundPacket.class
        );
    }

    @Override
    protected void registerPackets() {
        this.registerClientbound(
            ClientboundStatusPackets.STATUS_RESPONSE,
            new PacketHandlers() {
                @Override
                public void register() {
                    this.map(Type.STRING);
                    this.handler(
                        wrapper -> {
                            ProtocolInfo info = wrapper.user().getProtocolInfo();
                            String originalStatus = wrapper.get(Type.STRING, 0);

                            try {
                                JsonElement json = GsonUtil.getGson().fromJson(originalStatus, JsonElement.class);
                                int protocol = 0;
                                JsonObject version;
                                if (json.isJsonObject()) {
                                    if (json.getAsJsonObject().has("version")) {
                                        version = json.getAsJsonObject().get("version").getAsJsonObject();
                                        if (version.has("protocol")) {
                                            protocol = Long.valueOf(version.get("protocol").getAsLong()).intValue();
                                        }
                                    } else {
                                        json.getAsJsonObject().add("version", version = new JsonObject());
                                    }
                                } else {
                                    json = new JsonObject();
                                    json.getAsJsonObject().add("version", version = new JsonObject());
                                }

                                ProtocolVersion protocolVersion = ProtocolVersion.getProtocol(protocol);
                                if (Via.getConfig().isSendSupportedVersions()) {
                                    version.add(
                                        "supportedVersions",
                                        GsonUtil.getGson().toJsonTree(Via.getAPI().getSupportedVersions())
                                    );
                                }

                                if (!Via.getAPI().getServerVersion().isKnown()) {
                                    ProtocolManagerImpl protocolManager = (ProtocolManagerImpl)Via.getManager()
                                        .getProtocolManager();
                                    protocolManager.setServerProtocol(
                                        new ServerProtocolVersionSingleton(protocolVersion)
                                    );
                                }

                                VersionProvider versionProvider = Via.getManager()
                                    .getProviders()
                                    .get(VersionProvider.class);
                                if (versionProvider == null) {
                                    wrapper.user().setActive(false);
                                    return;
                                }

                                ProtocolVersion closestServerProtocol = versionProvider.getClosestServerProtocol(
                                    wrapper.user()
                                );
                                List<ProtocolPathEntry> protocols = null;
                                if (info.protocolVersion().newerThanOrEqualTo(closestServerProtocol)
                                    || Via.getPlatform().isOldClientsAllowed()) {
                                    protocols = Via.getManager()
                                        .getProtocolManager()
                                        .getProtocolPath(info.protocolVersion(), closestServerProtocol);
                                }

                                if (protocols != null) {
                                    if (protocolVersion.equalTo(closestServerProtocol)
                                        || protocolVersion.getVersion() == 0) {
                                        version.addProperty("protocol", info.protocolVersion().getOriginalVersion());
                                    }
                                } else {
                                    wrapper.user().setActive(false);
                                }

                                if (Via.getConfig().blockedProtocolVersions().contains(info.protocolVersion())) {
                                    version.addProperty("protocol", -1);
                                }

                                wrapper.set(Type.STRING, 0, GsonUtil.getGson().toJson(json));
                            } catch (JsonParseException e) {
                                Via.getPlatform().getLogger().log(Level.SEVERE, "Error handling StatusResponse", e);
                            }
                        }
                    );
                }
            }
        );
        this.registerClientbound(
            ClientboundLoginPackets.GAME_PROFILE,
            wrapper -> {
                ProtocolInfo info = wrapper.user().getProtocolInfo();
                if (info.protocolVersion().olderThan(ProtocolVersion.v1_20_2)) {
                    info.setState(State.PLAY);
                }

                UUID uuid = this.passthroughLoginUUID(wrapper);
                info.setUuid(uuid);
                String username = wrapper.passthrough(Type.STRING);
                info.setUsername(username);
                Via.getManager().getConnectionManager().onLoginSuccess(wrapper.user());
                if (!info.getPipeline().hasNonBaseProtocols()) {
                    wrapper.user().setActive(false);
                }

                if (Via.getManager().isDebug()) {
                    Via.getPlatform()
                        .getLogger()
                        .log(
                            Level.INFO,
                            "{0} logged in with protocol {1}, Route: {2}",
                            new Object[]{
                                username,
                                info.protocolVersion().getName(),
                                Joiner.on(", ").join(info.getPipeline().pipes(), ", ", new Object[0])
                            }
                        );
                }
            }
        );
        this.registerServerbound(
            ServerboundLoginPackets.HELLO,
            wrapper -> {
                ProtocolVersion protocol = wrapper.user().getProtocolInfo().protocolVersion();
                if (Via.getConfig().blockedProtocolVersions().contains(protocol)) {
                    if (!wrapper.user().getChannel().isOpen()) {
                        return;
                    }

                    if (!wrapper.user().shouldApplyBlockProtocol()) {
                        return;
                    }

                    String disconnectMessage = ChatColorUtil.translateAlternateColorCodes(
                        Via.getConfig().getBlockedDisconnectMsg()
                    );
                    PacketWrapper disconnectPacket = PacketWrapper.create(
                        ClientboundLoginPackets.LOGIN_DISCONNECT, wrapper.user()
                    );
                    wrapper.write(Type.COMPONENT, ComponentUtil.plainToJson(disconnectMessage));
                    wrapper.cancel();
                    ChannelFuture future = disconnectPacket.sendFuture(null);
                    future.addListener(f -> wrapper.user().getChannel().close());
                }
            }
        );
        this.registerServerbound(ServerboundLoginPackets.LOGIN_ACKNOWLEDGED, wrapper -> {
            ProtocolInfo info = wrapper.user().getProtocolInfo();
            info.setState(State.CONFIGURATION);
        });
    }

    @Override
    public boolean isBaseProtocol() {
        return true;
    }

    public static String addDashes(String trimmedUUID) {
        StringBuilder idBuff = new StringBuilder(trimmedUUID);
        idBuff.insert(20, '-');
        idBuff.insert(16, '-');
        idBuff.insert(12, '-');
        idBuff.insert(8, '-');
        return idBuff.toString();
    }

    protected UUID passthroughLoginUUID(PacketWrapper wrapper) throws Exception {
        String uuidString = wrapper.passthrough(Type.STRING);
        if (uuidString.length() == 32) {
            uuidString = addDashes(uuidString);
        }

        return UUID.fromString(uuidString);
    }

    @Override
    protected PacketTypesProvider<BaseClientboundPacket, BaseClientboundPacket, BaseServerboundPacket, BaseServerboundPacket> createPacketTypesProvider() {
        return BasePacketTypesProvider.INSTANCE;
    }
}
