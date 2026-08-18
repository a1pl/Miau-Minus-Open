package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import miau.util.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class LiquidWalk extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty modeValue = new ModeProperty(
        "Mode",
        1,
        new String[]{
            "Vanilla", "NCP", "AAC", "AAC3.3.11", "AACFly", "Spartan", "Dolphin", "TatakoLatest", "VulcanA", "VulcanB"
        }
    );
    public final FloatProperty aacFly = new FloatProperty(
        "AACFlyMotion", 0.5F, 0.1F, 1.0F, () -> this.modeValue.getModeString().equals("AACFly")
    );
    public final BooleanProperty noJump = new BooleanProperty("NoJump", false);
    private boolean nextTick = false;
    private boolean wasInWater = false;

    public LiquidWalk() {
        super("LiquidWalk", false);
    }

    private boolean collideLiquid(AxisAlignedBB bb) {
        int minX = MathHelper.func_76128_c(bb.field_72340_a);
        int maxX = MathHelper.func_76128_c(bb.field_72336_d + 1.0);
        int minY = MathHelper.func_76128_c(bb.field_72338_b);
        int maxY = MathHelper.func_76128_c(bb.field_72337_e + 1.0);
        int minZ = MathHelper.func_76128_c(bb.field_72339_c);
        int maxZ = MathHelper.func_76128_c(bb.field_72334_f + 1.0);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    if (BlockUtil.getBlock(new BlockPos(x, y, z)) instanceof BlockLiquid) {
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

            String mode = this.modeValue.getModeString();
            if (mc.field_71439_g.func_70090_H()) {
                if (mode.equals("TatakoLatest")) {
                    mc.field_71439_g.field_70181_x += 0.13;
                } else if (mode.equals("VulcanA")) {
                    mc.field_71439_g.field_70181_x = 0.5;
                    MoveUtil.strafe(0.36);
                } else if (mode.equals("VulcanB")) {
                    MoveUtil.strafe(0.3F - (float)(Math.random() / 1000.0));
                    mc.field_71439_g.field_70181_x = 0.5;
                    this.wasInWater = true;
                } else if (mode.equals("AACFly")) {
                    mc.field_71439_g.field_70181_x = this.aacFly.getValue().floatValue();
                }
            }

            if (!mc.field_71439_g.func_70090_H() && this.wasInWater && mode.equals("VulcanB")) {
                mc.field_71439_g.field_70181_x = -1.0;
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            if (mc.field_71439_g == null || mc.field_71441_e == null) {
                return;
            }

            if (mc.field_71439_g.func_70093_af()) {
                return;
            }

            String mode = this.modeValue.getModeString();
            if (!mode.equals("NCP") && !mode.equals("Vanilla")) {
                if (mode.equals("AAC")) {
                    BlockPos blockPos = mc.field_71439_g.func_180425_c().func_177977_b();
                    if (!mc.field_71439_g.field_70122_E && BlockUtil.getBlock(blockPos) == Blocks.field_150355_j
                        || mc.field_71439_g.func_70090_H()) {
                        mc.field_71439_g.field_70159_w *= 0.99999;
                        mc.field_71439_g.field_70181_x *= 0.0;
                        mc.field_71439_g.field_70179_y *= 0.99999;
                        if (mc.field_71439_g.field_70123_F) {
                            int trunc = (int)(mc.field_71439_g.field_70163_u - 1.0);
                            mc.field_71439_g.field_70181_x = (int)(mc.field_71439_g.field_70163_u - trunc) / 8.0F;
                        }

                        if (mc.field_71439_g.field_70143_R >= 4.0F) {
                            mc.field_71439_g.field_70181_x = -0.004;
                        } else if (mc.field_71439_g.func_70090_H()) {
                            mc.field_71439_g.field_70181_x = 0.09;
                        }
                    }

                    if (mc.field_71439_g.field_70737_aN != 0) {
                        mc.field_71439_g.field_70122_E = false;
                    }
                } else if (mode.equals("Spartan")) {
                    if (mc.field_71439_g.func_70090_H()) {
                        if (mc.field_71439_g.field_70123_F) {
                            mc.field_71439_g.field_70181_x += 0.15;
                            return;
                        }

                        Block block = BlockUtil.getBlock(new BlockPos(mc.field_71439_g).func_177984_a());
                        Block blockUp = BlockUtil.getBlock(
                            new BlockPos(
                                mc.field_71439_g.field_70165_t,
                                mc.field_71439_g.field_70163_u + 1.1,
                                mc.field_71439_g.field_70161_v
                            )
                        );
                        if (blockUp instanceof BlockLiquid) {
                            mc.field_71439_g.field_70181_x = 0.1;
                        } else if (block instanceof BlockLiquid) {
                            mc.field_71439_g.field_70181_x = 0.0;
                        }

                        mc.field_71439_g.field_70122_E = true;
                        mc.field_71439_g.field_70159_w *= 1.085;
                        mc.field_71439_g.field_70179_y *= 1.085;
                    }
                } else if (mode.equals("AAC3.3.11")) {
                    if (mc.field_71439_g.func_70090_H()) {
                        mc.field_71439_g.field_70159_w *= 1.17;
                        mc.field_71439_g.field_70179_y *= 1.17;
                        if (mc.field_71439_g.field_70123_F) {
                            mc.field_71439_g.field_70181_x = 0.24;
                        } else if (BlockUtil.getBlock(new BlockPos(mc.field_71439_g).func_177984_a())
                            != Blocks.field_150350_a) {
                            mc.field_71439_g.field_70181_x += 0.04;
                        }
                    }
                } else if (mode.equals("Dolphin") && mc.field_71439_g.func_70090_H()) {
                    mc.field_71439_g.field_70181_x += 0.04F;
                }
            } else if (this.collideLiquid(mc.field_71439_g.func_174813_aQ())
                && mc.field_71439_g.func_70055_a(Material.field_151579_a)) {
                mc.field_71439_g.field_70181_x = 0.08;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.SEND) {
            if (mc.field_71439_g == null || mc.field_71441_e == null) {
                return;
            }

            if (!this.modeValue.getModeString().equals("NCP")) {
                return;
            }

            if (event.getPacket() instanceof C03PacketPlayer) {
                AxisAlignedBB bb = mc.field_71439_g.func_174813_aQ();
                if (this.collideLiquid(
                    AxisAlignedBB.func_178781_a(
                        bb.field_72336_d,
                        bb.field_72337_e,
                        bb.field_72334_f,
                        bb.field_72340_a,
                        bb.field_72338_b - 0.01,
                        bb.field_72339_c
                    )
                )) {
                    this.nextTick = !this.nextTick;
                    if (this.nextTick) {
                        mc.field_71439_g.field_70181_x -= 0.001;
                    }
                }
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.modeValue.getModeString()};
    }
}
