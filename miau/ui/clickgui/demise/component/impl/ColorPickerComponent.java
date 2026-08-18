package miau.ui.clickgui.demise.component.impl;

import java.awt.Color;
import miau.property.properties.ColorProperty;
import miau.ui.clickgui.demise.Component;
import miau.util.demise.MouseUtils;
import miau.util.demise.RoundedUtils;
import miau.util.font.FontRepository;
import miau.util.render.ShapeUtil;

public class ColorPickerComponent extends Component {
    private final ColorProperty setting;
    private float open = 0.0F;
    private boolean opened;
    private boolean pickingHue;
    private boolean pickingOthers;
    private boolean pickingAlpha;
    private float hue;
    private float saturation;
    private float brightness;
    private float alpha;

    public ColorPickerComponent(ColorProperty setting) {
        this.setting = setting;
        Color c = new Color(setting.getValue());
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        this.alpha = 1.0F;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.open = this.animate(this.open, this.opened ? 1.0F : 0.0F, 0.15F);
        this.setHeight(
            FontRepository.getFont("Inter Regular", 15.0F).height()
                + (FontRepository.getFont("Inter Regular", 15.0F).height() + 2.0F + 45.0F + 2.0F + 12.0F) * this.open
        );
        FontRepository.getFont("Inter Regular", 15.0F)
            .draw(this.setting.getName(), this.getX() + 4.0F, this.getY(), -1);
        RoundedUtils.drawRound(
            this.getX() + this.getWidth() - 18.0F,
            this.getY(),
            15.0F,
            FontRepository.getFont("Inter Regular", 15.0F).height() - 3.0F,
            2.0F,
            new Color(this.setting.getValue())
        );
        if (this.opened) {
            RoundedUtils.drawGradientRound(
                this.getX() + 2.0F,
                this.getY() + FontRepository.getFont("Inter Regular", 15.0F).height() + 2.0F,
                this.getWidth() - 4.0F,
                45.0F * this.open,
                4.0F,
                Color.BLACK,
                Color.WHITE,
                Color.BLACK,
                Color.getHSBColor(this.hue, 1.0F, 1.0F)
            );
            int max = (int)(this.getWidth() - 8.0F);

            for (int i = 0; i < max; i++) {
                RoundedUtils.drawRound(
                    this.getX() + i + 4.0F,
                    this.getY()
                        + FontRepository.getFont("Inter Regular", 15.0F).height()
                        + 2.0F
                        + 45.0F * this.open
                        + 4.0F,
                    2.0F,
                    4.0F,
                    2.0F,
                    Color.getHSBColor((float)i / max, 1.0F, 1.0F)
                );
            }

            float alphaSliderY = this.getY()
                + FontRepository.getFont("Inter Regular", 15.0F).height()
                + 2.0F
                + 45.0F * this.open
                + 12.0F;
            this.drawCheckerboard(this.getX() + 4.0F, alphaSliderY, this.getWidth() - 8.0F, 4.0F);
            int maxx = (int)(this.getWidth() - 8.0F);

            for (int i = 0; i < maxx; i++) {
                float alphaValue = (float)i / maxx;
                Color alphaColor = new Color(
                    new Color(this.setting.getValue()).getRed(),
                    new Color(this.setting.getValue()).getGreen(),
                    new Color(this.setting.getValue()).getBlue(),
                    (int)(alphaValue * 255.0F)
                );
                RoundedUtils.drawRound(this.getX() + i + 4.0F, alphaSliderY, 2.0F, 4.0F, 1.0F, alphaColor);
            }

            float sliderX = this.getX() + 4.0F;
            float sliderWidth = this.getWidth() - 8.0F;
            float alphaHandleX = sliderX + sliderWidth * this.alpha;
            alphaHandleX = Math.max(sliderX + 2.0F, Math.min(sliderX + sliderWidth - 2.0F, alphaHandleX));
            ShapeUtil.drawFilledCircle(alphaHandleX, alphaSliderY + 2.0F, 2.0, -1);
            float gradientX = this.getX() + 4.0F;
            float gradientY = this.getY() + FontRepository.getFont("Inter Regular", 15.0F).height() + 2.0F;
            float gradientWidth = this.getWidth() - 8.0F;
            float gradientHeight = 45.0F * this.open;
            float pickerY = gradientY + gradientHeight * (1.0F - this.brightness);
            float pickerX = gradientX + (gradientWidth * this.saturation - 1.0F);
            pickerY = Math.max(Math.min(gradientY + gradientHeight - 2.0F, pickerY), gradientY - 2.0F);
            pickerX = Math.max(Math.min(gradientX + gradientWidth - 2.0F, pickerX), gradientX - 2.0F);
            if (this.pickingHue) {
                this.hue = Math.min(1.0F, Math.max(0.0F, (mouseX - gradientX) / gradientWidth));
                this.updateColor();
            }

            if (this.pickingOthers) {
                this.brightness = Math.min(1.0F, Math.max(0.0F, 1.0F - (mouseY - gradientY) / gradientHeight));
                this.saturation = Math.min(1.0F, Math.max(0.0F, (mouseX - gradientX) / gradientWidth));
                this.updateColor();
            }

            if (this.pickingAlpha) {
                float newAlpha = (mouseX - sliderX) / sliderWidth;
                newAlpha = Math.max(0.0F, Math.min(1.0F, newAlpha));
                this.alpha = newAlpha;
                this.updateColor();
            }

            ShapeUtil.drawFilledCircle(pickerX, pickerY, 2.0, -1);
        }

        super.drawScreen(mouseX, mouseY);
    }

