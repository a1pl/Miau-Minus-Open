package miau.ui.clickgui.demise.component.impl;

import java.awt.Color;
import miau.property.properties.TextProperty;
import miau.ui.clickgui.demise.Component;
import miau.util.demise.MouseUtils;
import miau.util.font.FontRepository;

public class StringComponent extends Component {
    private final TextProperty setting;
    private float inputAnim = 0.0F;
    private boolean inputting;
    private String text = "";

    public StringComponent(TextProperty setting) {
        this.setting = setting;
        this.setHeight(FontRepository.getFont("Inter Regular", 14.0F).height() * 2.0F + 4.0F);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.inputAnim = this.animate(this.inputAnim, this.inputting ? 1.0F : 0.0F, 0.15F);
        this.text = this.setting.getValue();
        if (this.text == null) {
            this.text = "";
        }

        String textToDraw = this.text.isEmpty() && !this.inputting ? "Empty..." : this.text;
        FontRepository.getFont("Inter Regular", 14.0F)
            .draw(this.setting.getName(), this.getX() + 4.0F, this.getY(), -1);
        this.drawTextWithLineBreaks(
            textToDraw
                + (this.inputting && this.text.length() < 59 && System.currentTimeMillis() % 1000L > 500L ? "|" : ""),
            this.getX() + 6.0F,
            this.getY() + FontRepository.getFont("Inter Regular", 14.0F).height() + 2.0F
        );
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (MouseUtils.isHovered(
                this.getX(),
                this.getY() + FontRepository.getFont("Inter Regular", 14.0F).height() + 4.0F,
                this.getWidth(),
                4.0F,
                mouseX,
                mouseY
            )
            && mouseButton == 0) {
            this.inputting = !this.inputting;
        } else {
            this.inputting = false;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.inputting) {
            if (keyCode == 14) {
                this.deleteLastCharacter();
            }

            if (Character.isLetterOrDigit(typedChar) || keyCode == 57) {
                this.text = this.text + typedChar;
                this.setting.setValue(this.text);
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void drawTextWithLineBreaks(String text, float x, float y) {
        String[] lines = text.split("\n");
        float currentY = y;

        for (String line : lines) {
            Color color = this.interpolateColorC(new Color(-1).darker(), new Color(-1), this.inputAnim);
            FontRepository.getFont("Inter Regular", 15.0F).draw(line, x, currentY, color.getRGB());
            currentY += FontRepository.getFont("Inter Regular", 15.0F).height();
        }
    }

    private void deleteLastCharacter() {
        if (!this.text.isEmpty()) {
            this.text = this.text.substring(0, this.text.length() - 1);
            this.setting.setValue(this.text);
        }
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
