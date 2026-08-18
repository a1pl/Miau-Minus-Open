package miau.util.world;

import miau.util.math.RandomUtil;
import miau.util.player.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockEndPortal;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockJukebox;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockSlime;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTNT;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockVine;
import net.minecraft.block.BlockWall;
import net.minecraft.block.BlockWeb;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class BlockUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public static IBlockState getBlockState(BlockPos pos) {
        return mc.field_71441_e.func_180495_p(pos);
    }

    public static Block getBlock(BlockPos pos) {
        return mc.field_71441_e.func_180495_p(pos).func_177230_c();
    }

    public static boolean isReplaceable(BlockPos blockPos) {
        return mc.field_71439_g != null && mc.field_71441_e != null
            ? getBlock(blockPos).func_176200_f(mc.field_71441_e, blockPos)
            : true;
    }

    public static boolean isInteractable(BlockPos blockPos) {
        return isInteractable(mc.field_71441_e.func_180495_p(blockPos).func_177230_c());
    }

    public static boolean isInteractable(Block block) {
        if (block instanceof BlockContainer) {
            return true;
        } else if (block instanceof BlockWorkbench) {
            return true;
        } else if (block instanceof BlockAnvil) {
            return true;
        } else if (block instanceof BlockBed) {
            return true;
        } else if (block instanceof BlockDoor && block.func_149688_o() != Material.field_151573_f) {
            return true;
        } else if (block instanceof BlockTrapDoor) {
            return true;
        } else if (block instanceof BlockFenceGate) {
            return true;
        } else if (block instanceof BlockFence) {
            return true;
        } else if (block instanceof BlockButton) {
            return true;
        } else {
            return block instanceof BlockLever ? true : block instanceof BlockJukebox;
        }
    }

    public static boolean isSolid(Block block) {
        if (block instanceof BlockStairs) {
            return false;
        } else if (block instanceof BlockSlab) {
            return false;
        } else if (block instanceof BlockEndPortalFrame) {
            return false;
        } else if (block instanceof BlockEndPortal) {
            return false;
        } else if (block instanceof BlockVine) {
            return false;
        } else if (block instanceof BlockPumpkin) {
            return false;
        } else if (block instanceof BlockCactus) {
            return false;
        } else if (block instanceof BlockBush) {
            return false;
        } else if (block instanceof BlockFalling) {
            return false;
        } else if (block instanceof BlockWeb) {
            return false;
        } else if (block instanceof BlockPane) {
            return false;
        } else if (block instanceof BlockCarpet) {
            return false;
        } else if (block instanceof BlockSnow) {
            return false;
        } else if (block instanceof BlockFence) {
            return false;
        } else if (block instanceof BlockFenceGate) {
            return false;
        } else if (block instanceof BlockWall) {
            return false;
        } else if (block instanceof BlockLadder) {
            return false;
        } else if (block instanceof BlockTorch) {
            return false;
        } else if (block instanceof BlockRedstoneWire) {
            return false;
        } else if (block instanceof BlockRedstoneDiode) {
            return false;
        } else if (block instanceof BlockBasePressurePlate) {
            return false;
        } else if (block instanceof BlockTripWire) {
            return false;
        } else if (block instanceof BlockTripWireHook) {
            return false;
        } else if (block instanceof BlockRailBase) {
            return false;
        } else {
            return block instanceof BlockSlime ? false : !(block instanceof BlockTNT);
        }
    }

    public static Vec3 getHitVec(BlockPos blockPos, EnumFacing enumFacing, float yaw, float pitch) {
        MovingObjectPosition movingObjectPosition = RotationUtil.rayCastBlock(
            mc.field_71442_b.func_78757_d(), yaw, pitch
        );
        return movingObjectPosition != null
                && movingObjectPosition.field_72313_a == MovingObjectType.BLOCK
                && movingObjectPosition.func_178782_a().equals(blockPos)
                && movingObjectPosition.field_178784_b == enumFacing
            ? movingObjectPosition.field_72307_f
            : getClickVec(blockPos, enumFacing);
    }

    public static Vec3 getClickVec(BlockPos blockPos, EnumFacing enumFacing) {
        Block block = mc.field_71441_e.func_180495_p(blockPos).func_177230_c();
        Vec3 vec3 = new Vec3(
            blockPos.func_177958_n()
                + Math.min(Math.max(RandomUtil.nextDouble(0.0, 1.0), block.func_149704_x()), block.func_149753_y()),
            blockPos.func_177956_o()
                + Math.min(Math.max(RandomUtil.nextDouble(0.0, 1.0), block.func_149665_z()), block.func_149669_A()),
            blockPos.func_177952_p()
                + Math.min(Math.max(RandomUtil.nextDouble(0.0, 1.0), block.func_149706_B()), block.func_149693_C())
        );
        switch (enumFacing) {
            case UP:
                return new Vec3(
                    vec3.field_72450_a, blockPos.func_177956_o() + block.func_149669_A(), vec3.field_72449_c
                );
            case NORTH:
                return new Vec3(
                    vec3.field_72450_a, vec3.field_72448_b, blockPos.func_177952_p() + block.func_149706_B()
                );
            case EAST:
                return new Vec3(
                    blockPos.func_177958_n() + block.func_149753_y(), vec3.field_72448_b, vec3.field_72449_c
                );
            case SOUTH:
                return new Vec3(
                    vec3.field_72450_a, vec3.field_72448_b, blockPos.func_177952_p() + block.func_149693_C()
                );
            case WEST:
                return new Vec3(
                    blockPos.func_177958_n() + block.func_149704_x(), vec3.field_72448_b, vec3.field_72449_c
                );
            default:
                return new Vec3(
                    vec3.field_72450_a, blockPos.func_177956_o() + block.func_149665_z(), vec3.field_72449_c
                );
        }
    }
}
