package miau.ui.clickgui.demise.component.impl;

import java.awt.Color;
import miau.property.properties.BooleanProperty;
import miau.ui.clickgui.demise.Component;
import miau.util.demise.MouseUtils;
import miau.util.demise.RoundedUtils;
import miau.util.font.FontRepository;

public class BooleanComponent extends Component {
    private final BooleanProperty setting;
    private float toggleAnimation = 0.0F;

    public BooleanComponent(BooleanProperty setting) {
        this.setting = setting;
        this.setHeight(FontRepository.getFont("Inter Regular", 15.0F).height() + 5.0F);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.toggleAnimation = this.animate(this.toggleAnimation, this.setting.getValue() ? 1.0F : 0.0F, 0.15F);
        FontRepository.getFont("Inter Regular", 15.0F)
            .draw(this.setting.getName(), this.getX() + 4.0F, this.getY() + 2.5F, -1);
        RoundedUtils.drawRound(
            this.getX() + this.getWidth() - 15.5F,
            this.getY() + 2.5F,
            13.0F,
            5.0F,
            2.25F,
            this.interpolateColorC(new Color(128, 128, 128, 255), Color.white, this.toggleAnimation).darker()
        );
        RoundedUtils.drawRound(
            this.getX() + this.getWidth() - 15.5F + 8.0F * this.toggleAnimation,
            this.getY() + 2.5F,
            5.0F,
            5.0F,
            2.25F,
            Color.WHITE
        );
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (MouseUtils.isHovered(this.getX() + this.getWidth() - 17.5F, this.getY() + 2.5F, 12.5F, 5.0F, mouseX, mouseY)
            && mouseButton == 0) {
            this.setting.setValue(!this.setting.getValue());
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean isVisible() {
        return this.setting.isVisible();
    }

    @Override
    public boolean isChild() {
        return false;
    }

    private Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1.0F, Math.max(0.0F, amount));
        return new Color(
            (int)(color1.getRed() + (color2.getRed() - color1.getRed()) * amount),
            (int)(color1.getGreen() + (color2.getGreen() - color1.getGreen()) * amount),
            (int)(color1.getBlue() + (color2.getBlue() - color1.getBlue()) * amount),
            (int)(color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * amount)
        );
    }

    private float animate(float current, float target, float speed) {
        return current + (target - current) / Math.max(1.0F, speed * 10.0F);
    }
}
