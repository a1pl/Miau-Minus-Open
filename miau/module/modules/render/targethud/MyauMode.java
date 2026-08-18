package miau.module.modules.render.targethud;

import java.awt.Color;
import miau.Miau;
import miau.enums.ChatColors;
import miau.module.modules.render.TargetHUD;
import miau.util.player.TeamUtil;
import miau.util.render.ColorUtil;
import miau.util.render.RenderUtil;
import miau.util.render.ShapeUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class MyauMode extends TargetHUDMode {
    private ResourceLocation headTexture = null;
    private final TimerUtil animTimer = new TimerUtil();
    private float oldHealth = 0.0F;
    private float newHealth = 0.0F;
    private float maxHealth = 1.0F;
    private EntityLivingBase lastTarget = null;

    public MyauMode(TargetHUD targetHUD) {
        super(targetHUD);
    }

    public void reset() {
        this.headTexture = null;
        this.oldHealth = 0.0F;
        this.newHealth = 0.0F;
        this.maxHealth = 1.0F;
        this.lastTarget = null;
    }

    private ResourceLocation getSkin(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            NetworkPlayerInfo playerInfo = mc.func_147114_u().func_175104_a(entityLivingBase.func_70005_c_());
            if (playerInfo != null) {
                return playerInfo.func_178837_g();
            }
        }

        return null;
    }

    private Color getTargetColor(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            if (TeamUtil.isFriend((EntityPlayer)entityLivingBase)) {
                return Miau.friendManager.getColor();
            }

            if (TeamUtil.isTarget((EntityPlayer)entityLivingBase)) {
                return Miau.targetManager.getColor();
            }
        }

        switch (this.parent.color.getValue()) {
            case 0:
                return ColorUtil.getHealthBlend(entityLivingBase.func_110143_aJ() / entityLivingBase.func_110138_aP());
            case 1:
                return new Color(0, 0, 0);
            case 2:
                return new Color(255, 255, 255);
            default:
                return Color.WHITE;
        }
    }

    @Override
    public void render(EntityLivingBase target, float x, float y) {
        if (target != null && !target.field_70128_L && !target.func_82150_aj()) {
            double[] screenPos = this.projectTo2D(target);
            if (screenPos != null) {
                float distanceScale = this.calculateDistanceScale(target);
                float totalScale = distanceScale * this.parent.scale.getValue();
                float health = (mc.field_71439_g.func_110143_aJ() + mc.field_71439_g.func_110139_bj()) / 2.0F;
                float abs = target.func_110139_bj() / 2.0F;
                float heal = target.func_110143_aJ() / 2.0F + abs;
                if (target != this.lastTarget) {
                    this.headTexture = null;
                    this.animTimer.setTime();
                    this.oldHealth = heal;
                    this.newHealth = heal;
                    this.lastTarget = target;
                }

                if (!this.parent.animations.getValue() || this.animTimer.hasTimeElapsed(150L)) {
                    this.oldHealth = this.newHealth;
                    this.newHealth = heal;
                    this.maxHealth = target.func_110138_aP() / 2.0F;
                    if (this.oldHealth != this.newHealth) {
                        this.animTimer.reset();
                    }
                }

                ResourceLocation resourceLocation = this.getSkin(target);
                if (resourceLocation != null) {
                    this.headTexture = resourceLocation;
                }

                float elapsedTime = (float)Math.min(Math.max(this.animTimer.getElapsedTime(), 0L), 150L);
                float healthRatio = Math.min(
                    Math.max(
                        RenderUtil.lerpFloat(this.newHealth, this.oldHealth, elapsedTime / 150.0F) / this.maxHealth,
                        0.0F
                    ),
                    1.0F
                );
                Color targetColor = this.getTargetColor(target);
                Color healthBarColor = this.parent.color.getValue() == 0
                    ? ColorUtil.getHealthBlend(healthRatio)
                    : targetColor;
                float healthDeltaRatio = Math.min(Math.max((health - heal + 1.0F) / 2.0F, 0.0F), 1.0F);
                Color healthDeltaColor = ColorUtil.getHealthBlend(healthDeltaRatio);
                String targetNameText = ChatColors.formatColor(String.format("&r%s&r", TeamUtil.stripName(target)));
                int targetNameWidth = mc.field_71466_p.func_78256_a(targetNameText);
                String healthText = ChatColors.formatColor(
                    String.format("&r&f%s%s❤&r", healthFormat.format(heal), abs > 0.0F ? "&6" : "&c")
                );
                int healthTextWidth = mc.field_71466_p.func_78256_a(healthText);
                String statusText = ChatColors.formatColor(
                    String.format("&r&l%s&r", heal == health ? "D" : (heal < health ? "W" : "L"))
                );
                int statusTextWidth = mc.field_71466_p.func_78256_a(statusText);
                String healthDiffText = ChatColors.formatColor(
                    String.format("&r%s&r", heal == health ? "0.0" : diffFormat.format(health - heal))
                );
                int healthDiffWidth = mc.field_71466_p.func_78256_a(healthDiffText);
                float barContentWidth = Math.max(
                    targetNameWidth + (this.parent.indicator.getValue() ? 2.0F + statusTextWidth + 2.0F : 0.0F),
                    healthTextWidth + (this.parent.indicator.getValue() ? 2.0F + healthDiffWidth + 2.0F : 0.0F)
                );
                float headIconOffset = this.parent.head.getValue() && this.headTexture != null ? 25.0F : 0.0F;
                float barTotalWidth = Math.max(headIconOffset + 70.0F, headIconOffset + 2.0F + barContentWidth + 2.0F);
                float posX = (float)(screenPos[0] / totalScale) - barTotalWidth / 2.0F;
                float posY = (float)(screenPos[1] / totalScale) + 10.0F;
                GlStateManager.func_179094_E();
                GlStateManager.func_179152_a(totalScale, totalScale, 1.0F);
                GlStateManager.func_179109_b(posX, posY, -450.0F);
                GlStateManager.func_179147_l();
                GlStateManager.func_179112_b(770, 771);
                GlStateManager.func_179118_c();
                GlStateManager.func_179097_i();
                GlStateManager.func_179090_x();
                int backgroundColor = new Color(0.0F, 0.0F, 0.0F, this.parent.background.getValue().intValue() / 100.0F)
                    .getRGB();
                int outlineColor = this.parent.outline.getValue()
                    ? targetColor.getRGB()
                    : new Color(0, 0, 0, 0).getRGB();
                ShapeUtil.drawOutlineRect(0.0F, 0.0F, barTotalWidth, 27.0F, 1.5F, backgroundColor, outlineColor);
                ShapeUtil.drawRect(
                    headIconOffset + 2.0F,
                    22.0F,
                    barTotalWidth - 2.0F,
                    25.0F,
                    ColorUtil.darker(healthBarColor, 0.2F).getRGB()
                );
                ShapeUtil.drawRect(
                    headIconOffset + 2.0F,
                    22.0F,
                    headIconOffset + 2.0F + healthRatio * (barTotalWidth - 2.0F - headIconOffset - 2.0F),
                    25.0F,
                    healthBarColor.getRGB()
                );
                GlStateManager.func_179098_w();
                GlStateManager.func_179141_d();
                GlStateManager.func_179112_b(770, 771);
                mc.field_71466_p
                    .func_175065_a(targetNameText, headIconOffset + 2.0F, 2.0F, -1, this.parent.shadow.getValue());
                mc.field_71466_p
                    .func_175065_a(healthText, headIconOffset + 2.0F, 12.0F, -1, this.parent.shadow.getValue());
                if (this.parent.indicator.getValue()) {
                    mc.field_71466_p
                        .func_175065_a(
                            statusText,
                            barTotalWidth - 2.0F - statusTextWidth,
                            2.0F,
                            healthDeltaColor.getRGB(),
                            this.parent.shadow.getValue()
                        );
                    mc.field_71466_p
                        .func_175065_a(
                            healthDiffText,
                            barTotalWidth - 2.0F - healthDiffWidth,
                            12.0F,
                            ColorUtil.darker(healthDeltaColor, 0.8F).getRGB(),
                            this.parent.shadow.getValue()
                        );
                }

                if (this.parent.head.getValue() && this.headTexture != null) {
                    GlStateManager.func_179124_c(1.0F, 1.0F, 1.0F);
                    mc.func_110434_K().func_110577_a(this.headTexture);
                    Gui.func_152125_a(2, 2, 8.0F, 8.0F, 8, 8, 23, 23, 64.0F, 64.0F);
                    Gui.func_152125_a(2, 2, 40.0F, 8.0F, 8, 8, 23, 23, 64.0F, 64.0F);
                    GlStateManager.func_179124_c(1.0F, 1.0F, 1.0F);
                }

                GlStateManager.func_179084_k();
                GlStateManager.func_179126_j();
                GlStateManager.func_179121_F();
            }
        }
    }
}
