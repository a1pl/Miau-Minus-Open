package miau.module.modules.movement;

import com.google.common.base.CaseFormat;
import miau.Miau;
import miau.enums.BlinkModules;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorC03PacketPlayer;
import miau.mixin.IAccessorMinecraft;
import miau.module.Module;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.client.ChatUtil;
import miau.util.network.PacketUtil;
import miau.util.network.ServerUtil;
import miau.util.player.PlayerUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class NoFall extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final TimerUtil packetDelayTimer = new TimerUtil();
    private final TimerUtil scoreboardResetTimer = new TimerUtil();
    private boolean slowFalling = false;
    private boolean lastOnGround = false;
    private int lastMlgSlot = -1;
    private boolean mlgPlaced = false;
    public final ModeProperty mode = new ModeProperty(
        "mode", 0, new String[]{"PACKET", "BLINK", "NO_GROUND", "SPOOF", "LEGIT", "VULCAN", "TIMER"}
    );
    public final FloatProperty distance = new FloatProperty("distance", 3.0F, 0.0F, 20.0F);
    public final IntProperty delay = new IntProperty("delay", 0, 0, 10000);
    private final TimerUtil timerTickTimer = new TimerUtil();
    private int timerTicks = 0;
    private int timerSettick = 0;
    private double timerRepdist = 0.0;

    public boolean canTrigger() {
        return this.scoreboardResetTimer.hasTimeElapsed(3000L)
            && this.packetDelayTimer.hasTimeElapsed(this.delay.getValue().longValue());
    }

    public NoFall() {
        super("NoFall", false);
    }

    @EventTarget(1)
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && event.getPacket() instanceof S08PacketPlayerPosLook) {
            this.onDisabled();
        } else if (this.isEnabled()
            && event.getType() == EventType.SEND
            && !event.isCancelled()
            && event.getPacket() instanceof C03PacketPlayer) {
            C03PacketPlayer packet = (C03PacketPlayer)event.getPacket();
            switch (this.mode.getValue()) {
                case 0:
                    if (this.slowFalling) {
                        this.slowFalling = false;
                        ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
                    } else if (!packet.func_149465_i()) {
                        AxisAlignedBB aabb = mc.field_71439_g.func_174813_aQ().func_72314_b(2.0, 0.0, 2.0);
                        if (PlayerUtil.canFly(this.distance.getValue())
                            && !PlayerUtil.checkInWater(aabb)
                            && this.canTrigger()) {
                            this.packetDelayTimer.reset();
                            this.slowFalling = true;
                            ((IAccessorMinecraft)mc).getTimer().field_74278_d = 0.5F;
                        }
                    }
                    break;
                case 1:
                    boolean allowed = !mc.field_71439_g.func_70617_f_()
                        && !mc.field_71439_g.field_71075_bZ.field_75101_c
                        && mc.field_71439_g.field_70737_aN == 0;
                    if (Miau.blinkManager.getBlinkingModule() != BlinkModules.NO_FALL) {
                        if (this.lastOnGround
                            && !packet.func_149465_i()
                            && allowed
                            && PlayerUtil.canFly(this.distance.getValue().intValue())
                            && mc.field_71439_g.field_70181_x < 0.0) {
                            Miau.blinkManager.setBlinkState(false, Miau.blinkManager.getBlinkingModule());
                            Miau.blinkManager.setBlinkState(true, BlinkModules.NO_FALL);
                        }
                    } else if (!allowed) {
                        Miau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
                        ChatUtil.display("%s%s: &cFailed player check!&r", this.getName());
                    } else if (PlayerUtil.checkInWater(mc.field_71439_g.func_174813_aQ().func_72314_b(2.0, 0.0, 2.0))) {
                        Miau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
                        ChatUtil.display("%s%s: &cFailed void check!&r", this.getName());
                    } else if (packet.func_149465_i()) {
                        for (Packet<?> blinkedPacket : Miau.blinkManager.blinkedPackets) {
                            if (blinkedPacket instanceof C03PacketPlayer) {
                                ((IAccessorC03PacketPlayer)blinkedPacket).setOnGround(true);
                            }
                        }

                        Miau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
                        this.packetDelayTimer.reset();
                    }

                    this.lastOnGround = packet.func_149465_i() && allowed && this.canTrigger();
                    break;
                case 2:
                    ((IAccessorC03PacketPlayer)packet).setOnGround(false);
                    break;
                case 3:
                    if (!packet.func_149465_i()) {
                        AxisAlignedBB aabb = mc.field_71439_g.func_174813_aQ().func_72314_b(2.0, 0.0, 2.0);
                        if (PlayerUtil.canFly(this.distance.getValue())
                            && !PlayerUtil.checkInWater(aabb)
                            && this.canTrigger()) {
                            this.packetDelayTimer.reset();
                            ((IAccessorC03PacketPlayer)packet).setOnGround(true);
                            mc.field_71439_g.field_70143_R = 0.0F;
                        }
                    }
                case 5:
                    if (!packet.func_149465_i() && mc.field_71439_g.field_70143_R > 7.0F && this.canTrigger()) {
                        this.packetDelayTimer.reset();
                        ((IAccessorC03PacketPlayer)packet).setOnGround(true);
                        mc.field_71439_g.field_70143_R = 0.0F;
                        mc.field_71439_g.field_70181_x = 0.0;
                    }
                case 4:
                default:
                    break;
                case 6:
                    float fallDist = mc.field_71439_g.field_70143_R;
                    if (fallDist >= 1.51F && this.timerRepdist == 0.0 && !this.isOverVoid()) {
                        ((IAccessorMinecraft)mc).getTimer().field_74278_d = 0.45F;
                        PacketUtil.sendPacketNoEvent(new C03PacketPlayer(true));
                        this.timerSettick = this.timerTicks + 1;
                        this.timerRepdist = fallDist;
                    }

                    if (fallDist - this.timerRepdist >= 1.51F && !this.isOverVoid()) {
                        ((IAccessorMinecraft)mc).getTimer().field_74278_d = 0.6F;
                        PacketUtil.sendPacketNoEvent(new C03PacketPlayer(true));
                        this.timerSettick = this.timerTicks + 5;
                        this.timerRepdist = fallDist;
                    }

                    if (mc.field_71439_g.field_70122_E) {
                        ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
                        this.timerRepdist = 0.0;
                    }
            }
        }
    }

    @EventTarget(0)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            this.timerTicks++;
            if (ServerUtil.hasPlayerCountInfo()) {
                this.scoreboardResetTimer.reset();
            }

            if (this.mode.getValue() == 0 && this.slowFalling) {
                PacketUtil.sendPacketNoEvent(new C03PacketPlayer(true));
                mc.field_71439_g.field_70143_R = 0.0F;
            }

            if (this.mode.getValue() == 4) {
                this.handleLegitMlg();
            }

            if (this.mode.getValue() == 6) {
                if (this.timerTicks == this.timerSettick) {
                    ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
                }

                if (this.timerTicks >= this.timerSettick) {
                    this.timerSettick = 0;
                }
            }
        }
    }

    private boolean isOverVoid() {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            int x = MathHelper.func_76128_c(mc.field_71439_g.field_70165_t);
            int z = MathHelper.func_76128_c(mc.field_71439_g.field_70161_v);

            for (int y = MathHelper.func_76128_c(mc.field_71439_g.field_70163_u); y > -1; y--) {
                if (!mc.field_71441_e.func_175623_d(new BlockPos(x, y, z))) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    private void handleLegitMlg() {
        if (mc.field_71439_g != null && mc.field_71441_e != null && mc.field_71442_b != null) {
            if (!mc.field_71439_g.field_70122_E
                && !mc.field_71439_g.field_71075_bZ.field_75100_b
                && !mc.field_71439_g.func_70090_H()
                && !mc.field_71439_g.func_70617_f_()) {
                if (!(mc.field_71439_g.field_70143_R < this.distance.getValue())
                    && !(mc.field_71439_g.field_70181_x >= -0.1)) {
                    int waterSlot = this.findWaterBucketSlot();
                    if (waterSlot != -1) {
                        BlockPos target = this.findMlgTarget();
                        if (target != null) {
                            if (this.lastMlgSlot == -1) {
                                this.lastMlgSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                            }

                            mc.field_71439_g.field_71071_by.field_70461_c = waterSlot;
                            mc.field_71442_b.func_78765_e();
                            mc.field_71439_g.field_70125_A = 90.0F;
                            if (!this.mlgPlaced
                                && mc.field_71439_g
                                        .func_70011_f(
                                            target.func_177958_n() + 0.5,
                                            target.func_177956_o() + 0.5,
                                            target.func_177952_p() + 0.5
                                        )
                                    <= mc.field_71442_b.func_78757_d() + 1.5F) {
                                Vec3 hitVec = new Vec3(
                                    target.func_177958_n() + 0.5,
                                    target.func_177956_o() + 1.0,
                                    target.func_177952_p() + 0.5
                                );
                                ItemStack stack = mc.field_71439_g.field_71071_by.func_70448_g();
                                if (stack != null
                                    && mc.field_71442_b
                                        .func_178890_a(
                                            mc.field_71439_g, mc.field_71441_e, stack, target, EnumFacing.UP, hitVec
                                        )) {
                                    mc.field_71439_g.func_71038_i();
                                    this.mlgPlaced = true;
                                }
                            }
                        }
                    }
                }
            } else {
                this.resetLegitMlg();
            }
        }
    }

    private int findWaterBucketSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.field_70462_a[i];
            if (stack != null && stack.func_77973_b() == Items.field_151131_as) {
                return i;
            }
        }

        return -1;
    }

    private BlockPos findMlgTarget() {
        BlockPos playerPos = new BlockPos(
            mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v
        );

        for (int y = 1; y <= 6; y++) {
            BlockPos pos = playerPos.func_177979_c(y);
            if (!mc.field_71441_e.func_175623_d(pos) && mc.field_71441_e.func_175623_d(pos.func_177984_a())) {
                return pos;
            }
        }

        MovingObjectPosition ray = mc.field_71441_e
            .func_147447_a(
                new Vec3(
                    mc.field_71439_g.field_70165_t,
                    mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(),
                    mc.field_71439_g.field_70161_v
                ),
                new Vec3(
                    mc.field_71439_g.field_70165_t,
                    mc.field_71439_g.field_70163_u - mc.field_71442_b.func_78757_d() - 2.0,
                    mc.field_71439_g.field_70161_v
                ),
                false,
                true,
                false
            );
        return ray != null && ray.field_72313_a == MovingObjectType.BLOCK ? ray.func_178782_a() : null;
    }

    private void resetLegitMlg() {
        if (this.lastMlgSlot != -1 && mc.field_71439_g != null) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.lastMlgSlot;
            mc.field_71442_b.func_78765_e();
        }

        this.lastMlgSlot = -1;
        this.mlgPlaced = false;
    }

    @Override
    public void onDisabled() {
        this.lastOnGround = false;
        this.resetLegitMlg();
        Miau.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
        if (this.slowFalling) {
            this.slowFalling = false;
            ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
        }

        this.timerTicks = 0;
        this.timerSettick = 0;
        this.timerRepdist = 0.0;
        ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
    }

    @Override
    public void verifyValue(String mode) {
        if (this.isEnabled()) {
            this.onDisabled();
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
