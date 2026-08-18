package miau.module.modules.render.targethud;

import java.awt.Color;
import miau.Miau;
import miau.module.modules.render.HUD;
import miau.module.modules.render.TargetHUD;
import miau.util.render.ColorUtil;
import miau.util.render.Themes;
import miau.util.shader.BlurUtils;
import miau.util.shader.RoundedUtils;
import miau.util.time.TimerUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class RavenMode extends TargetHUDMode {
    private TimerUtil fadeTimer;
    private TimerUtil healthBarTimer;
    private long lastAliveMS;
    private double lastHealth;
    private float lastHealthBar;

    public RavenMode(TargetHUD parent) {
        super(parent);
    }

    public void setFadeTimer(TimerUtil fadeTimer) {
        this.fadeTimer = fadeTimer;
    }

    public TimerUtil getFadeTimer() {
        return this.fadeTimer;
    }

    public void setLastAliveMS(long ms) {
        this.lastAliveMS = ms;
    }

    public long getLastAliveMS() {
        return this.lastAliveMS;
    }

    public void setHealthBarTimer(TimerUtil healthBarTimer) {
        this.healthBarTimer = healthBarTimer;
    }

    public double getLastHealth() {
        return this.lastHealth;
    }

    public void setLastHealth(double lastHealth) {
        this.lastHealth = lastHealth;
    }

    public void reset() {
        this.fadeTimer = null;
        this.healthBarTimer = null;
        this.lastAliveMS = 0L;
        this.lastHealth = 0.0;
        this.lastHealthBar = 0.0F;
    }

    @Override
    public void render(EntityLivingBase target, float x, float y) {
        if (target != null && !target.field_70128_L && !target.func_82150_aj()) {
            double[] screenPos = this.projectTo2D(target);
            if (screenPos != null) {
                float distanceScale = this.calculateDistanceScale(target);
                float totalScale = distanceScale * this.parent.scale.getValue();
                String playerInfo = target.func_145748_c_().func_150254_d();
                double health = target.func_110143_aJ() / target.func_110138_aP();
                if (target.field_70128_L) {
                    health = 0.0;
                }

                String healthStr = " "
                    + (
                        target.func_110143_aJ() == (int)target.func_110143_aJ()
                            ? String.valueOf((int)target.func_110143_aJ())
                            : healthFormat.format(target.func_110143_aJ())
                    );
                double healthPct = target.func_110143_aJ() / target.func_110138_aP();
                if (healthPct >= 0.5) {
                    playerInfo = playerInfo + " §a" + healthStr.trim();
                } else if (target.func_110143_aJ() >= 0.2) {
                    playerInfo = playerInfo + " §e" + healthStr.trim();
                } else {
                    playerInfo = playerInfo + " §c" + healthStr.trim();
                }

                this.drawTargetHUD(target, playerInfo, health, screenPos, totalScale);
            }
        }
    }

    private void drawTargetHUD(
        EntityLivingBase target, String string, double health, double[] screenPos, float totalScale
    ) {
        if (this.parent.showStatus.getValue()) {
            float playerTotalHealth = mc.field_71439_g.func_110143_aJ() + mc.field_71439_g.func_110139_bj();
            float playerMaxHealth = mc.field_71439_g.func_110138_aP();
            boolean shouldWin = health <= playerTotalHealth / playerMaxHealth;
            string = string + (shouldWin ? " §aW" : " §cL");
        }

        int padding = 8;
        int targetStrWithPadding = mc.field_71466_p.func_78256_a(string) + padding;
        float boxWidth = targetStrWithPadding + padding * 2;
        float boxHeight = mc.field_71466_p.field_78288_b + 12 + padding * 2;
        float absX = (float)(screenPos[0] / totalScale) - boxWidth / 2.0F;
        float absY = (float)(screenPos[1] / totalScale) + 10.0F;
        int alpha = this.fadeTimer == null
            ? 255
            : Math.max(0, 255 - (int)(this.fadeTimer.getElapsedTime() * 255L / 400L));
        if (alpha > 0) {
            int maxAlphaOutline = Math.min(alpha, 110);
            int maxAlphaBackground = Math.min(alpha, 210);
            float invSc = 1.0F / totalScale;
            GlStateManager.func_179094_E();
            if (totalScale != 1.0F) {
                GL11.glScalef(totalScale, totalScale, 1.0F);
            }

            float n6 = absX * invSc;
            float n7 = absY * invSc;
            float n8 = (absX + boxWidth) * invSc;
            float n9 = (absY + boxHeight - 13.0F) * invSc;
            HUD hud = (HUD)Miau.moduleManager.getModule(HUD.class);
            boolean blurOn = hud != null && hud.isEnabled() && hud.shaders.getValue();
            if (blurOn) {
                switch (this.parent.ravenMode.getValue()) {
                    case 0:
                        float bloomRadius = this.fadeTimer == null ? 2.0F : 2.0F * alpha / 255.0F;
                        float blurRadius = this.fadeTimer == null ? 3.0F : 3.0F * alpha / 255.0F;
                        BlurUtils.prepareBloom();
                        RoundedUtils.drawRound(
                            n6,
                            n7,
                            Math.abs(n6 - n8),
                            Math.abs(n7 - (n9 + 13.0F * invSc)),
                            8.0F,
                            true,
                            new Color(81, 99, 149, 80)
                        );
                        BlurUtils.bloomEnd(3, bloomRadius);
                        BlurUtils.prepareBlur();
                        RoundedUtils.drawRound(
                            n6,
                            n7,
                            Math.abs(n6 - n8),
                            Math.abs(n7 - (n9 + 13.0F * invSc)),
                            8.0F,
                            true,
                            new Color(this.mergeAlpha(Color.black.getRGB(), maxAlphaOutline))
                        );
                        BlurUtils.blurEnd(2, blurRadius);
                        break;
                    case 1:
                        int[] gradientColors = new int[]{
                            Themes.getCurrentTheme().getFirstColor().getRGB(),
                            Themes.getCurrentTheme().getSecondColor().getRGB()
                        };
                        this.drawRoundedGradientOutlinedRectangle(
                            n6,
                            n7,
                            n8,
                            n9 + 13.0F * invSc,
                            10.0F,
                            this.mergeAlpha(Color.black.getRGB(), maxAlphaOutline),
                            this.mergeAlpha(gradientColors[0], alpha),
                            this.mergeAlpha(gradientColors[1], alpha)
                        );
                }
            } else if (this.parent.ravenMode.getValue() == 1) {
                int[] gradientColors = new int[]{
                    Themes.getCurrentTheme().getFirstColor().getRGB(),
                    Themes.getCurrentTheme().getSecondColor().getRGB()
                };
                this.drawRoundedGradientOutlinedRectangle(
                    n6,
                    n7,
                    n8,
                    n9 + 13.0F * invSc,
                    10.0F,
                    this.mergeAlpha(Color.black.getRGB(), maxAlphaOutline),
                    this.mergeAlpha(gradientColors[0], alpha),
                    this.mergeAlpha(gradientColors[1], alpha)
                );
            }

            float n13 = n6 + 6.0F * invSc;
            float n14 = n8 - 6.0F * invSc;
            float n15 = n9;
            this.drawRoundedRectangle(
                n13, n15, n14, n15 + 5.0F * invSc, 4.0F, this.mergeAlpha(Color.black.getRGB(), maxAlphaOutline)
            );
            int mergedGradientLeft = this.mergeAlpha(
                Themes.getCurrentTheme().getFirstColor().getRGB(), maxAlphaBackground
            );
            int mergedGradientRight = this.mergeAlpha(
                Themes.getCurrentTheme().getSecondColor().getRGB(), maxAlphaBackground
            );
            float healthBar = n14 + (n13 - n14) * (float)(1.0 - health);
            boolean smoothBack = false;
            if (healthBar != this.lastHealthBar
                && Math.abs(this.lastHealthBar - n13) >= 3.0F * invSc
                && this.healthBarTimer != null) {
                float diff = this.lastHealthBar - healthBar;
                long elapsed = this.healthBarTimer.getElapsedTime();
                long duration = this.parent.ravenMode.getValue() == 0 ? 500L : 350L;
                float t = Math.min(1.0F, (float)elapsed / (float)duration);
                if (this.parent.ravenMode.getValue() == 0) {
                    t = this.quadInOut(t);
                } else {
                    t = this.easeInOutCubic(t);
                }

                if (diff > 0.0F) {
                    this.lastHealthBar -= diff * t;
                } else {
                    smoothBack = true;
                    this.lastHealthBar = this.lastHealthBar + (healthBar - this.lastHealthBar) * t;
                }
            } else {
                this.lastHealthBar = healthBar;
            }

            if (this.parent.healthColor.getValue()) {
                Color healthBlend = ColorUtil.getHealthBlend((float)health);
                mergedGradientLeft = mergedGradientRight = this.mergeAlpha(healthBlend.getRGB(), maxAlphaBackground);
            }

            if (this.lastHealthBar > n14) {
                this.lastHealthBar = n14;
            }

            switch (this.parent.ravenMode.getValue()) {
                case 0:
                    this.drawRoundedRectangle(
                        n13,
                        n15,
                        this.lastHealthBar,
                        n15 + 5.0F * invSc,
                        4.0F,
                        this.mergeAlpha(
                            ColorUtil.darker(new Color(mergedGradientRight), 0.75F).getRGB(), maxAlphaBackground
                        )
                    );
                    this.drawRoundedGradientRect(
                        n13,
                        n15,
                        smoothBack ? this.lastHealthBar : healthBar,
                        n15 + 5.0F * invSc,
                        4.0F,
                        mergedGradientLeft,
                        mergedGradientLeft,
                        mergedGradientRight,
                        mergedGradientRight
                    );
                    break;
                case 1:
                    this.drawRoundedGradientRect(
                        n13,
                        n15,
                        this.lastHealthBar,
                        n15 + 5.0F * invSc,
                        4.0F,
                        mergedGradientLeft,
                        mergedGradientLeft,
                        mergedGradientRight,
                        mergedGradientRight
                    );
            }

            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            GL13.glActiveTexture(33984);
            GlStateManager.func_179144_i(0);
            GlStateManager.func_179098_w();
            GlStateManager.func_179147_l();
            GlStateManager.func_179112_b(770, 771);
            GlStateManager.func_179092_a(516, 0.01F);
            int textColor = new Color(220, 220, 220, 255).getRGB() & 16777215
                | MathHelper.func_76125_a(alpha + 15, 0, 255) << 24;
            mc.field_71466_p
                .func_175065_a(
                    string, n6 + padding * invSc, n7 + padding * invSc, textColor, this.parent.shadow.getValue()
                );
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.func_179121_F();
        } else {
            this.healthBarTimer = null;
        }
    }

    private float quadInOut(float t) {
        return t < 0.5F ? 2.0F * t * t : -1.0F + (4.0F - 2.0F * t) * t;
    }

    private float easeInOutCubic(float t) {
        return t < 0.5F ? 4.0F * t * t * t : (t - 1.0F) * (2.0F * t - 2.0F) * (2.0F * t - 2.0F) + 1.0F;
    }

    private void drawRoundedGradientOutlinedRectangle(
        float x, float y, float x2, float y2, float radius, int fillColor, int startColor, int endColor
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
        this.glColor(fillColor);

        for (int i = 0; i <= 90; i += 3) {
            double angle = i * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(angle) * radius * -1.0, y + radius + Math.cos(angle) * radius * -1.0);
        }

        for (int i = 90; i <= 180; i += 3) {
            double angle = i * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(angle) * radius * -1.0, y2 - radius + Math.cos(angle) * radius * -1.0);
        }

        for (int i = 0; i <= 90; i += 3) {
            double angle = i * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x2 - radius + Math.sin(angle) * radius, y2 - radius + Math.cos(angle) * radius);
        }

        for (int i = 90; i <= 180; i += 3) {
            double angle = i * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x2 - radius + Math.sin(angle) * radius, y + radius + Math.cos(angle) * radius);
        }

        GL11.glEnd();
        GL11.glPushMatrix();
        GL11.glShadeModel(7425);
        GL11.glLineWidth(2.0F);
        GL11.glBegin(2);
        if (startColor != 0) {
            this.glColor(startColor);
        }

        for (int i = 0; i <= 90; i += 3) {
            double angle = i * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(angle) * radius * -1.0, y + radius + Math.cos(angle) * radius * -1.0);
        }

        for (int i = 90; i <= 180; i += 3) {
            double angle = i * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(angle) * radius * -1.0, y2 - radius + Math.cos(angle) * radius * -1.0);
        }

        if (endColor != 0) {
            this.glColor(endColor);
        }

        for (int i = 0; i <= 90; i += 3) {
            double angle = i * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x2 - radius + Math.sin(angle) * radius, y2 - radius + Math.cos(angle) * radius);
        }

        for (int i = 90; i <= 180; i += 3) {
            double angle = i * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x2 - radius + Math.sin(angle) * radius, y + radius + Math.cos(angle) * radius);
        }

        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
        GL11.glLineWidth(1.0F);
        GL11.glShadeModel(7424);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
