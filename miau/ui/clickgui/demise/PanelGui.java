package miau.ui.clickgui.demise;

import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import miau.Miau;
import miau.module.Module;
import miau.ui.clickgui.demise.component.Category;
import miau.ui.clickgui.demise.component.SearchCategory;
import miau.ui.clickgui.demise.component.config.ConfigCategoryComponent;
import miau.util.demise.RoundedUtils;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

public class PanelGui extends GuiScreen {
    private final List<Category> categories = new ArrayList<>();
    public Category selectedCategory;
    public ConfigCategoryComponent selectedConfigCategory;
    public SearchCategory selectedSearchCategory;
    public static boolean dragging;
    private float dragX;
    private float dragY;
    public static float posX = 255.0F;
    public static float posY = 120.0F;
    private final ConfigCategoryComponent configCategoryComponent;
    private final SearchCategory searchCategoryComponent;
    public static float interpolatedScale;
    private boolean closing;

    public PanelGui() {
        float height = 45.0F;

        for (Module module : Miau.moduleManager.modules.values()) {
            String cat = module.getCategory();
            if (cat != null) {
                Category existing = null;

                for (Category c : this.categories) {
                    if (c.getCategoryName().equalsIgnoreCase(cat)) {
                        existing = c;
                        break;
                    }
                }

                if (existing == null) {
                    existing = new Category(cat, posX + 7.0F, posY + height);
                    this.categories.add(existing);
                    height += FontRepository.getFont("Inter Regular", 18.0F).height() + 7.0F;
                }

                existing.addModule(module);
            }
        }

        this.configCategoryComponent = new ConfigCategoryComponent(posX + 7.0F, posY + height);
        this.searchCategoryComponent = new SearchCategory();
        if (this.selectedCategory == null && !this.categories.isEmpty()) {
            this.selectedCategory = this.categories.get(0);
        }
    }

    public void func_73866_w_() {
        this.closing = false;
        interpolatedScale = 0.0F;
        if (this.selectedConfigCategory != null) {
            this.selectedConfigCategory.initGui();
        }
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        interpolatedScale = this.interpolate(interpolatedScale, !this.closing ? 1.0F : 0.0F, 0.25F);
        if (interpolatedScale < 0.01F && this.closing) {
            this.field_146297_k.func_147108_a(null);
        }

        ScaledResolution sr = new ScaledResolution(this.field_146297_k);
        RenderUtil.scaleStart(sr.func_78326_a() / 2.0F, sr.func_78328_b() / 2.0F, interpolatedScale);
        if (dragging) {
            float deltaX = mouseX - (this.dragX + posX);
            float deltaY = mouseY - (this.dragY + posY);
            posX = mouseX - this.dragX;
            posY = mouseY - this.dragY;

            for (Category category : this.categories) {
                category.setX(category.getX() + deltaX);
                category.setY(category.getY() + deltaY);
            }

            this.configCategoryComponent.setX(this.configCategoryComponent.getX() + deltaX);
            this.configCategoryComponent.setY(this.configCategoryComponent.getY() + deltaY);
        }

        boolean skipped = true;

        for (Category category : this.categories) {
            boolean hovered = isHovered(
                category.getX(),
                category.getY(),
                FontRepository.getFont("Inter Regular", 18.0F).getStringWidth(category.getCategoryName()),
                FontRepository.getFont("Inter Regular", 18.0F).height(),
                mouseX,
                mouseY
            );
            if (hovered && Mouse.isButtonDown(0)) {
                if (this.selectedCategory != category) {
                    category.initCategory();
                }

                this.selectedCategory = category;
                this.selectedConfigCategory = null;
                this.selectedSearchCategory = null;
                skipped = false;
            }

            category.setHovered(hovered);
            category.setSelected(this.selectedCategory != null && this.selectedCategory == category);
        }

        boolean skipped1 = true;
        if (skipped) {
            boolean hovered = isHovered(
                this.configCategoryComponent.getX(),
                this.configCategoryComponent.getY(),
                FontRepository.getFont("Inter Regular", 18.0F).getStringWidth("Configs"),
                FontRepository.getFont("Inter Regular", 18.0F).height(),
                mouseX,
                mouseY
            );
            if (hovered && Mouse.isButtonDown(0)) {
                if (this.selectedConfigCategory == null) {
                    this.configCategoryComponent.initCategory();
                }

                this.selectedConfigCategory = this.configCategoryComponent;
                this.selectedCategory = null;
                this.selectedSearchCategory = null;
                skipped1 = false;
            }

            this.configCategoryComponent.setHovered(hovered);
            this.configCategoryComponent.setSelected(this.selectedConfigCategory != null);
        }

        RoundedUtils.drawRound(posX, posY, 450.0F, 300.0F, 7.0F, new Color(0, 0, 0, 140));
        float x = posX + 7.0F;
        float y = posY + 7.0F;
        FontRepository.getFont("Inter Bold", 35.0F).draw("Miau", x, y, new Color(255, 255, 255, 208).getRGB());
        FontRepository.getFont("Inter Bold", 24.0F)
            .draw(
                "1.2.0",
                FontRepository.getFont("Inter Bold", 35.0F).getStringWidth("Miau") + 2 + x,
                FontRepository.getFont("Inter Bold", 35.0F).height()
                    + y
                    - FontRepository.getFont("Inter Bold", 24.0F).height() * 1.1F,
                new Color(245, 245, 245, 208).getRGB()
            );
        float watermarkWidth = FontRepository.getFont("Inter Bold", 35.0F).getStringWidth("Miau")
            + 2
            + FontRepository.getFont("Inter Bold", 24.0F).getStringWidth("1.2.0");
        float calcWidth = 450.0F - watermarkWidth - 19.0F;
        RoundedUtils.drawRound(
            posX + watermarkWidth + 13.0F, posY + 7.0F, calcWidth, 20.0F, 7.0F, new Color(0, 0, 0, 100)
        );
        boolean searchHovered = isHovered(posX + watermarkWidth + 13.0F, posY + 7.0F, calcWidth, 20.0F, mouseX, mouseY);
        if (searchHovered && Mouse.isButtonDown(0) && skipped1) {
            if (this.selectedSearchCategory == null) {
                this.searchCategoryComponent.initCategory();
            }

            this.selectedSearchCategory = this.searchCategoryComponent;
            this.selectedCategory = null;
            this.selectedConfigCategory = null;
        }

        this.searchCategoryComponent.setSelected(this.selectedSearchCategory != null);
        if (this.selectedSearchCategory == null) {
            FontRepository.getFont("Inter Regular", 18.0F)
                .draw(
                    "Search...",
                    posX + watermarkWidth + 18.0F,
                    posY + 7.0F + FontRepository.getFont("Inter Regular", 15.0F).height() - 2.0F,
                    new Color(147, 147, 147, 255).getRGB()
                );
        }

        this.configCategoryComponent.render(false);
        if (this.selectedConfigCategory != null) {
            this.selectedConfigCategory.drawScreen(mouseX, mouseY);
        }

        this.searchCategoryComponent.render(false);
        if (this.selectedSearchCategory != null) {
            this.selectedSearchCategory.drawScreen(mouseX, mouseY);
        }

        for (Category category : this.categories) {
            category.render(false);
        }

        if (this.selectedCategory != null) {
            this.selectedCategory.drawScreen(mouseX, mouseY);
        }

        int total = Miau.moduleManager.modules.size();
        long enabled = Miau.moduleManager.modules.values().stream().filter(Module::isEnabled).count();
        String str = "Total modules: " + total + ", Enabled: " + enabled;
        FontRepository.getFont("Inter Regular", 14.0F)
            .draw(
                str,
                posX + 450.0F - FontRepository.getFont("Inter Regular", 14.0F).getStringWidth(str) - 4.0F,
                posY + 300.0F - FontRepository.getFont("Inter Regular", 14.0F).height(),
                new Color(255, 255, 255, 208).getRGB()
            );
        FontRepository.getFont("Inter Regular", 14.0F)
            .draw(
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                posX + 3.5,
                posY + 300.0F - FontRepository.getFont("Inter Regular", 14.0F).height(),
                new Color(255, 255, 255, 208).getRGB()
            );
        RenderUtil.scaleEnd();
    }

