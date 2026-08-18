package miau.util.shader;

import java.util.ArrayList;
import java.util.List;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

public class KawaseBloom {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public static ShaderUtils kawaseDown = new ShaderUtils("kawaseDownBloom");
    public static ShaderUtils kawaseUp = new ShaderUtils("kawaseUpBloom");
    public static Framebuffer framebuffer = new Framebuffer(1, 1, false);
    private static int currentIterations;
    private static final List<Framebuffer> framebufferList = new ArrayList<>();

    private static void initFramebuffers(float iterations) {
        for (Framebuffer framebuffer : framebufferList) {
            framebuffer.func_147608_a();
        }

        framebufferList.clear();
        framebufferList.add(KawaseBloom.framebuffer = RenderUtil.createFrameBuffer(null, false));

        for (int i = 1; i <= iterations; i++) {
            Framebuffer currentBuffer = new Framebuffer(
                (int)(mc.field_71443_c / Math.pow(2.0, i)), (int)(mc.field_71440_d / Math.pow(2.0, i)), false
            );
            currentBuffer.func_147607_a(9729);
            GlStateManager.func_179144_i(currentBuffer.field_147617_g);
            GL11.glTexParameteri(3553, 10242, 33648);
            GL11.glTexParameteri(3553, 10243, 33648);
            GlStateManager.func_179144_i(0);
            framebufferList.add(currentBuffer);
        }
    }

    public static void renderBlur(int framebufferTexture, int iterations, float offset) {
        if (currentIterations != iterations
            || framebuffer.field_147621_c != mc.field_71443_c
            || framebuffer.field_147618_d != mc.field_71440_d) {
            initFramebuffers(iterations);
            currentIterations = iterations;
        }

        RenderUtil.setAlphaLimit(0.0F);
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(1, 1);
        GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        float currentOffset = offset;
        renderFBO(framebufferList.get(1), framebufferTexture, kawaseDown, currentOffset);

        for (int i = 1; i < iterations; i++) {
            currentOffset = offset / (float)Math.pow(1.5, i);
            renderFBO(framebufferList.get(i + 1), framebufferList.get(i).field_147617_g, kawaseDown, currentOffset);
        }

        for (int i = iterations; i > 1; i--) {
            currentOffset = offset / (float)Math.pow(1.5, i - 1);
            renderFBO(framebufferList.get(i - 1), framebufferList.get(i).field_147617_g, kawaseUp, currentOffset);
        }

        Framebuffer lastBuffer = framebufferList.get(0);
        lastBuffer.func_147614_f();
        lastBuffer.func_147610_a(false);
        kawaseUp.init();
        kawaseUp.setUniformf("offset", offset, offset);
        kawaseUp.setUniformi("inTexture", 0);
        kawaseUp.setUniformi("check", 1);
        kawaseUp.setUniformi("textureToCheck", 16);
        kawaseUp.setUniformf("halfpixel", 1.0F / lastBuffer.field_147621_c, 1.0F / lastBuffer.field_147618_d);
        kawaseUp.setUniformf("iResolution", lastBuffer.field_147621_c, lastBuffer.field_147618_d);
        GlStateManager.func_179138_g(34000);
        RenderUtil.bindTexture(framebufferTexture);
        GlStateManager.func_179138_g(33984);
        RenderUtil.bindTexture(framebufferList.get(1).field_147617_g);
        ShaderUtils.drawQuads();
        kawaseUp.unload();
        GlStateManager.func_179082_a(0.0F, 0.0F, 0.0F, 0.0F);
        mc.func_147110_a().func_147610_a(false);
        RenderUtil.bindTexture(framebufferList.get(0).field_147617_g);
        RenderUtil.setAlphaLimit(0.0F);
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        ShaderUtils.drawQuads();
        GlStateManager.func_179144_i(0);
        RenderUtil.setAlphaLimit(0.0F);
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
    }

    private static void renderFBO(Framebuffer framebuffer, int framebufferTexture, ShaderUtils shader, float offset) {
        framebuffer.func_147614_f();
        framebuffer.func_147610_a(false);
        shader.init();
        RenderUtil.bindTexture(framebufferTexture);
        shader.setUniformf("offset", offset, offset);
        shader.setUniformi("inTexture", 0);
        shader.setUniformi("check", 0);
        shader.setUniformf("halfpixel", 1.0F / framebuffer.field_147621_c, 1.0F / framebuffer.field_147618_d);
        shader.setUniformf("iResolution", framebuffer.field_147621_c, framebuffer.field_147618_d);
        ShaderUtils.drawQuads();
        shader.unload();
    }
}
