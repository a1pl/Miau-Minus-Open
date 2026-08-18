package miau.module.modules.combat;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.module.modules.movement.Blink;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.TextProperty;
import miau.util.client.ChatUtil;
import miau.util.misc.BackTrackUtil;
import miau.util.misc.SomeUtil;
import miau.util.network.BlinkUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.util.Vec3;

public class SmartBlinker extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final ModeProperty tagMode = new ModeProperty(
        "TagMode", 0, new String[]{"Normal", "MaxTime", "Custom", "PacketCount"}
    );
    private final TextProperty customTag = new TextProperty("CustomTag", "");
    private final FloatProperty range = new FloatProperty("Range", 2.0F, 4.0F, 0.0F, 6.0F);
    private final BooleanProperty limitBlinkTime = new BooleanProperty("BlinkTime", true);
    private final BooleanProperty limitMoveRange = new BooleanProperty("MoveRange", false);
    private final IntProperty maxBlinkTime = new IntProperty(
        "MaxBlinkTime", 500, 0, 5000, () -> this.limitBlinkTime.getValue()
    );
    private final FloatProperty maxMoveRangePerBlink = new FloatProperty(
        "MaxMoveRangePerBlink", 5.0F, 0.0F, 50.0F, () -> this.limitMoveRange.getValue()
    );
    private final IntProperty minDelayBetweenCancelBlink = new IntProperty("MinDelayBetweenPerCancelBlink", 0, 0, 5000);
    private final IntProperty delay = new IntProperty("Delay", 1000, 0, 5000);
    private final BooleanProperty stopOnAttack = new BooleanProperty("StopOnAttack", true);
    private final BooleanProperty stopOnPlaceBlock = new BooleanProperty("StopOnPlaceBlock", false);
    private final BooleanProperty stopOnHurt = new BooleanProperty("StopOnHurt", false);
    private final BooleanProperty stopOnLag = new BooleanProperty("StopOnServerTP", true);
    private final BooleanProperty blockAllPackets = new BooleanProperty("BlockAllPackets", false);
    private final BooleanProperty tips = new BooleanProperty("Tips", true);
    private final BooleanProperty debugger = new BooleanProperty("Debugger", false);
    private final TimerUtil delayTimer = new TimerUtil();
    private final TimerUtil delayTimer2 = new TimerUtil();
    private EntityLivingBase bufferTarget = null;
    private boolean isBlinking = false;
    private boolean lastBlinkState = false;
    private long blinkStartTime = 0L;
    private Vec3 lastPlayerPos = null;
    private float totalMoveDistance = 0.0F;
    private int actualDelay = 0;

    public SmartBlinker() {
        super("SmartBlinker", false);
    }

    @EventTarget
    public void onGameLoop(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null) {
                if (!this.isBlinking) {
                    this.actualDelay = this.delay.getValue();
                }

                KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
                if (killAura != null && killAura.getTarget() instanceof EntityLivingBase) {
                    this.bufferTarget = killAura.getTarget();
                }

                Blink blink = (Blink)Miau.moduleManager.modules.get(Blink.class);
                if (this.isBlinking && blink != null && blink.isEnabled()) {
                    blink.toggle();
                    ChatUtil.display("Don't enable your Blink Module When This Module Is Working!");
                }

                if (this.isBlinking && this.limitMoveRange.getValue()) {
                    Vec3 currentPos = new Vec3(
                        mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v
                    );
                    if (this.lastPlayerPos != null) {
                        float segmentDistance = (float)this.lastPlayerPos.func_72438_d(currentPos);
                        this.totalMoveDistance += segmentDistance;
                        if (this.totalMoveDistance >= this.maxMoveRangePerBlink.getValue()) {
                            this.debugMessage(
                                String.format("MaxMoveRange reached (%.3f blocks), stopping...", this.totalMoveDistance)
                            );
                            this.reset();
                            return;
                        }
                    }

                    this.lastPlayerPos = currentPos;
                }

                if (this.isBlinking
                    && this.blinkStartTime > 0L
                    && this.limitBlinkTime.getValue()
                    && System.currentTimeMillis() - this.blinkStartTime >= this.maxBlinkTime.getValue().intValue()) {
                    this.debugMessage(
                        String.format(
                            "MaxBlinkTime reached, stopping... Duration: %dms",
                            System.currentTimeMillis() - this.blinkStartTime
                        )
                    );
                    this.reset();
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        Packet<?> packet = event.getPacket();
        if ((
                this.stopOnAttack.getValue() && isAttackPacket(packet)
                    || this.stopOnPlaceBlock.getValue() && isPlaceBlockPacket(packet)
                    || this.stopOnLag.getValue() && isServerLagPacket(packet)
            )
            && this.isBlinking) {
            this.debugMessage(
                String.format(
                    "StopWorking, DuringTime:%dms, MoveDistance:%.3f blocks",
                    System.currentTimeMillis() - this.blinkStartTime,
                    this.totalMoveDistance
                )
            );
            this.reset();
        } else {
            if (this.shouldBlink()) {
                if (!this.isBlinking) {
                    Vec3 startPos = new Vec3(
                        mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v
                    );
                    this.lastPlayerPos = startPos;
                    this.totalMoveDistance = 0.0F;
                    this.debugMessage(
                        String.format(
                            "Blink started at position: (%.2f, %.2f, %.2f)",
                            startPos.field_72450_a,
                            startPos.field_72448_b,
                            startPos.field_72449_c
                        )
                    );
                }

                BlinkUtil.blink(
                    event,
                    true,
                    this.blockAllPackets.getValue(),
                    p -> p instanceof S14PacketEntity,
                    Integer.MAX_VALUE,
                    null
                );
                if (!this.lastBlinkState) {
                    this.debugMessage("StartWorking");
                    this.blinkStartTime = System.currentTimeMillis();
                }

                this.isBlinking = true;
                this.lastBlinkState = true;
            } else if (!this.shouldBlink() && this.isBlinking) {
                long duration = System.currentTimeMillis() - this.blinkStartTime;
                this.debugMessage(
                    String.format(
                        "StopWorking, DuringTime:%dms, TotalMoveDistance:%.3f blocks", duration, this.totalMoveDistance
                    )
                );
                this.reset();
            }
        }
    }

    private boolean shouldBlink() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        boolean killAuraIsWorking = killAura != null && killAura.isEnabled() && killAura.getTarget() != null;
        if (this.stopOnHurt.getValue() && SomeUtil.isHurting()) {
            return false;
        }

        if (!killAuraIsWorking) {
            return false;
        }

        if (this.bufferTarget == null) {
            return false;
        }

        if (!this.delayTimer.hasTimeElapsed(this.actualDelay)) {
            return false;
        }

        double distance = BackTrackUtil.getDistanceToEntityBox(this.bufferTarget);
        boolean withinRange = distance >= this.range.getValue().floatValue()
            && distance <= this.range.getSecondValue().floatValue();
        if (!this.isBlinking) {
            return withinRange;
        }

        boolean continueBlink = withinRange;
        if (this.limitBlinkTime.getValue()) {
            continueBlink = continueBlink
                && this.blinkStartTime > 0L
                && System.currentTimeMillis() - this.blinkStartTime < this.maxBlinkTime.getValue().intValue();
        }

        if (this.limitMoveRange.getValue()) {
            continueBlink = continueBlink && this.totalMoveDistance < this.maxMoveRangePerBlink.getValue();
        }

        return continueBlink;
    }

    private void debugMessage(String msg) {
        if (this.debugger.getValue()) {
            ChatUtil.display(msg);
        }
    }

    private void reset() {
        if (this.delayTimer2.hasTimeElapsed(this.minDelayBetweenCancelBlink.getValue().intValue())) {
            this.delayTimer.reset();
            this.delayTimer2.reset();
            BlinkUtil.unblink();
            this.isBlinking = false;
            this.blinkStartTime = 0L;
            this.lastBlinkState = false;
            this.lastPlayerPos = null;
            this.totalMoveDistance = 0.0F;
        }
    }

    @Override
    public void onEnabled() {
        if (this.tips.getValue()) {
            ChatUtil.display(
                "If you open this module, when module is working, the blink module will be automatically disabled"
            );
        }
    }

    @Override
    public String[] getSuffix() {
        String tagModeString = this.tagMode.getModeString();
        switch (tagModeString) {
            case "Normal":
                return new String[]{String.format("%s - %s", this.range.getValue(), this.range.getSecondValue())};
            case "MaxTime":
                return new String[]{this.maxBlinkTime.getValue() + "ms"};
            case "Custom":
                return new String[]{this.customTag.getValue()};
            case "PacketCount":
                return new String[]{String.valueOf(BlinkUtil.getPacketCount())};
            default:
                return new String[0];
        }
    }

    private static boolean isAttackPacket(Packet<?> packet) {
        return packet instanceof C02PacketUseEntity && ((C02PacketUseEntity)packet).func_149565_c() == Action.ATTACK;
    }

    private static boolean isPlaceBlockPacket(Packet<?> packet) {
        return packet instanceof C08PacketPlayerBlockPlacement;
    }

    private static boolean isServerLagPacket(Packet<?> packet) {
        return packet instanceof S08PacketPlayerPosLook;
    }
}
