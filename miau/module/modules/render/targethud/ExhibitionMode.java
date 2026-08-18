package miau.module.modules.render.targethud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import miau.module.modules.render.TargetHUD;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class ExhibitionMode extends TargetHUDMode {
    public ExhibitionMode(TargetHUD parent) {
        super(parent);
    }

    @Override
    public void render(EntityLivingBase target, float x, float y) {
        if (target != null && !target.field_70128_L && !target.func_82150_aj()) {
            if (target instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer)target;
                double[] screenPos = this.projectTo2D(target);
                if (screenPos != null) {
                    float distanceScale = this.calculateDistanceScale(target);
                    float totalScale = distanceScale * this.parent.scale.getValue();
                    double skeetW = mc.field_71466_p.func_78256_a(player.func_70005_c_()) > 70.0F
                        ? 124.0F + mc.field_71466_p.func_78256_a(player.func_70005_c_()) - 70.0F
                        : 124.0;
                    float posX = (float)(screenPos[0] / totalScale) - (float)skeetW / 2.0F;
                    float posY = (float)(screenPos[1] / totalScale) + 10.0F;
                    GlStateManager.func_179094_E();
                    GlStateManager.func_179152_a(totalScale, totalScale, 1.0F);
                    GlStateManager.func_179109_b(posX, posY, 0.0F);
                    this.skeetRect(0.0, -2.0, skeetW, 38.0, 1.0);
                    this.skeetRectSmall(0.0, -2.0, 124.0, 38.0, 1.0);
                    mc.field_71466_p.func_78276_b(player.func_70005_c_(), 42, 0, -1);
                    float health = player.func_110143_aJ();
                    float healthWithAbsorption = player.func_110143_aJ() + player.func_110139_bj();
                    float progress = health / player.func_110138_aP();
                    Color healthColor = health >= 0.0F
                        ? this.blendColors(
                                new float[]{0.0F, 0.5F, 1.0F},
                                new Color[]{Color.RED, Color.YELLOW, Color.GREEN},
                                progress
                            )
                            .brighter()
                        : Color.RED;
                    double cockWidth = 50.0;
                    double healthBarPos = cockWidth * progress;
                    this.rectangle(42.5, 10.3, 103.0, 13.5, healthColor.darker().darker().darker().darker().getRGB());
                    this.rectangle(42.5, 10.3, 53.0 + healthBarPos + 0.5, 13.5, healthColor.getRGB());
                    if (player.func_110139_bj() > 0.0F) {
                        this.rectangle(
                            97.5 - player.func_110139_bj(), 10.3, 103.5, 13.5, new Color(137, 112, 9).getRGB()
                        );
                    }

                    this.rectangleBordered(42.0, 9.8F, 54.0 + cockWidth, 14.0, 0.5, 0, Color.BLACK.getRGB());

                    for (int dist = 1; dist < 10; dist++) {
                        double cock = cockWidth / 8.5 * dist;
                        this.rectangle(43.5 + cock, 9.8, 43.5 + cock + 0.5, 14.0, Color.BLACK.getRGB());
                    }

                    GlStateManager.func_179139_a(0.5, 0.5, 0.5);
                    int distance = (int)mc.field_71439_g.func_70032_d(player);
                    String nice = "HP: " + (int)healthWithAbsorption + " | Dist: " + distance;
                    mc.field_71466_p.func_175065_a(nice, 85.3F, 32.3F, -1, true);
                    GlStateManager.func_179139_a(2.0, 2.0, 2.0);
                    GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
                    GlStateManager.func_179141_d();
                    GlStateManager.func_179147_l();
                    GlStateManager.func_179120_a(770, 771, 1, 0);
                    GL11.glPushMatrix();
                    List<ItemStack> stuff = new ArrayList<>();
                    int cock = -2;

                    for (int i = 3; i >= 0; i--) {
                        ItemStack armor = player.func_82169_q(i);
                        if (armor != null) {
                            stuff.add(armor);
                        }
                    }

                    if (player.func_70694_bm() != null) {
                        stuff.add(player.func_70694_bm());
                    }

                    for (ItemStack yes : stuff) {
                        if (mc.field_71441_e != null) {
                            RenderHelper.func_74520_c();
                            cock += 16;
                        }

                        GlStateManager.func_179094_E();
                        GlStateManager.func_179118_c();
                        GlStateManager.func_179086_m(256);
                        GlStateManager.func_179147_l();
                        mc.func_175599_af().func_175042_a(yes, cock + 28, 20);
                        mc.func_175599_af().func_175030_a(mc.field_71466_p, yes, cock + 28, 20);
                        this.renderEnchantText(yes, cock + 28, 20.5F);
                        GlStateManager.func_179084_k();
                        GlStateManager.func_179139_a(0.5, 0.5, 0.5);
                        GlStateManager.func_179097_i();
                        GlStateManager.func_179140_f();
                        GlStateManager.func_179126_j();
                        GlStateManager.func_179152_a(2.0F, 2.0F, 2.0F);
                        GlStateManager.func_179141_d();
                        GlStateManager.func_179121_F();
                    }

                    GL11.glPopMatrix();
                    GlStateManager.func_179118_c();
                    GlStateManager.func_179084_k();
                    GlStateManager.func_179139_a(0.31, 0.31, 0.31);
                    GlStateManager.func_179109_b(73.0F, 102.0F, 40.0F);
                    GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
                    this.drawModel(player.field_70177_z, player.field_70125_A, player);
                    GlStateManager.func_179121_F();
                }
            }
        }
    }
}
