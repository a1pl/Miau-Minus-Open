package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorEntity;
import miau.module.Module;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.network.PacketUtil;
import miau.util.player.MoveUtil;
import miau.util.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockVine;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class FastClimb extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty modeValue = new ModeProperty(
        "Mode",
        0,
        new String[]{"Vanilla", "Delay", "Clip", "AAC3.0.0", "AAC3.0.5", "SAAC3.1.2", "AAC3.1.2", "GrizzlyLatest"}
    );
    public final FloatProperty speed = new FloatProperty(
        "Speed", 1.0F, 0.01F, 5.0F, () -> this.modeValue.getModeString().equals("Vanilla")
    );
    public final FloatProperty climbSpeed = new FloatProperty(
        "ClimbSpeed", 1.0F, 0.01F, 5.0F, () -> this.modeValue.getModeString().equals("Delay")
    );
    public final IntProperty tickDelay = new IntProperty(
        "TickDelay", 10, 1, 20, () -> this.modeValue.getModeString().equals("Delay")
    );
    private final int climbDelay = this.tickDelay.getValue();
    private int climbCount = 0;

    public FastClimb() {
        super("FastClimb", false);
    }

    private void playerClimb() {
        mc.field_71439_g.field_70181_x = 0.0;
        ((IAccessorEntity)mc.field_71439_g).setIsInWeb(true);
        mc.field_71439_g.field_70122_E = true;
        ((IAccessorEntity)mc.field_71439_g).setIsInWeb(false);
    }

    private boolean intersectLadderOrVine(AxisAlignedBB bb) {
        int minX = MathHelper.func_76128_c(bb.field_72340_a);
        int maxX = MathHelper.func_76128_c(bb.field_72336_d + 1.0);
        int minY = MathHelper.func_76128_c(bb.field_72338_b);
        int maxY = MathHelper.func_76128_c(bb.field_72337_e + 1.0);
        int minZ = MathHelper.func_76128_c(bb.field_72339_c);
        int maxZ = MathHelper.func_76128_c(bb.field_72334_f + 1.0);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    Block block = BlockUtil.getBlock(new BlockPos(x, y, z));
                    if (block instanceof BlockLadder || block instanceof BlockVine) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @EventTarget
    public void onMotion(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g == null || mc.field_71441_e == null) {
                return;
            }

            if (this.modeValue.getModeString().equals("GrizzlyLatest")
                && mc.field_71439_g.field_70123_F
                && mc.field_71439_g.func_70617_f_()) {
                mc.field_71439_g.field_70181_x = 0.19;
                if (mc.field_71439_g.field_70173_aa % 2 == 1) {
                    double yaw = MoveUtil.getMoveDirection();
                    mc.field_71439_g.field_70159_w = 0.0;
                    mc.field_71439_g.field_70179_y = 0.0;
                    mc.field_71439_g.field_70159_w = mc.field_71439_g.field_70159_w
                        + MathHelper.func_76126_a((float)yaw) * 0.15F;
                    mc.field_71439_g.field_70179_y = mc.field_71439_g.field_70179_y
                        - MathHelper.func_76134_b((float)yaw) * 0.15F;
                }
            }
        }
    }

    @EventTarget
    public void onMove(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            if (mc.field_71439_g == null || mc.field_71441_e == null) {
                return;
            }

            String mode = this.modeValue.getModeString();
            if (mode.equals("Vanilla") && mc.field_71439_g.field_70123_F && mc.field_71439_g.func_70617_f_()) {
                mc.field_71439_g.field_70181_x = this.speed.getValue().floatValue();
            } else if (mode.equals("Delay") && mc.field_71439_g.field_70123_F && mc.field_71439_g.func_70617_f_()) {
                if (this.climbCount >= this.climbDelay) {
                    this.playerClimb();
                    mc.field_71439_g.field_70181_x = this.climbSpeed.getValue().floatValue();
                    PacketUtil.sendPacket(
                        new C04PacketPlayerPosition(
                            mc.field_71439_g.field_70165_t,
                            mc.field_71439_g.field_70163_u,
                            mc.field_71439_g.field_70161_v,
                            true
                        )
                    );
                    this.climbCount = 0;
                } else {
                    mc.field_71439_g.field_70163_u = mc.field_71439_g.field_70167_r;
                    this.playerClimb();
                    this.climbCount++;
                }
            } else if (mode.equals("AAC3.0.0") && mc.field_71439_g.field_70123_F) {
                double x = 0.0;
                double z = 0.0;
                switch (mc.field_71439_g.func_174811_aO()) {
                    case NORTH:
                        z = -0.99;
                        break;
                    case EAST:
                        x = 0.99;
                        break;
                    case SOUTH:
                        z = 0.99;
                        break;
                    case WEST:
                        x = -0.99;
                }

                Block block = BlockUtil.getBlock(
                    new BlockPos(
                        mc.field_71439_g.field_70165_t + x,
                        mc.field_71439_g.field_70163_u,
                        mc.field_71439_g.field_70161_v + z
                    )
                );
                if (block instanceof BlockLadder || block instanceof BlockVine) {
                    mc.field_71439_g.field_70181_x = 0.5;
                }
            } else if (mode.equals("AAC3.0.5")
                && mc.field_71474_y.field_74351_w.func_151470_d()
                && this.intersectLadderOrVine(mc.field_71439_g.func_174813_aQ())) {
                mc.field_71439_g.field_70159_w = 0.0;
                mc.field_71439_g.field_70181_x = 0.5;
                mc.field_71439_g.field_70179_y = 0.0;
            } else if (mode.equals("SAAC3.1.2") && mc.field_71439_g.field_70123_F && mc.field_71439_g.func_70617_f_()) {
                mc.field_71439_g.field_70181_x = 0.1649;
            } else if (mode.equals("AAC3.1.2") && mc.field_71439_g.field_70123_F && mc.field_71439_g.func_70617_f_()) {
                mc.field_71439_g.field_70181_x = 0.1699;
            } else if (mode.equals("Clip")
                && mc.field_71439_g.func_70617_f_()
                && mc.field_71474_y.field_74351_w.func_151470_d()) {
                for (int i = (int)mc.field_71439_g.field_70163_u; i <= (int)mc.field_71439_g.field_70163_u + 8; i++) {
                    Block block = BlockUtil.getBlock(
                        new BlockPos(mc.field_71439_g.field_70165_t, i, mc.field_71439_g.field_70161_v)
                    );
                    if (!(block instanceof BlockLadder)) {
                        double x = 0.0;
                        double z = 0.0;
                        switch (mc.field_71439_g.func_174811_aO()) {
                            case NORTH:
                                z = -1.0;
                                break;
                            case EAST:
                                x = 1.0;
                                break;
                            case SOUTH:
                                z = 1.0;
                                break;
                            case WEST:
                                x = -1.0;
                        }

                        mc.field_71439_g
                            .func_70107_b(mc.field_71439_g.field_70165_t + x, i, mc.field_71439_g.field_70161_v + z);
                        break;
                    }

                    mc.field_71439_g.func_70107_b(mc.field_71439_g.field_70165_t, i, mc.field_71439_g.field_70161_v);
                }
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.modeValue.getModeString()};
    }
}
