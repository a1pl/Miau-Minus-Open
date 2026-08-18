package miau.management;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public class RotationState {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static int state = -1;
    private static float prevRenderYawOffset;
    private static float renderYawOffset;
    private static float prevRotationYawHead;
    private static float rotationYawHead;
    private static float prevRotationPitch;
    private static float rotationPitch;
    private static float smoothYaw;
    private static int priority;

    private static float calculateRenderYawOffset(float targetYaw, float currentYawOffset) {
        float newYawOffset = currentYawOffset;
        double deltaX = mc.field_71439_g.field_70165_t - mc.field_71439_g.field_70169_q;
        double deltaZ = mc.field_71439_g.field_70161_v - mc.field_71439_g.field_70166_s;
        if ((float)(deltaX * deltaX + deltaZ * deltaZ) > 0.0025000002F) {
            newYawOffset = (float)MathHelper.func_181159_b(deltaZ, deltaX) * 180.0F / (float) Math.PI - 90.0F;
        }

        if (mc.field_71439_g.field_70733_aJ > 0.0F) {
            newYawOffset = targetYaw;
        }

        float f4 = MathHelper.func_76142_g(newYawOffset - currentYawOffset);
        float var9;
        float f5 = MathHelper.func_76142_g(targetYaw - (var9 = currentYawOffset + f4 * 0.3F));
        if (f5 < -75.0F) {
            f5 = -75.0F;
        }

        if (f5 >= 75.0F) {
            f5 = 75.0F;
        }

        newYawOffset = targetYaw - f5;
        if (f5 * f5 > 2500.0F) {
            newYawOffset += f5 * 0.2F;
        }

        return newYawOffset;
    }

    public static void applyState(boolean bl, float f, float f2, float f3, int n) {
        state = bl ? 0 : state + 1;
        prevRenderYawOffset = renderYawOffset;
        renderYawOffset = bl ? calculateRenderYawOffset(f, renderYawOffset) : mc.field_71439_g.field_70761_aq;
        prevRotationYawHead = rotationYawHead;
        rotationYawHead = bl ? f : mc.field_71439_g.field_70759_as;
        prevRotationPitch = rotationPitch;
        rotationPitch = bl ? f2 : mc.field_71439_g.field_70125_A;
        smoothYaw = f3;
        priority = n;
    }

    public static boolean isActived() {
        return isRotated(0);
    }

    public static boolean isRotated(int state) {
        return RotationState.state < 0 ? false : RotationState.state <= state;
    }

    public static float getPrevRenderYawOffset() {
        return prevRenderYawOffset;
    }

    public static float getRenderYawOffset() {
        return renderYawOffset;
    }

    public static float getPrevRotationYawHead() {
        return prevRotationYawHead;
    }

    public static float getRotationYawHead() {
        return rotationYawHead;
    }

    public static float getPrevRotationPitch() {
        return prevRotationPitch;
    }

    public static float getRotationPitch() {
        return rotationPitch;
    }

    public static float getSmoothedYaw() {
        return smoothYaw;
    }

    public static float getPriority() {
        return priority;
    }
}
