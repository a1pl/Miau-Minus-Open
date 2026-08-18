package miau.module.modules.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import miau.event.EventTarget;
import miau.event.impl.LivingUpdateEvent;
import miau.event.impl.Render3DEvent;
import miau.mixin.IAccessorRenderManager;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.util.render.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

public class PearlESP extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty theme = new ModeProperty(
        "Theme",
        0,
        new String[]{
            "Default",
            "Rainbow",
            "Aurora",
            "Cherry",
            "Cotton Candy",
            "Flare",
            "Flower",
            "Forest",
            "Frost",
            "Gold",
            "Grayscale",
            "Inferno",
            "Royal",
            "Sandstorm",
            "Sky",
            "Vine"
        }
    );
    public final FloatProperty lineWidth = new FloatProperty("Line width", 1.5F, 0.5F, 3.0F);
    public final BooleanProperty outlineBlock = new BooleanProperty("Outline block", true);
    public final BooleanProperty shadeBlock = new BooleanProperty("Shade block", true);
    public final BooleanProperty trajectoryLine = new BooleanProperty("Trajectory line", true);
    private static final double DRAG = 0.99;
    private static final double GRAVITY = 0.03;
    private static final int MAX_PREDICTION_TICKS = 240;
    private static final int MAX_COLLISION_SUBSTEPS = 12;
    private final Map<Integer, List<Vec3>> cachedTrajectory = new HashMap<>();
    private final Map<Integer, Vec3> cachedLanding = new HashMap<>();
    private final Map<Integer, Float> pearlAlpha = new HashMap<>();
    private final Map<Integer, Vec3> predictedVelocity = new HashMap<>();
    private final Map<Integer, Vec3> lastPredictedPosition = new HashMap<>();
    private Vec3 predictResultLanding = null;
    private List<Vec3> predictResultPts = new ArrayList<>();

    public PearlESP() {
        super("PearlESP", false);
    }

    @Override
    public void onEnabled() {
        this.resetPearls();
    }

    @Override
    public void onDisabled() {
        this.resetPearls();
    }

    private void resetPearls() {
        this.cachedTrajectory.clear();
        this.cachedLanding.clear();
        this.pearlAlpha.clear();
        this.predictedVelocity.clear();
        this.lastPredictedPosition.clear();
    }

    private boolean isSolid(int x, int y, int z) {
        if (mc.field_71441_e == null) {
            return false;
        }

        if (mc.field_71441_e.func_175623_d(new BlockPos(x, y, z))) {
            return false;
        }

        Block block = mc.field_71441_e.func_180495_p(new BlockPos(x, y, z)).func_177230_c();
        return block == Blocks.field_150355_j
                || block == Blocks.field_150358_i
                || block == Blocks.field_150353_l
                || block == Blocks.field_150356_k
            ? false
            : block != Blocks.field_150329_H
                && block != Blocks.field_150398_cm
                && block != Blocks.field_150327_N
                && block != Blocks.field_150328_O
                && block != Blocks.field_150330_I
                && block != Blocks.field_150395_bd
                && block != Blocks.field_150480_ab;
    }

    private boolean collidesAt(double x, double y, double z) {
        int blockX = MathHelper.func_76128_c(x);
        int blockY = MathHelper.func_76128_c(y);
        int blockZ = MathHelper.func_76128_c(z);
        if (!this.isSolid(blockX, blockY, blockZ)) {
            return false;
        }

        AxisAlignedBB box = mc.field_71441_e
            .func_180495_p(new BlockPos(blockX, blockY, blockZ))
            .func_177230_c()
            .func_180640_a(
                mc.field_71441_e,
                new BlockPos(blockX, blockY, blockZ),
                mc.field_71441_e.func_180495_p(new BlockPos(blockX, blockY, blockZ))
            );
        if (box == null) {
            return false;
        }

        double minX = (1.0 - (box.field_72336_d - box.field_72340_a)) * 0.5;
        double minZ = (1.0 - (box.field_72334_f - box.field_72339_c)) * 0.5;
        double localX = x - blockX;
        double localY = y - blockY;
        double localZ = z - blockZ;
        return localX >= minX
            && localX <= minX + (box.field_72336_d - box.field_72340_a)
            && localY >= 0.0
            && localY <= box.field_72337_e - box.field_72338_b
            && localZ >= minZ
            && localZ <= minZ + (box.field_72334_f - box.field_72339_c);
    }

    private boolean isWaterAt(Vec3 position) {
        if (mc.field_71441_e == null) {
            return false;
        }

        Block block = mc.field_71441_e
            .func_180495_p(
                new BlockPos(
                    MathHelper.func_76128_c(position.field_72450_a),
                    MathHelper.func_76128_c(position.field_72448_b),
                    MathHelper.func_76128_c(position.field_72449_c)
                )
            )
            .func_177230_c();
        return block == Blocks.field_150355_j || block == Blocks.field_150358_i;
    }

    private Vec3 advancePearlVelocity(Vec3 velocity, Vec3 position) {
        double drag = this.isWaterAt(position) ? 0.8 : 0.99;
        return new Vec3(
            velocity.field_72450_a * drag, velocity.field_72448_b * drag - 0.03, velocity.field_72449_c * drag
        );
    }

    private void predictTrajectory(Vec3 pos, Vec3 vel) {
        List<Vec3> pts = new ArrayList<>();
        double px = pos.field_72450_a;
        double py = pos.field_72448_b;
        double pz = pos.field_72449_c;
        double vx = vel.field_72450_a;
        double vy = vel.field_72448_b;
        double vz = vel.field_72449_c;
        pts.add(new Vec3(px, py, pz));

        for (int step = 0; step < 240; step++) {
            double sx = px;
            double sy = py;
            double sz = pz;
            double nx = px + vx;
            double ny = py + vy;
            double nz = pz + vz;
            double largestAxis = Math.max(Math.abs(vx), Math.max(Math.abs(vy), Math.abs(vz)));
            int substeps = Math.max(2, Math.min(12, (int)Math.ceil(largestAxis / 0.18)));

            for (int sub = 1; sub <= substeps; sub++) {
                double t = (double)sub / substeps;
                double cx = sx + (nx - sx) * t;
                double cy = sy + (ny - sy) * t;
                double cz = sz + (nz - sz) * t;
                pts.add(new Vec3(cx, cy, cz));
                if (this.collidesAt(cx, cy, cz)) {
                    this.predictResultLanding = new Vec3(Math.floor(cx), Math.floor(cy), Math.floor(cz));
                    this.predictResultPts = pts;
                    return;
                }

                if (cy < -64.0) {
                    this.predictResultLanding = null;
                    this.predictResultPts = pts;
                    return;
                }
            }

            px = nx;
            py = ny;
            pz = nz;
            double drag = this.isWaterAt(new Vec3(px, py, pz)) ? 0.8 : 0.99;
            vx *= drag;
            vy = vy * drag - 0.03;
            vz *= drag;
            if (ny < -64.0) {
                this.predictResultLanding = null;
                this.predictResultPts = pts;
                return;
            }
        }

        this.predictResultLanding = null;
        this.predictResultPts = pts;
    }

    private int clampInt(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private int withAlpha(int color, int alpha) {
        return (alpha & 0xFF) << 24 | color & 16777215;
    }

    private int lerpColor(int c1, int c2, double t) {
        int r = this.clampInt((int)((c1 >> 16 & 0xFF) + ((c2 >> 16 & 0xFF) - (c1 >> 16 & 0xFF)) * t), 0, 255);
        int g = this.clampInt((int)((c1 >> 8 & 0xFF) + ((c2 >> 8 & 0xFF) - (c1 >> 8 & 0xFF)) * t), 0, 255);
        int b = this.clampInt((int)((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t), 0, 255);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private int getThemeColor(String name) {
        String lo = name.toLowerCase().trim();
        double ms = System.currentTimeMillis();
        if (lo.equals("rainbow")) {
            double t = ms / 420.0;
            return 0xFF000000
                | this.clampInt((int)(128.0 + 127.0 * Math.sin(t)), 0, 255) << 16
                | this.clampInt((int)(128.0 + 127.0 * Math.sin(t + 2.094)), 0, 255) << 8
                | this.clampInt((int)(128.0 + 127.0 * Math.sin(t + 4.189)), 0, 255);
        } else {
            double p = (Math.sin(ms / 1200.0) + 1.0) / 2.0;
            if (lo.equals("aurora")) {
                return this.lerpColor(-9240126, -15208271, p);
            } else if (lo.equals("cherry")) {
                return this.lerpColor(-2278039, -2051145, p);
            } else if (lo.equals("cotton candy")) {
                return this.lerpColor(-7152920, -1218376, p);
            } else if (lo.equals("flare")) {
                return this.lerpColor(-890090, -1792483, p);
            } else if (lo.equals("flower")) {
                return this.lerpColor(-3630376, -5482055, p);
            } else if (lo.equals("forest")) {
                return this.lerpColor(-14715369, -10443229, p);
            } else if (lo.equals("frost")) {
                return this.lerpColor(-2104349, -4405814, p);
            } else if (lo.equals("gold")) {
                return this.lerpColor(-1712336, -2434378, p);
            } else if (lo.equals("grayscale")) {
                return this.lerpColor(-10394776, -1578774, p);
            } else if (lo.equals("inferno")) {
                return this.lerpColor(-13303808, -4179694, p);
            } else if (lo.equals("royal")) {
                return this.lerpColor(-8011800, -14860921, p);
            } else if (lo.equals("sandstorm")) {
                return this.lerpColor(-6450327, -662604, p);
            } else if (lo.equals("sky")) {
                return this.lerpColor(-8262920, -15352621, p);
            } else {
                return lo.equals("vine") ? this.lerpColor(-14162887, -6621023, p) : -1;
            }
        }
    }

    private int themeColor(int alpha) {
        int base = this.getThemeColor(this.theme.getModeString());
        return this.withAlpha(base, alpha);
    }

    private int themeColorDim(int alpha) {
        int base = this.getThemeColor(this.theme.getModeString());
        int r = base >> 16 & 0xFF;
        int g = base >> 8 & 0xFF;
        int b = base & 0xFF;
        return this.withAlpha(0xFF000000 | (int)(r * 0.6) << 16 | (int)(g * 0.6) << 8 | (int)(b * 0.6), alpha);
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && mc.field_71441_e != null && mc.field_71439_g != null) {
            HashSet<Integer> live = new HashSet<>();
            List<Integer> remove = new ArrayList<>();

            for (Object o : mc.field_71441_e.field_72996_f) {
                Entity e = (Entity)o;
                if (e != null && !e.field_70128_L && e instanceof EntityEnderPearl) {
                    Vec3 pos = new Vec3(e.field_70165_t, e.field_70163_u, e.field_70161_v);
                    if (!(mc.field_71439_g.func_70068_e(e) > 262144.0)) {
                        int key = e.func_145782_y();
                        live.add(key);
                        Float alpha = this.pearlAlpha.get(key);
                        float alphaVal = alpha == null ? 0.0F : alpha;
                        alphaVal = Math.min(1.0F, alphaVal + 0.12F);
                        this.pearlAlpha.put(key, alphaVal);
                        if (e.field_70173_aa >= 2) {
                            Vec3 last = new Vec3(e.field_70142_S, e.field_70137_T, e.field_70136_U);
                            Vec3 observedVelocity = new Vec3(
                                pos.field_72450_a - last.field_72450_a,
                                pos.field_72448_b - last.field_72448_b,
                                pos.field_72449_c - last.field_72449_c
                            );
                            double speed = Math.sqrt(
                                observedVelocity.field_72450_a * observedVelocity.field_72450_a
                                    + observedVelocity.field_72448_b * observedVelocity.field_72448_b
                                    + observedVelocity.field_72449_c * observedVelocity.field_72449_c
                            );
                            if (!(speed < 0.01)) {
                                Vec3 previousPosition = this.lastPredictedPosition.get(key);
                                if (previousPosition == null
                                    || previousPosition.field_72450_a != pos.field_72450_a
                                    || previousPosition.field_72448_b != pos.field_72448_b
                                    || previousPosition.field_72449_c != pos.field_72449_c) {
                                    Vec3 nextVelocity = this.advancePearlVelocity(observedVelocity, pos);
                                    Vec3 previousVelocity = this.predictedVelocity.get(key);
                                    if (previousVelocity != null) {
                                        Vec3 expectedVelocity = this.advancePearlVelocity(previousVelocity, pos);
                                        double errorX = nextVelocity.field_72450_a - expectedVelocity.field_72450_a;
                                        double errorY = nextVelocity.field_72448_b - expectedVelocity.field_72448_b;
                                        double errorZ = nextVelocity.field_72449_c - expectedVelocity.field_72449_c;
                                        double errorSq = errorX * errorX + errorY * errorY + errorZ * errorZ;
                                        if (errorSq < 0.09) {
                                            nextVelocity = new Vec3(
                                                nextVelocity.field_72450_a * 0.82
                                                    + expectedVelocity.field_72450_a * 0.18,
                                                nextVelocity.field_72448_b * 0.82
                                                    + expectedVelocity.field_72448_b * 0.18,
                                                nextVelocity.field_72449_c * 0.82
                                                    + expectedVelocity.field_72449_c * 0.18
                                            );
                                        }
                                    }

                                    this.predictTrajectory(pos, nextVelocity);
                                    this.predictedVelocity.put(key, nextVelocity);
                                    this.lastPredictedPosition.put(key, pos);
                                    if (this.predictResultLanding != null) {
                                        this.cachedLanding.put(key, this.predictResultLanding);
                                    }

                                    this.cachedTrajectory.put(key, new ArrayList<>(this.predictResultPts));
                                }
                            }
                        }
                    }
                }
            }

            for (Integer id : this.pearlAlpha.keySet()) {
                if (!live.contains(id)) {
                    Float alpha = this.pearlAlpha.get(id);
                    float faded = (alpha == null ? 1.0F : alpha) - 0.14F;
                    if (faded <= 0.0F) {
                        remove.add(id);
                    } else {
                        this.pearlAlpha.put(id, faded);
                    }
                }
            }

            for (Integer id : remove) {
                this.cachedLanding.remove(id);
                this.cachedTrajectory.remove(id);
                this.pearlAlpha.remove(id);
                this.predictedVelocity.remove(id);
                this.lastPredictedPosition.remove(id);
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && mc.field_71441_e != null && mc.field_71439_g != null) {
            float lineW = this.lineWidth.getValue();

            for (Entry<Integer, List<Vec3>> entry : this.cachedTrajectory.entrySet()) {
                Vec3 landing = this.cachedLanding.get(entry.getKey());
                Float rawAlpha = this.pearlAlpha.get(entry.getKey());
                float fa = rawAlpha != null ? rawAlpha : 1.0F;
                int colBlock = this.themeColor((int)(fa * 180.0F));
                int colShade = this.themeColorDim((int)(fa * 60.0F));
                if (this.trajectoryLine.getValue()) {
                    this.drawSmoothTrajectory(entry.getValue(), lineW, fa);
                }

                if (landing != null) {
                    int bx = MathHelper.func_76128_c(landing.field_72450_a);
                    int by = MathHelper.func_76128_c(landing.field_72448_b);
                    int bz = MathHelper.func_76128_c(landing.field_72449_c);
                    double rx = bx - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX();
                    double ry = by - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY();
                    double rz = bz - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ();
                    if (this.shadeBlock.getValue()) {
                        RenderUtil.enableRenderState();
                        RenderUtil.setColor(colShade);
                        RenderUtil.drawFilledBox(
                            new AxisAlignedBB(rx, ry, rz, rx + 1.0, ry + 1.0, rz + 1.0),
                            colShade >> 16 & 0xFF,
                            colShade >> 8 & 0xFF,
                            colShade & 0xFF
                        );
                        RenderUtil.disableRenderState();
                    }

                    if (this.outlineBlock.getValue()) {
                        RenderUtil.enableRenderState();
                        RenderUtil.setColor(colBlock);
                        RenderUtil.drawBoundingBox(
                            new AxisAlignedBB(rx, ry, rz, rx + 1.0, ry + 1.0, rz + 1.0),
                            colBlock >> 16 & 0xFF,
                            colBlock >> 8 & 0xFF,
                            colBlock & 0xFF,
                            255,
                            1.5F
                        );
                        RenderUtil.disableRenderState();
                    }
                }
            }
        }
    }

    private void drawSmoothTrajectory(List<Vec3> pts, float lineWidth, float alpha) {
        if (pts != null && pts.size() >= 2) {
            int base = this.getThemeColor(this.theme.getModeString());
            int r = base >> 16 & 0xFF;
            int g = base >> 8 & 0xFF;
            int b = base & 0xFF;
            int sz = pts.size();
            RenderUtil.enableRenderState();
            RenderUtil.setColor(-1);
            GL11.glLineWidth(lineWidth);
            GL11.glEnable(2848);
            GL11.glHint(3154, 4354);
            double rx = ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX();
            double ry = ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY();
            double rz = ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ();

            for (int i = 0; i < sz - 1; i++) {
                float frac = (float)i / Math.max(1, sz - 1);
                int a = this.clampInt((int)(alpha * (255.0F - frac * 180.0F)), 0, 255);
                int color = a << 24 | r << 16 | g << 8 | b;
                RenderUtil.setColor(color);
                Vec3 p1 = pts.get(i);
                Vec3 p2 = pts.get(i + 1);
                GL11.glBegin(1);
                GL11.glVertex3d(p1.field_72450_a - rx, p1.field_72448_b - ry, p1.field_72449_c - rz);
                GL11.glVertex3d(p2.field_72450_a - rx, p2.field_72448_b - ry, p2.field_72449_c - rz);
                GL11.glEnd();
            }

            GL11.glDisable(2848);
            GL11.glLineWidth(2.0F);
            GlStateManager.func_179117_G();
            RenderUtil.disableRenderState();
        }
    }
}
