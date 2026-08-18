package miau.ui.clickgui.components.impl;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import miau.Miau;
import miau.module.Module;
import miau.ui.clickgui.animation.ScrollOffsetAnimation;
import miau.ui.clickgui.components.Component;
import miau.util.animation.AnimationTimer;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import miau.util.render.Themes;
import miau.util.vector.Vector2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class CategoryComponent {
    private static long interactionSequence;
    private static final Map<String, CategoryComponent.CategoryIconStacks> CATEGORY_ICON_STACKS = buildCategoryIconStacks();
    public List<Component> modules = new CopyOnWriteArrayList<>();
    public String category;
    public boolean opened;
    public float width;
    public float y;
    public float x;
    public float titleHeight;
    public boolean dragging;
    public float xx;
    public float yy;
    public boolean hovering = false;
    public boolean hoveringOverCategory = false;
    public AnimationTimer smoothTimer;
    public AnimationTimer guiOpenTimer;
    private AnimationTimer textTimer;
    public float big;
    private static final int TRANSLUCENT_BACKGROUND = new Color(0, 0, 0, 110).getRGB();
    private static final int REGULAR_OUTLINE = new Color(81, 99, 149).getRGB();
    private static final int REGULAR_OUTLINE2 = new Color(97, 67, 133).getRGB();
    private static final int CATEGORY_NAME_COLOR = new Color(220, 220, 220).getRGB();
    public float lastHeight;
    private float lastNamePos;
    private float animationStartNamePos;
    public float moduleY;
    private float screenHeight;
    private float screenWidth;
    private float animationStartHeight;
    private final ScrollOffsetAnimation scrollAnim = new ScrollOffsetAnimation(200L);
    public long lastInteractedTime = 0L;
    public float renderX;
    public float renderY;
    private long lastRenderMS = System.currentTimeMillis();
    private float cachedExtra;
    private float cachedLiftY;
    private float cachedHoverAnim;
    private float cachedRenderModuleY;

    public CategoryComponent(String category) {
        this.category = category;
        this.width = this.category.equalsIgnoreCase("Config") ? 135.0F : 92.0F;
        this.renderX = this.x = 5.0F;
        this.renderY = this.moduleY = this.y = 5.0F;
        this.titleHeight = 13.0F;
        float moduleRenderY = this.titleHeight + 3.0F;
        this.scrollAnim.reset(this.moduleY);
        this.lastHeight = this.y + this.titleHeight + 4.0F;
        this.animationStartHeight = this.lastHeight;
        List<Module> mods = Miau.moduleManager.getModulesByCategory().get(category);
        if (mods != null) {
            for (Module mod : mods) {
                ModuleComponent b = new ModuleComponent(mod, this, moduleRenderY);
                this.modules.add(b);
                moduleRenderY += 16.0F;
            }
        }
    }

    public List<Component> getModules() {
        return this.modules;
    }

    public void reloadModules() {
        Map<String, Boolean> openStates = this.captureModuleOpenStates();
        this.modules.clear();
        this.titleHeight = 13.0F;
        float moduleRenderY = this.titleHeight + 3.0F;
        if (this.category.equalsIgnoreCase("Search")) {
            SearchBarComponent searchBar = new SearchBarComponent(this, moduleRenderY);
            this.modules.add(searchBar);
            this.syncAfterModuleReload();
        } else if (this.category.equalsIgnoreCase("Themes")) {
            for (Themes theme : Themes.values()) {
                ThemeSelectComponent tsc = new ThemeSelectComponent(this, moduleRenderY, theme);
                this.modules.add(tsc);
                moduleRenderY += tsc.getHeightF();
            }

            this.syncAfterModuleReload();
        } else {
            List<Module> mods = Miau.moduleManager.getModulesByCategory().get(this.category);
            if (mods != null) {
                for (Module mod : mods) {
                    ModuleComponent component = new ModuleComponent(mod, this, moduleRenderY);
                    component.restoreOpenState(Boolean.TRUE.equals(openStates.get(mod.getName())));
                    this.modules.add(component);
                    moduleRenderY += 16.0F;
                }
            }

            this.syncAfterModuleReload();
        }
    }

    public void updateSearchResults(String query) {
        if (this.category.equalsIgnoreCase("Search")) {
            Map<String, Boolean> openStates = this.captureModuleOpenStates();
            Component searchBar = null;
            if (!this.modules.isEmpty() && this.modules.get(0) instanceof SearchBarComponent) {
                searchBar = this.modules.get(0);
            }

            this.modules.clear();
            float moduleRenderY = this.titleHeight + 3.0F;
            if (searchBar != null) {
                this.modules.add(searchBar);
                moduleRenderY += searchBar.getHeightF();
            }

            if (query != null && !query.trim().isEmpty()) {
                String lowerQuery = query.toLowerCase().replace(" ", "");

                for (Module mod : Miau.moduleManager.modules.values()) {
                    if (!mod.getName().equalsIgnoreCase("ClickGUI")
                        && !mod.getName().equalsIgnoreCase("GUI")
                        && mod.getName().toLowerCase().replace(" ", "").contains(lowerQuery)) {
                        ModuleComponent component = new ModuleComponent(mod, this, moduleRenderY);
                        component.restoreOpenState(Boolean.TRUE.equals(openStates.get(mod.getName())));
                        this.modules.add(component);
                        moduleRenderY += component.getHeightF();
                    }
                }
            }

            this.syncAfterModuleReload();
        }
    }

    private Map<String, Boolean> captureModuleOpenStates() {
        Map<String, Boolean> openStates = new HashMap<>();

        for (Component moduleComponent : this.modules) {
            if (moduleComponent instanceof ModuleComponent && ((ModuleComponent)moduleComponent).mod != null) {
                openStates.put(
                    ((ModuleComponent)moduleComponent).mod.getName(), ((ModuleComponent)moduleComponent).isOpened
                );
            }
        }

        return openStates;
    }

    private void syncAfterModuleReload() {
        CategoryComponent.CategoryLayoutMetrics layoutMetrics = this.computeLayoutMetrics(
            this.opened || this.smoothTimer != null
        );
        float minScrollY = layoutMetrics.minScrollY;
        float maxScrollY = this.y;
        float clampedScroll = Math.max(minScrollY, Math.min(maxScrollY, this.scrollAnim.getTarget()));
        this.moduleY = clampedScroll;
        this.scrollAnim.reset(clampedScroll);
        if (this.opened && !this.modules.isEmpty()) {
            this.big = layoutMetrics.visibleHeight;
            this.lastHeight = layoutMetrics.contentBottom;
        } else {
            if (!this.opened && this.smoothTimer == null) {
                this.big = 0.0F;
            }

            this.lastHeight = this.y + this.titleHeight + 4.0F;
        }
    }

    public void setX(float newX, boolean limit) {
        if (limit) {
            newX = Math.max(newX, 2.0F);
            newX = Math.min(newX, this.screenWidth - this.width - 4.0F);
        }

        this.x = newX;
    }

    public void setY(float y, boolean limit) {
        if (limit) {
            y = Math.max(y, 1.0F);
            float maxY = this.screenHeight - this.titleHeight - 5.0F;
            y = Math.min(y, maxY);
        }

        float scrollOffset = this.scrollAnim.getTarget() - this.y;
        this.y = y;
        float newTarget = y + scrollOffset;
        this.moduleY = newTarget;
        this.scrollAnim.reset(newTarget);
    }

    public void overTitle(boolean d) {
        this.dragging = d;
    }

    public boolean isOpened() {
        return this.opened;
    }

    public void markInteracted() {
        this.lastInteractedTime = ++interactionSequence;
    }

    public void mouseClicked(boolean on) {
        this.animationStartHeight = this.getCurrentAnimatedCategoryHeight();
        this.animationStartNamePos = this.getCurrentAnimatedNamePos();
        float animationDuration = 250.0F;
        this.opened = on;
        (this.smoothTimer = new AnimationTimer(animationDuration)).start();
        (this.textTimer = new AnimationTimer(animationDuration)).start();
    }

    public void onScroll(int mouseScrollInput) {
        this.onScroll(mouseScrollInput, Float.NaN, Float.NaN);
    }

    public void onScroll(int mouseScrollInput, float mouseX, float mouseY) {
        for (Component mod : this.modules) {
            mod.onScroll(mouseScrollInput);
        }

        if (this.hoveringOverCategory && this.opened) {
            this.markInteracted();
            float scrollSpeed = 10.0F;
            float minScrollY = this.computeMinScrollY();
            float maxScrollY = this.y;
            float delta = scrollSpeed * (mouseScrollInput / 120.0F);
            if (delta != 0.0F) {
                this.scrollAnim.extend(delta);
            }

            this.scrollAnim.clampTarget(minScrollY, maxScrollY);
        }
    }

    private float computeMinScrollY() {
        return this.computeLayoutMetrics(false).minScrollY;
    }

    public void render(FontRenderer renderer) {
        this.width = this.category.equalsIgnoreCase("Config") ? 135.0F : 92.0F;
        Font titleRenderer = FontRepository.getClickGuiFont();
        long currentMS = System.currentTimeMillis();
        float delta = (float)(currentMS - this.lastRenderMS);
        this.lastRenderMS = currentMS;
        if (delta > 50.0F || delta < 0.0F) {
            delta = 16.0F;
        }

        float speed = 0.02F * delta;
        if (Math.abs(this.renderX - this.x) > 0.1F) {
            this.renderX = this.renderX + (this.x - this.renderX) * speed;
        } else {
            this.renderX = this.x;
        }

        if (Math.abs(this.renderY - this.y) > 0.1F) {
            this.renderY = this.renderY + (this.y - this.renderY) * speed;
        } else {
            this.renderY = this.y;
        }

        if (this.smoothTimer != null && System.currentTimeMillis() - this.smoothTimer.last >= 280L) {
            this.smoothTimer = null;
        }

        if (this.textTimer != null && System.currentTimeMillis() - this.textTimer.last >= 280L) {
            this.textTimer = null;
        }

        for (Component c : this.modules) {
            if (c instanceof ModuleComponent) {
                ((ModuleComponent)c).updateAnimationState();
            }
        }

        CategoryComponent.CategoryLayoutMetrics layoutMetrics = this.computeLayoutMetrics(
            this.opened || this.smoothTimer != null
        );
        this.big = !this.opened && this.smoothTimer == null ? 0.0F : layoutMetrics.visibleHeight;
        float maxScrollY = this.renderY;
        float minScrollY = layoutMetrics.minScrollY - (this.y - this.renderY);
        this.scrollAnim.clampTarget(layoutMetrics.minScrollY, this.y);
        this.moduleY = this.scrollAnim.getValue();
        this.moduleY = Math.max(layoutMetrics.minScrollY, Math.min(this.y, this.moduleY));
        float renderModuleY = this.moduleY - (this.y - this.renderY);
        float middlePos = this.renderX + this.width / 2.0F - titleRenderer.width(this.category.toLowerCase()) / 2.0F;
        float contentBottom = layoutMetrics.contentBottom - (this.y - this.renderY);
        float extra;
        if (this.smoothTimer != null) {
            float targetHeight = this.opened ? contentBottom : this.renderY + this.titleHeight + 4.0F;
            extra = this.smoothTimer
                .getValueFloat(this.animationStartHeight - (this.y - this.renderY), targetHeight, 1);
            if (this.opened && extra > targetHeight || !this.opened && extra < targetHeight) {
                extra = targetHeight;
            }
        } else {
            extra = contentBottom;
        }

        float targetNamePos = this.opened ? middlePos : this.renderX + 12.0F;
        float namePos;
        if (this.textTimer == null) {
            namePos = targetNamePos;
        } else {
            namePos = this.textTimer
                .getValueFloat(this.animationStartNamePos - (this.x - this.renderX), targetNamePos, 1);
        }

        this.lastNamePos = namePos + (this.x - this.renderX);
        this.lastHeight = extra + (this.y - this.renderY);
        float liftY = 0.0F;
        this.cachedExtra = extra;
        this.cachedLiftY = 0.0F;
        this.cachedHoverAnim = 0.0F;
        this.cachedRenderModuleY = renderModuleY;
        float openAnimationValue = 1.0F;
        if (this.guiOpenTimer != null) {
            openAnimationValue = this.guiOpenTimer.getValueFloat(0.0F, 1.0F, 1);
            if (openAnimationValue >= 1.0F) {
                this.guiOpenTimer = null;
            }
        }

        if (openAnimationValue < 1.0F) {
            GL11.glEnable(3089);
            float scissorHeight = (extra - this.renderY) * openAnimationValue;
            RenderUtil.scissor(this.renderX - 3.0F, this.renderY - 3.0F, this.width + 6.0F, scissorHeight + 6.0F);
        }

        Color themeColor1 = Themes.getCurrentTheme().getAccentColor(new Vector2d(this.renderX, this.renderY));
        Color themeColor2 = Themes.getCurrentTheme().getAccentColor(new Vector2d(this.renderX + this.width, extra));
        this.drawRoundedGradientOutlinedRectangle(
            this.renderX - 2.0F,
            this.renderY,
            this.renderX + this.width + 2.0F,
            extra,
            10.0F,
            TRANSLUCENT_BACKGROUND,
            themeColor1.getRGB(),
            themeColor2.getRGB()
        );
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, -liftY, 0.0F);
        this.renderItemForCategory(this.category, (int)(this.renderX + 1.0F), (int)(this.renderY + 4.0F), false);
        titleRenderer.draw(this.category.toLowerCase(), namePos, this.renderY + 4.0F, CATEGORY_NAME_COLOR, false);
        float moduleAreaTop = this.renderY + this.titleHeight + 3.0F - liftY;
        float scissorBottom = extra - 2.0F - liftY;
        float moduleAreaHeight = Math.max(0.0F, scissorBottom - moduleAreaTop);
        if (this.opened || this.smoothTimer != null) {
            GL11.glEnable(3089);
            RenderUtil.scissor(0.0, moduleAreaTop, this.renderX + this.width + 4.0F, moduleAreaHeight);
            float scrollOffset = renderModuleY - this.renderY;
            GL11.glPushMatrix();
            GL11.glTranslatef(this.renderX - this.x, this.renderY - this.y + scrollOffset, 0.0F);

            for (Component c2 : this.modules) {
                c2.render();
            }

            GL11.glPopMatrix();
            GL11.glDisable(3089);

            for (Component c2 : this.modules) {
                if (c2 instanceof ModuleComponent) {
                    ((ModuleComponent)c2).renderOverlays();
                }
            }
        }

        if (openAnimationValue < 1.0F) {
            GL11.glDisable(3089);
        }

        GL11.glPopMatrix();
    }

    public void renderBloom(FontRenderer renderer) {
        if (this.isOpened()) {
            for (Component c2 : this.getModules()) {
                c2.renderBloom();
            }
        }
    }

    public void updateHeight() {
        float y = this.titleHeight + 3.0F;

        for (Component component : this.modules) {
            component.updateHeight(y);
            y += component.getHeightF();
        }
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getModuleY() {
        return this.moduleY - this.cachedLiftY;
    }

    public float getWidth() {
        return this.width;
    }

    public void mousePosition(int mouseX, int mouseY, boolean isTopmostUnderCursor) {
        if (this.dragging) {
            float newX = mouseX - this.xx;
            float newY = mouseY - this.yy;
            newX = Math.max(newX, 2.0F);
            newX = Math.min(newX, this.screenWidth - this.width - 4.0F);
            newY = Math.max(newY, 1.0F);
            int maxY = (int)(this.screenHeight - this.titleHeight - 5.0F);
            newY = Math.min(newY, maxY);
            this.setX(newX, false);
            this.setY(newY, false);
        }

        this.hoveringOverCategory = isTopmostUnderCursor && this.overCategory(mouseX, mouseY);
        this.hovering = this.hoveringOverCategory;
    }

    public boolean overTitle(int x, int y) {
        float effectiveY = this.y - this.cachedLiftY;
        return x >= this.x
            && x <= this.x + this.width
            && y >= effectiveY + 2.0F
            && y <= effectiveY + this.titleHeight + 1.0F;
    }

    public boolean overCategory(int x, int y) {
        float effectiveY = this.y - this.cachedLiftY;
        return x >= this.x - 2.0F
            && x <= this.x + this.width + 2.0F
            && y >= effectiveY + 2.0F
            && y <= effectiveY + this.titleHeight + this.big + 1.0F;
    }

    public boolean draggable(int x, int y) {
        float effectiveY = this.y - this.cachedLiftY;
        return x >= this.x && x <= this.x + this.width && y >= effectiveY && y <= effectiveY + this.titleHeight;
    }

    public boolean overRect(int x, int y) {
        float effectiveY = this.y - this.cachedLiftY;
        float effectiveLastHeight = this.lastHeight - this.cachedLiftY;
        return x >= this.x - 2.0F && x <= this.x + this.width + 2.0F && y >= effectiveY && y <= effectiveLastHeight;
    }

    private void renderItemForCategory(String category, int x, int y, boolean enchant) {
        RenderItem renderItem = Minecraft.func_71410_x().func_175599_af();
        double scale = 0.55;
        GlStateManager.func_179094_E();
        GlStateManager.func_179139_a(scale, scale, scale);
        CategoryComponent.CategoryIconStacks iconStacks = CATEGORY_ICON_STACKS.get(category);
        ItemStack itemStack = iconStacks == null ? null : (enchant ? iconStacks.activeStack : iconStacks.normalStack);
        if (itemStack != null) {
            RenderHelper.func_74520_c();
            GlStateManager.func_179084_k();
            GlStateManager.func_179109_b((float)(x / scale), (float)(y / scale), 0.0F);
            renderItem.func_180450_b(itemStack, 0, 0);
            GlStateManager.func_179147_l();
            RenderHelper.func_74518_a();
        }

        GlStateManager.func_179152_a(1.0F, 1.0F, 1.0F);
        GlStateManager.func_179121_F();
    }

    private float getCurrentAnimatedNamePos() {
        if (this.textTimer != null) {
            return this.lastNamePos;
        }

        float middlePos = this.x
            + this.width / 2.0F
            - FontRepository.getClickGuiFont().width(this.category.toLowerCase()) / 2.0F;
        return this.opened ? middlePos : this.x + 12.0F;
    }

    private float getCurrentAnimatedCategoryHeight() {
        if (this.lastHeight > 0.0F) {
            return this.lastHeight;
        }

        if (!this.modules.isEmpty() && (this.opened || this.smoothTimer != null)) {
            float modulesHeight = 0.0F;

            for (Component c : this.modules) {
                modulesHeight += c.getHeightF();
            }

            return this.y + this.titleHeight + modulesHeight + 4.0F;
        } else {
            return this.y + this.titleHeight + 4.0F;
        }
    }

    public void setScreenSize(float screenWidth, float screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void limitPositions() {
        this.setX(this.x, true);
        this.setY(this.y, true);
    }

    public void applySavedState(float x, float y, boolean opened, boolean clampToScreen) {
        if (clampToScreen) {
            this.setX(x, true);
            this.setY(y, true);
        } else {
            float scrollOffset = this.scrollAnim.getTarget() - this.y;
            this.x = x;
            this.y = y;
            float newTarget = y + scrollOffset;
            this.moduleY = newTarget;
            this.scrollAnim.reset(newTarget);
        }

        this.opened = opened;
        this.smoothTimer = null;
        this.textTimer = null;
        if (opened) {
            boolean hasContent = !this.modules.isEmpty();
            if (hasContent) {
                CategoryComponent.CategoryLayoutMetrics layoutMetrics = this.computeLayoutMetrics(true);
                this.big = layoutMetrics.visibleHeight;
                this.lastHeight = layoutMetrics.contentBottom;
            } else {
                this.big = 0.0F;
                this.lastHeight = this.y + this.titleHeight + 4.0F;
            }
        } else {
            this.big = 0.0F;
            this.lastHeight = this.y + this.titleHeight + 4.0F;
        }

        this.moduleY = this.y;
        this.scrollAnim.reset(this.y);
        this.renderX = this.x;
        this.renderY = this.y;
    }

    public void onGuiClosed() {
        if (this.smoothTimer != null || this.textTimer != null) {
            float finalHeight = this.y + this.titleHeight;
            if (!this.opened) {
                finalHeight += 4.0F;
            } else if (this.modules.isEmpty()) {
                finalHeight += 4.0F;
            } else {
                float modulesHeight = 0.0F;

                for (Component c : this.modules) {
                    modulesHeight += c.getHeightF();
                }

                finalHeight += modulesHeight + 4.0F;
            }

            this.lastHeight = finalHeight;
        }

        this.smoothTimer = null;
        this.textTimer = null;
        this.moduleY = this.scrollAnim.getTarget();
        this.scrollAnim.reset(this.moduleY);
    }

    private CategoryComponent.CategoryLayoutMetrics computeLayoutMetrics(boolean updateModuleOffsets) {
        if (!this.modules.isEmpty() && (this.opened || this.smoothTimer != null)) {
            float maxModulesHeight = this.screenHeight * 0.9F - this.titleHeight - 4.0F;
            float visibleHeight = 0.0F;
            float totalScrollExtent = 0.0F;
            float moduleOffset = this.titleHeight + 3.0F;

            for (Component component : this.modules) {
                if (updateModuleOffsets) {
                    component.updateHeight(moduleOffset);
                }

                float componentHeight = component.getHeightF();
                moduleOffset += componentHeight;
                totalScrollExtent += component.getScrollExtentHeightF();
                if (visibleHeight < maxModulesHeight) {
                    visibleHeight += Math.min(componentHeight, maxModulesHeight - visibleHeight);
                }
            }

            float viewport = Math.min(maxModulesHeight, totalScrollExtent);
            float overflow = Math.max(0.0F, totalScrollExtent - viewport);
            float minScrollY = overflow > 0.0F ? this.y - overflow : this.y;
            float maxBottom = this.y + this.screenHeight * 0.9F;
            float contentBottom = Math.min(this.y + this.titleHeight + visibleHeight + 4.0F, maxBottom);
            return new CategoryComponent.CategoryLayoutMetrics(Math.max(0.0F, visibleHeight), minScrollY, contentBottom);
        } else {
            return new CategoryComponent.CategoryLayoutMetrics(0.0F, this.y, this.y + this.titleHeight + 4.0F);
        }
    }

    private static Map<String, CategoryComponent.CategoryIconStacks> buildCategoryIconStacks() {
        Map<String, CategoryComponent.CategoryIconStacks> iconStacks = new HashMap<>();
        String[] categories = new String[]{
            "Combat",
            "Ghost",
            "Movement",
            "Render",
            "Player",
            "Misc",
            "Network",
            "Minigames",
            "Grind",
            "Search",
            "Themes"
        };

        for (String cat : categories) {
            ItemStack normalStack = createCategoryIconStack(cat, false);
            ItemStack activeStack = createCategoryIconStack(cat, true);
            if (normalStack != null && activeStack != null) {
                iconStacks.put(cat, new CategoryComponent.CategoryIconStacks(normalStack, activeStack));
            }
        }

        return iconStacks;
    }

    private static ItemStack createCategoryIconStack(String category, boolean active) {
        ItemStack itemStack;
        if (category.equalsIgnoreCase("Combat")) {
            itemStack = new ItemStack(Items.field_151048_u);
        } else if (category.equalsIgnoreCase("Ghost")) {
            itemStack = new ItemStack(Items.field_151040_l);
        } else if (category.equalsIgnoreCase("Movement")) {
            itemStack = new ItemStack(Items.field_151175_af);
        } else if (category.equalsIgnoreCase("Render")) {
            itemStack = new ItemStack(Items.field_151061_bv);
        } else if (category.equalsIgnoreCase("Player")) {
            itemStack = new ItemStack(Items.field_151153_ao);
        } else if (category.equalsIgnoreCase("Misc")) {
            itemStack = new ItemStack(Items.field_151113_aN);
        } else if (category.equalsIgnoreCase("Network")) {
            itemStack = new ItemStack(Items.field_151137_ax);
        } else if (category.equalsIgnoreCase("Minigames")) {
            itemStack = new ItemStack(Items.field_151043_k);
        } else if (category.equalsIgnoreCase("Grind")) {
            itemStack = new ItemStack(Items.field_151035_b);
        } else if (category.equalsIgnoreCase("Target")) {
            itemStack = new ItemStack(Items.field_151032_g);
        } else if (category.equalsIgnoreCase("Search")) {
            itemStack = new ItemStack(Items.field_151057_cb);
        } else {
            if (!category.equalsIgnoreCase("Themes")) {
                return null;
            }

            itemStack = new ItemStack(Items.field_151100_aR, 1, 9);
        }

        if (!active) {
            return itemStack;
        }

        if (!category.equalsIgnoreCase("Player")) {
            itemStack.func_77966_a(Enchantment.field_77347_r, 2);
        } else {
            itemStack.func_77964_b(1);
        }

        return itemStack;
    }

    private void drawRoundedGradientOutlinedRectangle(
        float x, float y, float x2, float y2, float radius, int n6, int n7, int n8
    ) {
        x *= 2.0F;
        y *= 2.0F;
        x2 *= 2.0F;
        y2 *= 2.0F;
        GL11.glPushMatrix();
        GL11.glPushAttrib(1048575);
        GL11.glScaled(0.5, 0.5, 0.5);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glBegin(6);
        this.glColor(n6);

        for (int i = 0; i <= 90; i += 3) {
            double n9 = i * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(n9) * radius * -1.0, y + radius + Math.cos(n9) * radius * -1.0);
        }

        for (int j = 90; j <= 180; j += 3) {
            double n10 = j * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(n10) * radius * -1.0, y2 - radius + Math.cos(n10) * radius * -1.0);
        }

        for (int k = 0; k <= 90; k += 3) {
            double n11 = k * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x2 - radius + Math.sin(n11) * radius, y2 - radius + Math.cos(n11) * radius);
        }

        for (int l = 90; l <= 180; l += 3) {
            double n12 = l * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x2 - radius + Math.sin(n12) * radius, y + radius + Math.cos(n12) * radius);
        }

        GL11.glEnd();
        GL11.glPushMatrix();
        GL11.glShadeModel(7425);
        GL11.glLineWidth(2.0F);
        GL11.glBegin(2);
        if (n7 != 0L) {
            this.glColor(n7);
        }

        for (int n13 = 0; n13 <= 90; n13 += 3) {
            double n14 = n13 * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(n14) * radius * -1.0, y + radius + Math.cos(n14) * radius * -1.0);
        }

        for (int n15 = 90; n15 <= 180; n15 += 3) {
            double n16 = n15 * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(n16) * radius * -1.0, y2 - radius + Math.cos(n16) * radius * -1.0);
        }

        if (n8 != 0) {
            this.glColor(n8);
        }

        for (int n17 = 0; n17 <= 90; n17 += 3) {
            double n18 = n17 * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x2 - radius + Math.sin(n18) * radius, y2 - radius + Math.cos(n18) * radius);
        }

        for (int n19 = 90; n19 <= 180; n19 += 3) {
            double n20 = n19 * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x2 - radius + Math.sin(n20) * radius, y + radius + Math.cos(n20) * radius);
        }

        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
        GL11.glEnable(3553);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
        GL11.glLineWidth(1.0F);
        GL11.glShadeModel(7424);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void glColor(int color) {
        GL11.glColor4f(
            (color >> 16 & 0xFF) / 255.0F,
            (color >> 8 & 0xFF) / 255.0F,
            (color & 0xFF) / 255.0F,
            (color >> 24 & 0xFF) / 255.0F
        );
    }

    private static final class CategoryIconStacks {
        private final ItemStack normalStack;
        private final ItemStack activeStack;

        private CategoryIconStacks(ItemStack normalStack, ItemStack activeStack) {
            this.normalStack = normalStack;
            this.activeStack = activeStack;
        }
    }

    private static final class CategoryLayoutMetrics {
        private final float visibleHeight;
        private final float minScrollY;
        private final float contentBottom;

        private CategoryLayoutMetrics(float visibleHeight, float minScrollY, float contentBottom) {
            this.visibleHeight = visibleHeight;
            this.minScrollY = minScrollY;
            this.contentBottom = contentBottom;
        }
    }
}
