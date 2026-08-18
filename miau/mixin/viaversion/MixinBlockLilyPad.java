package miau.mixin.viaversion;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockLilyPad;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BlockLilyPad.class)
public abstract class MixinBlockLilyPad extends BlockBush {
    @Overwrite
    public AxisAlignedBB func_180640_a(World worldIn, BlockPos pos, IBlockState state) {
        return ViaLoadingBase.getInstance().getTargetVersion().getVersion() >= 107
            ? new AxisAlignedBB(
                pos.func_177958_n() + 0.0625,
                pos.func_177956_o(),
                pos.func_177952_p() + 0.0625,
                pos.func_177958_n() + 0.9375,
                pos.func_177956_o() + 0.09375,
                pos.func_177952_p() + 0.9375
            )
            : new AxisAlignedBB(
                pos.func_177958_n(),
                pos.func_177956_o(),
                pos.func_177952_p(),
                pos.func_177958_n() + 1.0,
                pos.func_177956_o() + 0.015625,
                pos.func_177952_p() + 1.0
            );
    }
}
