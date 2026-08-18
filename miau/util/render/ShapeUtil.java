package miau.util.render;

import miau.mixin.IAccessorEntityRenderer;
import miau.mixin.IAccessorMinecraft;
import miau.mixin.IAccessorRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

public class ShapeUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public static void drawFilledCircle(double x, double y, double radius, int color) {
        float a = (color >> 24 & 0xFF) / 255.0F;
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        GlStateManager.func_179147_l();
        GlStateManager.func_179090_x();
        GlStateManager.func_179112_b(770, 771);
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(6);

        for (int i = 0; i <= 360; i++) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x + Math.sin(angle) * radius, y + Math.cos(angle) * radius);
        }

        GL11.glEnd();
        GlStateManager.func_179098_w();
        GlStateManager.func_179084_k();
        GlStateManager.func_179117_G();
    }

    public static void drawRect(float x1, float y1, float x2, float y2, int color) {
        if (color != 0) {
            boolean texture2D = GL11.glIsEnabled(3553);
            boolean blend = GL11.glIsEnabled(3042);
            if (texture2D) {
                GL11.glDisable(3553);
            }

            if (!blend) {
                GL11.glEnable(3042);
            }

            GL11.glBlendFunc(770, 771);
            RenderUtil.setColor(color);
            Tessellator tessellator = Tessellator.func_178181_a();
            WorldRenderer worldrenderer = tessellator.func_178180_c();
            worldrenderer.func_181668_a(7, DefaultVertexFormats.field_181705_e);
            worldrenderer.func_181662_b(x1, y1, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x1, y2, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x2, y2, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x2, y1, 0.0).func_181675_d();
            tessellator.func_78381_a();
            if (!blend) {
                GL11.glDisable(3042);
            }

            if (texture2D) {
                GL11.glEnable(3553);
            }

            GlStateManager.func_179117_G();
        }
    }

    public static void drawRect(double left, double top, double right, double bottom, int color) {
        float f3 = (color >> 24 & 0xFF) / 255.0F;
        float f = (color >> 16 & 0xFF) / 255.0F;
        float f1 = (color >> 8 & 0xFF) / 255.0F;
        float f2 = (color & 0xFF) / 255.0F;
        GlStateManager.func_179094_E();
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        GlStateManager.func_179147_l();
        GlStateManager.func_179090_x();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        GlStateManager.func_179131_c(f, f1, f2, f3);
        worldrenderer.func_181668_a(7, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(left, bottom, 0.0).func_181675_d();
        worldrenderer.func_181662_b(right, bottom, 0.0).func_181675_d();
        worldrenderer.func_181662_b(right, top, 0.0).func_181675_d();
        worldrenderer.func_181662_b(left, top, 0.0).func_181675_d();
        tessellator.func_78381_a();
        GlStateManager.func_179098_w();
        GlStateManager.func_179084_k();
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.func_179121_F();
    }

    public static void drawRect3D(float x1, float y1, float x2, float y2, int color) {
        if (color != 0) {
            RenderUtil.setColor(color);
            GL11.glEnable(2881);
            GL11.glHint(3155, 4354);
            Tessellator tessellator = Tessellator.func_178181_a();
            WorldRenderer worldrenderer = tessellator.func_178180_c();
            worldrenderer.func_181668_a(9, DefaultVertexFormats.field_181705_e);

            for (int i = 0; i < 2; i++) {
                worldrenderer.func_181662_b(x1, y1, 0.0).func_181675_d();
                worldrenderer.func_181662_b(x1, y2, 0.0).func_181675_d();
                worldrenderer.func_181662_b(x2, y2, 0.0).func_181675_d();
                worldrenderer.func_181662_b(x2, y1, 0.0).func_181675_d();
            }

            tessellator.func_78381_a();
            GL11.glDisable(2881);
            GlStateManager.func_179117_G();
        }
    }

    public static void drawOutlineRect(
        float x1, float y1, float x2, float y2, float lineWidth, int backgroundColor, int lineColor
    ) {
        drawRect(x1, y1, x2, y2, backgroundColor);
        if (lineColor != 0) {
            RenderUtil.setColor(lineColor);
            GL11.glLineWidth(lineWidth);
            GL11.glEnable(2848);
            GL11.glHint(3154, 4354);
            Tessellator tessellator = Tessellator.func_178181_a();
            WorldRenderer worldrenderer = tessellator.func_178180_c();
            worldrenderer.func_181668_a(1, DefaultVertexFormats.field_181705_e);
            worldrenderer.func_181662_b(x1, y1, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x1, y2, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x2, y2, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x2, y1, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x1, y1, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x2, y1, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x1, y2, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x2, y2, 0.0).func_181675_d();
            tessellator.func_78381_a();
            GL11.glDisable(2848);
            GL11.glLineWidth(2.0F);
            GlStateManager.func_179117_G();
        }
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        RenderUtil.setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(1, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(x1, y1, 0.0).func_181675_d();
        worldrenderer.func_181662_b(x2, y2, 0.0).func_181675_d();
        tessellator.func_78381_a();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0F);
        GlStateManager.func_179117_G();
    }

    public static void drawLine3D(
        Vec3 start,
        double endX,
        double endY,
        double endZ,
        float red,
        float green,
        float blue,
        float alpha,
        float lineWidth
    ) {
        GlStateManager.func_179094_E();
        GlStateManager.func_179131_c(red, green, blue, alpha);
        boolean bl = mc.field_71474_y.field_74336_f;
        mc.field_71474_y.field_74336_f = false;
        ((IAccessorEntityRenderer)mc.field_71460_t)
            .callSetupCameraTransform(((IAccessorMinecraft)mc).getTimer().field_74281_c, 2);
        mc.field_71474_y.field_74336_f = bl;
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(1, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(start.field_72450_a, start.field_72448_b, start.field_72449_c).func_181675_d();
        worldrenderer.func_181662_b(
                endX - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX(),
                endY - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY(),
                endZ - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ()
            )
            .func_181675_d();
        tessellator.func_78381_a();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0F);
        GlStateManager.func_179117_G();
        GlStateManager.func_179121_F();
    }

    public static void drawArrow(float centerX, float centerY, float angle, float length, float lineWidth, int color) {
        float f6 = angle + (float)Math.toRadians(45.0);
        float f7 = angle - (float)Math.toRadians(45.0);
        RenderUtil.setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(1, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(centerX, centerY, 0.0).func_181675_d();
        worldrenderer.func_181662_b(centerX + length * (float)Math.cos(f6), centerY + length * (float)Math.sin(f6), 0.0)
            .func_181675_d();
        worldrenderer.func_181662_b(centerX, centerY, 0.0).func_181675_d();
        worldrenderer.func_181662_b(centerX + length * (float)Math.cos(f7), centerY + length * (float)Math.sin(f7), 0.0)
            .func_181675_d();
        tessellator.func_78381_a();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0F);
        GlStateManager.func_179117_G();
    }

    public static void drawTriangle(float centerX, float centerY, float angle, float length, int color) {
        float f5 = angle + (float)Math.toRadians(26.25);
        float f6 = angle - (float)Math.toRadians(26.25);
        RenderUtil.setColor(color);
        GL11.glEnable(2881);
        GL11.glHint(3155, 4354);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(9, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(centerX, centerY, 0.0).func_181675_d();
        worldrenderer.func_181662_b(centerX + length * (float)Math.cos(f5), centerY + length * (float)Math.sin(f5), 0.0)
            .func_181675_d();
        worldrenderer.func_181662_b(centerX + length * (float)Math.cos(f6), centerY + length * (float)Math.sin(f6), 0.0)
            .func_181675_d();
        tessellator.func_78381_a();
        GL11.glDisable(2881);
        GlStateManager.func_179117_G();
    }

    public static void drawTriangle(double x, double y, double size, double widthDiv, double heightDiv, int color) {
        boolean blend = GL11.glIsEnabled(3042);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(2848);
        GL11.glPushMatrix();
        RenderUtil.setColor(color);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(7, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(x, y, 0.0).func_181675_d();
        worldrenderer.func_181662_b(x - size / widthDiv, y + size, 0.0).func_181675_d();
        worldrenderer.func_181662_b(x, y + size / heightDiv, 0.0).func_181675_d();
        worldrenderer.func_181662_b(x + size / widthDiv, y + size, 0.0).func_181675_d();
        worldrenderer.func_181662_b(x, y, 0.0).func_181675_d();
        tessellator.func_78381_a();
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.8F);
        worldrenderer.func_181668_a(2, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(x, y, 0.0).func_181675_d();
        worldrenderer.func_181662_b(x - size / widthDiv, y + size, 0.0).func_181675_d();
        worldrenderer.func_181662_b(x, y + size / heightDiv, 0.0).func_181675_d();
        worldrenderer.func_181662_b(x + size / widthDiv, y + size, 0.0).func_181675_d();
        worldrenderer.func_181662_b(x, y, 0.0).func_181675_d();
        tessellator.func_78381_a();
        GL11.glPopMatrix();
        GL11.glEnable(3553);
        if (!blend) {
            GL11.glDisable(3042);
        }

        GL11.glDisable(2848);
    }

    public static void fillCircle(double x, double y, double radius, int segments, int color) {
        GlStateManager.func_179147_l();
        GlStateManager.func_179090_x();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        RenderUtil.setColor(color);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(6, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(x, y, 0.0).func_181675_d();

        for (int i = 0; i <= segments; i++) {
            double angle = i * ((Math.PI * 2) / segments);
            double px = x + Math.cos(angle) * radius;
            double py = y + Math.sin(angle) * radius;
            worldrenderer.func_181662_b(px, py, 0.0).func_181675_d();
        }

        tessellator.func_78381_a();
        GlStateManager.func_179098_w();
        GlStateManager.func_179084_k();
        GlStateManager.func_179117_G();
    }

    public static void drawCircle(
        double centerX, double centerY, double centerZ, double radius, int segments, int color
    ) {
        RenderUtil.setColor(color);
        GL11.glLineWidth(3.0F);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(2, DefaultVertexFormats.field_181705_e);

        for (int i = 0; i <= segments; i++) {
            double d5 = i * ((Math.PI * 2) / segments);
            worldrenderer.func_181662_b(centerX + Math.cos(d5) * radius, centerY, centerZ + Math.sin(d5) * radius)
                .func_181675_d();
        }

        tessellator.func_78381_a();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0F);
        GlStateManager.func_179117_G();
    }

    public static void drawCircle(
        double x, double y, double z, double radius, int sides, float lineWidth, int color, boolean chroma
    ) {
        float a = (color >> 24 & 0xFF) / 255.0F;
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        mc.field_71460_t.func_175072_h();
        GL11.glDisable(3553);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(2929);
        GL11.glEnable(2848);
        GL11.glDepthMask(false);
        GL11.glLineWidth(lineWidth);
        if (!chroma) {
            GL11.glColor4f(r, g, b, a);
        }

        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(1, DefaultVertexFormats.field_181705_e);
        long d = 0L;
        long ed = 15000L / sides;
        long hed = ed / 2L;

        for (int i = 0; i < sides * 2; i++) {
            if (chroma) {
                if (i % 2 != 0) {
                    if (i == 47) {
                        d = hed;
                    }

                    d += ed;
                }

                int c = ColorUtil.getChroma(2L, d);
                float r2 = (c >> 16 & 0xFF) / 255.0F;
                float g2 = (c >> 8 & 0xFF) / 255.0F;
                float b2 = (c & 0xFF) / 255.0F;
                GL11.glColor3f(r2, g2, b2);
            }

            double angle = (Math.PI * 2) * i / sides + Math.toRadians(180.0);
            worldrenderer.func_181662_b(x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius).func_181675_d();
        }

        tessellator.func_78381_a();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDepthMask(true);
        GL11.glDisable(2848);
        GL11.glEnable(2929);
        GL11.glDisable(3042);
        GL11.glEnable(3553);
        mc.field_71460_t.func_180436_i();
    }

    public static void draw3DRect(float x1, float y1, float x2, float y2) {
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(9, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(x2, y1, 0.0).func_181675_d();
        worldrenderer.func_181662_b(x1, y1, 0.0).func_181675_d();
        worldrenderer.func_181662_b(x1, y2, 0.0).func_181675_d();
        worldrenderer.func_181662_b(x2, y2, 0.0).func_181675_d();
        tessellator.func_78381_a();
    }
}
