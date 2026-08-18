package miau.util.shader;

import miau.util.render.RenderUtil;
import net.minecraft.client.shader.Framebuffer;

public class BlurUtils {
    private static Framebuffer stencilFrameBufferBlur = new Framebuffer(1, 1, false);
    private static Framebuffer stencilFrameBufferBloom = new Framebuffer(1, 1, false);

    public static void prepareBlur() {
        stencilFrameBufferBlur = RenderUtil.createFrameBuffer(stencilFrameBufferBlur);
        stencilFrameBufferBlur.func_147614_f();
        stencilFrameBufferBlur.func_147610_a(false);
    }

    public static void blurEnd(int passes, float radius) {
        stencilFrameBufferBlur.func_147609_e();
        KawaseBlur.renderBlur(stencilFrameBufferBlur.field_147617_g, passes, radius);
    }

    public static void prepareBloom() {
        stencilFrameBufferBloom = RenderUtil.createFrameBuffer(stencilFrameBufferBloom);
        stencilFrameBufferBloom.func_147614_f();
        stencilFrameBufferBloom.func_147610_a(false);
    }

    public static void bloomEnd(int passes, float radius) {
        stencilFrameBufferBloom.func_147609_e();
        KawaseBloom.renderBlur(stencilFrameBufferBloom.field_147617_g, passes, radius);
    }
}
