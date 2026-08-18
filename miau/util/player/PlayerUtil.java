package miau.util.player;

import miau.util.client.KeyBindUtil;
import miau.util.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.ForgeHooks;

public class PlayerUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public static boolean isJumping() {
        return mc.field_71462_r == null && KeyBindUtil.isKeyDown(mc.field_71474_y.field_74314_A.func_151463_i());
    }

    public static boolean isSneaking() {
        return mc.field_71462_r == null && KeyBindUtil.isKeyDown(mc.field_71474_y.field_74311_E.func_151463_i());
    }

    public static boolean isMovingLeft() {
        return mc.field_71462_r == null && KeyBindUtil.isKeyDown(mc.field_71474_y.field_74370_x.func_151463_i());
    }

    public static boolean isMovingRight() {
        return mc.field_71462_r == null && KeyBindUtil.isKeyDown(mc.field_71474_y.field_74366_z.func_151463_i());
    }

    public static boolean isAttacking() {
        return mc.field_71462_r == null && KeyBindUtil.isKeyDown(mc.field_71474_y.field_74312_F.func_151463_i());
    }

    public static boolean isUsingItem() {
        return mc.field_71462_r == null && KeyBindUtil.isKeyDown(mc.field_71474_y.field_74313_G.func_151463_i());
    }

    public static boolean canFly(float fallThreshold) {
        if (!mc.field_71439_g.field_71075_bZ.field_75101_c && !mc.field_71439_g.field_71075_bZ.field_75102_a) {
            PotionEffect jumpEffect = mc.field_71439_g.func_70660_b(Potion.field_76430_j);
            float jumpBoost = jumpEffect != null ? jumpEffect.func_76458_c() + 1 : 0.0F;
            float fallDistance = mc.field_71439_g.field_70143_R;
            if (mc.field_71439_g.field_70181_x < -0.67 || !isAirBelow()) {
                fallDistance -= (float)mc.field_71439_g.field_70181_x;
            }

            return MathHelper.func_76123_f(fallDistance - fallThreshold - jumpBoost) > 0;
        } else {
            return false;
        }
    }

    public static boolean canFly(int checkHeight) {
        if (!mc.field_71439_g.field_71075_bZ.field_75101_c && !mc.field_71439_g.field_71075_bZ.field_75102_a) {
            int playerY = MathHelper.func_76128_c(mc.field_71439_g.field_70163_u);

            for (int offset = 0; offset <= checkHeight; offset++) {
                int currentY = playerY - offset;
                if (currentY < 0) {
                    break;
                }

                Block block = mc.field_71441_e
                    .func_180495_p(
                        new BlockPos(mc.field_71439_g.field_70165_t, currentY, mc.field_71439_g.field_70161_v)
                    )
                    .func_177230_c();
                if (!(block instanceof BlockAir)) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public static boolean isInWater() {
        return checkInWater(mc.field_71439_g.func_174813_aQ().func_72314_b(-1.0E-6, 0.0, -1.0E-6));
    }

    public static boolean checkInWater(AxisAlignedBB boundingBox) {
        if (!mc.field_71439_g.func_70090_H() && !mc.field_71439_g.func_180799_ab()) {
            int minY = MathHelper.func_76128_c(boundingBox.field_72338_b);
            if (minY < 0) {
                return true;
            }

            int minX = MathHelper.func_76128_c(boundingBox.field_72340_a);
            int maxX = MathHelper.func_76128_c(boundingBox.field_72336_d + 1.0);
            int minZ = MathHelper.func_76128_c(boundingBox.field_72339_c);
            int maxZ = MathHelper.func_76128_c(boundingBox.field_72334_f + 1.0);

            for (int x = minX; x < maxX; x++) {
                for (int z = minZ; z < maxZ; z++) {
                    for (int y = minY; y >= 0; y--) {
                        if (!BlockUtil.isReplaceable(new BlockPos(x, y, z))) {
                            return false;
                        }
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public static boolean canMove(double x, double z) {
        return canMove(x, z, -1.0);
    }

    public static boolean canMove(double x, double z, double y) {
        AxisAlignedBB boundingBox = mc.field_71439_g.func_174813_aQ().func_72317_d(x, y, z);
        return mc.field_71441_e.func_72945_a(mc.field_71439_g, boundingBox).isEmpty();
    }

    public static boolean isAirBelow() {
        AxisAlignedBB axisAlignedBB = mc.field_71439_g.func_174813_aQ().func_72317_d(0.0, -1.0, 0.0);
        return !mc.field_71441_e.func_72945_a(mc.field_71439_g, axisAlignedBB).isEmpty();
    }

    public static boolean isAirAbove() {
        AxisAlignedBB axisAlignedBB = mc.field_71439_g.func_174813_aQ().func_72317_d(0.0, 1.0, 0.0);
        return !mc.field_71441_e.func_72945_a(mc.field_71439_g, axisAlignedBB).isEmpty();
    }

    public static boolean canReach(BlockPos blockPos, double reach) {
        return isBlockWithinReach(
            blockPos,
            mc.field_71439_g.field_70165_t,
            mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(),
            mc.field_71439_g.field_70161_v,
            reach
        );
    }

    public static boolean isBlockWithinReach(BlockPos blockPos, double x, double y, double z, double reach) {
        return blockPos.func_177957_d(x, y, z) < Math.pow(reach, 2.0);
    }

    public static double calculatePerfectRangeToEntity(Entity entity) {
        double range = 1000.0;
        Vec3 eyes = mc.field_71439_g.func_174824_e(1.0F);
        float[] rotations = RotationUtil.calculate(entity);
        Vec3 rotationVector = RayCastUtil.getVectorForRotation(rotations[1], rotations[0]);
        AxisAlignedBB bb = entity.func_174813_aQ().func_72314_b(0.1, 0.1, 0.1);
        MovingObjectPosition mop = bb.func_72327_a(
            eyes,
            eyes.func_72441_c(
                rotationVector.field_72450_a * range,
                rotationVector.field_72448_b * range,
                rotationVector.field_72449_c * range
            )
        );
        return mop != null ? mop.field_72307_f.func_72438_d(eyes) : Double.MAX_VALUE;
    }

    public static Block getBlock(BlockPos pos) {
        return mc.field_71441_e.func_180495_p(pos).func_177230_c();
    }

    public static Block block(double x, double y, double z) {
        return mc.field_71441_e.func_180495_p(new BlockPos(x, y, z)).func_177230_c();
    }

    public static Block blockRelativeToPlayer(float offsetX, float offsetY, float offsetZ) {
        return mc.field_71441_e
            .func_180495_p(
                new BlockPos(
                    mc.field_71439_g.field_70165_t + offsetX,
                    mc.field_71439_g.field_70163_u + offsetY,
                    mc.field_71439_g.field_70161_v + offsetZ
                )
            )
            .func_177230_c();
    }

    public static Block blockRelativeToPlayer(double offsetX, double offsetY, double offsetZ) {
        return block(
            mc.field_71439_g.field_70165_t + offsetX,
            mc.field_71439_g.field_70163_u + offsetY,
            mc.field_71439_g.field_70161_v + offsetZ
        );
    }

    public static boolean isBlockUnder(double height) {
        return isBlockUnder(height, true);
    }

    public static boolean isBlockUnder(double height, boolean boundingBox) {
        if (boundingBox) {
            AxisAlignedBB bb = mc.field_71439_g.func_174813_aQ().func_72317_d(0.0, -height, 0.0);
            return !mc.field_71441_e.func_72945_a(mc.field_71439_g, bb).isEmpty();
        }

        for (int offset = 0; offset < height; offset++) {
            if (blockRelativeToPlayer(0.0F, -offset, 0.0F).func_149730_j()) {
                return true;
            }
        }

        return false;
    }

    public static void attackEntity(Entity target) {
        if (ForgeHooks.onPlayerAttackTarget(mc.field_71439_g, target)
            && target.func_70075_an()
            && !target.func_85031_j(mc.field_71439_g)) {
            float baseDamage = (float)mc.field_71439_g
                .func_110148_a(SharedMonsterAttributes.field_111264_e)
                .func_111126_e();
            float enchantmentBonus = EnchantmentHelper.func_152377_a(
                mc.field_71439_g.func_70694_bm(),
                target instanceof EntityLivingBase
                    ? ((EntityLivingBase)target).func_70668_bt()
                    : EnumCreatureAttribute.UNDEFINED
            );
            int knockbackLevel = EnchantmentHelper.func_77501_a(mc.field_71439_g);
            if (mc.field_71439_g.func_70051_ag()) {
                knockbackLevel++;
            }

            mc.field_71439_g.func_71059_n(target);
        }
    }
}
