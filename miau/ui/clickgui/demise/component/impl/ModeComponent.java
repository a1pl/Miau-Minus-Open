package miau.ui.clickgui.demise.component.impl;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import miau.property.properties.ModeProperty;
import miau.ui.clickgui.demise.Component;
import miau.util.demise.MouseUtils;
import miau.util.font.FontRepository;

public class ModeComponent extends Component {
    private final ModeProperty setting;
    private final Map<String, Float> select = new HashMap<>();
    private boolean isExpanded = false;
    private float openProgress = 0.0F;

    public ModeComponent(ModeProperty setting) {
        this.setting = setting;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.openProgress = this.animate(this.openProgress, this.isExpanded ? 1.0F : 0.0F, 0.15F);
        float heightoff = 4.0F;
        float lineHeight = FontRepository.getFont("Inter Regular", 13.0F).height() + 2.0F;
        FontRepository.getFont("Inter SemiBold", 15.0F)
            .draw(this.setting.getName(), this.getX() + 4.0F, this.getY() + 2.0F, -1);
        FontRepository.getFont("Inter Regular", 13.0F)
            .draw(
                this.setting.getModeString(),
                this.getX()
                    + FontRepository.getFont("Inter SemiBold", 15.0F).getStringWidth(this.setting.getName())
                    + 10.0F,
                this.getY() + 3.0F,
                new Color(150, 150, 150).getRGB()
            );
        if (this.openProgress > 0.01F) {
            for (String text : this.setting.getModes()) {
                if (!this.select.containsKey(text)) {
                    this.select.put(text, 0.0F);
                }

                float anim = this.select.get(text);
                anim = this.animate(anim, text.equals(this.setting.getModeString()) ? 1.0F : 0.0F, 0.1F);
                this.select.put(text, anim);
                Color color = this.interpolateColorC(new Color(128, 128, 128), Color.white, anim);
                int alpha = (int)(color.getAlpha() * this.openProgress);
                alpha = Math.max(0, Math.min(255, alpha));
                Color finalColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                float textY = this.getY() + FontRepository.getFont("Inter Regular", 15.0F).height() + 1.0F + heightoff;
                textY -= (1.0F - this.openProgress) * 10.0F;
                FontRepository.getFont("Inter Regular", 13.0F)
                    .draw(text, this.getX() + 8.0F, textY, finalColor.getRGB());
                heightoff += lineHeight;
            }
        }

        this.setHeight(FontRepository.getFont("Inter Regular", 15.0F).height() + 4.0F + heightoff * this.openProgress);
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouse) {
        float headerWidth = FontRepository.getFont("Inter SemiBold", 15.0F).getStringWidth(this.setting.getName()) + 50;
        float headerHeight = FontRepository.getFont("Inter SemiBold", 15.0F).height() + 4.0F;
        if (MouseUtils.isHovered(this.getX(), this.getY(), headerWidth, headerHeight, mouseX, mouseY)
            && (mouse == 1 || mouse == 0)) {
            this.isExpanded = !this.isExpanded;
        }

        if (this.isExpanded && this.openProgress > 0.5F) {
            float heightoff = 4.0F;
            float lineHeight = FontRepository.getFont("Inter Regular", 13.0F).height() + 2.0F;
            String[] modes = this.setting.getModes();

            for (int i = 0; i < modes.length; i++) {
                String text = modes[i];
                float textY = this.getY() + FontRepository.getFont("Inter Regular", 15.0F).height() + 1.0F + heightoff;
                if (MouseUtils.isHovered(
                        this.getX() + 8.0F,
                        textY,
                        FontRepository.getFont("Inter Regular", 13.0F).getStringWidth(text),
                        FontRepository.getFont("Inter Regular", 13.0F).height(),
                        mouseX,
                        mouseY
                    )
                    && mouse == 0) {
                    this.setting.setValue(i);
                }

                heightoff += lineHeight;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouse);
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
