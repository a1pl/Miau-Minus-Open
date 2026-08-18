package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import miau.util.world.BlockUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

public class IceSpeed extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"NCP", "AAC", "Spartan"});

    public IceSpeed() {
        super("IceSpeed", false);
    }

    @Override
    public void onEnabled() {
        if (this.mode.getModeString().equals("NCP")) {
            Blocks.field_150432_aD.field_149765_K = 0.39F;
            Blocks.field_150403_cj.field_149765_K = 0.39F;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null && mc.field_71441_e != null) {
                String mode = this.mode.getModeString();
                if (mode.equals("NCP")) {
                    Blocks.field_150432_aD.field_149765_K = 0.39F;
                    Blocks.field_150403_cj.field_149765_K = 0.39F;
                } else {
                    Blocks.field_150432_aD.field_149765_K = 0.98F;
                    Blocks.field_150403_cj.field_149765_K = 0.98F;
                }

                if (mc.field_71439_g.field_70122_E
                    && !mc.field_71439_g.func_70617_f_()
                    && !mc.field_71439_g.func_70093_af()
                    && mc.field_71439_g.func_70051_ag()
                    && MoveUtil.isMoving()) {
                    if (BlockUtil.getBlock(mc.field_71439_g.func_180425_c().func_177977_b()) == Blocks.field_150432_aD
                        || BlockUtil.getBlock(mc.field_71439_g.func_180425_c().func_177977_b())
                            == Blocks.field_150403_cj) {
                        if (mode.equals("AAC")) {
                            mc.field_71439_g.field_70159_w *= 1.342;
                            mc.field_71439_g.field_70179_y *= 1.342;
                            Blocks.field_150432_aD.field_149765_K = 0.6F;
                            Blocks.field_150403_cj.field_149765_K = 0.6F;
                        } else if (mode.equals("Spartan")) {
                            BlockPos upBlock = new BlockPos(mc.field_71439_g).func_177981_b(2);
                            if (BlockUtil.getBlock(upBlock) != Blocks.field_150350_a) {
                                mc.field_71439_g.field_70159_w *= 1.342;
                                mc.field_71439_g.field_70179_y *= 1.342;
                            } else {
                                mc.field_71439_g.field_70159_w *= 1.18;
                                mc.field_71439_g.field_70179_y *= 1.18;
                            }

                            Blocks.field_150432_aD.field_149765_K = 0.6F;
                            Blocks.field_150403_cj.field_149765_K = 0.6F;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisabled() {
        Blocks.field_150432_aD.field_149765_K = 0.98F;
        Blocks.field_150403_cj.field_149765_K = 0.98F;
    }
}
