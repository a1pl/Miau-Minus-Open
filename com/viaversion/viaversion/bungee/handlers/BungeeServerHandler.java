package com.viaversion.viaversion.bungee.handlers;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.ClientEntityIdChangeListener;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.ProtocolPipeline;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.bungee.storage.BungeeStorage;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.packets.InventoryPackets;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.Protocol1_9To1_8;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.EntityIdProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.storage.EntityTracker1_9;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.score.Team;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.packet.PluginMessage;

public class BungeeServerHandler implements Listener {
    private static final Method getHandshake;
    private static final Method getRegisteredChannels;
    private static final Method getBrandMessage;
    private static final Method setProtocol;
    private static final Method getEntityMap;
    private static final Method setVersion;
    private static final Field entityRewrite;
    private static final Field channelWrapper;

    @EventHandler(priority = 120)
    public void onServerConnect(ServerConnectEvent event) {
        if (!event.isCancelled()) {
            UserConnection user = Via.getManager()
                .getConnectionManager()
                .getConnectedClient(event.getPlayer().getUniqueId());
            if (user != null) {
                if (!user.has(BungeeStorage.class)) {
                    user.put(new BungeeStorage(event.getPlayer()));
                }

                ProtocolVersion serverProtocolVersion = Via.proxyPlatform()
                    .protocolDetectorService()
                    .serverProtocolVersion(event.getTarget().getName());
                ProtocolVersion clientProtocolVersion = user.getProtocolInfo().protocolVersion();
                List<ProtocolPathEntry> protocols = Via.getManager()
                    .getProtocolManager()
                    .getProtocolPath(clientProtocolVersion, serverProtocolVersion);

                try {
                    Object handshake = getHandshake.invoke(event.getPlayer().getPendingConnection());
                    setProtocol.invoke(
                        handshake,
                        protocols == null ? clientProtocolVersion.getVersion() : serverProtocolVersion.getVersion()
                    );
                } catch (InvocationTargetException | IllegalAccessException e) {
                    Via.getPlatform().getLogger().log(Level.SEVERE, "Error setting handshake version", e);
                }
            }
        }
    }

    @EventHandler(priority = -120)
    public void onServerConnected(ServerConnectedEvent event) {
        try {
            this.checkServerChange(
                event, Via.getManager().getConnectionManager().getConnectedClient(event.getPlayer().getUniqueId())
            );
        } catch (Exception e) {
            Via.getPlatform().getLogger().log(Level.SEVERE, "Failed to handle server switch", e);
        }
    }

    @EventHandler(priority = -120)
    public void onServerSwitch(ServerSwitchEvent event) {
        UserConnection userConnection = Via.getManager()
            .getConnectionManager()
            .getConnectedClient(event.getPlayer().getUniqueId());
        if (userConnection != null) {
            int playerId;
            try {
                playerId = Via.getManager().getProviders().get(EntityIdProvider.class).getEntityId(userConnection);
            } catch (Exception ignored) {
                return;
            }

            for (EntityTracker tracker : userConnection.getEntityTrackers()) {
                tracker.setClientEntityId(playerId);
            }

            for (StorableObject object : userConnection.getStoredObjects().values()) {
                if (object instanceof ClientEntityIdChangeListener) {
                    ((ClientEntityIdChangeListener)object).setClientEntityId(playerId);
                }
            }
        }
    }

