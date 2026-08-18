package miau.ui.clickgui.demise.component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import miau.Miau;
import miau.module.Module;
import miau.ui.clickgui.demise.IComponent;
import miau.ui.clickgui.demise.PanelGui;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class SearchCategory implements IComponent {
    private boolean isSelected;
    private float interpolatedLineWidth;
    private final List<ModuleComponent> moduleComponents = new ArrayList<>();
    private float scrollOffset = 0.0F;
    private float targetScrollOffset = 0.0F;
    private float maxScroll = 0.0F;
    private String filter = "";
    private boolean inputting;

    public SearchCategory() {
        this.isSelected = false;
    }

    public void initCategory() {
        this.moduleComponents.clear();
        Set<String> addedModules = new HashSet<>();

        for (Module module : Miau.moduleManager.modules.values()) {
            String name = module.getName().toLowerCase();
            String query = this.filter.toLowerCase().trim();
            boolean matches = this.filter.isEmpty()
                || name.contains(query)
                || name.replace(" ", "").contains(query.replace(" ", ""));
            if (matches && !addedModules.contains(name)) {
                this.moduleComponents.add(new ModuleComponent(module));
                addedModules.add(name);
            }
        }

        for (ModuleComponent mc : this.moduleComponents) {
            mc.initCategory();
        }
    }

    public void render(boolean shader) {
        if (this.isSelected) {
            String thing = System.currentTimeMillis() % 1000L > 500L ? "|" : "";
            if (this.inputting) {
                if (!this.filter.isEmpty()) {
                    this.drawText(this.filter + thing);
                } else {
                    this.drawText("Search..." + thing);
                }
            } else {
                this.drawText("Search...");
            }

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
                module.setX(PanelGui.posX + 67.0F);
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

    private void drawText(String text) {
        float watermarkWidth = FontRepository.getFont("Inter Bold", 35.0F).getStringWidth("Miau")
            + 2
            + FontRepository.getFont("Inter Bold", 24.0F).getStringWidth("1.2.0");
        int color = this.inputting ? new Color(193, 193, 193).getRGB() : new Color(119, 119, 119, 255).getRGB();
        FontRepository.getFont("Inter Regular", 18.0F)
            .draw(
                text,
                PanelGui.posX + watermarkWidth + 18.0F,
                PanelGui.posY + 7.0F + FontRepository.getFont("Inter Regular", 15.0F).height() - 2.0F,
                color
            );
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        for (ModuleComponent moduleComponent : this.moduleComponents) {
            moduleComponent.drawScreen(mouseX, mouseY);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float watermarkWidth = FontRepository.getFont("Inter Bold", 35.0F).getStringWidth("Miau")
            + 2
            + FontRepository.getFont("Inter Bold", 24.0F).getStringWidth("1.2.0");
        float calcWidth = 450.0F - watermarkWidth - 19.0F;
        this.inputting = PanelGui.isHovered(
                PanelGui.posX + watermarkWidth + 13.0F, PanelGui.posY + 7.0F, calcWidth, 20.0F, mouseX, mouseY
            )
            && mouseButton == 0;

        for (ModuleComponent moduleComponent : this.moduleComponents) {
            moduleComponent.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.inputting) {
            String lastFilter = this.filter;
            if (keyCode == 14) {
                this.deleteLastCharacter();
            }

            if (Character.isLetterOrDigit(typedChar) || keyCode == 57) {
                this.filter = this.filter + typedChar;
            }

            if (!lastFilter.equals(this.filter)) {
                this.initCategory();
            }
        }

        for (ModuleComponent moduleComponent : this.moduleComponents) {
            moduleComponent.keyTyped(typedChar, keyCode);
        }
    }

    private void deleteLastCharacter() {
        if (!this.filter.isEmpty()) {
            this.filter = this.filter.substring(0, this.filter.length() - 1);
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
