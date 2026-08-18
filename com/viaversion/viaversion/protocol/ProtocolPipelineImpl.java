package com.viaversion.viaversion.protocol;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.debug.DebugHandler;
import com.viaversion.viaversion.api.protocol.AbstractSimpleProtocol;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.ProtocolPipeline;
import com.viaversion.viaversion.api.protocol.packet.Direction;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.util.ProtocolUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ProtocolPipelineImpl extends AbstractSimpleProtocol implements ProtocolPipeline {
    private final List<Protocol> protocolList = new ArrayList<>();
    private final Set<Class<? extends Protocol>> protocolSet = new HashSet<>();
    private final UserConnection userConnection;
    private List<Protocol> reversedProtocolList = new ArrayList<>();
    private int baseProtocols;

    public ProtocolPipelineImpl(UserConnection userConnection) {
        this.userConnection = userConnection;
        userConnection.getProtocolInfo().setPipeline(this);
        this.registerPackets();
    }

    @Override
    protected void registerPackets() {
        this.add(Via.getManager().getProtocolManager().getBaseProtocol());
    }

    @Override
    public void init(UserConnection userConnection) {
        throw new UnsupportedOperationException("ProtocolPipeline can only be initialized once");
    }

    @Override
    public void add(Protocol protocol) {
        this.reversedProtocolList.add(this.baseProtocols, protocol);
        if (protocol.isBaseProtocol()) {
            this.protocolList.add(this.baseProtocols, protocol);
            this.baseProtocols++;
        } else {
            this.protocolList.add(protocol);
        }

        this.protocolSet.add((Class<? extends Protocol>)protocol.getClass());
        protocol.init(this.userConnection);
    }

    @Override
    public void add(Collection<Protocol> protocols) {
        for (Protocol protocol : protocols) {
            if (protocol.isBaseProtocol()) {
                throw new UnsupportedOperationException("Base protocols cannot be added in bulk");
            }

            protocol.init(this.userConnection);
            this.protocolSet.add((Class<? extends Protocol>)protocol.getClass());
        }

        this.protocolList.addAll(protocols);
        this.refreshReversedList();
    }

    private void refreshReversedList() {
        List<Protocol> reversedProtocols = new ArrayList<>(this.protocolList.size());

        for (int i = 0; i < this.baseProtocols; i++) {
            reversedProtocols.add(this.protocolList.get(i));
        }

        for (int i = this.protocolList.size() - 1; i >= this.baseProtocols; i--) {
            reversedProtocols.add(this.protocolList.get(i));
        }

        this.reversedProtocolList = reversedProtocols;
    }

    @Override
    public void transform(Direction direction, State state, PacketWrapper packetWrapper) throws Exception {
        int originalID = packetWrapper.getId();
        DebugHandler debugHandler = Via.getManager().debugHandler();
        boolean debug = debugHandler.enabled();
        if (debug && !debugHandler.logPostPacketTransform() && debugHandler.shouldLog(packetWrapper, direction)) {
            this.logPacket(direction, state, packetWrapper, originalID);
        }

        packetWrapper.apply(direction, state, this.protocolListFor(direction));
        super.transform(direction, state, packetWrapper);
        if (debug && debugHandler.logPostPacketTransform() && debugHandler.shouldLog(packetWrapper, direction)) {
            this.logPacket(direction, state, packetWrapper, originalID);
        }
    }

    private List<Protocol> protocolListFor(Direction direction) {
        return direction == Direction.SERVERBOUND ? this.protocolList : this.reversedProtocolList;
    }

    private void logPacket(Direction direction, State state, PacketWrapper packetWrapper, int originalID) {
        ProtocolInfo protocolInfo = this.userConnection.getProtocolInfo();
        String actualUsername = protocolInfo.getUsername();
        String username = actualUsername != null ? actualUsername + " " : "";
        Via.getPlatform()
            .getLogger()
            .log(
                Level.INFO,
                "{0}{1} {2}: {3} ({4}) -> {5} ({6}) [{7}] {8}",
                new Object[]{
                    username,
                    direction,
                    state,
                    originalID,
                    ProtocolUtil.toNiceHex(originalID),
                    packetWrapper.getId(),
                    ProtocolUtil.toNiceHex(packetWrapper.getId()),
                    protocolInfo.protocolVersion().getName(),
                    packetWrapper
                }
            );
    }

    @Override
    public boolean contains(Class<? extends Protocol> protocolClass) {
        return this.protocolSet.contains(protocolClass);
    }

    @Override
    public <P extends Protocol> @Nullable P getProtocol(Class<P> pipeClass) {
        for (Protocol protocol : this.protocolList) {
            if (protocol.getClass() == pipeClass) {
                return (P)protocol;
            }
        }

        return null;
    }

    @Override
    public List<Protocol> pipes(
        @Nullable Class<? extends Protocol> protocolClass, boolean skipCurrentPipeline, Direction direction
    ) {
        List<Protocol> protocolList = this.protocolListFor(direction);
        int index = this.indexOf(protocolClass, skipCurrentPipeline, protocolList);
        List<Protocol> pipes = new ArrayList<>(this.baseProtocols + protocolList.size() - index);
        int i = 0;

        for (int size = Math.min(index, this.baseProtocols); i < size; i++) {
            pipes.add(protocolList.get(i));
        }

        i = index;

        for (int size = protocolList.size(); i < size; i++) {
            pipes.add(protocolList.get(i));
        }

        return pipes;
    }

    private int indexOf(
        @Nullable Class<? extends Protocol> protocolClass, boolean skipCurrentPipeline, List<Protocol> protocolList
    ) {
        if (protocolClass == null) {
            return 0;
        }

        int index = -1;

        for (int i = 0; i < protocolList.size(); i++) {
            if (protocolList.get(i).getClass() == protocolClass) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            throw new NoSuchElementException(protocolClass.getCanonicalName());
        }

        if (skipCurrentPipeline) {
            index = Math.min(index + 1, protocolList.size());
        }

        return index;
    }

    @Override
    public List<Protocol> pipes() {
        return Collections.unmodifiableList(this.protocolList);
    }

    @Override
    public List<Protocol> reversedPipes() {
        return Collections.unmodifiableList(this.reversedProtocolList);
    }

    @Override
    public int baseProtocolCount() {
        return this.baseProtocols;
    }

    @Override
    public boolean hasNonBaseProtocols() {
        return this.protocolList.size() > this.baseProtocols;
    }

    @Override
    public void cleanPipes() {
        this.protocolList.clear();
        this.reversedProtocolList.clear();
        this.protocolSet.clear();
        this.baseProtocols = 0;
        this.registerPackets();
    }

    @Override
    public String toString() {
        return "ProtocolPipelineImpl{protocolList=" + this.protocolList + '}';
    }
}
