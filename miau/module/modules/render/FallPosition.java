package miau.module.modules.render;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import miau.event.EventTarget;
import miau.event.impl.LivingUpdateEvent;
import miau.event.impl.Render3DEvent;
import miau.mixin.IAccessorEntityRenderer;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ColorProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.util.render.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

public class FallPosition extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty showTrajectory = new BooleanProperty("Show Trajectory Line", false);
    public final BooleanProperty showDownArrow = new BooleanProperty("Show Down Arrow", true);
    public final BooleanProperty showReticle = new BooleanProperty("Show Target Reticle", true);
    public final BooleanProperty showFallDamage = new BooleanProperty("Show Fall Damage", true);
    public final IntProperty simulationTicks = new IntProperty("Simulation Ticks", 100, 20, 200);
    public final FloatProperty arrowHeight = new FloatProperty("Arrow Height", 2.5F, 1.0F, 6.0F);
    public final ColorProperty landingColor = new ColorProperty("Landing Color", 16757800);
    public final ColorProperty wallColor = new ColorProperty("Wall Color", 16739990);
    public final ColorProperty lineColor = new ColorProperty("Line Color", 51455);
    public final ColorProperty alignedColor = new ColorProperty("Aligned Color", 3997500);
    public final ColorProperty offTargetColor = new ColorProperty("Off Target Color", 16727100);
    private Vec3 landingPos = null;
    private double fallDistanceAtCalc = 0.0;
    private boolean isAligned = false;
    private static final FloatBuffer MODELVIEW = GLAllocation.func_74529_h(16);
    private static final FloatBuffer PROJECTION = GLAllocation.func_74529_h(16);
    private static final IntBuffer VIEWPORT = GLAllocation.func_74527_f(16);
    private static final FloatBuffer SCREEN_COORDS = GLAllocation.func_74529_h(3);

    public FallPosition() {
        super("FallPosition", false);
    }

    @Override
    public void onEnabled() {
        this.landingPos = null;
    }

    @Override
    public void onDisabled() {
        this.landingPos = null;
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            boolean isFalling = mc.field_71439_g.field_70143_R > 0.5F
                && !mc.field_71439_g.field_70122_E
                && !mc.field_71439_g.func_70090_H()
                && !mc.field_71439_g.func_180799_ab();
            if (!isFalling) {
                this.landingPos = null;
            } else {
                double px = mc.field_71439_g.field_70165_t;
                double py = mc.field_71439_g.field_70163_u;
                double pz = mc.field_71439_g.field_70161_v;
                double vx = mc.field_71439_g.field_70159_w;
                double vy = mc.field_71439_g.field_70181_x;
                double vz = mc.field_71439_g.field_70179_y;
                int maxTicks = this.simulationTicks.getValue();
                Vec3 result = null;

                for (int t = 0; t < maxTicks; t++) {
                    vy -= 0.08;
                    px += vx;
                    py += vy;
                    pz += vz;
                    vy *= 0.98F;
                    vx *= 0.91;
                    vz *= 0.91;
                    if (py < 0.0
                        || this.isSolid(new BlockPos((int)Math.floor(px), (int)Math.floor(py), (int)Math.floor(pz)))) {
                        result = new Vec3(Math.floor(px), Math.floor(py), Math.floor(pz));
                        break;
                    }
                }

                this.landingPos = result;
                this.fallDistanceAtCalc = mc.field_71439_g.field_70163_u
                    - (result != null ? result.field_72448_b : mc.field_71439_g.field_70163_u);
                if (this.landingPos != null) {
                    this.isAligned = Math.floor(mc.field_71439_g.field_70165_t) == this.landingPos.field_72450_a
                        && Math.floor(mc.field_71439_g.field_70161_v) == this.landingPos.field_72449_c;
                } else {
                    this.isAligned = false;
                }
            }
        } else {
            this.landingPos = null;
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && this.landingPos != null && mc.field_71439_g != null && mc.field_71441_e != null) {
            int landingColor = this.landingColor.getValue();
            int wallColor = this.wallColor.getValue();
            BlockPos landingPos = new BlockPos(
                (int)this.landingPos.field_72450_a,
                (int)this.landingPos.field_72448_b,
                (int)this.landingPos.field_72449_c
            );
            RenderUtil.renderBlock(landingPos, landingColor, true, true);
            RenderUtil.renderBlock(landingPos.func_177984_a(), wallColor, true, false);
            Vec3 surfaceCenter = new Vec3(
                landingPos.func_177958_n() + 0.5, landingPos.func_177956_o() + 1.01, landingPos.func_177952_p() + 0.5
            );
            if (this.showTrajectory.getValue()) {
                int lineColor = this.lineColor.getValue();
                Vec3 eyePos = new Vec3(
                    mc.field_71439_g.field_70165_t,
                    mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(),
                    mc.field_71439_g.field_70161_v
                );
                this.drawLine3D(eyePos, surfaceCenter, 1.5F, lineColor);
            }

            if (this.showDownArrow.getValue()) {
                int arrowColor = this.isAligned ? this.alignedColor.getValue() : this.offTargetColor.getValue();
                double height = this.arrowHeight.getValue().floatValue();
                this.drawDownArrow(surfaceCenter, height, 0.35, 2.5F, arrowColor);
            }

            if (this.showReticle.getValue()) {
                int reticleColor = this.isAligned ? this.alignedColor.getValue() : this.offTargetColor.getValue();
                Vec3 playerCenter = new Vec3(
                    mc.field_71439_g.field_70165_t,
                    mc.field_71439_g.field_70163_u + 0.05,
                    mc.field_71439_g.field_70161_v
                );
                this.drawGroundCircle(playerCenter, 0.42, 28, 2.0F, reticleColor);
                this.drawGroundCircle(playerCenter, 0.28, 28, 4.0F, reticleColor);
                this.drawGroundCircle(playerCenter, 0.12, 20, 2.0F, reticleColor);
            }

            if (this.showFallDamage.getValue()) {
                double totalFall = this.fallDistanceAtCalc;
                String heightLabel = String.format("%.0f ↓", totalFall);
                String distLabel = String.format("%.0f blocks", totalFall);
                this.drawText3D(
                    heightLabel,
                    new Vec3(
                        surfaceCenter.field_72450_a, surfaceCenter.field_72448_b + 0.55, surfaceCenter.field_72449_c
                    ),
                    16746564
                );
                this.drawText3D(
                    distLabel,
                    new Vec3(
                        surfaceCenter.field_72450_a, surfaceCenter.field_72448_b + 0.3, surfaceCenter.field_72449_c
                    ),
                    16777215
                );
            }
        }
    }

    private void drawDownArrow(Vec3 tip, double height, double baseHalfWidth, float lineWidth, int color) {
        Vec3 base = new Vec3(tip.field_72450_a, tip.field_72448_b + height, tip.field_72449_c);
        Vec3 c1 = new Vec3(base.field_72450_a + baseHalfWidth, base.field_72448_b, base.field_72449_c + baseHalfWidth);
        Vec3 c2 = new Vec3(base.field_72450_a + baseHalfWidth, base.field_72448_b, base.field_72449_c - baseHalfWidth);
        Vec3 c3 = new Vec3(base.field_72450_a - baseHalfWidth, base.field_72448_b, base.field_72449_c - baseHalfWidth);
        Vec3 c4 = new Vec3(base.field_72450_a - baseHalfWidth, base.field_72448_b, base.field_72449_c + baseHalfWidth);
        this.drawLine3D(base, tip, lineWidth, color);
        this.drawLine3D(c1, tip, lineWidth, color);
        this.drawLine3D(c2, tip, lineWidth, color);
        this.drawLine3D(c3, tip, lineWidth, color);
        this.drawLine3D(c4, tip, lineWidth, color);
        this.drawLine3D(c1, c2, lineWidth, color);
        this.drawLine3D(c2, c3, lineWidth, color);
        this.drawLine3D(c3, c4, lineWidth, color);
        this.drawLine3D(c4, c1, lineWidth, color);
    }

    private void drawGroundCircle(Vec3 center, double radius, int segments, float lineWidth, int color) {
        Vec3 prev = null;

        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2) * i / segments;
            Vec3 point = new Vec3(
                center.field_72450_a + Math.cos(angle) * radius,
                center.field_72448_b,
                center.field_72449_c + Math.sin(angle) * radius
            );
            if (prev != null) {
                this.drawLine3D(prev, point, lineWidth, color);
            }

            prev = point;
        }
    }

    private void drawLine3D(Vec3 start, Vec3 end, float lineWidth, int color) {
        double x1 = start.field_72450_a - mc.func_175598_ae().field_78730_l;
        double y1 = start.field_72448_b - mc.func_175598_ae().field_78731_m;
        double z1 = start.field_72449_c - mc.func_175598_ae().field_78728_n;
        double x2 = end.field_72450_a - mc.func_175598_ae().field_78730_l;
        double y2 = end.field_72448_b - mc.func_175598_ae().field_78731_m;
        double z2 = end.field_72449_c - mc.func_175598_ae().field_78728_n;
        RenderUtil.enableRenderState();
        RenderUtil.setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        GL11.glBegin(1);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x2, y2, z2);
        GL11.glEnd();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0F);
        RenderUtil.disableRenderState();
    }

    private void drawText3D(String label, Vec3 pos, int color) {
        double[] screen = this.worldToScreen(pos, 1.0F);
        if (screen != null) {
            GlStateManager.func_179094_E();
            GlStateManager.func_179152_a(0.5F, 0.5F, 1.0F);
            mc.field_71466_p
                .func_175063_a(
                    label,
                    (float)(screen[0] / 0.5) - mc.field_71466_p.func_78256_a(label) / 2.0F / 0.5F,
                    (float)(screen[1] / 0.5),
                    color
                );
            GlStateManager.func_179121_F();
        }
    }

    private boolean isSolid(BlockPos pos) {
        if (pos != null && mc.field_71441_e != null) {
            Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
            return block != Blocks.field_150350_a
                && block != Blocks.field_150355_j
                && block != Blocks.field_150358_i
                && block != Blocks.field_150353_l
                && block != Blocks.field_150356_k
                && block != Blocks.field_150480_ab;
        } else {
            return false;
        }
    }

    private double[] worldToScreen(Vec3 pos, float partialTicks) {
        ((IAccessorEntityRenderer)mc.field_71460_t).callSetupCameraTransform(partialTicks, 0);
        GL11.glGetFloat(2982, MODELVIEW);
        GL11.glGetFloat(2983, PROJECTION);
        GL11.glGetInteger(2978, VIEWPORT);
        ((Buffer)SCREEN_COORDS).clear();
        boolean success = GLU.gluProject(
            (float)(pos.field_72450_a - mc.func_175598_ae().field_78730_l),
            (float)(pos.field_72448_b - mc.func_175598_ae().field_78731_m),
            (float)(pos.field_72449_c - mc.func_175598_ae().field_78728_n),
            MODELVIEW,
            PROJECTION,
            VIEWPORT,
            SCREEN_COORDS
        );
        mc.field_71460_t.func_78478_c();
        if (!success) {
            return null;
        }

        double scale = new ScaledResolution(mc).func_78325_e();
        double screenX = SCREEN_COORDS.get(0) / scale;
        double screenY = (mc.field_71440_d - SCREEN_COORDS.get(1)) / scale;
        double screenZ = SCREEN_COORDS.get(2);
        return !(screenZ < 0.0) && !(screenZ >= 1.0) ? new double[]{screenX, screenY} : null;
    }
}
