package miau.ui.clickgui.components.impl;

import java.awt.Color;
import miau.property.properties.ColorProperty;
import miau.ui.clickgui.components.Component;
import miau.util.animation.AnimationTimer;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import org.lwjgl.opengl.GL11;

public class ColorComponent extends Component {
    public ColorProperty property;
    private ModuleComponent moduleComponent;
    public float o;
    public float x;
    private float y;
    public float xOffset;
    public boolean expanded;
    private int dragMode;
    private float cachedHue;
    private float cachedSat;
    private float cachedBri;
    private AnimationTimer smoothTimer;
    private float animationProgress;
    private float animationStartProgress;
    private float animationTargetProgress;
    private static final float ANIMATION_DURATION = 250.0F;
    private static final float LABEL_HEIGHT = 12.0F;
    private static final float SQUARE_SIZE = 50.0F;
    private static final float HUE_BAR_WIDTH = 10.0F;
    private static final float HUE_GAP = 4.0F;
    private static final float BLACK_BRI_EPSILON = 0.001F;
    private static final float GREY_SAT_EPSILON = 0.001F;
    private static final float SQUARE_TOP_PAD = 2.0F;
    private static final float BOTTOM_PAD = 2.0F;
    private static final int HUE_STEPS = 20;
    private static final float PREVIEW_BOX_SIZE = 5.0F;

    public ColorComponent(ColorProperty property, ModuleComponent moduleComponent, float o) {
        this.property = property;
        this.moduleComponent = moduleComponent;
        this.o = o;
        this.animationProgress = 0.0F;
        this.animationStartProgress = 0.0F;
        this.animationTargetProgress = 0.0F;
    }

    public boolean hasAlpha() {
        return false;
    }

    public int getColorRGB() {
        return this.property.getValue() | 0xFF000000;
    }

