package miau.module.modules.render;

import java.awt.Color;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import miau.event.EventTarget;
import miau.event.impl.LivingUpdateEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.Render3DEvent;
import miau.mixin.IAccessorEntityRenderer;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.util.client.SoundUtil;
import miau.util.render.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

public class TimerBeacon extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty allowSelf = new BooleanProperty("Allow self", true);
    public final BooleanProperty ping = new BooleanProperty("Ping", true);
    private static final long FROZEN_TIME_MS = 4500L;
    private static final double XZ_THRESHOLD = 3.0;
    private static final double XZ_HARD_RESET_THRESHOLD = 7.0;
    private static final double SLOW_FALL_BLOCKS_PER_SECOND = 2.25;
    private final Map<Integer, Vec3> anchorPositions = new HashMap<>();
    private final Map<Integer, Long> stuckSince = new HashMap<>();
    private final Map<Integer, Boolean> alerted = new HashMap<>();
    private final Map<String, Integer> teamColours = new HashMap<>();
    private final Map<Integer, String> beaconNames = new LinkedHashMap<>();
    private final Map<Integer, Integer> beaconColours = new LinkedHashMap<>();
    private final Map<Integer, Boolean> beaconMessagesSent = new LinkedHashMap<>();
    private static final FloatBuffer MODELVIEW = GLAllocation.func_74529_h(16);
    private static final FloatBuffer PROJECTION = GLAllocation.func_74529_h(16);
    private static final IntBuffer VIEWPORT = GLAllocation.func_74527_f(16);
    private static final FloatBuffer SCREEN_COORDS = GLAllocation.func_74529_h(3);

    public TimerBeacon() {
        super("TimerBeacon", false);
        this.loadTeamColours();
    }

    @Override
    public void onEnabled() {
        this.resetAll();
    }

    @Override
    public void onDisabled() {
        this.resetAll();
    }

    private void resetAll() {
        this.anchorPositions.clear();
        this.stuckSince.clear();
        this.alerted.clear();
        this.clearAllBeacons();
    }

    private void loadTeamColours() {
        this.teamColours.put("0", new Color(0, 0, 0, 255).getRGB());
        this.teamColours.put("1", new Color(0, 0, 170, 255).getRGB());
        this.teamColours.put("2", new Color(0, 170, 0, 255).getRGB());
        this.teamColours.put("3", new Color(0, 170, 170, 255).getRGB());
        this.teamColours.put("4", new Color(170, 0, 0, 255).getRGB());
        this.teamColours.put("5", new Color(170, 0, 170, 255).getRGB());
        this.teamColours.put("6", new Color(255, 170, 0, 255).getRGB());
        this.teamColours.put("7", new Color(170, 170, 170, 255).getRGB());
        this.teamColours.put("8", new Color(85, 85, 85, 255).getRGB());
        this.teamColours.put("9", new Color(85, 85, 255, 255).getRGB());
        this.teamColours.put("a", new Color(85, 255, 85, 255).getRGB());
        this.teamColours.put("b", new Color(85, 255, 255, 255).getRGB());
        this.teamColours.put("c", new Color(255, 85, 85, 255).getRGB());
        this.teamColours.put("d", new Color(255, 85, 255, 255).getRGB());
        this.teamColours.put("e", new Color(255, 255, 85, 255).getRGB());
        this.teamColours.put("f", new Color(255, 255, 255, 255).getRGB());
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            long now = System.currentTimeMillis();
            HashSet<Integer> activeIds = new HashSet<>();
            if (this.allowSelf.getValue()) {
                this.scanEntity(mc.field_71439_g, now, activeIds);
            } else {
                this.clearSelfTimerState(mc.field_71439_g);
            }

            for (Object o : mc.field_71441_e.field_73010_i) {
                Entity entity = (Entity)o;
                if (entity != null && entity != mc.field_71439_g) {
                    this.scanEntity(entity, now, activeIds);
                }
            }

            this.pruneInactive(activeIds);
        } else {
            this.resetAll();
        }
    }

    private void scanEntity(Entity entity, long now, HashSet<Integer> activeIds) {
        if (!this.isInvalidTarget(entity)) {
            int id = entity.func_145782_y();
            activeIds.add(id);
            Vec3 pos = new Vec3(entity.field_70165_t, entity.field_70163_u, entity.field_70161_v);
            if (pos != null) {
                if (this.isInVoid(pos) && !this.isInWater(pos)) {
                    if (!this.anchorPositions.containsKey(id)) {
                        this.anchorPositions.put(id, pos);
                        this.stuckSince.put(id, now);
                        this.alerted.put(id, false);
                    } else {
                        Vec3 anchor = this.anchorPositions.get(id);
                        long since = this.stuckSince.containsKey(id) ? this.stuckSince.get(id) : now;
                        long trackedFor = now - since;
                        double xzDistSq = anchor == null
                            ? Double.MAX_VALUE
                            : this.xzDistanceSq(
                                anchor.field_72450_a, anchor.field_72449_c, pos.field_72450_a, pos.field_72449_c
                            );
                        if (anchor != null && !(xzDistSq > 49.0)) {
                            if (xzDistSq > 9.0) {
                                if (!this.isSlowVoidFall(anchor, pos, trackedFor)) {
                                    this.resetTracking(id, pos);
                                    return;
                                }

                                this.anchorPositions
                                    .put(id, new Vec3(pos.field_72450_a, anchor.field_72448_b, pos.field_72449_c));
                            }

                            if (trackedFor >= 4500L && !this.alerted.getOrDefault(id, false)) {
                                if (!this.isSlowVoidFall(this.anchorPositions.get(id), pos, trackedFor)) {
                                    this.resetTracking(id, pos);
                                    return;
                                }

                                if (this.notifyPotentialTimer(entity)) {
                                    this.alerted.put(id, true);
                                }
                            }
                        } else {
                            this.resetTracking(id, pos);
                        }
                    }
                } else {
                    this.resetTracking(id, pos);
                }
            }
        }
    }

    private boolean isInvalidTarget(Entity entity) {
        try {
            if (entity.field_70128_L) {
                return true;
            }
        } catch (Exception var5) {
        }

        if (entity instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer)entity;

            try {
                if (p.func_110143_aJ() <= 0.0F) {
                    return true;
                }
            } catch (Exception var4) {
            }
        }

        return false;
    }

    private void resetTracking(int id, Vec3 pos) {
        this.anchorPositions.put(id, pos);
        this.stuckSince.put(id, System.currentTimeMillis());
        this.alerted.put(id, false);
    }

    private void pruneInactive(HashSet<Integer> activeIds) {
        Iterator<Entry<Integer, Vec3>> it = this.anchorPositions.entrySet().iterator();

        while (it.hasNext()) {
            Entry<Integer, Vec3> entry = it.next();
            int id = entry.getKey();
            if (!activeIds.contains(id)) {
                it.remove();
                this.stuckSince.remove(id);
                this.alerted.remove(id);
            }
        }
    }

    private double xzDistanceSq(double ax, double az, double bx, double bz) {
        double dx = ax - bx;
        double dz = az - bz;
        return dx * dx + dz * dz;
    }

    private boolean isSlowVoidFall(Vec3 anchor, Vec3 pos, long elapsedMs) {
        if (anchor != null && pos != null && elapsedMs >= 750L) {
            double elapsedSeconds = Math.max(0.75, elapsedMs / 1000.0);
            double yDrop = Math.max(0.0, anchor.field_72448_b - pos.field_72448_b);
            return yDrop / elapsedSeconds <= 2.25;
        } else {
            return true;
        }
    }

    private boolean notifyPotentialTimer(Entity entity) {
        if (this.isClientSpectator()) {
            return false;
        }

        if (entity == mc.field_71439_g && !this.allowSelf.getValue()) {
            return false;
        }

        if (this.beaconNames.containsKey(entity.func_145782_y())) {
            return true;
        }

        this.activateBeaconForEntity(entity);
        return true;
    }

    private boolean isClientSpectator() {
        if (mc.field_71439_g == null) {
            return true;
        }

        try {
            if (mc.field_71439_g.field_70128_L) {
                return true;
            }
        } catch (Exception var4) {
        }

        try {
            if (mc.field_71439_g.func_110143_aJ() <= 0.0F) {
                return true;
            }
        } catch (Exception var3) {
        }

        try {
            if (mc.field_71439_g.field_71075_bZ.field_75101_c && !mc.field_71439_g.field_70122_E) {
                return true;
            }
        } catch (Exception var2) {
        }

        return false;
    }

    private boolean isFriendlyEntity(Entity entity) {
        if (entity == mc.field_71439_g) {
            return true;
        } else {
            String myPrefix = this.getOwnTeamPrefix();
            if (myPrefix != null && !myPrefix.isEmpty()) {
                String display = this.getEntityDisplayName(entity);
                return display != null && display.startsWith(myPrefix);
            } else {
                return false;
            }
        }
    }

    private String getOwnTeamPrefix() {
        if (mc.field_71439_g == null) {
            return "";
        }

        String prefix = this.getTeamPrefixFromDisplay(this.getEntityDisplayName(mc.field_71439_g));
        return prefix.isEmpty() ? "" : prefix;
    }

    private String getEntityDisplayName(Entity entity) {
        try {
            if (entity.func_145748_c_() != null) {
                return entity.func_145748_c_().func_150254_d();
            }
        } catch (Exception var3) {
        }

        return "";
    }

    private String getTeamPrefixFromDisplay(String display) {
        if (display != null && !display.isEmpty()) {
            for (String color : new String[]{"c", "9", "a", "e", "b", "f", "d", "8"}) {
                String prefix = "§" + color;
                if (display.startsWith(prefix)) {
                    return prefix;
                }
            }

            return "";
        } else {
            return "";
        }
    }

    private void clearSelfTimerState(Entity self) {
        if (self != null) {
            int id = self.func_145782_y();
            this.anchorPositions.remove(id);
            this.stuckSince.remove(id);
            this.alerted.remove(id);
            this.clearBeacon(id);
        }
    }

    private void activateBeaconForEntity(Entity entity) {
        int id = entity.func_145782_y();
        this.beaconNames.put(id, this.getEntityName(entity));
        this.beaconColours.put(id, this.withAlpha(this.getPlayerColour(entity), 190));
        this.beaconMessagesSent.put(id, false);
    }

    private void clearBeacon(int id) {
        this.beaconNames.remove(id);
        this.beaconColours.remove(id);
        this.beaconMessagesSent.remove(id);
    }

    private void clearAllBeacons() {
        this.beaconNames.clear();
        this.beaconColours.clear();
        this.beaconMessagesSent.clear();
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && !this.beaconNames.isEmpty()) {
            if (this.isClientSpectator()) {
                this.clearAllBeacons();
            } else {
                for (int id : new ArrayList<>(this.beaconNames.keySet())) {
                    Entity entity = this.findEntityById(id);
                    if (entity == null) {
                        this.clearBeacon(id);
                    } else {
                        Vec3 currentPos = new Vec3(entity.field_70165_t, entity.field_70163_u, entity.field_70161_v);
                        if (this.isInVoid(currentPos) && !this.isInWater(currentPos)) {
                            Vec3 pos = this.getLiveBeaconPosition(entity, event.getPartialTicks());
                            if (pos == null) {
                                this.clearBeacon(id);
                            } else {
                                int colour = this.withAlpha(this.getPlayerColour(entity), 190);
                                this.beaconNames.put(id, this.getEntityName(entity));
                                this.beaconColours.put(id, colour);
                                this.drawBeaconBeam(pos, colour);
                                boolean messageSent = this.beaconMessagesSent.containsKey(id)
                                    && this.beaconMessagesSent.get(id);
                                if (!messageSent) {
                                    this.beaconMessagesSent.put(id, true);
                                    this.playAlertSound();
                                }
                            }
                        } else {
                            this.clearBeacon(id);
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled() && !this.beaconNames.isEmpty()) {
            if (!this.isClientSpectator()) {
                float partialTicks = event.getPartialTicks();

                for (int id : new ArrayList<>(this.beaconNames.keySet())) {
                    Entity entity = this.findEntityById(id);
                    if (entity != null) {
                        Vec3 currentPos = new Vec3(entity.field_70165_t, entity.field_70163_u, entity.field_70161_v);
                        if (this.isInVoid(currentPos) && !this.isInWater(currentPos)) {
                            Vec3 pos = this.getLiveBeaconPosition(entity, partialTicks);
                            if (pos != null) {
                                double[] screen = this.worldToScreen(
                                    pos.field_72450_a,
                                    pos.field_72448_b + this.getEntityHeight(entity) + 8.0,
                                    pos.field_72449_c,
                                    partialTicks
                                );
                                if (screen != null) {
                                    int colour = this.beaconColours.containsKey(id)
                                        ? this.beaconColours.get(id)
                                        : -1426063361;
                                    int coreColour = this.withAlpha(colour, 210);
                                    String name = this.beaconNames.containsKey(id)
                                        ? this.beaconNames.get(id)
                                        : "player";
                                    String prefix = "Timer: ";
                                    float labelScale = this.getBeaconLabelScale(pos);
                                    float prefixWidth = mc.field_71466_p.func_78256_a(prefix) * labelScale;
                                    float nameWidth = mc.field_71466_p.func_78256_a(name) * labelScale;
                                    float x = (float)screen[0] - (prefixWidth + nameWidth) / 2.0F;
                                    int outlineColor = this.isFriendlyEntity(entity) ? -14494101 : -48060;
                                    this.drawBeaconLabelBox(
                                        prefix,
                                        name,
                                        x,
                                        (float)screen[1] - 18.0F,
                                        labelScale,
                                        -1,
                                        coreColour,
                                        outlineColor
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private float getBeaconLabelScale(Vec3 pos) {
        if (mc.field_71439_g != null && pos != null) {
            double dx = mc.field_71439_g.field_70165_t - pos.field_72450_a;
            double dy = mc.field_71439_g.field_70163_u - pos.field_72448_b;
            double dz = mc.field_71439_g.field_70161_v - pos.field_72449_c;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            return this.clamp((float)(18.0 / Math.max(20.0, distance)), 0.45F, 0.72F);
        } else {
            return 0.72F;
        }
    }

    private void drawBeaconLabelBox(
        String prefix, String name, float x, float y, float scale, int prefixColor, int nameColor, int outlineColor
    ) {
        float prefixWidth = mc.field_71466_p.func_78256_a(prefix) * scale;
        float width = prefixWidth + mc.field_71466_p.func_78256_a(name) * scale;
        float height = mc.field_71466_p.field_78288_b * scale;
        float padX = 3.5F * scale;
        float padY = 2.0F * scale;
        float x1 = x - padX;
        float y1 = y - padY;
        float x2 = x + width + padX;
        float y2 = y + height + padY;
        this.drawRectOutline(x1, y1, x2, y2, 1.0F, this.withAlpha(outlineColor, 230));
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b(x, y, 0.0F);
        GlStateManager.func_179152_a(scale, scale, 1.0F);
        mc.field_71466_p.func_175065_a(prefix, 0.0F, 0.0F, prefixColor, true);
        mc.field_71466_p.func_175065_a(name, prefixWidth / scale, 0.0F, nameColor, true);
        GlStateManager.func_179121_F();
    }

    private void drawRectOutline(float x1, float y1, float x2, float y2, float thickness, int color) {
        RenderUtil.drawRect(x1, y1, x2, y1 + thickness, color);
        RenderUtil.drawRect(x1, y2 - thickness, x2, y2, color);
        RenderUtil.drawRect(x1, y1, x1 + thickness, y2, color);
        RenderUtil.drawRect(x2 - thickness, y1, x2, y2, color);
    }

    private void drawBeaconBeam(Vec3 pos, int colour) {
        double x = pos.field_72450_a - mc.func_175598_ae().field_78730_l;
        double y = pos.field_72448_b - mc.func_175598_ae().field_78731_m;
        double z = pos.field_72449_c - mc.func_175598_ae().field_78728_n;
        RenderUtil.enableRenderState();
        RenderUtil.setColor(this.withAlpha(colour, 85));
        GL11.glLineWidth(2.0F);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        GL11.glBegin(1);
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x, y + 128.0, z);
        GL11.glEnd();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0F);
        RenderUtil.disableRenderState();
    }

    private Vec3 getLiveBeaconPosition(Entity entity, float partialTicks) {
        double x = RenderUtil.lerpDouble(entity.field_70165_t, entity.field_70142_S, partialTicks);
        double y = RenderUtil.lerpDouble(entity.field_70163_u, entity.field_70137_T, partialTicks);
        double z = RenderUtil.lerpDouble(entity.field_70161_v, entity.field_70136_U, partialTicks);
        return new Vec3(Math.floor(x) + 0.5, Math.floor(y), Math.floor(z) + 0.5);
    }

    private double getEntityHeight(Entity entity) {
        try {
            return entity.field_70131_O;
        } catch (Exception var3) {
            return 2.0;
        }
    }

    private Entity findEntityById(int id) {
        if (mc.field_71439_g != null && mc.field_71439_g.func_145782_y() == id) {
            return mc.field_71439_g;
        }

        if (mc.field_71441_e == null) {
            return null;
        }

        for (Object o : mc.field_71441_e.field_73010_i) {
            Entity entity = (Entity)o;
            if (entity != null && entity.func_145782_y() == id) {
                return entity;
            }
        }

        return null;
    }

    private boolean isInVoid(Vec3 pos) {
        int y = (int)Math.floor(pos.field_72448_b);
        double radius = 0.42;
        double[] xs = new double[]{pos.field_72450_a, pos.field_72450_a - radius, pos.field_72450_a + radius};
        double[] zs = new double[]{pos.field_72449_c, pos.field_72449_c - radius, pos.field_72449_c + radius};

        for (double sampleX : xs) {
            for (double sampleZ : zs) {
                int x = (int)Math.floor(sampleX);
                int z = (int)Math.floor(sampleZ);

                for (int checkY = y - 1; checkY >= 0; checkY--) {
                    if (!mc.field_71441_e.func_175623_d(new BlockPos(x, checkY, z))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean isInWater(Vec3 pos) {
        BlockPos blockPos = new BlockPos(
            (int)Math.floor(pos.field_72450_a), (int)Math.floor(pos.field_72448_b), (int)Math.floor(pos.field_72449_c)
        );
        Block block = mc.field_71441_e.func_180495_p(blockPos).func_177230_c();
        return block == Blocks.field_150355_j
            || block == Blocks.field_150358_i
            || block == Blocks.field_150353_l
            || block == Blocks.field_150356_k;
    }

    private int getPlayerColour(Entity entity) {
        String name = this.getEntityDisplayName(entity);
        if (name != null && !name.isEmpty()) {
            for (int i = 0; i < name.length() - 1; i++) {
                if (name.charAt(i) == 167) {
                    String code = String.valueOf(name.charAt(i + 1)).toLowerCase();
                    if (this.teamColours.containsKey(code)) {
                        return this.teamColours.get(code);
                    }
                }
            }
        }

        return new Color(255, 255, 255, 255).getRGB();
    }

    private int withAlpha(int color, int alpha) {
        return (alpha & 0xFF) << 24 | color & 16777215;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private String getEntityName(Entity entity) {
        try {
            String name = this.getEntityDisplayName(entity);
            if (name != null && !name.isEmpty()) {
                return name.replaceAll("§[\\da-fk-or]", "");
            }
        } catch (Exception var4) {
        }

        try {
            if (entity instanceof EntityPlayer) {
                String name = ((EntityPlayer)entity).func_70005_c_();
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        } catch (Exception var3) {
        }

        return "player";
    }

    private void playAlertSound() {
        if (this.ping.getValue()) {
            try {
                SoundUtil.playSound("random.orb");
            } catch (Exception var2) {
            }
        }
    }

    private double[] worldToScreen(double x, double y, double z, float partialTicks) {
        ((IAccessorEntityRenderer)mc.field_71460_t).callSetupCameraTransform(partialTicks, 0);
        GL11.glGetFloat(2982, MODELVIEW);
        GL11.glGetFloat(2983, PROJECTION);
        GL11.glGetInteger(2978, VIEWPORT);
        ((Buffer)SCREEN_COORDS).clear();
        boolean success = GLU.gluProject(
            (float)(x - mc.func_175598_ae().field_78730_l),
            (float)(y - mc.func_175598_ae().field_78731_m),
            (float)(z - mc.func_175598_ae().field_78728_n),
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
        return !(screenZ < 0.0) && !(screenZ >= 1.0) ? new double[]{screenX, screenY, screenZ} : null;
    }
}
