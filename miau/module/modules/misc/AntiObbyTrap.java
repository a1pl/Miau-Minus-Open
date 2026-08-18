package miau.module.modules.misc;

import miau.module.Module;
import miau.property.properties.BooleanProperty;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class AntiObbyTrap extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty setAir = new BooleanProperty("set-air", true);

    public AntiObbyTrap() {
        super("AntiObbyTrap", false);
    }

    public boolean isInsideBlock(World world, BlockPos blockPos) {
        IBlockState blockState = world.func_180495_p(blockPos);
        Block block = blockState.func_177230_c();
        if (block.func_149688_o().func_76220_a() && block.func_149730_j()) {
            Vec3 hitVec = new Vec3(
                mc.field_71439_g.field_70165_t,
                mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(),
                mc.field_71439_g.field_70161_v
            );
            return block.func_180640_a(mc.field_71441_e, blockPos, blockState).func_72318_a(hitVec);
        } else {
            return false;
        }
    }
}
