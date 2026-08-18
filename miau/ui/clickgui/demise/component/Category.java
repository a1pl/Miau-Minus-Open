package miau.ui.clickgui.demise.component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import miau.module.Module;
import miau.ui.clickgui.demise.IComponent;
import miau.ui.clickgui.demise.PanelGui;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class Category implements IComponent {
    private String categoryName;
    private float x;
    private float y;
    private boolean isHovered;
    private boolean isSelected;
    private float interpolatedX;
    private float interpolatedLineWidth;
    private final List<ModuleComponent> moduleComponents = new ArrayList<>();
    private float scrollOffset = 0.0F;
    private float targetScrollOffset = 0.0F;
    private float maxScroll = 0.0F;

    public Category(String categoryName, float x, float y) {
        this.categoryName = categoryName;
        this.x = x;
        this.y = y;
        this.isSelected = false;
        this.isHovered = false;
        this.interpolatedX = x;
    }

    public void addModule(Module module) {
        this.moduleComponents.add(new ModuleComponent(module));
    }

    public void initCategory() {
        for (ModuleComponent mc : this.moduleComponents) {
            mc.initCategory();
        }
    }

    public void render(boolean shader) {
        float x = this.x;
        if (this.isSelected) {
            x += 3.0F;
            float width = FontRepository.getFont("Inter Regular", 18.0F).getStringWidth(this.categoryName);
            this.interpolatedLineWidth = this.animate(this.interpolatedLineWidth, width, 0.05F);
        } else {
            this.interpolatedLineWidth = this.animate(this.interpolatedLineWidth, 0.0F, 0.05F);
        }

        if (this.isHovered) {
            x += 2.5F;
        }

        if (!PanelGui.dragging) {
            this.interpolatedX = this.animate(this.interpolatedX, x, 0.15F);
        } else {
            this.interpolatedX = x;
        }

        if (!shader) {
            FontRepository.getFont("Inter Regular", 18.0F)
                .draw(this.categoryName, this.interpolatedX, this.y, Color.white.getRGB());
            RenderUtil.drawRect(
                this.interpolatedX,
                this.y + FontRepository.getFont("Inter Regular", 18.0F).height() - 2.6F,
                this.interpolatedX + this.interpolatedLineWidth,
                this.y + FontRepository.getFont("Inter Regular", 18.0F).height() - 2.6F + 0.5F,
                Color.white.getRGB()
            );
        }

        if (this.isSelected) {
            this.handleScroll();
            float componentStartY = PanelGui.posY + 45.0F;
            float viewHeight = 255.0F;
            float totalHeight = 0.0F;

            for (ModuleComponent module : this.moduleComponents) {
                totalHeight += module.getHeight() + 10.0F;
            }

            this.maxScroll = Math.max(0.0F, totalHeight - viewHeight);
            this.scrollOffset = this.animate(this.scrollOffset, this.targetScrollOffset, 0.1F);
            RenderUtil.scissor(0.0F, componentStartY, PanelGui.posX + 450.0F, viewHeight, PanelGui.interpolatedScale);
            GL11.glEnable(3089);
            float componentOffsetY = componentStartY + 2.0F;

            for (ModuleComponent module : this.moduleComponents) {
                float moduleY = componentOffsetY - this.scrollOffset;
                module.setX(this.x + 60.0F);
                module.setY(moduleY);
                module.setVisible(moduleY + 35.0F >= componentStartY && moduleY <= componentStartY + viewHeight);
                module.setVisibleSetting(
                    moduleY + module.getHeight() >= componentStartY && moduleY <= componentStartY + viewHeight
                );
                module.render(shader);
                componentOffsetY += module.getHeight() + 10.0F;
            }

            GL11.glDisable(3089);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        for (ModuleComponent moduleComponent : this.moduleComponents) {
            moduleComponent.drawScreen(mouseX, mouseY);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (ModuleComponent moduleComponent : this.moduleComponents) {
            moduleComponent.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        for (ModuleComponent moduleComponent : this.moduleComponents) {
            moduleComponent.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        for (ModuleComponent moduleComponent : this.moduleComponents) {
            moduleComponent.mouseReleased(mouseX, mouseY, state);
        }
    }

    public void handleScroll() {
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            float scrollAmount = wheel > 0 ? -25.0F : 25.0F;
            this.targetScrollOffset = MathHelper.func_76131_a(
                this.targetScrollOffset + scrollAmount, 0.0F, this.maxScroll
            );
        }
    }

    public String getCategoryName() {
        return this.categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public float getX() {
        return this.x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return this.y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public boolean isHovered() {
        return this.isHovered;
    }

    public void setHovered(boolean hovered) {
        this.isHovered = hovered;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    private float animate(float current, float target, float speed) {
        return current + (target - current) / Math.max(1.0F, speed * 10.0F);
    }
}
