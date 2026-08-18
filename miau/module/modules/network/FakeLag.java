package miau.module.modules.network;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import miau.Miau;
import miau.enums.BlinkModules;
import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorS27PacketExplosion;
import miau.module.Module;
import miau.module.modules.combat.KillAura;
import miau.module.modules.player.Scaffold;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ColorProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.TextProperty;
import miau.util.client.ChatUtil;
import miau.util.network.PacketUtil;
import miau.util.player.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C12PacketUpdateSign;
import net.minecraft.network.play.client.C19PacketResourcePackStatus;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

public class FakeLag extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty(
        "mode", 0, new String[]{"Latence", "Dynamic", "Adaptive", "Stutter", "Crew"}
    );
    public final IntProperty delay = new IntProperty("delay", 550, 0, 1000, () -> this.mode.getValue() == 0);
    public final IntProperty recoilTime = new IntProperty("recoil-time", 750, 0, 2000, () -> this.mode.getValue() == 0);
    public final FloatProperty allowedDistToEnemy = new FloatProperty(
        "allowed-dist", 1.5F, 3.5F, 0.0F, 6.0F, () -> this.mode.getValue() == 0
    );
    public final BooleanProperty blinkOnAttack = new BooleanProperty(
        "blink-on-attack", true, () -> this.mode.getValue() == 0
    );
    public final BooleanProperty blinkOnInteract = new BooleanProperty(
        "blink-on-interact", true, () -> this.mode.getValue() == 0
    );
    public final BooleanProperty blinkOnHurt = new BooleanProperty(
        "blink-on-hurt", true, () -> this.mode.getValue() == 0
    );
    public final BooleanProperty blinkOnKnockBack = new BooleanProperty(
        "blink-on-knock-back", true, () -> this.mode.getValue() == 0
    );
    public final BooleanProperty blinkOnUsingItem = new BooleanProperty(
        "blink-on-using-item", false, () -> this.mode.getValue() == 0
    );
    public final BooleanProperty blinkOnEating = new BooleanProperty(
        "blink-on-eating", false, () -> this.mode.getValue() == 0 && !this.blinkOnUsingItem.getValue()
    );
    public final BooleanProperty blinkOnScaffolding = new BooleanProperty(
        "blink-on-scaffolding", false, () -> this.mode.getValue() == 0
    );
    public final BooleanProperty pauseOnNoMove = new BooleanProperty(
        "pause-on-no-move", true, () -> this.mode.getValue() == 0
    );
    public final BooleanProperty pauseOnChest = new BooleanProperty(
        "pause-on-chest", false, () -> this.mode.getValue() == 0
    );
    public final BooleanProperty onlyKillAura = new BooleanProperty(
        "only-kill-aura", false, () -> this.mode.getValue() == 0
    );
    public final BooleanProperty onlyWorkWhenInRange = new BooleanProperty(
        "only-work-when-in-range", false, () -> this.mode.getValue() == 0 && this.onlyKillAura.getValue()
    );
    public final FloatProperty rangeLimit = new FloatProperty(
        "range-limit",
        0.0F,
        4.5F,
        0.0F,
        10.0F,
        () -> this.mode.getValue() == 0 && this.onlyKillAura.getValue() && this.onlyWorkWhenInRange.getValue()
    );
    public final BooleanProperty pauseOnS08 = new BooleanProperty("pause-on-s08", true, () -> this.mode.getValue() == 0);
    public final IntProperty pauseTick = new IntProperty(
        "pause-tick", 10, 0, 20, () -> this.mode.getValue() == 0 && this.pauseOnS08.getValue()
    );
    public final BooleanProperty line = new BooleanProperty("line", true, () -> this.mode.getValue() == 0);
    public final ColorProperty lineColor = new ColorProperty(
        "line-color", Color.GREEN.getRGB(), () -> this.mode.getValue() == 0 && this.line.getValue()
    );
    public final BooleanProperty renderModel = new BooleanProperty(
        "render-model", false, () -> this.mode.getValue() == 0
    );
    public final ModeProperty tagMode = new ModeProperty("tag-mode", 0, new String[]{"PacketCount", "Custom", "None"});
    public final TextProperty customTagText = new TextProperty("tag-text", "", () -> this.tagMode.getValue() == 1);
    public final IntProperty dynamicDelay = new IntProperty(
        "dynamic-delay", 200, 25, 1000, () -> this.mode.getValue() == 1
    );
    public final IntProperty dynamicCooldown = new IntProperty(
        "dynamic-cooldown", 120, 0, 500, () -> this.mode.getValue() == 1
    );
    public final BooleanProperty dynamicDebug = new BooleanProperty(
        "dynamic-debug", false, () -> this.mode.getValue() == 1
    );
    public final BooleanProperty dynamicIgnoreTeammates = new BooleanProperty(
        "dynamic-ignore-teammates", true, () -> this.mode.getValue() == 1
    );
    public final BooleanProperty dynamicStopOnHurt = new BooleanProperty(
        "dynamic-stop-on-hurt", true, () -> this.mode.getValue() == 1
    );
    public final IntProperty dynamicStopOnHurtTime = new IntProperty(
        "dynamic-stop-on-hurt-time", 500, 0, 1000, () -> this.mode.getValue() == 1 && this.dynamicStopOnHurt.getValue()
    );
    public final FloatProperty dynamicStartRange = new FloatProperty(
        "dynamic-start-range", 6.0F, 3.0F, 10.0F, () -> this.mode.getValue() == 1
    );
    public final FloatProperty dynamicStopRange = new FloatProperty(
        "dynamic-stop-range", 3.5F, 1.0F, 6.0F, () -> this.mode.getValue() == 1
    );
    public final FloatProperty dynamicMaxTargetRange = new FloatProperty(
        "dynamic-max-target-range", 15.0F, 6.0F, 20.0F, () -> this.mode.getValue() == 1
    );
    public final FloatProperty adaptiveEnableRange = new FloatProperty(
        "adaptive-enable-range", 20.0F, 4.0F, 64.0F, () -> this.mode.getValue() == 2
    );
    public final FloatProperty adaptiveSafeRange = new FloatProperty(
        "adaptive-safe-range", 5.0F, 1.0F, 20.0F, () -> this.mode.getValue() == 2
    );
    public final FloatProperty adaptiveDelay = new FloatProperty(
        "adaptive-delay", 100.0F, 200.0F, 0.0F, 2000.0F, () -> this.mode.getValue() == 2
    );
    public final FloatProperty adaptiveBuildupDuration = new FloatProperty(
        "adaptive-buildup-duration", 600.0F, 0.0F, 3000.0F, () -> this.mode.getValue() == 2
    );
    public final FloatProperty adaptiveBuildupDelay = new FloatProperty(
        "adaptive-cooldown", 500.0F, 0.0F, 5000.0F, () -> this.mode.getValue() == 2
    );
    public final IntProperty stutterHoldDelay = new IntProperty(
        "stutter-hold", 250, 50, 1000, () -> this.mode.getValue() == 3
    );
    public final IntProperty stutterReleaseDelay = new IntProperty(
        "stutter-release", 120, 0, 1000, () -> this.mode.getValue() == 3
    );
    public final BooleanProperty stutterRandomize = new BooleanProperty(
        "stutter-randomize", true, () -> this.mode.getValue() == 3
    );
    public final IntProperty stutterMinDelay = new IntProperty(
        "stutter-min-delay", 150, 50, 500, () -> this.mode.getValue() == 3 && this.stutterRandomize.getValue()
    );
    public final IntProperty stutterMaxDelay = new IntProperty(
        "stutter-max-delay", 400, 50, 1000, () -> this.mode.getValue() == 3 && this.stutterRandomize.getValue()
    );
    public final IntProperty crewMinDelay = new IntProperty(
        "crew-min-delay", 100, 0, 2000, () -> this.mode.getValue() == 4
    );
    public final IntProperty crewMaxDelay = new IntProperty(
        "crew-max-delay", 200, 0, 2000, () -> this.mode.getValue() == 4
    );
    private final LinkedList<FakeLag.QueueData> packetQueue = new LinkedList<>();
    private final LinkedList<FakeLag.PositionData> positions = new LinkedList<>();
    private final LinkedList<FakeLag.PositionData> renderPositions = new LinkedList<>();
    private final Object queueLock = new Object();
    private long resetTimer;
    private boolean wasNearEnemy;
    private boolean ignoreWholeTick;
    private int pauseTicks;
    private AbstractClientPlayer dynamicTarget;
    private long dynamicLastDisableTime = -1L;
    private long dynamicLastStopBlinkTime = -1L;
    private boolean dynamicLastHurt;
    private long dynamicLastStartBlinkTime = -1L;
    private final List<Vec3> positionHistory = new ArrayList<>();
    private boolean lagging;
    private double currentDelay = 0.0;
    private double targetDelay = 0.0;
    private long lastReleaseTime = 0L;
    private EntityPlayer closestPlayer;
    private double closestDistance = Double.MAX_VALUE;
    private boolean stutterHolding = true;
    private long stutterHoldStart;
    private long stutterReleaseStart;
    private long stutterCurrentHoldTime = 0L;
    private int crewCurrentDelay = 0;
    private long crewLastRoll = 0L;
    private final Random crewRandom = new Random();

    public FakeLag() {
        super("FakeLag", false);
    }

    @Override
    public void onEnabled() {
        this.clearPackets();
        this.resetTimer = System.currentTimeMillis();
        this.wasNearEnemy = false;
        this.ignoreWholeTick = false;
        this.pauseTicks = 0;
        this.dynamicTarget = null;
        this.dynamicLastDisableTime = -1L;
        this.dynamicLastStopBlinkTime = -1L;
        this.dynamicLastHurt = false;
        this.dynamicLastStartBlinkTime = -1L;
        this.lagging = false;
        this.currentDelay = 0.0;
        this.targetDelay = 0.0;
        this.lastReleaseTime = 0L;
        this.closestPlayer = null;
        this.closestDistance = Double.MAX_VALUE;
        this.positionHistory.clear();
        this.stutterHolding = true;
        this.stutterReleaseStart = 0L;
        this.stutterHoldStart = System.currentTimeMillis();
        this.stutterCurrentHoldTime = this.nextStutterHoldTime();
        this.crewCurrentDelay = 0;
        this.crewLastRoll = 0L;
    }

    @Override
    public void onDisabled() {
        this.stopDynamicBlink();
        if (this.lagging) {
            this.stopLag(true);
        }

        if (mc.field_71439_g != null) {
            this.blink(true);
        } else {
            this.clearPackets();
        }

        this.positionHistory.clear();
        if (this.mode.getValue() == 4) {
            Miau.lagManager.setDelay(0);
            this.crewCurrentDelay = 0;
            this.crewLastRoll = 0L;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null && !event.isCancelled()) {
            Packet<?> packet = event.getPacket();
            if (this.mode.getValue() == 2) {
                if (event.getType() == EventType.SEND && this.lagging) {
                    event.setCancelled(true);
                    synchronized (this.queueLock) {
                        this.packetQueue
                            .add(new FakeLag.QueueData(packet, System.currentTimeMillis() + (long)this.currentDelay));
                    }
                }
            } else if (this.mode.getValue() == 1) {
                if (event.getType() == EventType.SEND) {
                    this.handleDynamicAttackTarget(packet);
                }
            } else if (this.mode.getValue() == 3) {
                if (event.getType() == EventType.SEND && this.stutterHolding) {
                    event.setCancelled(true);
                    synchronized (this.queueLock) {
                        this.packetQueue.add(new FakeLag.QueueData(packet, System.currentTimeMillis()));
                    }
                }
            } else if (this.mode.getValue() != 4) {
                if (mc.field_71439_g.field_70173_aa >= 10 && this.pauseTicks == 0) {
                    if (mc.field_71439_g.func_71039_bw()) {
                        if (this.blinkOnUsingItem.getValue()) {
                            this.blink(true);
                            return;
                        }

                        if (this.blinkOnEating.getValue()
                            && mc.field_71439_g.func_70694_bm() != null
                            && mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemFood) {
                            this.blink(true);
                            return;
                        }
                    }

                    if (!this.ignoreWholeTick
                        && !this.wasNearEnemy
                        && !mc.field_71439_g.field_70128_L
                        && !event.isCancelled()) {
                        if (!this.isIgnoredPacket(packet)) {
                            if (this.onlyKillAura.getValue() && !this.isKillAuraActive()) {
                                this.blink(true);
                            } else {
                                if (this.onlyKillAura.getValue()) {
                                    EntityLivingBase target = this.getKillAuraTarget();
                                    if (target == null) {
                                        this.blink(true);
                                        return;
                                    }

                                    if (this.onlyWorkWhenInRange.getValue()
                                        && !this.isInRange(
                                            target, this.rangeLimit.getValue(), this.rangeLimit.getSecondValue()
                                        )) {
                                        this.blink(true);
                                        return;
                                    }
                                }

                                if (this.pauseOnNoMove.getValue() && !this.isMoving()) {
                                    this.blink(true);
                                } else if (this.blinkOnHurt.getValue()
                                    && mc.field_71439_g.field_70737_aN != 0
                                    && mc.field_71439_g.func_110143_aJ() < mc.field_71439_g.func_110138_aP()) {
                                    this.blink(true);
                                } else if (this.blinkOnScaffolding.getValue() && this.isScaffolding()) {
                                    this.blink(true);
                                } else if (packet instanceof C02PacketUseEntity) {
                                    C02PacketUseEntity useEntity = (C02PacketUseEntity)packet;
                                    if (useEntity.func_149565_c() == Action.ATTACK) {
                                        if (this.blinkOnAttack.getValue()) {
                                            this.blink(true);
                                        }
                                    } else if (this.blinkOnInteract.getValue()) {
                                        this.blink(true);
                                    }
                                } else if (this.pauseOnChest.getValue() && mc.field_71462_r instanceof GuiContainer) {
                                    this.blink(true);
                                } else if (packet instanceof C0EPacketClickWindow
                                    || packet instanceof C0DPacketCloseWindow) {
                                    this.blink(true);
                                } else if (!(packet instanceof S08PacketPlayerPosLook)
                                    && !(packet instanceof C08PacketPlayerBlockPlacement)
                                    && !(packet instanceof C07PacketPlayerDigging)
                                    && !(packet instanceof C12PacketUpdateSign)
                                    && !(packet instanceof C19PacketResourcePackStatus)) {
                                    if (packet instanceof S12PacketEntityVelocity
                                        && ((S12PacketEntityVelocity)packet).func_149412_c()
                                            == mc.field_71439_g.func_145782_y()) {
                                        if (this.blinkOnKnockBack.getValue()) {
                                            this.blink(true);
                                        }
                                    } else {
                                        if (packet instanceof S27PacketExplosion) {
                                            S27PacketExplosion explosion = (S27PacketExplosion)packet;
                                            if (((IAccessorS27PacketExplosion)explosion).getMotionX() != 0.0F
                                                || ((IAccessorS27PacketExplosion)explosion).getMotionY() != 0.0F
                                                || ((IAccessorS27PacketExplosion)explosion).getMotionZ() != 0.0F) {
                                                if (this.blinkOnKnockBack.getValue()) {
                                                    this.blink(true);
                                                }

                                                return;
                                            }
                                        }

                                        if (this.hasTimePassed(this.resetTimer, this.recoilTime.getValue())) {
                                            if (mc.func_71356_B() || mc.func_147104_D() == null) {
                                                this.blink(true);
                                            } else if (event.getType() == EventType.SEND) {
                                                event.setCancelled(true);
                                                synchronized (this.queueLock) {
                                                    this.packetQueue
                                                        .add(new FakeLag.QueueData(packet, System.currentTimeMillis()));
                                                }

                                                if (packet instanceof C03PacketPlayer) {
                                                    C03PacketPlayer playerPacket = (C03PacketPlayer)packet;
                                                    if (playerPacket.func_149466_j()) {
                                                        FakeLag.PositionData positionData = new FakeLag.PositionData(
                                                            new Vec3(
                                                                playerPacket.func_149464_c(),
                                                                playerPacket.func_149467_d(),
                                                                playerPacket.func_149472_e()
                                                            ),
                                                            System.currentTimeMillis()
                                                        );
                                                        this.positions.add(positionData);
                                                        this.renderPositions.add(positionData);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    if (packet instanceof S08PacketPlayerPosLook && this.pauseOnS08.getValue()) {
                                        this.pauseTicks = this.pauseTick.getValue();
                                    }

                                    this.blink(true);
                                }
                            }
                        }
                    }
                } else {
                    this.blink(true);
                }
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled() && this.mode.getValue() == 2 && event.getTarget() instanceof EntityPlayer) {
            this.stopLag(false);
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        this.stopDynamicBlink();
        if (this.lagging) {
            this.stopLag(true);
        }

        this.blink(false);
        if (this.mode.getValue() == 4) {
            Miau.lagManager.setDelay(0);
            this.crewCurrentDelay = 0;
            this.crewLastRoll = 0L;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()
            && event.getType() == EventType.PRE
            && mc.field_71439_g != null
            && mc.field_71441_e != null) {
            if (this.pauseTicks > 0) {
                this.pauseTicks--;
            }

            if (this.mode.getValue() == 2) {
                this.handleAdaptive();
            } else if (this.mode.getValue() == 1) {
                this.handleDynamic();
            } else if (this.mode.getValue() == 3) {
                this.handleStutter();
            } else if (this.mode.getValue() == 4) {
                this.handleCrew();
            } else {
                this.checkEnemyDistance();
                if (mc.field_71439_g.field_70128_L || mc.field_71439_g.func_71039_bw()) {
                    this.blink(true);
                } else if (this.hasTimePassed(this.resetTimer, this.recoilTime.getValue())) {
                    this.handlePackets(false);
                    this.ignoreWholeTick = false;
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        this.pruneRenderPositions();
        if (this.isEnabled() && this.mode.getValue() == 0 && mc.field_71439_g != null) {
            if (this.line.getValue() && !this.renderPositions.isEmpty()) {
                Color color = new Color(this.lineColor.getValue(), true);
                double renderX = mc.func_175598_ae().field_78730_l;
                double renderY = mc.func_175598_ae().field_78731_m;
                double renderZ = mc.func_175598_ae().field_78728_n;
                GL11.glPushMatrix();
                GL11.glDisable(3553);
                GL11.glBlendFunc(770, 771);
                GL11.glEnable(2848);
                GL11.glEnable(3042);
                GL11.glDisable(2929);
                GL11.glLineWidth(2.0F);
                GL11.glColor4f(
                    color.getRed() / 255.0F,
                    color.getGreen() / 255.0F,
                    color.getBlue() / 255.0F,
                    color.getAlpha() / 255.0F
                );
                GL11.glBegin(3);

                for (FakeLag.PositionData position : this.renderPositions) {
                    GL11.glVertex3d(
                        position.pos.field_72450_a - renderX,
                        position.pos.field_72448_b - renderY,
                        position.pos.field_72449_c - renderZ
                    );
                }

                GL11.glVertex3d(
                    mc.field_71439_g.field_70165_t - renderX,
                    mc.field_71439_g.field_70163_u - renderY,
                    mc.field_71439_g.field_70161_v - renderZ
                );
                GL11.glEnd();
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glEnable(2929);
                GL11.glDisable(2848);
                GL11.glDisable(3042);
                GL11.glEnable(3553);
                GL11.glPopMatrix();
            }

            if (this.renderModel.getValue() && !this.positions.isEmpty() && mc.field_71474_y.field_74320_O != 0) {
                FakeLag.PositionData first = this.positions.getFirst();
                double renderX = mc.func_175598_ae().field_78730_l;
                double renderY = mc.func_175598_ae().field_78731_m;
                double renderZ = mc.func_175598_ae().field_78728_n;
                GL11.glPushMatrix();
                GL11.glPushAttrib(1048575);
                GL11.glColor4f(0.6F, 0.6F, 0.6F, 1.0F);
                mc.func_175598_ae()
                    .func_147939_a(
                        mc.field_71439_g,
                        first.pos.field_72450_a - renderX,
                        first.pos.field_72448_b - renderY,
                        first.pos.field_72449_c - renderZ,
                        mc.field_71439_g.field_70177_z,
                        event.getPartialTicks(),
                        true
                    );
                GL11.glPopAttrib();
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glPopMatrix();
            }
        }
    }

    private void handleAdaptive() {
        this.recordPosition();
        this.findClosestPlayer();
        if (this.closestDistance <= this.adaptiveSafeRange.getValue().floatValue()) {
            if (this.lagging) {
                this.stopLag(true);
            }

            this.handlePackets(false);
        } else if (this.closestDistance > this.adaptiveEnableRange.getValue().floatValue()) {
            if (this.lagging) {
                this.stopLag(false);
            }

            this.handlePackets(false);
        } else {
            if (this.lagging) {
                if (this.serverSidedPositionIsCloser()) {
                    this.stopLag(false);
                } else {
                    this.buildUp();
                }
            } else if ((float)(System.currentTimeMillis() - this.lastReleaseTime)
                >= this.adaptiveBuildupDelay.getValue()) {
                this.startLag();
            }

            this.handlePackets(false);
        }
    }

    private void handleStutter() {
        long now = System.currentTimeMillis();
        if (this.stutterHolding) {
            if (now - this.stutterHoldStart >= this.stutterCurrentHoldTime) {
                this.stutterHolding = false;
                this.stutterReleaseStart = now;
                this.handlePackets(true);
            }
        } else {
            int releaseTime = this.stutterReleaseDelay.getValue();
            if (now - this.stutterReleaseStart >= releaseTime) {
                this.stutterHolding = true;
                this.stutterHoldStart = now;
                this.stutterCurrentHoldTime = this.nextStutterHoldTime();
            }
        }

        this.handlePackets(false);
    }

    private long nextStutterHoldTime() {
        if (!this.stutterRandomize.getValue()) {
            return this.stutterHoldDelay.getValue().intValue();
        }

        int min = this.stutterMinDelay.getValue();
        int max = Math.max(min, this.stutterMaxDelay.getValue());
        return min + (long)(Math.random() * (max - min + 1));
    }

    private void handleCrew() {
        long now = System.currentTimeMillis();
        if (now - this.crewLastRoll >= this.crewCurrentDelay || this.crewCurrentDelay == 0) {
            int min = Math.min(this.crewMinDelay.getValue(), this.crewMaxDelay.getValue());
            int max = Math.max(this.crewMinDelay.getValue(), this.crewMaxDelay.getValue());
            this.crewCurrentDelay = max <= min ? min : min + this.crewRandom.nextInt(max - min + 1);
            this.crewLastRoll = now;
        }

        Miau.lagManager.setDelay(this.crewCurrentDelay / 50);
    }

    private void recordPosition() {
        this.positionHistory
            .add(
                0,
                new Vec3(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v)
            );

        while (this.positionHistory.size() > 400) {
            this.positionHistory.remove(this.positionHistory.size() - 1);
        }
    }

    private void findClosestPlayer() {
        this.closestPlayer = null;
        this.closestDistance = Double.MAX_VALUE;
        if (mc.field_71441_e != null && mc.field_71439_g != null) {
            double maxDistance = Math.ceil(this.adaptiveEnableRange.getValue().floatValue());

            for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                if (player != mc.field_71439_g && !player.field_70128_L) {
                    double distance = mc.field_71439_g.func_70032_d(player);
                    if (distance <= maxDistance && distance < this.closestDistance) {
                        this.closestDistance = distance;
                        this.closestPlayer = player;
                    }
                }
            }
        }
    }

    private boolean serverSidedPositionIsCloser() {
        if (this.closestPlayer != null && !this.positionHistory.isEmpty()) {
            int ticksAgo = (int)(this.getPing() / 50 + this.currentDelay / 20.0);
            ticksAgo = Math.max(0, Math.min(ticksAgo, this.positionHistory.size() - 1));
            Vec3 serverSided = this.positionHistory.get(ticksAgo);
            Vec3 enemyPos = new Vec3(
                this.closestPlayer.field_70165_t, this.closestPlayer.field_70163_u, this.closestPlayer.field_70161_v
            );
            double serverSidedDistance = serverSided.func_72438_d(enemyPos);
            return serverSidedDistance < this.closestDistance;
        } else {
            return false;
        }
    }

    private int getPing() {
        if (mc.field_71439_g != null && mc.func_147114_u() != null) {
            NetworkPlayerInfo playerInfo = mc.func_147114_u().func_175102_a(mc.field_71439_g.func_110124_au());
            return playerInfo != null ? playerInfo.func_178853_c() : 0;
        } else {
            return 0;
        }
    }

    private void startLag() {
        if (!(this.closestDistance <= this.adaptiveSafeRange.getValue().floatValue())) {
            this.lagging = true;
            this.currentDelay = 0.0;
            float min = this.adaptiveDelay.getValue();
            float max = this.adaptiveDelay.getSecondValue();
            this.targetDelay = min + Math.random() * (max - min);
        }
    }

    private void buildUp() {
        if (!(this.currentDelay >= this.targetDelay)) {
            double increment = this.adaptiveBuildupDuration.getValue() <= 0.0F
                ? this.targetDelay
                : this.targetDelay * 50.0 / this.adaptiveBuildupDuration.getValue().floatValue();
            this.currentDelay = Math.min(this.targetDelay, this.currentDelay + increment);
        }
    }

    private void stopLag(boolean emergency) {
        if (this.lagging) {
            this.lagging = false;
            this.lastReleaseTime = System.currentTimeMillis();
            this.handlePackets(true);
        }
    }

    private void handleDynamicAttackTarget(Packet<?> packet) {
        if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity useEntity = (C02PacketUseEntity)packet;
            if (useEntity.func_149565_c() == Action.ATTACK) {
                Entity entity = useEntity.func_149564_a(mc.field_71441_e);
                if (entity instanceof AbstractClientPlayer) {
                    if (this.dynamicIgnoreTeammates.getValue() && TeamUtil.isSameTeam((EntityPlayer)entity)) {
                        return;
                    }

                    this.dynamicTarget = (AbstractClientPlayer)entity;
                }
            }
        }
    }

    private void handleDynamic() {
        boolean blinking = this.isDynamicBlinking();
        long now = System.currentTimeMillis();
        if (this.dynamicStopOnHurt.getValue()
            && this.dynamicLastDisableTime > 0L
            && now - this.dynamicLastDisableTime <= this.dynamicStopOnHurtTime.getValue().intValue()) {
            if (blinking) {
                this.dynamicMessage("stop lag: hurt cooldown.");
                this.stopDynamicBlink();
                blinking = false;
            }

            this.dynamicLastHurt = mc.field_71439_g.field_70737_aN > 0;
        } else {
            if (blinking) {
                if (now - this.dynamicLastStartBlinkTime >= this.dynamicDelay.getValue().intValue()) {
                    this.dynamicMessage("stop lag: time out.");
                    this.stopDynamicBlink();
                    blinking = false;
                } else if (!this.dynamicLastHurt
                    && mc.field_71439_g.field_70737_aN > 0
                    && this.dynamicStopOnHurt.getValue()) {
                    this.dynamicMessage("stop lag: hurt.");
                    this.dynamicLastDisableTime = now;
                    this.stopDynamicBlink();
                    blinking = false;
                }
            }

            if (!this.isValidDynamicTarget(this.dynamicTarget)) {
                if (this.dynamicTarget != null) {
                    this.dynamicMessage("release target: invalid.");
                    this.dynamicTarget = null;
                }

                this.stopDynamicBlink();
                this.dynamicLastHurt = mc.field_71439_g.field_70737_aN > 0;
            } else {
                double distance = mc.field_71439_g.func_70032_d(this.dynamicTarget);
                float startRange = Math.max(this.dynamicStartRange.getValue(), this.dynamicStopRange.getValue());
                float stopRange = Math.min(this.dynamicStartRange.getValue(), this.dynamicStopRange.getValue());
                if (distance > this.dynamicMaxTargetRange.getValue().floatValue()) {
                    this.dynamicMessage("release target: " + this.dynamicTarget.func_70005_c_());
                    this.dynamicTarget = null;
                    this.stopDynamicBlink();
                } else if (blinking && distance <= stopRange) {
                    this.dynamicMessage("stop lag: too close.");
                    this.stopDynamicBlink();
                } else if (blinking && distance >= startRange) {
                    this.dynamicMessage("stop lag: out of range.");
                    this.stopDynamicBlink();
                } else if (!blinking
                    && distance > stopRange
                    && distance < startRange
                    && now - this.dynamicLastStopBlinkTime >= this.dynamicCooldown.getValue().intValue()) {
                    this.dynamicMessage("start lag: in range.");
                    this.dynamicLastStartBlinkTime = now;
                    this.startDynamicBlink();
                }

                this.dynamicLastHurt = mc.field_71439_g.field_70737_aN > 0;
            }
        }
    }

    private boolean isValidDynamicTarget(AbstractClientPlayer target) {
        return target != null
            && !target.field_70128_L
            && target.func_110143_aJ() > 0.0F
            && mc.field_71441_e != null
            && mc.field_71441_e.field_72996_f.contains(target)
            && (!this.dynamicIgnoreTeammates.getValue() || !TeamUtil.isSameTeam(target));
    }

    private boolean isDynamicBlinking() {
        return Miau.blinkManager.getBlinkingModule() == BlinkModules.FAKE_LAG;
    }

    private void startDynamicBlink() {
        if (!this.isDynamicBlinking()) {
            Miau.blinkManager.setBlinkState(false, Miau.blinkManager.getBlinkingModule());
            Miau.blinkManager.setBlinkState(true, BlinkModules.FAKE_LAG);
        }
    }

    private void stopDynamicBlink() {
        if (Miau.blinkManager.setBlinkState(false, BlinkModules.FAKE_LAG)) {
            this.dynamicLastStopBlinkTime = System.currentTimeMillis();
        }
    }

    private void dynamicMessage(String message) {
        if (this.dynamicDebug.getValue()) {
            ChatUtil.display(Miau.clientName + this.getName() + ": &7" + message);
        }
    }

    private boolean isIgnoredPacket(Packet<?> packet) {
        return packet instanceof C00Handshake
            || packet instanceof C00PacketServerQuery
            || packet instanceof C01PacketPing
            || packet instanceof C01PacketChatMessage
            || packet instanceof S01PacketPong;
    }

    private void checkEnemyDistance() {
        if (!(this.allowedDistToEnemy.getSecondValue() <= 0.0F) && !this.positions.isEmpty()) {
            Vec3 serverPos = this.positions.getFirst().pos;
            float min = this.allowedDistToEnemy.getValue();
            float max = this.allowedDistToEnemy.getSecondValue();
            this.wasNearEnemy = false;

            for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                if (player != mc.field_71439_g && !player.field_70128_L) {
                    double distance = player.func_70011_f(
                        serverPos.field_72450_a, serverPos.field_72448_b, serverPos.field_72449_c
                    );
                    if (distance >= min && distance <= max) {
                        this.blink(true);
                        this.wasNearEnemy = true;
                        return;
                    }
                }
            }
        } else {
            this.wasNearEnemy = false;
        }
    }

    private void blink(boolean handlePackets) {
        mc.func_152344_a(() -> {
            if (handlePackets) {
                this.resetTimer = System.currentTimeMillis();
            }

            this.handlePackets(true);
            this.ignoreWholeTick = true;
        });
    }

    private void handlePackets(boolean clear) {
        long now = System.currentTimeMillis();
        int mode = this.mode.getValue();
        synchronized (this.queueLock) {
            Iterator<FakeLag.QueueData> packetIterator = this.packetQueue.iterator();

            while (packetIterator.hasNext()) {
                FakeLag.QueueData data = packetIterator.next();
                boolean shouldRelease = false;
                if (clear) {
                    shouldRelease = true;
                } else if (mode == 2) {
                    shouldRelease = data.time <= now;
                } else if (mode == 3) {
                    shouldRelease = false;
                } else {
                    shouldRelease = data.time <= now - this.delay.getValue().intValue();
                }

                if (shouldRelease) {
                    PacketUtil.sendPacketNoEvent(data.packet);
                    packetIterator.remove();
                }
            }

            Iterator<FakeLag.PositionData> positionIterator = this.positions.iterator();

            while (positionIterator.hasNext()) {
                FakeLag.PositionData data = positionIterator.next();
                boolean shouldRelease = false;
                if (clear) {
                    shouldRelease = true;
                } else if (mode == 2) {
                    shouldRelease = data.time <= now;
                } else if (mode == 3) {
                    shouldRelease = false;
                } else {
                    shouldRelease = data.time <= now - this.delay.getValue().intValue();
                }

                if (shouldRelease) {
                    positionIterator.remove();
                }
            }
        }

        this.pruneRenderPositions();
    }

    private void pruneRenderPositions() {
        long now = System.currentTimeMillis();
        long keepTime = Math.max(this.delay.getValue(), this.recoilTime.getValue()) + 1000L;
        Iterator<FakeLag.PositionData> renderIterator = this.renderPositions.iterator();

        while (renderIterator.hasNext()) {
            FakeLag.PositionData data = renderIterator.next();
            if (data.time <= now - keepTime) {
                renderIterator.remove();
            }
        }
    }

    private void clearPackets() {
        synchronized (this.queueLock) {
            this.packetQueue.clear();
            this.positions.clear();
            this.renderPositions.clear();
        }
    }

    private boolean hasTimePassed(long timer, int delay) {
        return System.currentTimeMillis() - timer >= delay;
    }

    private boolean isMoving() {
        return mc.field_71439_g != null
            && (mc.field_71439_g.field_70701_bs != 0.0F || mc.field_71439_g.field_70702_br != 0.0F);
    }

    private boolean isScaffolding() {
        Scaffold scaffold = (Scaffold)Miau.moduleManager.modules.get(Scaffold.class);
        return scaffold != null && scaffold.isEnabled();
    }

    private boolean isKillAuraActive() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        return killAura != null && killAura.isEnabled();
    }

    private EntityLivingBase getKillAuraTarget() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        return killAura != null ? killAura.getTarget() : null;
    }

    private boolean isInRange(EntityLivingBase target, float min, float max) {
        double distance = mc.field_71439_g.func_70032_d(target);
        return distance >= min && distance <= max;
    }

    public Vec3 getServerPositionForDebug() {
        if (!this.isEnabled()) {
            return null;
        }

        if (!this.positions.isEmpty()) {
            return this.positions.getFirst().pos;
        }

        this.pruneRenderPositions();
        return this.renderPositions.isEmpty() ? null : this.renderPositions.getFirst().pos;
    }

    @Override
    public String[] getSuffix() {
        if (this.mode.getValue() == 4) {
            return new String[]{this.crewMinDelay.getValue() + "-" + this.crewMaxDelay.getValue() + "ms"};
        } else if (this.mode.getValue() == 1) {
            return new String[]{this.mode.getModeString()};
        } else if (this.tagMode.getValue() == 0) {
            return new String[]{this.mode.getModeString() + " " + this.packetQueue.size()};
        } else {
            return this.tagMode.getValue() == 1
                ? new String[]{this.customTagText.getValue()}
                : new String[]{this.mode.getModeString()};
        }
    }

    private static class PositionData {
        private final Vec3 pos;
        private final long time;

        private PositionData(Vec3 pos, long time) {
            this.pos = pos;
            this.time = time;
        }
    }

    private static class QueueData {
        private final Packet<?> packet;
        private final long time;

        private QueueData(Packet<?> packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }
}
