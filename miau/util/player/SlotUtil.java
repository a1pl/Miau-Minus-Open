package miau.util.player;

import java.util.Arrays;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockTNT;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockPos;

public final class SlotUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public static final List<Block> blacklist = Arrays.asList(
        Blocks.field_150381_bn,
        Blocks.field_150486_ae,
        Blocks.field_150477_bB,
        Blocks.field_150447_bR,
        Blocks.field_150467_bQ,
        Blocks.field_150354_m,
        Blocks.field_150321_G,
        Blocks.field_150478_aa,
        Blocks.field_150462_ai,
        Blocks.field_150460_al,
        Blocks.field_150392_bi,
        Blocks.field_150367_z,
        Blocks.field_150456_au,
        Blocks.field_150452_aw,
        Blocks.field_150323_B,
        Blocks.field_150409_cd,
        Blocks.field_150335_W,
        Blocks.field_180393_cK,
        Blocks.field_180394_cL,
        Blocks.field_150429_aA,
        Blocks.field_150423_aK
    );

    public static int findBlock() {
        for (int i = 36; i < 45; i++) {
            ItemStack item = mc.field_71439_g.field_71069_bz.func_75139_a(i).func_75211_c();
            if (item != null
                && item.func_77973_b() instanceof ItemBlock
                && (item.field_77994_a > 5 || item.field_77994_a > 20)) {
                Block block = ((ItemBlock)item.func_77973_b()).func_179223_d();
                if ((
                        block.func_149730_j()
                            || block instanceof BlockGlass
                            || block instanceof BlockStainedGlass
                            || block instanceof BlockTNT
                    )
                    && !blacklist.contains(block)) {
                    return i - 36;
                }
            }
        }

        return -1;
    }

    public static int findTool(BlockPos blockPos) {
        float bestSpeed = 1.0F;
        int bestSlot = -1;
        IBlockState blockState = mc.field_71441_e.func_180495_p(blockPos);

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (itemStack != null) {
                float speed = itemStack.func_150997_a(blockState.func_177230_c());
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }

        return bestSlot;
    }

    public static int findSword() {
        int bestDurability = -1;
        float bestDamage = -1.0F;
        int bestSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (itemStack != null && itemStack.func_77973_b() instanceof ItemSword) {
                ItemSword sword = (ItemSword)itemStack.func_77973_b();
                int sharpnessLevel = EnchantmentHelper.func_77506_a(Enchantment.field_180314_l.field_77352_x, itemStack);
                float damage = sword.func_150931_i() + sharpnessLevel * 1.25F;
                int durability = sword.func_77612_l();
                if (bestDamage < damage) {
                    bestDamage = damage;
                    bestDurability = durability;
                    bestSlot = i;
                }

                if (damage == bestDamage && durability > bestDurability) {
                    bestDurability = durability;
                    bestSlot = i;
                }
            }
        }

        return bestSlot;
    }

    public static int findItem(Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (itemStack == null) {
                if (item == null) {
                    return i;
                }
            } else if (itemStack.func_77973_b() == item) {
                return i;
            }
        }

        return -1;
    }

    public static int findBlock(Block block) {
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (itemStack == null) {
                if (block == null) {
                    return i;
                }
            } else if (itemStack.func_77973_b() instanceof ItemBlock
                && ((ItemBlock)itemStack.func_77973_b()).func_179223_d() == block) {
                return i;
            }
        }

        return -1;
    }

    private SlotUtil() {
    }
}