    public float getHue() {
        int rgb = this.property.getValue();
        float[] hsb = Color.RGBtoHSB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, null);
        return hsb[0] * 360.0F;
    }

    public float getSaturation() {
        int rgb = this.property.getValue();
        float[] hsb = Color.RGBtoHSB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, null);
        return hsb[1];
    }

    public float getBrightness() {
        int rgb = this.property.getValue();
        float[] hsb = Color.RGBtoHSB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, null);
        return hsb[2];
    }

    public void setFromHSB(float h, float s, float b) {
        int rgb = Color.HSBtoRGB(h / 360.0F, s, b) & 16777215;
        this.property.setValue(rgb);
    }

    public float getExpandedHeight() {
        return 66.0F;
    }

    public float getAnimationProgress() {
        if (this.smoothTimer != null) {
            if ((float)(System.currentTimeMillis() - this.smoothTimer.last) >= 280.0F) {
                this.smoothTimer = null;
                this.animationProgress = this.animationTargetProgress;
                this.animationStartProgress = this.animationTargetProgress;
            } else {
                this.animationProgress = this.smoothTimer
                    .getValueFloat(this.animationStartProgress, this.animationTargetProgress, 1);
                if (this.animationProgress == this.animationTargetProgress) {
                    this.smoothTimer = null;
                    this.animationStartProgress = this.animationTargetProgress;
                }
            }
        }

        return this.animationProgress;
    }

    @Override
    public void render() {
        float cx = this.moduleComponent.categoryComponent.getX();
        float cy = this.moduleComponent.categoryComponent.getY();
        float cw = this.moduleComponent.categoryComponent.getWidth();
        float boxX = cx + 4.0F + this.xOffset / 2.0F;
        float boxY = cy + this.o + 3.0F;
        RenderUtil.drawRect(boxX - 0.5, boxY - 0.5, boxX + 5.0F + 0.5, boxY + 5.0F + 0.5, -12829626);
        RenderUtil.drawRect(boxX, boxY, boxX + 5.0F, boxY + 5.0F, this.getColorRGB());
        Font renderer = FontRepository.getClickGuiFont();
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        float textOffset = renderer.getStringWidth("[+]  ");
        renderer.draw(
            this.property.getName(),
            (cx + 4.0F) * 2.0F + this.xOffset + textOffset,
            (cy + this.o + 4.0F) * 2.0F,
            -1,
            true
        );
        GL11.glPopMatrix();
        float progress = this.getAnimationProgress();
        if (!(progress <= 0.0F)) {
            float scrollOffset = this.moduleComponent.categoryComponent.moduleY - cy;
            float contentTopScreen = cy + this.o + 12.0F + scrollOffset;
            float revealH = (this.getExpandedHeight() - 12.0F) * progress;
            RenderUtil.scissorPushGui(cx, contentTopScreen, cw, revealH);
            this.renderPickerContent(cx, cy);
            RenderUtil.scissorPop();
        }
    }

    private void renderPickerContent(float cx, float cy) {
        float areaLeft = cx + 4.0F + this.xOffset / 2.0F;
        float sqTop = cy + this.o + 12.0F + 2.0F;
        float sqRight = areaLeft + 50.0F;
        float sqBottom = sqTop + 50.0F;
        float bri = this.dragMode != 0 ? this.cachedBri : this.getBrightness();
        float satFromSetting = this.dragMode != 0 ? this.cachedSat : this.getSaturation();
        boolean isBlack = bri < 0.001F;
        boolean isGrey = satFromSetting < 0.001F;
        if (this.dragMode == 0 && !isBlack) {
            this.cachedBri = bri;
            this.cachedSat = this.getSaturation();
            if (!isGrey) {
                this.cachedHue = this.getHue();
            }
        }

        boolean useCachedHue = this.dragMode != 0 || isBlack || isGrey;
        float hue = useCachedHue ? this.cachedHue / 360.0F : this.getHue() / 360.0F;
        float sat = this.dragMode == 0 && !isBlack ? satFromSetting : this.cachedSat;
        int hueRGB = Color.HSBtoRGB(hue, 1.0F, 1.0F) | 0xFF000000;
        RenderUtil.drawRect(areaLeft, sqTop, sqRight, sqBottom, hueRGB);
        RenderUtil.drawHorizontalGradientRect(areaLeft, sqTop, sqRight, sqBottom, -1, 16777215);
        RenderUtil.drawVerticalGradientRect(areaLeft, sqTop, sqRight, sqBottom, 0, -16777216);
        RenderUtil.drawOutline(areaLeft - 1.0F, sqTop - 1.0F, sqRight + 1.0F, sqBottom + 1.0F, 1.0F, -12829626);
        float indX = areaLeft + sat * 50.0F;
        float indY = sqTop + (1.0F - bri) * 50.0F;
        RenderUtil.drawRect(indX - 2.0F, indY, indX + 3.0F, indY + 1.0F, -1);
        RenderUtil.drawRect(indX, indY - 2.0F, indX + 1.0F, indY + 3.0F, -1);
        float hueLeft = sqRight + 4.0F;
        float hueRight = hueLeft + 10.0F;
        float stepH = 2.5F;

        for (int i = 0; i < 20; i++) {
            float h1 = i / 20.0F;
            float h2 = (i + 1) / 20.0F;
            int c1 = Color.HSBtoRGB(h1, 1.0F, 1.0F) | 0xFF000000;
            int c2 = Color.HSBtoRGB(h2, 1.0F, 1.0F) | 0xFF000000;
            RenderUtil.drawVerticalGradientRect(hueLeft, sqTop + i * stepH, hueRight, sqTop + (i + 1) * stepH, c1, c2);
        }

        RenderUtil.drawOutline(hueLeft - 1.0F, sqTop - 1.0F, hueRight + 1.0F, sqBottom + 1.0F, 1.0F, -12829626);
        float hueIndY = sqTop + Math.max(0.0F, Math.min(1.0F, hue)) * 50.0F;
        RenderUtil.drawRect(hueLeft - 1.0F, hueIndY - 1.0F, hueRight + 1.0F, hueIndY + 2.0F, -1);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        this.x = this.moduleComponent.categoryComponent.getX();
        if (this.dragMode != 0 && !(this.getAnimationProgress() < 1.0F)) {
            float areaLeft = this.x + 4.0F + this.xOffset / 2.0F;
            float sqTop = this.y + 12.0F + 2.0F;
            float sqRight = areaLeft + 50.0F;
            float sqBottom = sqTop + 50.0F;
            if (this.dragMode == 1) {
                this.cachedSat = Math.max(0.0F, Math.min(1.0F, (mouseX - areaLeft) / 50.0F));
                this.cachedBri = Math.max(0.0F, Math.min(1.0F, 1.0F - (mouseY - sqTop) / 50.0F));
                this.setFromHSB(this.cachedHue, this.cachedSat, this.cachedBri);
            } else if (this.dragMode == 2) {
                this.cachedHue = Math.max(0.0F, Math.min(360.0F, (mouseY - sqTop) / 50.0F * 360.0F));
                this.setFromHSB(this.cachedHue, this.cachedSat, this.cachedBri);
            }
        }
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int button) {
        if (this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
            float cw = this.moduleComponent.categoryComponent.getWidth();
            if (!(mouseX > this.x)
                || !(mouseX < this.x + cw)
                || !(mouseY > this.y)
                || !(mouseY < this.y + 12.0F)
                || button != 0 && button != 1) {
                if (button != 0) {
                    return false;
                } else if (this.getAnimationProgress() < 1.0F) {
                    return false;
                } else {
                    float areaLeft = this.x + 4.0F + this.xOffset / 2.0F;
                    float sqTop = this.y + 12.0F + 2.0F;
                    float sqRight = areaLeft + 50.0F;
                    float sqBottom = sqTop + 50.0F;
                    float hueLeft = sqRight + 4.0F;
                    float hueRight = hueLeft + 10.0F;
                    if (mouseX >= areaLeft && mouseX <= sqRight && mouseY >= sqTop && mouseY <= sqBottom) {
                        this.cacheHSB();
                        this.dragMode = 1;
                        return false;
                    } else if (mouseX >= hueLeft - 2.0F
                        && mouseX <= hueRight + 2.0F
                        && mouseY >= sqTop
                        && mouseY <= sqBottom) {
                        this.cacheHSB();
                        this.dragMode = 2;
                        return false;
                    } else {
                        return false;
                    }
                }
            } else {
                float currentProgress = this.getAnimationProgress();
                this.animationStartProgress = currentProgress;
                this.expanded = !this.expanded;
                this.animationTargetProgress = this.expanded ? 1.0F : 0.0F;
                (this.smoothTimer = new AnimationTimer(250.0F)).start();
                this.moduleComponent.updateSettingPositions();
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        this.dragMode = 0;
    }

    @Override
    public void onGuiClosed() {
        this.dragMode = 0;
        this.smoothTimer = null;
        this.animationProgress = this.expanded ? 1.0F : 0.0F;
        this.animationStartProgress = this.animationProgress;
        this.animationTargetProgress = this.animationProgress;
    }

    @Override
    public void updateHeight(float n) {
        this.o = n;
    }

    @Override
    public float getOffset() {
        return this.o;
    }

    @Override
    public boolean isBaseVisible() {
        return this.property.isVisible();
    }

    public void restoreExpandedState(boolean expanded) {
        this.expanded = expanded;
        this.smoothTimer = null;
        this.animationProgress = expanded ? 1.0F : 0.0F;
        this.animationStartProgress = this.animationProgress;
        this.animationTargetProgress = this.animationProgress;
    }

    private void cacheHSB() {
        float bri = this.getBrightness();
        float sat = this.getSaturation();
        this.cachedBri = bri;
        if (bri >= 0.001F) {
            this.cachedSat = sat;
            if (sat >= 0.001F) {
                this.cachedHue = this.getHue();
            }
        }
    }
}
