package miau.ui.clickgui.components.impl;

import java.awt.Color;
import miau.ui.clickgui.components.Component;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import net.minecraft.client.gui.Gui;

public class SearchBarComponent extends Component {
    private final CategoryComponent categoryComponent;
    public float o;
    public float x;
    public float y;
    public boolean focused;
    public final StringBuilder currentText = new StringBuilder();

    public SearchBarComponent(CategoryComponent categoryComponent, float o) {
        this.categoryComponent = categoryComponent;
        this.o = o;
    }

    @Override
    public void render() {
        Font font = FontRepository.getClickGuiFont();
        String display = this.currentText.length() == 0 && !this.focused
            ? "Search module..."
            : this.currentText.toString() + (this.focused && System.currentTimeMillis() % 1000L < 500L ? "_" : "");
        Gui.func_73734_a(
            (int)(this.categoryComponent.getX() + 4.0F),
            (int)(this.categoryComponent.getY() + this.o + 2.0F),
            (int)(this.categoryComponent.getX() + this.categoryComponent.getWidth() - 4.0F),
            (int)(this.categoryComponent.getY() + this.o + 16.0F),
            new Color(0, 0, 0, 100).getRGB()
        );
        font.draw(
            display,
            this.categoryComponent.getX() + 8.0F,
            this.categoryComponent.getY() + this.o + 5.0F,
            this.focused ? Color.WHITE.getRGB() : new Color(150, 150, 150).getRGB(),
            false
        );
    }

    @Override
    public void updateHeight(float n) {
        this.o = n;
    }

    @Override
    public float getHeightF() {
        return 20.0F;
    }

    @Override
    public float getOffset() {
        return this.o;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.y = this.categoryComponent.getModuleY() + this.o;
        this.x = this.categoryComponent.getX();
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int mouseButton) {
        if (this.isHovered(mouseX, mouseY) && this.categoryComponent.opened) {
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
                if (this.currentText.length() > 0) {
                    this.currentText.setLength(this.currentText.length() - 1);
                    this.categoryComponent.updateSearchResults(this.currentText.toString());
                }
            } else if (t >= ' ' && t <= '~' && this.currentText.length() < 18) {
                this.currentText.append(t);
                this.categoryComponent.updateSearchResults(this.currentText.toString());
            }
        }
    }

    private boolean isHovered(int mouseX, int mouseY) {
        return mouseX > this.x
            && mouseX < this.x + this.categoryComponent.getWidth()
            && mouseY > this.y
            && mouseY < this.y + this.getHeightF();
    }
}
