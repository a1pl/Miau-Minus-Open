package miau.module.modules.combat.killaura.render;

import java.awt.Color;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.Render3DEvent;
import miau.mixin.IAccessorRenderManager;
import miau.module.modules.combat.KillAura;
import miau.module.modules.render.HUD;
import miau.util.player.TeamUtil;
import miau.util.render.ColorUtil;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

public class TargetRenderer {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final KillAura killAura;
    private int ticks = 255;

    public TargetRenderer(KillAura killAura) {
        this.killAura = killAura;
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.killAura.isEnabled()
            && this.killAura.getTarget() != null
            && this.killAura.showTarget.getValue() != 0
            && TeamUtil.isEntityLoaded(this.killAura.getTarget())
            && this.killAura.isAttackAllowed()) {
            float partialTicks = event.getPartialTicks();
            EntityLivingBase player = this.killAura.getTarget();
            if (mc.func_175598_ae() == null || player == null) {
                return;
            }

            double x = player.field_70169_q
                + (player.field_70165_t - player.field_70169_q) * partialTicks
                - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX();
            double y = player.field_70167_r
                + (player.field_70163_u - player.field_70167_r) * partialTicks
                - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY();
            double z = player.field_70166_s
                + (player.field_70161_v - player.field_70166_s) * partialTicks
                - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ();
            if (this.killAura.showTarget.getValue() == 2) {
                Color color = ((HUD)Miau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
                double ringY = y + Math.sin(System.currentTimeMillis() / 200.0) + 1.0;
                GL11.glPushMatrix();
                GL11.glDisable(3553);
                GL11.glEnable(2848);
                GL11.glEnable(2832);
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glHint(3154, 4354);
                GL11.glHint(3155, 4354);
                GL11.glHint(3153, 4354);
                GL11.glDepthMask(false);
                GlStateManager.func_179092_a(516, 0.0F);
                GL11.glShadeModel(7425);
                GlStateManager.func_179129_p();
                GL11.glBegin(5);

                for (float i = 0.0F; i <= 6.53451271946677; i += 0.25132743F) {
                    double vecX = x + 0.67 * Math.cos(i);
                    double vecZ = z + 0.67 * Math.sin(i);
                    ColorUtil.glColor(ColorUtil.withAlpha(color, 63));
                    GL11.glVertex3d(vecX, ringY, vecZ);
                }

                for (float i = 0.0F; i <= 6.53451271946677; i = (float)(i + 0.25132741228718347)) {
                    double vecX = x + 0.67 * Math.cos(i);
                    double vecZ = z + 0.67 * Math.sin(i);
                    ColorUtil.glColor(ColorUtil.withAlpha(color, 63));
                    GL11.glVertex3d(vecX, ringY, vecZ);
                    ColorUtil.glColor(ColorUtil.withAlpha(color, 0));
                    GL11.glVertex3d(vecX, ringY - Math.cos(System.currentTimeMillis() / 200.0) / 2.0, vecZ);
                }

                GL11.glEnd();
                GL11.glShadeModel(7424);
                GL11.glDepthMask(true);
                GL11.glEnable(2929);
                GlStateManager.func_179092_a(516, 0.1F);
                GlStateManager.func_179089_o();
                GL11.glDisable(2848);
                GL11.glDisable(2848);
                GL11.glEnable(2832);
                GL11.glEnable(3553);
                GL11.glPopMatrix();
                GlStateManager.func_179117_G();
            } else if (this.killAura.showTarget.getValue() == 1) {
                boolean wasHurtRecently = false;
                if (player.field_70737_aN > 0) {
                    wasHurtRecently = true;
                    this.ticks = 0;
                }

                if (this.ticks <= 23) {
                    wasHurtRecently = true;
                }

                this.ticks++;
                Color color = wasHurtRecently
                    ? Color.red
                    : ((HUD)Miau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
                GL11.glPushMatrix();
                GL11.glEnable(3042);
                GL11.glLineWidth(1.8F);
                GL11.glBlendFunc(770, 771);
                GL11.glEnable(2848);
                GlStateManager.func_179132_a(true);
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glDisable(3553);
                GL11.glEnable(2848);
                GL11.glDisable(2929);
                GL11.glDepthMask(false);
                float width = player.field_70130_N / 1.15F;
                float height = player.field_70131_O + (player.func_70093_af() ? -0.2F : 0.1F);
                AxisAlignedBB aabb = new AxisAlignedBB(
                    x - width + 0.1, y, z - width + 0.1, x + width - 0.1, y + height + 0.1, z + width - 0.1
                );
                RenderUtil.drawBoundingBox(aabb, color.getRed(), color.getGreen(), color.getBlue(), 60, 1.8F);
                GL11.glDisable(2848);
                GL11.glEnable(3553);
                GL11.glEnable(2929);
                GL11.glDepthMask(true);
                GL11.glDisable(3042);
                GL11.glDisable(3042);
                GL11.glDisable(2848);
                GL11.glPopMatrix();
                GlStateManager.func_179117_G();
            }
        }
    }
}
