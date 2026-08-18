package miau.ui.clickgui.components.impl;

import java.awt.Color;
import miau.property.properties.TextProperty;
import miau.ui.clickgui.components.Component;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import net.minecraft.client.gui.Gui;

public class StringComponent extends Component {
    public TextProperty property;
    public ModuleComponent moduleComponent;
    public float o;
    public float x;
    public float y;
    public boolean focused;

    public StringComponent(TextProperty property, ModuleComponent moduleComponent, float o) {
        this.property = property;
        this.moduleComponent = moduleComponent;
        this.o = o;
    }

    @Override
    public void render() {
        Font font = FontRepository.getClickGuiFont();
        String text = this.property.getValue();
        String display;
        if (this.focused) {
            display = text + (System.currentTimeMillis() % 1000L < 500L ? "_" : "");
        } else {
            display = text.isEmpty() ? this.property.getName() + "..." : this.property.getName() + ": " + text;
        }

        Gui.func_73734_a(
            (int)(this.moduleComponent.categoryComponent.getX() + 4.0F),
            (int)(this.moduleComponent.categoryComponent.getY() + this.o),
            (int)(
                this.moduleComponent.categoryComponent.getX()
                    + this.moduleComponent.categoryComponent.getWidth()
                    - 4.0F
            ),
            (int)(this.moduleComponent.categoryComponent.getY() + this.o + 12.0F),
            new Color(0, 0, 0, 100).getRGB()
        );
        font.draw(
            display,
            this.moduleComponent.categoryComponent.getX() + 8.0F,
            this.moduleComponent.categoryComponent.getY() + this.o + 2.0F,
            this.focused ? Color.WHITE.getRGB() : new Color(150, 150, 150).getRGB(),
            true
        );
    }

    @Override
    public void updateHeight(float n) {
        this.o = n;
    }

    @Override
    public float getHeightF() {
        return 12.0F;
    }

    @Override
    public float getOffset() {
        return this.o;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        this.x = this.moduleComponent.categoryComponent.getX();
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int mouseButton) {
        if (this.isHovered(mouseX, mouseY) && this.moduleComponent.isOpened) {
            this.focused = true;
            return true;
        } else {
            this.focused = false;
            return false;
        }
    }

    @Override
    public void keyTyped(char t, int k) {
        if (this.focused) {
            if (k == 1) {
                this.focused = false;
            } else if (k == 14) {
                String currentText = this.property.getValue();
                if (currentText.length() > 0) {
                    this.property.setValue(currentText.substring(0, currentText.length() - 1));
                }
            } else if (k == 28 || k == 156) {
                this.focused = false;
            } else if (t >= ' ' && t <= '~') {
                this.property.setValue(this.property.getValue() + t);
            }
        }
    }

    @Override
    public boolean isBaseVisible() {
        return this.property.isVisible();
    }

    private boolean isHovered(int mouseX, int mouseY) {
        return mouseX > this.x
            && mouseX < this.x + this.moduleComponent.categoryComponent.getWidth()
            && mouseY > this.y
            && mouseY < this.y + this.getHeightF();
    }
}
