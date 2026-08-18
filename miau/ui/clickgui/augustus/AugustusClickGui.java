package miau.ui.clickgui.augustus;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miau.Miau;
import miau.module.Module;
import miau.module.modules.render.HUD;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.client.KeyBindUtil;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import miau.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class AugustusClickGui extends GuiScreen {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public static Color themeColor = new Color(0, 233, 255);
    public static String colorMode = "Custom";
    public static int customR = 0;
    public static int customG = 233;
    public static int customB = 255;
    public static final Color PALETTE_BG = new Color(724757);
    public static final Color PALETTE_PANEL = new Color(1185825);
    public static final Color PALETTE_CARD = new Color(1514791);
    public static final Color PALETTE_CARD_HOVER = new Color(1844272);
    public static final Color PALETTE_ACCENT = new Color(55551);
    public static final Color PALETTE_TEXT = new Color(16777215);
    public static final Color PALETTE_SECONDARY = new Color(11121603);
    public static final Color PALETTE_DANGER = new Color(16734810);
    public static final Color PALETTE_SUCCESS = new Color(4644986);
    public static final Color PALETTE_BORDER = new Color(255, 255, 255, 20);
    public static final Color PALETTE_GLOW = new Color(0, 216, 255, 51);
    private static final String[] CATEGORIES = new String[]{
        "Combat", "Movement", "Player", "Render", "Ghost", "Network", "Minigames", "Misc"
    };
    private boolean showSettingsPopup = false;
    private String draggingSlider = null;
    private float panelX = 50.0F;
    private float panelY = 20.0F;
    private float panelWidth = 620.0F;
    private float panelHeight = 380.0F;
    private float categoryScroll = 0.0F;
    private float moduleScroll = 0.0F;
    private float valueScroll = 0.0F;
    private float categoryScrollTarget = 0.0F;
    private float moduleScrollTarget = 0.0F;
    private float valueScrollTarget = 0.0F;
    private boolean isDragging = false;
    private float dragOffX = 0.0F;
    private float dragOffY = 0.0F;
    private String currentCategory = "Render";
    private String currentModule = null;
    private float modulePanelWidth = 170.0F;
    private boolean isResizingPanel = false;
    private float resizeStartX = 0.0F;
    private float resizeStartWidth = 0.0F;
    private AugustusClickGui.AbstractValueComponent draggingComponent = null;
    private float dragBarX = 0.0F;
    private float dragBarX2 = 0.0F;
    private final Map<Module, List<AugustusClickGui.AbstractValueComponent>> componentCache = new HashMap<>();
    private final List<AugustusClickGui.TargetEntry> targetEntries = new ArrayList<>();
    private final Map<String, AugustusClickGui.AnimFloat> categoryHover = new HashMap<>();
    private final Map<String, AugustusClickGui.AnimFloat> moduleHover = new HashMap<>();
    private final AugustusClickGui.AnimFloat closeHover = new AugustusClickGui.AnimFloat(0.0F);
    private final AugustusClickGui.AnimFloat settingHover = new AugustusClickGui.AnimFloat(0.0F);
    private final AugustusClickGui.AnimFloat profileHover = new AugustusClickGui.AnimFloat(0.0F);
    private final AugustusClickGui.AnimFloat underlinePos = new AugustusClickGui.AnimFloat(0.0F);
    private final AugustusClickGui.AnimFloat underlineWidth = new AugustusClickGui.AnimFloat(0.0F);
    private final AugustusClickGui.AnimFloat openAnim = new AugustusClickGui.AnimFloat(0.0F);

    public AugustusClickGui() {
        this.targetEntries
            .add(
                new AugustusClickGui.TargetEntry(
                    "Players",
                    () -> AugustusClickGui.EntityTargets.player,
                    () -> AugustusClickGui.EntityTargets.player = !AugustusClickGui.EntityTargets.player
                )
            );
        this.targetEntries
            .add(
                new AugustusClickGui.TargetEntry(
                    "Mobs",
                    () -> AugustusClickGui.EntityTargets.mob,
                    () -> AugustusClickGui.EntityTargets.mob = !AugustusClickGui.EntityTargets.mob
                )
            );
        this.targetEntries
            .add(
                new AugustusClickGui.TargetEntry(
                    "Animals",
                    () -> AugustusClickGui.EntityTargets.animal,
                    () -> AugustusClickGui.EntityTargets.animal = !AugustusClickGui.EntityTargets.animal
                )
            );
        this.targetEntries
            .add(
                new AugustusClickGui.TargetEntry(
                    "Invisible",
                    () -> AugustusClickGui.EntityTargets.invisible,
                    () -> AugustusClickGui.EntityTargets.invisible = !AugustusClickGui.EntityTargets.invisible
                )
            );
        this.targetEntries
            .add(
                new AugustusClickGui.TargetEntry(
                    "Dead",
                    () -> AugustusClickGui.EntityTargets.dead,
                    () -> AugustusClickGui.EntityTargets.dead = !AugustusClickGui.EntityTargets.dead
                )
            );
    }

    public void func_73866_w_() {
        this.isDragging = false;
        this.isResizingPanel = false;
        this.openAnim.value = 0.0F;
        this.openAnim.current = 0.0F;
    }

    public void func_146281_b() {
        this.isDragging = false;
        this.isResizingPanel = false;
        this.showSettingsPopup = false;
    }

    public boolean func_73868_f() {
        return false;
    }

    private float getTitleHeight() {
        return 50.0F;
    }

    private float endX() {
        return this.panelX + this.panelWidth;
    }

    private float endY() {
        return this.panelY + this.panelHeight;
    }

    private float getCategoryContentWidth() {
        Font fontCategory = FontRepository.getFont("augustus", 24.0F);
        float w = 0.0F;

        for (String category : CATEGORIES) {
            w += fontCategory.getStringWidth(category) + 22.0F;
        }

        return w + (fontCategory.getStringWidth("Targets") + 22.0F);
    }

    private void clampCategoryScroll() {
        float availW = this.endX() - 40.0F - (this.panelX + 145.0F);
        float contentW = this.getCategoryContentWidth();
        float maxScroll = Math.min(0.0F, availW - contentW);
        if (this.categoryScrollTarget > 0.0F) {
            this.categoryScrollTarget = 0.0F;
        }

        if (this.categoryScrollTarget < maxScroll) {
            this.categoryScrollTarget = maxScroll;
        }

        if (this.categoryScroll > 0.0F) {
            this.categoryScroll = 0.0F;
        }

        if (this.categoryScroll < maxScroll) {
            this.categoryScroll = maxScroll;
        }
    }

    private void setCategory(String category) {
        this.currentCategory = category;
        this.moduleScroll = 0.0F;
        this.moduleScrollTarget = 0.0F;
        this.valueScroll = 0.0F;
        this.valueScrollTarget = 0.0F;
        this.currentModule = null;
    }

    private void setModule(String moduleName) {
        this.currentModule = moduleName;
        this.valueScroll = 0.0F;
        this.valueScrollTarget = 0.0F;
    }

    private List<Module> getFilteredModules() {
        List<Module> filteredModules = new ArrayList<>();
        if (this.currentCategory == null) {
            return filteredModules;
        }

        for (Module mod : Miau.moduleManager.modules.values()) {
            if (mod != null
                && mod.getCategory() != null
                && String.valueOf(mod.getCategory()).equalsIgnoreCase(this.currentCategory)) {
                filteredModules.add(mod);
            }
        }

        return filteredModules;
    }

    private List<AugustusClickGui.AbstractValueComponent> getComponents(Module module) {
        List<AugustusClickGui.AbstractValueComponent> components = this.componentCache.get(module);
        if (components == null) {
            components = new ArrayList<>();
            if (module.getValues() != null) {
                for (Property<?> prop : module.getValues()) {
                    if (prop instanceof BooleanProperty) {
                        components.add(new AugustusClickGui.BoolValueComponent((BooleanProperty)prop));
                    } else if (prop instanceof IntProperty) {
                        components.add(new AugustusClickGui.IntValueComponent((IntProperty)prop));
                    } else if (prop instanceof FloatProperty) {
                        components.add(new AugustusClickGui.FloatValueComponent((FloatProperty)prop));
                    } else if (prop instanceof ModeProperty) {
                        components.add(new AugustusClickGui.ListValueComponent((ModeProperty)prop));
                    } else if (prop.getClass().getName().toLowerCase().contains("color")) {
                        components.add(new AugustusClickGui.ColorValueComponent(prop));
                    }
                }
            }

            this.componentCache.put(module, components);
        }

        return components;
    }

    private void startScissorBox(float x, float y, float x2, float y2) {
        GL11.glEnable(3089);
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.func_78325_e();
        int screenHeight = mc.field_71440_d;
        int renderX = (int)(x * scale);
        int renderY = (int)(screenHeight - y2 * scale);
        int renderWidth = (int)((x2 - x) * scale);
        int renderHeight = (int)((y2 - y) * scale);
        GL11.glScissor(renderX, renderY, Math.max(0, renderWidth), Math.max(0, renderHeight));
    }

    private void updateThemeColor() {
        if ("HUD".equalsIgnoreCase(colorMode)) {
            try {
                HUD hud = (HUD)Miau.moduleManager.modules.get(HUD.class);
                if (hud != null) {
                    themeColor = hud.getColor(System.currentTimeMillis());
                }
            } catch (Exception var2) {
            }
        } else {
            themeColor = new Color(customR, customG, customB);
        }
    }

    private static float easeTo(float current, float target, float partialTicks) {
        float factor = 1.0F - (float)Math.pow(0.03, partialTicks);
        float next = current + (target - current) * factor;
        if (Math.abs(target - next) < 0.01F) {
            next = target;
        }

        return next;
    }

    private static Color blend(Color a, Color b, float t) {
        return new Color(
            (int)(a.getRed() + (b.getRed() - a.getRed()) * t),
            (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
            (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
        );
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        this.updateThemeColor();
        this.openAnim.value = 1.0F;
        this.openAnim.current = easeTo(this.openAnim.current, this.openAnim.value, partialTicks);
        float open = this.openAnim.current;
        float openScale = 0.93F + 0.07F * open;
        float cx = this.panelX + this.panelWidth / 2.0F;
        float cy = this.panelY + this.panelHeight / 2.0F;
        GL11.glPushMatrix();
        GL11.glTranslatef(cx, cy, 0.0F);
        GL11.glScalef(openScale, openScale, 1.0F);
        GL11.glTranslatef(-cx, -cy, 0.0F);
        if (this.isDragging) {
            this.panelX = mouseX - this.dragOffX;
            this.panelY = mouseY - this.dragOffY;
        }

        if (this.isResizingPanel) {
            float delta = mouseX - this.resizeStartX;
            this.modulePanelWidth = this.resizeStartWidth + delta;
            if (this.modulePanelWidth < 140.0F) {
                this.modulePanelWidth = 140.0F;
            }

            if (this.modulePanelWidth > this.panelWidth - 180.0F) {
                this.modulePanelWidth = this.panelWidth - 180.0F;
            }
        }

        this.categoryScroll = easeTo(this.categoryScroll, this.categoryScrollTarget, partialTicks);
        this.moduleScroll = easeTo(this.moduleScroll, this.moduleScrollTarget, partialTicks);
        this.valueScroll = easeTo(this.valueScroll, this.valueScrollTarget, partialTicks);
        float ex = this.endX();
        float ey = this.endY();
        float th = this.getTitleHeight();
        RoundedUtils.drawRound(
            0.0F, 0.0F, this.field_146294_l, this.field_146295_m, 0.0F, new Color(724757, true).getRGB()
        );
        RenderUtil.drawRect(
            0.0F, 0.0F, this.field_146294_l, this.field_146295_m, new Color(724757).getRGB() & 16777215 | 1711276032
        );
        RoundedUtils.drawRound(
            this.panelX + 2.0F,
            this.panelY + 4.0F,
            this.panelWidth + 4.0F,
            this.panelHeight + 4.0F,
            14.0F,
            new Color(0, 0, 0, 70).getRGB()
        );
        RoundedUtils.drawRound(
            this.panelX, this.panelY, this.panelWidth, this.panelHeight, 14.0F, true, new Color(1185825, true).getRGB()
        );
        RoundedUtils.drawRoundOutline(
            this.panelX,
            this.panelY,
            this.panelWidth,
            this.panelHeight,
            14.0F,
            1.0F,
            new Color(0, 0, 0, 0),
            PALETTE_BORDER
        );
        RoundedUtils.drawGradientHorizontal(
            this.panelX + 5.0F,
            this.panelY + th - 1.5F,
            this.panelWidth - 10.0F,
            1.5F,
            0.75F,
            new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0),
            themeColor
        );
        Font fontLarge = FontRepository.getFont("augustus", 46.0F);
        fontLarge.draw("CLICKGUI", this.panelX + 10.0F, this.panelY + 4.0F, new Color(255, 255, 255).getRGB(), false);
        float closeX = ex - 26.0F;
        float closeY = this.panelY + 12.0F;
        boolean hoverClose = this.isHover(mouseX, mouseY, closeX - 6.0F, closeY - 6.0F, closeX + 20.0F, closeY + 20.0F);
        this.closeHover.value = hoverClose ? 1.0F : 0.0F;
        this.closeHover.current = easeTo(this.closeHover.current, this.closeHover.value, partialTicks);
        RoundedUtils.drawRound(
            closeX - 5.0F,
            closeY - 5.0F,
            24.0F,
            24.0F,
            7.0F,
            blend(PALETTE_CARD, PALETTE_DANGER, this.closeHover.current * 0.85F)
        );
        Font fontMedium = FontRepository.getFont("augustus", 22.0F);
        fontMedium.draw(
            "X",
            closeX + 1.0F,
            closeY - 1.0F,
            blend(new Color(160, 160, 160), new Color(255, 255, 255), this.closeHover.current).getRGB(),
            false
        );
        Font fontCategory = FontRepository.getFont("augustus", 24.0F);
        float catAreaStart = this.panelX + 145.0F;
        float catAreaEnd = ex - 40.0F;
        float catPanelY = this.panelY + 12.0F;
        this.startScissorBox(catAreaStart, this.panelY, catAreaEnd, this.panelY + th);
        float catPos = this.categoryScroll;

        for (String category : CATEGORIES) {
            float sw = fontCategory.getStringWidth(category);
            float sx = catAreaStart + catPos;
            boolean active = this.currentCategory != null && this.currentCategory.equalsIgnoreCase(category);
            boolean hover = this.isHover(
                mouseX,
                mouseY,
                sx - 6.0F,
                catPanelY - 6.0F,
                sx + sw + 6.0F,
                catPanelY + fontCategory.getFontHeight() + 6.0F
            );
            AugustusClickGui.AnimFloat anim = this.categoryHover
                .computeIfAbsent(category, k -> new AugustusClickGui.AnimFloat(0.0F));
            anim.value = hover ? 1.0F : 0.0F;
            anim.current = easeTo(anim.current, anim.value, partialTicks);
            RoundedUtils.drawRound(
                sx - 6.0F,
                catPanelY - 3.0F,
                sw + 12.0F,
                fontCategory.getFontHeight() + 6.0F,
                6.0F,
                blend(new Color(255, 255, 255, 8), themeColor, Math.max(active ? 0.9F : 0.0F, anim.current * 0.55F))
            );
            Color color = active ? new Color(255, 255, 255) : blend(PALETTE_SECONDARY, PALETTE_TEXT, anim.current);
            fontCategory.draw(category, sx, catPanelY, color.getRGB(), false);
            if (active) {
                RenderUtil.fillCircle(
                    sx + sw + 10.0F,
                    catPanelY + fontCategory.getFontHeight() / 2.0F + 1.0F,
                    2.5,
                    12,
                    themeColor.getRGB()
                );
            }

            catPos += sw + 22.0F;
        }

        String targetsLabel = "Targets";
        float tsw = fontCategory.getStringWidth(targetsLabel);
        float targetsStart = catAreaStart + catPos;
        boolean targetsActive = this.currentCategory == null;
        AugustusClickGui.AnimFloat targetsAnim = this.categoryHover
            .computeIfAbsent("Targets", k -> new AugustusClickGui.AnimFloat(0.0F));
        targetsAnim.value = this.isHover(
                mouseX,
                mouseY,
                targetsStart - 6.0F,
                catPanelY - 6.0F,
                targetsStart + tsw + 6.0F,
                catPanelY + fontCategory.getFontHeight() + 6.0F
            )
            ? 1.0F
            : 0.0F;
        targetsAnim.current = easeTo(targetsAnim.current, targetsAnim.value, partialTicks);
        RoundedUtils.drawRound(
            targetsStart - 6.0F,
            catPanelY - 3.0F,
            tsw + 12.0F,
            fontCategory.getFontHeight() + 6.0F,
            6.0F,
            blend(
                new Color(255, 255, 255, 8),
                themeColor,
                Math.max(targetsActive ? 0.9F : 0.0F, targetsAnim.current * 0.55F)
            )
        );
        fontCategory.draw(
            targetsLabel,
            targetsStart,
            catPanelY,
            targetsActive ? new Color(255, 255, 255).getRGB() : PALETTE_SECONDARY.getRGB(),
            false
        );
        if (targetsActive) {
            RenderUtil.fillCircle(
                targetsStart + tsw + 10.0F,
                catPanelY + fontCategory.getFontHeight() / 2.0F + 1.0F,
                2.5,
                12,
                themeColor.getRGB()
            );
        }

        float targetUnderlineX = 0.0F;
        float targetUnderlineW = 0.0F;
        if (this.currentCategory == null) {
            targetUnderlineX = targetsStart;
            targetUnderlineW = tsw;
        } else {
            float tmpPos = this.categoryScroll;

            for (String category : CATEGORIES) {
                float sw = fontCategory.getStringWidth(category);
                if (this.currentCategory.equalsIgnoreCase(category)) {
                    targetUnderlineX = catAreaStart + tmpPos;
                    targetUnderlineW = sw;
                    break;
                }

                tmpPos += sw + 22.0F;
            }
        }

        this.underlinePos.current = easeTo(this.underlinePos.current, targetUnderlineX, partialTicks);
        this.underlineWidth.current = easeTo(this.underlineWidth.current, targetUnderlineW, partialTicks);
        RoundedUtils.drawGradientHorizontal(
            this.underlinePos.current,
            this.panelY + th - 2.5F,
            this.underlineWidth.current,
            2.5F,
            1.25F,
            new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0),
            themeColor
        );
        float catFadeW = 22.0F;
        float catFadeY = catPanelY - 5.0F;
        float catFadeH = fontCategory.getFontHeight() + 10.0F;
        float contentW = this.getCategoryContentWidth();
        float availW = catAreaEnd - catAreaStart;
        if (this.categoryScroll < -1.0F) {
            RoundedUtils.drawGradientHorizontal(
                catAreaStart, catFadeY, catFadeW, catFadeH, 0.0F, new Color(18, 24, 33), new Color(18, 24, 33, 0)
            );
        }

        if (contentW > availW) {
            RoundedUtils.drawGradientHorizontal(
                catAreaEnd - catFadeW,
                catFadeY,
                catFadeW,
                catFadeH,
                0.0F,
                new Color(18, 24, 33, 0),
                new Color(18, 24, 33)
            );
        }

        GL11.glDisable(3089);
        RoundedUtils.drawRound(
            this.panelX + this.modulePanelWidth - 1.5F,
            this.panelY + th,
            3.0F,
            this.panelHeight - th,
            1.5F,
            new Color(255, 255, 255, 14)
        );
        float modulePanelStartY = this.panelY + th + 2.0F;
        if (this.currentCategory == null) {
            this.drawTargetsScreen(mouseX, mouseY, ex, ey, modulePanelStartY, this.panelY + th);
        } else {
            this.drawModulesScreen(mouseX, mouseY, ex, ey, modulePanelStartY, this.panelY + th, th);
        }

        float profileY = ey - 58.0F;
        boolean hoverProfile = this.isHover(
            mouseX, mouseY, this.panelX, profileY, this.panelX + this.modulePanelWidth, ey
        );
        this.profileHover.value = hoverProfile ? 1.0F : 0.0F;
        this.profileHover.current = easeTo(this.profileHover.current, this.profileHover.value, partialTicks);
        RoundedUtils.drawRound(
            this.panelX + 2.0F,
            profileY,
            this.modulePanelWidth - 4.0F,
            56.0F,
            10.0F,
            blend(PALETTE_CARD, new Color(1976372), this.profileHover.current * 0.6F)
        );
        RoundedUtils.drawRoundOutline(
            this.panelX + 2.0F,
            profileY,
            this.modulePanelWidth - 4.0F,
            56.0F,
            10.0F,
            1.0F,
            new Color(0, 0, 0, 0),
            PALETTE_BORDER
        );
        RoundedUtils.drawGradientVertical(
            this.panelX + 2.0F, profileY, this.modulePanelWidth - 4.0F, 3.0F, 1.5F, new Color(0, 0, 0, 0), themeColor
        );
        if (mc.field_71439_g != null) {
            RoundedUtils.drawRound(
                this.panelX + 8.0F, profileY + 10.0F, 36.0F, 36.0F, 8.0F, new Color(255, 255, 255, 12)
            );
            GL11.glPushMatrix();
            GL11.glEnable(3089);
            ScaledResolution sr = new ScaledResolution(mc);
            int scale = sr.func_78325_e();
            int avX = (int)((this.panelX + 8.0F) * scale);
            int avY = (int)(mc.field_71440_d - (profileY + 46.0F) * scale);
            GL11.glScissor(avX, avY, (int)(36.0F * scale), (int)(36.0F * scale));
            mc.func_110434_K().func_110577_a(mc.field_71439_g.func_110306_p());
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            Gui.func_152125_a(
                (int)(this.panelX + 8.0F), (int)(profileY + 10.0F), 8.0F, 8.0F, 8, 8, 36, 36, 64.0F, 64.0F
            );
            GL11.glDisable(3089);
            GL11.glPopMatrix();
            RoundedUtils.drawRoundOutline(
                this.panelX + 8.0F, profileY + 10.0F, 36.0F, 36.0F, 8.0F, 1.0F, new Color(0, 0, 0, 0), PALETTE_BORDER
            );
            RenderUtil.fillCircle(this.panelX + 39.0F, profileY + 41.0F, 4.0, 14, PALETTE_SUCCESS.getRGB());
            RoundedUtils.drawRoundOutline(
                this.panelX + 39.0F - 4.0F,
                profileY + 41.0F - 4.0F,
                8.0F,
                8.0F,
                4.0F,
                1.0F,
                new Color(0, 0, 0, 0),
                new Color(0, 0, 0, 80)
            );
            Font fontSmall = FontRepository.getFont("augustus", 14.0F);
            fontSmall.draw(
                mc.field_71439_g.func_70005_c_(),
                this.panelX + 52.0F,
                profileY + 8.0F,
                new Color(240, 244, 250).getRGB(),
                false
            );
            Font rankFont = FontRepository.getFont("augustus", 9.0F);
            String rank = "PREMIUM";
            float rw = rankFont.getStringWidth(rank) + 7.0F;
            RoundedUtils.drawRound(
                this.panelX + 52.0F + fontSmall.getStringWidth(mc.field_71439_g.func_70005_c_()) + 6.0F,
                profileY + 10.0F,
                rw,
                9.0F,
                4.5F,
                new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 40)
            );
            rankFont.draw(
                rank,
                this.panelX + 52.0F + fontSmall.getStringWidth(mc.field_71439_g.func_70005_c_()) + 9.5F,
                profileY + 10.0F,
                themeColor.getRGB(),
                false
            );
            Font fontTiny = FontRepository.getFont("augustus", 11.0F);
            int mins = mc.field_71439_g.field_70173_aa / 1200;
            int secs = mc.field_71439_g.field_70173_aa % 1200 / 20;
            fontTiny.draw(
                String.format("Session  %02d:%02d", mins, secs),
                this.panelX + 52.0F,
                profileY + 22.0F,
                PALETTE_SECONDARY.getRGB(),
                false
            );
            int fps = Minecraft.func_175610_ah();
            int ping = 0;

            try {
                if (mc.field_71439_g.field_71174_a.func_175102_a(mc.field_71439_g.func_110124_au()) != null) {
                    ping = mc.field_71439_g
                        .field_71174_a
                        .func_175102_a(mc.field_71439_g.func_110124_au())
                        .func_178853_c();
                }
            } catch (Exception var51) {
            }

            long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576L;
            fontTiny.draw(
                "FPS " + fps,
                this.panelX + 52.0F,
                profileY + 34.0F,
                blend(PALETTE_SUCCESS, themeColor, 0.35F).getRGB(),
                false
            );
            fontTiny.draw(
                "Ping " + ping + "ms", this.panelX + 52.0F + 44.0F, profileY + 34.0F, themeColor.getRGB(), false
            );
            fontTiny.draw(
                "Mem " + usedMem + "MB",
                this.panelX + 52.0F + 44.0F + 66.0F,
                profileY + 34.0F,
                PALETTE_SECONDARY.getRGB(),
                false
            );
        }

        float settingX = this.panelX + this.modulePanelWidth - 28.0F;
        float settingY = profileY + 14.0F;
        boolean hoverSetting = this.isHover(
            mouseX, mouseY, settingX - 4.0F, settingY - 4.0F, settingX + 18.0F, settingY + 18.0F
        );
        this.settingHover.value = hoverSetting ? 1.0F : 0.0F;
        this.settingHover.current = easeTo(this.settingHover.current, this.settingHover.value, partialTicks);
        Font fontIcon = FontRepository.getFont("augustus", 22.0F);
        RoundedUtils.drawRound(
            settingX - 3.0F,
            settingY - 3.0F,
            22.0F,
            22.0F,
            7.0F,
            blend(PALETTE_CARD, themeColor, this.settingHover.current * 0.55F)
        );
        fontIcon.draw(
            "⚙",
            settingX + 2.0F,
            settingY + 1.0F,
            blend(new Color(200, 200, 200), Color.WHITE, this.settingHover.current).getRGB(),
            false
        );
        if (this.showSettingsPopup) {
            this.drawColorSettingsPopup(mouseX, mouseY, settingX, settingY);
        }

        if (open < 1.0F) {
            RenderUtil.drawRect(
                0.0F,
                0.0F,
                this.field_146294_l,
                this.field_146295_m,
                new Color(0, 0, 0, (int)((1.0F - open) * 120.0F)).getRGB()
            );
        }

        GL11.glPopMatrix();
    }

    private void drawColorSettingsPopup(int mouseX, int mouseY, float settingX, float settingY) {
        float popX = this.panelX + this.modulePanelWidth + 10.0F;
        float popY = this.endY() - ("Custom".equalsIgnoreCase(colorMode) ? 140.0F : 80.0F);
        float popW = 160.0F;
        float popH = "Custom".equalsIgnoreCase(colorMode) ? 130.0F : 70.0F;
        RoundedUtils.drawRound(popX, popY, popW, popH, 10.0F, new Color(1119774, true).getRGB());
        RoundedUtils.drawRoundOutline(popX, popY, popW, popH, 10.0F, 1.0F, new Color(0, 0, 0, 0), PALETTE_BORDER);
        Font fontSmall = FontRepository.getFont("augustus", 18.0F);
        float btn1X = popX + 10.0F;
        float btn1Y = popY + 10.0F;
        float btnW = 65.0F;
        float btnH = 20.0F;
        boolean activeCustom = "Custom".equalsIgnoreCase(colorMode);
        boolean hoverCustom = this.isHover(mouseX, mouseY, btn1X, btn1Y, btn1X + btnW, btn1Y + btnH);
        RoundedUtils.drawRound(
            btn1X,
            btn1Y,
            btnW,
            btnH,
            6.0F,
            activeCustom ? themeColor : blend(PALETTE_CARD, PALETTE_CARD_HOVER, hoverCustom ? 0.8F : 0.0F)
        );
        fontSmall.draw(
            "Custom", btn1X + 8.0F, btn1Y + 3.0F, activeCustom ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false
        );
        float btn2X = popX + 85.0F;
        float btn2Y = popY + 10.0F;
        boolean activeHUD = "HUD".equalsIgnoreCase(colorMode);
        boolean hoverHUD = this.isHover(mouseX, mouseY, btn2X, btn2Y, btn2X + btnW, btn2Y + btnH);
        RoundedUtils.drawRound(
            btn2X,
            btn2Y,
            btnW,
            btnH,
            6.0F,
            activeHUD ? themeColor : blend(PALETTE_CARD, PALETTE_CARD_HOVER, hoverHUD ? 0.8F : 0.0F)
        );
        fontSmall.draw(
            "HUD", btn2X + 18.0F, btn2Y + 3.0F, activeHUD ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false
        );
        if (activeCustom) {
            float sliderY = popY + 40.0F;
            this.drawColorSlider("R", customR, popX + 10.0F, sliderY, popW - 20.0F, new Color(255, 80, 80));
            this.drawColorSlider("G", customG, popX + 10.0F, sliderY + 25.0F, popW - 20.0F, new Color(80, 255, 80));
            this.drawColorSlider("B", customB, popX + 10.0F, sliderY + 50.0F, popW - 20.0F, new Color(80, 180, 255));
            if (this.draggingSlider != null && Mouse.isButtonDown(0)) {
                float barX = popX + 25.0F;
                float barW = popW - 40.0F;
                float pct = Math.max(0.0F, Math.min(1.0F, (mouseX - barX) / barW));
                int val = Math.round(pct * 255.0F);
                if ("R".equals(this.draggingSlider)) {
                    customR = val;
                }

                if ("G".equals(this.draggingSlider)) {
                    customG = val;
                }

                if ("B".equals(this.draggingSlider)) {
                    customB = val;
                }
            }
        }
    }

    private void drawColorSlider(String label, int value, float x, float y, float width, Color col) {
        Font font = FontRepository.getFont("augustus", 16.0F);
        font.draw(label, x, y + 2.0F, col.getRGB(), false);
        float barX = x + 15.0F;
        float barY = y + 3.0F;
        float barW = width - 25.0F;
        float barH = 6.0F;
        RoundedUtils.drawRound(barX, barY, barW, barH, 3.0F, new Color(724757, true).getRGB());
        RoundedUtils.drawRoundOutline(barX, barY, barW, barH, 3.0F, 0.5F, new Color(0, 0, 0, 0), PALETTE_BORDER);
        float pct = value / 255.0F;
        float fillW = barW * pct;
        if (fillW > 1.0F) {
            RoundedUtils.drawGradientHorizontal(
                barX, barY, fillW, barH, 3.0F, new Color(col.getRed(), col.getGreen(), col.getBlue(), 60), col
            );
        }

        float knobX = barX + fillW;
        RoundedUtils.drawRound(knobX - 3.0F, barY - 2.5F, 7.0F, 11.0F, 5.5F, Color.WHITE);
        RoundedUtils.drawRound(knobX - 3.0F, barY - 2.5F, 7.0F, 11.0F, 5.5F, new Color(255, 255, 255, 200));
        font.draw(String.valueOf(value), barX + barW + 5.0F, y + 2.0F, new Color(210, 214, 222).getRGB(), false);
    }

    private void drawTargetsScreen(
        int mouseX, int mouseY, float ex, float ey, float modulePanelStartY, float catPanelEndY
    ) {
        Font fontMedium = FontRepository.getFont("augustus", 22.0F);
        float sepX = this.panelX + this.modulePanelWidth;
        RoundedUtils.drawRound(
            this.panelX + 2.0F,
            modulePanelStartY,
            sepX - this.panelX - 4.0F,
            ey - 58.0F - modulePanelStartY,
            8.0F,
            PALETTE_CARD
        );
        fontMedium.draw("Targets", this.panelX + 10.0F, modulePanelStartY + 8.0F, themeColor.getRGB(), false);
        float itemY = modulePanelStartY + 35.0F;

        for (AugustusClickGui.TargetEntry entry : this.targetEntries) {
            boolean active = entry.stateGetter.get();
            boolean hover = this.isHover(
                mouseX,
                mouseY,
                this.panelX + 6.0F,
                itemY - 3.0F,
                this.panelX + this.modulePanelWidth - 6.0F,
                itemY + fontMedium.getFontHeight() + 3.0F
            );
            AugustusClickGui.AnimFloat anim = this.categoryHover
                .computeIfAbsent("entry_" + entry.label, k -> new AugustusClickGui.AnimFloat(0.0F));
            anim.value = hover ? 1.0F : 0.0F;
            anim.current = easeTo(anim.current, anim.value, 0.05F);
            if (hover || active) {
                RoundedUtils.drawRound(
                    this.panelX + 6.0F,
                    itemY - 3.0F,
                    this.modulePanelWidth - 12.0F,
                    fontMedium.getFontHeight() + 6.0F,
                    6.0F,
                    active
                        ? new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 22)
                        : blend(PALETTE_CARD, PALETTE_CARD_HOVER, anim.current)
                );
            }

            Color color = active ? themeColor : blend(PALETTE_SECONDARY, PALETTE_TEXT, anim.current);
            fontMedium.draw(entry.label, this.panelX + 12.0F, itemY, color.getRGB(), false);
            RoundedUtils.drawRound(
                this.panelX + this.modulePanelWidth - 22.0F,
                itemY + 5.0F,
                12.0F,
                12.0F,
                4.0F,
                active ? themeColor : new Color(40, 46, 56)
            );
            if (active) {
                RenderUtil.fillCircle(
                    this.panelX + this.modulePanelWidth - 16.0F, itemY + 11.0F, 2.5, 10, Color.WHITE.getRGB()
                );
            }

            itemY += fontMedium.getFontHeight() + 8.0F;
        }

        Font fontLarge = FontRepository.getFont("augustus", 32.0F);
        fontLarge.draw("Entity Targets", sepX + 15.0F, catPanelEndY + 15.0F, new Color(255, 255, 255).getRGB(), false);
        RoundedUtils.drawGradientHorizontal(
            sepX + 18.0F,
            catPanelEndY + fontLarge.getFontHeight() + 20.0F,
            80.0F,
            2.0F,
            1.0F,
            new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0),
            themeColor
        );
    }

    private void drawModulesScreen(
        int mouseX, int mouseY, float ex, float ey, float modulePanelStartY, float catPanelEndY, float th
    ) {
        if (this.currentCategory != null) {
            Font fontMedium = FontRepository.getFont("augustus", 22.0F);
            float moduleItemHeight = fontMedium.getFontHeight() + 10.0F;
            float sepX = this.panelX + this.modulePanelWidth;
            RoundedUtils.drawRound(
                this.panelX + 2.0F,
                modulePanelStartY,
                sepX - this.panelX - 2.0F,
                ey - 58.0F - modulePanelStartY,
                8.0F,
                PALETTE_CARD
            );
            List<Module> filteredModules = this.getFilteredModules();
            this.startScissorBox(this.panelX + 2.0F, modulePanelStartY, sepX, ey - 58.0F);
            float currentY = modulePanelStartY + 8.0F + this.moduleScroll;

            for (Module module : filteredModules) {
                float msx = this.panelX + 12.0F;
                Color colorState = module.isEnabled() ? themeColor : blend(PALETTE_SECONDARY, PALETTE_TEXT, 0.15F);
                boolean isSelected = module.getName().equals(this.currentModule);
                boolean hover = this.isHover(
                    mouseX, mouseY, this.panelX + 2.0F, currentY - 3.0F, sepX, currentY + moduleItemHeight - 3.0F
                );
                AugustusClickGui.AnimFloat anim = this.moduleHover
                    .computeIfAbsent(module.getName(), k -> new AugustusClickGui.AnimFloat(0.0F));
                anim.value = hover ? 1.0F : 0.0F;
                anim.current = easeTo(anim.current, anim.value, 0.05F);
                RoundedUtils.drawRound(
                    this.panelX + 4.0F,
                    currentY - 3.0F,
                    sepX - this.panelX - 6.0F,
                    moduleItemHeight - 4.0F,
                    7.0F,
                    isSelected
                        ? new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 30)
                        : blend(new Color(255, 255, 255, 0), PALETTE_CARD_HOVER, anim.current * 0.9F)
                );
                if (isSelected) {
                    RoundedUtils.drawRoundOutline(
                        this.panelX + 4.0F,
                        currentY - 3.0F,
                        sepX - this.panelX - 6.0F,
                        moduleItemHeight - 4.0F,
                        7.0F,
                        1.0F,
                        new Color(0, 0, 0, 0),
                        themeColor
                    );
                }

                Color dotCol = module.isEnabled() ? themeColor : new Color(60, 68, 82);
                RenderUtil.fillCircle(
                    this.panelX + 10.0F, currentY + moduleItemHeight / 2.0F - 1.0F, 3.0, 12, dotCol.getRGB()
                );
                float nameX = this.panelX + 19.0F;
                fontMedium.draw(module.getName(), nameX, currentY, colorState.getRGB(), false);
                String keyName = KeyBindUtil.getKeyName(module.getKey());
                Font kfont = FontRepository.getFont("augustus", 12.0F);
                if (module.getKey() != 0) {
                    float kw = kfont.getStringWidth(keyName) + 7.0F;
                    float kx = this.panelX + this.modulePanelWidth - 34.0F - kw;
                    RoundedUtils.drawRound(
                        kx, currentY + 2.0F, kw, moduleItemHeight - 12.0F, 4.0F, new Color(255, 255, 255, 10)
                    );
                    kfont.draw(keyName, kx + 3.5F, currentY + 4.0F, new Color(160, 168, 180).getRGB(), false);
                }

                currentY += moduleItemHeight;
            }

            GL11.glDisable(3089);
            if (this.currentModule != null) {
                Module module = null;

                for (Module mod : Miau.moduleManager.modules.values()) {
                    if (mod.getName().equals(this.currentModule)) {
                        module = mod;
                        break;
                    }
                }

                if (module != null) {
                    Font fontLarge = FontRepository.getFont("augustus", 32.0F);
                    fontLarge.draw(
                        module.getName(), sepX + 18.0F, catPanelEndY + 15.0F, new Color(255, 255, 255).getRGB(), false
                    );
                    RoundedUtils.drawGradientHorizontal(
                        sepX + 20.0F,
                        catPanelEndY + fontLarge.getFontHeight() + 20.0F,
                        60.0F,
                        2.0F,
                        1.0F,
                        new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0),
                        themeColor
                    );
                    float valueStartY = catPanelEndY + fontLarge.getFontHeight() + 25.0F;
                    this.startScissorBox(sepX + 4.0F, valueStartY, ex - 4.0F, ey - 6.0F);
                    List<AugustusClickGui.AbstractValueComponent> components = this.getComponents(module);
                    float totalValueY = 0.0F;

                    for (AugustusClickGui.AbstractValueComponent component : components) {
                        if (component.value.isVisible()) {
                            float valueY = valueStartY + totalValueY + this.valueScroll;
                            component.drawValueName(sepX + 18.0F, valueY);
                            totalValueY += component.drawValue(
                                mouseX, mouseY, sepX + 18.0F, valueY, ex - 18.0F, ey - 6.0F
                            );
                        }
                    }

                    GL11.glDisable(3089);
                }
            }
        }
    }

    public void func_73864_a(int mouseX, int mouseY, int mouseButton) {
        float ex = this.endX();
        float ey = this.endY();
        float th = this.getTitleHeight();
        if (mouseButton == 0
            && this.isHover(mouseX, mouseY, ex - 30.0F, this.panelY + 8.0F, ex - 2.0F, this.panelY + 38.0F)) {
            mc.func_147108_a(null);
        } else {
            float profileY = ey - 58.0F;
            float settingX = this.panelX + this.modulePanelWidth - 28.0F;
            float settingY = profileY + 14.0F;
            if (mouseButton == 0
                && this.isHover(mouseX, mouseY, settingX - 4.0F, settingY - 4.0F, settingX + 18.0F, settingY + 18.0F)) {
                this.showSettingsPopup = !this.showSettingsPopup;
            } else {
                if (this.showSettingsPopup) {
                    float popX = this.panelX + this.modulePanelWidth + 10.0F;
                    float popY = this.endY() - ("Custom".equalsIgnoreCase(colorMode) ? 140.0F : 80.0F);
                    float popW = 160.0F;
                    if (mouseButton == 0) {
                        if (this.isHover(mouseX, mouseY, popX + 10.0F, popY + 10.0F, popX + 75.0F, popY + 30.0F)) {
                            colorMode = "Custom";
                            return;
                        }

                        if (this.isHover(mouseX, mouseY, popX + 85.0F, popY + 10.0F, popX + 150.0F, popY + 30.0F)) {
                            colorMode = "HUD";
                            return;
                        }

                        if ("Custom".equalsIgnoreCase(colorMode)) {
                            float sliderY = popY + 40.0F;
                            float barX = popX + 25.0F;
                            float barW = popW - 40.0F;
                            if (this.isHover(mouseX, mouseY, barX, sliderY - 2.0F, barX + barW, sliderY + 10.0F)) {
                                this.draggingSlider = "R";
                                return;
                            }

                            if (this.isHover(mouseX, mouseY, barX, sliderY + 23.0F, barX + barW, sliderY + 35.0F)) {
                                this.draggingSlider = "G";
                                return;
                            }

                            if (this.isHover(mouseX, mouseY, barX, sliderY + 48.0F, barX + barW, sliderY + 60.0F)) {
                                this.draggingSlider = "B";
                                return;
                            }
                        }
                    }
                }

                Font fontCategory = FontRepository.getFont("augustus", 24.0F);
                float catAreaStart = this.panelX + 145.0F;
                float catPanelY = this.panelY + 12.0F;
                float catPos = this.categoryScroll;

                for (String category : CATEGORIES) {
                    float sw = fontCategory.getStringWidth(category);
                    float sx = catAreaStart + catPos;
                    if (this.isHover(
                            mouseX,
                            mouseY,
                            sx - 6.0F,
                            catPanelY - 6.0F,
                            sx + sw + 6.0F,
                            catPanelY + fontCategory.getFontHeight() + 6.0F
                        )
                        && mouseButton == 0) {
                        this.setCategory(category);
                        return;
                    }

                    catPos += sw + 22.0F;
                }

                String targetsLabel = "Targets";
                float tsx = catAreaStart + catPos;
                float tex = tsx + fontCategory.getStringWidth(targetsLabel);
                if (this.isHover(
                        mouseX,
                        mouseY,
                        tsx - 6.0F,
                        catPanelY - 6.0F,
                        tex + 6.0F,
                        catPanelY + fontCategory.getFontHeight() + 6.0F
                    )
                    && mouseButton == 0) {
                    this.setCategory(null);
                } else if (mouseButton == 0
                    && this.isHover(mouseX, mouseY, this.panelX, this.panelY, ex, this.panelY + th)) {
                    this.isDragging = true;
                    this.dragOffX = mouseX - this.panelX;
                    this.dragOffY = mouseY - this.panelY;
                } else {
                    float sepX = this.panelX + this.modulePanelWidth;
                    if (mouseButton == 0
                        && mouseX >= sepX - 6.0F
                        && mouseX <= sepX + 6.0F
                        && mouseY > this.panelY + th
                        && mouseY < ey) {
                        this.isResizingPanel = true;
                        this.resizeStartX = mouseX;
                        this.resizeStartWidth = this.modulePanelWidth;
                    } else {
                        if (this.currentCategory == null) {
                            this.handleTargetsClick(mouseX, mouseY, mouseButton, this.panelY + th, ex, ey);
                        } else {
                            this.handleModuleClick(mouseX, mouseY, mouseButton, this.panelY + th, th, ex, ey);
                        }
                    }
                }
            }
        }
    }

    private void handleTargetsClick(int mouseX, int mouseY, int mouseButton, float catPanelEndY, float ex, float ey) {
        Font fontMedium = FontRepository.getFont("augustus", 22.0F);
        float modulePanelStartY = this.panelY + this.getTitleHeight() + 2.0F;
        float itemY = modulePanelStartY + 35.0F;

        for (AugustusClickGui.TargetEntry entry : this.targetEntries) {
            if (mouseButton == 0
                && this.isHover(
                    mouseX,
                    mouseY,
                    this.panelX + 6.0F,
                    itemY - 3.0F,
                    this.panelX + this.modulePanelWidth - 6.0F,
                    itemY + fontMedium.getFontHeight() + 3.0F
                )) {
                entry.toggle.toggle();
                return;
            }

            itemY += fontMedium.getFontHeight() + 8.0F;
        }
    }

    private void handleModuleClick(
        int mouseX, int mouseY, int mouseButton, float catPanelEndY, float th, float ex, float ey
    ) {
        if (this.currentCategory != null) {
            Font fontMedium = FontRepository.getFont("augustus", 22.0F);
            float modulePanelStartY = this.panelY + th + 2.0F;
            float moduleItemHeight = fontMedium.getFontHeight() + 10.0F;
            float sepX = this.panelX + this.modulePanelWidth;
            List<Module> filteredModules = this.getFilteredModules();
            if (this.isHover(mouseX, mouseY, this.panelX + 2.0F, modulePanelStartY, sepX, ey - 58.0F)) {
                float currentY = modulePanelStartY + 8.0F + this.moduleScroll;

                for (Module module : filteredModules) {
                    if (this.isHover(
                        mouseX, mouseY, this.panelX + 2.0F, currentY - 3.0F, sepX, currentY + moduleItemHeight - 3.0F
                    )) {
                        if (mouseButton == 0) {
                            module.toggle();
                        } else if (mouseButton == 1) {
                            this.setModule(module.getName());
                        }

                        return;
                    }

                    currentY += moduleItemHeight;
                }
            }

            if (this.currentModule != null) {
                Module module = null;

                for (Module mod : Miau.moduleManager.modules.values()) {
                    if (mod.getName().equals(this.currentModule)) {
                        module = mod;
                        break;
                    }
                }

                if (module != null) {
                    Font fontLarge = FontRepository.getFont("augustus", 32.0F);
                    float valueStartY = catPanelEndY + fontLarge.getFontHeight() + 25.0F;
                    List<AugustusClickGui.AbstractValueComponent> components = this.getComponents(module);
                    float totalValueY = 0.0F;

                    for (AugustusClickGui.AbstractValueComponent component : components) {
                        if (component.value.isVisible()) {
                            float valueY = valueStartY + totalValueY + this.valueScroll;
                            float labelX = sepX + 18.0F;
                            float valueX2 = ex - 18.0F;
                            float inc = component.mouseClicked(
                                mouseX, mouseY, labelX, valueY, valueX2, ey - 6.0F, mouseButton
                            );
                            if (inc == -1.0F) {
                                this.draggingComponent = component;
                                this.dragBarX = component.getBarStartX(labelX);
                                this.dragBarX2 = valueX2;
                                return;
                            }

                            if (inc == 0.0F) {
                                return;
                            }

                            totalValueY += inc;
                        }
                    }
                }
            }
        }
    }

    public void func_146274_d() {
        try {
            super.func_146274_d();
            int wheel = Mouse.getEventDWheel() != 0 ? Mouse.getEventDWheel() : Mouse.getDWheel();
            if (wheel != 0) {
                int mouseX = Mouse.getEventX() * this.field_146294_l / mc.field_71443_c;
                int mouseY = this.field_146295_m - Mouse.getEventY() * this.field_146295_m / mc.field_71440_d - 1;
                float sepX = this.panelX + this.modulePanelWidth;
                if (this.isHover(
                    mouseX, mouseY, this.panelX, this.panelY, this.endX(), this.panelY + this.getTitleHeight()
                )) {
                    this.categoryScrollTarget += wheel > 0 ? 30.0F : -30.0F;
                    this.clampCategoryScroll();
                } else if (this.isHover(mouseX, mouseY, this.panelX, this.panelY, sepX, this.endY())) {
                    this.moduleScrollTarget += wheel > 0 ? 25.0F : -25.0F;
                    Font fontMedium = FontRepository.getFont("augustus", 22.0F);
                    float moduleItemHeight = fontMedium.getFontHeight() + 10.0F;
                    List<Module> filteredModules = this.getFilteredModules();
                    float totalHeight = filteredModules.size() * moduleItemHeight + 16.0F;
                    float visibleHeight = this.endY() - 58.0F - (this.panelY + this.getTitleHeight() + 2.0F);
                    float maxScroll = Math.min(0.0F, visibleHeight - totalHeight);
                    if (this.moduleScrollTarget > 0.0F) {
                        this.moduleScrollTarget = 0.0F;
                    }

                    if (this.moduleScrollTarget < maxScroll) {
                        this.moduleScrollTarget = maxScroll;
                    }
                } else if (this.isHover(mouseX, mouseY, sepX, this.panelY, this.endX(), this.endY())) {
                    this.valueScrollTarget += wheel > 0 ? 25.0F : -25.0F;
                    if (this.valueScrollTarget > 0.0F) {
                        this.valueScrollTarget = 0.0F;
                    }
                }
            }
        } catch (Exception var11) {
        }
    }

    protected void func_146273_a(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.func_146273_a(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (this.draggingComponent != null && clickedMouseButton == 0) {
            this.draggingComponent.updateDrag(mouseX, this.dragBarX, this.dragBarX2);
        }
    }

    public void func_146286_b(int mouseX, int mouseY, int state) {
        if (state == 0) {
            this.isDragging = false;
            this.isResizingPanel = false;
            this.draggingComponent = null;
            this.draggingSlider = null;
        }
    }

    public void func_73869_a(char typedChar, int keyCode) {
        if (keyCode == 1) {
            mc.func_147108_a(null);
        }
    }

    private boolean isHover(int mx, int my, float x, float y, float x2, float y2) {
        return mx >= x && mx <= x2 && my >= y && my <= y2;
    }

    private static int getIntPropMin(IntProperty v) {
        try {
            Method m = v.getClass().getMethod("getMin");
            return (Integer)m.invoke(v);
        } catch (Exception e1) {
            try {
                Method m = v.getClass().getMethod("getMinimum");
                return (Integer)m.invoke(v);
            } catch (Exception e2) {
                try {
                    Field f = v.getClass().getDeclaredField("min");
                    f.setAccessible(true);
                    return f.getInt(v);
                } catch (Exception e3) {
                    return 0;
                }
            }
        }
    }

    private static int getIntPropMax(IntProperty v) {
        try {
            Method m = v.getClass().getMethod("getMax");
            return (Integer)m.invoke(v);
        } catch (Exception e1) {
            try {
                Method m = v.getClass().getMethod("getMaximum");
                return (Integer)m.invoke(v);
            } catch (Exception e2) {
                try {
                    Field f = v.getClass().getDeclaredField("max");
                    f.setAccessible(true);
                    return f.getInt(v);
                } catch (Exception e3) {
                    return 100;
                }
            }
        }
    }

    private static float getFloatPropMin(FloatProperty v) {
        try {
            Method m = v.getClass().getMethod("getMin");
            return (Float)m.invoke(v);
        } catch (Exception e1) {
            try {
                Method m = v.getClass().getMethod("getMinimum");
                return (Float)m.invoke(v);
            } catch (Exception e2) {
                try {
                    Field f = v.getClass().getDeclaredField("min");
                    f.setAccessible(true);
                    return f.getFloat(v);
                } catch (Exception e3) {
                    return 0.0F;
                }
            }
        }
    }

    private static float getFloatPropMax(FloatProperty v) {
        try {
            Method m = v.getClass().getMethod("getMax");
            return (Float)m.invoke(v);
        } catch (Exception e1) {
            try {
                Method m = v.getClass().getMethod("getMaximum");
                return (Float)m.invoke(v);
            } catch (Exception e2) {
                try {
                    Field f = v.getClass().getDeclaredField("max");
                    f.setAccessible(true);
                    return f.getFloat(v);
                } catch (Exception e3) {
                    return 100.0F;
                }
            }
        }
    }

    public abstract static class AbstractValueComponent {
        public Property<?> value;

        protected AbstractValueComponent(Property<?> value) {
            this.value = value;
        }

        protected Font getFont() {
            return FontRepository.getFont("augustus", 22.0F);
        }

        public void drawValueName(float x, float y) {
            this.getFont().draw(this.value.getName() + ": ", x, y, new Color(210, 210, 210).getRGB(), false);
        }

        public float getBarStartX(float x) {
            return x + this.getFont().getStringWidth(this.value.getName() + ": ") + 10.0F;
        }

        public abstract float drawValue(int var1, int var2, float var3, float var4, float var5, float var6);

        public abstract float mouseClicked(int var1, int var2, float var3, float var4, float var5, float var6, int var7);

        public void updateDrag(int mouseX, float barX, float barX2) {
        }
    }

    public static class AnimFloat {
        public float value;
        public float current;

        public AnimFloat(float initial) {
            this.value = initial;
            this.current = initial;
        }
    }

    public static class BoolValueComponent extends AugustusClickGui.AbstractValueComponent {
        private BooleanProperty v;
        private final AugustusClickGui.AnimFloat switchAnim = new AugustusClickGui.AnimFloat(0.0F);

        public BoolValueComponent(BooleanProperty v) {
            super(v);
            this.v = v;
        }

        @Override
        public float drawValue(int mouseX, int mouseY, float x, float y, float x2, float y2) {
            boolean hover = this.isHover(mouseX, mouseY, x, y - 2.0F, x2, y + this.getFont().getFontHeight() + 2.0F);
            this.switchAnim.value = this.v.getValue() ? 1.0F : 0.0F;
            this.switchAnim.current = AugustusClickGui.easeTo(this.switchAnim.current, this.switchAnim.value, 0.05F);
            float swX = x2 - 38.0F;
            float swY = y + 1.0F;
            float swW = 32.0F;
            float swH = 15.0F;
            RoundedUtils.drawRound(
                swX,
                swY,
                swW,
                swH,
                swH / 2.0F,
                AugustusClickGui.blend(
                    this.v.getValue() ? AugustusClickGui.themeColor : new Color(2765634),
                    new Color(3819094),
                    this.switchAnim.current
                )
            );
            RoundedUtils.drawRoundOutline(
                swX,
                swY,
                swW,
                swH,
                swH / 2.0F,
                0.5F,
                new Color(0, 0, 0, 0),
                this.v.getValue()
                    ? new Color(
                        AugustusClickGui.themeColor.getRed(),
                        AugustusClickGui.themeColor.getGreen(),
                        AugustusClickGui.themeColor.getBlue(),
                        90
                    )
                    : AugustusClickGui.PALETTE_BORDER
            );
            float knobOffset = this.switchAnim.current * (swW - 13.0F);
            RoundedUtils.drawRound(swX + 1.0F + knobOffset, swY + 1.0F, 13.0F, 13.0F, 6.5F, Color.WHITE);
            String text = this.v.getValue() ? "ON" : "OFF";
            Font font = this.getFont();
            font.draw(
                text,
                swX - font.getStringWidth(text) - 6.0F,
                y,
                this.v.getValue() ? AugustusClickGui.themeColor.getRGB() : AugustusClickGui.PALETTE_SECONDARY.getRGB(),
                false
            );
            return this.getFont().getFontHeight() + 10.0F;
        }

        @Override
        public float mouseClicked(int mouseX, int mouseY, float x, float y, float x2, float y2, int mouseButton) {
            if (mouseButton == 0
                && this.isHover(mouseX, mouseY, x, y - 2.0F, x2, y + this.getFont().getFontHeight() + 2.0F)) {
                this.v.setValue(!this.v.getValue());
                return 0.0F;
            } else {
                return this.getFont().getFontHeight() + 10.0F;
            }
        }

        private boolean isHover(int mx, int my, float x, float y, float x2, float y2) {
            return mx >= x && mx <= x2 && my >= y && my <= y2;
        }
    }

    public static class ColorValueComponent extends AugustusClickGui.AbstractValueComponent {
        private boolean expanded = false;
        private String draggingColorChannel = null;
        private final AugustusClickGui.AnimFloat expandAnim = new AugustusClickGui.AnimFloat(0.0F);

        public ColorValueComponent(Property<?> value) {
            super(value);
        }

        private Color getColorVal() {
            try {
                if (this.value.getValue() instanceof Color) {
                    return (Color)this.value.getValue();
                }

                if (this.value.getValue() instanceof Integer) {
                    return new Color((Integer)this.value.getValue());
                }
            } catch (Exception var2) {
            }

            return Color.WHITE;
        }

        private void setColorVal(Color color) {
            try {
                if (this.value.getValue() instanceof Color) {
                    this.value.setValue(color);
                } else if (this.value.getValue() instanceof Integer) {
                    this.value.setValue(color.getRGB());
                }
            } catch (Exception var3) {
            }
        }

        private boolean isHover(int mx, int my, float x, float y, float x2, float y2) {
            return mx >= x && mx <= x2 && my >= y && my <= y2;
        }

        @Override
        public float drawValue(int mouseX, int mouseY, float x, float y, float x2, float y2) {
            Color curCol = this.getColorVal();
            float previewX = x2 - 30.0F;
            RoundedUtils.drawRound(previewX, y, 24.0F, 14.0F, 4.0F, curCol);
            RoundedUtils.drawRoundOutline(
                previewX, y, 24.0F, 14.0F, 4.0F, 1.0F, new Color(0, 0, 0, 0), AugustusClickGui.PALETTE_BORDER
            );
            boolean hoverPreview = this.isHover(mouseX, mouseY, previewX, y, previewX + 24.0F, y + 14.0F);
            if (hoverPreview) {
                RoundedUtils.drawRoundOutline(
                    previewX, y, 24.0F, 14.0F, 4.0F, 1.5F, new Color(0, 0, 0, 0), AugustusClickGui.themeColor
                );
            }

            if (!this.expanded) {
                this.expandAnim.current = AugustusClickGui.easeTo(this.expandAnim.current, 0.0F, 0.05F);
                return this.getFont().getFontHeight() + 10.0F;
            }

            this.expandAnim.current = AugustusClickGui.easeTo(this.expandAnim.current, 1.0F, 0.05F);
            float subY = y + this.getFont().getFontHeight() + 10.0F;
            float barX = x + 20.0F;
            float barW = x2 - x - 60.0F;
            subY += this.drawSliderRow("R", curCol.getRed(), barX, subY, barW, new Color(255, 80, 80));
            subY += this.drawSliderRow("G", curCol.getGreen(), barX, subY, barW, new Color(80, 255, 80));
            subY += this.drawSliderRow("B", curCol.getBlue(), barX, subY, barW, new Color(80, 180, 255));
            if (this.draggingColorChannel != null && Mouse.isButtonDown(0)) {
                float pct = Math.max(0.0F, Math.min(1.0F, (mouseX - (barX + 20.0F)) / (barW - 20.0F)));
                int val = Math.round(pct * 255.0F);
                if ("R".equals(this.draggingColorChannel)) {
                    this.setColorVal(new Color(val, curCol.getGreen(), curCol.getBlue()));
                }

                if ("G".equals(this.draggingColorChannel)) {
                    this.setColorVal(new Color(curCol.getRed(), val, curCol.getBlue()));
                }

                if ("B".equals(this.draggingColorChannel)) {
                    this.setColorVal(new Color(curCol.getRed(), curCol.getGreen(), val));
                }
            }

            return subY - y;
        }

        private float drawSliderRow(String channel, int value, float x, float y, float width, Color col) {
            Font font = this.getFont();
            font.draw(channel, x, y, col.getRGB(), false);
            float barX = x + 20.0F;
            float barW = width - 20.0F;
            RoundedUtils.drawRound(barX, y + 4.0F, barW, 6.0F, 3.0F, new Color(724757, true).getRGB());
            float fillW = barW * (value / 255.0F);
            if (fillW > 1.0F) {
                RoundedUtils.drawGradientHorizontal(
                    barX, y + 4.0F, fillW, 6.0F, 3.0F, new Color(col.getRed(), col.getGreen(), col.getBlue(), 60), col
                );
            }

            float knobX = barX + fillW;
            RoundedUtils.drawRound(knobX - 2.5F, y + 2.5F, 6.0F, 9.0F, 4.5F, Color.WHITE);
            font.draw(String.valueOf(value), barX + barW + 10.0F, y, new Color(210, 214, 222).getRGB(), false);
            return 20.0F;
        }

        @Override
        public float mouseClicked(int mouseX, int mouseY, float x, float y, float x2, float y2, int mouseButton) {
            float previewX = x2 - 30.0F;
            if (mouseButton == 0
                && mouseX >= previewX
                && mouseX <= previewX + 24.0F
                && mouseY >= y
                && mouseY <= y + 14.0F) {
                this.expanded = !this.expanded;
                return 0.0F;
            }

            if (this.expanded && mouseButton == 0) {
                float subY = y + this.getFont().getFontHeight() + 10.0F;
                float barX = x + 40.0F;
                float barW = x2 - x - 80.0F;
                if (mouseY >= subY && mouseY <= subY + 18.0F && mouseX >= barX && mouseX <= barX + barW) {
                    this.draggingColorChannel = "R";
                    return -1.0F;
                }

                if (mouseY >= subY + 20.0F && mouseY <= subY + 38.0F && mouseX >= barX && mouseX <= barX + barW) {
                    this.draggingColorChannel = "G";
                    return -1.0F;
                }

                if (mouseY >= subY + 40.0F && mouseY <= subY + 58.0F && mouseX >= barX && mouseX <= barX + barW) {
                    this.draggingColorChannel = "B";
                    return -1.0F;
                }
            }

            return this.getFont().getFontHeight() + 10.0F;
        }
    }

    public static class EntityTargets {
        public static boolean player = true;
        public static boolean mob = true;
        public static boolean animal = true;
        public static boolean invisible = false;
        public static boolean dead = false;
    }

    public static class FloatValueComponent extends AugustusClickGui.AbstractValueComponent {
        private FloatProperty v;
        private boolean isRange;
        private boolean draggingMin = false;
        private final AugustusClickGui.AnimFloat fillAnim = new AugustusClickGui.AnimFloat(0.0F);

        public FloatValueComponent(FloatProperty v) {
            super(v);
            this.v = v;
            this.isRange = checkIsRange(v);
        }

        private static boolean checkIsRange(FloatProperty prop) {
            String className = prop.getClass().getName().toLowerCase();
            if (!className.contains("range") && !className.contains("multi")) {
                try {
                    prop.getClass().getMethod("getMinValue");
                    return true;
                } catch (Exception e1) {
                    try {
                        prop.getClass().getMethod("getMinVal");
                        return true;
                    } catch (Exception e2) {
                        try {
                            prop.getClass().getMethod("isRange");
                            return (Boolean)prop.getClass().getMethod("isRange").invoke(prop);
                        } catch (Exception e3) {
                            try {
                                prop.getClass().getDeclaredField("minValue");
                                return true;
                            } catch (Exception e4) {
                                return false;
                            }
                        }
                    }
                }
            } else {
                return true;
            }
        }

        private float getCurMin() {
            if (!this.isRange) {
                return AugustusClickGui.getFloatPropMin(this.v);
            }

            try {
                Method m = this.v.getClass().getMethod("getMinValue");
                return (Float)m.invoke(this.v);
            } catch (Exception e1) {
                try {
                    Method m = this.v.getClass().getMethod("getMinVal");
                    return (Float)m.invoke(this.v);
                } catch (Exception e2) {
                    try {
                        Field f = this.v.getClass().getDeclaredField("minValue");
                        f.setAccessible(true);
                        return f.getFloat(this.v);
                    } catch (Exception e3) {
                        return AugustusClickGui.getFloatPropMin(this.v);
                    }
                }
            }
        }

        private float getCurMax() {
            if (!this.isRange) {
                return this.v.getValue();
            }

            try {
                Method m = this.v.getClass().getMethod("getMaxValue");
                return (Float)m.invoke(this.v);
            } catch (Exception e1) {
                try {
                    Method m = this.v.getClass().getMethod("getMaxVal");
                    return (Float)m.invoke(this.v);
                } catch (Exception e2) {
                    try {
                        Field f = this.v.getClass().getDeclaredField("maxValue");
                        f.setAccessible(true);
                        return f.getFloat(this.v);
                    } catch (Exception e3) {
                        return this.v.getValue();
                    }
                }
            }
        }

        private void setCurMin(float val) {
            if (!this.isRange) {
                this.v.setValue(val);
            } else {
                try {
                    Method m = this.v.getClass().getMethod("setMinValue", float.class);
                    m.invoke(this.v, val);
                } catch (Exception e1) {
                    try {
                        Method m = this.v.getClass().getMethod("setMinVal", float.class);
                        m.invoke(this.v, val);
                    } catch (Exception e2) {
                        try {
                            Field f = this.v.getClass().getDeclaredField("minValue");
                            f.setAccessible(true);
                            f.setFloat(this.v, val);
                        } catch (Exception var5) {
                            this.v.setValue(val);
                        }
                    }
                }
            }
        }

        private void setCurMax(float val) {
            if (!this.isRange) {
                this.v.setValue(val);
            } else {
                try {
                    Method m = this.v.getClass().getMethod("setMaxValue", float.class);
                    m.invoke(this.v, val);
                } catch (Exception e1) {
                    try {
                        Method m = this.v.getClass().getMethod("setMaxVal", float.class);
                        m.invoke(this.v, val);
                    } catch (Exception e2) {
                        try {
                            Field f = this.v.getClass().getDeclaredField("maxValue");
                            f.setAccessible(true);
                            f.setFloat(this.v, val);
                        } catch (Exception var5) {
                            this.v.setValue(val);
                        }
                    }
                }
            }
        }

        private float barHeight() {
            return this.getFont().getFontHeight() + 4.0F;
        }

        @Override
        public float drawValue(int mouseX, int mouseY, float x, float y, float x2, float y2) {
            float barX = this.getBarStartX(x);
            float barY = y - 2.0F;
            float barH = this.barHeight();
            float minBound = AugustusClickGui.getFloatPropMin(this.v);
            float maxBound = AugustusClickGui.getFloatPropMax(this.v);
            float rangeBound = maxBound - minBound;
            if (this.isRange) {
                float curMin = this.getCurMin();
                float curMax = this.getCurMax();
                float pMin = rangeBound <= 0.0F ? 0.0F : (curMin - minBound) / rangeBound;
                float pMax = rangeBound <= 0.0F ? 0.0F : (curMax - minBound) / rangeBound;
                pMin = Math.max(0.0F, Math.min(1.0F, pMin));
                pMax = Math.max(0.0F, Math.min(1.0F, pMax));
                if (pMin > pMax) {
                    pMin = pMax;
                }

                RoundedUtils.drawRound(barX, barY, x2 - barX, barH, barH / 2.0F, new Color(724757, true).getRGB());
                RoundedUtils.drawRoundOutline(
                    barX,
                    barY,
                    x2 - barX,
                    barH,
                    barH / 2.0F,
                    0.5F,
                    new Color(0, 0, 0, 0),
                    AugustusClickGui.PALETTE_BORDER
                );
                float fillStart = barX + (x2 - barX) * pMin;
                float fillEnd = barX + (x2 - barX) * pMax;
                if (fillEnd > fillStart) {
                    RoundedUtils.drawGradientHorizontal(
                        fillStart,
                        barY,
                        fillEnd - fillStart,
                        barH,
                        barH / 2.0F,
                        new Color(
                            AugustusClickGui.themeColor.getRed(),
                            AugustusClickGui.themeColor.getGreen(),
                            AugustusClickGui.themeColor.getBlue(),
                            50
                        ),
                        AugustusClickGui.themeColor
                    );
                }

                float knobRadius = (barH + 4.0F) / 2.0F;
                float centerY = barY + barH / 2.0F;
                RoundedUtils.drawRound(
                    fillStart - knobRadius,
                    centerY - knobRadius,
                    knobRadius * 2.0F,
                    knobRadius * 2.0F,
                    knobRadius,
                    Color.WHITE
                );
                RoundedUtils.drawRound(
                    fillEnd - knobRadius,
                    centerY - knobRadius,
                    knobRadius * 2.0F,
                    knobRadius * 2.0F,
                    knobRadius,
                    Color.WHITE
                );
                String display = String.format("%.1f - %.1f", curMin, curMax);
                float textWidth = this.getFont().getStringWidth(display);
                float textX = barX + (x2 - barX - textWidth) / 2.0F;
                this.getFont().draw(display, textX, y, new Color(235, 239, 245).getRGB(), false);
            } else {
                float curVal = this.v.getValue();
                float percent = rangeBound <= 0.0F ? 0.0F : (curVal - minBound) / rangeBound;
                percent = Math.max(0.0F, Math.min(1.0F, percent));
                this.fillAnim.value = percent;
                this.fillAnim.current = AugustusClickGui.easeTo(this.fillAnim.current, this.fillAnim.value, 0.05F);
                RoundedUtils.drawRound(barX, barY, x2 - barX, barH, barH / 2.0F, new Color(724757, true).getRGB());
                RoundedUtils.drawRoundOutline(
                    barX,
                    barY,
                    x2 - barX,
                    barH,
                    barH / 2.0F,
                    0.5F,
                    new Color(0, 0, 0, 0),
                    AugustusClickGui.PALETTE_BORDER
                );
                float fillEnd = barX + (x2 - barX) * this.fillAnim.current;
                if (fillEnd > barX + 1.0F) {
                    RoundedUtils.drawGradientHorizontal(
                        barX,
                        barY,
                        fillEnd - barX,
                        barH,
                        barH / 2.0F,
                        new Color(
                            AugustusClickGui.themeColor.getRed(),
                            AugustusClickGui.themeColor.getGreen(),
                            AugustusClickGui.themeColor.getBlue(),
                            50
                        ),
                        AugustusClickGui.themeColor
                    );
                }

                float knobRadius = (barH + 4.0F) / 2.0F;
                float centerY = barY + barH / 2.0F;
                float knobX = fillEnd > barX + 1.0F ? fillEnd : barX;
                RoundedUtils.drawRound(
                    knobX - knobRadius,
                    centerY - knobRadius,
                    knobRadius * 2.0F,
                    knobRadius * 2.0F,
                    knobRadius,
                    Color.WHITE
                );
                String display = String.format("%.1f", curVal);
                float textWidth = this.getFont().getStringWidth(display);
                float textX = barX + (x2 - barX - textWidth) / 2.0F;
                this.getFont().draw(display, textX, y, new Color(235, 239, 245).getRGB(), false);
            }

            return barH + 8.0F;
        }

        @Override
        public float mouseClicked(int mouseX, int mouseY, float x, float y, float x2, float y2, int mouseButton) {
            float barX = this.getBarStartX(x);
            float barY = y - 2.0F;
            float barH = this.barHeight();
            if (mouseButton == 0 && this.isHover(mouseX, mouseY, barX, barY, x2, barY + barH)) {
                if (this.isRange) {
                    float span = x2 - barX;
                    float percent = span <= 0.0F ? 0.0F : (mouseX - barX) / span;
                    percent = Math.max(0.0F, Math.min(1.0F, percent));
                    float minBound = AugustusClickGui.getFloatPropMin(this.v);
                    float maxBound = AugustusClickGui.getFloatPropMax(this.v);
                    float clickedVal = minBound + percent * (maxBound - minBound);
                    clickedVal = Math.round(clickedVal * 10.0F) / 10.0F;
                    float curMin = this.getCurMin();
                    float curMax = this.getCurMax();
                    if (Math.abs(clickedVal - curMin) <= Math.abs(clickedVal - curMax)) {
                        this.draggingMin = true;
                        this.setCurMin(Math.min(clickedVal, curMax));
                    } else {
                        this.draggingMin = false;
                        this.setCurMax(Math.max(clickedVal, curMin));
                    }
                } else {
                    this.updateDrag(mouseX, barX, x2);
                }

                return -1.0F;
            } else {
                return barH + 8.0F;
            }
        }

        @Override
        public void updateDrag(int mouseX, float barX, float barX2) {
            float span = barX2 - barX;
            float percent = span <= 0.0F ? 0.0F : (mouseX - barX) / span;
            percent = Math.max(0.0F, Math.min(1.0F, percent));
            float minBound = AugustusClickGui.getFloatPropMin(this.v);
            float maxBound = AugustusClickGui.getFloatPropMax(this.v);
            float newValue = minBound + percent * (maxBound - minBound);
            newValue = Math.round(newValue * 10.0F) / 10.0F;
            if (this.isRange) {
                if (this.draggingMin) {
                    float curMax = this.getCurMax();
                    if (newValue > curMax) {
                        newValue = curMax;
                    }

                    this.setCurMin(newValue);
                } else {
                    float curMin = this.getCurMin();
                    if (newValue < curMin) {
                        newValue = curMin;
                    }

                    this.setCurMax(newValue);
                }
            } else {
                this.v.setValue(newValue);
            }
        }

        private boolean isHover(int mx, int my, float x, float y, float x2, float y2) {
            return mx >= x && mx <= x2 && my >= y && my <= y2;
        }
    }

    public static class IntValueComponent extends AugustusClickGui.AbstractValueComponent {
        private IntProperty v;
        private boolean isRange;
        private boolean draggingMin = false;
        private final AugustusClickGui.AnimFloat fillAnim = new AugustusClickGui.AnimFloat(0.0F);

        public IntValueComponent(IntProperty v) {
            super(v);
            this.v = v;
            this.isRange = checkIsRange(v);
        }

        private static boolean checkIsRange(IntProperty prop) {
            String className = prop.getClass().getName().toLowerCase();
            if (!className.contains("range") && !className.contains("multi")) {
                try {
                    prop.getClass().getMethod("getMinValue");
                    return true;
                } catch (Exception e1) {
                    try {
                        prop.getClass().getMethod("getMinVal");
                        return true;
                    } catch (Exception e2) {
                        try {
                            prop.getClass().getMethod("isRange");
                            return (Boolean)prop.getClass().getMethod("isRange").invoke(prop);
                        } catch (Exception e3) {
                            try {
                                prop.getClass().getDeclaredField("minValue");
                                return true;
                            } catch (Exception e4) {
                                return false;
                            }
                        }
                    }
                }
            } else {
                return true;
            }
        }

        private int getCurMin() {
            if (!this.isRange) {
                return AugustusClickGui.getIntPropMin(this.v);
            }

            try {
                Method m = this.v.getClass().getMethod("getMinValue");
                return (Integer)m.invoke(this.v);
            } catch (Exception e1) {
                try {
                    Method m = this.v.getClass().getMethod("getMinVal");
                    return (Integer)m.invoke(this.v);
                } catch (Exception e2) {
                    try {
                        Field f = this.v.getClass().getDeclaredField("minValue");
                        f.setAccessible(true);
                        return f.getInt(this.v);
                    } catch (Exception e3) {
                        return AugustusClickGui.getIntPropMin(this.v);
                    }
                }
            }
        }

        private int getCurMax() {
            if (!this.isRange) {
                return this.v.getValue();
            }

            try {
                Method m = this.v.getClass().getMethod("getMaxValue");
                return (Integer)m.invoke(this.v);
            } catch (Exception e1) {
                try {
                    Method m = this.v.getClass().getMethod("getMaxVal");
                    return (Integer)m.invoke(this.v);
                } catch (Exception e2) {
                    try {
                        Field f = this.v.getClass().getDeclaredField("maxValue");
                        f.setAccessible(true);
                        return f.getInt(this.v);
                    } catch (Exception e3) {
                        return this.v.getValue();
                    }
                }
            }
        }

        private void setCurMin(int val) {
            if (!this.isRange) {
                this.v.setValue(val);
            } else {
                try {
                    Method m = this.v.getClass().getMethod("setMinValue", int.class);
                    m.invoke(this.v, val);
                } catch (Exception e1) {
                    try {
                        Method m = this.v.getClass().getMethod("setMinVal", int.class);
                        m.invoke(this.v, val);
                    } catch (Exception e2) {
                        try {
                            Field f = this.v.getClass().getDeclaredField("minValue");
                            f.setAccessible(true);
                            f.setInt(this.v, val);
                        } catch (Exception var5) {
                            this.v.setValue(val);
                        }
                    }
                }
            }
        }

        private void setCurMax(int val) {
            if (!this.isRange) {
                this.v.setValue(val);
            } else {
                try {
                    Method m = this.v.getClass().getMethod("setMaxValue", int.class);
                    m.invoke(this.v, val);
                } catch (Exception e1) {
                    try {
                        Method m = this.v.getClass().getMethod("setMaxVal", int.class);
                        m.invoke(this.v, val);
                    } catch (Exception e2) {
                        try {
                            Field f = this.v.getClass().getDeclaredField("maxValue");
                            f.setAccessible(true);
                            f.setInt(this.v, val);
                        } catch (Exception var5) {
                            this.v.setValue(val);
                        }
                    }
                }
            }
        }

        private float barHeight() {
            return this.getFont().getFontHeight() + 4.0F;
        }

        @Override
        public float drawValue(int mouseX, int mouseY, float x, float y, float x2, float y2) {
            float barX = this.getBarStartX(x);
            float barY = y - 2.0F;
            float barH = this.barHeight();
            int minBound = AugustusClickGui.getIntPropMin(this.v);
            int maxBound = AugustusClickGui.getIntPropMax(this.v);
            int rangeBound = maxBound - minBound;
            if (this.isRange) {
                int curMin = this.getCurMin();
                int curMax = this.getCurMax();
                float pMin = rangeBound <= 0 ? 0.0F : (float)(curMin - minBound) / rangeBound;
                float pMax = rangeBound <= 0 ? 0.0F : (float)(curMax - minBound) / rangeBound;
                pMin = Math.max(0.0F, Math.min(1.0F, pMin));
                pMax = Math.max(0.0F, Math.min(1.0F, pMax));
                if (pMin > pMax) {
                    pMin = pMax;
                }

                RoundedUtils.drawRound(barX, barY, x2 - barX, barH, barH / 2.0F, new Color(724757, true).getRGB());
                RoundedUtils.drawRoundOutline(
                    barX,
                    barY,
                    x2 - barX,
                    barH,
                    barH / 2.0F,
                    0.5F,
                    new Color(0, 0, 0, 0),
                    AugustusClickGui.PALETTE_BORDER
                );
                float fillStart = barX + (x2 - barX) * pMin;
                float fillEnd = barX + (x2 - barX) * pMax;
                if (fillEnd > fillStart) {
                    RoundedUtils.drawGradientHorizontal(
                        fillStart,
                        barY,
                        fillEnd - fillStart,
                        barH,
                        barH / 2.0F,
                        new Color(
                            AugustusClickGui.themeColor.getRed(),
                            AugustusClickGui.themeColor.getGreen(),
                            AugustusClickGui.themeColor.getBlue(),
                            50
                        ),
                        AugustusClickGui.themeColor
                    );
                }

                float knobRadius = (barH + 4.0F) / 2.0F;
                float centerY = barY + barH / 2.0F;
                RoundedUtils.drawRound(
                    fillStart - knobRadius,
                    centerY - knobRadius,
                    knobRadius * 2.0F,
                    knobRadius * 2.0F,
                    knobRadius,
                    Color.WHITE
                );
                RoundedUtils.drawRound(
                    fillEnd - knobRadius,
                    centerY - knobRadius,
                    knobRadius * 2.0F,
                    knobRadius * 2.0F,
                    knobRadius,
                    Color.WHITE
                );
                String display = curMin + " - " + curMax;
                float textWidth = this.getFont().getStringWidth(display);
                float textX = barX + (x2 - barX - textWidth) / 2.0F;
                this.getFont().draw(display, textX, y, new Color(235, 239, 245).getRGB(), false);
            } else {
                int curVal = this.v.getValue();
                float percent = rangeBound <= 0 ? 0.0F : (float)(curVal - minBound) / rangeBound;
                percent = Math.max(0.0F, Math.min(1.0F, percent));
                this.fillAnim.value = percent;
                this.fillAnim.current = AugustusClickGui.easeTo(this.fillAnim.current, this.fillAnim.value, 0.05F);
                RoundedUtils.drawRound(barX, barY, x2 - barX, barH, barH / 2.0F, new Color(724757, true).getRGB());
                RoundedUtils.drawRoundOutline(
                    barX,
                    barY,
                    x2 - barX,
                    barH,
                    barH / 2.0F,
                    0.5F,
                    new Color(0, 0, 0, 0),
                    AugustusClickGui.PALETTE_BORDER
                );
                float fillEnd = barX + (x2 - barX) * this.fillAnim.current;
                if (fillEnd > barX + 1.0F) {
                    RoundedUtils.drawGradientHorizontal(
                        barX,
                        barY,
                        fillEnd - barX,
                        barH,
                        barH / 2.0F,
                        new Color(
                            AugustusClickGui.themeColor.getRed(),
                            AugustusClickGui.themeColor.getGreen(),
                            AugustusClickGui.themeColor.getBlue(),
                            50
                        ),
                        AugustusClickGui.themeColor
                    );
                }

                float knobRadius = (barH + 4.0F) / 2.0F;
                float centerY = barY + barH / 2.0F;
                float knobX = fillEnd > barX + 1.0F ? fillEnd : barX;
                RoundedUtils.drawRound(
                    knobX - knobRadius,
                    centerY - knobRadius,
                    knobRadius * 2.0F,
                    knobRadius * 2.0F,
                    knobRadius,
                    Color.WHITE
                );
                String display = String.valueOf(curVal);
                float textWidth = this.getFont().getStringWidth(display);
                float textX = barX + (x2 - barX - textWidth) / 2.0F;
                this.getFont().draw(display, textX, y, new Color(235, 239, 245).getRGB(), false);
            }

            return barH + 8.0F;
        }

        @Override
        public float mouseClicked(int mouseX, int mouseY, float x, float y, float x2, float y2, int mouseButton) {
            float barX = this.getBarStartX(x);
            float barY = y - 2.0F;
            float barH = this.barHeight();
            if (mouseButton == 0 && this.isHover(mouseX, mouseY, barX, barY, x2, barY + barH)) {
                if (this.isRange) {
                    float span = x2 - barX;
                    float percent = span <= 0.0F ? 0.0F : (mouseX - barX) / span;
                    percent = Math.max(0.0F, Math.min(1.0F, percent));
                    int minBound = AugustusClickGui.getIntPropMin(this.v);
                    int maxBound = AugustusClickGui.getIntPropMax(this.v);
                    int clickedVal = Math.round(minBound + percent * (maxBound - minBound));
                    int curMin = this.getCurMin();
                    int curMax = this.getCurMax();
                    if (Math.abs(clickedVal - curMin) <= Math.abs(clickedVal - curMax)) {
                        this.draggingMin = true;
                        this.setCurMin(Math.min(clickedVal, curMax));
                    } else {
                        this.draggingMin = false;
                        this.setCurMax(Math.max(clickedVal, curMin));
                    }
                } else {
                    this.updateDrag(mouseX, barX, x2);
                }

                return -1.0F;
            } else {
                return barH + 8.0F;
            }
        }

        @Override
        public void updateDrag(int mouseX, float barX, float barX2) {
            float span = barX2 - barX;
            float percent = span <= 0.0F ? 0.0F : (mouseX - barX) / span;
            percent = Math.max(0.0F, Math.min(1.0F, percent));
            int minBound = AugustusClickGui.getIntPropMin(this.v);
            int maxBound = AugustusClickGui.getIntPropMax(this.v);
            int newValue = Math.round(minBound + percent * (maxBound - minBound));
            if (this.isRange) {
                if (this.draggingMin) {
                    int curMax = this.getCurMax();
                    if (newValue > curMax) {
                        newValue = curMax;
                    }

                    this.setCurMin(newValue);
                } else {
                    int curMin = this.getCurMin();
                    if (newValue < curMin) {
                        newValue = curMin;
                    }

                    this.setCurMax(newValue);
                }
            } else {
                this.v.setValue(newValue);
            }
        }

        private boolean isHover(int mx, int my, float x, float y, float x2, float y2) {
            return mx >= x && mx <= x2 && my >= y && my <= y2;
        }
    }

    public static class ListValueComponent extends AugustusClickGui.AbstractValueComponent {
        private ModeProperty v;
        private final AugustusClickGui.AnimFloat hoverAnim = new AugustusClickGui.AnimFloat(0.0F);

        public ListValueComponent(ModeProperty v) {
            super(v);
            this.v = v;
        }

        @Override
        public float drawValue(int mouseX, int mouseY, float x, float y, float x2, float y2) {
            String[] modes = this.v.getModes();
            String selected = modes != null && this.v.getValue() >= 0 && this.v.getValue() < modes.length
                ? modes[this.v.getValue()]
                : String.valueOf(this.v.getValue());
            boolean hover = this.isHover(mouseX, mouseY, x, y - 2.0F, x2, y + this.getFont().getFontHeight() + 2.0F);
            this.hoverAnim.value = hover ? 1.0F : 0.0F;
            this.hoverAnim.current = AugustusClickGui.easeTo(this.hoverAnim.current, this.hoverAnim.value, 0.05F);
            float pillW = this.getFont().getStringWidth(selected) + 26.0F;
            float pillX = x2 - pillW;
            RoundedUtils.drawRound(
                pillX,
                y - 1.0F,
                pillW,
                this.getFont().getFontHeight() + 2.0F,
                6.0F,
                AugustusClickGui.blend(
                    AugustusClickGui.PALETTE_CARD_HOVER, AugustusClickGui.themeColor, this.hoverAnim.current * 0.35F
                )
            );
            RoundedUtils.drawRoundOutline(
                pillX,
                y - 1.0F,
                pillW,
                this.getFont().getFontHeight() + 2.0F,
                6.0F,
                1.0F,
                new Color(0, 0, 0, 0),
                AugustusClickGui.blend(
                    AugustusClickGui.PALETTE_BORDER, AugustusClickGui.themeColor, this.hoverAnim.current * 0.6F
                )
            );
            this.getFont().draw(selected, pillX + 8.0F, y, AugustusClickGui.themeColor.getRGB(), false);
            Font fontChev = FontRepository.getFont("augustus", 18.0F);
            fontChev.draw("v", pillX + pillW - 14.0F, y + 2.0F, AugustusClickGui.PALETTE_SECONDARY.getRGB(), false);
            return this.getFont().getFontHeight() + 10.0F;
        }

        @Override
        public float mouseClicked(int mouseX, int mouseY, float x, float y, float x2, float y2, int mouseButton) {
            if (mouseButton == 0
                && this.isHover(mouseX, mouseY, x, y - 2.0F, x2, y + this.getFont().getFontHeight() + 2.0F)) {
                String[] modes = this.v.getModes();
                int maxModes = modes != null && modes.length > 0 ? modes.length : 10;
                int nextIndex = this.v.getValue() + 1;
                if (nextIndex >= maxModes) {
                    nextIndex = 0;
                }

                this.v.setValue(nextIndex);
                return 0.0F;
            } else {
                return this.getFont().getFontHeight() + 10.0F;
            }
        }

        private boolean isHover(int mx, int my, float x, float y, float x2, float y2) {
            return mx >= x && mx <= x2 && my >= y && my <= y2;
        }
    }

    public static class TargetEntry {
        public String label;
        private AugustusClickGui.TargetEntry.StateGetter stateGetter;
        private AugustusClickGui.TargetEntry.Toggle toggle;

        public TargetEntry(
            String label,
            AugustusClickGui.TargetEntry.StateGetter stateGetter,
            AugustusClickGui.TargetEntry.Toggle toggle
        ) {
            this.label = label;
            this.stateGetter = stateGetter;
            this.toggle = toggle;
        }

        public interface StateGetter {
            boolean get();
        }

        public interface Toggle {
            void toggle();
        }
    }
}
