package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.JumpEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.FloatProperty;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class ReverseStep extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final FloatProperty motion = new FloatProperty("Motion", 1.0F, 0.21F, 4.0F);
    private boolean jumped = false;

    public ReverseStep() {
        super("ReverseStep", false);
    }

    private boolean collideBlock(AxisAlignedBB axisAlignedBB) {
        int y = MathHelper.func_76128_c(axisAlignedBB.field_72338_b);
        int startX = MathHelper.func_76128_c(mc.field_71439_g.func_174813_aQ().field_72340_a);
        int endX = MathHelper.func_76128_c(mc.field_71439_g.func_174813_aQ().field_72336_d) + 1;
        int startZ = MathHelper.func_76128_c(mc.field_71439_g.func_174813_aQ().field_72339_c);
        int endZ = MathHelper.func_76128_c(mc.field_71439_g.func_174813_aQ().field_72334_f) + 1;

        for (int x = startX; x < endX; x++) {
            for (int z = startZ; z < endZ; z++) {
                Block block = mc.field_71441_e.func_180495_p(new BlockPos(x, y, z)).func_177230_c();
                if (!(block instanceof BlockLiquid)) {
                    return false;
                }
            }
        }

        return true;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null && mc.field_71441_e != null) {
                if (mc.field_71439_g.field_70122_E) {
                    this.jumped = false;
                }

                if (mc.field_71439_g.field_70181_x > 0.0) {
                    this.jumped = true;
                }

                if (!this.collideBlock(mc.field_71439_g.func_174813_aQ())
                    && !this.collideBlock(
                        AxisAlignedBB.func_178781_a(
                            mc.field_71439_g.func_174813_aQ().field_72336_d,
                            mc.field_71439_g.func_174813_aQ().field_72337_e,
                            mc.field_71439_g.func_174813_aQ().field_72334_f,
                            mc.field_71439_g.func_174813_aQ().field_72340_a,
                            mc.field_71439_g.func_174813_aQ().field_72338_b - 0.01,
                            mc.field_71439_g.func_174813_aQ().field_72339_c
                        )
                    )) {
                    if (!mc.field_71474_y.field_74314_A.func_151470_d()
                        && !mc.field_71439_g.field_70122_E
                        && !mc.field_71439_g.field_71158_b.field_78901_c
                        && mc.field_71439_g.field_70181_x <= 0.0
                        && mc.field_71439_g.field_70143_R <= 1.0F
                        && !this.jumped) {
                        mc.field_71439_g.field_70181_x = -this.motion.getValue();
                    }
                }
            }
        }
    }

    @EventTarget
    public void onJump(JumpEvent event) {
        this.jumped = true;
    }
}
