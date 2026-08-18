package miau.ui.clickgui.demise.component.impl;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import miau.property.properties.FloatProperty;
import miau.ui.clickgui.demise.Component;
import miau.util.demise.MouseUtils;
import miau.util.demise.RoundedUtils;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.render.ShapeUtil;

public class SliderComponent extends Component {
    private final FloatProperty setting;
    private float anim;
    private float anim2;
    private boolean dragging;
    private boolean dragging2;
    private float previousSetting;
    private final Font font;
    private final Color colorDarkest = Color.white.darker().darker().darker().darker();
    private final Color colorDarker = Color.white.darker().darker();
    private final int colorBrighter = Color.white.brighter().brighter().getRGB();
    private final int colorGray = new Color(160, 160, 160).getRGB();

    public SliderComponent(FloatProperty setting) {
        this.setting = setting;
        this.font = FontRepository.getFont("Inter Regular", 15.0F);
        this.previousSetting = setting.getValue();
        this.setHeight(this.font.height() * 2.0F + this.font.height() + 2.0F);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.font.draw(this.setting.getName(), this.getX() + 4.0F, this.getY(), -1);
        this.anim = this.animate(
            this.anim,
            (this.getWidth() - 8.0F)
                * (this.setting.getValue() - this.setting.getMin())
                / (this.setting.getMax() - this.setting.getMin()),
            15.0F
        );
        if (this.setting.isDoubleSlider() && this.setting.getSecondValue() != null) {
            this.anim2 = this.animate(
                this.anim2,
                (this.getWidth() - 8.0F)
                    * (this.setting.getSecondValue() - this.setting.getMin())
                    / (this.setting.getMax() - this.setting.getMin()),
                15.0F
            );
        }

        float sliderWidth = this.anim;
        RoundedUtils.drawRound(
            this.getX() + 4.0F,
            this.getY() + this.font.height() + 2.0F,
            this.getWidth() - 8.0F,
            2.0F,
            1.0F,
            this.colorDarkest
        );
        if (this.setting.isDoubleSlider()) {
            float minX = Math.min(this.anim, this.anim2);
            float maxX = Math.max(this.anim, this.anim2);
            RoundedUtils.drawRound(
                this.getX() + 4.0F + minX,
                this.getY() + this.font.height() + 2.0F,
                maxX - minX,
                2.0F,
                1.0F,
                this.colorDarker
            );
            ShapeUtil.drawFilledCircle(
                this.getX() + 4.0F + this.anim, this.getY() + this.font.height() + 3.0F, 3.0, this.colorBrighter
            );
            ShapeUtil.drawFilledCircle(
                this.getX() + 4.0F + this.anim2, this.getY() + this.font.height() + 3.0F, 3.0, this.colorBrighter
            );
        } else {
            RoundedUtils.drawRound(
                this.getX() + 4.0F, this.getY() + this.font.height() + 2.0F, sliderWidth, 2.0F, 1.0F, this.colorDarker
            );
            ShapeUtil.drawFilledCircle(
                this.getX() + 4.0F + sliderWidth, this.getY() + this.font.height() + 3.0F, 3.0, this.colorBrighter
            );
        }

        this.font
            .draw(
                String.valueOf(this.setting.getMin()),
                this.getX() + 2.0F,
                this.getY() + this.font.height() * 2.0F + 2.0F,
                this.colorGray
            );
        this.font
            .drawCentered(
                this.setting.isDoubleSlider()
                    ? this.setting.getValue() + " - " + this.setting.getSecondValue()
                    : String.valueOf(this.setting.getValue()),
                this.getX() + this.getWidth() / 2.0F,
                this.getY() + this.font.height() * 2.0F + 2.0F,
                -1
            );
        this.font
            .draw(
                String.valueOf(this.setting.getMax()),
                this.getX() - 2.0F + this.getWidth() - this.font.getStringWidth(String.valueOf(this.setting.getMax())),
                this.getY() + this.font.height() * 2.0F + 2.0F,
                this.colorGray
            );
        if (this.dragging || this.dragging2) {
            double clampedRatio = Math.max(
                0.0, Math.min(1.0, (double)(mouseX - this.getX() - 4.0F) / (this.getWidth() - 8.0F))
            );
            double difference = this.setting.getMax() - this.setting.getMin();
            double value = this.setting.getMin().floatValue() + clampedRatio * difference;
            float newVal = BigDecimal.valueOf(incValue(value, 1.0)).setScale(1, RoundingMode.CEILING).floatValue();
            if (this.dragging) {
                this.setting.setValue(newVal);
            } else {
                this.setting.setSecondValue(newVal);
            }
        }
    }

    public static double incValue(double value, double increment) {
        return Math.round(value / increment) * increment;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0
            && MouseUtils.isHovered(
                this.getX() + 2.0F, this.getY() + this.font.height() + 2.0F, this.getWidth(), 4.0F, mouseX, mouseY
            )) {
            if (this.setting.isDoubleSlider()) {
                float mouseRelX = mouseX - (this.getX() + 4.0F);
                if (Math.abs(mouseRelX - this.anim) <= Math.abs(mouseRelX - this.anim2)) {
                    this.dragging = true;
                } else {
                    this.dragging2 = true;
                }
            } else {
                this.dragging = true;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            this.dragging = false;
            this.dragging2 = false;
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
        return current + (target - current) / Math.max(1.0F, speed);
    }
}
