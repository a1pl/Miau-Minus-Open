package miau.module.modules.network;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import miau.Miau;
import miau.event.EventManager;
import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorRenderManager;
import miau.mixin.IAccessorS14PacketEntity;
import miau.module.Module;
import miau.module.modules.combat.KillAura;
import miau.module.modules.misc.AntiBot;
import miau.module.modules.player.Scaffold;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ColorProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.math.RandomUtil;
import miau.util.misc.BackTrackUtil;
import miau.util.misc.ITruePosition;
import miau.util.network.PacketUtil;
import miau.util.player.RotationUtil;
import miau.util.player.TeamUtil;
import miau.util.render.RenderUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.DataWatcher.WatchableObject;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S24PacketBlockAction;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldSettings.GameType;
import org.lwjgl.opengl.GL11;

public class BackTrack extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final String[] NON_DELAYED_SOUND_SUBSTRINGS = new String[]{"game.player.hurt", "game.player.die"};
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"NORMAL", "BUFFER", "MODERN", "CREW"});
    public final IntProperty nextBacktrackDelay = new IntProperty(
        "next-backtrack-delay",
        0,
        0,
        2000,
        () -> this.mode.getValue() == 0 || this.mode.getValue() == 2 || this.mode.getValue() == 3
    );
    public final FloatProperty delayMs = new FloatProperty(
        "delay", 80.0F, 80.0F, 0.0F, 2000.0F, () -> this.mode.getValue() == 0 || this.mode.getValue() == 2
    );
    public final ModeProperty style = new ModeProperty(
        "style", 1, new String[]{"PULSE", "SMOOTH"}, () -> this.mode.getValue() == 0
    );
    public final ModeProperty distanceMode = new ModeProperty(
        "distance-mode", 0, new String[]{"CUSTOM", "SMART"}, () -> this.mode.getValue() == 2
    );
    public final FloatProperty distance = new FloatProperty(
        "distance",
        2.0F,
        3.0F,
        0.0F,
        10.0F,
        () -> this.mode.getValue() == 0 || this.mode.getValue() == 2 && this.distanceMode.getValue() == 0
    );
    public final BooleanProperty smart = new BooleanProperty(
        "smart", true, () -> this.mode.getValue() == 0 || this.mode.getValue() == 2
    );
    public final ModeProperty espMode = new ModeProperty(
        "esp",
        1,
        new String[]{"NONE", "BOX", "MODEL", "WIREFRAME", "OUTLINE", "TRACER", "2DBOX", "HEALTHBAR", "ARROW"},
        () -> this.mode.getValue() == 0 || this.mode.getValue() == 2
    );
    public final FloatProperty wireframeWidth = new FloatProperty(
        "wireframe-width",
        1.0F,
        0.5F,
        5.0F,
        () -> (this.mode.getValue() == 0 || this.mode.getValue() == 2) && this.espMode.getValue() == 3
    );
    public final FloatProperty outlineWidth = new FloatProperty(
        "outline-width", 2.0F, 0.5F, 5.0F, () -> this.espMode.getValue() == 4
    );
    public final FloatProperty tracerWidth = new FloatProperty(
        "tracer-width", 1.5F, 0.5F, 5.0F, () -> this.espMode.getValue() == 5
    );
    public final FloatProperty arrowSize = new FloatProperty(
        "arrow-size", 15.0F, 5.0F, 30.0F, () -> this.espMode.getValue() == 8
    );
    public final BooleanProperty showHealth = new BooleanProperty(
        "show-health", true, () -> this.espMode.getValue() == 6 || this.espMode.getValue() == 7
    );
    public final BooleanProperty showDistance = new BooleanProperty(
        "show-distance", true, () -> this.espMode.getValue() == 6 || this.espMode.getValue() == 8
    );
    public final ColorProperty espColor = new ColorProperty("color", -16711936);
    public final ModeProperty extraMode = new ModeProperty(
        "extra-mode", 0, new String[]{"NOEXTRA", "POLAR", "INTAVE"}, () -> this.mode.getValue() == 2
    );
    public final ModeProperty startMode = new ModeProperty(
        "start-mode", 0, new String[]{"INRANGE", "ONATTACK"}, () -> this.mode.getValue() == 2
    );
    public final ModeProperty modernStyle = new ModeProperty(
        "modern-style",
        1,
        new String[]{"PULSE", "SMOOTH", "INSTANT", "WAVE", "STEP", "BOUNCE"},
        () -> this.mode.getValue() == 2
    );
    public final FloatProperty waveFrequency = new FloatProperty(
        "wave-frequency", 2.0F, 0.5F, 5.0F, () -> this.mode.getValue() == 2 && this.modernStyle.getValue() == 3
    );
    public final IntProperty stepCount = new IntProperty(
        "step-count", 3, 2, 10, () -> this.mode.getValue() == 2 && this.modernStyle.getValue() == 4
    );
    public final FloatProperty bounceIntensity = new FloatProperty(
        "bounce-intensity", 0.3F, 0.1F, 1.0F, () -> this.mode.getValue() == 2 && this.modernStyle.getValue() == 5
    );
    public final IntProperty instantThreshold = new IntProperty(
        "instant-threshold", 50, 0, 200, () -> this.mode.getValue() == 2 && this.modernStyle.getValue() == 2
    );
    public final ModeProperty delayUpdateMode = new ModeProperty(
        "delay-update-mode",
        0,
        new String[]{"NORMAL", "ATTACK", "EVERYTICK", "KILLPLAYER"},
        () -> this.mode.getValue() == 2
    );
    public final IntProperty attackCountToUpdate = new IntProperty(
        "attack-count-to-update", 1, 0, 10, () -> this.mode.getValue() == 2 && this.delayUpdateMode.getValue() == 1
    );
    public final FloatProperty expandRange = new FloatProperty(
        "expand-range", 1.0F, 0.0F, 5.0F, () -> this.mode.getValue() == 2 && this.distanceMode.getValue() == 1
    );
    public final FloatProperty minExpandRange = new FloatProperty(
        "min-expand-range", 0.0F, 0.0F, 5.0F, () -> this.mode.getValue() == 2 && this.distanceMode.getValue() == 1
    );
    public final IntProperty trackingBuffer = new IntProperty(
        "tracking-buffer", 0, 0, 10000, () -> this.mode.getValue() == 2
    );
    public final FloatProperty ownHurtTime = new FloatProperty(
        "own-hurt-time", 0.0F, 10.0F, 0.0F, 10.0F, () -> this.mode.getValue() == 2
    );
    public final FloatProperty enemyHurtTime = new FloatProperty(
        "enemy-hurt-time", 0.0F, 10.0F, 0.0F, 10.0F, () -> this.mode.getValue() == 2
    );
    public final BooleanProperty autoRestart = new BooleanProperty(
        "auto-restart", true, () -> this.mode.getValue() == 2
    );
    public final ModeProperty autoRestartDelayMode = new ModeProperty(
        "auto-restart-delay-mode",
        0,
        new String[]{"FOLLOW", "CUSTOM"},
        () -> this.mode.getValue() == 2 && this.autoRestart.getValue()
    );
    public final IntProperty autoRestartDelay = new IntProperty(
        "auto-restart-delay",
        1000,
        50,
        30000,
        () -> this.mode.getValue() == 2 && this.autoRestart.getValue() && this.autoRestartDelayMode.getValue() == 1
    );
    public final FloatProperty autoRestartDelayFactor = new FloatProperty(
        "follow-factor",
        1.0F,
        0.0F,
        10.0F,
        () -> this.mode.getValue() == 2 && this.autoRestart.getValue() && this.autoRestartDelayMode.getValue() == 0
    );
    public final BooleanProperty onlyOnKillAura = new BooleanProperty(
        "only-on-killaura", false, () -> this.mode.getValue() == 2
    );
    public final BooleanProperty showProgressBar = new BooleanProperty(
        "show-progress-bar", true, () -> this.mode.getValue() == 2
    );
    public final ModeProperty progressBarPosition = new ModeProperty(
        "progress-bar-position",
        4,
        new String[]{"TOPLEFT", "TOPRIGHT", "BOTTOMLEFT", "BOTTOMRIGHT", "BOTTOMCENTER"},
        () -> this.mode.getValue() == 2 && this.showProgressBar.getValue()
    );
    public final IntProperty progressBarWidth = new IntProperty(
        "progress-bar-width", 150, 50, 300, () -> this.mode.getValue() == 2 && this.showProgressBar.getValue()
    );
    public final IntProperty progressBarHeight = new IntProperty(
        "progress-bar-height", 8, 4, 20, () -> this.mode.getValue() == 2 && this.showProgressBar.getValue()
    );
    public final IntProperty progressBarMargin = new IntProperty(
        "progress-bar-margin", 10, 0, 50, () -> this.mode.getValue() == 2 && this.showProgressBar.getValue()
    );
    public final FloatProperty packetDistance = new FloatProperty(
        "packet-distance", 4.0F, 5.0F, 0.0F, 7.0F, () -> this.mode.getValue() == 1
    );
    public final IntProperty packetTimer = new IntProperty(
        "packet-timer", 200, 0, 2000, () -> this.mode.getValue() == 1
    );
    public final ModeProperty packetMode = new ModeProperty(
        "packet-mode", 0, new String[]{"PING", "DELAY"}, () -> this.mode.getValue() == 1
    );
    public final IntProperty packetPingSize = new IntProperty(
        "packet-ping-size", 0, 0, 2000, () -> this.mode.getValue() == 1 && this.packetMode.getValue() == 0
    );
    public final BooleanProperty packetPlayerModel = new BooleanProperty(
        "packet-player-model", true, () -> this.mode.getValue() == 1
    );
    public final BooleanProperty packetResetVelocity = new BooleanProperty(
        "packet-reset-velocity", false, () -> this.mode.getValue() == 1
    );
    public final ModeProperty legacyPos = new ModeProperty("legacy-pos", 0, new String[]{"OFF", "ON"});
    public final IntProperty maximumCachedPositions = new IntProperty("max-cached-positions", 10, 1, 100);
    public final IntProperty minDelay = new IntProperty("min-delay", 100, 0, 1000, () -> this.mode.getValue() == 3);
    public final IntProperty maxDelay = new IntProperty("max-delay", 150, 0, 1000, () -> this.mode.getValue() == 3);
    public final FloatProperty range = new FloatProperty("range", 3.0F, 0.0F, 10.0F, () -> this.mode.getValue() == 3);
    public final IntProperty chance = new IntProperty("chance", 100, 0, 100, () -> this.mode.getValue() == 3);
    public final BooleanProperty pauseOnHurtTime = new BooleanProperty(
        "pause-on-hurttime", false, () -> this.mode.getValue() == 3
    );
    public final IntProperty hurtTimeThreshold = new IntProperty(
        "hurttime-threshold", 3, 0, 10, () -> this.mode.getValue() == 3 && this.pauseOnHurtTime.getValue()
    );
    public final BooleanProperty showPosition = new BooleanProperty(
        "show-position", true, () -> this.mode.getValue() == 3
    );
    public final IntProperty manualWindow = new IntProperty(
        "manual-hit-window", 500, 50, 2000, () -> this.mode.getValue() == 3
    );
    private Vec3 crewLastPosition = null;
    private Vec3 crewCurrentPosition = null;
    private boolean crewHasTarget = false;
    private int crewCurrentChance = -1;
    private long crewNextAllowedTime = 0L;
    private EntityLivingBase crewManualTarget = null;
    private long crewManualTargetTime = 0L;
    private final Random crewRandom = new Random();
    private final Queue<BackTrack.QueuedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private final Queue<BackTrack.TimedPosition> positions = new ConcurrentLinkedQueue<>();
    private final Queue<BackTrack.PacketLog> packetPingQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Packet<?>> packetDelayQueue = new ConcurrentLinkedQueue<>();
    private final TimerUtil packetTimerUtil = new TimerUtil();
    private final Map<UUID, List<BackTrack.BacktrackData>> backtrackedPlayer = new ConcurrentHashMap<>();
    private final TimerUtil globalTimer = new TimerUtil();
    public EntityLivingBase target;
    private boolean shouldRender = true;
    private boolean ignoreWholeTick = false;
    private long delayForNextBacktrack = 0L;
    private int modernDelayValue = 80;
    private boolean modernDelayBoolean = false;
    private EntityPlayer packetTarget;
    private double packetRealX;
    private double packetRealY;
    private double packetRealZ;
    private final TimerUtil trackingBufferTimer = new TimerUtil();
    private final TimerUtil autoRestartTimer = new TimerUtil();
    private int attackCounter = 0;
    private boolean smartDistanceInitialized = false;
    private boolean isFirstAttack = true;
    private float rangeBase = 0.0F;
    private float smartMinRange = 0.0F;
    private float smartMaxRange = 0.0F;
    private boolean started = false;
    private EntityPlayer targetSaver;
    private int actualAutoRestartDelay = 0;
    private boolean progressBarActive = false;
    private long progressBarStartTime = 0L;
    private long progressBarCurrentTime = 0L;
    private static BackTrack instance;
    public static boolean isReleasingPackets = false;

    public BackTrack() {
        super("BackTrack", false);
        instance = this;
    }

    public static <T> T runWithNearestTrackedDistance(Entity entity, Supplier<T> action) {
        if (instance != null
            && instance.isEnabled()
            && instance.mode.getValue() != 2
            && entity instanceof EntityPlayer
            && entity == instance.target) {
            List<Vec3> candidates = new ArrayList<>();
            synchronized (instance.positions) {
                for (BackTrack.TimedPosition tp : instance.positions) {
                    candidates.add(tp.position);
                }
            }

            if (candidates.isEmpty()) {
                return action.get();
            }

            candidates.sort(
                Comparator.comparingDouble(
                    pos -> BackTrackUtil.runWithSimulatedPosition(
                        entity, pos, () -> BackTrackUtil.getDistanceToEntityBox(entity)
                    )
                )
            );
            Vec3 nearest = candidates.get(0);
            T result = BackTrackUtil.runWithSimulatedPosition(entity, nearest, action);
            return result != null ? result : action.get();
        } else {
            return action.get();
        }
    }

    private int getSupposedDelay() {
        return this.mode.getValue() != 0 && this.mode.getValue() != 2
            ? this.delayMs.getSecondValue().intValue()
            : this.modernDelayValue;
    }

    @Override
    public void onDisabled() {
        Miau.lagManager.setDelay(0);
        this.crewHasTarget = false;
        this.crewLastPosition = null;
        this.crewCurrentPosition = null;
        this.crewCurrentChance = -1;
        this.crewNextAllowedTime = 0L;
        this.crewManualTarget = null;
        this.crewManualTargetTime = 0L;
        this.clearPackets(true, true);
        this.clearPacketMode(true);
        this.backtrackedPlayer.clear();
        this.reset();
    }

    @Override
    public void onEnabled() {
        this.reset();
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        if (this.mode.getValue() == 0) {
            this.clearPackets(false, true);
            this.target = null;
        }

        this.backtrackedPlayer.clear();
        this.clearPacketMode(false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            Scaffold scaffold = (Scaffold)Miau.moduleManager.modules.get(Scaffold.class);
            if (scaffold != null && scaffold.isEnabled()) {
                this.clear();
            } else if (this.mode.getValue() == 3) {
                this.onTickCrew(event);
            } else if (this.mode.getValue() == 1) {
                this.updatePacketMode();
            } else if (this.mode.getValue() == 2) {
                this.onTickModern();
            } else {
                if (this.mode.getValue() == 0) {
                    if (this.shouldBacktrack() && this.target instanceof ITruePosition) {
                        ITruePosition targetMixin = (ITruePosition)this.target;
                        if (targetMixin.isTruePos()) {
                            double trueDist = mc.field_71439_g
                                .func_70011_f(targetMixin.getTrueX(), targetMixin.getTrueY(), targetMixin.getTrueZ());
                            double dist = mc.field_71439_g
                                .func_70011_f(
                                    this.target.field_70165_t, this.target.field_70163_u, this.target.field_70161_v
                                );
                            double boxDist = RotationUtil.distanceToBox(this.target.func_174813_aQ());
                            if (trueDist <= 6.0
                                && (!this.smart.getValue() || trueDist >= dist)
                                && (
                                    this.style.getValue() == 1
                                        || !this.globalTimer.hasTimeElapsed(this.getSupposedDelay())
                                )) {
                                this.shouldRender = true;
                                if (boxDist >= this.distance.getValue().floatValue()
                                    && boxDist <= this.distance.getSecondValue().floatValue()) {
                                    this.handlePackets();
                                } else {
                                    this.handlePacketsRange();
                                }
                            } else {
                                this.clear();
                            }
                        }
                    } else {
                        this.clear();
                    }
                }

                this.ignoreWholeTick = false;
                this.updateDelayCooldown();
            }
        }
    }

    private void onTickModern() {
        if (this.target != null && !mc.field_71441_e.field_72996_f.contains(this.target)) {
            this.clearPacketsImmediately();
            this.reset();
        } else {
            if (this.delayUpdateMode.getValue() == 2) {
                this.modernDelayValue = randomInt(
                    this.delayMs.getValue().intValue(), this.delayMs.getSecondValue().intValue()
                );
                this.modernDelayBoolean = true;
            }

            if (this.autoRestart.getValue() && this.autoRestartTimer.hasTimeElapsed(this.actualAutoRestartDelay)) {
                this.clearPackets(true, true);
                this.isFirstAttack = true;
            }

            if (!this.onlyOnKillAura.getValue() || this.isKillAuraActive()) {
                if (this.shouldBacktrackModern() && this.target instanceof ITruePosition) {
                    ITruePosition targetMixin = (ITruePosition)this.target;
                    if (targetMixin.isTruePos()) {
                        double trueDist = mc.field_71439_g
                            .func_70011_f(targetMixin.getTrueX(), targetMixin.getTrueY(), targetMixin.getTrueZ());
                        double dist = mc.field_71439_g
                            .func_70011_f(
                                this.target.field_70165_t, this.target.field_70163_u, this.target.field_70161_v
                            );
                        long effectiveDelay = this.getEffectiveDelay();
                        if (!this.progressBarActive) {
                            this.progressBarActive = true;
                            this.progressBarStartTime = System.currentTimeMillis();
                        }

                        this.progressBarCurrentTime = System.currentTimeMillis();
                        if (trueDist <= 6.0
                            && (!this.smart.getValue() || trueDist >= dist)
                            && (this.modernStyle.getValue() == 1 || !this.globalTimer.hasTimeElapsed(effectiveDelay))) {
                            this.shouldRender = true;
                            if (this.isInCurrentDistanceRange()) {
                                this.handlePacketsModern();
                            } else {
                                this.handlePacketsRangeModern();
                            }
                        } else {
                            this.clear();
                        }
                    }
                } else {
                    if (this.progressBarActive) {
                        this.progressBarActive = false;
                        this.progressBarStartTime = 0L;
                        this.progressBarCurrentTime = 0L;
                    }

                    this.clear();
                }

                this.ignoreWholeTick = false;
                if (this.delayUpdateMode.getValue() == 0) {
                    boolean shouldChangeDelay = this.packetQueue.isEmpty();
                    if (!shouldChangeDelay) {
                        this.modernDelayBoolean = false;
                    }

                    if (shouldChangeDelay && !this.modernDelayBoolean && !this.shouldBacktrackModern()) {
                        this.delayForNextBacktrack = System.currentTimeMillis()
                            + this.nextBacktrackDelay.getValue().intValue();
                        this.modernDelayValue = randomInt(
                            this.delayMs.getValue().intValue(), this.delayMs.getSecondValue().intValue()
                        );
                        this.modernDelayBoolean = true;
                    }
                }
            }
        }
    }

    private boolean isWorldUpdatePacket(Packet<?> packet) {
        return packet instanceof S21PacketChunkData
            || packet instanceof S22PacketMultiBlockChange
            || packet instanceof S23PacketBlockChange
            || packet instanceof S24PacketBlockAction
            || packet instanceof S25PacketBlockBreakAnim
            || packet instanceof S26PacketMapChunkBulk
            || packet instanceof S35PacketUpdateTileEntity;
    }

    private int crewRandomDelay() {
        int min = Math.min(this.minDelay.getValue(), this.maxDelay.getValue());
        int max = Math.max(this.minDelay.getValue(), this.maxDelay.getValue());
        return max <= min ? min : min + this.crewRandom.nextInt(max - min + 1);
    }

    private void onPacketCrew(PacketEvent event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity p = (C02PacketUseEntity)packet;
            if (p.func_149565_c() == Action.ATTACK && mc.field_71441_e != null) {
                Entity ent = p.func_149564_a(mc.field_71441_e);
                if (ent instanceof EntityLivingBase && ent != mc.field_71439_g) {
                    this.crewManualTarget = (EntityLivingBase)ent;
                    this.crewManualTargetTime = System.currentTimeMillis();
                }
            }
        }
    }

    private boolean crewIsValidTarget(EntityLivingBase entity) {
        if (entity == null) {
            return false;
        }

        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)entity;
            if (TeamUtil.isSameTeam(player)) {
                return false;
            }

            AntiBot antiBot = (AntiBot)Miau.moduleManager.modules.get(AntiBot.class);
            if (antiBot != null && antiBot.isEnabled() && antiBot.isBotPlayer(entity)) {
                return false;
            }
        }

        return true;
    }

    private EntityLivingBase crewResolveTarget() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        if (killAura != null && killAura.isEnabled()) {
            EntityLivingBase t = killAura.getTarget();
            if (t != null && this.crewIsValidTarget(t)) {
                return t;
            }
        }

        if (this.crewManualTarget != null && !this.crewManualTarget.field_70128_L) {
            long age = System.currentTimeMillis() - this.crewManualTargetTime;
            if (age <= this.manualWindow.getValue().intValue()) {
                if (this.crewIsValidTarget(this.crewManualTarget)) {
                    return this.crewManualTarget;
                }
            } else {
                this.crewManualTarget = null;
            }
        }

        return mc.field_147125_j instanceof EntityLivingBase
                && mc.field_147125_j != mc.field_71439_g
                && this.crewIsValidTarget((EntityLivingBase)mc.field_147125_j)
            ? (EntityLivingBase)mc.field_147125_j
            : null;
    }

    private void onTickCrew(TickEvent event) {
        EntityLivingBase target = this.crewResolveTarget();
        boolean shouldBacktrack = false;
        if (target != null) {
            float distance = mc.field_71439_g.func_70032_d(target);
            boolean inRange = distance <= this.range.getValue();
            if (this.crewCurrentChance < 0) {
                this.crewCurrentChance = this.crewRandom.nextInt(100);
            }

            boolean chanceOk = this.crewCurrentChance < this.chance.getValue();
            boolean paused = this.pauseOnHurtTime.getValue()
                && target.field_70737_aN >= this.hurtTimeThreshold.getValue();
            boolean cooldownOk = System.currentTimeMillis() >= this.crewNextAllowedTime;
            shouldBacktrack = inRange && chanceOk && !paused && cooldownOk && mc.field_71439_g.field_70173_aa > 10;
        }

        if (shouldBacktrack) {
            int ticks = Math.max(1, this.crewRandomDelay() / 50);
            Miau.lagManager.setDelay(ticks);
            this.crewHasTarget = true;
        } else {
            if (this.crewHasTarget) {
                int cd = this.nextBacktrackDelay.getValue();
                if (cd > 0) {
                    this.crewNextAllowedTime = System.currentTimeMillis() + cd;
                }

                this.crewCurrentChance = -1;
            }

            Miau.lagManager.setDelay(0);
            this.crewHasTarget = false;
        }

        if (event.getType() == EventType.POST) {
            Vec3 savedPosition = Miau.lagManager.getLastPosition();
            this.crewLastPosition = this.crewCurrentPosition;
            this.crewCurrentPosition = savedPosition;
        }
    }

    private void onRender3DCrew(Render3DEvent event) {
        if (this.showPosition.getValue()
            && this.crewHasTarget
            && this.crewLastPosition != null
            && this.crewCurrentPosition != null) {
            Color color = new Color(60, 162, 253, 100);
            double x = RenderUtil.lerpDouble(
                this.crewCurrentPosition.field_72450_a, this.crewLastPosition.field_72450_a, event.getPartialTicks()
            );
            double y = RenderUtil.lerpDouble(
                this.crewCurrentPosition.field_72448_b, this.crewLastPosition.field_72448_b, event.getPartialTicks()
            );
            double z = RenderUtil.lerpDouble(
                this.crewCurrentPosition.field_72449_c, this.crewLastPosition.field_72449_c, event.getPartialTicks()
            );
            float size = mc.field_71439_g.func_70111_Y();
            AxisAlignedBB aabb = new AxisAlignedBB(
                    x - mc.field_71439_g.field_70130_N / 2.0,
                    y,
                    z - mc.field_71439_g.field_70130_N / 2.0,
                    x + mc.field_71439_g.field_70130_N / 2.0,
                    y + mc.field_71439_g.field_70131_O,
                    z + mc.field_71439_g.field_70130_N / 2.0
                )
                .func_72314_b(size, size, size)
                .func_72317_d(
                    -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX(),
                    -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY(),
                    -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ()
                );
            RenderUtil.enableRenderState();
            RenderUtil.drawFilledBox(aabb, color.getRed(), color.getGreen(), color.getBlue());
            RenderUtil.disableRenderState();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && !event.isCancelled()) {
            if (!isReleasingPackets) {
                if (Miau.blinkManager == null || !Miau.blinkManager.isBlinking()) {
                    Packet<?> packet = event.getPacket();
                    if (event.getType() != EventType.RECEIVE || !this.isWorldUpdatePacket(packet)) {
                        if (this.mode.getValue() == 3) {
                            this.onPacketCrew(event);
                        } else if (this.mode.getValue() == 1) {
                            this.handlePacketMode(event, packet);
                        } else if (this.mode.getValue() == 2) {
                            this.onPacketModern(event);
                        } else {
                            if (this.mode.getValue() == 0) {
                                if (mc.func_71356_B() || mc.func_147104_D() == null) {
                                    this.clearPackets(true, false);
                                    return;
                                }

                                if (this.packetQueue.isEmpty() && !this.shouldBacktrack()) {
                                    return;
                                }

                                if (packet instanceof C00Handshake
                                    || packet instanceof C00PacketServerQuery
                                    || packet instanceof S02PacketChat
                                    || packet instanceof S01PacketPong) {
                                    return;
                                }

                                if (packet instanceof S29PacketSoundEffect) {
                                    String soundName = ((S29PacketSoundEffect)packet).func_149212_c();

                                    for (String s : NON_DELAYED_SOUND_SUBSTRINGS) {
                                        if (soundName.contains(s)) {
                                            return;
                                        }
                                    }
                                }

                                if (packet instanceof S06PacketUpdateHealth
                                    && ((S06PacketUpdateHealth)packet).func_149332_c() <= 0.0F) {
                                    this.clearPackets(true, true);
                                    return;
                                }

                                if (packet instanceof S13PacketDestroyEntities && this.target != null) {
                                    for (int id : ((S13PacketDestroyEntities)packet).func_149098_c()) {
                                        if (id == this.target.func_145782_y()) {
                                            this.clearPackets(true, true);
                                            this.reset();
                                            return;
                                        }
                                    }
                                }

                                if (packet instanceof S1CPacketEntityMetadata
                                    && this.target != null
                                    && ((S1CPacketEntityMetadata)packet).func_149375_d() == this.target.func_145782_y()
                                    )
                                 {
                                    if (this.isDeadMetadata((S1CPacketEntityMetadata)packet)) {
                                        this.clearPackets(true, true);
                                        this.reset();
                                        return;
                                    }

                                    return;
                                }

                                if (packet instanceof S19PacketEntityStatus && this.target != null) {
                                    Entity entity = ((S19PacketEntityStatus)packet).func_149161_a(mc.field_71441_e);
                                    if (entity != null && entity.func_145782_y() == this.target.func_145782_y()) {
                                        return;
                                    }
                                }

                                if (event.getType() == EventType.RECEIVE) {
                                    if (packet instanceof S14PacketEntity && this.target != null) {
                                        Entity entity = ((S14PacketEntity)packet).func_149065_a(mc.field_71441_e);
                                        if (entity != null && entity.func_145782_y() == this.target.func_145782_y()) {
                                            S14PacketEntity s14 = (S14PacketEntity)packet;
                                            double newX = this.target.field_70165_t + s14.func_149062_c() / 32.0;
                                            double newY = this.target.field_70163_u + s14.func_149061_d() / 32.0;
                                            double newZ = this.target.field_70161_v + s14.func_149064_e() / 32.0;
                                            this.positions
                                                .add(
                                                    new BackTrack.TimedPosition(
                                                        new Vec3(newX, newY, newZ), System.currentTimeMillis()
                                                    )
                                                );
                                        }
                                    } else if (packet instanceof S18PacketEntityTeleport
                                        && this.target != null
                                        && ((S18PacketEntityTeleport)packet).func_149451_c()
                                            == this.target.func_145782_y()) {
                                        S18PacketEntityTeleport s18 = (S18PacketEntityTeleport)packet;
                                        double newX = s18.func_149449_d() / 32.0;
                                        double newY = s18.func_149448_e() / 32.0;
                                        double newZ = s18.func_149446_f() / 32.0;
                                        this.positions
                                            .add(
                                                new BackTrack.TimedPosition(
                                                    new Vec3(newX, newY, newZ), System.currentTimeMillis()
                                                )
                                            );
                                    }

                                    event.setCancelled(true);
                                    this.packetQueue
                                        .add(new BackTrack.QueuedPacket(packet, System.currentTimeMillis()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void onPacketModern(PacketEvent event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof S13PacketDestroyEntities && this.target != null) {
            for (int id : ((S13PacketDestroyEntities)packet).func_149098_c()) {
                if (id == this.target.func_145782_y()) {
                    this.clearPacketsImmediately();
                    this.reset();
                    break;
                }
            }
        } else if (!this.onlyOnKillAura.getValue() || this.isKillAuraActive()) {
            if (mc.field_71439_g == null
                || this.target == null
                || mc.field_71439_g.field_70737_aN >= this.ownHurtTime.getValue()
                    && mc.field_71439_g.field_70737_aN <= this.ownHurtTime.getSecondValue()
                    && this.target.field_70737_aN >= this.enemyHurtTime.getValue()
                    && this.target.field_70737_aN <= this.enemyHurtTime.getSecondValue()) {
                if (this.startMode.getValue() != 1 || this.started) {
                    if (this.packetQueue.isEmpty() && event.getType() == EventType.RECEIVE) {
                        this.autoRestartTimer.reset();
                    }

                    if (mc.func_71356_B() || mc.func_147104_D() == null) {
                        this.clearPackets(true, false);
                    } else if (!this.packetQueue.isEmpty() || this.shouldBacktrackModern()) {
                        if (!(packet instanceof C00Handshake)
                            && !(packet instanceof C00PacketServerQuery)
                            && !(packet instanceof S02PacketChat)
                            && !(packet instanceof S01PacketPong)) {
                            if (packet instanceof S29PacketSoundEffect) {
                                String soundName = ((S29PacketSoundEffect)packet).func_149212_c();

                                for (String s : NON_DELAYED_SOUND_SUBSTRINGS) {
                                    if (soundName.contains(s)) {
                                        return;
                                    }
                                }
                            }

                            if (packet instanceof S06PacketUpdateHealth
                                && ((S06PacketUpdateHealth)packet).func_149332_c() <= 0.0F) {
                                this.clearPackets(true, true);
                            } else if (!(packet instanceof S13PacketDestroyEntities)) {
                                if (!(packet instanceof S00PacketKeepAlive) || this.extraMode.getValue() != 2) {
                                    if (!(packet instanceof S12PacketEntityVelocity)
                                            && !(packet instanceof S27PacketExplosion)
                                        || this.extraMode.getValue() != 1) {
                                        if (this.extraMode.getValue() == 1 && event.getType() == EventType.RECEIVE) {
                                            if (packet instanceof S14PacketEntity) {
                                                if (this.target != null
                                                    && ((IAccessorS14PacketEntity)packet).getEntityId()
                                                        == this.target.func_145782_y()) {
                                                    event.setCancelled(true);
                                                    this.packetQueue
                                                        .add(
                                                            new BackTrack.QueuedPacket(
                                                                packet, System.currentTimeMillis()
                                                            )
                                                        );
                                                    return;
                                                }
                                            } else if (packet instanceof S18PacketEntityTeleport) {
                                                if (this.target != null
                                                    && ((S18PacketEntityTeleport)packet).func_149451_c()
                                                        == this.target.func_145782_y()) {
                                                    event.setCancelled(true);
                                                    this.packetQueue
                                                        .add(
                                                            new BackTrack.QueuedPacket(
                                                                packet, System.currentTimeMillis()
                                                            )
                                                        );
                                                    return;
                                                }
                                            } else if (packet instanceof S1CPacketEntityMetadata) {
                                                if (this.target != null
                                                    && ((S1CPacketEntityMetadata)packet).func_149375_d()
                                                        == this.target.func_145782_y()) {
                                                    event.setCancelled(true);
                                                    this.packetQueue
                                                        .add(
                                                            new BackTrack.QueuedPacket(
                                                                packet, System.currentTimeMillis()
                                                            )
                                                        );
                                                    return;
                                                }
                                            } else if (packet instanceof S19PacketEntityStatus
                                                || packet instanceof S0CPacketSpawnPlayer) {
                                                return;
                                            }
                                        }

                                        if (event.getType() == EventType.RECEIVE) {
                                            if (packet instanceof S14PacketEntity && this.target != null) {
                                                Entity entity = ((S14PacketEntity)packet)
                                                    .func_149065_a(mc.field_71441_e);
                                                if (entity != null
                                                    && entity.func_145782_y() == this.target.func_145782_y()) {
                                                    ITruePosition tm = (ITruePosition)this.target;
                                                    this.positions
                                                        .add(
                                                            new BackTrack.TimedPosition(
                                                                new Vec3(tm.getTrueX(), tm.getTrueY(), tm.getTrueZ()),
                                                                System.currentTimeMillis()
                                                            )
                                                        );
                                                }
                                            } else if (packet instanceof S18PacketEntityTeleport
                                                && this.target != null
                                                && ((S18PacketEntityTeleport)packet).func_149451_c()
                                                    == this.target.func_145782_y()) {
                                                ITruePosition tm = (ITruePosition)this.target;
                                                this.positions
                                                    .add(
                                                        new BackTrack.TimedPosition(
                                                            new Vec3(tm.getTrueX(), tm.getTrueY(), tm.getTrueZ()),
                                                            System.currentTimeMillis()
                                                        )
                                                    );
                                            } else if (packet instanceof S1CPacketEntityMetadata
                                                && this.target != null
                                                && ((S1CPacketEntityMetadata)packet).func_149375_d()
                                                    == this.target.func_145782_y()
                                                && this.isDeadMetadata((S1CPacketEntityMetadata)packet)) {
                                                this.clearPackets(true, true);
                                                this.reset();
                                                return;
                                            }

                                            event.setCancelled(true);
                                            this.packetQueue
                                                .add(new BackTrack.QueuedPacket(packet, System.currentTimeMillis()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isValidTarget(event.getTarget())) {
            if (this.mode.getValue() == 1) {
                this.handlePacketAttack(event);
            } else if (this.mode.getValue() == 2) {
                this.onAttackModern(event);
            } else {
                if (this.mode.getValue() == 0) {
                    EntityLivingBase living = (EntityLivingBase)event.getTarget();
                    if (this.target != living) {
                        if (this.mode.getValue() == 0) {
                            this.clearPackets(true, true);
                        }

                        this.reset();
                    }

                    this.target = living;
                }
            }
        }
    }

    private void onAttackModern(AttackEvent event) {
        EntityLivingBase living = (EntityLivingBase)event.getTarget();
        switch (this.delayUpdateMode.getValue()) {
            case 1:
                if (this.attackCounter <= this.attackCountToUpdate.getValue()) {
                    this.attackCounter++;
                } else {
                    this.modernDelayValue = randomInt(
                        this.delayMs.getValue().intValue(), this.delayMs.getSecondValue().intValue()
                    );
                    this.modernDelayBoolean = true;
                    this.attackCounter = 0;
                }
                break;
            case 3:
                if (living instanceof EntityPlayer) {
                    this.targetSaver = (EntityPlayer)living;
                }
        }

        if (this.startMode.getValue() == 1) {
            this.started = true;
        }

        if (this.distanceMode.getValue() == 1 && living != null) {
            float initialDistance = (float)RotationUtil.distanceToBox(living.func_174813_aQ());
            if (this.isFirstAttack) {
                this.rangeBase = initialDistance;
                this.smartMinRange = Math.max(this.rangeBase - this.minExpandRange.getValue(), 0.0F);
                this.smartMaxRange = this.rangeBase + this.expandRange.getValue();
                this.smartDistanceInitialized = true;
                this.isFirstAttack = false;
            } else {
                this.smartMinRange = Math.max(this.rangeBase - this.minExpandRange.getValue(), 0.0F);
                this.smartMaxRange = this.rangeBase + this.expandRange.getValue();
                this.smartDistanceInitialized = true;
            }
        }

        if (this.target != living) {
            this.clearPackets(true, true);
            this.reset();
        }

        this.target = living;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof EntityLivingBase)) {
            return false;
        }

        EntityLivingBase living = (EntityLivingBase)entity;
        if (!living.func_70089_S()) {
            return false;
        }

        if (living == mc.field_71439_g) {
            return false;
        }

        if (living.func_82150_aj()) {
            return false;
        }

        if (living instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)living;
            if (player.func_175149_v()) {
                return false;
            }

            if (TeamUtil.isBot(player)) {
                return false;
            }

            if (TeamUtil.isFriend(player)) {
                return false;
            }
        }

        return true;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && mc.func_175598_ae() != null) {
            if (this.mode.getValue() == 3) {
                this.onRender3DCrew(event);
            } else if (this.mode.getValue() == 1) {
                this.renderPacketMode(event);
            } else if (this.mode.getValue() == 2) {
                this.onRender3DModern(event);
            } else if (this.mode.getValue() == 0) {
                if (this.shouldBacktrack() && this.shouldRender && this.target != null) {
                    BackTrack.TimedPosition renderPos = null;

                    for (BackTrack.TimedPosition p : this.positions) {
                        renderPos = p;
                    }

                    if (renderPos != null) {
                        double x = renderPos.position.field_72450_a - mc.func_175598_ae().field_78730_l;
                        double y = renderPos.position.field_72448_b - mc.func_175598_ae().field_78731_m;
                        double z = renderPos.position.field_72449_c - mc.func_175598_ae().field_78728_n;
                        Color color = new Color(this.espColor.getValue());
                        switch (this.espMode.getValue()) {
                            case 1:
                                AxisAlignedBB box = this.target
                                    .func_174813_aQ()
                                    .func_72317_d(
                                        renderPos.position.field_72450_a - this.target.field_70165_t,
                                        renderPos.position.field_72448_b - this.target.field_70163_u,
                                        renderPos.position.field_72449_c - this.target.field_70161_v
                                    );
                                this.drawBacktrackBox(box, color);
                                break;
                            case 2:
                                GlStateManager.func_179094_E();
                                GL11.glPushAttrib(1048575);
                                GlStateManager.func_179131_c(0.6F, 0.6F, 0.6F, 1.0F);
                                mc.func_175598_ae()
                                    .func_147939_a(
                                        this.target,
                                        x,
                                        y,
                                        z,
                                        this.target.field_70126_B
                                            + (this.target.field_70177_z - this.target.field_70126_B)
                                                * event.getPartialTicks(),
                                        event.getPartialTicks(),
                                        true
                                    );
                                GL11.glPopAttrib();
                                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                                GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
                                GlStateManager.func_179121_F();
                                break;
                            case 3:
                                GlStateManager.func_179094_E();
                                GL11.glPushAttrib(1048575);
                                GL11.glPolygonMode(1032, 6913);
                                GL11.glDisable(3553);
                                GL11.glDisable(2896);
                                GL11.glDisable(2929);
                                GL11.glEnable(2848);
                                GL11.glEnable(3042);
                                GL11.glBlendFunc(770, 771);
                                GL11.glLineWidth(this.wireframeWidth.getValue());
                                GL11.glColor4f(
                                    color.getRed() / 255.0F,
                                    color.getGreen() / 255.0F,
                                    color.getBlue() / 255.0F,
                                    color.getAlpha() / 255.0F
                                );
                                mc.func_175598_ae()
                                    .func_147939_a(
                                        this.target,
                                        x,
                                        y,
                                        z,
                                        this.target.field_70126_B
                                            + (this.target.field_70177_z - this.target.field_70126_B)
                                                * event.getPartialTicks(),
                                        event.getPartialTicks(),
                                        true
                                    );
                                GL11.glPopAttrib();
                                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                                GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
                                GlStateManager.func_179121_F();
                        }
                    }
                }
            }
        }
    }

    private void onRender3DModern(Render3DEvent event) {
        if (this.shouldBacktrackModern() && this.shouldRender && this.target != null) {
            if (!this.onlyOnKillAura.getValue() || this.isKillAuraActive()) {
                BackTrack.TimedPosition renderPos = null;

                for (BackTrack.TimedPosition p : this.positions) {
                    renderPos = p;
                }

                if (renderPos != null) {
                    double x = renderPos.position.field_72450_a - mc.func_175598_ae().field_78730_l;
                    double y = renderPos.position.field_72448_b - mc.func_175598_ae().field_78731_m;
                    double z = renderPos.position.field_72449_c - mc.func_175598_ae().field_78728_n;
                    Color color = new Color(this.espColor.getValue());
                    switch (this.espMode.getValue()) {
                        case 1:
                            AxisAlignedBB box = this.target
                                .func_174813_aQ()
                                .func_72317_d(
                                    renderPos.position.field_72450_a - this.target.field_70165_t,
                                    renderPos.position.field_72448_b - this.target.field_70163_u,
                                    renderPos.position.field_72449_c - this.target.field_70161_v
                                );
                            this.drawBacktrackBox(box, color);
                            break;
                        case 2:
                            this.renderBacktrackModel(this.target, x, y, z, event.getPartialTicks(), color);
                            break;
                        case 3:
                            this.renderBacktrackWireframe(this.target, x, y, z, event.getPartialTicks(), color);
                            break;
                        case 4:
                            this.drawOutline(this.target, x, y, z, event.getPartialTicks(), color);
                            break;
                        case 5:
                            this.drawTracer(this.target, x, y, z);
                            break;
                        case 6:
                            this.draw2DBox(this.target, x, y, z);
                            break;
                        case 7:
                            this.drawHealthBar(this.target, x, y, z);
                            break;
                        case 8:
                            this.drawArrowIndicator(this.target, x, y, z);
                    }
                }
            }
        }
    }

    private void handleLegacyTick() {
        long cutoff = System.currentTimeMillis() - this.getSupposedDelay();
        Iterator<Entry<UUID, List<BackTrack.BacktrackData>>> it = this.backtrackedPlayer.entrySet().iterator();

        while (it.hasNext()) {
            List<BackTrack.BacktrackData> data = it.next().getValue();
            data.removeIf(d -> d.time < cutoff);
            if (data.isEmpty()) {
                it.remove();
            }
        }

        if (this.legacyPos.getValue() == 0 && mc.field_71441_e != null) {
            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityPlayer && entity != mc.field_71439_g) {
                    this.addBacktrackData(
                        entity.func_110124_au(),
                        entity.field_70165_t,
                        entity.field_70163_u,
                        entity.field_70161_v,
                        System.currentTimeMillis()
                    );
                }
            }
        }
    }

    private void addBacktrackData(UUID id, double x, double y, double z, long time) {
        List<BackTrack.BacktrackData> data = this.backtrackedPlayer.get(id);
        if (data != null) {
            if (data.size() >= this.maximumCachedPositions.getValue()) {
                data.remove(0);
            }

            data.add(new BackTrack.BacktrackData(x, y, z, time));
        } else {
            List<BackTrack.BacktrackData> newData = new ArrayList<>();
            newData.add(new BackTrack.BacktrackData(x, y, z, time));
            this.backtrackedPlayer.put(id, newData);
        }
    }

    private List<BackTrack.BacktrackData> getBacktrackData(UUID id) {
        return this.backtrackedPlayer.get(id);
    }

    private void handleLegacyClientPosTick() {
        if (mc.field_71441_e != null) {
            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityPlayer && entity != mc.field_71439_g) {
                    this.addBacktrackData(
                        entity.func_110124_au(),
                        entity.field_70165_t,
                        entity.field_70163_u,
                        entity.field_70161_v,
                        System.currentTimeMillis()
                    );
                }
            }
        }
    }

    private void removeBacktrackData(UUID id) {
        this.backtrackedPlayer.remove(id);
    }

    private void handlePackets() {
        long delay = this.getSupposedDelay();
        this.packetQueue.removeIf(queuedPacket -> {
            if (queuedPacket.time <= System.currentTimeMillis() - delay) {
                this.receiveQueuedPacket(queuedPacket.packet);
                return true;
            } else {
                return false;
            }
        });
        this.positions.removeIf(pos -> pos.time < System.currentTimeMillis() - delay);
    }

    private void handlePacketsRange() {
        long time = this.getRangeTime();
        if (time == -1L) {
            this.clearPackets(true, true);
        } else {
            this.packetQueue.removeIf(queuedPacket -> {
                if (queuedPacket.time <= time) {
                    this.receiveQueuedPacket(queuedPacket.packet);
                    return true;
                } else {
                    return false;
                }
            });
            this.positions.removeIf(pos -> pos.time < time);
        }
    }

    private long getRangeTime() {
        if (this.target == null) {
            return -1L;
        }

        long time = 0L;
        boolean found = false;

        for (BackTrack.TimedPosition data : this.positions) {
            time = data.time;
            AxisAlignedBB targetBox = this.target
                .func_174813_aQ()
                .func_72317_d(
                    data.position.field_72450_a - this.target.field_70165_t,
                    data.position.field_72448_b - this.target.field_70163_u,
                    data.position.field_72449_c - this.target.field_70161_v
                );
            double dist = this.getDistanceToBox(targetBox);
            if (dist >= this.distance.getValue().floatValue() && dist <= this.distance.getSecondValue().floatValue()) {
                found = true;
                break;
            }
        }

        return found ? time : -1L;
    }

    private void handlePacketsModern() {
        long effectiveDelay = this.getEffectiveDelay();
        long now = System.currentTimeMillis();
        switch (this.modernStyle.getValue()) {
            case 0:
            case 1:
            case 3:
                long ed = effectiveDelay;
                this.packetQueue.removeIf(queuedPacket -> {
                    if (queuedPacket.time <= now - ed) {
                        this.receiveQueuedPacket(queuedPacket.packet);
                        return true;
                    } else {
                        return false;
                    }
                });
                break;
            case 2:
                long ed2 = effectiveDelay;
                this.packetQueue
                    .removeIf(
                        queuedPacket -> {
                            boolean shouldProcess = queuedPacket.time <= now - ed2
                                || now - queuedPacket.time < this.instantThreshold.getValue().intValue();
                            if (shouldProcess) {
                                this.receiveQueuedPacket(queuedPacket.packet);
                                return true;
                            } else {
                                return false;
                            }
                        }
                    );
                break;
            case 4:
                long ed4 = effectiveDelay;
                int stepCount = this.stepCount.getValue();
                boolean inRange = this.isInCurrentDistanceRange();
                int effectiveStepCount = inRange ? stepCount : Math.max(2, stepCount / 2);
                long stepSize = ed4 / effectiveStepCount;
                long currentStep = now / Math.max(1L, stepSize) % effectiveStepCount;
                this.packetQueue.removeIf(queuedPacket -> {
                    long packetStep = queuedPacket.time / Math.max(1L, stepSize) % effectiveStepCount;
                    if (packetStep <= currentStep) {
                        this.receiveQueuedPacket(queuedPacket.packet);
                        return true;
                    } else {
                        return false;
                    }
                });
                break;
            case 5:
                long ed5 = effectiveDelay;
                float effectiveBounce = this.bounceIntensity.getValue();
                if (this.autoRestart.getValue()
                    && this.autoRestartTimer.hasTimeElapsed((long)(this.actualAutoRestartDelay * 0.8))) {
                    effectiveBounce *= 0.3F;
                }

                float bounceFactor = (float)(Math.abs(Math.sin(now * 0.003)) * effectiveBounce + 1.0);
                long adjustedDelay = (long)((float)ed5 * bounceFactor);
                long ad = adjustedDelay;
                this.packetQueue.removeIf(queuedPacket -> {
                    if (queuedPacket.time <= now - ad) {
                        this.receiveQueuedPacket(queuedPacket.packet);
                        return true;
                    } else {
                        return false;
                    }
                });
        }

        this.positions.removeIf(pos -> pos.time < now - effectiveDelay);
    }

    private void handlePacketsRangeModern() {
        long time = this.getRangeTimeModern();
        if (time == -1L) {
            this.clearPackets(true, true);
        } else {
            this.packetQueue.removeIf(queuedPacket -> {
                if (queuedPacket.time <= time) {
                    this.receiveQueuedPacket(queuedPacket.packet);
                    return true;
                } else {
                    return false;
                }
            });
            this.positions.removeIf(pos -> pos.time < time);
        }
    }

    private long getRangeTimeModern() {
        if (this.target == null) {
            return -1L;
        }

        long time = 0L;
        boolean found = false;

        for (BackTrack.TimedPosition data : this.positions) {
            time = data.time;
            AxisAlignedBB targetBox = this.target
                .func_174813_aQ()
                .func_72317_d(
                    data.position.field_72450_a - this.target.field_70165_t,
                    data.position.field_72448_b - this.target.field_70163_u,
                    data.position.field_72449_c - this.target.field_70161_v
                );
            double dist = this.getDistanceToBox(targetBox);
            if (this.isInRange(dist)) {
                found = true;
                break;
            }
        }

        return found ? time : -1L;
    }

    private long getEffectiveDelay() {
        long baseDelay = this.getSupposedDelay();
        if ((this.target != null ? this.target.field_70737_aN : 0) > 5) {
            baseDelay = (long)(baseDelay * 0.8);
        }

        if (this.smart.getValue() && this.target instanceof ITruePosition) {
            ITruePosition tm = (ITruePosition)this.target;
            if (tm.isTruePos()) {
                double trueDist = mc.field_71439_g.func_70011_f(tm.getTrueX(), tm.getTrueY(), tm.getTrueZ());
                double dist = mc.field_71439_g
                    .func_70011_f(this.target.field_70165_t, this.target.field_70163_u, this.target.field_70161_v);
                if (trueDist < dist) {
                    baseDelay = (long)(baseDelay * 1.2);
                }
            }
        }

        return baseDelay;
    }

    private boolean isInCurrentDistanceRange() {
        if (this.distanceMode.getValue() == 1 && this.smartDistanceInitialized) {
            double boxDist = RotationUtil.distanceToBox(this.target.func_174813_aQ());
            return boxDist >= this.smartMinRange && boxDist <= this.smartMaxRange;
        } else {
            double boxDist = RotationUtil.distanceToBox(this.target.func_174813_aQ());
            return boxDist >= this.distance.getValue().floatValue()
                && boxDist <= this.distance.getSecondValue().floatValue();
        }
    }

    private boolean isInRange(double dist) {
        return this.distanceMode.getValue() == 1 && this.smartDistanceInitialized
            ? dist >= this.smartMinRange && dist <= this.smartMaxRange
            : dist >= this.distance.getValue().floatValue() && dist <= this.distance.getSecondValue().floatValue();
    }

    private boolean isKillAuraActive() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        return killAura != null && killAura.isEnabled() && killAura.getTarget() != null;
    }

    private boolean shouldBacktrackModern() {
        if (mc.field_71439_g != null
            && mc.field_71441_e != null
            && this.target != null
            && !(mc.field_71439_g.func_110143_aJ() <= 0.0F)
            && (Float.isNaN(this.target.func_110143_aJ()) || this.target.func_110143_aJ() > 0.0F)
            && mc.field_71442_b.func_178889_l() != GameType.SPECTATOR
            && System.currentTimeMillis() >= this.delayForNextBacktrack
            && mc.field_71439_g.field_70173_aa > 20
            && !this.ignoreWholeTick) {
            if (!this.target.func_70089_S()) {
                return false;
            }

            if (this.target == mc.field_71439_g) {
                return false;
            }

            if (this.target.func_82150_aj()) {
                return false;
            }

            if (this.target instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer)this.target;
                if (player.func_175149_v()) {
                    return false;
                }

                if (TeamUtil.isBot(player)) {
                    return false;
                }

                if (TeamUtil.isFriend(player)) {
                    return false;
                }
            }

            if (this.startMode.getValue() == 1 && !this.started) {
                return false;
            }

            boolean inRange = this.isInCurrentDistanceRange();
            if (inRange) {
                this.trackingBufferTimer.reset();
            }

            return inRange || !this.trackingBufferTimer.hasTimeElapsed(this.trackingBuffer.getValue().intValue());
        } else {
            if (!this.isFirstAttack) {
                this.isFirstAttack = true;
            }

            return false;
        }
    }

    private void clearPacketsImmediately() {
        this.packetQueue.removeIf(queuedPacket -> {
            this.receiveQueuedPacket(queuedPacket.packet);
            return true;
        });
        this.positions.clear();
        this.autoRestartTimer.reset();
        this.shouldRender = false;
        this.ignoreWholeTick = true;
        this.progressBarActive = false;
        this.progressBarStartTime = 0L;
        this.progressBarCurrentTime = 0L;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && this.mode.getValue() == 2) {
            if (event.getType() == EventType.PRE) {
                if (mc.field_71439_g != null && mc.field_71441_e != null) {
                    this.actualAutoRestartDelay = this.autoRestartDelayMode.getValue() == 1
                        ? this.autoRestartDelay.getValue()
                        : (int)((float)this.getEffectiveDelay() * this.autoRestartDelayFactor.getValue());
                    EntityPlayer currentTarget = this.targetSaver;
                    if (currentTarget != null) {
                        if ((currentTarget.func_110143_aJ() <= 0.0F || currentTarget.field_70128_L)
                            && !mc.field_71441_e.field_72996_f.contains(currentTarget)) {
                            this.modernDelayValue = randomInt(
                                this.delayMs.getValue().intValue(), this.delayMs.getSecondValue().intValue()
                            );
                            this.modernDelayBoolean = true;
                            this.targetSaver = null;
                        }
                    }
                }
            }
        }
    }

    private double getDistanceToBox(AxisAlignedBB box) {
        if (mc.field_71439_g != null && box != null) {
            double x = clamp(mc.field_71439_g.field_70165_t, box.field_72340_a, box.field_72336_d);
            double y = clamp(
                mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(), box.field_72338_b, box.field_72337_e
            );
            double z = clamp(mc.field_71439_g.field_70161_v, box.field_72339_c, box.field_72334_f);
            return Math.sqrt(
                (mc.field_71439_g.field_70165_t - x) * (mc.field_71439_g.field_70165_t - x)
                    + (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e() - y)
                        * (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e() - y)
                    + (mc.field_71439_g.field_70161_v - z) * (mc.field_71439_g.field_70161_v - z)
            );
        } else {
            return 0.0;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void clearPackets(boolean handlePackets, boolean stopRendering) {
        this.packetQueue.removeIf(queuedPacket -> {
            if (handlePackets) {
                this.receiveQueuedPacket(queuedPacket.packet);
            }

            return true;
        });
        this.positions.clear();
        if (stopRendering) {
            this.shouldRender = false;
            this.ignoreWholeTick = true;
        }
    }

    private void updateDelayCooldown() {
        boolean shouldChangeDelay = this.packetQueue.isEmpty();
        if (!shouldChangeDelay) {
            this.modernDelayBoolean = false;
        }

        if (shouldChangeDelay && !this.modernDelayBoolean && !this.shouldBacktrack()) {
            this.delayForNextBacktrack = System.currentTimeMillis() + this.nextBacktrackDelay.getValue().intValue();
            this.modernDelayValue = randomInt(
                this.delayMs.getValue().intValue(), this.delayMs.getSecondValue().intValue()
            );
            this.modernDelayBoolean = true;
        }
    }

    private void clear() {
        this.clearPackets(true, true);
        this.globalTimer.reset();
    }

    private void reset() {
        this.target = null;
        this.globalTimer.reset();
    }

    private boolean shouldBacktrack() {
        if (mc.field_71439_g == null
            || mc.field_71441_e == null
            || this.target == null
            || mc.field_71439_g.func_110143_aJ() <= 0.0F
            || !Float.isNaN(this.target.func_110143_aJ()) && !(this.target.func_110143_aJ() > 0.0F)
            || mc.field_71442_b.func_178889_l() == GameType.SPECTATOR
            || System.currentTimeMillis() < this.delayForNextBacktrack
            || mc.field_71439_g.field_70173_aa <= 20
            || this.ignoreWholeTick) {
            return false;
        }

        if (!this.target.func_70089_S()) {
            return false;
        }

        if (this.target == mc.field_71439_g) {
            return false;
        }

        if (this.target.func_82150_aj()) {
            return false;
        }

        if (this.target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)this.target;
            if (player.func_175149_v()) {
                return false;
            }

            if (TeamUtil.isBot(player)) {
                return false;
            }

            if (TeamUtil.isFriend(player)) {
                return false;
            }
        }

        return true;
    }

    private boolean isDeadMetadata(S1CPacketEntityMetadata packet) {
        if (packet.func_149376_c() == null) {
            return false;
        }

        for (Object watchedObject : packet.func_149376_c()) {
            if (watchedObject instanceof WatchableObject) {
                WatchableObject data = (WatchableObject)watchedObject;
                if (data.func_75672_a() == 6 && data.func_75669_b() != null) {
                    try {
                        double value = Double.parseDouble(data.func_75669_b().toString());
                        if (!Double.isNaN(value) && value <= 0.0) {
                            return true;
                        }
                    } catch (NumberFormatException var7) {
                    }
                }
            }
        }

        return false;
    }

    private void receiveQueuedPacket(Packet<?> packet) {
        if (packet != null && mc.func_147114_u() != null && mc.field_71441_e != null && mc.field_71439_g != null) {
            try {
                isReleasingPackets = true;
                PacketEvent event = new PacketEvent(EventType.RECEIVE, packet);
                EventManager.call(event);
                if (!event.isCancelled()) {
                    PacketUtil.handlePacket((Packet<INetHandlerPlayClient>)packet);
                }

                isReleasingPackets = false;
            } catch (RuntimeException ignored) {
                isReleasingPackets = false;
            }
        }
    }

    private void renderBacktrackModel(Entity entity, double x, double y, double z, float partialTicks, Color color) {
        GlStateManager.func_179094_E();
        GL11.glPushAttrib(1048575);
        GlStateManager.func_179131_c(0.6F, 0.6F, 0.6F, 1.0F);
        mc.func_175598_ae()
            .func_147939_a(
                entity,
                x,
                y,
                z,
                entity.field_70126_B + (entity.field_70177_z - entity.field_70126_B) * partialTicks,
                partialTicks,
                true
            );
        GL11.glPopAttrib();
        GlStateManager.func_179117_G();
        GlStateManager.func_179121_F();
    }

    private void renderBacktrackWireframe(Entity entity, double x, double y, double z, float partialTicks, Color color) {
        GlStateManager.func_179094_E();
        GL11.glPushAttrib(1048575);
        GL11.glPolygonMode(1032, 6913);
        GL11.glDisable(3553);
        GL11.glDisable(2896);
        GL11.glDisable(2929);
        GL11.glEnable(2848);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glLineWidth(this.wireframeWidth.getValue());
        GL11.glColor4f(
            color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F
        );
        mc.func_175598_ae()
            .func_147939_a(
                entity,
                x,
                y,
                z,
                entity.field_70126_B + (entity.field_70177_z - entity.field_70126_B) * partialTicks,
                partialTicks,
                true
            );
        GL11.glPopAttrib();
        GlStateManager.func_179117_G();
        GlStateManager.func_179121_F();
    }

    private void drawOutline(EntityLivingBase entity, double x, double y, double z, float partialTicks, Color color) {
        GlStateManager.func_179094_E();
        GL11.glPushAttrib(1048575);
        GL11.glDisable(3553);
        GL11.glDisable(2896);
        GL11.glEnable(2848);
        GL11.glEnable(3042);
        GL11.glDisable(2929);
        GL11.glBlendFunc(770, 771);
        boolean hurt = this.target != null
            && this.target.field_70737_aN >= this.enemyHurtTime.getValue()
            && this.target.field_70737_aN <= this.enemyHurtTime.getSecondValue();
        Color highlight = hurt ? Color.RED : color;
        GL11.glLineWidth(hurt ? this.outlineWidth.getValue() * 1.5F : this.outlineWidth.getValue());
        GL11.glColor4f(
            highlight.getRed() / 255.0F,
            highlight.getGreen() / 255.0F,
            highlight.getBlue() / 255.0F,
            highlight.getAlpha() / 255.0F
        );
        mc.func_175598_ae()
            .func_147939_a(
                entity,
                x,
                y,
                z,
                entity.field_70126_B + (entity.field_70177_z - entity.field_70126_B) * partialTicks,
                partialTicks,
                false
            );
        GL11.glPopAttrib();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.func_179117_G();
        GlStateManager.func_179121_F();
    }

    private void drawTracer(EntityLivingBase entity, double x, double y, double z) {
        Color color = new Color(this.espColor.getValue());
        GL11.glPushMatrix();
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glEnable(3042);
        GL11.glDisable(2929);
        GL11.glLineWidth(this.tracerWidth.getValue());
        GL11.glColor4f(
            color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F
        );
        GL11.glBegin(1);
        GL11.glVertex3d(0.0, mc.field_71439_g.func_70047_e(), 0.0);
        GL11.glVertex3d(x, y + entity.field_70131_O / 2.0, z);
        GL11.glEnd();
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDisable(2848);
        GL11.glDisable(3042);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    private void draw2DBox(EntityLivingBase entity, double x, double y, double z) {
        Color color = new Color(this.espColor.getValue());
        GL11.glPushMatrix();
        GL11.glDisable(3553);
        GL11.glEnable(3042);
        GL11.glDisable(2929);
        GL11.glColor4f(
            color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F
        );
        GL11.glBegin(2);
        GL11.glVertex3d(x - 0.3, y, z - 0.3);
        GL11.glVertex3d(x + 0.3, y, z - 0.3);
        GL11.glVertex3d(x + 0.3, y + entity.field_70131_O, z - 0.3);
        GL11.glVertex3d(x - 0.3, y + entity.field_70131_O, z - 0.3);
        GL11.glEnd();
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDisable(3042);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    private void drawHealthBar(EntityLivingBase entity, double x, double y, double z) {
        GL11.glPushMatrix();
        GL11.glDisable(3553);
        GL11.glEnable(3042);
        GL11.glDisable(2929);
        float health = entity.func_110143_aJ();
        float maxHealth = entity.func_110138_aP();
        float healthPercent = health / maxHealth;
        if (healthPercent > 1.0F) {
            healthPercent = 1.0F;
        }

        if (healthPercent < 0.0F) {
            healthPercent = 0.0F;
        }

        float barWidth = 0.8F;
        float barHeight = 0.15F;
        float verticalOffset = 0.3F;
        GL11.glTranslated(x, y + entity.field_70131_O + verticalOffset, z);
        GL11.glColor4f(0.3F, 0.3F, 0.3F, 0.7F);
        GL11.glBegin(7);
        GL11.glVertex3d(-barWidth / 2.0, -barHeight / 2.0, 0.0);
        GL11.glVertex3d(barWidth / 2.0, -barHeight / 2.0, 0.0);
        GL11.glVertex3d(barWidth / 2.0, barHeight / 2.0, 0.0);
        GL11.glVertex3d(-barWidth / 2.0, barHeight / 2.0, 0.0);
        GL11.glEnd();
        float barRight = -barWidth / 2.0F + barWidth * healthPercent;
        float r;
        float g;
        float b;
        if (healthPercent > 0.5F) {
            r = 0.0F;
            g = 1.0F;
            b = 0.0F;
        } else if (healthPercent > 0.2F) {
            r = 1.0F;
            g = 1.0F;
            b = 0.0F;
        } else {
            r = 1.0F;
            g = 0.0F;
            b = 0.0F;
        }

        GL11.glColor4f(r, g, b, 0.7F);
        GL11.glBegin(7);
        GL11.glVertex3d(-barWidth / 2.0, -barHeight / 2.0, 0.0);
        GL11.glVertex3d(barRight, -barHeight / 2.0, 0.0);
        GL11.glVertex3d(barRight, barHeight / 2.0, 0.0);
        GL11.glVertex3d(-barWidth / 2.0, barHeight / 2.0, 0.0);
        GL11.glEnd();
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDisable(3042);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
        GlStateManager.func_179117_G();
    }

    private void drawArrowIndicator(EntityLivingBase entity, double x, double y, double z) {
        Color color = new Color(this.espColor.getValue());
        GL11.glPushMatrix();
        GL11.glDisable(3553);
        GL11.glEnable(3042);
        GL11.glDisable(2929);
        GL11.glEnable(2848);
        GL11.glColor4f(
            color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F
        );
        GL11.glLineWidth(2.0F);
        GL11.glBegin(1);
        GL11.glVertex3d(x, y + entity.field_70131_O / 2.0, z);
        GL11.glVertex3d(x, y + entity.field_70131_O / 2.0 + this.arrowSize.getValue().floatValue() / 10.0, z);
        GL11.glEnd();
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDisable(2848);
        GL11.glDisable(3042);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    private void drawBacktrackBox(AxisAlignedBB box, Color color) {
        GL11.glPushAttrib(1048575);
        GlStateManager.func_179094_E();
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glEnable(3042);
        GL11.glDepthMask(false);
        RenderGlobal.func_181563_a(
            new AxisAlignedBB(
                box.field_72340_a - mc.func_175598_ae().field_78730_l,
                box.field_72338_b - mc.func_175598_ae().field_78731_m,
                box.field_72339_c - mc.func_175598_ae().field_78728_n,
                box.field_72336_d - mc.func_175598_ae().field_78730_l,
                box.field_72337_e - mc.func_175598_ae().field_78731_m,
                box.field_72334_f - mc.func_175598_ae().field_78728_n
            ),
            color.getRed(),
            color.getGreen(),
            color.getBlue(),
            color.getAlpha()
        );
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDisable(3042);
        GL11.glDepthMask(true);
        GlStateManager.func_179121_F();
        GL11.glPopAttrib();
        GlStateManager.func_179117_G();
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled() && this.mode.getValue() == 2 && mc.field_71466_p != null) {
            if (this.showProgressBar.getValue() && this.progressBarActive) {
                if (!this.onlyOnKillAura.getValue() || this.isKillAuraActive()) {
                    long supposed = this.getSupposedDelay();
                    if (supposed > 0L) {
                        float progress = (float)(this.progressBarCurrentTime - this.progressBarStartTime)
                            / (float)supposed;
                        if (progress > 1.0F) {
                            progress = 1.0F;
                        }

                        if (progress < 0.0F) {
                            progress = 0.0F;
                        }

                        ScaledResolution sr = new ScaledResolution(mc);
                        int width = this.progressBarWidth.getValue();
                        int height = this.progressBarHeight.getValue();
                        int margin = this.progressBarMargin.getValue();
                        int sw = sr.func_78326_a();
                        int sh = sr.func_78328_b();
                        int x;
                        int y;
                        switch (this.progressBarPosition.getValue()) {
                            case 0:
                                x = margin;
                                y = margin;
                                break;
                            case 1:
                                x = sw - width - margin;
                                y = margin;
                                break;
                            case 2:
                                x = margin;
                                y = sh - height - margin;
                                break;
                            case 3:
                                x = sw - width - margin;
                                y = sh - height - margin;
                                break;
                            default:
                                x = (sw - width) / 2;
                                y = sh - height - margin;
                        }

                        GL11.glPushMatrix();
                        GL11.glDisable(3553);
                        GL11.glEnable(3042);
                        GL11.glBlendFunc(770, 771);
                        GL11.glDisable(2929);
                        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.58F);
                        GL11.glBegin(7);
                        GL11.glVertex2i(x, y);
                        GL11.glVertex2i(x + width, y);
                        GL11.glVertex2i(x + width, y + height);
                        GL11.glVertex2i(x, y + height);
                        GL11.glEnd();
                        int progressWidth = (int)(width * progress);
                        if (progressWidth > 0) {
                            GL11.glColor4f(0.0F, 0.58F, 1.0F, 0.78F);
                            GL11.glBegin(7);
                            GL11.glVertex2i(x, y);
                            GL11.glVertex2i(x + progressWidth, y);
                            GL11.glVertex2i(x + progressWidth, y + height);
                            GL11.glVertex2i(x, y + height);
                            GL11.glEnd();
                        }

                        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                        GL11.glLineWidth(1.0F);
                        GL11.glBegin(2);
                        GL11.glVertex2i(x, y);
                        GL11.glVertex2i(x + width, y);
                        GL11.glVertex2i(x + width, y + height);
                        GL11.glVertex2i(x, y + height);
                        GL11.glEnd();
                        GL11.glEnable(3553);
                        GL11.glEnable(2929);
                        GL11.glDisable(3042);
                        GL11.glPopMatrix();
                        long elapsed = this.progressBarCurrentTime - this.progressBarStartTime;
                        if (elapsed > supposed) {
                            elapsed = supposed;
                        }

                        String text = elapsed + "ms / " + supposed + "ms";
                        int textWidth = mc.field_71466_p.func_78256_a(text);
                        mc.field_71466_p
                            .func_78276_b(text, x + (width - textWidth) / 2, y - mc.field_71466_p.field_78288_b - 2, -1);
                        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return this.mode.getValue() == 2
            ? new String[]{this.modernStyle.getModeString() + " " + this.getSupposedDelay() + "ms"}
            : super.getSuffix();
    }

    private boolean shouldPacketBacktrack() {
        return mc.field_71439_g != null
            && mc.field_71441_e != null
            && this.packetTarget != null
            && !this.packetTarget.field_70128_L
            && this.packetTarget.func_70089_S()
            && mc.field_71439_g.func_70032_d(this.packetTarget) >= this.packetDistance.getValue()
            && mc.field_71439_g.func_70032_d(this.packetTarget) <= this.packetDistance.getSecondValue()
            && !TeamUtil.isBot(this.packetTarget);
    }

    private void handlePacketAttack(AttackEvent event) {
        if (event.getTarget() instanceof EntityPlayer && mc.field_71439_g != null && mc.field_71441_e != null) {
            EntityPlayer attacked = (EntityPlayer)event.getTarget();
            if (this.packetTarget != attacked) {
                this.clearPacketMode(true);
                this.packetTarget = attacked;
                this.packetRealX = attacked.field_70165_t;
                this.packetRealY = attacked.field_70163_u;
                this.packetRealZ = attacked.field_70161_v;
            }
        }
    }

    private void handlePacketMode(PacketEvent event, Packet<?> packet) {
        if (mc.field_71439_g != null
            && mc.field_71441_e != null
            && this.packetTarget != null
            && event.getType() == EventType.RECEIVE) {
            if (packet instanceof S40PacketDisconnect
                || packet instanceof S02PacketChat
                || packet instanceof S08PacketPlayerPosLook) {
                this.flushPacketMode();
            } else if (packet instanceof S06PacketUpdateHealth
                && ((S06PacketUpdateHealth)packet).func_149332_c() <= 0.0F) {
                this.flushPacketMode();
            } else if (packet instanceof S13PacketDestroyEntities
                && this.containsEntityId((S13PacketDestroyEntities)packet, this.packetTarget.func_145782_y())) {
                this.flushPacketMode();
            } else if (packet.getClass().getSimpleName().toLowerCase().startsWith("s")) {
                if (packet instanceof S14PacketEntity) {
                    Entity entity = ((S14PacketEntity)packet).func_149065_a(mc.field_71441_e);
                    if (entity != null && entity.func_145782_y() == this.packetTarget.func_145782_y()) {
                        this.packetRealX = this.packetRealX + ((S14PacketEntity)packet).func_149062_c() / 32.0;
                        this.packetRealY = this.packetRealY + ((S14PacketEntity)packet).func_149061_d() / 32.0;
                        this.packetRealZ = this.packetRealZ + ((S14PacketEntity)packet).func_149064_e() / 32.0;
                    }
                } else if (packet instanceof S18PacketEntityTeleport
                    && ((S18PacketEntityTeleport)packet).func_149451_c() == this.packetTarget.func_145782_y()) {
                    this.packetRealX = ((S18PacketEntityTeleport)packet).func_149449_d() / 32.0;
                    this.packetRealY = ((S18PacketEntityTeleport)packet).func_149448_e() / 32.0;
                    this.packetRealZ = ((S18PacketEntityTeleport)packet).func_149446_f() / 32.0;
                } else if (packet instanceof S12PacketEntityVelocity
                    && this.packetResetVelocity.getValue()
                    && ((S12PacketEntityVelocity)packet).func_149412_c() == mc.field_71439_g.func_145782_y()) {
                    this.clearPacketMode(true);
                    return;
                }

                event.setCancelled(true);
                if (this.packetMode.getValue() == 0) {
                    this.packetPingQueue.add(new BackTrack.PacketLog(packet, System.currentTimeMillis()));
                } else {
                    this.packetDelayQueue.add(packet);
                }
            }
        }
    }

    private boolean containsEntityId(S13PacketDestroyEntities packet, int id) {
        for (int entityId : packet.func_149098_c()) {
            if (entityId == id) {
                return true;
            }
        }

        return false;
    }

    private void flushPacketMode() {
        this.packetPingQueue.removeIf(log -> {
            this.receiveQueuedPacket(log.packet);
            return true;
        });
        this.packetDelayQueue.removeIf(packet -> {
            this.receiveQueuedPacket((Packet<?>)packet);
            return true;
        });
    }

    private void updatePacketMode() {
        if (!this.shouldPacketBacktrack()) {
            this.clearPacketMode(true);
        } else {
            if (this.packetMode.getValue() == 0) {
                this.clearPacketPing(this.packetPingSize.getValue());
            } else if (this.packetTimerUtil.hasTimeElapsed(this.packetTimer.getValue().intValue())) {
                this.clearPacketDelay();
            }
        }
    }

    private void clearPacketMode(boolean handlePackets) {
        if (handlePackets) {
            this.clearPacketPing(0);
            this.clearPacketDelay();
        }

        this.packetPingQueue.clear();
        this.packetDelayQueue.clear();
        this.packetTarget = null;
        this.packetRealX = this.packetRealY = this.packetRealZ = 0.0;
    }

    private void clearPacketPing(int delay) {
        this.packetPingQueue.removeIf(log -> {
            if (delay != 0 && System.currentTimeMillis() <= log.time + delay) {
                return false;
            }

            this.receiveQueuedPacket(log.packet);
            return true;
        });
    }

    private void clearPacketDelay() {
        this.packetDelayQueue.removeIf(packet -> {
            this.receiveQueuedPacket((Packet<?>)packet);
            this.packetTimerUtil.reset();
            return true;
        });
    }

    private void renderPacketMode(Render3DEvent event) {
        if (this.packetTarget != null && this.packetPlayerModel.getValue()) {
            GlStateManager.func_179094_E();
            mc.func_175598_ae()
                .func_147939_a(
                    this.packetTarget,
                    this.packetRealX - mc.func_175598_ae().field_78730_l,
                    this.packetRealY - mc.func_175598_ae().field_78731_m,
                    this.packetRealZ - mc.func_175598_ae().field_78728_n,
                    this.packetTarget.field_70177_z,
                    event.getPartialTicks(),
                    true
                );
            GlStateManager.func_179121_F();
            GlStateManager.func_179117_G();
        }
    }

    private static int randomInt(int min, int max) {
        return RandomUtil.nextInt(Math.min(min, max), Math.max(min, max));
    }

    private static class BacktrackData {
        private final double x;
        private final double y;
        private final double z;
        private final long time;

        BacktrackData(double x, double y, double z, long time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
        }
    }

    private static class PacketLog {
        private final Packet<?> packet;
        private final long time;

        PacketLog(Packet<?> packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }

    private static class QueuedPacket {
        private final Packet<?> packet;
        private final long time;

        QueuedPacket(Packet<?> packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }

    private static class TimedPosition {
        private final Vec3 position;
        private final long time;

        TimedPosition(Vec3 position, long time) {
            this.position = position;
            this.time = time;
        }
    }
}
