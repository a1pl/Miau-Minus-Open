package miau.ui.clickgui.miauminus;

import java.awt.Color;
import java.awt.Desktop;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
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
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import miau.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MiauMinusClickGui extends GuiScreen {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public static Color themeColor = new Color(0, 190, 245, 200);
    public static final Color GLASS_BG = new Color(10, 14, 20, 180);
    public static final Color BORDER_COLOR = new Color(255, 255, 255, 25);
    public static final Color TEXT_SELECTED = new Color(255, 255, 255);
    public static final Color TEXT_UNSELECTED = new Color(140, 155, 170);
    public static String colorMode = "Custom";
    public static int customR = 0;
    public static int customG = 190;
    public static int customB = 245;
    private boolean showSettingsPopup = false;
    private String draggingSlider = null;
    private float panelX = 0.0F;
    private float panelY = 0.0F;
    private float panelWidth = 580.0F;
    private float panelHeight = 360.0F;
    private float categoryScroll = 0.0F;
    private float moduleScroll = 0.0F;
    private float valueScroll = 0.0F;
    private boolean isDragging = false;
    private float dragOffX = 0.0F;
    private float dragOffY = 0.0F;
    private String currentCategory = "Render";
    private String currentModule = "Ambience";
    private float modulePanelWidth = 150.0F;
    private MiauMinusClickGui.AbstractValueComponent draggingComponent = null;
    private float dragBarX = 0.0F;
    private float dragBarX2 = 0.0F;
    private final Map<Module, List<MiauMinusClickGui.AbstractValueComponent>> componentCache = new HashMap<>();
    private final List<MiauMinusClickGui.TargetEntry> targetEntries = new ArrayList<>();
    private final List<MiauMinusClickGui.AmbiencePresetButton> ambiencePresetButtons = new ArrayList<>();

    public MiauMinusClickGui() {
        this.targetEntries
            .add(
                new MiauMinusClickGui.TargetEntry(
                    "Players",
                    () -> MiauMinusClickGui.EntityTargets.player,
                    () -> MiauMinusClickGui.EntityTargets.player = !MiauMinusClickGui.EntityTargets.player
                )
            );
        this.targetEntries
            .add(
                new MiauMinusClickGui.TargetEntry(
                    "Mobs",
                    () -> MiauMinusClickGui.EntityTargets.mob,
                    () -> MiauMinusClickGui.EntityTargets.mob = !MiauMinusClickGui.EntityTargets.mob
                )
            );
        this.targetEntries
            .add(
                new MiauMinusClickGui.TargetEntry(
                    "Animals",
                    () -> MiauMinusClickGui.EntityTargets.animal,
                    () -> MiauMinusClickGui.EntityTargets.animal = !MiauMinusClickGui.EntityTargets.animal
                )
            );
        this.targetEntries
            .add(
                new MiauMinusClickGui.TargetEntry(
                    "Invisible",
                    () -> MiauMinusClickGui.EntityTargets.invisible,
                    () -> MiauMinusClickGui.EntityTargets.invisible = !MiauMinusClickGui.EntityTargets.invisible
                )
            );
        this.targetEntries
            .add(
                new MiauMinusClickGui.TargetEntry(
                    "Dead",
                    () -> MiauMinusClickGui.EntityTargets.dead,
                    () -> MiauMinusClickGui.EntityTargets.dead = !MiauMinusClickGui.EntityTargets.dead
                )
            );
    }

    public void func_73866_w_() {
        this.isDragging = false;
        ScaledResolution sr = new ScaledResolution(mc);
        this.panelWidth = Math.min(610.0F, sr.func_78326_a() * 0.85F);
        this.panelHeight = Math.min(380.0F, sr.func_78328_b() * 0.85F);
        this.panelX = (sr.func_78326_a() - this.panelWidth) / 2.0F;
        this.panelY = (sr.func_78328_b() - this.panelHeight) / 2.0F;
    }

    public void func_146281_b() {
        this.isDragging = false;
        this.showSettingsPopup = false;
    }

    public boolean func_73868_f() {
        return false;
    }

    private float getHeaderHeight() {
        return 42.0F;
    }

    private float endX() {
        return this.panelX + this.panelWidth;
    }

    private float endY() {
        return this.panelY + this.panelHeight;
    }

    private void setCategory(String category) {
        this.currentCategory = category;
        this.moduleScroll = 0.0F;
        this.valueScroll = 0.0F;
        this.currentModule = null;
    }

    private void setModule(String moduleName) {
        this.currentModule = moduleName;
        this.valueScroll = 0.0F;
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

    private List<MiauMinusClickGui.AbstractValueComponent> getComponents(Module module) {
        List<MiauMinusClickGui.AbstractValueComponent> components = this.componentCache.get(module);
        if (components == null) {
            components = new ArrayList<>();
            if (module.getValues() != null) {
                for (Property<?> prop : module.getValues()) {
                    if (prop instanceof BooleanProperty) {
                        components.add(new MiauMinusClickGui.BoolValueComponent((BooleanProperty)prop));
                    } else if (prop instanceof IntProperty) {
                        components.add(new MiauMinusClickGui.IntValueComponent((IntProperty)prop));
                    } else if (prop instanceof FloatProperty) {
                        components.add(new MiauMinusClickGui.FloatValueComponent((FloatProperty)prop));
                    } else if (prop instanceof ModeProperty) {
                        components.add(new MiauMinusClickGui.ListValueComponent((ModeProperty)prop));
                    } else if (prop.getClass().getName().toLowerCase().contains("color")) {
                        components.add(new MiauMinusClickGui.ColorValueComponent(prop));
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
        try {
            HUD hud = (HUD)Miau.moduleManager.modules.get(HUD.class);
            if (hud != null) {
                Color hudCol = hud.getColor(System.currentTimeMillis());
                themeColor = new Color(hudCol.getRed(), hudCol.getGreen(), hudCol.getBlue(), 200);
                return;
            }
        } catch (Exception var3) {
        }

        if ("Custom".equalsIgnoreCase(colorMode)) {
            themeColor = new Color(customR, customG, customB, 200);
        }
    }

    private void drawCustomImage(String path, float x, float y, float width, float height) {
        try {
            ResourceLocation loc = new ResourceLocation(path);
            mc.func_110434_K().func_110577_a(loc);
            GL11.glPushMatrix();
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            Gui.func_146110_a((int)x, (int)y, 0.0F, 0.0F, (int)width, (int)height, width, height);
            GL11.glDisable(3042);
            GL11.glPopMatrix();
        } catch (Exception var7) {
        }
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        this.updateThemeColor();
        if (this.isDragging) {
            this.panelX = mouseX - this.dragOffX;
            this.panelY = mouseY - this.dragOffY;
        }

        float ex = this.endX();
        float ey = this.endY();
        float headerH = this.getHeaderHeight();
        RenderUtil.drawRect(0.0F, 0.0F, this.field_146294_l, this.field_146295_m, new Color(0, 0, 0, 80).getRGB());
        float glowAlpha = 0.3F + 0.2F * (float)Math.sin(System.currentTimeMillis() / 400.0);
        Color glowWhite = new Color(255, 255, 255, (int)(glowAlpha * 255.0F));
        RoundedUtils.drawRoundOutline(
            this.panelX - 1.5F,
            this.panelY - 1.5F,
            this.panelWidth + 3.0F,
            this.panelHeight + 3.0F,
            9.5F,
            1.5F,
            glowWhite,
            themeColor
        );
        RoundedUtils.drawRoundOutline(
            this.panelX, this.panelY, this.panelWidth, this.panelHeight, 8.0F, 1.2F, themeColor, themeColor
        );
        RoundedUtils.drawRound(this.panelX, this.panelY, this.panelWidth, headerH, 8.0F, GLASS_BG);
        this.drawHeaderContent(ex, mouseX, mouseY);
        float contentTopY = this.panelY + headerH + 8.0F;
        float contentHeight = ey - contentTopY;
        float profileHeight = 54.0F;
        float moduleListHeight = contentHeight - profileHeight - 8.0F;
        RoundedUtils.drawRound(this.panelX, contentTopY, this.modulePanelWidth, moduleListHeight, 8.0F, GLASS_BG);
        RoundedUtils.drawRoundOutline(
            this.panelX, contentTopY, this.modulePanelWidth, moduleListHeight, 8.0F, 0.8F, BORDER_COLOR, BORDER_COLOR
        );
        float profileY = contentTopY + moduleListHeight + 8.0F;
        RoundedUtils.drawRound(this.panelX, profileY, this.modulePanelWidth, profileHeight, 8.0F, GLASS_BG);
        RoundedUtils.drawRoundOutline(
            this.panelX, profileY, this.modulePanelWidth, profileHeight, 8.0F, 0.8F, BORDER_COLOR, BORDER_COLOR
        );
        this.drawProfileContent(profileY, mouseX, mouseY);
        float settingX = this.panelX + this.modulePanelWidth + 8.0F;
        float settingW = ex - settingX;
        RoundedUtils.drawRound(settingX, contentTopY, settingW, contentHeight, 8.0F, GLASS_BG);
        RoundedUtils.drawRoundOutline(
            settingX, contentTopY, settingW, contentHeight, 8.0F, 0.8F, BORDER_COLOR, BORDER_COLOR
        );
        if (this.currentCategory == null) {
            this.drawTargetsScreen(mouseX, mouseY, settingX, contentTopY, settingW, contentHeight);
        } else {
            this.drawModulesAndSettings(
                mouseX, mouseY, contentTopY, moduleListHeight, settingX, settingW, contentHeight
            );
        }

        if (this.showSettingsPopup) {
            this.drawColorSettingsPopup(mouseX, mouseY);
        }
    }

    private void drawHeaderContent(float ex, int mouseX, int mouseY) {
        this.drawCustomImage("miau/moduleimage/clickgui.png", this.panelX + 12.0F, this.panelY + 11.0F, 75.0F, 20.0F);
        Font fontCat = FontRepository.getFont("Inter Bold", 16.0F);
        float catAreaStart = this.panelX + 105.0F;
        float catAreaEnd = ex - 30.0F;
        float catY = this.panelY + 13.0F;
        this.startScissorBox(catAreaStart, this.panelY, catAreaEnd, this.panelY + this.getHeaderHeight());
        float catPos = this.categoryScroll;
        String[] categories = new String[]{
            "Combat", "Movement", "Player", "Render", "Ghost", "Network", "Minigames", "Misc"
        };

        for (String category : categories) {
            float sw = fontCat.getStringWidth(category);
            float sx = catAreaStart + catPos;
            boolean isSelected = this.currentCategory != null && this.currentCategory.equalsIgnoreCase(category);
            Color color = isSelected ? TEXT_SELECTED : TEXT_UNSELECTED;
            this.drawCustomImage(
                "miau/moduleimage/" + category.toLowerCase() + ".png", sx - 14.0F, catY + 1.0F, 12.0F, 12.0F
            );
            fontCat.draw(category, sx, catY, color.getRGB(), false);
            if (isSelected) {
                RoundedUtils.drawRound(
                    sx - 14.0F, this.panelY + this.getHeaderHeight() - 4.0F, sw + 18.0F, 2.0F, 1.0F, themeColor
                );
            }

            catPos += sw + 26.0F;
        }

        String targetsLabel = "Targets";
        float tsw = fontCat.getStringWidth(targetsLabel);
        float targetsStart = catAreaStart + catPos;
        boolean isTargetsSelected = this.currentCategory == null;
        Color tColor = isTargetsSelected ? TEXT_SELECTED : TEXT_UNSELECTED;
        fontCat.draw(targetsLabel, targetsStart, catY, tColor.getRGB(), false);
        if (isTargetsSelected) {
            RoundedUtils.drawRound(
                targetsStart - 1.0F, this.panelY + this.getHeaderHeight() - 4.0F, tsw + 2.0F, 2.0F, 1.0F, themeColor
            );
        }

        GL11.glDisable(3089);
        float closeX = ex - 20.0F;
        float closeY = this.panelY + 13.0F;
        Font fontClose = FontRepository.getFont("Inter Bold", 16.0F);
        boolean isCloseHover = this.isHover(
            mouseX, mouseY, closeX - 4.0F, closeY - 4.0F, closeX + 12.0F, closeY + 12.0F
        );
        fontClose.draw(
            "✕", closeX, closeY, isCloseHover ? new Color(255, 90, 90).getRGB() : TEXT_UNSELECTED.getRGB(), false
        );
    }

    private void drawProfileContent(float profileY, int mouseX, int mouseY) {
        if (mc.field_71439_g != null) {
            GL11.glPushMatrix();
            mc.func_110434_K().func_110577_a(mc.field_71439_g.func_110306_p());
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            Gui.func_152125_a((int)(this.panelX + 8.0F), (int)(profileY + 6.0F), 8.0F, 8.0F, 8, 8, 24, 24, 64.0F, 64.0F);
            GL11.glPopMatrix();
            Font fontBold = FontRepository.getFont("Inter Bold", 13.0F);
            Font fontSmall = FontRepository.getFont("Inter Regular", 11.0F);
            fontBold.draw(
                mc.field_71439_g.func_70005_c_(), this.panelX + 36.0F, profileY + 6.0F, TEXT_SELECTED.getRGB(), false
            );
            int mins = mc.field_71439_g.field_70173_aa / 1200;
            fontSmall.draw(
                String.format("Time: %02dh %02dm", mins / 60, mins % 60),
                this.panelX + 36.0F,
                profileY + 18.0F,
                TEXT_UNSELECTED.getRGB(),
                false
            );
        }

        float discX = this.panelX + 8.0F;
        float discY = profileY + 32.0F;
        float discW = this.modulePanelWidth - 16.0F;
        float discH = 16.0F;
        boolean isDiscHover = this.isHover(mouseX, mouseY, discX, discY, discX + discW, discY + discH);
        RoundedUtils.drawRound(
            discX, discY, discW, discH, 4.0F, isDiscHover ? new Color(88, 101, 242, 180) : new Color(88, 101, 242, 120)
        );
        this.drawCustomImage("miau/moduleimage/discord.png", discX + 4.0F, discY + 2.0F, 12.0F, 12.0F);
        Font fontDisc = FontRepository.getFont("Inter Bold", 11.0F);
        fontDisc.draw("Discord Community", discX + 20.0F, discY + 4.0F, Color.WHITE.getRGB(), false);
        float settingX = this.panelX + this.modulePanelWidth - 20.0F;
        float settingY = profileY + 8.0F;
        Font fontIcon = FontRepository.getFont("Inter Bold", 15.0F);
        fontIcon.draw("⚙", settingX, settingY, TEXT_UNSELECTED.getRGB(), false);
    }

    private void drawModulesAndSettings(
        int mouseX,
        int mouseY,
        float contentTopY,
        float moduleListHeight,
        float settingX,
        float settingW,
        float contentHeight
    ) {
        Font fontModule = FontRepository.getFont("Inter Bold", 14.0F);
        float moduleItemHeight = 30.0F;
        List<Module> filteredModules = this.getFilteredModules();
        this.startScissorBox(
            this.panelX + 2.0F,
            contentTopY + 4.0F,
            this.panelX + this.modulePanelWidth - 2.0F,
            contentTopY + moduleListHeight - 4.0F
        );
        float currentY = contentTopY + 6.0F + this.moduleScroll;

        for (Module module : filteredModules) {
            float msx = this.panelX + 6.0F;
            float mWidth = this.modulePanelWidth - 12.0F;
            boolean isSelected = module.getName().equalsIgnoreCase(this.currentModule);
            boolean isHovered = this.isHover(
                mouseX, mouseY, msx, currentY, msx + mWidth, currentY + moduleItemHeight - 4.0F
            );
            if (isSelected) {
                RoundedUtils.drawRound(
                    msx, currentY, mWidth, moduleItemHeight - 4.0F, 6.0F, new Color(255, 255, 255, 15)
                );
                RoundedUtils.drawRound(msx, currentY + 3.0F, 3.0F, moduleItemHeight - 10.0F, 1.5F, themeColor);
            } else if (isHovered) {
                RoundedUtils.drawRound(
                    msx, currentY, mWidth, moduleItemHeight - 4.0F, 6.0F, new Color(255, 255, 255, 8)
                );
            }

            Color colorText = module.isEnabled() ? themeColor : (isSelected ? TEXT_SELECTED : TEXT_UNSELECTED);
            fontModule.draw(
                module.getName(), msx + (isSelected ? 12.0F : 8.0F), currentY + 7.0F, colorText.getRGB(), false
            );
            currentY += moduleItemHeight;
        }

        GL11.glDisable(3089);
        if (this.currentModule == null && !filteredModules.isEmpty()) {
            this.currentModule = filteredModules.get(0).getName();
        }

        if (this.currentModule != null) {
            Module module = null;

            for (Module mod : Miau.moduleManager.modules.values()) {
                if (mod.getName().equalsIgnoreCase(this.currentModule)) {
                    module = mod;
                    break;
                }
            }

            if (module != null) {
                Font fontLarge = FontRepository.getFont("Inter Bold", 20.0F);
                float titleX = settingX + 15.0F;
                float titleY = contentTopY + 12.0F;
                fontLarge.draw(module.getName(), titleX, titleY, TEXT_SELECTED.getRGB(), false);
                Font fontSub = FontRepository.getFont("Inter Regular", 12.0F);
                fontSub.draw(
                    "Adjust settings for " + module.getName() + ".",
                    titleX,
                    titleY + 16.0F,
                    TEXT_UNSELECTED.getRGB(),
                    false
                );
                float valueStartY = titleY + 36.0F;
                boolean isAmbience = "Ambience".equalsIgnoreCase(module.getName());
                float settingsBoxW = isAmbience ? settingW * 0.55F : settingW - 30.0F;
                this.startScissorBox(titleX, valueStartY, titleX + settingsBoxW, contentTopY + contentHeight - 10.0F);
                List<MiauMinusClickGui.AbstractValueComponent> components = this.getComponents(module);
                float totalValueY = 0.0F;

                for (MiauMinusClickGui.AbstractValueComponent component : components) {
                    if (component.value.isVisible()) {
                        float valueY = valueStartY + totalValueY + this.valueScroll;
                        component.drawValueName(titleX, valueY);
                        totalValueY += component.drawValue(
                            mouseX, mouseY, titleX, valueY, titleX + settingsBoxW, contentTopY + contentHeight
                        );
                    }
                }

                GL11.glDisable(3089);
                if (isAmbience) {
                    float previewX = titleX + settingsBoxW + 15.0F;
                    float previewW = settingX + settingW - previewX - 15.0F;
                    if (previewW > 80.0F) {
                        int currentTime = 6000;

                        for (MiauMinusClickGui.AbstractValueComponent comp : components) {
                            if (comp.value.getName().equalsIgnoreCase("Time") && comp.value instanceof IntProperty) {
                                currentTime = ((IntProperty)comp.value).getValue();
                                break;
                            }
                        }

                        RoundedUtils.drawRound(previewX, valueStartY, previewW, 120.0F, 6.0F, new Color(15, 22, 36));
                        RoundedUtils.drawRoundOutline(
                            previewX, valueStartY, previewW, 120.0F, 6.0F, 1.0F, BORDER_COLOR, BORDER_COLOR
                        );
                        Color skyColor = this.getSkyColorForTime(currentTime);
                        RoundedUtils.drawRound(
                            previewX + 2.0F, valueStartY + 2.0F, previewW - 4.0F, 85.0F, 5.0F, skyColor
                        );
                        float sunProgress = currentTime / 24000.0F;
                        float sunX = previewX + 15.0F + (previewW - 30.0F) * sunProgress;
                        float sunY = valueStartY + 45.0F - (float)Math.sin(sunProgress * Math.PI * 2.0) * 25.0F;
                        if (currentTime >= 12000 && currentTime < 23500) {
                            RoundedUtils.drawRound(
                                sunX - 6.0F, sunY - 6.0F, 12.0F, 12.0F, 6.0F, new Color(220, 220, 240, 200)
                            );
                        } else {
                            RoundedUtils.drawRound(
                                sunX - 7.0F, sunY - 7.0F, 14.0F, 14.0F, 7.0F, new Color(255, 230, 100, 220)
                            );
                        }

                        String timeLabel = this.getTimeName(currentTime);
                        Font fontTime = FontRepository.getFont("Inter Bold", 12.0F);
                        fontTime.draw(timeLabel, previewX + 8.0F, valueStartY + 95.0F, Color.WHITE.getRGB(), false);
                        float presetY = valueStartY + 128.0F;
                        float presetH = contentTopY + contentHeight - presetY - 10.0F;
                        if (presetH > 25.0F) {
                            RoundedUtils.drawRound(previewX, presetY, previewW, presetH, 6.0F, GLASS_BG);
                            RoundedUtils.drawRoundOutline(
                                previewX, presetY, previewW, presetH, 6.0F, 1.0F, BORDER_COLOR, BORDER_COLOR
                            );
                            fontSub.draw("Toi Bi Gay", previewX + 8.0F, presetY + 5.0F, TEXT_SELECTED.getRGB(), false);
                            this.ambiencePresetButtons.clear();
                            String[] presets = new String[]{"Sunrise", "Day", "Afternoon", "Night", "Midnight"};
                            int[] presetTimes = new int[]{23000, 6000, 13000, 18000, 0};
                            float btnY = presetY + 18.0F;
                            float btnW = previewW - 12.0F;
                            float btnH = 14.0F;
                            Font fontBtn = FontRepository.getFont("Inter Regular", 11.0F);

                            for (int i = 0; i < presets.length; i++) {
                                float btnX = previewX + 6.0F;
                                boolean isHoverBtn = this.isHover(mouseX, mouseY, btnX, btnY, btnX + btnW, btnY + btnH);
                                RoundedUtils.drawRound(
                                    btnX,
                                    btnY,
                                    btnW,
                                    btnH,
                                    3.0F,
                                    isHoverBtn ? new Color(255, 255, 255, 30) : new Color(255, 255, 255, 12)
                                );
                                fontBtn.draw(presets[i], btnX + 6.0F, btnY + 2.0F, TEXT_SELECTED.getRGB(), false);
                                this.ambiencePresetButtons
                                    .add(
                                        new MiauMinusClickGui.AmbiencePresetButton(
                                            btnX, btnY, btnW, btnH, presetTimes[i]
                                        )
                                    );
                                btnY += btnH + 3.0F;
                            }
                        }
                    }
                }
            }
        }
    }

    private Color getSkyColorForTime(int time) {
        if (time >= 22500 || time < 1000) {
            return new Color(15, 15, 35);
        } else if (time >= 1000 && time < 6000) {
            return new Color(230, 130, 70);
        } else if (time >= 6000 && time < 12000) {
            return new Color(85, 160, 245);
        } else {
            return time >= 12000 && time < 18000 ? new Color(210, 90, 50) : new Color(25, 30, 60);
        }
    }

    private String getTimeName(int time) {
        if (time >= 22500 || time < 1000) {
            return "Midnight (0)";
        } else if (time >= 1000 && time < 6000) {
            return "Sunrise (23000)";
        } else if (time >= 6000 && time < 12000) {
            return "Day (6000)";
        } else {
            return time >= 12000 && time < 18000 ? "Afternoon (13000)" : "Night (18000)";
        }
    }

    private void drawTargetsScreen(
        int mouseX, int mouseY, float settingX, float contentTopY, float settingW, float contentHeight
    ) {
        Font fontTitle = FontRepository.getFont("Inter Bold", 20.0F);
        fontTitle.draw("Entity Targets", settingX + 15.0F, contentTopY + 12.0F, TEXT_SELECTED.getRGB(), false);
        Font fontMedium = FontRepository.getFont("Inter Regular", 16.0F);
        float itemY = contentTopY + 45.0F;

        for (MiauMinusClickGui.TargetEntry entry : this.targetEntries) {
            boolean active = entry.stateGetter.get();
            Color color = active ? themeColor : TEXT_UNSELECTED;
            fontMedium.draw(entry.label, settingX + 15.0F, itemY, color.getRGB(), false);
            itemY += fontMedium.getFontHeight() + 10.0F;
        }
    }

    private void drawColorSettingsPopup(int mouseX, int mouseY) {
        float popX = this.panelX + this.modulePanelWidth + 8.0F;
        float popY = this.endY() - 120.0F;
        float popW = 140.0F;
        float popH = 110.0F;
        RoundedUtils.drawRound(popX, popY, popW, popH, 6.0F, new Color(15, 20, 28, 240));
        RoundedUtils.drawRoundOutline(popX, popY, popW, popH, 6.0F, 1.0F, BORDER_COLOR, BORDER_COLOR);
        Font fontSmall = FontRepository.getFont("Inter Regular", 12.0F);
        fontSmall.draw("RGB Theme Color", popX + 8.0F, popY + 8.0F, TEXT_SELECTED.getRGB(), false);
        float sliderY = popY + 28.0F;
        this.drawColorSlider("R", customR, popX + 8.0F, sliderY, popW - 16.0F, new Color(255, 80, 80));
        this.drawColorSlider("G", customG, popX + 8.0F, sliderY + 22.0F, popW - 16.0F, new Color(80, 255, 80));
        this.drawColorSlider("B", customB, popX + 8.0F, sliderY + 44.0F, popW - 16.0F, new Color(80, 180, 255));
        if (this.draggingSlider != null && Mouse.isButtonDown(0)) {
            float barX = popX + 20.0F;
            float barW = popW - 35.0F;
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

    private void drawColorSlider(String label, int value, float x, float y, float width, Color col) {
        Font font = FontRepository.getFont("Inter Regular", 12.0F);
        font.draw(label, x, y + 1.0F, col.getRGB(), false);
        float barX = x + 12.0F;
        float barY = y + 3.0F;
        float barW = width - 20.0F;
        float barH = 3.0F;
        RoundedUtils.drawRound(barX, barY, barW, barH, 1.5F, new Color(255, 255, 255, 20));
        float pct = value / 255.0F;
        float fillW = barW * pct;
        if (fillW > 0.0F) {
            RoundedUtils.drawRound(barX, barY, fillW, barH, 1.5F, col);
        }

        RoundedUtils.drawRound(barX + fillW - 2.0F, barY - 2.0F, 4.0F, 7.0F, 1.5F, Color.WHITE);
        font.draw(String.valueOf(value), barX + barW + 4.0F, y + 1.0F, TEXT_UNSELECTED.getRGB(), false);
    }

    public void func_73864_a(int mouseX, int mouseY, int mouseButton) {
        float ex = this.endX();
        float ey = this.endY();
        float headerH = this.getHeaderHeight();
        if (mouseButton == 0
            && this.isHover(mouseX, mouseY, ex - 24.0F, this.panelY + 8.0F, ex - 4.0F, this.panelY + 28.0F)) {
            mc.func_147108_a(null);
        } else {
            if (mouseButton == 0 && this.currentModule != null && this.currentModule.equalsIgnoreCase("Ambience")) {
                for (MiauMinusClickGui.AmbiencePresetButton btn : this.ambiencePresetButtons) {
                    if (this.isHover(mouseX, mouseY, btn.x, btn.y, btn.x + btn.width, btn.y + btn.height)) {
                        Module mod = null;

                        for (Module m : Miau.moduleManager.modules.values()) {
                            if (m.getName().equalsIgnoreCase("Ambience")) {
                                mod = m;
                                break;
                            }
                        }

                        if (mod != null && mod.getValues() != null) {
                            for (Property<?> p : mod.getValues()) {
                                if (p.getName().equalsIgnoreCase("Time") && p instanceof IntProperty) {
                                    ((IntProperty)p).setValue(btn.targetTime);
                                    break;
                                }
                            }
                        }

                        return;
                    }
                }
            }

            float profileY = this.panelY + headerH + 8.0F + (ey - (this.panelY + headerH + 8.0F) - 54.0F - 8.0F);
            float discX = this.panelX + 8.0F;
            float discY = profileY + 32.0F;
            float discW = this.modulePanelWidth - 16.0F;
            float discH = 16.0F;
            if (mouseButton == 0 && this.isHover(mouseX, mouseY, discX, discY, discX + discW, discY + discH)) {
                try {
                    Desktop.getDesktop().browse(new URI("https://discord.gg/4r9M52zRge"));
                } catch (Exception var36) {
                }
            } else {
                float settingX = this.panelX + this.modulePanelWidth - 24.0F;
                float settingY = profileY + 8.0F;
                if (mouseButton == 0
                    && this.isHover(
                        mouseX, mouseY, settingX - 4.0F, settingY - 4.0F, settingX + 16.0F, settingY + 16.0F
                    )) {
                    this.showSettingsPopup = !this.showSettingsPopup;
                } else {
                    Font fontCat = FontRepository.getFont("Inter Bold", 16.0F);
                    float catAreaStart = this.panelX + 105.0F;
                    float catY = this.panelY + 13.0F;
                    float catPos = this.categoryScroll;
                    String[] categories = new String[]{
                        "Combat", "Movement", "Player", "Render", "Ghost", "Network", "Minigames", "Misc"
                    };

                    for (String category : categories) {
                        float sw = fontCat.getStringWidth(category);
                        float sx = catAreaStart + catPos;
                        if (this.isHover(
                                mouseX,
                                mouseY,
                                sx - 14.0F,
                                catY - 3.0F,
                                sx + sw + 8.0F,
                                catY + fontCat.getFontHeight() + 3.0F
                            )
                            && mouseButton == 0) {
                            this.setCategory(category);
                            return;
                        }

                        catPos += sw + 26.0F;
                    }

                    String targetsLabel = "Targets";
                    float tsx = catAreaStart + catPos;
                    float tex = tsx + fontCat.getStringWidth(targetsLabel);
                    if (this.isHover(
                            mouseX, mouseY, tsx - 3.0F, catY - 3.0F, tex + 8.0F, catY + fontCat.getFontHeight() + 3.0F
                        )
                        && mouseButton == 0) {
                        this.setCategory(null);
                    } else if (mouseButton == 0
                        && this.isHover(mouseX, mouseY, this.panelX, this.panelY, ex, this.panelY + headerH)) {
                        this.isDragging = true;
                        this.dragOffX = mouseX - this.panelX;
                        this.dragOffY = mouseY - this.panelY;
                    } else {
                        float contentTopY = this.panelY + headerH + 8.0F;
                        float moduleListHeight = ey - contentTopY - 54.0F - 8.0F;
                        if (this.isHover(
                            mouseX,
                            mouseY,
                            this.panelX + 2.0F,
                            contentTopY,
                            this.panelX + this.modulePanelWidth,
                            contentTopY + moduleListHeight
                        )) {
                            float moduleItemHeight = 30.0F;
                            List<Module> filteredModules = this.getFilteredModules();
                            float currentY = contentTopY + 6.0F + this.moduleScroll;

                            for (Module module : filteredModules) {
                                if (this.isHover(
                                    mouseX,
                                    mouseY,
                                    this.panelX + 4.0F,
                                    currentY,
                                    this.panelX + this.modulePanelWidth - 4.0F,
                                    currentY + moduleItemHeight - 4.0F
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
                                if (mod.getName().equalsIgnoreCase(this.currentModule)) {
                                    module = mod;
                                    break;
                                }
                            }

                            if (module != null) {
                                float titleX = this.panelX + this.modulePanelWidth + 23.0F;
                                float valueStartY = contentTopY + 48.0F;
                                float settingW = ex - (this.panelX + this.modulePanelWidth + 8.0F);
                                boolean isAmbience = "Ambience".equalsIgnoreCase(module.getName());
                                float settingsBoxW = isAmbience ? settingW * 0.55F : settingW - 30.0F;
                                List<MiauMinusClickGui.AbstractValueComponent> components = this.getComponents(module);
                                float totalValueY = 0.0F;

                                for (MiauMinusClickGui.AbstractValueComponent component : components) {
                                    if (component.value.isVisible()) {
                                        float valueY = valueStartY + totalValueY + this.valueScroll;
                                        float inc = component.mouseClicked(
                                            mouseX, mouseY, titleX, valueY, titleX + settingsBoxW, ey, mouseButton
                                        );
                                        if (inc == -1.0F) {
                                            this.draggingComponent = component;
                                            this.dragBarX = component.getBarStartX(titleX);
                                            this.dragBarX2 = titleX + settingsBoxW;
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
                float headerH = this.getHeaderHeight();
                float contentTopY = this.panelY + headerH + 8.0F;
                float moduleListHeight = this.endY() - contentTopY - 54.0F - 8.0F;
                if (this.isHover(
                    mouseX,
                    mouseY,
                    this.panelX,
                    contentTopY,
                    this.panelX + this.modulePanelWidth,
                    contentTopY + moduleListHeight
                )) {
                    this.moduleScroll += wheel > 0 ? 20.0F : -20.0F;
                    float moduleItemHeight = 30.0F;
                    List<Module> filteredModules = this.getFilteredModules();
                    float totalHeight = filteredModules.size() * moduleItemHeight + 8.0F;
                    float maxScroll = Math.min(0.0F, moduleListHeight - totalHeight);
                    if (this.moduleScroll > 0.0F) {
                        this.moduleScroll = 0.0F;
                    }

                    if (this.moduleScroll < maxScroll) {
                        this.moduleScroll = maxScroll;
                    }
                } else if (this.isHover(
                    mouseX, mouseY, this.panelX + this.modulePanelWidth, contentTopY, this.endX(), this.endY()
                )) {
                    this.valueScroll += wheel > 0 ? 20.0F : -20.0F;
                    if (this.valueScroll > 0.0F) {
                        this.valueScroll = 0.0F;
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
            return FontRepository.getFont("Inter Regular", 14.0F);
        }

        public void drawValueName(float x, float y) {
            this.getFont().draw(this.value.getName(), x, y + 2.0F, MiauMinusClickGui.TEXT_SELECTED.getRGB(), false);
        }

        public float getBarStartX(float x) {
            return x;
        }

        public abstract float drawValue(int var1, int var2, float var3, float var4, float var5, float var6);

        public abstract float mouseClicked(int var1, int var2, float var3, float var4, float var5, float var6, int var7);

        public void updateDrag(int mouseX, float barX, float barX2) {
        }
    }

    public static class AmbiencePresetButton {
        public float x;
        public float y;
        public float width;
        public float height;
        public int targetTime;

        public AmbiencePresetButton(float x, float y, float width, float height, int targetTime) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.targetTime = targetTime;
        }
    }

    public static class BoolValueComponent extends MiauMinusClickGui.AbstractValueComponent {
        private BooleanProperty v;

        public BoolValueComponent(BooleanProperty v) {
            super(v);
            this.v = v;
        }

        @Override
        public float drawValue(int mouseX, int mouseY, float x, float y, float x2, float y2) {
            float switchW = 28.0F;
            float switchH = 14.0F;
            float switchX = x2 - switchW;
            float switchY = y + 1.0F;
            if (this.v.getValue()) {
                RoundedUtils.drawRound(switchX, switchY, switchW, switchH, switchH / 2.0F, MiauMinusClickGui.themeColor);
                RoundedUtils.drawRound(
                    switchX + switchW - switchH + 2.0F,
                    switchY + 2.0F,
                    switchH - 4.0F,
                    switchH - 4.0F,
                    (switchH - 4.0F) / 2.0F,
                    Color.WHITE
                );
            } else {
                RoundedUtils.drawRound(switchX, switchY, switchW, switchH, switchH / 2.0F, new Color(255, 255, 255, 25));
                RoundedUtils.drawRound(
                    switchX + 2.0F,
                    switchY + 2.0F,
                    switchH - 4.0F,
                    switchH - 4.0F,
                    (switchH - 4.0F) / 2.0F,
                    MiauMinusClickGui.TEXT_UNSELECTED
                );
            }

            return 22.0F;
        }

        @Override
        public float mouseClicked(int mouseX, int mouseY, float x, float y, float x2, float y2, int mouseButton) {
            if (mouseButton == 0 && this.isHover(mouseX, mouseY, x, y - 1.0F, x2, y + 16.0F)) {
                this.v.setValue(!this.v.getValue());
                return 0.0F;
            } else {
                return 22.0F;
            }
        }

        private boolean isHover(int mx, int my, float x, float y, float x2, float y2) {
            return mx >= x && mx <= x2 && my >= y && my <= y2;
        }
    }

    public static class ColorValueComponent extends MiauMinusClickGui.AbstractValueComponent {
        private boolean expanded = false;
        private String draggingChannel = null;

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

        @Override
        public float drawValue(int mouseX, int mouseY, float x, float y, float x2, float y2) {
            Color curCol = this.getColorVal();
            float previewX = x2 - 24.0F;
            RoundedUtils.drawRound(previewX, y, 20.0F, 12.0F, 3.0F, curCol);
            RoundedUtils.drawRoundOutline(previewX, y, 20.0F, 12.0F, 3.0F, 1.0F, Color.WHITE, Color.WHITE);
            if (!this.expanded) {
                return 24.0F;
            }

            float subY = y + 18.0F;
            float barX = x + 8.0F;
            float barW = x2 - x - 16.0F;
            subY += this.drawSliderRow("R", curCol.getRed(), barX, subY, barW, new Color(255, 80, 80));
            subY += this.drawSliderRow("G", curCol.getGreen(), barX, subY, barW, new Color(80, 255, 80));
            subY += this.drawSliderRow("B", curCol.getBlue(), barX, subY, barW, new Color(80, 180, 255));
            if (this.draggingChannel != null && Mouse.isButtonDown(0)) {
                float pct = Math.max(0.0F, Math.min(1.0F, (mouseX - (barX + 15.0F)) / (barW - 15.0F)));
                int val = Math.round(pct * 255.0F);
                if ("R".equals(this.draggingChannel)) {
                    this.setColorVal(new Color(val, curCol.getGreen(), curCol.getBlue()));
                }

                if ("G".equals(this.draggingChannel)) {
                    this.setColorVal(new Color(curCol.getRed(), val, curCol.getBlue()));
                }

                if ("B".equals(this.draggingChannel)) {
                    this.setColorVal(new Color(curCol.getRed(), curCol.getGreen(), val));
                }
            }

            return subY - y;
        }

        private float drawSliderRow(String channel, int value, float x, float y, float width, Color col) {
            Font font = this.getFont();
            font.draw(channel, x, y, col.getRGB(), false);
            float barX = x + 15.0F;
            float barW = width - 15.0F;
            RoundedUtils.drawRound(barX, y + 3.0F, barW, 3.0F, 1.5F, new Color(255, 255, 255, 20));
            float fillW = barW * (value / 255.0F);
            if (fillW > 0.0F) {
                RoundedUtils.drawRound(barX, y + 3.0F, fillW, 3.0F, 1.5F, col);
            }

            font.draw(String.valueOf(value), barX + barW + 6.0F, y, MiauMinusClickGui.TEXT_UNSELECTED.getRGB(), false);
            return 15.0F;
        }

        @Override
        public float mouseClicked(int mouseX, int mouseY, float x, float y, float x2, float y2, int mouseButton) {
            float previewX = x2 - 24.0F;
            if (mouseButton == 0
                && mouseX >= previewX
                && mouseX <= previewX + 20.0F
                && mouseY >= y
                && mouseY <= y + 12.0F) {
                this.expanded = !this.expanded;
                return 0.0F;
            }

            if (this.expanded && mouseButton == 0) {
                float subY = y + 18.0F;
                float barX = x + 23.0F;
                float barW = x2 - x - 30.0F;
                if (mouseY >= subY && mouseY <= subY + 15.0F && mouseX >= barX && mouseX <= barX + barW) {
                    this.draggingChannel = "R";
                    return -1.0F;
                }

                if (mouseY >= subY + 15.0F && mouseY <= subY + 30.0F && mouseX >= barX && mouseX <= barX + barW) {
                    this.draggingChannel = "G";
                    return -1.0F;
                }

                if (mouseY >= subY + 30.0F && mouseY <= subY + 45.0F && mouseX >= barX && mouseX <= barX + barW) {
                    this.draggingChannel = "B";
                    return -1.0F;
                }
            }

            return 24.0F;
        }
    }

    public static class EntityTargets {
        public static boolean player = true;
        public static boolean mob = true;
        public static boolean animal = true;
        public static boolean invisible = false;
        public static boolean dead = false;
    }

    public static class FloatValueComponent extends MiauMinusClickGui.AbstractValueComponent {
        private FloatProperty v;

        public FloatValueComponent(FloatProperty v) {
            super(v);
            this.v = v;
        }

        @Override
        public float drawValue(int mouseX, int mouseY, float x, float y, float x2, float y2) {
            float minBound = MiauMinusClickGui.getFloatPropMin(this.v);
            float maxBound = MiauMinusClickGui.getFloatPropMax(this.v);
            float rangeBound = maxBound - minBound;
            float curVal = this.v.getValue();
            float percent = rangeBound <= 0.0F ? 0.0F : (curVal - minBound) / rangeBound;
            percent = Math.max(0.0F, Math.min(1.0F, percent));
            String display = String.format("%.2f", curVal);
            Font font = this.getFont();
            float strW = font.getStringWidth(display);
            float valBoxW = Math.max(36.0F, strW + 8.0F);
            RoundedUtils.drawRound(x2 - valBoxW, y - 1.0F, valBoxW, 13.0F, 3.0F, new Color(0, 0, 0, 50));
            font.draw(
                display,
                x2 - valBoxW + (valBoxW - strW) / 2.0F,
                y + 1.0F,
                MiauMinusClickGui.TEXT_SELECTED.getRGB(),
                false
            );
            float barY = y + 18.0F;
            float barW = x2 - x;
            float barH = 3.0F;
            RoundedUtils.drawRound(x, barY, barW, barH, 1.5F, new Color(255, 255, 255, 20));
            float fillEnd = barW * percent;
            if (fillEnd > 0.0F) {
                RoundedUtils.drawRound(
                    x,
                    barY - 1.0F,
                    fillEnd,
                    barH + 2.0F,
                    2.0F,
                    new Color(
                        MiauMinusClickGui.themeColor.getRed(),
                        MiauMinusClickGui.themeColor.getGreen(),
                        MiauMinusClickGui.themeColor.getBlue(),
                        70
                    )
                );
                RoundedUtils.drawRound(x, barY, fillEnd, barH, 1.5F, MiauMinusClickGui.themeColor);
            }

            float knobX = x + fillEnd;
            RoundedUtils.drawRound(
                knobX - 4.0F,
                barY + barH / 2.0F - 4.0F,
                8.0F,
                8.0F,
                4.0F,
                new Color(
                    MiauMinusClickGui.themeColor.getRed(),
                    MiauMinusClickGui.themeColor.getGreen(),
                    MiauMinusClickGui.themeColor.getBlue(),
                    100
                )
            );
            RoundedUtils.drawRound(knobX - 3.0F, barY + barH / 2.0F - 3.0F, 6.0F, 6.0F, 3.0F, Color.WHITE);
            return 26.0F;
        }

        @Override
        public float mouseClicked(int mouseX, int mouseY, float x, float y, float x2, float y2, int mouseButton) {
            float barY = y + 18.0F;
            if (mouseButton == 0 && this.isHover(mouseX, mouseY, x, barY - 3.0F, x2, barY + 6.0F)) {
                this.updateDrag(mouseX, x, x2);
                return -1.0F;
            } else {
                return 26.0F;
            }
        }

        @Override
        public void updateDrag(int mouseX, float barX, float barX2) {
            float span = barX2 - barX;
            float percent = span <= 0.0F ? 0.0F : (mouseX - barX) / span;
            percent = Math.max(0.0F, Math.min(1.0F, percent));
            float minBound = MiauMinusClickGui.getFloatPropMin(this.v);
            float maxBound = MiauMinusClickGui.getFloatPropMax(this.v);
            float newValue = minBound + percent * (maxBound - minBound);
            newValue = Math.round(newValue * 100.0F) / 100.0F;
            this.v.setValue(newValue);
        }

        private boolean isHover(int mx, int my, float x, float y, float x2, float y2) {
            return mx >= x && mx <= x2 && my >= y && my <= y2;
        }
    }

    public static class IntValueComponent extends MiauMinusClickGui.AbstractValueComponent {
        private IntProperty v;

        public IntValueComponent(IntProperty v) {
            super(v);
            this.v = v;
        }

        @Override
        public float drawValue(int mouseX, int mouseY, float x, float y, float x2, float y2) {
            int minBound = MiauMinusClickGui.getIntPropMin(this.v);
            int maxBound = MiauMinusClickGui.getIntPropMax(this.v);
            int rangeBound = maxBound - minBound;
            int curVal = this.v.getValue();
            float percent = rangeBound <= 0 ? 0.0F : (float)(curVal - minBound) / rangeBound;
            percent = Math.max(0.0F, Math.min(1.0F, percent));
            String display = String.valueOf(curVal);
            Font font = this.getFont();
            float strW = font.getStringWidth(display);
            float valBoxW = Math.max(32.0F, strW + 8.0F);
            RoundedUtils.drawRound(x2 - valBoxW, y - 1.0F, valBoxW, 13.0F, 3.0F, new Color(0, 0, 0, 50));
            font.draw(
                display,
                x2 - valBoxW + (valBoxW - strW) / 2.0F,
                y + 1.0F,
                MiauMinusClickGui.TEXT_SELECTED.getRGB(),
                false
            );
            float barY = y + 18.0F;
            float barW = x2 - x;
            float barH = 3.0F;
            RoundedUtils.drawRound(x, barY, barW, barH, 1.5F, new Color(255, 255, 255, 20));
            float fillEnd = barW * percent;
            if (fillEnd > 0.0F) {
                RoundedUtils.drawRound(
                    x,
                    barY - 1.0F,
                    fillEnd,
                    barH + 2.0F,
                    2.0F,
                    new Color(
                        MiauMinusClickGui.themeColor.getRed(),
                        MiauMinusClickGui.themeColor.getGreen(),
                        MiauMinusClickGui.themeColor.getBlue(),
                        70
                    )
                );
                RoundedUtils.drawRound(x, barY, fillEnd, barH, 1.5F, MiauMinusClickGui.themeColor);
            }

            float knobX = x + fillEnd;
            RoundedUtils.drawRound(
                knobX - 4.0F,
                barY + barH / 2.0F - 4.0F,
                8.0F,
                8.0F,
                4.0F,
                new Color(
                    MiauMinusClickGui.themeColor.getRed(),
                    MiauMinusClickGui.themeColor.getGreen(),
                    MiauMinusClickGui.themeColor.getBlue(),
                    100
                )
            );
            RoundedUtils.drawRound(knobX - 3.0F, barY + barH / 2.0F - 3.0F, 6.0F, 6.0F, 3.0F, Color.WHITE);
            return 26.0F;
        }

        @Override
        public float mouseClicked(int mouseX, int mouseY, float x, float y, float x2, float y2, int mouseButton) {
            float barY = y + 18.0F;
            if (mouseButton == 0 && this.isHover(mouseX, mouseY, x, barY - 3.0F, x2, barY + 6.0F)) {
                this.updateDrag(mouseX, x, x2);
                return -1.0F;
            } else {
                return 26.0F;
            }
        }

        @Override
        public void updateDrag(int mouseX, float barX, float barX2) {
            float span = barX2 - barX;
            float percent = span <= 0.0F ? 0.0F : (mouseX - barX) / span;
            percent = Math.max(0.0F, Math.min(1.0F, percent));
            int minBound = MiauMinusClickGui.getIntPropMin(this.v);
            int maxBound = MiauMinusClickGui.getIntPropMax(this.v);
            int newValue = Math.round(minBound + percent * (maxBound - minBound));
            this.v.setValue(newValue);
        }

        private boolean isHover(int mx, int my, float x, float y, float x2, float y2) {
            return mx >= x && mx <= x2 && my >= y && my <= y2;
        }
    }

    public static class ListValueComponent extends MiauMinusClickGui.AbstractValueComponent {
        private ModeProperty v;

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
            Font font = this.getFont();
            float strW = font.getStringWidth(selected);
            float boxW = Math.max(55.0F, strW + 16.0F);
            float boxX = x2 - boxW;
            RoundedUtils.drawRound(boxX, y - 1.0F, boxW, 14.0F, 3.0F, new Color(0, 0, 0, 50));
            font.draw(selected, boxX + 6.0F, y + 1.0F, MiauMinusClickGui.TEXT_SELECTED.getRGB(), false);
            font.draw("v", boxX + boxW - 10.0F, y, MiauMinusClickGui.TEXT_UNSELECTED.getRGB(), false);
            return 22.0F;
        }

        @Override
        public float mouseClicked(int mouseX, int mouseY, float x, float y, float x2, float y2, int mouseButton) {
            if (mouseButton == 0 && this.isHover(mouseX, mouseY, x, y - 1.0F, x2, y + 14.0F)) {
                String[] modes = this.v.getModes();
                int maxModes = modes != null && modes.length > 0 ? modes.length : 10;
                int nextIndex = this.v.getValue() + 1;
                if (nextIndex >= maxModes) {
                    nextIndex = 0;
                }

                this.v.setValue(nextIndex);
                return 0.0F;
            } else {
                return 22.0F;
            }
        }

        private boolean isHover(int mx, int my, float x, float y, float x2, float y2) {
            return mx >= x && mx <= x2 && my >= y && my <= y2;
        }
    }

    public static class TargetEntry {
        public String label;
        private MiauMinusClickGui.TargetEntry.StateGetter stateGetter;
        private MiauMinusClickGui.TargetEntry.Toggle toggle;

        public TargetEntry(
            String label,
            MiauMinusClickGui.TargetEntry.StateGetter stateGetter,
            MiauMinusClickGui.TargetEntry.Toggle toggle
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
