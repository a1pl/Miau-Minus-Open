package com.viaversion.viaversion.protocols.base;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.platform.providers.ViaProviders;
import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.ProtocolManager;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.ProtocolPipeline;
import com.viaversion.viaversion.api.protocol.packet.Direction;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.base.packet.BaseClientboundPacket;
import com.viaversion.viaversion.protocols.base.packet.BasePacketTypesProvider;
import com.viaversion.viaversion.protocols.base.packet.BaseServerboundPacket;
import java.util.ArrayList;
import java.util.List;

public class BaseProtocol
    extends AbstractProtocol<BaseClientboundPacket, BaseClientboundPacket, BaseServerboundPacket, BaseServerboundPacket> {
    private static final int STATUS_INTENT = 1;
    private static final int LOGIN_INTENT = 2;
    private static final int TRANSFER_INTENT = 3;

    public BaseProtocol() {
        super(
            BaseClientboundPacket.class,
            BaseClientboundPacket.class,
            BaseServerboundPacket.class,
            BaseServerboundPacket.class
        );
    }

    @Override
    protected void registerPackets() {
        this.registerServerbound(
            ServerboundHandshakePackets.CLIENT_INTENTION,
            wrapper -> {
                int protocolVersion = wrapper.passthrough(Type.VAR_INT);
                wrapper.passthrough(Type.STRING);
                wrapper.passthrough(Type.UNSIGNED_SHORT);
                int state = wrapper.passthrough(Type.VAR_INT);
                ProtocolInfo info = wrapper.user().getProtocolInfo();
                info.setProtocolVersion(ProtocolVersion.getProtocol(protocolVersion));
                VersionProvider versionProvider = Via.getManager().getProviders().get(VersionProvider.class);
                if (versionProvider == null) {
                    wrapper.user().setActive(false);
                } else {
                    ProtocolVersion serverProtocol = versionProvider.getClosestServerProtocol(wrapper.user());
                    info.setServerProtocolVersion(serverProtocol);
                    List<ProtocolPathEntry> protocolPath = null;
                    ProtocolManager protocolManager = Via.getManager().getProtocolManager();
                    if (info.protocolVersion().newerThanOrEqualTo(serverProtocol)
                        || Via.getPlatform().isOldClientsAllowed()) {
                        protocolPath = protocolManager.getProtocolPath(info.protocolVersion(), serverProtocol);
                    }

                    ProtocolPipeline pipeline = info.getPipeline();
                    if (serverProtocol.getVersionType() != VersionType.SPECIAL) {
                        Protocol baseProtocol = protocolManager.getBaseProtocol(serverProtocol);
                        if (baseProtocol != null) {
                            pipeline.add(baseProtocol);
                        }
                    }

                    if (protocolPath != null) {
                        List<Protocol> protocols = new ArrayList<>(protocolPath.size());

                        for (ProtocolPathEntry entry : protocolPath) {
                            protocols.add(entry.protocol());
                            protocolManager.completeMappingDataLoading(
                                (Class<? extends Protocol>)entry.protocol().getClass()
                            );
                        }

                        pipeline.add(protocols);
                        wrapper.set(Type.VAR_INT, 0, serverProtocol.getOriginalVersion());
                    }

                    if (Via.getManager().isDebug()) {
                        Via.getPlatform()
                            .getLogger()
                            .info(
                                "User connected with protocol: "
                                    + info.protocolVersion()
                                    + " and serverProtocol: "
                                    + info.serverProtocolVersion()
                            );
                        Via.getPlatform().getLogger().info("Protocol pipeline: " + pipeline.pipes());
                    }

                    if (state == 1) {
                        info.setState(State.STATUS);
                    } else if (state == 2) {
                        info.setState(State.LOGIN);
                    } else if (state == 3) {
                        info.setState(State.LOGIN);
                        if (serverProtocol.olderThan(ProtocolVersion.v1_20_5)) {
                            wrapper.set(Type.VAR_INT, 1, 2);
                        }
                    }
                }
            }
        );
    }

    @Override
    public boolean isBaseProtocol() {
        return true;
    }

    @Override
    public void register(ViaProviders providers) {
        providers.register(VersionProvider.class, new BaseVersionProvider());
    }

    @Override
    public void transform(Direction direction, State state, PacketWrapper packetWrapper) throws Exception {
        super.transform(direction, state, packetWrapper);
        if (direction == Direction.SERVERBOUND && state == State.HANDSHAKE && packetWrapper.getId() != 0) {
            packetWrapper.user().setActive(false);
        }
    }

    @Override
    protected PacketTypesProvider<BaseClientboundPacket, BaseClientboundPacket, BaseServerboundPacket, BaseServerboundPacket> createPacketTypesProvider() {
        return BasePacketTypesProvider.INSTANCE;
    }
}
