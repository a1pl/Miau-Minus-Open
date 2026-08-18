package miau.module.modules.combat;

import java.util.concurrent.ConcurrentLinkedDeque;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ItemListProperty;
import miau.util.network.PacketUtil;
import miau.util.player.CombatTargeting;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import org.lwjgl.input.Mouse;

public class KnockbackDelay extends Module {
    public final FloatProperty distanceToTarget = new FloatProperty("Distance-to-target", 6.0F, 3.0F, 12.0F);
    public final FloatProperty chance = new FloatProperty("Chance", 100.0F, 0.0F, 100.0F);
    public final FloatProperty minimumDelay = new FloatProperty("Minimum-delay", 100.0F, 0.0F, 1000.0F);
    public final FloatProperty maximumDelay = new FloatProperty("Maximum-delay", 200.0F, 50.0F, 1000.0F);
    public final BooleanProperty inAir = new BooleanProperty("In-air", true);
    public final BooleanProperty lookingAtPlayer = new BooleanProperty("Looking-at-player", false);
    public final BooleanProperty requireLeftMouse = new BooleanProperty("Require-Left-mouse", false);
    public final BooleanProperty onlyWhitelistedItem = new BooleanProperty("Restrict-held-item", false);
    public final ItemListProperty whitelistedItems = new ItemListProperty("Whitelisted-items", "");
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final ConcurrentLinkedDeque<Packet<?>> delayedPackets = new ConcurrentLinkedDeque<>();
    private long lagStartTime = -1L;
    private long targetDelay = 0L;

    public KnockbackDelay() {
        super("KnockbackDelay", false);
    }

    @Override
    public void onEnabled() {
        this.delayedPackets.clear();
        this.lagStartTime = -1L;
    }

    @Override
    public void onDisabled() {
        this.flushDelayedPackets();
    }

    @EventTarget(0)
    public void onReceivePacketHigh(PacketEvent e) {
        if (this.isEnabled()) {
            if (e.getType() == EventType.RECEIVE) {
                if (e.getPacket() instanceof S08PacketPlayerPosLook) {
                    this.flushDelayedPackets();
                } else if (this.lagStartTime != -1L) {
                    e.setCancelled(true);
                    this.delayedPackets.addLast(e.getPacket());
                } else if (e.getPacket() instanceof S12PacketEntityVelocity) {
                    if (mc.field_71439_g != null && mc.field_71441_e != null) {
                        S12PacketEntityVelocity packet = (S12PacketEntityVelocity)e.getPacket();
                        if (packet.func_149412_c() == mc.field_71439_g.func_145782_y()) {
                            if (this.conditionsFailureReason() == null) {
                                if (!(this.chance.getValue().floatValue() < 100.0)
                                    || !(Math.random() * 100.0 >= this.chance.getValue().floatValue())) {
                                    e.setCancelled(true);
                                    this.delayedPackets.addLast(e.getPacket());
                                    this.lagStartTime = System.currentTimeMillis();
                                    long minD = this.minimumDelay.getValue().longValue();
                                    long maxD = this.maximumDelay.getValue().longValue();
                                    if (minD > maxD) {
                                        minD = maxD;
                                    }

                                    this.targetDelay = minD + (long)(Math.random() * (maxD - minD + 1L));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget(4)
    public void onGameTick(TickEvent e) {
        if (this.isEnabled()) {
            if (e.getType() == EventType.PRE) {
                if (mc.field_71439_g == null || mc.field_71441_e == null || mc.field_71439_g.field_70128_L) {
                    this.flushDelayedPackets();
                } else if (this.lagStartTime != -1L) {
                    if (this.conditionsFailureReason() != null) {
                        this.flushDelayedPackets();
                    } else {
                        long nowMs = System.currentTimeMillis();
                        if (nowMs - this.lagStartTime >= this.targetDelay) {
                            this.flushDelayedPackets();
                        }
                    }
                }
            }
        }
    }

    private void flushDelayedPackets() {
        this.lagStartTime = -1L;
        if (mc.field_71439_g != null && mc.func_147114_u() != null) {
            while (!this.delayedPackets.isEmpty()) {
                Packet<?> packet = this.delayedPackets.pollFirst();
                if (packet != null) {
                    PacketUtil.handlePacket((Packet<INetHandlerPlayClient>)packet);
                }
            }
        }

        this.delayedPackets.clear();
    }

    private String conditionsFailureReason() {
        double maxRange = this.distanceToTarget.getValue().floatValue();
        if (CombatTargeting.getTarget(
                true, false, false, true, false, true, maxRange, CombatTargeting.SortMode.DISTANCE
            )
            == null) {
            return "no target in range";
        }

        if (this.inAir.getValue() && mc.field_71439_g.field_70122_E) {
            return "not in air";
        }

        if (this.lookingAtPlayer.getValue()
            && CombatTargeting.getTarget(
                    true, false, false, true, false, true, maxRange, CombatTargeting.SortMode.CROSSHAIR
                )
                == null) {
            return "not looking at player";
        }

        if (this.requireLeftMouse.getValue() && !Mouse.isButtonDown(0)) {
            return "LMB not held";
        }

        if (this.onlyWhitelistedItem.getValue()) {
            ItemStack held = mc.field_71439_g.func_70694_bm();
            if (held == null || !this.whitelistedItems.matches(held)) {
                return "held item not whitelisted";
            }
        }

        return null;
    }
}
