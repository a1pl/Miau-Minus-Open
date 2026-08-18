package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.WebSlowDownEvent;
import miau.management.RotationState;
import miau.module.Module;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import miau.util.player.SimulatedPlayer;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

public class NoWeb extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"POLAR", "VANILLA"});
    public final IntProperty hurtTime = new IntProperty("hurt-time", 1, 0, 10);
    public final FloatProperty xzMotion = new FloatProperty("xz-motion", 0.62F, 0.0F, 1.0F);
    public final FloatProperty yMotion = new FloatProperty("y-motion", 0.89F, 0.0F, 1.0F);

    public NoWeb() {
        super("NoWeb", false);
    }

    @EventTarget
    public void onWebSlowDown(WebSlowDownEvent e) {
        if (this.isEnabled()) {
            if (this.mode.getValue() == 0) {
                if (mc.field_71439_g.field_70737_aN >= this.hurtTime.getValue()) {
                    boolean jumpHeld = mc.field_71474_y.field_74314_A.func_151470_d();
                    e.setCancelled(jumpHeld);
                } else {
                    BlockPos below = new BlockPos(
                        mc.field_71439_g.field_70165_t,
                        mc.field_71439_g.field_70163_u - 1.0,
                        mc.field_71439_g.field_70161_v
                    );
                    Block blockBelow = mc.field_71441_e.func_180495_p(below).func_177230_c();
                    if (!blockBelow.func_149730_j() && !blockBelow.func_149662_c()) {
                        boolean sneaking = mc.field_71474_y.field_74311_E.func_151470_d();
                        if (!sneaking) {
                            e.setMotionY(0.0804F);
                        } else {
                            e.setMotionY(-10.0);
                        }

                        SimulatedPlayer sim = SimulatedPlayer.fromClientPlayer(mc.field_71439_g.field_71158_b);
                        sim.rotationYaw = RotationState.isActived()
                            ? RotationState.getRotationYawHead()
                            : mc.field_71439_g.field_70177_z;
                        sim.tick();
                        if (sim.isInWeb() && MoveUtil.isMoving()) {
                            double moveDirection = MoveUtil.getMoveDirection();
                            e.setMotionX(-Math.sin(moveDirection) * 0.1);
                            e.setMotionZ(Math.cos(moveDirection) * 0.1);
                        }
                    }
                }
            } else if (this.mode.getValue() == 1) {
                e.setCancelled(true);
            }
        }
    }
}
