package miau.module.modules.render;

import miau.event.EventTarget;
import miau.event.impl.Render3DEvent;
import miau.event.impl.TickEvent;
import miau.mixin.IAccessorPlayerControllerMP;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ModeProperty;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class BreakProgress extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"PERCENTAGE", "SECOND", "DECIMAL"});
    public final BooleanProperty manual = new BooleanProperty("show-manual", true);
    public final BooleanProperty progressBar = new BooleanProperty("progress-bar", false);
    private double progress;
    private double animatedProgress;
    private BlockPos block;
    private String progressStr = "";

    public BreakProgress() {
        super("BreakProgress", false, false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            this.updateProgress();
            this.animatedProgress = this.animatedProgress + (this.progress - this.animatedProgress) * 0.35;
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled()
            && this.progress != 0.0
            && this.block != null
            && mc.field_71439_g != null
            && mc.field_71441_e != null) {
            double x = this.block.func_177958_n() + 0.5 - mc.func_175598_ae().field_78730_l;
            double y = this.block.func_177956_o() + 0.5 - mc.func_175598_ae().field_78731_m;
            double z = this.block.func_177952_p() + 0.5 - mc.func_175598_ae().field_78728_n;
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b((float)x, (float)y, (float)z);
            GlStateManager.func_179114_b(-mc.func_175598_ae().field_78735_i, 0.0F, 1.0F, 0.0F);
            GlStateManager.func_179114_b(mc.func_175598_ae().field_78732_j, 1.0F, 0.0F, 0.0F);
            GlStateManager.func_179152_a(-0.02266667F, -0.02266667F, -0.02266667F);
            GlStateManager.func_179132_a(false);
            GlStateManager.func_179097_i();
            int textWidth = mc.field_71466_p.func_78256_a(this.progressStr);
            mc.field_71466_p.func_175065_a(this.progressStr, -textWidth / 2.0F, -8.0F, -1, true);
            if (this.progressBar.getValue()) {
                this.drawProgressBar();
            }

            GlStateManager.func_179126_j();
            GlStateManager.func_179132_a(true);
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.func_179121_F();
        }
    }

    private void drawProgressBar() {
        int width = 42;
        int height = 4;
        int filled = (int)Math.round(width * Math.max(0.0, Math.min(1.0, this.animatedProgress)));
        Gui.func_73734_a(-width / 2, 4, width / 2, 4 + height, -1441787880);
        Gui.func_73734_a(-width / 2, 4, -width / 2 + filled, 4 + height, -2557075);
    }

    private void updateProgress() {
        if (mc.field_71439_g == null
            || mc.field_71441_e == null
            || mc.field_71439_g.field_71075_bZ.field_75098_d
            || !mc.field_71439_g.field_71075_bZ.field_75099_e) {
            this.resetVariables();
        } else if (this.manual.getValue()
            && mc.field_71476_x != null
            && mc.field_71476_x.field_72313_a == MovingObjectType.BLOCK) {
            this.progress = ((IAccessorPlayerControllerMP)mc.field_71442_b).getCurBlockDamageMP();
            if (this.progress == 0.0) {
                this.resetVariables();
            } else {
                this.block = mc.field_71476_x.func_178782_a();
                this.setProgressText();
            }
        } else {
            this.resetVariables();
        }
    }

    private void setProgressText() {
        switch (this.mode.getValue()) {
            case 0:
                this.progressStr = (int)(100.0 * this.progress) + "%";
                break;
            case 1:
                this.progressStr = this.getTimeLeftText();
                break;
            case 2:
                this.progressStr = String.format("%.2f", this.progress);
                break;
            default:
                this.progressStr = "";
        }
    }

    private String getTimeLeftText() {
        if (this.block == null) {
            return "0s";
        }

        Block targetBlock = mc.field_71441_e.func_180495_p(this.block).func_177230_c();
        float hardness = targetBlock.func_180647_a(mc.field_71439_g, mc.field_71441_e, this.block);
        if (hardness <= 0.0F) {
            return "0s";
        }

        double ticksLeft = Math.max(0.0, 1.0 - this.progress) / hardness;
        double seconds = Math.round(ticksLeft / 20.0 * 10.0) / 10.0;
        return seconds == 0.0 ? "0" : seconds + "s";
    }

    @Override
    public void onDisabled() {
        this.resetVariables();
    }

    private void resetVariables() {
        this.progress = 0.0;
        this.animatedProgress = 0.0;
        this.block = null;
        this.progressStr = "";
    }
}
