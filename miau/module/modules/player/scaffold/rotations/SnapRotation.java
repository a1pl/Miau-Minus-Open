package miau.module.modules.player.scaffold.rotations;

import miau.event.impl.UpdateEvent;
import miau.module.modules.player.Scaffold;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class SnapRotation implements IRotationLogic {
    private int airTicks = 0;
    private static final int ROTATION_DELAY_TICKS = 2;
    private float lockedYaw = 0.0F;
    private float lockedPitch = 79.5F;
    private boolean hasSnapped = false;

    @Override
    public void handleInitialRotation(
        Scaffold scaffold, UpdateEvent event, float currentYaw, float yawDiffTo180, float diagonalYaw
    ) {
    }

    public void updateRotation(Scaffold scaffold, UpdateEvent event) {
        Minecraft mc = Scaffold.mc;
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            float forwardYaw = player.field_70177_z;
            float forwardPitch = player.field_70125_A;
            if (player.field_70122_E) {
                this.airTicks = 0;
                this.hasSnapped = false;
            } else {
                this.airTicks++;
            }

            if (player.field_70122_E) {
                scaffold.yaw = forwardYaw;
                scaffold.pitch = forwardPitch;
                scaffold.bridgeYaw = forwardYaw;
            } else if (this.airTicks < 2) {
                scaffold.yaw = forwardYaw;
                scaffold.pitch = forwardPitch;
                scaffold.bridgeYaw = forwardYaw;
            } else {
                if (!this.hasSnapped) {
                    this.lockedYaw = MathHelper.func_76142_g(forwardYaw - 180.0F);
                    Scaffold.BlockData blockData = scaffold.getBlockData();
                    if (blockData != null && blockData.blockPos != null) {
                        float[] rot = this.getRotationToBlock(player, blockData.blockPos);
                        this.lockedYaw = rot[0];
                        this.lockedPitch = rot[1];
                    } else {
                        this.lockedPitch = 79.5F;
                    }

                    this.hasSnapped = true;
                }

                float smoothSpeed = scaffold.keepYFeature.keepY.getValue() == 5
                    ? scaffold.keepYFeature.betaSmoothSpeed.getValue()
                    : 0.6F;
                scaffold.yaw = this.smoothAngle(scaffold.yaw, this.lockedYaw, smoothSpeed);
                scaffold.pitch = this.smoothAngle(scaffold.pitch, this.lockedPitch, smoothSpeed);
                scaffold.bridgeYaw = scaffold.yaw;
            }
        }
    }

    private float[] getRotationToBlock(EntityPlayer player, BlockPos pos) {
        double dx = pos.func_177958_n() + 0.5 - player.field_70165_t;
        double dy = pos.func_177956_o() + 0.5 - (player.field_70163_u + player.func_70047_e());
        double dz = pos.func_177952_p() + 0.5 - player.field_70161_v;
        double distHoriz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, distHoriz)));
        yaw = MathHelper.func_76142_g(yaw);
        pitch = MathHelper.func_76131_a(pitch, -90.0F, 90.0F);
        return new float[]{yaw, pitch};
    }

    private float smoothAngle(float current, float target, float alpha) {
        float diff = MathHelper.func_76142_g(target - current);
        return current + diff * alpha;
    }
}
