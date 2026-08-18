package miau.ui.clickgui.demise.component.impl;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miau.property.properties.BooleanProperty;
import miau.ui.clickgui.demise.Component;
import miau.util.demise.MouseUtils;
import miau.util.font.FontRepository;

public class MultiBooleanComponent extends Component {
    private final String name;
    private final List<BooleanProperty> values;
    private final Map<BooleanProperty, Float> select = new HashMap<>();

    public MultiBooleanComponent(String name, List<BooleanProperty> values) {
        this.name = name;
        this.values = values;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        float heightoff = 4.0F;
        float lineHeight = FontRepository.getFont("Inter Regular", 13.0F).height() + 2.0F;
        FontRepository.getFont("Inter SemiBold", 15.0F).draw(this.name, this.getX() + 4.0F, this.getY() + 2.0F, -1);

        for (BooleanProperty boolValue : this.values) {
            if (!this.select.containsKey(boolValue)) {
                this.select.put(boolValue, 0.0F);
            }

            float anim = this.select.get(boolValue);
            anim = this.animate(anim, boolValue.getValue() ? 1.0F : 0.0F, 0.1F);
            this.select.put(boolValue, anim);
            Color color = this.interpolateColorC(new Color(128, 128, 128), Color.white, anim);
            FontRepository.getFont("Inter Regular", 13.0F)
                .draw(
                    boolValue.getName(),
                    this.getX() + 8.0F,
                    this.getY() + FontRepository.getFont("Inter Regular", 15.0F).height() + 1.0F + heightoff,
                    color.getRGB()
                );
            heightoff += lineHeight;
        }

        this.setHeight(FontRepository.getFont("Inter Regular", 15.0F).height() + 4.0F + heightoff);
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouse) {
        float heightoff = 4.0F;
        float lineHeight = FontRepository.getFont("Inter Regular", 13.0F).height() + 2.0F;

        for (BooleanProperty boolValue : this.values) {
            if (MouseUtils.isHovered(
                    this.getX() + 8.0F,
                    this.getY() + FontRepository.getFont("Inter Regular", 15.0F).height() + 1.0F + heightoff,
                    FontRepository.getFont("Inter Regular", 13.0F).getStringWidth(boolValue.getName()),
                    FontRepository.getFont("Inter Regular", 13.0F).height(),
                    mouseX,
                    mouseY
                )
                && mouse == 0) {
                boolValue.setValue(!boolValue.getValue());
            }

            heightoff += lineHeight;
        }

        super.mouseClicked(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return true;
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
