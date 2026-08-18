package de.florianmichael.viamcp.fixes;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class FixedSoundEngine {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public static boolean destroyBlock(World world, BlockPos pos, boolean dropBlock) {
        IBlockState iblockstate = world.func_180495_p(pos);
        Block block = iblockstate.func_177230_c();
        world.func_175718_b(2001, pos, Block.func_176210_f(iblockstate));
        if (block.func_149688_o() == Material.field_151579_a) {
            return false;
        }

        if (dropBlock) {
            block.func_176226_b(world, pos, iblockstate, 0);
        }

        return world.func_180501_a(pos, Blocks.field_150350_a.func_176223_P(), 3);
    }

    public static boolean onItemUse(
        ItemBlock iblock,
        ItemStack stack,
        EntityPlayer playerIn,
        World worldIn,
        BlockPos pos,
        EnumFacing side,
        float hitX,
        float hitY,
        float hitZ
    ) {
        IBlockState iblockstate = worldIn.func_180495_p(pos);
        Block block = iblockstate.func_177230_c();
        if (!block.func_176200_f(worldIn, pos)) {
            pos = pos.func_177972_a(side);
        }

        if (stack.field_77994_a == 0) {
            return false;
        }

        if (!playerIn.func_175151_a(pos, side, stack)) {
            return false;
        }

        if (worldIn.func_175716_a(iblock.func_179223_d(), pos, false, side, (Entity)null, stack)) {
            int i = iblock.func_77647_b(stack.func_77960_j());
            IBlockState iblockstate1 = iblock.func_179223_d()
                .func_180642_a(worldIn, pos, side, hitX, hitY, hitZ, i, playerIn);
            if (worldIn.func_180501_a(pos, iblockstate1, 3)) {
                iblockstate1 = worldIn.func_180495_p(pos);
                if (iblockstate1.func_177230_c() == iblock.func_179223_d()) {
                    ItemBlock.func_179224_a(worldIn, playerIn, pos, stack);
                    iblock.func_179223_d().func_180633_a(worldIn, pos, iblockstate1, playerIn, stack);
                }

                if (ViaLoadingBase.getInstance().getTargetVersion().getOriginalVersion() != 47) {
                    mc.field_71441_e
                        .func_175731_a(
                            pos.func_177963_a(0.5, 0.5, 0.5),
                            iblock.func_179223_d().field_149762_H.func_150496_b(),
                            (iblock.func_179223_d().field_149762_H.func_150497_c() + 1.0F) / 2.0F,
                            iblock.func_179223_d().field_149762_H.func_150494_d() * 0.8F,
                            false
                        );
                } else {
                    worldIn.func_72908_a(
                        pos.func_177958_n() + 0.5F,
                        pos.func_177956_o() + 0.5F,
                        pos.func_177952_p() + 0.5F,
                        iblock.func_179223_d().field_149762_H.func_150496_b(),
                        (iblock.func_179223_d().field_149762_H.func_150497_c() + 1.0F) / 2.0F,
                        iblock.func_179223_d().field_149762_H.func_150494_d() * 0.8F
                    );
                }

                stack.field_77994_a--;
            }

            return true;
        } else {
            return false;
        }
    }
}
