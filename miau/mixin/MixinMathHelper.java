package miau.mixin;

import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MathHelper.class)
public class MixinMathHelper {
    private static final float BF_SIN_TO_COS = (float) (Math.PI / 2);
    private static final int BF_SIN_BITS = 12;
    private static final int BF_SIN_MASK = ~(-1 << BF_SIN_BITS);
    private static final int BF_SIN_COUNT = BF_SIN_MASK + 1;
    private static final float BF_radFull = (float) (Math.PI * 2);
    private static final float BF_radToIndex = BF_SIN_COUNT / BF_radFull;
    private static final float[] BF_sinFull = new float[BF_SIN_COUNT];

    @Overwrite
    public static float func_76126_a(float rad) {
        return BF_sinFull[(int)(rad * BF_radToIndex) & BF_SIN_MASK];
    }

    @Overwrite
    public static float func_76134_b(float rad) {
        return BF_sinFull[(int)((rad + BF_SIN_TO_COS) * BF_radToIndex) & BF_SIN_MASK];
    }

    static {
        for (int i = 0; i < BF_SIN_COUNT; i++) {
            BF_sinFull[i] = (float)Math.sin((i + Math.min(1, i % (BF_SIN_COUNT / 4)) * 0.5) / BF_SIN_COUNT * BF_radFull);
        }
    }
}
