package miau.module.modules.combat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import miau.Miau;
import miau.enums.BlinkModules;
import miau.event.EventTarget;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ItemListProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.CombatTargeting;
import miau.util.player.MoveUtil;
import miau.util.player.RotationUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.util.BlockPos.MutableBlockPos;
import org.lwjgl.opengl.GL11;

public class Displace extends Module {
    private static final int DISPLACE_WINDOW_TICKS = 10;
    private static final int VOID_SCAN_DIRECTIONS = 32;
    private static final int VOID_SCAN_RINGS = 12;
    private static final int VOID_SCAN_DEPTH = 10;
    private static final double VOID_SCAN_STEP = 0.5;
    private static final double DYNAMIC_SCAN_STEP = 0.5;
    private static final double DYNAMIC_SCAN_DISTANCE = 6.0;
    private static final double DYNAMIC_SCAN_SIDE_STEP = 0.45;
    private static final double DYNAMIC_WALL_CHECK_STEP = 0.25;
    private static final double DYNAMIC_COLLISION_INSET = 0.03;
    private static final long ARROW_FADE_MS = 250L;
    private static final double ARROW_FORWARD_GAP = 0.24;
    private static final double ARROW_BODY_LENGTH = 0.74;
    private static final double ARROW_BODY_HALF_HEIGHT = 0.08;
    private static final double ARROW_HEAD_BACKSET = 0.18;
    private static final double ARROW_HEAD_LENGTH = 0.52;
    private static final double ARROW_HEAD_HALF_HEIGHT = 0.3;
    private static final double[] VOID_SCAN_X = new double[32];
    private static final double[] VOID_SCAN_Z = new double[32];
    public final ModeProperty dynamicAngle = new ModeProperty("Dynamic-angle", 0, new String[]{"Static", "Dynamic"});
    public final FloatProperty yawOffset = new FloatProperty(
        "Yaw-offset", 90.0F, 0.0F, 180.0F, () -> this.dynamicAngle.getValue() == 0
    );
    public final FloatProperty delay = new FloatProperty("Delay", 0.0F, 0.0F, 500.0F);
    public final ModeProperty direction = new ModeProperty(
        "Direction", 0, new String[]{"Left", "Right"}, () -> this.dynamicAngle.getValue() == 0
    );
    public final BooleanProperty showDirection = new BooleanProperty("Show-direction", true);
    public final BooleanProperty findVoid = new BooleanProperty(
        "Find-void", false, () -> this.dynamicAngle.getValue() == 0
    );
    public final BooleanProperty blink = new BooleanProperty("Blink", false);
    public final BooleanProperty ignoreTeammates = new BooleanProperty("Ignore-teammates", true);
    public final BooleanProperty hasKnockback = new BooleanProperty("Has-knockback", false);
    public final BooleanProperty itemWhitelistToggle = new BooleanProperty("Item-whitelist", false);
    public final ItemListProperty itemWhitelist = new ItemListProperty("Whitelisted-items", "");
    private boolean displaceThisTick = false;
    private boolean active = false;
    private boolean hasKB = false;
    private boolean compensateNextTick = false;
    private boolean displaceLeft = false;
    private boolean wasDisplacingLastTick = false;
    private boolean releaseBlinkNextGameTick = false;
    private boolean blinkingModule = false;
    private Float dynamicVoidYaw = null;
    private Float renderDisplaceYaw = null;
    private EntityPlayer renderTarget = null;
    private Float fadingDisplaceYaw = null;
    private EntityPlayer fadingTarget = null;
    private long arrowFadeStartMs = 0L;
    private Float lastRenderedDisplaceYaw = null;
    private EntityPlayer lastRenderedTarget = null;
    private long lastRenderedArrowMs = 0L;
    private int tickCounter;
    private final Map<Integer, Integer> targetWindowStartTicks = new HashMap<>();
    private static final Minecraft mc = Minecraft.func_71410_x();

    public Displace() {
        super("Displace", false);
    }