    private void updateColor() {
        int rgb = Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
        int argb = rgb & 16777215 | (int)(this.alpha * 255.0F) << 24;
        this.setting.setValue(rgb);
    }

    private void drawCheckerboard(float x, float y, float width, float height) {
        RoundedUtils.drawRound(x, y, width, height, 2.0F, new Color(200, 200, 200));
        int squareSize = 4;
        boolean white = true;

        for (int i = 0; i < width; i += squareSize) {
            for (int j = 0; j < height; j += squareSize) {
                if (!white) {
                    Color color = new Color(150, 150, 150);
                    float drawWidth = Math.min(squareSize, width - i);
                    float drawHeight = Math.min(squareSize, height - j);
                    if (i > 2 && i < width - 2.0F || j > 0 && j < height - 0.0F) {
                        RoundedUtils.drawRound(x + i, y + j, drawWidth, drawHeight, 0.0F, color);
                    }
                }

                white = !white;
            }

            if (height / squareSize % 2.0F == 0.0F) {
                white = !white;
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (MouseUtils.isHovered(
            this.getX() + this.getWidth() - 18.0F,
            this.getY(),
            15.0F,
            FontRepository.getFont("Inter Regular", 15.0F).height(),
            mouseX,
            mouseY
        )) {
            this.opened = !this.opened;
        }

        if (this.opened) {
            if (MouseUtils.isHovered(
                this.getX() + 4.0F,
                this.getY() + FontRepository.getFont("Inter Regular", 15.0F).height() + 2.0F,
                this.getWidth() - 8.0F,
                45.0F * this.open,
                mouseX,
                mouseY
            )) {
                this.pickingOthers = true;
            }

            if (MouseUtils.isHovered(
                this.getX() + 4.0F,
                this.getY() + FontRepository.getFont("Inter Regular", 15.0F).height() + 2.0F + 45.0F * this.open + 4.0F,
                this.getWidth() - 8.0F,
                6.0F,
                mouseX,
                mouseY
            )) {
                this.pickingHue = true;
            }

            float alphaSliderY = this.getY()
                + FontRepository.getFont("Inter Regular", 15.0F).height()
                + 2.0F
                + 45.0F * this.open
                + 12.0F;
            if (MouseUtils.isHovered(this.getX() + 4.0F, alphaSliderY, this.getWidth() - 8.0F, 6.0F, mouseX, mouseY)) {
                this.pickingAlpha = true;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            this.pickingHue = false;
            this.pickingOthers = false;
            this.pickingAlpha = false;
        }

        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean isVisible() {
        return this.setting.isVisible();
    }

    @Override
    public boolean isChild() {
        return false;
    }

    private float animate(float current, float target, float speed) {
        return current + (target - current) / Math.max(1.0F, speed * 10.0F);
    }
}
