package miau.module.modules.render;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import miau.Miau;
import miau.enums.BlinkModules;
import miau.enums.ChatColors;
import miau.event.EventTarget;
import miau.event.impl.Render2DEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorGuiChat;
import miau.mixin.IAccessorMinecraft;
import miau.mixin.IAccessorRenderManager;
import miau.module.Module;
import miau.module.modules.misc.Balance;
import miau.module.modules.render.hud.InterfaceComponent;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ColorProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.TextProperty;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.math.MathUtil;
import miau.util.render.ColorUtil;
import miau.util.render.MenuBackground;
import miau.util.render.RenderUtil;
import miau.util.render.Themes;
import miau.util.shader.BlurUtils;
import miau.util.shader.RoundedUtils;
import miau.util.vector.Vector2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class HUD extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final Map<Module, InterfaceComponent> components = new HashMap<>();
    private long lastMS = System.currentTimeMillis();
    private List<Module> activeModules = new ArrayList<>();
    private float watermarkFade = 0.0F;
    private int firstWX = 0;
    private int firstWY = 0;
    private boolean watermarkTrack = false;
    private boolean watermarkFirstClick = true;
    private float idleTicks = 0.0F;
    private final List<HUD.FootParticle> particles = new ArrayList<>();
    private double lastX = 0.0;
    private double lastZ = 0.0;
    private String bpsString = "0.00";
    private static final ResourceLocation WATERMARK_IMAGE = new ResourceLocation("miau/watermark.png");
    private static final int WATERMARK_TEXTURE_WIDTH = 1645;
    private static final int WATERMARK_TEXTURE_HEIGHT = 656;
    public final BooleanProperty showWatermark = new BooleanProperty("watermark", true);
    public final ModeProperty hudMode = new ModeProperty("mode", 0, new String[]{"NORMAL", "EXHIBITION", "WATERMARK+"});
    public final FloatProperty watermarkPlusX = new FloatProperty("Watermark+ X", 10.0F, -2000.0F, 2000.0F);
    public final FloatProperty watermarkPlusY = new FloatProperty("Watermark+ Y", 10.0F, -2000.0F, 2000.0F);
    public final FloatProperty watermarkPlusScale = new FloatProperty("Watermark+ Scale", 0.12F, 0.02F, 1.0F);
    public final IntProperty watermarkPlusOpacity = new IntProperty("Watermark+ Opacity", 255, 0, 255);
    public final BooleanProperty watermarkPlusDrag = new BooleanProperty("Watermark+ Drag", true);
    public final BooleanProperty watermarkPlusFade = new BooleanProperty("Watermark+ Fade", true);
    public final ModeProperty watermarkPlusColorMode = new ModeProperty(
        "Watermark+ Color Mode", 0, new String[]{"NONE", "THEME", "STATIC", "RAINBOW"}
    );
    public final ColorProperty watermarkPlusColor = new ColorProperty("Watermark+ Color", 16777215);
    public final ModeProperty watermarkPlusStyle = new ModeProperty(
        "Watermark+ Style", 0, new String[]{"NONE", "SHADOW", "OUTLINE", "REFLECTION"}
    );
    public final FloatProperty watermarkPlusShadowOffset = new FloatProperty(
        "Watermark+ Shadow Offset", 2.0F, 0.5F, 10.0F
    );
    public final IntProperty watermarkPlusShadowOpacity = new IntProperty("Watermark+ Shadow Opacity", 100, 0, 255);
    public final FloatProperty watermarkPlusReflectionGap = new FloatProperty(
        "Watermark+ Reflection Gap", 4.0F, 0.0F, 20.0F
    );
    public final IntProperty watermarkPlusReflectionOpacity = new IntProperty(
        "Watermark+ Reflection Opacity", 90, 0, 255
    );
    public final TextProperty watermarkName = new TextProperty(
        "watermark-name", "Miau Minus", this.showWatermark::getValue
    );
    public final BooleanProperty showCoordinates = new BooleanProperty(
        "coordinates", true, () -> this.hudMode.getValue() == 1
    );
    public final BooleanProperty showTime = new BooleanProperty("show-time", true, this.showWatermark::getValue);
    public final BooleanProperty showFps = new BooleanProperty("show-fps", true, this.showWatermark::getValue);
    public final BooleanProperty showPing = new BooleanProperty("show-ping", true, this.showWatermark::getValue);
    public final BooleanProperty showBps = new BooleanProperty("show-bps", true, this.showWatermark::getValue);
    public final BooleanProperty showBalance = new BooleanProperty("show-balance", false, this.showWatermark::getValue);
    public final BooleanProperty customHotbar = new BooleanProperty("Custom Hotbar", true);
    public final ModeProperty colorAnimation = new ModeProperty(
        "color-animation", 1, new String[]{"STATIC", "FADE", "RAINBOW"}
    );
    public final ModeProperty modulesToShow = new ModeProperty(
        "modules-to-show", 1, new String[]{"ALL", "EXCLUDE RENDER", "ONLY BOUND"}
    );
    public final ModeProperty fontFace = new ModeProperty("Font", 1, FontRepository.FONT_NAMES);
    public final ModeProperty posX = new ModeProperty("position-x", 0, new String[]{"LEFT", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 0, new String[]{"TOP", "BOTTOM"});
    public final IntProperty offsetX = new IntProperty("offset-x", 2, 0, 255);
    public final IntProperty offsetY = new IntProperty("offset-y", 2, 0, 255);
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final BooleanProperty showBar = new BooleanProperty("bar", true);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty suffixes = new BooleanProperty("suffixes", true);
    public final BooleanProperty lowerCase = new BooleanProperty("lower-case", false);
    public final BooleanProperty chatOutline = new BooleanProperty("chat-outline", true);
    public final BooleanProperty blinkTimer = new BooleanProperty("blink-timer", true);
    public final BooleanProperty toggleSound = new BooleanProperty("toggle-sounds", true);
    public final BooleanProperty toggleAlerts = new BooleanProperty("toggle-alerts", false);
    public final BooleanProperty notifications = new BooleanProperty("notifications", true);
    public final BooleanProperty shaders = new BooleanProperty("Shaders", false);
    public final IntProperty backgroundAlpha = new IntProperty("Background Alpha", 110, 0, 255);
    public final FloatProperty roundingRadius = new FloatProperty("Rounding Radius", 1.0F, 0.0F, 10.0F);
    public final ModeProperty menuBackground = new ModeProperty(
        "Menu Background", MenuBackground.NAMES.length - 1, MenuBackground.NAMES
    );

    public float getModuleListHeight() {
        float itemHeight = this.hudMode.getValue() == 1 ? 12.0F : 10.0F;
        int count = 0;

        for (Module module : Miau.moduleManager.modules.values()) {
            if (module.isEnabled()
                && (this.modulesToShow.getValue() != 1 || !module.isHidden())
                && (this.modulesToShow.getValue() != 2 || module.getKey() != 0)) {
                String name = module.getName().toLowerCase();
                if (!name.equals("hud") && !name.equals("gui") && !name.equals("clickgui")) {
                    count++;
                }
            }
        }

        return count * itemHeight * this.scale.getValue();
    }

    private InterfaceComponent getComponent(Module module) {
        return this.components.computeIfAbsent(module, InterfaceComponent::new);
    }

    private String getModuleName(Module module) {
        String moduleName = module.getName();
        if (this.lowerCase.getValue()) {
            moduleName = moduleName.toLowerCase(Locale.ROOT);
        }

        return moduleName;
    }

    private String[] getModuleSuffix(Module module) {
        String[] moduleSuffix = module.getSuffix();
        if (this.lowerCase.getValue()) {
            for (int i = 0; i < moduleSuffix.length; i++) {
                moduleSuffix[i] = moduleSuffix[i].toLowerCase();
            }
        }

        return moduleSuffix;
    }

    private int getModuleWidth(Module module) {
        return this.calculateStringWidth(this.getModuleName(module), this.getModuleSuffix(module));
    }

    private int calculateStringWidth(String string, String[] arr) {
        int width = this.getFont().getStringWidth(string);
        if (this.suffixes.getValue()) {
            for (String str : arr) {
                width += 3 + this.getFont().getStringWidth(str);
            }
        }

        return width;
    }

    public Font getFont() {
        return FontRepository.getHudFont(18);
    }

    public HUD() {
        super("HUD", true, true);
        FontRepository.setHudFace(this.fontFace.getValue());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRenderOverlay(Pre event) {
        if (this.isEnabled()
            && this.customHotbar.getValue()
            && (
                event.type == ElementType.HOTBAR
                    || event.type == ElementType.HEALTH
                    || event.type == ElementType.FOOD
                    || event.type == ElementType.ARMOR
                    || event.type == ElementType.EXPERIENCE
            )) {
            event.setCanceled(true);
        }
    }

    @Override
    public void verifyValue(String name) {
        if (name.equalsIgnoreCase("Font")) {
            FontRepository.setHudFace(this.fontFace.getValue());
            FontRepository.clearCache();
        }
    }

    public Color getColor(long time) {
        return this.getColor(time, 0L);
    }

    public Color getColor(long time, long yPos) {
        Themes theme = Themes.getCurrentTheme();
        switch (this.colorAnimation.getValue()) {
            case 0:
                return theme.getFirstColor();
            case 1:
                return theme.getAccentColor(new Vector2d(0.0, yPos));
            case 2:
                return ColorUtil.rainbow((int)(time * 500L / 6L));
            default:
                return Color.white;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE && mc.field_71439_g != null) {
            double dist = Math.hypot(
                mc.field_71439_g.field_70165_t - this.lastX, mc.field_71439_g.field_70161_v - this.lastZ
            );
            this.bpsString = String.valueOf(
                MathUtil.round(dist * 20.0 * ((IAccessorMinecraft)mc).getTimer().field_74278_d, 2)
            );
            this.lastX = mc.field_71439_g.field_70165_t;
            this.lastZ = mc.field_71439_g.field_70161_v;
        }

        if (this.isEnabled() && event.getType() == EventType.POST) {
            this.activeModules = Miau.moduleManager
                .modules
                .values()
                .stream()
                .filter(
                    module -> module.isEnabled()
                        && (this.modulesToShow.getValue() == 0 || !module.isHidden())
                        && this.getComponent(module).shouldDisplay(this)
                )
                .sorted(Comparator.comparingInt(this::getModuleWidth).reversed())
                .collect(Collectors.toList());

            try {
                Miau.clientName = ChatColors.getDynamicPrefix();
            } catch (Exception var4) {
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.func_175598_ae() != null) {
            if (mc.field_71439_g.field_71158_b == null
                || mc.field_71439_g.field_71158_b.field_78900_b == 0.0F
                    && mc.field_71439_g.field_71158_b.field_78902_a == 0.0F) {
                this.idleTicks = Math.min(100.0F, this.idleTicks + 1.0F);
            } else {
                this.idleTicks = Math.max(0.0F, this.idleTicks - 2.0F);
            }

            double pX = RenderUtil.lerpDouble(
                    mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70142_S, event.getPartialTicks()
                )
                - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX();
            double pY = RenderUtil.lerpDouble(
                    mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70137_T, event.getPartialTicks()
                )
                - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY()
                + 0.02;
            double pZ = RenderUtil.lerpDouble(
                    mc.field_71439_g.field_70161_v, mc.field_71439_g.field_70136_U, event.getPartialTicks()
                )
                - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ();
            long time = System.currentTimeMillis();
            Color themeColor = this.getColor(time);
            GlStateManager.func_179094_E();
            GlStateManager.func_179123_a();
            GlStateManager.func_179147_l();
            GlStateManager.func_179090_x();
            GlStateManager.func_179140_f();
            GlStateManager.func_179129_p();
            GlStateManager.func_179132_a(false);
            GlStateManager.func_179112_b(770, 1);
            float radius = 0.75F;
            int points = 45;
            GL11.glBegin(6);
            GL11.glColor4f(
                themeColor.getRed() / 255.0F, themeColor.getGreen() / 255.0F, themeColor.getBlue() / 255.0F, 0.12F
            );
            GL11.glVertex3d(pX, pY, pZ);

            for (int i = 0; i <= points; i++) {
                double angle = i * (Math.PI * 2) / points;
                double rx = pX + Math.sin(angle) * radius;
                double rz = pZ + Math.cos(angle) * radius;
                GL11.glVertex3d(rx, pY, rz);
            }

            GL11.glEnd();
            GL11.glLineWidth(2.0F);
            GL11.glBegin(2);

            for (int i = 0; i < points; i++) {
                double angle = i * (Math.PI * 2) / points;
                Color pointColor = this.getColor(time + i * 40L);
                GL11.glColor4f(
                    pointColor.getRed() / 255.0F, pointColor.getGreen() / 255.0F, pointColor.getBlue() / 255.0F, 0.4F
                );
                double rx = pX + Math.sin(angle) * radius;
                double rz = pZ + Math.cos(angle) * radius;
                GL11.glVertex3d(rx, pY, rz);
            }

            GL11.glEnd();
            if (this.idleTicks > 20.0F && Math.random() < this.idleTicks / 100.0F * 0.4) {
                double spawnAngle = Math.random() * Math.PI * 2.0;
                double spawnX = pX + Math.sin(spawnAngle) * (radius * 0.9);
                double spawnZ = pZ + Math.cos(spawnAngle) * (radius * 0.9);
                double mX = Math.sin(spawnAngle) * 0.008 + (Math.random() - 0.5) * 0.005;
                double mY = 0.015 + Math.random() * 0.015;
                double mZ = Math.cos(spawnAngle) * 0.008 + (Math.random() - 0.5) * 0.005;
                int maxAge = 25 + (int)(Math.random() * 20.0);
                this.particles
                    .add(
                        new HUD.FootParticle(
                            spawnX, pY, spawnZ, mX, mY, mZ, maxAge, this.getColor(time + (long)(Math.random() * 500.0))
                        )
                    );
            }

            this.particles.removeIf(p -> {
                boolean dead = p.update();
                float lifeRatio = 1.0F - (float)p.age / p.maxAge;
                float alpha = lifeRatio * 0.6F;
                float pSize = 0.035F * lifeRatio;
                Color c = p.color;
                GL11.glColor4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, alpha);
                GL11.glBegin(7);
                GL11.glVertex3d(p.x - pSize, p.y - pSize, p.z - pSize);
                GL11.glVertex3d(p.x + pSize, p.y - pSize, p.z - pSize);
                GL11.glVertex3d(p.x + pSize, p.y + pSize, p.z + pSize);
                GL11.glVertex3d(p.x - pSize, p.y + pSize, p.z + pSize);
                GL11.glEnd();
                return dead;
            });
            GlStateManager.func_179099_b();
            GlStateManager.func_179121_F();
            GlStateManager.func_179117_G();
        }
    }

    private String getExhibitionWatermark() {
        String customName = this.watermarkName.getValue();
        if (customName == null || customName.isEmpty()) {
            customName = "Miau Minus";
        }

        int ping = 0;
        if (mc.func_147114_u() != null && mc.field_71439_g != null) {
            NetworkPlayerInfo playerInfo = mc.func_147114_u().func_175102_a(mc.field_71439_g.func_110124_au());
            if (playerInfo != null) {
                ping = playerInfo.func_178853_c();
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
        String formattedTime = sdf.format(new Date());
        String text = customName.charAt(0) + "§7" + customName.substring(1);
        if (this.showTime.getValue()) {
            text = text + " [§f" + formattedTime + "§7]";
        }

        if (this.showFps.getValue()) {
            text = text + " [§f" + Minecraft.func_175610_ah() + " FPS§7]";
        }

        if (this.showPing.getValue()) {
            text = text + " [§f" + ping + "ms§7]";
        }

        if (this.showBps.getValue()) {
            text = text + " [§f" + this.bpsString + " BPS§7]";
        }

        if (this.showBalance.getValue()) {
            text = text + " [§f" + Balance.balance + " Balance§7]";
        }

        return text;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        long currentMS = System.currentTimeMillis();
        float delta = (float)(currentMS - this.lastMS);
        this.lastMS = currentMS;
        if (delta > 200.0F || delta < 0.0F) {
            delta = 16.0F;
        }

        ScaledResolution sr = new ScaledResolution(mc);

        for (Module module : Miau.moduleManager.modules.values()) {
            InterfaceComponent component = this.getComponent(module);
            boolean shouldBeVisible = module.isEnabled()
                && (this.modulesToShow.getValue() == 0 || !module.isHidden())
                && component.shouldDisplay(this);
            if (shouldBeVisible) {
                component.animationTime = (float)Math.min(1.0, component.animationTime + delta * 0.006);
            } else {
                component.animationTime = (float)Math.max(0.0, component.animationTime - delta * 0.006);
            }
        }

        List<InterfaceComponent> animatingComponents = Miau.moduleManager
            .modules
            .values()
            .stream()
            .map(this::getComponent)
            .filter(c -> c.animationTime > 0.001)
            .sorted(Comparator.<InterfaceComponent>comparingInt(c -> this.getModuleWidth(c.module)).reversed())
            .collect(Collectors.toList());
        boolean isMcFont = FontRepository.isMinecraftSelected();
        float heightExhibition = 12.0F;
        float heightNormal = 10.0F;
        float currentYExhibition = this.offsetY.getValue().intValue() + 1.0F * this.scale.getValue();
        float currentYNormal = this.offsetY.getValue().intValue() + 1.0F * this.scale.getValue();
        if (this.posX.getValue() == 0) {
            if (this.posY.getValue() == 0) {
                if (this.showWatermark.getValue()) {
                    float watermarkHeight = this.getFont().getFontHeight() + 6.0F;
                    currentYExhibition += watermarkHeight;
                    currentYNormal += watermarkHeight;
                }
            } else {
                float bottomOffset = 0.0F;
                if (this.hudMode.getValue() == 1 && this.showCoordinates.getValue() && mc.field_71439_g != null) {
                    bottomOffset += this.getFont().getFontHeight() * 3 + 12.0F;
                }

                currentYExhibition += bottomOffset;
                currentYNormal += bottomOffset;
            }
        }

        if (this.posY.getValue() == 1) {
            currentYExhibition = sr.func_78328_b() - currentYExhibition - heightExhibition * this.scale.getValue();
            currentYNormal = sr.func_78328_b() - currentYNormal - heightNormal * this.scale.getValue();
        }

        for (InterfaceComponent component : animatingComponents) {
            float targetY = this.hudMode.getValue() == 1 ? currentYExhibition : currentYNormal;
            if (component.position.y == 0.0) {
                component.position.y = targetY;
            }

            component.position.y = MathUtil.lerp((float)component.position.y, targetY, 0.015F * delta);
            if (component.module.isEnabled()
                && (this.modulesToShow.getValue() == 0 || !component.module.isHidden())
                && component.shouldDisplay(this)) {
                float spacingEx = heightExhibition * this.scale.getValue() * (this.posY.getValue() == 0 ? 1.0F : -1.0F);
                float spacingNorm = (heightNormal + (this.shadow.getValue() ? 1.0F : 0.0F))
                    * this.scale.getValue()
                    * (this.posY.getValue() == 0 ? 1.0F : -1.0F);
                currentYExhibition += spacingEx;
                currentYNormal += spacingNorm;
            }
        }

        if (this.chatOutline.getValue() && mc.field_71462_r instanceof GuiChat) {
            String text = ((IAccessorGuiChat)mc.field_71462_r).getInputField().func_146179_b().trim();
            if (Miau.commandManager != null && Miau.commandManager.isTypingCommand(text)) {
                RenderUtil.enableRenderState();
                RenderUtil.drawOutlineRect(
                    2.0F,
                    mc.field_71462_r.field_146295_m - 14,
                    mc.field_71462_r.field_146294_l - 2,
                    mc.field_71462_r.field_146295_m - 2,
                    1.5F,
                    0,
                    this.getColor(System.currentTimeMillis()).getRGB()
                );
                RenderUtil.disableRenderState();
            }
        }

        if (this.isEnabled() && !mc.field_71474_y.field_74330_P) {
            long l = System.currentTimeMillis();
            if (this.shaders.getValue()) {
                BlurUtils.prepareBloom();
                this.renderElements(l, delta, animatingComponents, sr, true, true);
                BlurUtils.bloomEnd(3, 2.0F);
                BlurUtils.prepareBlur();
                this.renderElements(l, delta, animatingComponents, sr, true, true);
                BlurUtils.blurEnd(2, 3.0F);
            }

            this.renderElements(l, delta, animatingComponents, sr, true, false);
            this.renderPotions(sr);
            if (this.customHotbar.getValue()) {
                this.renderCustomHotbar(sr);
            }
        }
    }

    private void renderPotions(ScaledResolution sr) {
        if (mc.field_71439_g != null) {
            Collection<PotionEffect> effects = mc.field_71439_g.func_70651_bq();
            if (!effects.isEmpty()) {
                Font font = this.getFont();
                float drawY = sr.func_78328_b() - 3;
                List<PotionEffect> sortedEffects = new ArrayList<>(effects);
                sortedEffects.sort(
                    (a, b) -> {
                        String nameA = I18n.func_135052_a(a.func_76453_d(), new Object[0]);
                        String nameB = I18n.func_135052_a(b.func_76453_d(), new Object[0]);
                        String timeA = Potion.func_76389_a(a);
                        String timeB = Potion.func_76389_a(b);
                        String textA = (this.lowerCase.getValue() ? nameA.toLowerCase() : nameA)
                            + (a.func_76458_c() > 0 ? " " + (a.func_76458_c() + 1) : "")
                            + " §7"
                            + timeA;
                        String textB = (this.lowerCase.getValue() ? nameB.toLowerCase() : nameB)
                            + (b.func_76458_c() > 0 ? " " + (b.func_76458_c() + 1) : "")
                            + " §7"
                            + timeB;
                        return Float.compare(-font.getStringWidth(textA), -font.getStringWidth(textB));
                    }
                );

                for (PotionEffect effect : sortedEffects) {
                    Potion potion = Potion.field_76425_a[effect.func_76456_a()];
                    if (potion != null) {
                        String name = I18n.func_135052_a(potion.func_76393_a(), new Object[0]);
                        if (this.lowerCase.getValue()) {
                            name = name.toLowerCase();
                        }

                        if (effect.func_76458_c() > 0) {
                            name = name + " " + (effect.func_76458_c() + 1);
                        }

                        String time = Potion.func_76389_a(effect);
                        String text = name + " §7" + time;
                        int textWidth = font.getStringWidth(text);
                        float drawX = sr.func_78326_a() - 2;
                        drawY -= font.height() + 1.5F;
                        float pX = drawX - textWidth - 14.0F - 2.0F;
                        float pY = drawY;
                        float pW = textWidth + 14 + 4;
                        float pH = font.height() + 1.5F;
                        if (this.backgroundAlpha.getValue() > 0) {
                            RoundedUtils.drawRound(
                                pX, pY, pW, pH, 4.0F, new Color(0, 0, 0, this.backgroundAlpha.getValue())
                            );
                        }

                        int effectColor = potion.func_76401_j() | 0xFF000000;
                        font.drawWithShadow(text, drawX - textWidth - 1.0F, drawY, effectColor);
                        if (potion.func_76400_d()) {
                            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
                            GlStateManager.func_179147_l();
                            mc.func_110434_K()
                                .func_110577_a(new ResourceLocation("textures/gui/container/inventory.png"));
                            int iconIndex = potion.func_76392_e();
                            Gui.func_152125_a(
                                (int)(drawX - textWidth - 14.0F),
                                (int)drawY,
                                iconIndex % 8 * 18,
                                198 + iconIndex / 8 * 18,
                                18,
                                18,
                                9,
                                9,
                                256.0F,
                                256.0F
                            );
                            GlStateManager.func_179084_k();
                        }
                    }
                }
            }
        }
    }

    private void renderCustomHotbar(ScaledResolution sr) {
        if (mc.field_71439_g != null && mc.field_71442_b != null) {
            if (mc.field_71462_r == null || mc.field_71462_r instanceof GuiChat) {
                float width = 230.0F;
                float height = 60.0F;
                float x = (sr.func_78326_a() - width) / 2.0F;
                float y = sr.func_78328_b() - height - 5.0F;
                RoundedUtils.drawRound(x, y, width, height, 6.0F, new Color(20, 20, 20, 160));
                boolean hasAbsorption = mc.field_71439_g.func_70644_a(Potion.field_76444_x);
                float absorption = mc.field_71439_g.func_110139_bj();
                float hp = mc.field_71439_g.func_110143_aJ();
                float maxHp = mc.field_71439_g.func_110138_aP();
                float hpPercent = Math.max(0.0F, Math.min(1.0F, hp / maxHp));
                float leftWidth = 100.0F;
                float hpBarY = hasAbsorption ? y + 10.0F : y + 8.0F;
                float hpBarH = hasAbsorption ? 10.0F : 14.0F;
                RoundedUtils.drawRound(x + 8.0F, hpBarY, leftWidth, hpBarH, 4.0F, new Color(40, 40, 40, 200));
                RoundedUtils.drawRound(
                    x + 8.0F, hpBarY, leftWidth * hpPercent, hpBarH, 4.0F, new Color(45, 230, 45, 255)
                );
                String hpText = String.valueOf((int)hp);
                Font font = this.getFont();
                font.drawWithShadow(
                    hpText,
                    x + 8.0F + leftWidth / 2.0F - font.getStringWidth(hpText) / 2,
                    hpBarY + hpBarH / 2.0F - font.height() / 2.0F,
                    -1
                );
                if (hasAbsorption && absorption > 0.0F) {
                    float maxAbsorption = 20.0F;
                    float absorptionPercent = Math.max(0.0F, Math.min(1.0F, absorption / maxAbsorption));
                    RoundedUtils.drawRound(x + 8.0F, y + 3.0F, leftWidth, 5.0F, 2.0F, new Color(40, 40, 40, 200));
                    RoundedUtils.drawRound(
                        x + 8.0F, y + 3.0F, leftWidth * absorptionPercent, 5.0F, 2.0F, new Color(255, 215, 0, 255)
                    );
                    String absText = String.valueOf((int)absorption);
                    font.drawWithShadow(
                        absText, x + 8.0F + leftWidth / 2.0F - font.getStringWidth(absText) / 2, y - 1.0F, -1
                    );
                }

                float rightX = x + width - 100.0F - 8.0F;
                int armor = mc.field_71439_g.func_70658_aO();
                float armorPercent = Math.max(0.0F, Math.min(1.0F, armor / 20.0F));
                RoundedUtils.drawRound(rightX, y + 8.0F, 100.0F, 6.0F, 2.0F, new Color(40, 40, 40, 200));
                RoundedUtils.drawRound(rightX, y + 8.0F, 100.0F * armorPercent, 6.0F, 2.0F, new Color(0, 150, 255, 255));
                int food = mc.field_71439_g.func_71024_bL().func_75116_a();
                float foodPercent = Math.max(0.0F, Math.min(1.0F, food / 20.0F));
                RoundedUtils.drawRound(rightX, y + 16.0F, 100.0F, 6.0F, 2.0F, new Color(40, 40, 40, 200));
                RoundedUtils.drawRound(rightX, y + 16.0F, 100.0F * foodPercent, 6.0F, 2.0F, new Color(255, 170, 0, 255));
                float xp = mc.field_71439_g.field_71106_cc;
                RoundedUtils.drawRound(x + 8.0F, y + 26.0F, width - 16.0F, 3.0F, 1.5F, new Color(15, 15, 15, 220));
                RoundedUtils.drawRound(
                    x + 8.0F, y + 26.0F, (width - 16.0F) * xp, 3.0F, 1.5F, new Color(0, 200, 255, 255)
                );
                float slotY = y + 33.0F;
                float slotSize = 20.0F;
                float slotSpacing = (width - 16.0F - 9.0F * slotSize) / 8.0F;
                RenderHelper.func_74520_c();

                for (int i = 0; i < 9; i++) {
                    float slotX = x + 8.0F + i * (slotSize + slotSpacing);
                    RoundedUtils.drawRound(slotX, slotY, slotSize, slotSize, 3.0F, new Color(30, 30, 30, 180));
                    if (mc.field_71439_g.field_71071_by.field_70461_c == i) {
                        RoundedUtils.drawRound(
                            slotX - 1.5F,
                            slotY - 1.5F,
                            slotSize + 3.0F,
                            slotSize + 3.0F,
                            4.0F,
                            new Color(0, 170, 255, 200)
                        );
                    }

                    ItemStack stack = mc.field_71439_g.field_71071_by.field_70462_a[i];
                    if (stack != null) {
                        mc.func_175599_af().func_180450_b(stack, (int)slotX + 2, (int)slotY + 2);
                        if (stack.field_77994_a > 1) {
                            GlStateManager.func_179094_E();
                            GlStateManager.func_179097_i();
                            GlStateManager.func_179140_f();
                            GlStateManager.func_179152_a(0.55F, 0.55F, 0.55F);
                            String countStr = String.valueOf(stack.field_77994_a);
                            float renderX = (slotX + slotSize - 2.0F) / 0.55F - mc.field_71466_p.func_78256_a(countStr);
                            float renderY = (slotY + slotSize - 2.0F) / 0.55F - mc.field_71466_p.field_78288_b;
                            mc.field_71466_p.func_175063_a(countStr, renderX, renderY, -1);
                            GlStateManager.func_179145_e();
                            GlStateManager.func_179126_j();
                            GlStateManager.func_179121_F();
                        }
                    }
                }

                RenderHelper.func_74518_a();
                GlStateManager.func_179141_d();
                GlStateManager.func_179084_k();
                GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    private void renderElements(
        long l,
        float delta,
        List<InterfaceComponent> animatingComponents,
        ScaledResolution sr,
        boolean updateState,
        boolean backgroundsOnly
    ) {
        boolean isMcFont = FontRepository.isMinecraftSelected();
        float heightExhibition = 12.0F;
        float heightNormal = 10.0F;
        if (this.hudMode.getValue() == 2 && !backgroundsOnly) {
            if (this.watermarkPlusFade.getValue()) {
                this.watermarkFade = (float)Math.min(1.0, this.watermarkFade + delta * 0.008);
            } else {
                this.watermarkFade = 1.0F;
            }

            this.renderWatermarkImage(sr);
        } else {
            this.watermarkFade = (float)Math.max(0.0, this.watermarkFade - delta * 0.008);
        }

        if (this.showWatermark.getValue() && this.hudMode.getValue() != 2) {
            String watermark = this.getExhibitionWatermark();
            if (watermark != null) {
                try {
                    float wX = 3.0F;
                    float wY = 3.0F;
                    float wW = mc.field_71466_p.func_78256_a(watermark);
                    float wH = mc.field_71466_p.field_78288_b;
                    if (this.backgroundAlpha.getValue() > 0) {
                        RoundedUtils.drawRound(
                            wX - 1.0F,
                            wY - 1.0F,
                            wW + 2.0F,
                            wH + 2.0F,
                            4.0F,
                            new Color(0, 0, 0, this.backgroundAlpha.getValue())
                        );
                    }

                    if (!backgroundsOnly) {
                        mc.field_71466_p.func_175063_a(watermark, wX, wY, this.getColor(l).getRGB());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (this.hudMode.getValue() == 0 && !backgroundsOnly) {
            float yCoord = sr.func_78328_b() - this.getFont().getFontHeight() - 2.0F;
            int hudColor = this.getColor(l).getRGB();
            int whiteColor = -1;
            float currentX = 2.0F;
            this.getFont().drawWithShadow("Version: ", currentX, yCoord, whiteColor);
            currentX += this.getFont().getStringWidth("Version: ");
            String ver = Miau.version;
            if (ver != null && ver.length() > 0) {
                String firstChar = ver.substring(0, 1);
                String restVer = ver.substring(1);
                this.getFont().drawWithShadow(firstChar, currentX, yCoord, hudColor);
                currentX += this.getFont().getStringWidth(firstChar);
                this.getFont().drawWithShadow(restVer, currentX, yCoord, whiteColor);
                currentX += this.getFont().getStringWidth(restVer);
            }

            this.getFont().drawWithShadow(" Username: ", currentX, yCoord, whiteColor);
            currentX += this.getFont().getStringWidth(" Username: ");
            this.getFont().drawWithShadow(mc.func_110432_I().func_111285_a(), currentX, yCoord, hudColor);
        }

        if (this.hudMode.getValue() == 1) {
            if (this.showCoordinates.getValue() && mc.field_71439_g != null) {
                String posX2 = String.valueOf(Math.round(mc.field_71439_g.field_70165_t));
                String posY2 = String.valueOf(Math.round(mc.field_71439_g.field_70163_u));
                String posZ2 = String.valueOf(Math.round(mc.field_71439_g.field_70161_v));
                float yCoord = sr.func_78328_b() - 10;
                float fontHeight = this.getFont().getFontHeight();
                int colour = this.getColor(l).getRGB();
                this.getFont().drawWithShadow("X: §7" + posX2, 3.0, yCoord - fontHeight * 2.0F, colour);
                this.getFont().drawWithShadow("Y: §7" + posY2, 3.0, yCoord - fontHeight, colour);
                this.getFont().drawWithShadow("Z: §7" + posZ2, 3.0, yCoord, colour);
            }

            float height = heightExhibition;
            float x = this.offsetX.getValue().intValue();
            if (this.posX.getValue() == 1) {
                x = sr.func_78326_a() - x;
            }

            GlStateManager.func_179094_E();
            GlStateManager.func_179152_a(this.scale.getValue(), this.scale.getValue(), 0.0F);

            for (InterfaceComponent component : animatingComponents) {
                Module module = component.module;
                String moduleName = this.getModuleName(module);
                String[] moduleSuffix = this.getModuleSuffix(module);
                float totalWidth = this.calculateStringWidth(moduleName, moduleSuffix)
                    - (this.shadow.getValue() ? 0 : 1);
                double animProgress = component.animationTime;
                float drawY = (float)component.position.y / this.scale.getValue();
                float baseX = x / this.scale.getValue();
                boolean shouldBeVisible = module.isEnabled()
                    && (this.modulesToShow.getValue() == 0 || !module.isHidden())
                    && component.shouldDisplay(this);
                float targetX;
                if (this.posX.getValue() == 1) {
                    targetX = baseX - totalWidth;
                    if (!shouldBeVisible) {
                        targetX += totalWidth + 20.0F;
                    }
                } else {
                    targetX = baseX;
                    if (!shouldBeVisible) {
                        targetX -= totalWidth + 20.0F;
                    }
                }

                if (component.position.x == 5000.0) {
                    component.position.x = this.posX.getValue() == 1 ? targetX + 50.0F : targetX - 50.0F;
                }

                if (updateState) {
                    component.position.x = MathUtil.lerp((float)component.position.x, targetX, 0.015F * delta);
                }

                float drawX = (float)component.position.x;
                int alpha = (int)(255.0 * animProgress);
                long finalY = (long)component.position.y;
                int color = alpha << 24 | this.getColor(l, finalY).getRGB() & 16777215;
                int bgAlphaVal = (int)(this.backgroundAlpha.getValue().intValue() * animProgress);
                if (this.backgroundAlpha.getValue() > 0) {
                    RoundedUtils.drawRound(
                        drawX - 2.0F,
                        drawY - 2.0F,
                        totalWidth + 4.0F,
                        height + 2.0F,
                        4.0F,
                        new Color(0, 0, 0, bgAlphaVal)
                    );
                }

                if (!backgroundsOnly) {
                    if (this.showBar.getValue()) {
                        RenderUtil.enableRenderState();
                        if (this.posX.getValue() == 0) {
                            RenderUtil.drawRect(drawX - 3.0F, drawY - 2.0F, drawX - 2.0F, drawY + height - 2.0F, color);
                        } else {
                            RenderUtil.drawRect(
                                drawX + totalWidth + 2.0F,
                                drawY - 2.0F,
                                drawX + totalWidth + 3.0F,
                                drawY + height - 2.0F,
                                color
                            );
                        }

                        RenderUtil.disableRenderState();
                    }

                    this.getFont().drawWithShadow(moduleName, drawX, drawY, color);
                    if (this.suffixes.getValue() && moduleSuffix.length > 0) {
                        float suffixX = drawX + this.getFont().getStringWidth(moduleName) + 2.0F;
                        int suffixColor = (int)(170.0 * animProgress) << 24 | 11184810;

                        for (String str : moduleSuffix) {
                            this.getFont().drawWithShadow(str, suffixX, drawY, suffixColor);
                            suffixX += this.getFont().getStringWidth(str) + 2.0F;
                        }
                    }
                }
            }

            GlStateManager.func_179121_F();
        } else {
            float height = heightNormal;
            float x = this.offsetX.getValue().intValue()
                + (1.0F + (this.showBar.getValue() ? (this.shadow.getValue() ? 2.0F : 1.0F) : 0.0F))
                    * this.scale.getValue();
            if (this.posX.getValue() == 1) {
                x = sr.func_78326_a() - x;
            }

            GlStateManager.func_179094_E();
            GlStateManager.func_179152_a(this.scale.getValue(), this.scale.getValue(), 0.0F);

            for (InterfaceComponent component : animatingComponents) {
                Module module = component.module;
                String moduleName = this.getModuleName(module);
                String[] moduleSuffix = this.getModuleSuffix(module);
                float totalWidth = this.calculateStringWidth(moduleName, moduleSuffix)
                    - (this.shadow.getValue() ? 0 : 1);
                double animProgress = component.animationTime;
                float drawY = (float)component.position.y / this.scale.getValue();
                float baseX = x / this.scale.getValue();
                boolean shouldBeVisible = module.isEnabled()
                    && (this.modulesToShow.getValue() == 0 || !module.isHidden())
                    && component.shouldDisplay(this);
                float targetX;
                if (this.posX.getValue() == 1) {
                    targetX = baseX - totalWidth;
                    if (!shouldBeVisible) {
                        targetX += totalWidth + 20.0F;
                    }
                } else {
                    targetX = baseX;
                    if (!shouldBeVisible) {
                        targetX -= totalWidth + 20.0F;
                    }
                }

                if (component.position.x == 5000.0) {
                    component.position.x = this.posX.getValue() == 1 ? targetX + 50.0F : targetX - 50.0F;
                }

                if (updateState) {
                    component.position.x = MathUtil.lerp((float)component.position.x, targetX, 0.015F * delta);
                }

                float drawX = (float)component.position.x;
                int alpha = (int)(255.0 * animProgress);
                long finalY = (long)component.position.y;
                int color = alpha << 24 | this.getColor(l, finalY).getRGB() & 16777215;
                int bgAlphaVal = (int)(this.backgroundAlpha.getValue().intValue() * animProgress);
                if (this.backgroundAlpha.getValue() > 0) {
                    RoundedUtils.drawRound(
                        drawX - 1.0F,
                        drawY - 1.0F,
                        totalWidth + 2.0F,
                        height + 2.0F,
                        4.0F,
                        new Color(0, 0, 0, bgAlphaVal)
                    );
                }

                if (!backgroundsOnly) {
                    if (this.showBar.getValue()) {
                        RenderUtil.enableRenderState();
                        if (this.shadow.getValue()) {
                            RenderUtil.drawRect(
                                drawX + (this.posX.getValue() == 0 ? -3.0F : totalWidth + 1.0F),
                                drawY - (this.posY.getValue() == 0 ? (finalY == 0L ? 1.0F : 0.0F) : 1.0F),
                                drawX + (this.posX.getValue() == 0 ? -2.0F : totalWidth + 2.0F),
                                drawY + height + (this.posY.getValue() == 0 ? 1.0F : (finalY == 0L ? 1.0F : 0.0F)),
                                color
                            );
                        } else {
                            RenderUtil.drawRect(
                                drawX + (this.posX.getValue() == 0 ? -2.0F : totalWidth + 1.0F),
                                drawY - (this.posY.getValue() == 0 ? (finalY == 0L ? 1.0F : 0.0F) : 0.0F),
                                drawX + (this.posX.getValue() == 0 ? -1.0F : totalWidth + 2.0F),
                                drawY + height + (this.posY.getValue() == 0 ? 0.0F : (finalY == 0L ? 1.0F : 0.0F)),
                                color
                            );
                        }

                        RenderUtil.disableRenderState();
                    }

                    GlStateManager.func_179097_i();
                    if (this.shadow.getValue()) {
                        this.getFont().drawWithShadow(moduleName, drawX, drawY, color);
                    } else {
                        this.getFont()
                            .draw(moduleName, drawX, drawY + (this.posY.getValue() == 1 ? 1.0F : 0.0F), color, false);
                    }

                    if (this.suffixes.getValue() && moduleSuffix.length > 0) {
                        float width = this.getFont().getStringWidth(moduleName) + 3.0F;
                        int suffixColor = (int)(160.0 * animProgress) << 24 | 11184810;

                        for (String string : moduleSuffix) {
                            if (this.shadow.getValue()) {
                                this.getFont().drawWithShadow(string, drawX + width, drawY, suffixColor);
                            } else {
                                this.getFont()
                                    .draw(
                                        string,
                                        drawX + width,
                                        drawY + (this.posY.getValue() == 1 ? 1.0F : 0.0F),
                                        suffixColor,
                                        false
                                    );
                            }

                            width += this.getFont().getStringWidth(string) + (this.shadow.getValue() ? 3.0F : 2.0F);
                        }
                    }
                }
            }

            if (this.blinkTimer.getValue() && !backgroundsOnly) {
                BlinkModules blinkingModule = Miau.blinkManager.getBlinkingModule();
                if (blinkingModule != BlinkModules.NONE && blinkingModule != BlinkModules.AUTO_BLOCK) {
                    long movementPacketSize = Miau.blinkManager.countMovement();
                    if (movementPacketSize > 0L) {
                        GlStateManager.func_179147_l();
                        GlStateManager.func_179112_b(770, 771);
                        this.getFont()
                            .draw(
                                String.valueOf(movementPacketSize),
                                sr.func_78326_a() / 2.0F / this.scale.getValue()
                                    - this.getFont().getStringWidth(String.valueOf(movementPacketSize)) / 2.0F,
                                sr.func_78328_b() / 5.0F * 3.0F / this.scale.getValue(),
                                this.getColor(l, 0L).getRGB() & 16777215 | -1090519040,
                                this.shadow.getValue()
                            );
                        GlStateManager.func_179084_k();
                    }
                }
            }

            GlStateManager.func_179126_j();
            GlStateManager.func_179121_F();
        }
    }

    private void renderWatermarkImage(ScaledResolution sr) {
        float scaleFactor = this.watermarkPlusScale.getValue();
        int drawWidth = (int)(1645.0F * scaleFactor);
        int drawHeight = (int)(656.0F * scaleFactor);
        int x = (int)this.watermarkPlusX.getValue().floatValue();
        int y = (int)this.watermarkPlusY.getValue().floatValue();
        float alpha = this.watermarkPlusOpacity.getValue().intValue() / 255.0F * this.watermarkFade;
        if (this.watermarkPlusDrag.getValue() && mc.field_71462_r instanceof GuiChat) {
            this.dragWatermark(sr, drawWidth, drawHeight);
            x = (int)this.watermarkPlusX.getValue().floatValue();
            y = (int)this.watermarkPlusY.getValue().floatValue();
        }

        float tr = 1.0F;
        float tg = 1.0F;
        float tb = 1.0F;
        switch (this.watermarkPlusColorMode.getValue()) {
            case 1:
                Color theme = this.getColor(System.currentTimeMillis());
                tr = theme.getRed() / 255.0F;
                tg = theme.getGreen() / 255.0F;
                tb = theme.getBlue() / 255.0F;
                break;
            case 2:
                Color custom = new Color(this.watermarkPlusColor.getValue());
                tr = custom.getRed() / 255.0F;
                tg = custom.getGreen() / 255.0F;
                tb = custom.getBlue() / 255.0F;
                break;
            case 3:
                Color rainbow = ColorUtil.rainbow((int)(System.currentTimeMillis() / 10L));
                tr = rainbow.getRed() / 255.0F;
                tg = rainbow.getGreen() / 255.0F;
                tb = rainbow.getBlue() / 255.0F;
        }

        float shadowOffset = this.watermarkPlusShadowOffset.getValue();
        float shadowAlpha = this.watermarkPlusShadowOpacity.getValue().intValue() / 255.0F * alpha;
        switch (this.watermarkPlusStyle.getValue()) {
            case 1:
                this.drawWatermarkImage(
                    x + (int)shadowOffset, y + (int)shadowOffset, drawWidth, drawHeight, 0.0F, 0.0F, 0.0F, shadowAlpha
                );
                break;
            case 2:
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx != 0 || dy != 0) {
                            this.drawWatermarkImage(
                                x + (int)(dx * shadowOffset),
                                y + (int)(dy * shadowOffset),
                                drawWidth,
                                drawHeight,
                                0.0F,
                                0.0F,
                                0.0F,
                                shadowAlpha
                            );
                        }
                    }
                }
                break;
            case 3:
                float gap = this.watermarkPlusReflectionGap.getValue();
                float reflectionAlpha = this.watermarkPlusReflectionOpacity.getValue().intValue() / 255.0F * alpha;
                this.drawWatermarkImageFlipped(
                    x, y + drawHeight + (int)gap, drawWidth, drawHeight, tr, tg, tb, reflectionAlpha
                );
        }

        this.drawWatermarkImage(x, y, drawWidth, drawHeight, tr, tg, tb, alpha);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.func_179084_k();
    }

    private void drawWatermarkImage(int x, int y, int width, int height, float r, float g, float b, float a) {
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        GlStateManager.func_179131_c(r, g, b, a);
        mc.func_110434_K().func_110577_a(WATERMARK_IMAGE);
        Gui.func_146110_a(x, y, 0.0F, 0.0F, width, height, width, height);
    }

    private void drawWatermarkImageFlipped(int x, int y, int width, int height, float r, float g, float b, float a) {
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b(x, y + height, 0.0F);
        GlStateManager.func_179152_a(1.0F, -1.0F, 1.0F);
        this.drawWatermarkImage(0, 0, width, height, r, g, b, a);
        GlStateManager.func_179121_F();
    }

    private void dragWatermark(ScaledResolution sr, int width, int height) {
        float sf = sr.func_78325_e();
        int x = (int)this.watermarkPlusX.getValue().floatValue();
        int y = (int)this.watermarkPlusY.getValue().floatValue();
        if (Mouse.isButtonDown(0) && this.watermarkFirstClick) {
            this.firstWX = (int)(Mouse.getX() / sf);
            this.firstWY = (int)((sr.func_78328_b() * 2 - Mouse.getY()) / sf);
            this.watermarkFirstClick = false;
            if (this.firstWX >= x && this.firstWX <= x + width && this.firstWY >= y && this.firstWY <= y + height) {
                this.watermarkTrack = true;
            }
        }

        if (!Mouse.isButtonDown(0)) {
            this.watermarkFirstClick = true;
            this.watermarkTrack = false;
        }

        if (this.watermarkTrack) {
            int mx = (int)(Mouse.getX() / sf);
            int my = (int)((sr.func_78328_b() * 2 - Mouse.getY()) / sf);
            int deltaX = mx - this.firstWX;
            int deltaY = my - this.firstWY;
            this.watermarkPlusX.setValue(this.watermarkPlusX.getValue() + deltaX);
            this.watermarkPlusY.setValue(this.watermarkPlusY.getValue() + deltaY);
            this.firstWX = mx;
            this.firstWY = my;
        }
    }

    private String toRoman(int value) {
        switch (value) {
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            default:
                return String.valueOf(value);
        }
    }

    private static class FootParticle {
        double x;
        double y;
        double z;
        double motionX;
        double motionY;
        double motionZ;
        int maxAge;
        int age;
        Color color;

        FootParticle(
            double x, double y, double z, double motionX, double motionY, double motionZ, int maxAge, Color color
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.maxAge = maxAge;
            this.age = 0;
            this.color = color;
        }

        boolean update() {
            this.x = this.x + this.motionX;
            this.y = this.y + this.motionY;
            this.z = this.z + this.motionZ;
            this.motionX *= 1.02;
            this.motionZ *= 1.02;
            this.age++;
            return this.age >= this.maxAge;
        }
    }
}
