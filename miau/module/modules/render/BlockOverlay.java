package miau.module.modules.render;

import java.awt.Color;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.Render2DEvent;
import miau.event.impl.Render3DEvent;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import org.lwjgl.opengl.GL11;

public class BlockOverlay extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Box", "OtherBox", "Outline"});
    public final ModeProperty colorMode = new ModeProperty("color", 0, new String[]{"HUD", "Custom"});
    public final BooleanProperty depth = new BooleanProperty("depth", false);
    public final BooleanProperty info = new BooleanProperty("info", false);
    public final IntProperty thickness = new IntProperty("thickness", 2, 1, 5);
    public final IntProperty red = new IntProperty("red", 68, 0, 255, () -> this.colorMode.getValue() == 1);
    public final IntProperty green = new IntProperty("green", 117, 0, 255, () -> this.colorMode.getValue() == 1);
    public final IntProperty blue = new IntProperty("blue", 255, 0, 255, () -> this.colorMode.getValue() == 1);
    public final IntProperty alpha = new IntProperty("alpha", 100, 0, 255);

    public BlockOverlay() {
        super("BlockOverlay", false);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && mc.field_71441_e != null && mc.field_71439_g != null) {
            BlockPos pos = this.getCurrentBlock();
            if (pos != null) {
                Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
                block.func_180654_a(mc.field_71441_e, pos);
                double renderX = mc.func_175598_ae().field_78730_l;
                double renderY = mc.func_175598_ae().field_78731_m;
                double renderZ = mc.func_175598_ae().field_78728_n;
                AxisAlignedBB bb = block.func_180646_a(mc.field_71441_e, pos)
                    .func_72314_b(0.002, 0.002, 0.002)
                    .func_72317_d(-renderX, -renderY, -renderZ);
                Color color = this.getOverlayColor();
                GlStateManager.func_179094_E();
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glEnable(2848);
                GL11.glHint(3154, 4354);
                GL11.glLineWidth(this.thickness.getValue().intValue());
                GL11.glDisable(3553);
                if (!this.depth.getValue()) {
                    GL11.glDisable(2929);
                }

                GL11.glDepthMask(false);
                GlStateManager.func_179131_c(
                    color.getRed() / 255.0F,
                    color.getGreen() / 255.0F,
                    color.getBlue() / 255.0F,
                    color.getAlpha() / 255.0F
                );
                String mode = this.mode.getModeString().toLowerCase();
                if (mode.equals("box") || mode.equals("otherbox")) {
                    this.drawFilledBox(bb);
                }

                if (mode.equals("box") || mode.equals("outline")) {
                    RenderGlobal.func_181561_a(bb);
                }

                GL11.glDepthMask(true);
                if (!this.depth.getValue()) {
                    GL11.glEnable(2929);
                }

                GL11.glEnable(3553);
                GL11.glDisable(3042);
                GL11.glDisable(2848);
                GlStateManager.func_179117_G();
                GlStateManager.func_179121_F();
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled() && this.info.getValue() && mc.field_71441_e != null) {
            BlockPos pos = this.getCurrentBlock();
            if (pos != null) {
                Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
                String text = block.func_149732_F() + " §7ID: " + Block.func_149682_b(block);
                ScaledResolution sr = new ScaledResolution(mc);
                int x = sr.func_78326_a() / 2;
                int y = sr.func_78328_b() / 2 + 7;
                int width = mc.field_71466_p.func_78256_a(text);
                Gui.func_73734_a(x - 3, y - 3, x + width + 3, y + 9, -1442840576);
                mc.field_71466_p.func_175063_a(text, x, y, -1);
            }
        }
    }

    private BlockPos getCurrentBlock() {
        if (mc.field_71476_x != null && mc.field_71476_x.field_72313_a == MovingObjectType.BLOCK) {
            BlockPos pos = mc.field_71476_x.func_178782_a();
            if (pos != null && mc.field_71441_e.func_175723_af().func_177746_a(pos)) {
                Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
                return block != Blocks.field_150350_a
                        && block != Blocks.field_150355_j
                        && block != Blocks.field_150353_l
                    ? pos
                    : null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private Color getOverlayColor() {
        if (this.colorMode.getValue() == 0) {
            HUD hud = (HUD)Miau.moduleManager.modules.get(HUD.class);
            Color hudColor = hud != null ? hud.getColor(System.currentTimeMillis()) : Color.WHITE;
            return new Color(hudColor.getRed(), hudColor.getGreen(), hudColor.getBlue(), this.alpha.getValue());
        } else {
            return new Color(this.red.getValue(), this.green.getValue(), this.blue.getValue(), this.alpha.getValue());
        }
    }

    private void drawFilledBox(AxisAlignedBB bb) {
        GL11.glBegin(7);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72338_b, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72338_b, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72338_b, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72338_b, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72337_e, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72337_e, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72337_e, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72337_e, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72338_b, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72337_e, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72337_e, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72338_b, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72338_b, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72338_b, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72337_e, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72337_e, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72338_b, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72338_b, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72337_e, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72340_a, bb.field_72337_e, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72338_b, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72337_e, bb.field_72339_c);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72337_e, bb.field_72334_f);
        GL11.glVertex3d(bb.field_72336_d, bb.field_72338_b, bb.field_72334_f);
        GL11.glEnd();
    }
}
