package miau.module.modules.render.targethud;

import java.awt.Color;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import miau.module.modules.render.TargetHUD;
import miau.util.animation.Animation;
import miau.util.animation.Easing;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.render.RenderUtil;
import miau.util.render.StencilUtil;
import miau.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Timer;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.glu.GLU;

public class TirumMode extends TargetHUDMode {
    private final Animation animation = new Animation(Easing.LINEAR, 180L);
    private static Field timerField = null;

    public TirumMode(TargetHUD targetHUD) {
        super(targetHUD);
    }

    @Override
    public void render(EntityLivingBase target, float defaultX, float defaultY) {
        if (target != null && !target.field_70128_L && !target.func_82150_aj()) {
            double[] screenPos = this.projectTo2D(target);
            if (screenPos != null) {
                double distance = mc.field_71439_g.func_70032_d(target);
                float distanceScale = (float)MathHelper.func_151237_a(1.0 - (distance - 3.0) * 0.06, 0.45, 1.25);
                float totalScale = distanceScale * this.parent.scale.getValue();
                float targetWidth = 155.0F;
                float height = 58.0F;
                float posX = (float)(screenPos[0] / totalScale) - targetWidth / 2.0F;
                float posY = (float)(screenPos[1] / totalScale) + 10.0F;
                Font font20 = FontRepository.getFont("inter-bold", 20.0F);
                Font font16 = FontRepository.getFont("inter-regular", 16.0F);
                GlStateManager.func_179094_E();
                GlStateManager.func_179152_a(totalScale, totalScale, 1.0F);
                Color bgColor = new Color(20, 20, 20, 143);
                RoundedUtils.drawRound(posX, posY, targetWidth, height, 6.0F, bgColor);
                if (target instanceof AbstractClientPlayer) {
                    StencilUtil.initStencilToWrite();
                    RoundedUtils.drawRound(posX + 5.0F, posY + 5.0F, 24.0F, 24.0F, 4.0F, Color.WHITE);
                    StencilUtil.readStencilBuffer(1);
                    RenderUtil.resetColor();
                    GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
                    this.renderPlayer2D(posX + 5.0F, posY + 5.0F, 24.0F, 24.0F, (AbstractClientPlayer)target);
                    StencilUtil.uninitStencilBuffer();
                    GlStateManager.func_179084_k();
                }

                String targetName = target.func_70005_c_();
                font20.draw(targetName, posX + 35.0F, posY + 5.0F, -1, true);
                float healthPercent = MathHelper.func_76131_a(
                    (target.func_110143_aJ() + target.func_110139_bj())
                        / (target.func_110138_aP() + target.func_110139_bj()),
                    0.0F,
                    1.0F
                );
                int healthBarWidth = (int)(targetWidth - 40.0F);
                int healthBarHeight = 3;
                this.animation.run(healthPercent * healthBarWidth);
                Color healthColor = this.getBlendColor(target.func_110143_aJ(), target.func_110138_aP());
                RoundedUtils.drawRound(
                    posX + 35.0F, posY + 22.0F, healthBarWidth, healthBarHeight, 1.5F, new Color(0, 0, 0, 150)
                );
                RoundedUtils.drawRound(
                    posX + 35.0F, posY + 22.0F, this.animation.getValue(), healthBarHeight, 1.5F, healthColor
                );
                String healthText = (int)target.func_110143_aJ() + " HP";
                font16.draw(healthText, posX + 35.0F, posY + 27.0F, -1, true);
                if (target instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer)target;
                    ItemStack[] items = new ItemStack[]{
                        player.func_71045_bC(),
                        player.func_82169_q(3),
                        player.func_82169_q(2),
                        player.func_82169_q(1),
                        player.func_82169_q(0)
                    };
                    int itemX = (int)posX + 5;
                    int itemY = (int)posY + 36;

                    for (ItemStack stack : items) {
                        if (stack != null) {
                            GlStateManager.func_179094_E();
                            RenderHelper.func_74520_c();
                            mc.func_175599_af().field_77023_b = -150.0F;
                            mc.func_175599_af().func_180450_b(stack, itemX, itemY);
                            mc.func_175599_af().func_175030_a(mc.field_71466_p, stack, itemX, itemY);
                            mc.func_175599_af().field_77023_b = 0.0F;
                            RenderHelper.func_74518_a();
                            GlStateManager.func_179121_F();
                            itemX += 17;
                        }
                    }
                }

                GlStateManager.func_179121_F();
            }
        }
    }

    @Override
    protected float getPartialTicks() {
        if (timerField != null) {
            try {
                return ((Timer)timerField.get(mc)).field_74281_c;
            } catch (Exception var6) {
            }
        }

        try {
            for (Field f : Minecraft.class.getDeclaredFields()) {
                if (f.getType() == Timer.class) {
                    f.setAccessible(true);
                    timerField = f;
                    return ((Timer)f.get(mc)).field_74281_c;
                }
            }
        } catch (Exception var5) {
        }

        return 1.0F;
    }

    @Override
    protected double[] projectTo2D(EntityLivingBase entity) {
        float partialTicks = this.getPartialTicks();
        double renderX = mc.func_175598_ae().field_78730_l;
        double renderY = mc.func_175598_ae().field_78731_m;
        double renderZ = mc.func_175598_ae().field_78728_n;
        double x = entity.field_70142_S + (entity.field_70165_t - entity.field_70142_S) * partialTicks - renderX;
        double y = entity.field_70137_T
            + (entity.field_70163_u - entity.field_70137_T) * partialTicks
            - renderY
            + entity.field_70131_O / 2.0;
        double z = entity.field_70136_U + (entity.field_70161_v - entity.field_70136_U) * partialTicks - renderZ;
        FloatBuffer winCoords = BufferUtils.createFloatBuffer(3);
        boolean result = GLU.gluProject(
            (float)x, (float)y, (float)z, TargetHUD.MODELVIEW, TargetHUD.PROJECTION, TargetHUD.VIEWPORT, winCoords
        );
        if (result) {
            float winZ = winCoords.get(2);
            if (winZ >= 0.0F && winZ <= 1.0F) {
                ScaledResolution sr = new ScaledResolution(mc);
                double screenX = winCoords.get(0) / sr.func_78325_e();
                double screenY = (TargetHUD.VIEWPORT.get(3) - winCoords.get(1)) / sr.func_78325_e();
                return new double[]{screenX, screenY};
            }
        }

        return null;
    }
}