    @Override
    public String[] getSuffix() {
        int ms = Math.round(this.delay.getValue());
        return new String[]{ms + "ms"};
    }

    @Override
    public void onEnabled() {
        this.displaceThisTick = false;
        this.active = false;
        this.hasKB = false;
        this.compensateNextTick = false;
        this.wasDisplacingLastTick = false;
        this.releaseBlinkNextGameTick = false;
        this.dynamicVoidYaw = null;
        this.renderDisplaceYaw = null;
        this.renderTarget = null;
        this.clearArrowState();
        this.tickCounter = 0;
        this.targetWindowStartTicks.clear();
        this.releaseBlink();
    }

    @Override
    public void onDisabled() {
        this.active = false;
        this.compensateNextTick = false;
        this.wasDisplacingLastTick = false;
        this.releaseBlinkNextGameTick = false;
        this.dynamicVoidYaw = null;
        this.renderDisplaceYaw = null;
        this.renderTarget = null;
        this.clearArrowState();
        this.targetWindowStartTicks.clear();
        this.releaseBlink();
    }

    private static int msToTicks(double ms) {
        return ms <= 0.0 ? 0 : (int)Math.ceil(ms / 50.0);
    }

    private boolean anyMovementKey() {
        return mc.field_71474_y.field_74351_w.func_151470_d()
            || mc.field_71474_y.field_74368_y.func_151470_d()
            || mc.field_71474_y.field_74370_x.func_151470_d()
            || mc.field_71474_y.field_74366_z.func_151470_d();
    }

    private boolean isDynamicAngle() {
        return this.dynamicAngle.getValue() == 1;
    }