    public void func_73864_a(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && isHovered(posX, posY, 450.0F, 35.0F, mouseX, mouseY)) {
            dragging = true;
            this.dragX = mouseX - posX;
            this.dragY = mouseY - posY;
        }

        if (this.selectedSearchCategory != null) {
            this.selectedSearchCategory.mouseClicked(mouseX, mouseY, mouseButton);
        } else if (this.selectedConfigCategory != null) {
            this.selectedConfigCategory.mouseClicked(mouseX, mouseY, mouseButton);
        } else {
            if (this.selectedCategory != null) {
                this.selectedCategory.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    protected void func_146286_b(int mouseX, int mouseY, int state) {
        dragging = false;
        if (this.selectedSearchCategory != null) {
            this.selectedSearchCategory.mouseReleased(mouseX, mouseY, state);
        } else if (this.selectedConfigCategory != null) {
            this.selectedConfigCategory.mouseReleased(mouseX, mouseY, state);
        } else {
            if (this.selectedCategory != null) {
                this.selectedCategory.mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    public void func_73869_a(char typedChar, int keyCode) {
        if (keyCode == 1) {
            this.closing = true;
        }

        if (keyCode == 15 && this.selectedCategory != null && !this.categories.isEmpty()) {
            this.selectedCategory = this.categories
                .get((this.categories.indexOf(this.selectedCategory) + 1) % this.categories.size());
        }

        if (!this.closing) {
            if (this.selectedSearchCategory != null) {
                this.selectedSearchCategory.keyTyped(typedChar, keyCode);
            } else if (this.selectedConfigCategory != null) {
                this.selectedConfigCategory.keyTyped(typedChar, keyCode);
            } else {
                if (this.selectedCategory != null) {
                    this.selectedCategory.keyTyped(typedChar, keyCode);
                }
            }
        }
    }

    public boolean func_73868_f() {
        return false;
    }

    private float interpolate(float current, float target, float factor) {
        return current + (target - current) * factor;
    }

    public static boolean isHovered(float x, float y, float width, float height, float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
