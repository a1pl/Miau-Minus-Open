package miau.util.demise;

import java.awt.Color;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

public class RoundedUtils {
    public static DemiseShaderUtil roundedShader = new DemiseShaderUtil("roundedRect");
    public static DemiseShaderUtil roundedOutlineShader = new DemiseShaderUtil("roundRectOutline");
    private static final DemiseShaderUtil roundedTexturedShader = new DemiseShaderUtil("roundRectTexture");
    private static final DemiseShaderUtil roundedGradientShader = new DemiseShaderUtil("roundedRectGradient");

    public static void drawRound(float x, float y, float width, float height, float radius, Color color) {
        drawRound(x, y, width, height, radius, false, color);
    }

    public static void drawShaderRound(float x, float y, float width, float height, float radius, Color color) {
        drawShaderRound(x, y, width, height, radius, false, color);
    }

    public static void drawGradientHorizontal(
        float x, float y, float width, float height, float radius, Color left, Color right
    ) {
        drawGradientRound(x, y, width, height, radius, left, left, right, right);
    }

    public static void drawGradientVertical(
        float x, float y, float width, float height, float radius, Color top, Color bottom
    ) {
        drawGradientRound(x, y, width, height, radius, bottom, top, bottom, top);
    }

    public static void drawGradientCornerLR(
        float x, float y, float width, float height, float radius, Color topLeft, Color bottomRight
    ) {
        Color mixedColor = RenderUtil.interpolateColorC(topLeft, bottomRight, 0.5F);
        drawGradientRound(x, y, width, height, radius, mixedColor, topLeft, bottomRight, mixedColor);
    }

    public static void drawGradientCornerRL(
        float x, float y, float width, float height, float radius, Color bottomLeft, Color topRight
    ) {
        Color mixedColor = RenderUtil.interpolateColorC(topRight, bottomLeft, 0.5F);
        drawGradientRound(x, y, width, height, radius, bottomLeft, mixedColor, mixedColor, topRight);
    }

    public static void drawGradientRound(
        float x,
        float y,
        float width,
        float height,
        float radius,
        Color bottomLeft,
        Color topLeft,
        Color bottomRight,
        Color topRight
    ) {
        RenderUtil.setAlphaLimit(0.0F);
        RenderUtil.resetColor();
        RenderUtil.startBlend();
        roundedGradientShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedGradientShader);
        roundedGradientShader.setUniformf(
            "color1",
            topLeft.getRed() / 255.0F,
            topLeft.getGreen() / 255.0F,
            topLeft.getBlue() / 255.0F,
            topLeft.getAlpha() / 255.0F
        );
        roundedGradientShader.setUniformf(
            "color2",
            bottomLeft.getRed() / 255.0F,
            bottomLeft.getGreen() / 255.0F,
            bottomLeft.getBlue() / 255.0F,
            bottomLeft.getAlpha() / 255.0F
        );
        roundedGradientShader.setUniformf(
            "color3",
            topRight.getRed() / 255.0F,
            topRight.getGreen() / 255.0F,
            topRight.getBlue() / 255.0F,
            topRight.getAlpha() / 255.0F
        );
        roundedGradientShader.setUniformf(
            "color4",
            bottomRight.getRed() / 255.0F,
            bottomRight.getGreen() / 255.0F,
            bottomRight.getBlue() / 255.0F,
            bottomRight.getAlpha() / 255.0F
        );
        DemiseShaderUtil.drawQuads(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F);
        roundedGradientShader.unload();
        RenderUtil.endBlend();
    }

    public static void drawRound(float x, float y, float width, float height, float radius, boolean blur, Color color) {
        RenderUtil.resetColor();
        RenderUtil.startBlend();
        GL11.glBlendFunc(770, 771);
        RenderUtil.setAlphaLimit(0.0F);
        roundedShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf(
            "color",
            color.getRed() / 255.0F,
            color.getGreen() / 255.0F,
            color.getBlue() / 255.0F,
            color.getAlpha() / 255.0F
        );
        DemiseShaderUtil.drawQuads(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F);
        roundedShader.unload();
        RenderUtil.endBlend();
    }

    public static void drawShaderRound(
        float x, float y, float width, float height, float radius, boolean blur, Color color
    ) {
        x += 0.2F;
        y += 0.2F;
        width -= 0.4F;
        height -= 0.4F;
        RenderUtil.resetColor();
        RenderUtil.startBlend();
        GL11.glBlendFunc(770, 771);
        RenderUtil.setAlphaLimit(0.0F);
        roundedShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf(
            "color",
            color.getRed() / 255.0F,
            color.getGreen() / 255.0F,
            color.getBlue() / 255.0F,
            color.getAlpha() / 255.0F
        );
        DemiseShaderUtil.drawQuads(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F);
        roundedShader.unload();
        RenderUtil.endBlend();
    }

    public static void drawRoundOutline(
        float x,
        float y,
        float width,
        float height,
        float radius,
        float outlineThickness,
        Color color,
        Color outlineColor
    ) {
        RenderUtil.resetColor();
        RenderUtil.startBlend();
        GL11.glBlendFunc(770, 771);
        RenderUtil.setAlphaLimit(0.0F);
        roundedOutlineShader.init();
        ScaledResolution sr = new ScaledResolution(Minecraft.func_71410_x());
        setupRoundedRectUniforms(x, y, width, height, radius, roundedOutlineShader);
        roundedOutlineShader.setUniformf("outlineThickness", outlineThickness * sr.func_78325_e());
        roundedOutlineShader.setUniformf(
            "color",
            color.getRed() / 255.0F,
            color.getGreen() / 255.0F,
            color.getBlue() / 255.0F,
            color.getAlpha() / 255.0F
        );
        roundedOutlineShader.setUniformf(
            "outlineColor",
            outlineColor.getRed() / 255.0F,
            outlineColor.getGreen() / 255.0F,
            outlineColor.getBlue() / 255.0F,
            outlineColor.getAlpha() / 255.0F
        );
        DemiseShaderUtil.drawQuads(
            x - (2.0F + outlineThickness),
            y - (2.0F + outlineThickness),
            width + (4.0F + outlineThickness * 2.0F),
            height + (4.0F + outlineThickness * 2.0F)
        );
        roundedOutlineShader.unload();
        RenderUtil.endBlend();
    }

    public static void drawRoundTextured(float x, float y, float width, float height, float radius, float alpha) {
        RenderUtil.resetColor();
        RenderUtil.setAlphaLimit(0.0F);
        RenderUtil.startBlend();
        roundedTexturedShader.init();
        roundedTexturedShader.setUniformi("textureIn", 0);
        setupRoundedRectUniforms(x, y, width, height, radius, roundedTexturedShader);
        roundedTexturedShader.setUniformf("alpha", alpha);
        DemiseShaderUtil.drawQuads(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F);
        roundedTexturedShader.unload();
        RenderUtil.endBlend();
    }

    private static void setupRoundedRectUniforms(
        float x, float y, float width, float height, float radius, DemiseShaderUtil roundedTexturedShader
    ) {
        ScaledResolution sr = new ScaledResolution(Minecraft.func_71410_x());
        roundedTexturedShader.setUniformf(
            "location",
            x * sr.func_78325_e(),
            Minecraft.func_71410_x().field_71440_d - height * sr.func_78325_e() - y * sr.func_78325_e()
        );
        roundedTexturedShader.setUniformf("rectSize", width * sr.func_78325_e(), height * sr.func_78325_e());
        roundedTexturedShader.setUniformf("radius", radius * sr.func_78325_e());
    }
}