    public void checkServerChange(ServerConnectedEvent event, UserConnection user) throws Exception {
        if (user != null) {
            BungeeStorage storage = user.get(BungeeStorage.class);
            if (storage != null) {
                Server server = event.getServer();
                if (server != null && !server.getInfo().getName().equals(storage.getCurrentServer())) {
                    EntityTracker1_9 oldEntityTracker = user.getEntityTracker(Protocol1_9To1_8.class);
                    if (oldEntityTracker != null && oldEntityTracker.isAutoTeam() && oldEntityTracker.isTeamExists()) {
                        oldEntityTracker.sendTeamPacket(false, true);
                    }

                    String serverName = server.getInfo().getName();
                    storage.setCurrentServer(serverName);
                    ProtocolVersion serverProtocolVersion = Via.proxyPlatform()
                        .protocolDetectorService()
                        .serverProtocolVersion(serverName);
                    if (serverProtocolVersion.olderThanOrEqualTo(ProtocolVersion.v1_8) && storage.getBossbar() != null) {
                        if (user.getProtocolInfo().getPipeline().contains(Protocol1_9To1_8.class)) {
                            for (UUID uuid : storage.getBossbar()) {
                                PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_9.BOSSBAR, null, user);
                                wrapper.write(Type.UUID, uuid);
                                wrapper.write(Type.VAR_INT, 1);
                                wrapper.send(Protocol1_9To1_8.class);
                            }
                        }

                        storage.getBossbar().clear();
                    }

                    ProtocolInfo info = user.getProtocolInfo();
                    ProtocolVersion previousServerProtocol = info.serverProtocolVersion();
                    List<ProtocolPathEntry> protocolPath = Via.getManager()
                        .getProtocolManager()
                        .getProtocolPath(info.protocolVersion(), serverProtocolVersion);
                    ProtocolPipeline pipeline = user.getProtocolInfo().getPipeline();
                    user.clearStoredObjects(true);
                    pipeline.cleanPipes();
                    if (protocolPath != null) {
                        info.setServerProtocolVersion(serverProtocolVersion);
                        pipeline.add(
                            protocolPath.stream().map(ProtocolPathEntry::protocol).collect(Collectors.toList())
                        );
                    } else {
                        serverProtocolVersion = info.protocolVersion();
                        info.setServerProtocolVersion(serverProtocolVersion);
                    }

                    pipeline.add(Via.getManager().getProtocolManager().getBaseProtocol(serverProtocolVersion));
                    boolean toNewId = previousServerProtocol.olderThan(ProtocolVersion.v1_13)
                        && serverProtocolVersion.newerThanOrEqualTo(ProtocolVersion.v1_13);
                    boolean toOldId = previousServerProtocol.newerThanOrEqualTo(ProtocolVersion.v1_13)
                        && serverProtocolVersion.olderThan(ProtocolVersion.v1_13);
                    if (previousServerProtocol.isKnown() && (toNewId || toOldId)) {
                        Collection<String> registeredChannels = (Collection<String>)getRegisteredChannels.invoke(
                            event.getPlayer().getPendingConnection()
                        );
                        if (!registeredChannels.isEmpty()) {
                            Collection<String> newChannels = new HashSet<>();
                            Iterator<String> iterator = registeredChannels.iterator();

                            while (iterator.hasNext()) {
                                String channel = iterator.next();
                                String oldChannel = channel;
                                if (toNewId) {
                                    channel = InventoryPackets.getNewPluginChannelId(channel);
                                } else {
                                    channel = InventoryPackets.getOldPluginChannelId(channel);
                                }

                                if (channel == null) {
                                    iterator.remove();
                                } else if (!oldChannel.equals(channel)) {
                                    iterator.remove();
                                    newChannels.add(channel);
                                }
                            }

                            registeredChannels.addAll(newChannels);
                        }

                        PluginMessage brandMessage = (PluginMessage)getBrandMessage.invoke(
                            event.getPlayer().getPendingConnection()
                        );
                        if (brandMessage != null) {
                            String channel = brandMessage.getTag();
                            if (toNewId) {
                                channel = InventoryPackets.getNewPluginChannelId(channel);
                            } else {
                                channel = InventoryPackets.getOldPluginChannelId(channel);
                            }

                            if (channel != null) {
                                brandMessage.setTag(channel);
                            }
                        }
                    }

                    user.put(storage);
                    user.setActive(protocolPath != null);
                    ProxiedPlayer player = storage.getPlayer();
                    EntityTracker1_9 newTracker = user.getEntityTracker(Protocol1_9To1_8.class);
                    if (newTracker != null && Via.getConfig().isAutoTeam()) {
                        String currentTeam = null;

                        for (Team team : player.getScoreboard().getTeams()) {
                            if (team.getPlayers().contains(info.getUsername())) {
                                currentTeam = team.getName();
                            }
                        }

                        newTracker.setAutoTeam(true);
                        if (currentTeam == null) {
                            newTracker.sendTeamPacket(true, true);
                            newTracker.setCurrentTeam("viaversion");
                        } else {
                            newTracker.setAutoTeam(Via.getConfig().isAutoTeam());
                            newTracker.setCurrentTeam(currentTeam);
                        }
                    }

                    Object wrapper = channelWrapper.get(player);
                    setVersion.invoke(wrapper, serverProtocolVersion.getVersion());
                    Object entityMap = getEntityMap.invoke(null, serverProtocolVersion.getVersion());
                    entityRewrite.set(player, entityMap);
                }
            }
        }
    }

    static {
        try {
            getHandshake = Class.forName("net.md_5.bungee.connection.InitialHandler").getDeclaredMethod("getHandshake");
            getRegisteredChannels = Class.forName("net.md_5.bungee.connection.InitialHandler")
                .getDeclaredMethod("getRegisteredChannels");
            getBrandMessage = Class.forName("net.md_5.bungee.connection.InitialHandler")
                .getDeclaredMethod("getBrandMessage");
            setProtocol = Class.forName("net.md_5.bungee.protocol.packet.Handshake")
                .getDeclaredMethod("setProtocolVersion", int.class);
            getEntityMap = Class.forName("net.md_5.bungee.entitymap.EntityMap")
                .getDeclaredMethod("getEntityMap", int.class);
            setVersion = Class.forName("net.md_5.bungee.netty.ChannelWrapper")
                .getDeclaredMethod("setVersion", int.class);
            channelWrapper = Class.forName("net.md_5.bungee.UserConnection").getDeclaredField("ch");
            channelWrapper.setAccessible(true);
            entityRewrite = Class.forName("net.md_5.bungee.UserConnection").getDeclaredField("entityRewrite");
            entityRewrite.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            Via.getPlatform()
                .getLogger()
                .severe("Error initializing BungeeServerHandler, try updating BungeeCord or ViaVersion!");
            throw new RuntimeException(e);
        }
    }
}
