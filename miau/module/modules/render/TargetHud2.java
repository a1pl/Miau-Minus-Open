package miau.module.modules.render;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PlayerUpdateEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.Render3DEvent;
import miau.module.Module;
import miau.module.modules.combat.KillAura;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.render.RenderUtil;
import miau.util.shader.BlurUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

public class TargetHud2 extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public static final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);
    public static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
    public static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private final String[] targetHuds = new String[]{"Astolfo", "Old Astolfo", "Very Old Astolfo"};
    private final Color cherry1 = new Color(243, 58, 106);
    private final Color cherry2 = new Color(253, 178, 185);
    private final Color cottonCandy1 = new Color(135, 215, 243);
    private final Color cottonCandy2 = new Color(254, 104, 204);
    private final Color flare1 = new Color(241, 39, 17);
    private final Color flare2 = new Color(244, 169, 24);
    private final Color flower1 = new Color(211, 91, 231);
    private final Color flower2 = new Color(214, 158, 231);
    private final Color gold1 = new Color(254, 252, 193);
    private final Color gold2 = new Color(255, 250, 53);
    private final Color greyScale1 = new Color(116, 116, 116);
    private final Color greyScale2 = new Color(186, 186, 186);
    private final Color royal1 = new Color(109, 182, 229);
    private final Color royal2 = new Color(33, 73, 166);
    private final Color sky1 = new Color(44, 220, 247);
    private final Color sky2 = new Color(139, 253, 249);
    private final Color vine1 = new Color(28, 255, 49);
    private final Color vine2 = new Color(171, 255, 172);
    private final Color[][] accents = new Color[][]{
        {null, null},
        {this.cherry1, this.cherry2},
        {this.cottonCandy1, this.cottonCandy2},
        {this.flare1, this.flare2},
        {this.flower1, this.flower2},
        {this.gold1, this.gold2},
        {this.greyScale1, this.greyScale2},
        {this.royal1, this.royal2},
        {this.sky1, this.sky2},
        {this.vine1, this.vine2}
    };
    private final float astolfoEndX = 150.0F;
    private final float astolfoEndY = 50.0F;
    private final float veryOldAstolfoEndX = 152.5F;
    private final float veryOldAstolfoEndY = 53.5F;
    private final float oldAstolfoEndX = 126.5F;
    private final float oldAstolfoEndY = 45.0F;
    private Color accent = new Color(0, 0, 0, 255);
    private float adjustedX;
    private float adjustedY;
    private int x;
    private int y;
    private int firstX;
    private int firstY;
    private int dragX;
    private int dragY;
    private boolean track = false;
    private boolean firstClick = true;
    private float timeMultiplier;
    private float offset;
    private float rangeSwing;
    private float followPlayerOffsetY;
    private float blurBackgroundRadius;
    private float borderRadius;
    private float outerBorderRadius;
    private boolean traditionHealthColor;
    private boolean followPlayer;
    private boolean showHudOnlyOnSwing;
    private boolean blurBackground;
    private boolean inChatOF;
    private int theme;
    private int borderRadiusPercent;
    private int screenFollowPlayerOffsetX;
    private int screenFollowPlayerOffsetY;
    private int backgroundBrightness;
    private int backgroundOpacity;
    private int blurBackgroundPasses;
    private int background;
    private int targetHud;
    public final ModeProperty targetHudMode = new ModeProperty("Target hud", 0, this.targetHuds);
    public final IntProperty borderRadiusPct = new IntProperty("Border radius", 25, 0, 50);
    public final IntProperty backgroundBrightnessPct = new IntProperty("Background brightness", 0, 0, 100);
    public final IntProperty backgroundOpacityValue = new IntProperty("Background opacity", 150, 0, 255);
    public final BooleanProperty blurBackgroundValue = new BooleanProperty("Blur background", true);
    public final IntProperty passesValue = new IntProperty("Passes", 5, 0, 10);
    public final IntProperty radiusValue = new IntProperty("Radius", 5, 0, 10);
    public final BooleanProperty showHudOnlyOnSwingValue = new BooleanProperty("Show hud only on swing", true);
    public final BooleanProperty followPlayerValue = new BooleanProperty("Follow player", false);
    public final IntProperty screenFollowOffsetX = new IntProperty("Screen x offset", 0, -200, 200);
    public final IntProperty screenFollowOffsetY = new IntProperty("Screen y offset", 0, -200, 200);
    public final FloatProperty followPlayerYOffset = new FloatProperty("Y offset", -0.5F, -5.0F, 5.0F);
    public final FloatProperty offsetValue = new FloatProperty("Offset", 1.0F, 0.0F, 10.0F);
    public final FloatProperty timeMultiplierValue = new FloatProperty("Time multiplier", 1.0F, 0.1F, 5.0F);
    public final ModeProperty themeProperty = new ModeProperty(
        "Theme",
        0,
        new String[]{
            "Default", "Cherry", "Cotton Candy", "Flare", "Flower", "Gold", "Grayscale", "Royal", "Sky", "Vine"
        }
    );
    public final IntProperty dragXProperty = new IntProperty("Drag X", 963, 0, 10000);
    public final IntProperty dragYProperty = new IntProperty("Drag Y", 565, 0, 10000);

    public TargetHud2() {
        super("TargetHud2", false, true);
        this.dragX = this.dragXProperty.getValue();
        this.dragY = this.dragYProperty.getValue();
        this.x = this.dragX;
        this.y = this.dragY;
        this.adjustedX = this.x / 2.0F;
        this.adjustedY = this.y / 2.0F;
    }

    @Override
    public void onEnabled() {
        this.updateComponents();
        this.updatePaint();
    }

    @EventTarget
    public void onPreUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            this.inChatOF = mc.field_71462_r instanceof GuiChat;
            int ticks = mc.field_71439_g.field_70173_aa;
            if (ticks % 5 == 0) {
                this.updateComponents();
                this.updatePaint();
            }
        }
    }

    private void updateComponents() {
        this.blurBackground = this.blurBackgroundValue.getValue();
        this.dragX = this.dragXProperty.getValue();
        this.dragY = this.dragYProperty.getValue();
        TargetHUD th = (TargetHUD)Miau.moduleManager.modules.get(TargetHUD.class);
        this.traditionHealthColor = th != null && th.healthColor.getValue();
        this.followPlayer = this.followPlayerValue.getValue();
        this.showHudOnlyOnSwing = this.showHudOnlyOnSwingValue.getValue();
        this.offset = this.offsetValue.getValue();
        this.timeMultiplier = this.timeMultiplierValue.getValue();
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        this.rangeSwing = killAura != null ? killAura.attackRange.getValue() : 3.5F;
        this.blurBackgroundRadius = this.radiusValue.getValue().intValue();
        this.blurBackgroundPasses = this.passesValue.getValue();
        this.theme = this.themeProperty.getValue();
        this.borderRadiusPercent = this.borderRadiusPct.getValue();
        this.backgroundBrightness = this.backgroundBrightnessPct.getValue();
        this.backgroundOpacity = this.backgroundOpacityValue.getValue();
        this.targetHud = this.targetHudMode.getValue();
        if (this.followPlayer) {
            this.screenFollowPlayerOffsetX = this.screenFollowOffsetX.getValue();
            this.screenFollowPlayerOffsetY = this.screenFollowOffsetY.getValue();
            this.followPlayerOffsetY = this.followPlayerYOffset.getValue();
        }
    }

    private void updatePaint() {
        switch (this.targetHud) {
            case 0:
                this.borderRadius = 7.0F * this.borderRadiusPercent / 100.0F;
                this.outerBorderRadius = this.borderRadius + 3.0F;
                break;
            case 1:
                this.borderRadius = 10.0F * this.borderRadiusPercent / 100.0F;
                this.outerBorderRadius = this.borderRadius + 5.0F;
                break;
            case 2:
                this.borderRadius = 12.0F * this.borderRadiusPercent / 100.0F;
                this.outerBorderRadius = this.borderRadius + 3.0F;
        }

        Color backgroundHSB = Color.getHSBColor(0.0F, 0.0F, this.backgroundBrightness / 100.0F);
        this.background = new Color(
                backgroundHSB.getRed(), backgroundHSB.getGreen(), backgroundHSB.getBlue(), this.backgroundOpacity
            )
            .getRGB();
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && this.followPlayer) {
            GL11.glGetFloat(2982, MODELVIEW);
            GL11.glGetFloat(2983, PROJECTION);
            GL11.glGetInteger(2978, VIEWPORT);
        }
    }

    @EventTarget
    public void onRenderTick(Render2DEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            this.inChatOF = mc.field_71462_r instanceof GuiChat;
            if (mc.field_71462_r == null || this.inChatOF) {
                float renderedHudEndX = 0.0F;
                float renderedHudEndY = 0.0F;
                EntityLivingBase entity = this.getKillAuraTarget();
                EntityLivingBase self = mc.field_71439_g;
                switch (this.targetHud) {
                    case 0:
                        renderedHudEndX = 150.0F;
                        renderedHudEndY = 50.0F;
                        break;
                    case 1:
                        renderedHudEndX = 126.5F;
                        renderedHudEndY = 45.0F;
                        break;
                    case 2:
                        renderedHudEndX = 152.5F;
                        renderedHudEndY = 53.5F;
                }

                if (!this.followPlayer && this.inChatOF) {
                    this.dragLogic((int)renderedHudEndX * 2, (int)renderedHudEndY * 2);
                } else if (!this.inChatOF) {
                    this.track = false;
                }

                if (this.inChatOF || entity != null) {
                    if (this.inChatOF) {
                        entity = self;
                    } else if (this.showHudOnlyOnSwing
                        && entity != null
                        && self.func_70032_d(entity) - 0.5 >= this.rangeSwing) {
                        return;
                    }

                    this.accent = this.traditionHealthColor
                        ? this.getHealthColor(entity)
                        : (
                            this.theme == 0
                                ? this.getRainbow(1)
                                : (
                                    this.theme > 0 && this.theme < this.accents.length
                                        ? this.blendColors(this.accents[this.theme][0], this.accents[this.theme][1], 1)
                                        : new Color(0, 0, 0, 255)
                                )
                        );
                    if (this.followPlayer) {
                        if (!RenderUtil.isInViewFrustum(entity)) {
                            return;
                        }

                        this.followPlayer(renderedHudEndX, renderedHudEndY, entity, event.getPartialTicks());
                    } else if (this.dragX != this.x || this.dragY != this.y) {
                        this.x = this.dragX;
                        this.y = this.dragY;
                        this.adjustedX = this.x / 2.0F;
                        this.adjustedY = this.y / 2.0F;
                    }

                    switch (this.targetHud) {
                        case 0:
                            this.drawAstolfo(entity);
                            break;
                        case 1:
                            this.drawOldAstolfo(entity);
                            break;
                        case 2:
                            this.drawVeryOldAstolfo(entity);
                    }
                }
            }
        }
    }

    private void drawAstolfo(EntityLivingBase entity) {
        if (this.borderRadius != 0.0F) {
            if (this.blurBackground) {
                BlurUtils.prepareBlur();
                RenderUtil.drawRoundedRectangle(
                    this.adjustedX,
                    this.adjustedY,
                    150.0F + this.adjustedX,
                    50.0F + this.adjustedY,
                    this.outerBorderRadius,
                    -1
                );
                BlurUtils.blurEnd(this.blurBackgroundPasses, this.blurBackgroundRadius);
            }

            RenderUtil.drawRoundedRectangle(
                this.adjustedX,
                this.adjustedY,
                150.0F + this.adjustedX,
                50.0F + this.adjustedY,
                this.outerBorderRadius,
                this.background
            );
            RenderUtil.drawRoundedRectangle(
                30.0F + this.adjustedX,
                40.0F + this.adjustedY,
                147.0F + this.adjustedX,
                47.0F + this.adjustedY,
                this.borderRadius,
                new Color(
                        this.clamp(this.accent.getRed() - 195, 0, 255),
                        this.clamp(this.accent.getGreen() - 195, 0, 255),
                        this.clamp(this.accent.getBlue() - 195, 0, 255),
                        255
                    )
                    .getRGB()
            );
            RenderUtil.drawRoundedRectangle(
                30.0F + this.adjustedX,
                40.0F + this.adjustedY,
                29.5F + this.adjustedX + entity.func_110143_aJ() / entity.func_110138_aP() * 117.5F,
                47.0F + this.adjustedY,
                this.borderRadius,
                new Color(
                        this.clamp(this.accent.getRed() - 77, 0, 255),
                        this.clamp(this.accent.getGreen() - 77, 0, 255),
                        this.clamp(this.accent.getBlue() - 77, 0, 255),
                        255
                    )
                    .getRGB()
            );
            RenderUtil.drawRoundedRectangle(
                30.0F + this.adjustedX,
                40.0F + this.adjustedY,
                this.clampFloat(
                    29.5F + this.adjustedX + entity.func_110143_aJ() / entity.func_110138_aP() * 117.5F - 5.0F,
                    30.0F + this.adjustedX,
                    this.adjustedX + 150.0F
                ),
                47.0F + this.adjustedY,
                this.borderRadius,
                new Color(this.accent.getRed(), this.accent.getGreen(), this.accent.getBlue(), 255).getRGB()
            );
        } else {
            if (this.blurBackground) {
                BlurUtils.prepareBlur();
                RenderUtil.drawRect(this.adjustedX, this.adjustedY, 150.0F + this.adjustedX, 50.0F + this.adjustedY, -1);
                BlurUtils.blurEnd(this.blurBackgroundPasses, this.blurBackgroundRadius);
            }

            RenderUtil.drawRect(
                this.adjustedX, this.adjustedY, 150.0F + this.adjustedX, 50.0F + this.adjustedY, this.background
            );
            RenderUtil.drawRect(
                30.0F + this.adjustedX,
                40.0F + this.adjustedY,
                147.0F + this.adjustedX,
                47.0F + this.adjustedY,
                new Color(
                        this.clamp(this.accent.getRed() - 195, 0, 255),
                        this.clamp(this.accent.getGreen() - 195, 0, 255),
                        this.clamp(this.accent.getBlue() - 195, 0, 255),
                        255
                    )
                    .getRGB()
            );
            RenderUtil.drawRect(
                30.0F + this.adjustedX,
                40.0F + this.adjustedY,
                29.5F + this.adjustedX + entity.func_110143_aJ() / entity.func_110138_aP() * 117.5F,
                47.0F + this.adjustedY,
                new Color(
                        this.clamp(this.accent.getRed() - 77, 0, 255),
                        this.clamp(this.accent.getGreen() - 77, 0, 255),
                        this.clamp(this.accent.getBlue() - 77, 0, 255),
                        255
                    )
                    .getRGB()
            );
            RenderUtil.drawRect(
                30.0F + this.adjustedX,
                40.0F + this.adjustedY,
                this.clampFloat(
                    29.5F + this.adjustedX + entity.func_110143_aJ() / entity.func_110138_aP() * 117.5F - 5.0F,
                    30.0F + this.adjustedX,
                    this.adjustedX + 150.0F
                ),
                47.0F + this.adjustedY,
                new Color(this.accent.getRed(), this.accent.getGreen(), this.accent.getBlue(), 255).getRGB()
            );
        }

        this.drawScaledText(
            this.formatDoubleStr(Math.round(10.0F * entity.func_110143_aJ() / 2.0F) / 10.0),
            30.0F + this.adjustedX,
            17.5F + this.adjustedY,
            2.0F,
            new Color(this.accent.getRed(), this.accent.getGreen(), this.accent.getBlue(), 255).getRGB(),
            true
        );
        this.drawScaledText(entity.func_70005_c_(), 30.0F + this.adjustedX, 5.0F + this.adjustedY, 1.0F, -1, true);
        this.renderEntity(entity, 15 + (int)this.adjustedX, 45 + (int)this.adjustedY, -200.0F, 0.0F, 20);
        if (this.track && this.borderRadius != 0.0F) {
            this.drawRoundedRectOutline(
                this.adjustedX,
                this.adjustedY,
                126.5F + this.adjustedX,
                45.0F + this.adjustedY,
                this.outerBorderRadius,
                1.0F,
                -1761607681
            );
        } else if (this.track) {
            this.drawRectOutline(
                this.adjustedX, this.adjustedY, 126.5F + this.adjustedX, 45.0F + this.adjustedY, 1.0F, -1761607681
            );
        }
    }

    private void drawOldAstolfo(EntityLivingBase entity) {
        if (this.borderRadius != 0.0F) {
            if (this.blurBackground) {
                BlurUtils.prepareBlur();
                RenderUtil.drawRoundedRectangle(
                    this.adjustedX,
                    this.adjustedY,
                    126.5F + this.adjustedX,
                    45.0F + this.adjustedY,
                    this.outerBorderRadius,
                    -1
                );
                BlurUtils.blurEnd(this.blurBackgroundPasses, this.blurBackgroundRadius);
            }

            RenderUtil.drawRoundedRectangle(
                this.adjustedX,
                this.adjustedY,
                126.5F + this.adjustedX,
                45.0F + this.adjustedY,
                this.outerBorderRadius,
                this.background
            );
            RenderUtil.drawRoundedRectangle(
                25.0F + this.adjustedX,
                15.0F + this.adjustedY,
                25.0F + this.adjustedX + entity.func_110143_aJ() / entity.func_110138_aP() * 192.0F / 2.0F,
                25.0F + this.adjustedY,
                this.borderRadius,
                this.accent.getRGB()
            );
        } else {
            if (this.blurBackground) {
                BlurUtils.prepareBlur();
                RenderUtil.drawRect(this.adjustedX, this.adjustedY, 126.5F + this.adjustedX, 45.0F + this.adjustedY, -1);
                BlurUtils.blurEnd(this.blurBackgroundPasses, this.blurBackgroundRadius);
            }

            RenderUtil.drawRect(
                this.adjustedX, this.adjustedY, 126.5F + this.adjustedX, 45.0F + this.adjustedY, this.background
            );
            RenderUtil.drawRect(
                25.0F + this.adjustedX,
                15.0F + this.adjustedY,
                25.0F + this.adjustedX + entity.func_110143_aJ() / entity.func_110138_aP() * 192.0F / 2.0F,
                25.0F + this.adjustedY,
                this.accent.getRGB()
            );
        }

        this.drawScaledText(
            this.formatDoubleStr(Math.round(10.0F * entity.func_110143_aJ() / 2.0F) / 10.0),
            65.0F + this.adjustedX,
            16.0F + this.adjustedY,
            1.0F,
            this.accent.getRGB(),
            true
        );
        this.drawScaledText(entity.func_70005_c_(), 25.0F + this.adjustedX, 3.0F + this.adjustedY, 1.0F, -1, true);
        this.renderEntity(entity, 13 + (int)this.adjustedX, 43 + (int)this.adjustedY, 200.0F, -entity.field_70125_A, 20);
        if (this.track && this.borderRadius != 0.0F) {
            this.drawRoundedRectOutline(
                this.adjustedX,
                this.adjustedY,
                126.5F + this.adjustedX,
                45.0F + this.adjustedY,
                this.outerBorderRadius,
                1.0F,
                -1761607681
            );
        } else if (this.track) {
            this.drawRectOutline(
                this.adjustedX, this.adjustedY, 126.5F + this.adjustedX, 45.0F + this.adjustedY, 1.0F, -1761607681
            );
        }
    }

    private void drawVeryOldAstolfo(EntityLivingBase entity) {
        if (this.borderRadius != 0.0F) {
            if (this.blurBackground) {
                BlurUtils.prepareBlur();
                RenderUtil.drawRoundedRectangle(
                    this.adjustedX,
                    this.adjustedY,
                    152.5F + this.adjustedX,
                    53.5F + this.adjustedY,
                    this.outerBorderRadius,
                    -1
                );
                BlurUtils.blurEnd(this.blurBackgroundPasses, this.blurBackgroundRadius);
            }

            RenderUtil.drawRoundedRectangle(
                this.adjustedX,
                this.adjustedY,
                152.5F + this.adjustedX,
                53.5F + this.adjustedY,
                this.outerBorderRadius,
                this.background
            );
            RenderUtil.drawRoundedRectangle(
                33.0F + this.adjustedX,
                17.5F + this.adjustedY,
                147.0F + this.adjustedX,
                29.5F + this.adjustedY,
                this.borderRadius,
                -16777216
            );
            RenderUtil.drawRoundedRectangle(
                33.0F + this.adjustedX,
                17.5F + this.adjustedY,
                33.0F + this.adjustedX + entity.func_110143_aJ() / entity.func_110138_aP() * 114.0F,
                29.5F + this.adjustedY,
                this.borderRadius,
                this.accent.getRGB()
            );
        } else {
            if (this.blurBackground) {
                BlurUtils.prepareBlur();
                RenderUtil.drawRect(this.adjustedX, this.adjustedY, 152.5F + this.adjustedX, 53.5F + this.adjustedY, -1);
                BlurUtils.blurEnd(this.blurBackgroundPasses, this.blurBackgroundRadius);
            }

            RenderUtil.drawRect(
                this.adjustedX, this.adjustedY, 152.5F + this.adjustedX, 53.5F + this.adjustedY, this.background
            );
            RenderUtil.drawRect(
                33.0F + this.adjustedX,
                17.5F + this.adjustedY,
                147.0F + this.adjustedX,
                29.5F + this.adjustedY,
                -16777216
            );
            RenderUtil.drawRect(
                33.0F + this.adjustedX,
                17.5F + this.adjustedY,
                33.0F + this.adjustedX + entity.func_110143_aJ() / entity.func_110138_aP() * 114.0F,
                29.5F + this.adjustedY,
                this.accent.getRGB()
            );
        }

        this.drawScaledText(
            this.formatDoubleStr(Math.round(10.0F * entity.func_110143_aJ() / 2.0F) / 10.0),
            86.0F + this.adjustedX,
            19.0F + this.adjustedY,
            1.0F,
            this.accent.getRGB(),
            true
        );
        this.drawScaledText(entity.func_70005_c_(), 33.0F + this.adjustedX, 6.5F + this.adjustedY, 1.0F, -1, true);
        this.drawScaledText(
            "Ping: §8" + this.getPing(entity), 33.0F + this.adjustedX, 32.5F + this.adjustedY, 1.0F, -1, true
        );
        this.renderEntity(entity, 15 + (int)this.adjustedX, 48 + (int)this.adjustedY, -200.0F, 0.0F, 20);
        if (this.track && this.borderRadius != 0.0F) {
            this.drawRoundedRectOutline(
                this.adjustedX,
                this.adjustedY,
                152.5F + this.adjustedX,
                53.5F + this.adjustedY,
                this.outerBorderRadius,
                1.0F,
                -1761607681
            );
        } else if (this.track) {
            this.drawRectOutline(
                this.adjustedX, this.adjustedY, 152.5F + this.adjustedX, 53.5F + this.adjustedY, 1.0F, -1761607681
            );
        }
    }

    private int getPing(Entity entity) {
        if (entity instanceof EntityPlayer) {
            try {
                NetworkPlayerInfo info = mc.func_147114_u().func_175102_a(entity.func_110124_au());
                if (info != null) {
                    return info.func_178853_c();
                }
            } catch (Exception var3) {
            }
        }

        return 0;
    }

    private void dragLogic(int offsetX, int offsetY) {
        ScaledResolution displaySize = new ScaledResolution(mc);
        if (Mouse.isButtonDown(0) && this.firstClick) {
            int positionX = Mouse.getX();
            int positionY = displaySize.func_78328_b() * 2 - Mouse.getY();
            this.firstX = positionX;
            this.firstY = positionY;
            this.firstClick = false;
            if (this.x <= this.firstX
                && this.firstX <= this.x + offsetX
                && this.y <= this.firstY
                && this.firstY <= this.y + offsetY) {
                this.track = true;
            }
        }

        if (!Mouse.isButtonDown(0)) {
            this.firstClick = true;
            this.track = false;
        }

        if (this.track) {
            int positionX = Mouse.getX();
            int positionY = displaySize.func_78328_b() * 2 - Mouse.getY();
            int deltaX = positionX - this.firstX;
            int deltaY = positionY - this.firstY;
            this.dragX += deltaX;
            this.dragY += deltaY;
            this.x = this.dragX;
            this.y = this.dragY;
            this.adjustedX = this.x / 2.0F;
            this.adjustedY = this.y / 2.0F;
            this.firstX += deltaX;
            this.firstY += deltaY;
            this.dragXProperty.setValue(this.dragX);
            this.dragYProperty.setValue(this.dragY);
        }
    }

    private Color getHealthColor(EntityLivingBase entity) {
        float ratio = entity.func_110143_aJ() / entity.func_110138_aP();
        if (ratio >= 0.75) {
            return new Color(3, 213, 2);
        } else if (ratio >= 0.5) {
            return new Color(212, 212, 1);
        } else {
            return ratio <= 0.25 ? new Color(229, 2, 1) : new Color(212, 167, 1);
        }
    }

    private void followPlayer(float renderedHudEndX, float renderedHudEndY, Entity entity, float partialTicks) {
        double posX = this.interpolate(entity.field_70165_t, entity.field_70142_S, partialTicks);
        double posY = this.interpolate(entity.field_70163_u, entity.field_70137_T, partialTicks);
        double posZ = this.interpolate(entity.field_70161_v, entity.field_70136_U, partialTicks);
        double heightOffset = posY
            + (!entity.func_70093_af() ? entity.field_70131_O : entity.field_70131_O - 0.25)
            + this.followPlayerOffsetY;
        Vec3 screen = this.worldToScreen(posX, heightOffset, posZ);
        if (screen != null) {
            this.adjustedX = (float)screen.field_72450_a - renderedHudEndX / 2.0F + this.screenFollowPlayerOffsetX;
            this.adjustedY = (float)screen.field_72448_b - renderedHudEndY / 4.0F + this.screenFollowPlayerOffsetY;
            this.x = (int)(this.adjustedX * 2.0F);
            this.y = (int)(this.adjustedY * 2.0F);
        }
    }

    private Vec3 worldToScreen(double x, double y, double z) {
        ScaledResolution sr = new ScaledResolution(mc);
        FloatBuffer winCoords = BufferUtils.createFloatBuffer(3);
        boolean result = GLU.gluProject((float)x, (float)y, (float)z, MODELVIEW, PROJECTION, VIEWPORT, winCoords);
        if (result) {
            float winZ = winCoords.get(2);
            if (winZ >= 0.0F && winZ <= 1.0F) {
                double screenX = winCoords.get(0) / sr.func_78325_e();
                double screenY = (VIEWPORT.get(3) - winCoords.get(1)) / sr.func_78325_e();
                return new Vec3(screenX, screenY, 0.0);
            }
        }

        return null;
    }

    private double interpolate(double current, double old, float scale) {
        return old + (current - old) * scale;
    }

    private void drawRectOutline(float x1, float y1, float x2, float y2, float width, int color) {
        RenderUtil.drawLine(x1, y1, x2, y1, width, color);
        RenderUtil.drawLine(x1, y2, x2, y2, width, color);
        RenderUtil.drawLine(x1, y1, x1, y2, width, color);
        RenderUtil.drawLine(x2, y1, x2, y2, width, color);
    }

    private void drawRoundedRectOutline(float x1, float y1, float x2, float y2, float radius, float width, int color) {
        if (x1 > x2) {
            float temp = x1;
            x1 = x2;
            x2 = temp;
        }

        if (y1 > y2) {
            float temp = y1;
            y1 = y2;
            y2 = temp;
        }

        float rectX1 = x1 + radius;
        float rectY1 = y1 + radius;
        float rectX2 = x2 - radius;
        float rectY2 = y2 - radius;
        RenderUtil.drawLine(rectX1, y1, rectX2, y1, width, color);
        RenderUtil.drawLine(rectX1, y2, rectX2, y2, width, color);
        RenderUtil.drawLine(x1, rectY1, x1, rectY2, width, color);
        RenderUtil.drawLine(x2, rectY1, x2, rectY2, width, color);
        double degree = Math.PI / 180.0;

        for (int corner = 0; corner < 4; corner++) {
            double centerX = corner < 2 ? rectX2 : rectX1;
            double centerY = corner % 3 == 0 ? rectY2 : rectY1;
            double startAngle = 90 * corner;
            double endAngle = startAngle + 90.0;
            int segments = (int)(endAngle - startAngle);

            for (int i = 0; i < segments; i++) {
                double angle1 = (startAngle + i) * degree;
                double angle2 = (startAngle + i + 1.0) * degree;
                double xStart = centerX + Math.sin(angle1) * radius;
                double yStart = centerY + Math.cos(angle1) * radius;
                double xEnd = centerX + Math.sin(angle2) * radius;
                double yEnd = centerY + Math.cos(angle2) * radius;
                RenderUtil.drawLine((float)xStart, (float)yStart, (float)xEnd, (float)yEnd, width, color);
            }
        }
    }

    private Color getRainbow(int i) {
        float hue = (float)(
                (System.currentTimeMillis() + i * (int)(10.0F * this.offset)) % (int)(15000.0F / this.timeMultiplier)
            )
            / (15000.0F / this.timeMultiplier);
        return Color.getHSBColor(hue, 1.0F, 1.0F);
    }

    private double getWaveRatio(int i) {
        float time = (float)(
                (System.currentTimeMillis() + i * (int)(10.0F * this.offset)) % (int)(3000.0F / this.timeMultiplier)
            )
            / (3000.0F / this.timeMultiplier);
        return time <= 0.5 ? time * 2.0F : 2.0F - time * 2.0F;
    }

    private Color blendColors(Color color1, Color color2, int i) {
        double ratio = this.getWaveRatio(i);
        int r = this.clamp((int)(color1.getRed() * ratio + color2.getRed() * (1.0 - ratio)), 0, 255);
        int g = this.clamp((int)(color1.getGreen() * ratio + color2.getGreen() * (1.0 - ratio)), 0, 255);
        int b = this.clamp((int)(color1.getBlue() * ratio + color2.getBlue() * (1.0 - ratio)), 0, 255);
        return new Color(r, g, b);
    }

    private int clamp(int val, int min, int max) {
        return val < min ? min : (val > max ? max : val);
    }

    private float clampFloat(float val, float min, float max) {
        return val < min ? min : (val > max ? max : val);
    }

    private String formatDoubleStr(double val) {
        return val == (long)val ? Long.toString((long)val) : Double.toString(val);
    }

    private void drawScaledText(String text, float x, float y, float scale, int color, boolean shadow) {
        RenderUtil.scaleStart(x, y, scale);
        if (shadow) {
            mc.field_71466_p.func_175063_a(text, x, y, color);
        } else {
            mc.field_71466_p.func_175065_a(text, x, y, color, false);
        }

        RenderUtil.scaleEnd();
    }

    private EntityLivingBase getKillAuraTarget() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        return killAura != null ? killAura.getTarget() : null;
    }

    private void renderEntity(Entity entity, float x, float y, float yaw, float pitch, int zoom) {
        if (entity instanceof EntityLivingBase) {
            EntityLivingBase entityLivingBase = (EntityLivingBase)entity;
            GlStateManager.func_179117_G();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.func_179142_g();
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b(x, y, 50.0F);
            GlStateManager.func_179152_a(-zoom, zoom, zoom);
            GlStateManager.func_179114_b(180.0F, 0.0F, 0.0F, 1.0F);
            float renderYawOffset = entityLivingBase.field_70761_aq;
            float rotationYaw = entityLivingBase.field_70177_z;
            float rotationPitch = entityLivingBase.field_70125_A;
            float prevRotationYawHead = entityLivingBase.field_70758_at;
            float rotationYawHead = entityLivingBase.field_70759_as;
            GlStateManager.func_179114_b(135.0F, 0.0F, 1.0F, 0.0F);
            RenderHelper.func_74519_b();
            GlStateManager.func_179114_b(-135.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.func_179114_b((float)(-Math.atan(pitch / 40.0F) * 20.0), 1.0F, 0.0F, 0.0F);
            entityLivingBase.field_70761_aq = yaw - yaw / yaw * 0.4F;
            entityLivingBase.field_70177_z = yaw - yaw / yaw * 0.4F;
            entityLivingBase.field_70125_A = pitch;
            entityLivingBase.field_70759_as = entityLivingBase.field_70177_z;
            entityLivingBase.field_70758_at = entityLivingBase.field_70177_z;
            RenderManager renderManager = mc.func_175598_ae();
            renderManager.func_178631_a(180.0F);
            renderManager.func_178633_a(false);
            renderManager.func_147940_a(entityLivingBase, 0.0, 0.0, 0.0, 0.0F, 1.0F);
            renderManager.func_178633_a(true);
            entityLivingBase.field_70761_aq = renderYawOffset;
            entityLivingBase.field_70177_z = rotationYaw;
            entityLivingBase.field_70125_A = rotationPitch;
            entityLivingBase.field_70758_at = prevRotationYawHead;
            entityLivingBase.field_70759_as = rotationYawHead;
            GlStateManager.func_179121_F();
            RenderHelper.func_74518_a();
            GlStateManager.func_179101_C();
            GlStateManager.func_179138_g(OpenGlHelper.field_77476_b);
            GlStateManager.func_179090_x();
            GlStateManager.func_179138_g(OpenGlHelper.field_77478_a);
            GlStateManager.func_179117_G();
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.targetHudMode.getModeString()};
    }
}
