package miau.ui.clickgui.components.impl;

import java.awt.Color;
import miau.ui.clickgui.components.Component;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.math.MathUtil;
import miau.util.render.ColorUtil;
import miau.util.render.RenderUtil;
import miau.util.render.Themes;
import miau.util.shader.RoundedUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

public class ThemeSelectComponent extends Component {
    private final CategoryComponent categoryComponent;
    private final Themes theme;
    public float o;
    public float x;
    public float y;
    private boolean hovered = false;
    private float hoverAnim = 0.0F;
    private float selectAnim = 0.0F;
    private long lastMS = System.currentTimeMillis();

    public ThemeSelectComponent(CategoryComponent categoryComponent, float o, Themes theme) {
        this.categoryComponent = categoryComponent;
        this.theme = theme;
        this.o = o;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.x = this.categoryComponent.getX();
        this.y = this.categoryComponent.getModuleY() + this.o;
        this.hovered = mouseX > this.x
            && mouseX < this.x + this.categoryComponent.getWidth()
            && mouseY > this.y
            && mouseY < this.y + this.getHeightF();
    }

    @Override
    public void render() {
        long currentMS = System.currentTimeMillis();
        float delta = (float)(currentMS - this.lastMS);
        this.lastMS = currentMS;
        if (delta > 50.0F || delta < 0.0F) {
            delta = 16.0F;
        }

        boolean isActive = Themes.getCurrentTheme() == this.theme;
        this.hoverAnim = MathUtil.lerp(this.hoverAnim, this.hovered ? 1.0F : 0.0F, 0.03F * delta);
        this.selectAnim = MathUtil.lerp(this.selectAnim, isActive ? 1.0F : 0.0F, 0.03F * delta);
        float cx = this.categoryComponent.getX() + 4.0F;
        float cy = this.categoryComponent.getY() + this.o + 2.0F;
        float w = this.categoryComponent.getWidth() - 8.0F;
        float h = 42.0F;
        float gradientH = 22.0F + this.hoverAnim * 20.0F;
        int bgAlpha = (int)(150.0F + this.hoverAnim * 50.0F);
        int bgColor = new Color(18, 21, 30, bgAlpha).getRGB();
        RenderUtil.drawRoundedRectangle(cx, cy, cx + w, cy + h, 6.0F, bgColor);
        Color c1 = ColorUtil.interpolate(
            this.hoverAnim, this.theme.getFirstColor(), this.theme.getFirstColor().brighter()
        );
        Color c2 = ColorUtil.interpolate(
            this.hoverAnim, this.theme.getSecondColor(), this.theme.getSecondColor().brighter()
        );
        long offsetMS = currentMS / 15L % 360L;
        if (this.hoverAnim > 0.01F) {
            float hue = (float)(offsetMS % 360L) / 360.0F;
            Color popC1 = Color.getHSBColor(hue, 0.8F, 1.0F);
            Color popC2 = Color.getHSBColor((hue + 0.5F) % 1.0F, 0.8F, 1.0F);
            c1 = ColorUtil.interpolate(this.hoverAnim, c1, popC1);
            c2 = ColorUtil.interpolate(this.hoverAnim, c2, popC2);
        }

        RoundedUtils.drawGradientHorizontal(cx, cy, w, gradientH, 6.0F, c1, c2);
        Font font = FontRepository.getHudFont(18);
        Color textColor = ColorUtil.interpolate(this.selectAnim, Color.WHITE, this.theme.getFirstColor());
        font.drawCentered(
            this.theme.getThemeName(), cx + w / 2.0F, cy + 22.0F + 8.0F + this.hoverAnim * 2.0F, textColor.getRGB()
        );
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.hovered && this.categoryComponent.opened) {
            Themes.setCurrentTheme(this.theme);
            return true;
        } else {
            return false;
        }
    }

    private void drawHorizontalGradient(float left, float top, float right, float bottom, int startColor, int endColor) {
        float f = (startColor >> 24 & 0xFF) / 255.0F;
        float f1 = (startColor >> 16 & 0xFF) / 255.0F;
        float f2 = (startColor >> 8 & 0xFF) / 255.0F;
        float f3 = (startColor & 0xFF) / 255.0F;
        float f4 = (endColor >> 24 & 0xFF) / 255.0F;
        float f5 = (endColor >> 16 & 0xFF) / 255.0F;
        float f6 = (endColor >> 8 & 0xFF) / 255.0F;
        float f7 = (endColor & 0xFF) / 255.0F;
        GlStateManager.func_179090_x();
        GlStateManager.func_179147_l();
        GlStateManager.func_179118_c();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        GlStateManager.func_179103_j(7425);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        worldrenderer.func_181662_b(left, top, 0.0).func_181666_a(f1, f2, f3, f).func_181675_d();
        worldrenderer.func_181662_b(left, bottom, 0.0).func_181666_a(f1, f2, f3, f).func_181675_d();
        worldrenderer.func_181662_b(right, bottom, 0.0).func_181666_a(f5, f6, f7, f4).func_181675_d();
        worldrenderer.func_181662_b(right, top, 0.0).func_181666_a(f5, f6, f7, f4).func_181675_d();
        tessellator.func_78381_a();
        GlStateManager.func_179103_j(7424);
        GlStateManager.func_179084_k();
        GlStateManager.func_179141_d();
        GlStateManager.func_179098_w();
    }

    @Override
    public void updateHeight(float n) {
        this.o = n;
    }

    @Override
    public float getHeightF() {
        return 46.0F + this.hoverAnim * 12.0F;
    }

    @Override
    public float getOffset() {
        return this.o;
    }
}
