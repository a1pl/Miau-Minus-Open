package miau.component;

import io.netty.util.concurrent.GenericFutureListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import miau.event.EventTarget;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.util.network.PacketUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C13PacketPlayerAbilities;
import net.minecraft.network.play.client.C15PacketClientSettings;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.client.C18PacketSpectate;
import net.minecraft.network.play.client.C19PacketResourcePackStatus;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.play.server.S20PacketEntityProperties;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.network.play.server.S14PacketEntity.S15PacketEntityRelMove;
import net.minecraft.network.play.server.S14PacketEntity.S16PacketEntityLook;
import net.minecraft.network.play.server.S14PacketEntity.S17PacketEntityLookMove;
import net.minecraft.util.Tuple;

public final class PingSpoofComponent {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public static ConcurrentLinkedQueue<PingSpoofComponent.TimedPacket> packets = new ConcurrentLinkedQueue<>();
    static TimerUtil enabledTimer = new TimerUtil();
    public static boolean enabled;
    static long amount;
    private static final Map<String, String> sessionOwners = new HashMap<>();
    private static final Map<String, Boolean> activeSessions = new HashMap<>();
    static Tuple<Class[], Boolean> regular = new Tuple(
        new Class[]{C0FPacketConfirmTransaction.class, C00PacketKeepAlive.class, S1CPacketEntityMetadata.class}, false
    );
    static Tuple<Class[], Boolean> velocity = new Tuple(
        new Class[]{S12PacketEntityVelocity.class, S27PacketExplosion.class}, false
    );
    static Tuple<Class[], Boolean> teleports = new Tuple(
        new Class[]{S08PacketPlayerPosLook.class, S39PacketPlayerAbilities.class, S09PacketHeldItemChange.class}, false
    );
    static Tuple<Class[], Boolean> players = new Tuple(
        new Class[]{
            S13PacketDestroyEntities.class,
            S14PacketEntity.class,
            S16PacketEntityLook.class,
            S15PacketEntityRelMove.class,
            S17PacketEntityLookMove.class,
            S18PacketEntityTeleport.class,
            S20PacketEntityProperties.class,
            S19PacketEntityHeadLook.class
        },
        false
    );
    static Tuple<Class[], Boolean> blink = new Tuple(
        new Class[]{
            C02PacketUseEntity.class,
            C0DPacketCloseWindow.class,
            C0EPacketClickWindow.class,
            C0CPacketInput.class,
            C0BPacketEntityAction.class,
            C08PacketPlayerBlockPlacement.class,
            C07PacketPlayerDigging.class,
            C09PacketHeldItemChange.class,
            C13PacketPlayerAbilities.class,
            C15PacketClientSettings.class,
            C16PacketClientStatus.class,
            C17PacketCustomPayload.class,
            C18PacketSpectate.class,
            C19PacketResourcePackStatus.class,
            C03PacketPlayer.class,
            C04PacketPlayerPosition.class,
            C05PacketPlayerLook.class,
            C06PacketPlayerPosLook.class,
            C0APacketAnimation.class
        },
        false
    );
    static Tuple<Class[], Boolean> movement = new Tuple(
        new Class[]{
            C03PacketPlayer.class,
            C04PacketPlayerPosition.class,
            C05PacketPlayerLook.class,
            C06PacketPlayerPosLook.class
        },
        false
    );
    static Tuple<Class[], Boolean>[] types = new Tuple[]{regular, velocity, teleports, players, blink, movement};

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!event.isCancelled()
            && enabled
            && Arrays.stream(types)
                .anyMatch(
                    tuple -> (Boolean)tuple.func_76340_b()
                        && Arrays.<Class>stream((Class[])tuple.func_76341_a())
                            .anyMatch(clazz -> clazz == event.getPacket().getClass())
                )) {
            event.setCancelled(true);
            packets.add(new PingSpoofComponent.TimedPacket(event.getPacket()));
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        dispatch();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.POST) {
            if (!(enabled = !enabledTimer.hasTimeElapsed(100L) && !(mc.field_71462_r instanceof GuiDownloadTerrain))) {
                dispatch();
            } else {
                enabled = false;
                Iterator<PingSpoofComponent.TimedPacket> iterator = packets.iterator();

                while (iterator.hasNext()) {
                    PingSpoofComponent.TimedPacket timedPacket = iterator.next();
                    if (timedPacket.getTime() + amount < System.currentTimeMillis()) {
                        queuePacket(timedPacket.getPacket());
                        iterator.remove();
                    }
                }

                enabled = true;
            }
        }
    }

    public static void dispatch() {
        if (!packets.isEmpty()) {
            boolean wasEnabled = enabled;
            enabled = false;

            for (PingSpoofComponent.TimedPacket timedPacket : packets) {
                queuePacket(timedPacket.getPacket());
            }

            enabled = wasEnabled;
            packets.clear();
        }
    }

    private static void queuePacket(Packet<?> packet) {
        String className = packet.getClass().getName();
        if (className.startsWith("net.minecraft.network.play.server")) {
            Packet<INetHandlerPlayClient> serverPacket = (Packet<INetHandlerPlayClient>)packet;
            PacketUtil.handlePacket(serverPacket);
        } else if (mc.func_147114_u() != null && mc.func_147114_u().func_147298_b() != null) {
            mc.func_147114_u().func_147298_b().func_179288_a(packet, null, new GenericFutureListener[0]);
        }
    }

    public static void disable() {
        enabled = false;
        enabledTimer.setTime(System.currentTimeMillis() - 999999999L);
    }

    public static void spoof(int amount, boolean regular, boolean velocity, boolean teleports, boolean players) {
        spoof(amount, regular, velocity, teleports, players, false);
    }

    public static void spoof(
        int amount, boolean regular, boolean velocity, boolean teleports, boolean players, boolean blink
    ) {
        spoof(amount, regular, velocity, teleports, players, blink, false);
    }

    public static void spoof(
        int amount,
        boolean regular,
        boolean velocity,
        boolean teleports,
        boolean players,
        boolean blink,
        boolean movement
    ) {
        enabledTimer.reset();
        PingSpoofComponent.regular = new Tuple((Class[])PingSpoofComponent.regular.func_76341_a(), regular);
        PingSpoofComponent.velocity = new Tuple((Class[])PingSpoofComponent.velocity.func_76341_a(), velocity);
        PingSpoofComponent.teleports = new Tuple((Class[])PingSpoofComponent.teleports.func_76341_a(), teleports);
        PingSpoofComponent.players = new Tuple((Class[])PingSpoofComponent.players.func_76341_a(), players);
        PingSpoofComponent.blink = new Tuple((Class[])PingSpoofComponent.blink.func_76341_a(), blink);
        PingSpoofComponent.movement = new Tuple((Class[])PingSpoofComponent.movement.func_76341_a(), movement);
        PingSpoofComponent.amount = amount;
    }

    public static void blink() {
        spoof(9999999, true, false, false, false, true);
    }

    public static void registerSessionOwner(String sessionId, String owner) {
        sessionOwners.put(sessionId, owner);
    }

    public static String getSessionOwner(String sessionId) {
        return sessionOwners.get(sessionId);
    }

    public static void clearSessionOwners() {
        sessionOwners.clear();
    }

    public static boolean isOwnedBy(String sessionId) {
        return activeSessions.containsKey(sessionId) && activeSessions.get(sessionId);
    }

    public static void beginSession(
        String sessionId,
        int amount,
        boolean regularPackets,
        boolean velocityPackets,
        boolean teleportPackets,
        boolean playerPackets,
        boolean blinkPackets,
        boolean movementPackets
    ) {
        activeSessions.put(sessionId, true);
        spoof(amount, regularPackets, velocityPackets, teleportPackets, playerPackets, blinkPackets, movementPackets);
    }

    public static void finishSession(String sessionId, boolean dispatchImmediately) {
        activeSessions.put(sessionId, false);
        if (dispatchImmediately) {
            dispatch();
        }

        disable();
    }

    public static class TimedPacket {
        private final Packet<?> packet;
        private final long time;

        public TimedPacket(Packet<?> packet) {
            this.packet = packet;
            this.time = System.currentTimeMillis();
        }

        public Packet<?> getPacket() {
            return this.packet;
        }

        public long getTime() {
            return this.time;
        }
    }
}
