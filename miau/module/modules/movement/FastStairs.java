package miau.module.modules.movement;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import miau.util.world.BlockUtil;
import net.minecraft.block.BlockStairs;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

public class FastStairs extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty(
        "Mode", 1, new String[]{"Step", "NCP", "AAC3.1.0", "AAC3.3.6", "AAC3.3.13"}
    );
    public final BooleanProperty longJump = new BooleanProperty(
        "LongJump", false, () -> this.mode.getModeString().startsWith("AAC")
    );
    private boolean canJump = false;
    private boolean walkingDown = false;

    public FastStairs() {
        super("FastStairs", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null && mc.field_71441_e != null) {
                if (MoveUtil.isMoving() && !Miau.moduleManager.modules.get(Speed.class).isEnabled()) {
                    if (mc.field_71439_g.field_70143_R > 0.0F && !this.walkingDown) {
                        this.walkingDown = true;
                    }

                    if (mc.field_71439_g.field_70163_u > mc.field_71439_g.field_70167_r) {
                        this.walkingDown = false;
                    }

                    String mode = this.mode.getModeString();
                    if (mc.field_71439_g.field_70122_E) {
                        BlockPos blockPos = new BlockPos(mc.field_71439_g);
                        if (BlockUtil.getBlock(blockPos) instanceof BlockStairs && !this.walkingDown) {
                            mc.field_71439_g
                                .func_70107_b(
                                    mc.field_71439_g.field_70165_t,
                                    mc.field_71439_g.field_70163_u + 0.5,
                                    mc.field_71439_g.field_70161_v
                                );
                            double motion;
                            if (mode.equals("NCP")) {
                                motion = 1.4;
                            } else if (mode.equals("AAC3.1.0")) {
                                motion = 1.5;
                            } else if (mode.equals("AAC3.3.13")) {
                                motion = 1.2;
                            } else {
                                motion = 1.0;
                            }

                            mc.field_71439_g.field_70159_w *= motion;
                            mc.field_71439_g.field_70179_y *= motion;
                        }

                        if (BlockUtil.getBlock(blockPos.func_177977_b()) instanceof BlockStairs) {
                            if (this.walkingDown) {
                                if (mode.equals("NCP")) {
                                    mc.field_71439_g.field_70181_x = -1.0;
                                } else if (mode.equals("AAC3.3.13")) {
                                    mc.field_71439_g.field_70181_x -= 0.014;
                                }

                                return;
                            }

                            double motion;
                            if (mode.equals("AAC3.3.6")) {
                                motion = 1.48;
                            } else if (mode.equals("AAC3.3.13")) {
                                motion = 1.52;
                            } else {
                                motion = 1.3;
                            }

                            mc.field_71439_g.field_70159_w *= motion;
                            mc.field_71439_g.field_70179_y *= motion;
                            this.canJump = true;
                        } else if (mode.startsWith("AAC") && this.canJump) {
                            if (this.longJump.getValue()) {
                                if (!mc.field_71474_y.field_74314_A.func_151470_d()) {
                                    mc.field_71439_g.func_70664_aZ();
                                }

                                mc.field_71439_g.field_70159_w *= 1.35;
                                mc.field_71439_g.field_70179_y *= 1.35;
                            }

                            this.canJump = false;
                        }
                    }
                }
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