    private Float findStaticVoidYaw(EntityPlayer target) {
        if (target != null && mc.field_71439_g != null && mc.field_71441_e != null) {
            double bestX = 0.0;
            double bestZ = 0.0;
            double bestScore = Double.MAX_VALUE;

            for (int ring = 1; ring <= 12; ring++) {
                double radius = ring * 0.5;
                boolean foundInRing = false;

                for (int i = 0; i < 32; i++) {
                    double x = target.field_70165_t + VOID_SCAN_X[i] * radius;
                    double z = target.field_70161_v + VOID_SCAN_Z[i] * radius;
                    if (this.isVoidColumn(x, target.field_70163_u, z)) {
                        double playerDx = x - mc.field_71439_g.field_70165_t;
                        double playerDz = z - mc.field_71439_g.field_70161_v;
                        double playerDistSq = playerDx * playerDx + playerDz * playerDz;
                        double score = radius * radius * 1000.0 + playerDistSq;
                        if (score < bestScore) {
                            bestScore = score;
                            bestX = x;
                            bestZ = z;
                            foundInRing = true;
                        }
                    }
                }

                if (foundInRing) {
                    break;
                }
            }

            if (bestScore == Double.MAX_VALUE) {
                return null;
            }

            this.updateDisplaceSide(target, bestX, bestZ);
            double dx = bestX - target.field_70165_t;
            double dz = bestZ - target.field_70161_v;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.001) {
                return null;
            }

            double aimRadius = Math.min(dist, Math.max(0.35, target.field_70130_N * 0.5 + 0.15));
            double aimX = target.field_70165_t + dx / dist * aimRadius;
            double aimZ = target.field_70161_v + dz / dist * aimRadius;
            Vec3 eyes = mc.field_71439_g.func_174824_e(1.0F);
            double adx = aimX - eyes.field_72450_a;
            double ady = target.field_70163_u + target.func_70047_e() * 0.5 - eyes.field_72448_b;
            double adz = aimZ - eyes.field_72449_c;
            return (float)Math.toDegrees(Math.atan2(adz, adx)) - 90.0F;
        } else {
            return null;
        }
    }

    private Float findDynamicVoidYaw(EntityPlayer target) {
        if (target != null && mc.field_71439_g != null && mc.field_71441_e != null) {
            double bestForwardX = 0.0;
            double bestForwardZ = 0.0;
            double bestScore = 0.0;

            for (int i = 0; i < 32; i++) {
                double forwardX = VOID_SCAN_X[i];
                double forwardZ = VOID_SCAN_Z[i];
                double score = this.scoreVoidPath(target, forwardX, forwardZ);
                if (score > bestScore) {
                    bestScore = score;
                    bestForwardX = forwardX;
                    bestForwardZ = forwardZ;
                }
            }

            if (bestScore <= 0.0) {
                return null;
            }

            this.updateDisplaceSide(target, target.field_70165_t + bestForwardX, target.field_70161_v + bestForwardZ);
            return this.yawFromForward(bestForwardX, bestForwardZ);
        } else {
            return null;
        }
    }

    private float yawFromForward(double forwardX, double forwardZ) {
        return (float)(Math.toDegrees(Math.atan2(forwardZ, forwardX)) - 90.0);
    }

    private double scoreVoidPath(EntityPlayer target, double forwardX, double forwardZ) {
        double sideX = -forwardZ;
        double sideZ = forwardX;
        double score = 0.0;
        double checkedForward = 0.0;
        int consecutiveCenterVoid = 0;
        AxisAlignedBB baseCollisionBox = target.func_174813_aQ().func_72331_e(0.03, 0.0, 0.03);

        for (int step = 1; step <= 12; step++) {
            double forward = step * 0.5;
            if (!this.isDynamicPathClear(target, baseCollisionBox, forwardX, forwardZ, checkedForward, forward)) {
                break;
            }

            checkedForward = forward;
            boolean centerVoid = false;

            for (int side = -1; side <= 1; side++) {
                double sideOffset = side * 0.45;
                double x = target.field_70165_t + forwardX * forward + sideX * sideOffset;
                double z = target.field_70161_v + forwardZ * forward + sideZ * sideOffset;
                if (this.isVoidColumn(x, target.field_70163_u, z)) {
                    double laneWeight = side == 0 ? 1.4 : 1.0;
                    score += laneWeight * (6.5 - forward);
                    centerVoid |= side == 0;
                }
            }

            if (centerVoid) {
                score += ++consecutiveCenterVoid * 2.0;
            } else {
                consecutiveCenterVoid = 0;
            }
        }

        return score;
    }

    private boolean isDynamicPathClear(
        EntityPlayer target,
        AxisAlignedBB baseCollisionBox,
        double forwardX,
        double forwardZ,
        double fromForward,
        double toForward
    ) {
        for (double forward = fromForward + 0.25; forward <= toForward + 1.0E-4; forward += 0.25) {
            AxisAlignedBB checkBox = baseCollisionBox.func_72317_d(forwardX * forward, 0.0, forwardZ * forward);
            if (this.hasBlockCollision(target, checkBox)) {
                return false;
            }
        }

        return true;
    }

    private boolean hasBlockCollision(EntityPlayer target, AxisAlignedBB box) {
        int minX = MathHelper.func_76128_c(box.field_72340_a);
        int maxX = MathHelper.func_76128_c(box.field_72336_d + 1.0);
        int minY = MathHelper.func_76128_c(box.field_72338_b);
        int maxY = MathHelper.func_76128_c(box.field_72337_e + 1.0);
        int minZ = MathHelper.func_76128_c(box.field_72339_c);
        int maxZ = MathHelper.func_76128_c(box.field_72334_f + 1.0);
        List<AxisAlignedBB> collisions = new ArrayList<>();
        MutableBlockPos blockPos = new MutableBlockPos();

        for (int blockX = minX; blockX < maxX; blockX++) {
            for (int blockZ = minZ; blockZ < maxZ; blockZ++) {
                if (!mc.field_71441_e.func_175667_e(blockPos.func_181079_c(blockX, 64, blockZ))) {
                    return true;
                }

                for (int blockY = minY; blockY < maxY; blockY++) {
                    if (blockY < 0 || blockY >= 256) {
                        return true;
                    }

                    blockPos.func_181079_c(blockX, blockY, blockZ);
                    IBlockState state = mc.field_71441_e.func_180495_p(blockPos);
                    state.func_177230_c().func_180638_a(mc.field_71441_e, blockPos, state, box, collisions, target);
                    if (!collisions.isEmpty()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isVoidColumn(double x, double y, double z) {
        int blockX = MathHelper.func_76128_c(x);
        int blockZ = MathHelper.func_76128_c(z);
        int startY = MathHelper.func_76128_c(y) - 1;
        int endY = Math.max(0, startY - 10);

        for (int blockY = startY; blockY >= endY; blockY--) {
            if (!mc.field_71441_e.func_175623_d(new BlockPos(blockX, blockY, blockZ))) {
                return false;
            }
        }

        return true;
    }

    private void updateDisplaceSide(EntityPlayer target, double voidX, double voidZ) {
        double targetDx = target.field_70165_t - mc.field_71439_g.field_70165_t;
        double targetDz = target.field_70161_v - mc.field_71439_g.field_70161_v;
        double voidDx = voidX - mc.field_71439_g.field_70165_t;
        double voidDz = voidZ - mc.field_71439_g.field_70161_v;
        double cross = targetDx * voidDz - targetDz * voidDx;
        this.displaceLeft = cross < 0.0;
    }

    private float getFixedDisplaceYaw() {
        float baseYaw = RotationUtil.customRots ? RotationUtil.serverYaw : mc.field_71439_g.field_70177_z;
        float offset = this.yawOffset.getValue();
        return this.displaceLeft ? baseYaw - offset : baseYaw + offset;
    }

    private void clearActiveState() {
        this.startArrowFade();
        this.active = false;
        this.displaceThisTick = false;
        this.compensateNextTick = false;
        this.wasDisplacingLastTick = false;
        this.dynamicVoidYaw = null;
        this.renderDisplaceYaw = null;
        this.renderTarget = null;
    }

    private void clearFadingArrow() {
        this.fadingDisplaceYaw = null;
        this.fadingTarget = null;
        this.arrowFadeStartMs = 0L;
    }

    private void clearArrowState() {
        this.clearFadingArrow();
        this.lastRenderedDisplaceYaw = null;
        this.lastRenderedTarget = null;
        this.lastRenderedArrowMs = 0L;
    }

    private void startArrowFade() {
        long nowMs = System.currentTimeMillis();
        if (this.lastRenderedDisplaceYaw != null
            && this.lastRenderedTarget != null
            && !this.lastRenderedTarget.field_70128_L
            && nowMs - this.lastRenderedArrowMs <= 250L) {
            this.fadingDisplaceYaw = this.lastRenderedDisplaceYaw;
            this.fadingTarget = this.lastRenderedTarget;
            this.arrowFadeStartMs = nowMs;
        }

        this.lastRenderedDisplaceYaw = null;
        this.lastRenderedTarget = null;
        this.lastRenderedArrowMs = 0L;
    }

    private void pruneTargetDelayStates() {
        if (mc.field_71441_e == null) {
            this.targetWindowStartTicks.clear();
        } else {
            Iterator<Entry<Integer, Integer>> iterator = this.targetWindowStartTicks.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry<Integer, Integer> entry = iterator.next();
                Entity entity = mc.field_71441_e.func_73045_a(entry.getKey());
                if (!(entity instanceof EntityPlayer)
                    || entity.field_70128_L
                    || ((EntityPlayer)entity).field_70725_aQ != 0) {
                    iterator.remove();
                }
            }
        }
    }

    private boolean shouldDisplaceInCurrentWindow(EntityPlayer target, int currentTick) {
        if (target == null) {
            return true;
        }

        int targetId = target.func_145782_y();
        Integer windowStartTick = this.targetWindowStartTicks.get(targetId);
        if (windowStartTick != null && currentTick - windowStartTick < 10) {
            int delayTicks = msToTicks(this.delay.getValue().floatValue());
            if (delayTicks <= 0) {
                return true;
            }

            int elapsed = currentTick - windowStartTick;
            return elapsed >= delayTicks;
        } else {
            this.targetWindowStartTicks.put(targetId, currentTick);
            return true;
        }
    }

    private void releaseBlink() {
        if (this.blinkingModule) {
            Miau.blinkManager.setBlinkState(false, BlinkModules.DISPLACE);
            this.blinkingModule = false;
        }
    }

    @EventTarget(0)
    public void onGameTick(TickEvent e) {
        if (e.getType() == EventType.PRE) {
            if (this.releaseBlinkNextGameTick) {
                this.releaseBlink();
                this.releaseBlinkNextGameTick = false;
            }
        }
    }

    @EventTarget
    public void onRenderWorld(Render3DEvent e) {
        if (this.isEnabled()) {
            if (!this.showDirection.getValue()) {
                this.clearArrowState();
            } else {
                long nowMs = System.currentTimeMillis();
                boolean activeArrow = this.active
                    && this.renderDisplaceYaw != null
                    && this.renderTarget != null
                    && !this.renderTarget.field_70128_L;
                Float arrowYaw = this.renderDisplaceYaw;
                EntityPlayer arrowTarget = this.renderTarget;
                float alpha = 1.0F;
                if (activeArrow) {
                    this.clearFadingArrow();
                } else {
                    if (this.fadingDisplaceYaw == null || this.fadingTarget == null || this.fadingTarget.field_70128_L) {
                        this.clearFadingArrow();
                        return;
                    }

                    long fadeElapsedMs = nowMs - this.arrowFadeStartMs;
                    if (fadeElapsedMs >= 250L) {
                        this.clearFadingArrow();
                        return;
                    }

                    arrowYaw = this.fadingDisplaceYaw;
                    arrowTarget = this.fadingTarget;
                    alpha = 1.0F - (float)fadeElapsedMs / 250.0F;
                }

                float partialTicks = e.getPartialTicks();
                double centerX = arrowTarget.field_70142_S
                    + (arrowTarget.field_70165_t - arrowTarget.field_70142_S) * partialTicks;
                double centerY = arrowTarget.field_70137_T
                    + (arrowTarget.field_70163_u - arrowTarget.field_70137_T) * partialTicks
                    + arrowTarget.field_70131_O * 0.5;
                double centerZ = arrowTarget.field_70136_U
                    + (arrowTarget.field_70161_v - arrowTarget.field_70136_U) * partialTicks;
                double yawRad = Math.toRadians(arrowYaw.floatValue());
                double forwardX = -Math.sin(yawRad);
                double forwardZ = Math.cos(yawRad);
                double baseOffset = arrowTarget.field_70130_N * 0.5 + 0.24;
                double tailX = centerX + forwardX * baseOffset;
                double tailZ = centerZ + forwardZ * baseOffset;
                double bodyEndX = tailX + forwardX * 0.74;
                double bodyEndZ = tailZ + forwardZ * 0.74;
                double headBackX = tailX + forwardX * 0.56;
                double headBackZ = tailZ + forwardZ * 0.56;
                double tipX = bodyEndX + forwardX * 0.52;
                double tipZ = bodyEndZ + forwardZ * 0.52;
                double viewerX = mc.func_175598_ae().field_78730_l;
                double viewerY = mc.func_175598_ae().field_78731_m;
                double viewerZ = mc.func_175598_ae().field_78728_n;
                GL11.glPushMatrix();
                GL11.glPushAttrib(24837);
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glDisable(3553);
                GL11.glDisable(2896);
                GL11.glDisable(2929);
                GL11.glDisable(2884);
                GL11.glDepthMask(false);
                GL11.glEnable(2848);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.82F * alpha);
                GL11.glBegin(4);
                GL11.glVertex3d(tailX - viewerX, centerY - viewerY, tailZ - viewerZ);
                this.arrowVertex(bodyEndX, centerY, bodyEndZ, -0.08, viewerX, viewerY, viewerZ);
                this.arrowVertex(bodyEndX, centerY, bodyEndZ, 0.08, viewerX, viewerY, viewerZ);
                this.arrowVertex(bodyEndX, centerY, bodyEndZ, -0.08, viewerX, viewerY, viewerZ);
                this.arrowVertex(headBackX, centerY, headBackZ, -0.3, viewerX, viewerY, viewerZ);
                GL11.glVertex3d(tipX - viewerX, centerY - viewerY, tipZ - viewerZ);
                this.arrowVertex(bodyEndX, centerY, bodyEndZ, -0.08, viewerX, viewerY, viewerZ);
                GL11.glVertex3d(tipX - viewerX, centerY - viewerY, tipZ - viewerZ);
                this.arrowVertex(bodyEndX, centerY, bodyEndZ, 0.08, viewerX, viewerY, viewerZ);
                this.arrowVertex(bodyEndX, centerY, bodyEndZ, 0.08, viewerX, viewerY, viewerZ);
                GL11.glVertex3d(tipX - viewerX, centerY - viewerY, tipZ - viewerZ);
                this.arrowVertex(headBackX, centerY, headBackZ, 0.3, viewerX, viewerY, viewerZ);
                GL11.glEnd();
                GL11.glLineWidth(2.0F);
                GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.95F * alpha);
                GL11.glBegin(2);
                GL11.glVertex3d(tailX - viewerX, centerY - viewerY, tailZ - viewerZ);
                this.arrowVertex(bodyEndX, centerY, bodyEndZ, -0.08, viewerX, viewerY, viewerZ);
                this.arrowVertex(headBackX, centerY, headBackZ, -0.3, viewerX, viewerY, viewerZ);
                GL11.glVertex3d(tipX - viewerX, centerY - viewerY, tipZ - viewerZ);
                this.arrowVertex(headBackX, centerY, headBackZ, 0.3, viewerX, viewerY, viewerZ);
                this.arrowVertex(bodyEndX, centerY, bodyEndZ, 0.08, viewerX, viewerY, viewerZ);
                GL11.glEnd();
                GL11.glPopAttrib();
                GL11.glPopMatrix();
                if (activeArrow) {
                    this.lastRenderedDisplaceYaw = arrowYaw;
                    this.lastRenderedTarget = arrowTarget;
                    this.lastRenderedArrowMs = nowMs;
                }
            }
        }
    }

    private void arrowVertex(
        double x, double y, double z, double verticalOffset, double viewerX, double viewerY, double viewerZ
    ) {
        GL11.glVertex3d(x - viewerX, y + verticalOffset - viewerY, z - viewerZ);
    }

    @EventTarget(4)
    public void onPostInput(MoveInputEvent e) {
        if (this.isEnabled()) {
            if (!this.active) {
                this.compensateNextTick = false;
            } else if (this.compensateNextTick && !this.displaceThisTick) {
                this.compensateNextTick = false;
                if (this.displaceLeft) {
                    mc.field_71439_g.field_71158_b.field_78902_a = -1.0F;
                } else {
                    mc.field_71439_g.field_71158_b.field_78902_a = 1.0F;
                }
            } else if (this.displaceThisTick && !this.hasKB) {
                if (this.anyMovementKey()) {
                    mc.field_71439_g.field_71158_b.field_78900_b = 1.0F;
                    this.compensateNextTick = true;
                }
            }
        }
    }

    @EventTarget(1)
    public void onSendPacket(PacketEvent e) {
        if (this.isEnabled()) {
            if (this.blink.getValue() && this.active && this.displaceThisTick && !this.releaseBlinkNextGameTick) {
                if (e.getPacket() instanceof C03PacketPlayer) {
                    if (!this.blinkingModule) {
                        Miau.blinkManager.setBlinkState(true, BlinkModules.DISPLACE);
                        this.blinkingModule = true;
                        this.releaseBlinkNextGameTick = true;
                    }
                }
            }
        }
    }

    @EventTarget(4)
    public void onUpdateLowest(UpdateEvent e) {
        if (this.isEnabled()) {
            if (e.getType() == EventType.PRE) {
                this.tickCounter++;
                int currentTick = this.tickCounter;
                this.pruneTargetDelayStates();
                boolean passesItemCondition = true;
                if (this.hasKnockback.getValue() || this.itemWhitelistToggle.getValue()) {
                    boolean kbPass = !this.hasKnockback.getValue()
                        || EnchantmentHelper.func_77501_a(mc.field_71439_g) > 0;
                    boolean wlPass = !this.itemWhitelistToggle.getValue()
                        || this.itemWhitelist.matches(mc.field_71439_g.func_70694_bm());
                    passesItemCondition = kbPass || wlPass;
                }

                if (!passesItemCondition) {
                    this.clearActiveState();
                } else {
                    EntityPlayer target = null;
                    boolean attacking = mc.field_71474_y.field_74312_F.func_151470_d()
                        || Miau.moduleManager.modules.get(KillAura.class) != null
                            && Miau.moduleManager.modules.get(KillAura.class).isEnabled()
                            && ((KillAura)Miau.moduleManager.modules.get(KillAura.class)).getTarget() != null;
                    if (attacking) {
                        target = CombatTargeting.findClosestTarget(9.0, this.ignoreTeammates.getValue());
                    }

                    boolean hasKBEnchant = EnchantmentHelper.func_77501_a(mc.field_71439_g) > 0;
                    this.active = target != null && (hasKBEnchant || this.anyMovementKey());
                    if (!this.active) {
                        this.clearActiveState();
                    } else {
                        this.dynamicVoidYaw = this.isDynamicAngle()
                            ? this.findDynamicVoidYaw(target)
                            : (this.findVoid.getValue() ? this.findStaticVoidYaw(target) : null);
                        if (this.dynamicVoidYaw == null && !this.isDynamicAngle()) {
                            this.displaceLeft = this.direction.getValue() == 0;
                        }

                        this.renderDisplaceYaw = this.dynamicVoidYaw != null
                            ? this.dynamicVoidYaw
                            : (this.isDynamicAngle() ? null : this.getFixedDisplaceYaw());
                        this.renderTarget = this.renderDisplaceYaw != null ? target : null;
                        if (this.renderDisplaceYaw == null) {
                            this.clearActiveState();
                        } else {
                            this.hasKB = hasKBEnchant;
                            this.displaceThisTick = !this.displaceThisTick;
                            if (this.displaceThisTick && !this.shouldDisplaceInCurrentWindow(target, currentTick)) {
                                this.startArrowFade();
                                this.displaceThisTick = false;
                                this.compensateNextTick = false;
                                this.wasDisplacingLastTick = false;
                                this.dynamicVoidYaw = null;
                                this.renderDisplaceYaw = null;
                                this.renderTarget = null;
                            } else {
                                if (!this.displaceThisTick && this.wasDisplacingLastTick) {
                                    int key = mc.field_71474_y.field_74312_F.func_151463_i();
                                    if (key != 0) {
                                        KeyBinding.func_74507_a(key);
                                    }
                                }

                                this.wasDisplacingLastTick = this.displaceThisTick;
                                if (this.displaceThisTick && this.renderDisplaceYaw != null) {
                                    e.setRotation(this.renderDisplaceYaw, mc.field_71439_g.field_70125_A, 100);
                                    MoveUtil.fixMovement(this.renderDisplaceYaw);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static {
        for (int i = 0; i < 32; i++) {
            double angle = (Math.PI * 2) * i / 32.0;
            VOID_SCAN_X[i] = Math.cos(angle);
            VOID_SCAN_Z[i] = Math.sin(angle);
        }
    }
}
