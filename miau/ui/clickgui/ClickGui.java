package miau.ui.clickgui;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import miau.Miau;
import miau.module.modules.render.ClickGUI;
import miau.module.modules.render.HUD;
import miau.ui.clickgui.components.Component;
import miau.ui.clickgui.components.impl.BindComponent;
import miau.ui.clickgui.components.impl.CategoryComponent;
import miau.ui.clickgui.components.impl.ModuleComponent;
import miau.ui.clickgui.components.impl.SearchBarComponent;
import miau.ui.clickgui.components.impl.SliderComponent;
import miau.ui.clickgui.faiths.FaithsCharacterRenderer;
import miau.util.animation.AnimationTimer;
import miau.util.animation.Easing;
import miau.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class ClickGui extends GuiScreen {
    public static float openingScale = 1.0F;
    private AnimationTimer backgroundFade;
    private AnimationTimer blurSmooth;
    private AnimationTimer scaleAnimation = new AnimationTimer(300.0F);
    private ScaledResolution sr;
    public static ArrayList<CategoryComponent> categories;
    public static int lastMouseX;
    public static int lastMouseY;
    public static SliderComponent activeModeDropdown = null;
    private ConfigWindow configWindow;
    private int actualScreenWidth;
    private int actualScreenHeight;
    private boolean pendingScaleRefresh;
    private long lastMS = System.currentTimeMillis();
    private float openingAnimation = 0.0F;

    public ClickGui() {
        categories = new ArrayList<>();
        String[] catNames = new String[]{
            "Combat",
            "Ghost",
            "Movement",
            "Player",
            "Render",
            "Misc",
            "Search",
            "Themes",
            "Network",
            "Minigames",
            "Grind"
        };
        ScaledResolution sr = new ScaledResolution(Minecraft.func_71410_x());
        int screenWidth = sr.func_78326_a();
        float startX = 15.0F;
        float startY = 15.0F;
        float marginX = 105.0F;
        float marginY = 60.0F;
        float currentX = startX;
        float currentY = startY;

        for (String name : catNames) {
            CategoryComponent cc = new CategoryComponent(name);
            if (currentX + cc.width + 10.0F > screenWidth) {
                currentX = startX;
                currentY += marginY;
            }

            cc.setX(currentX, false);
            cc.setY(currentY, false);
            categories.add(cc);
            currentX += marginX;
        }
    }

    public void initMain() {
        (this.blurSmooth = this.backgroundFade = new AnimationTimer(500.0F)).start();
        (this.blurSmooth = this.backgroundFade = new AnimationTimer(500.0F)).start();
    }

    private void updateAutoLayout(float delta) {
        float startX = 15.0F;
        float startY = 15.0F;
        float marginX = 105.0F;
        float marginY = 10.0F;

        for (int col = 0; col < 20; col++) {
            int currentCol = col;
            List<CategoryComponent> inCol = new ArrayList<>();

            for (CategoryComponent c : categories) {
                int cCol = Math.round((c.getX() - startX) / marginX);
                if (cCol == currentCol) {
                    inCol.add(c);
                }
            }

            inCol.sort(Comparator.comparingDouble(CategoryComponent::getY));
            float currentY = startY;

            for (CategoryComponent c : inCol) {
                if (!c.dragging) {
                    c.setY(this.lerp(c.getY(), currentY, 0.015F * delta), false);
                } else {
                    currentY = c.getY();
                }

                currentY += c.lastHeight - c.getY() + marginY;
            }
        }
    }

    private float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    public void func_73866_w_() {
        super.func_73866_w_();
        FaithsCharacterRenderer.resetAnimation();
        this.scaleAnimation.start();
        openingScale = 0.5F;
        this.sr = new ScaledResolution(this.field_146297_k);
        this.actualScreenWidth = this.sr.func_78326_a();
        this.actualScreenHeight = this.sr.func_78328_b();
        int delay = 0;

        for (CategoryComponent categoryComponent : categories) {
            categoryComponent.setScreenSize(this.field_146294_l, this.field_146295_m);
            categoryComponent.limitPositions();
            categoryComponent.reloadModules();
            categoryComponent.guiOpenTimer = new AnimationTimer(250 + delay * 80);
            categoryComponent.guiOpenTimer.start();
            delay++;
        }

        if (this.configWindow == null) {
            this.configWindow = new ConfigWindow(this.actualScreenWidth - 350, this.actualScreenHeight - 250);
        } else {
            this.configWindow.refreshLocalConfigs();
        }
    }

    private List<CategoryComponent> getCategoriesInRenderOrder() {
        List<CategoryComponent> renderOrder = new ArrayList<>(categories);
        renderOrder.sort(Comparator.comparingLong(c -> c.lastInteractedTime));
        return renderOrder;
    }

    private CategoryComponent getTopmostUnderCursor(List<CategoryComponent> renderOrder, int x, int y) {
        for (int i = renderOrder.size() - 1; i >= 0; i--) {
            if (renderOrder.get(i).overRect(x, y)) {
                return renderOrder.get(i);
            }
        }

        return null;
    }

    public void func_73863_a(int x, int y, float p) {
        long currentMS = System.currentTimeMillis();
        float delta = (float)(currentMS - this.lastMS);
        this.lastMS = currentMS;
        if (delta > 50.0F || delta < 0.0F) {
            delta = 16.0F;
        }

        float centerX = this.field_146294_l / 2.0F;
        float centerY = this.field_146295_m / 2.0F;
        float scaleFactor = 1.0F;
        openingScale = scaleFactor;
        int scaledX = x;
        int scaledY = y;
        lastMouseX = scaledX;
        lastMouseY = scaledY;
        this.updateAutoLayout(delta);
        HUD hudModule = (HUD)Miau.moduleManager.modules.get(HUD.class);
        ClickGUI guiModule = (ClickGUI)Miau.moduleManager.modules.get(ClickGUI.class);
        if (guiModule != null) {
            guiModule.checkModeSwitch();
        }

        int bgColorAlpha = (int)(130.0F * this.scaleAnimation.getValueFloat(0.0F, 1.0F, 1));
        func_73734_a(0, 0, this.field_146294_l, this.field_146295_m, new Color(0, 0, 0, bgColorAlpha).getRGB());
        FaithsCharacterRenderer.renderCharacter(1.0F);
        List<CategoryComponent> renderOrder = this.getCategoriesInRenderOrder();
        CategoryComponent topmostUnderCursor = this.getTopmostUnderCursor(renderOrder, scaledX, scaledY);
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0.0F);
        GL11.glScaled(scaleFactor, scaleFactor, 1.0);
        GL11.glTranslatef(-centerX, -centerY, 0.0F);

        for (CategoryComponent c : renderOrder) {
            c.render(this.field_146289_q);
            c.mousePosition(scaledX, scaledY, c == topmostUnderCursor);

            for (Component m : c.getModules()) {
                m.drawScreen(scaledX, scaledY);
            }
        }

        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        SliderComponent dropdown = activeModeDropdown;
        activeModeDropdown = null;
        if (dropdown != null && dropdown.isModeDropdownActive()) {
            GL11.glDisable(3089);
            dropdown.renderModeDropdownOverlay(scaledX, scaledY);
        }

        if (this.configWindow != null) {
            this.configWindow.drawWindow(scaledX, scaledY, delta);
        }

        GL11.glPopMatrix();
    }

    public void func_73864_a(int mouseX, int mouseY, int mouseButton) throws IOException {
        float centerX = this.field_146294_l / 2.0F;
        float centerY = this.field_146295_m / 2.0F;
        float progress = this.scaleAnimation.getValueFloat(0.0F, 1.0F, 1);
        float ease = (float)Easing.EASE_OUT_EXPO.apply(progress);
        float scaleFactor = 0.8F + 0.2F * ease;
        int scaledX = (int)(centerX + (mouseX - centerX) / scaleFactor);
        int scaledY = (int)(centerY + (mouseY - centerY) / scaleFactor);
        if (this.configWindow == null || !this.configWindow.mouseClicked(scaledX, scaledY, mouseButton)) {
            List<CategoryComponent> inputOrder = new ArrayList<>(categories);
            inputOrder.sort((a, b) -> Long.compare(b.lastInteractedTime, a.lastInteractedTime));
            if (!this.handleActiveModeDropdownClick(inputOrder, scaledX, scaledY, mouseButton)) {
                CategoryComponent topmostCategory = null;

                for (CategoryComponent category : inputOrder) {
                    if (category.overRect(scaledX, scaledY)) {
                        topmostCategory = category;
                        break;
                    }
                }

                if (topmostCategory != null) {
                    topmostCategory.markInteracted();
                }

                if (mouseButton == 0) {
                    for (CategoryComponent category : categories) {
                        category.overTitle(false);
                    }

                    if (topmostCategory != null && topmostCategory.draggable(scaledX, scaledY)) {
                        topmostCategory.overTitle(true);
                        topmostCategory.xx = scaledX - topmostCategory.getX();
                        topmostCategory.yy = scaledY - topmostCategory.getY();
                        topmostCategory.dragging = true;
                    }
                }

                if (mouseButton == 1 && topmostCategory != null && topmostCategory.overTitle(scaledX, scaledY)) {
                    topmostCategory.mouseClicked(!topmostCategory.isOpened());
                }

                if (topmostCategory != null
                    && topmostCategory.isOpened()
                    && !topmostCategory.getModules().isEmpty()
                    && !topmostCategory.overTitle(scaledX, scaledY)) {
                    for (Component component : topmostCategory.getModules()) {
                        if (component.onClick(scaledX, scaledY, mouseButton)) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean handleActiveModeDropdownClick(
        List<CategoryComponent> inputOrder, int scaledX, int scaledY, int mouseButton
    ) {
        for (CategoryComponent category : inputOrder) {
            if (category.isOpened()) {
                for (Component component : category.getModules()) {
                    if (component instanceof ModuleComponent) {
                        ModuleComponent module = (ModuleComponent)component;
                        SliderComponent dropdown = module.getActiveModeDropdown();
                        if (dropdown != null && dropdown.isMouseOverModeDropdown(scaledX, scaledY)) {
                            category.markInteracted();
                            module.onClick(scaledX, scaledY, mouseButton);
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public void func_146286_b(int x, int y, int button) {
        if (!(this.scaleAnimation.getValueFloat(0.0F, 1.0F, 1) < 0.95F)) {
            float centerX = this.field_146294_l / 2.0F;
            float centerY = this.field_146295_m / 2.0F;
            float progress = this.scaleAnimation.getValueFloat(0.0F, 1.0F, 1);
            float ease = (float)Easing.EASE_OUT_EXPO.apply(progress);
            float scaleFactor = 0.8F + 0.2F * ease;
            int scaledX = (int)(centerX + (x - centerX) / scaleFactor);
            int scaledY = (int)(centerY + (y - centerY) / scaleFactor);
            if (this.configWindow != null) {
                this.configWindow.mouseReleased(scaledX, scaledY, button);
            }

            if (button == 0) {
                for (CategoryComponent category : categories) {
                    category.overTitle(false);
                    if (category.isOpened() && !category.getModules().isEmpty()) {
                        for (Component module : category.getModules()) {
                            module.mouseReleased(scaledX, scaledY, button);
                        }
                    }
                }
            }
        }
    }

    public void func_146274_d() throws IOException {
        super.func_146274_d();
        if (!(this.scaleAnimation.getValueFloat(0.0F, 1.0F, 1) < 0.95F)) {
            int wheelInput = Mouse.getDWheel();
            if (wheelInput != 0) {
                int mouseX = Mouse.getEventX() * this.field_146294_l / this.field_146297_k.field_71443_c;
                int mouseY = this.field_146295_m
                    - Mouse.getEventY() * this.field_146295_m / this.field_146297_k.field_71440_d
                    - 1;
                float centerX = this.field_146294_l / 2.0F;
                float centerY = this.field_146295_m / 2.0F;
                float progress = this.scaleAnimation.getValueFloat(0.0F, 1.0F, 1);
                float ease = (float)Easing.EASE_OUT_EXPO.apply(progress);
                float scaleFactor = 0.8F + 0.2F * ease;
                int scaledX = (int)(centerX + (mouseX - centerX) / scaleFactor);
                int scaledY = (int)(centerY + (mouseY - centerY) / scaleFactor);
                if (this.configWindow != null) {
                    this.configWindow.onScroll(wheelInput, scaledX, scaledY);
                }

                for (CategoryComponent category : categories) {
                    category.onScroll(wheelInput);
                }
            }
        }
    }

    public void func_73869_a(char t, int k) {
        if (this.configWindow == null || !this.configWindow.keyTyped(t, k)) {
            boolean isBinding = this.binding();
            SearchBarComponent searchBar = null;
            CategoryComponent searchCategory = null;

            for (CategoryComponent category : categories) {
                if (category.category.equalsIgnoreCase("Search")) {
                    searchCategory = category;
                    if (!category.getModules().isEmpty() && category.getModules().get(0) instanceof SearchBarComponent) {
                        searchBar = (SearchBarComponent)category.getModules().get(0);
                    }
                    break;
                }
            }

            if (searchBar != null && searchCategory != null) {
                if (searchBar.focused) {
                    if (k == 1) {
                        searchBar.focused = false;
                        return;
                    }
                } else if (!isBinding && k != 1 && k != 28 && k != 14 && String.valueOf(t).matches("[a-zA-Z0-9 ]")) {
                    if (!searchCategory.isOpened()) {
                        searchCategory.mouseClicked(true);
                    }

                    searchBar.focused = true;
                }
            }

            if (k == 1 && !isBinding) {
                this.field_146297_k.func_147108_a(null);
            } else {
                for (CategoryComponent category : categories) {
                    if (category.isOpened() && !category.getModules().isEmpty()) {
                        for (Component module : category.getModules()) {
                            module.keyTyped(t, k);
                        }
                    }
                }
            }
        }
    }

    public boolean func_73868_f() {
        return false;
    }

    private boolean binding() {
        for (CategoryComponent c : categories) {
            for (Component m : c.getModules()) {
                if (m instanceof ModuleComponent) {
                    for (Component component : ((ModuleComponent)m).settings) {
                        if (component instanceof BindComponent && ((BindComponent)component).isBinding) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public void onSliderChange() {
        for (CategoryComponent c : categories) {
            for (Component m : c.getModules()) {
                if (m instanceof ModuleComponent) {
                    ((ModuleComponent)m).onSliderChange();
                }
            }
        }
    }

    public void requestScaleRefresh() {
        this.pendingScaleRefresh = true;
    }

    public static double getActiveRenderScale() {
        return 1.0;
    }

    public void drawForEffects(boolean bloom) {
        if (!bloom) {
            RoundedUtils.drawRound(
                0.0F, 0.0F, this.field_146294_l, this.field_146295_m, 0.0F, true, new Color(0, 0, 0, 150)
            );
        } else {
            RoundedUtils.drawRound(
                0.0F, 0.0F, this.field_146294_l, this.field_146295_m, 0.0F, true, new Color(81, 99, 149, 80)
            );
            float centerX = this.field_146294_l / 2.0F;
            float centerY = this.field_146295_m / 2.0F;
            GL11.glPushMatrix();
            GL11.glTranslatef(centerX, centerY, 0.0F);
            GL11.glScaled(openingScale, openingScale, 1.0);
            GL11.glTranslatef(-centerX, -centerY, 0.0F);

            for (CategoryComponent c : this.getCategoriesInRenderOrder()) {
                c.renderBloom(this.field_146297_k.field_71466_p);
            }

            if (this.configWindow != null) {
                this.configWindow.drawWindow(lastMouseX, lastMouseY, 0.0F);
            }

            GL11.glPopMatrix();
        }
    }
}
