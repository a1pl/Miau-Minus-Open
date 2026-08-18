package miau.module.modules.render.targethud;

import java.awt.Color;
import miau.Miau;
import miau.module.modules.render.HUD;
import miau.module.modules.render.TargetHUD;
import miau.util.animation.Animation;
import miau.util.animation.Easing;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import miau.util.render.StencilUtil;
import miau.util.shader.RoundedUtils;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

public class IdleoMode extends TargetHUDMode {
    private final Animation animation = new Animation(Easing.LINEAR, 180L);

    public IdleoMode(TargetHUD targetHUD) {
        super(targetHUD);
    }

    @Override
    public void render(EntityLivingBase target, float x, float y) {
        if (target != null && !target.field_70128_L && !target.func_82150_aj()) {
            double[] screenPos = this.projectTo2D(target);
            if (screenPos != null) {
                float distanceScale = this.calculateDistanceScale(target);
                float totalScale = distanceScale * this.parent.scale.getValue();
                Font font26 = FontRepository.getFont("inter-bold", 26.0F);
                Font font32 = FontRepository.getFont("inter-bold", 32.0F);
                float targetWidth = Math.max(145, font26.getStringWidth(target.func_70005_c_()) + 40);
                float height = 37.0F;
                float posX = (float)(screenPos[0] / totalScale) - targetWidth / 2.0F;
                float posY = (float)(screenPos[1] / totalScale) + 10.0F;
                GlStateManager.func_179094_E();
                GlStateManager.func_179152_a(totalScale, totalScale, 1.0F);
                HUD hud = (HUD)Miau.moduleManager.modules.get(HUD.class);
                Color c1 = hud.getColor(0L);
                Color c2 = hud.getColor(100L);
                Color color = new Color(20, 18, 18, 90);
                int textColor = -1;
                RoundedUtils.drawRound(posX, posY, targetWidth, height, 4.0F, color);
                if (target instanceof AbstractClientPlayer) {
                    StencilUtil.initStencilToWrite();
                    RoundedUtils.drawRound(posX + 3.0F, posY + 3.0F, 31.0F, 31.0F, 4.0F, Color.WHITE);
                    StencilUtil.readStencilBuffer(1);
                    RenderUtil.resetColor();
                    GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
                    this.renderPlayer2D(posX + 3.0F, posY + 3.0F, 31.0F, 31.0F, (AbstractClientPlayer)target);
                    StencilUtil.uninitStencilBuffer();
                    GlStateManager.func_179084_k();
                } else {
                    font32.draw(
                        "?",
                        posX + 20.0F - font32.getStringWidth("?") / 2.0F,
                        posY + 17.0F - font32.getFontHeight() / 2.0F,
                        textColor,
                        true
                    );
                }

                font26.draw(target.func_70005_c_(), posX + 39.0F, posY + 5.0F, textColor, true);
                float healthPercent = MathHelper.func_76131_a(
                    (target.func_110143_aJ() + target.func_110139_bj())
                        / (target.func_110138_aP() + target.func_110139_bj()),
                    0.0F,
                    1.0F
                );
                float realHealthWidth = targetWidth - 44.0F;
                float realHealthHeight = 3.0F;
                this.animation.run(healthPercent * realHealthWidth);
                Color backgroundHealthColor = new Color(0, 0, 0, 110);
                float healthWidth = this.animation.getValue();
                RoundedUtils.drawRound(
                    posX + 39.0F, posY + height - 12.0F, realHealthWidth, realHealthHeight, 1.5F, backgroundHealthColor
                );
                RoundedUtils.drawGradientHorizontal(
                    posX + 39.0F, posY + height - 12.0F, healthWidth, realHealthHeight, 1.5F, c1, c2
                );
                GlStateManager.func_179121_F();
            }
        }
    }
}
