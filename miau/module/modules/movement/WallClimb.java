package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import miau.util.world.BlockUtil;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class WallClimb extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty modeValue = new ModeProperty(
        "Mode", 0, new String[]{"Simple", "CheckerClimb", "Clip", "AAC3.3.12", "AACGlide", "Matrix", "Polar"}
    );
    public final ModeProperty clipMode = new ModeProperty(
        "ClipMode", 1, new String[]{"Jump", "Fast"}, () -> this.modeValue.getModeString().equals("Clip")
    );
    public final FloatProperty checkerClimbMotion = new FloatProperty(
        "CheckerClimbMotion", 0.0F, 0.0F, 1.0F, () -> this.modeValue.getModeString().equals("CheckerClimb")
    );
    private boolean glitch = false;
    private int waited = 0;
    private int airTicks = 0;

    public WallClimb() {
        super("WallClimb", false);
    }

    private boolean isInLiquid() {
        return mc.field_71439_g.func_70055_a(Material.field_151586_h)
            || mc.field_71439_g.func_70055_a(Material.field_151587_i);
    }

    private boolean collideBlockIntersects(AxisAlignedBB bb) {
        int minX = MathHelper.func_76128_c(bb.field_72340_a);
        int maxX = MathHelper.func_76128_c(bb.field_72336_d + 1.0);
        int minY = MathHelper.func_76128_c(bb.field_72338_b);
        int maxY = MathHelper.func_76128_c(bb.field_72337_e + 1.0);
        int minZ = MathHelper.func_76128_c(bb.field_72339_c);
        int maxZ = MathHelper.func_76128_c(bb.field_72334_f + 1.0);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    if (BlockUtil.getBlock(new BlockPos(x, y, z)) != Blocks.field_150350_a) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @EventTarget
    public void onMove(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g == null || mc.field_71441_e == null) {
                return;
            }

            if (!mc.field_71439_g.field_70123_F || mc.field_71439_g.func_70617_f_() || this.isInLiquid()) {
                return;
            }

            if (this.modeValue.getModeString().equals("Simple")) {
                mc.field_71439_g.field_70181_x = 0.2;
            }
        }
    }

    @EventTarget
    public void onMotion(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            if (mc.field_71439_g == null || mc.field_71441_e == null) {
                return;
            }

            if (mc.field_71439_g.field_70122_E) {
                this.airTicks = 0;
            } else {
                this.airTicks++;
            }

            String mode = this.modeValue.getModeString().toLowerCase();
            if (mode.equals("clip")) {
                if (mc.field_71439_g.field_70181_x < 0.0) {
                    this.glitch = true;
                }

                if (mc.field_71439_g.field_70123_F) {
                    if (this.clipMode.getModeString().equalsIgnoreCase("jump")) {
                        if (mc.field_71439_g.field_70122_E) {
                            mc.field_71439_g.func_70664_aZ();
                        }
                    } else if (mc.field_71439_g.field_70122_E) {
                        mc.field_71439_g.field_70181_x = 0.42;
                    } else if (mc.field_71439_g.field_70181_x < 0.0) {
                        mc.field_71439_g.field_70181_x = -0.3;
                    }
                }
            } else if (mode.equals("matrix")) {
                if (mc.field_71439_g.field_70181_x < 0.0) {
                    this.glitch = true;
                }

                if (mc.field_71439_g.field_70123_F) {
                    if (mc.field_71439_g.field_70122_E) {
                        mc.field_71439_g.field_70181_x = 0.42;
                    } else if (mc.field_71439_g.field_70181_x < 0.0 && this.airTicks >= 2) {
                        mc.field_71439_g.field_70181_x = -0.3;
                    }
                }
            } else if (mode.equals("checkerclimb")) {
                boolean isInsideBlock = this.collideBlockIntersects(mc.field_71439_g.func_174813_aQ());
                float motion = this.checkerClimbMotion.getValue();
                if (isInsideBlock && motion != 0.0F) {
                    mc.field_71439_g.field_70181_x = motion;
                }
            } else if (mode.equals("aac3.3.12")) {
                if (mc.field_71439_g.field_70123_F && !mc.field_71439_g.func_70617_f_()) {
                    this.waited++;
                    if (this.waited == 1) {
                        mc.field_71439_g.field_70181_x = 0.43;
                    }

                    if (this.waited == 12) {
                        mc.field_71439_g.field_70181_x = 0.43;
                    }

                    if (this.waited == 23) {
                        mc.field_71439_g.field_70181_x = 0.43;
                    }

                    if (this.waited == 29) {
                        mc.field_71439_g
                            .func_70107_b(
                                mc.field_71439_g.field_70165_t,
                                mc.field_71439_g.field_70163_u + 0.5,
                                mc.field_71439_g.field_70161_v
                            );
                    }

                    if (this.waited >= 30) {
                        this.waited = 0;
                    }
                } else if (mc.field_71439_g.field_70122_E) {
                    this.waited = 0;
                }
            } else if (mode.equals("aacglide")) {
                if (!mc.field_71439_g.field_70123_F || mc.field_71439_g.func_70617_f_()) {
                    return;
                }

                mc.field_71439_g.field_70181_x = -0.19;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()
            && event.getType() == EventType.SEND
            && event.getPacket() instanceof C03PacketPlayer
            && this.glitch) {
            double yaw = MoveUtil.getMoveDirection();
            mc.field_71439_g.field_70159_w = mc.field_71439_g.field_70159_w - Math.sin(yaw) * 1.0E-8;
            mc.field_71439_g.field_70179_y = mc.field_71439_g.field_70179_y + Math.cos(yaw) * 1.0E-8;
            this.glitch = false;
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.modeValue.getModeString()};
    }
}
