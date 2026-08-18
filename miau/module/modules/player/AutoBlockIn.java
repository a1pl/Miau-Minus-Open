package miau.module.modules.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import miau.event.EventTarget;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.SwapItemEvent;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.management.RotationState;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.ItemUtil;
import miau.util.player.MoveUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import org.lwjgl.opengl.GL11;

public class AutoBlockIn extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final Map<String, Integer> BLOCK_SCORE = new HashMap<>();
    private long lastPlaceTime = 0L;
    public final FloatProperty range = new FloatProperty("range", 4.5F, 3.0F, 6.0F);
    public final IntProperty speed = new IntProperty("speed", 20, 5, 100);
    public final IntProperty placeDelay = new IntProperty("place-delay", 50, 0, 200);
    public final IntProperty rotationTolerance = new IntProperty("rotation-tolerance", 25, 5, 100);
    public final BooleanProperty itemSpoof = new BooleanProperty("item-spoof", true);
    public final BooleanProperty showProgress = new BooleanProperty("show-progress", true);
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"});
    public final BooleanProperty disableOnMove = new BooleanProperty("disable-on-move", false);
    public final BooleanProperty autoDisable = new BooleanProperty("auto-disable", true);
    private BlockPos startPos;
    private float serverYaw;
    private float serverPitch;
    private float progress;
    private float aimYaw;
    private float aimPitch;
    private BlockPos targetBlock;
    private EnumFacing targetFacing;
    private Vec3 targetHitVec;
    private int lastSlot = -1;
    private float animStartProgress = 0.0F;
    private float animTargetProgress = 0.0F;
    private long animStartTime = 0L;
    private float lastProgress = -1.0F;
    private static final int[][] DIRS = new int[][]{{1, 0, 0}, {0, 0, 1}, {-1, 0, 0}, {0, 0, -1}};
    private static final double INSET = 0.05;
    private static final double STEP = 0.2;
    private static final double JIT = 0.020000000000000004;

    public AutoBlockIn() {
        super("AutoBlockIn", false);
        this.BLOCK_SCORE.put("obsidian", 0);
        this.BLOCK_SCORE.put("end_stone", 1);
        this.BLOCK_SCORE.put("planks", 2);
        this.BLOCK_SCORE.put("log", 2);
        this.BLOCK_SCORE.put("glass", 3);
        this.BLOCK_SCORE.put("stained_glass", 3);
        this.BLOCK_SCORE.put("hardened_clay", 4);
        this.BLOCK_SCORE.put("stained_hardened_clay", 4);
        this.BLOCK_SCORE.put("cloth", 5);
    }

    @Override
    public void onEnabled() {
        if (mc.field_71439_g != null) {
            this.serverYaw = mc.field_71439_g.field_70177_z;
            this.serverPitch = mc.field_71439_g.field_70125_A;
            this.aimYaw = this.serverYaw;
            this.aimPitch = this.serverPitch;
            this.progress = 0.0F;
            this.lastSlot = mc.field_71439_g.field_71071_by.field_70461_c;
            this.targetBlock = null;
            this.targetFacing = null;
            this.targetHitVec = null;
            this.lastPlaceTime = 0L;
            this.startPos = mc.field_71439_g.func_180425_c();
        }
    }

    @Override
    public void onDisabled() {
        if (this.lastSlot != -1
            && mc.field_71439_g != null
            && mc.field_71439_g.field_71071_by.field_70461_c != this.lastSlot) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.lastSlot;
        }

        this.progress = 0.0F;
        this.targetBlock = null;
        this.targetFacing = null;
        this.targetHitVec = null;
    }

    @EventTarget(1)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.PRE) {
                if (mc.field_71439_g != null && mc.field_71441_e != null) {
                    if (mc.field_71462_r == null) {
                        this.serverYaw = event.getYaw();
                        this.serverPitch = event.getPitch();
                        if (this.disableOnMove.getValue()
                            && this.startPos != null
                            && !mc.field_71439_g.func_180425_c().equals(this.startPos)) {
                            this.toggle();
                        } else {
                            this.updateProgress();
                            if (this.autoDisable.getValue() && this.progress >= 1.0F) {
                                this.toggle();
                            } else {
                                if (!ItemUtil.isHoldingSword()) {
                                    boolean adjacent = this.targetBlock != null
                                        && this.isTargetAdjacent(this.targetBlock);
                                    int blockSlot = adjacent
                                        ? this.findBestBlockSlot(true)
                                        : this.findBestBlockSlot(false);
                                    if (blockSlot != -1 && mc.field_71439_g.field_71071_by.field_70461_c != blockSlot) {
                                        mc.field_71439_g.field_71071_by.field_70461_c = blockSlot;
                                    }
                                }

                                ItemStack currentHeld = mc.field_71439_g.field_71071_by.func_70448_g();
                                boolean holdingBlock = currentHeld != null
                                    && currentHeld.func_77973_b() instanceof ItemBlock;
                                if (!holdingBlock) {
                                    this.targetBlock = null;
                                    this.targetFacing = null;
                                    this.targetHitVec = null;
                                } else {
                                    this.findBestPlacement();
                                    if (this.targetBlock != null
                                        && this.targetFacing != null
                                        && this.targetHitVec != null) {
                                        Vec3 eyes = mc.field_71439_g.func_174824_e(1.0F);
                                        double dx = this.targetHitVec.field_72450_a - eyes.field_72450_a;
                                        double dy = this.targetHitVec.field_72448_b - eyes.field_72448_b;
                                        double dz = this.targetHitVec.field_72449_c - eyes.field_72449_c;
                                        double dist = Math.sqrt(dx * dx + dz * dz);
                                        float targetYaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
                                        float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
                                        targetYaw = MathHelper.func_76142_g(targetYaw);
                                        float yawDiff = MathHelper.func_76142_g(targetYaw - this.serverYaw);
                                        float pitchDiff = targetPitch - this.serverPitch;
                                        float maxTurn = this.speed.getValue().floatValue();
                                        float yawStep = MathHelper.func_76131_a(yawDiff, -maxTurn, maxTurn);
                                        float pitchStep = MathHelper.func_76131_a(pitchDiff, -maxTurn, maxTurn);
                                        this.aimYaw = this.serverYaw + yawStep;
                                        this.aimPitch = MathHelper.func_76131_a(
                                            this.serverPitch + pitchStep, -90.0F, 90.0F
                                        );
                                        event.setRotation(this.aimYaw, this.aimPitch, 6);
                                        event.setPervRotation(
                                            this.moveFix.getValue() != 0 ? this.aimYaw : mc.field_71439_g.field_70177_z,
                                            6
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (this.isEnabled()
            && this.moveFix.getValue() == 1
            && RotationState.isActived()
            && RotationState.getPriority() == 6.0F
            && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
        }
    }

    @EventTarget(1)
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.PRE) {
                if (mc.field_71439_g != null && mc.field_71441_e != null) {
                    if (mc.field_71462_r == null) {
                        if (this.targetBlock != null && this.targetFacing != null && this.targetHitVec != null) {
                            if (!this.withinRotationTolerance(this.aimYaw, this.aimPitch)) {
                                return;
                            }

                            long currentTime = System.currentTimeMillis();
                            if (currentTime - this.lastPlaceTime >= this.placeDelay.getValue().intValue()) {
                                this.lastPlaceTime = currentTime;
                                MovingObjectPosition mop = this.rayTraceBlock(
                                    this.aimYaw, this.aimPitch, this.range.getValue().floatValue()
                                );
                                if (mop != null
                                    && mop.field_72313_a == MovingObjectType.BLOCK
                                    && mop.func_178782_a().equals(this.targetBlock)
                                    && mop.field_178784_b == this.targetFacing) {
                                    ItemStack heldStack = mc.field_71439_g.field_71071_by.func_70448_g();
                                    if (heldStack != null && heldStack.func_77973_b() instanceof ItemBlock) {
                                        mc.field_71442_b
                                            .func_178890_a(
                                                mc.field_71439_g,
                                                mc.field_71441_e,
                                                heldStack,
                                                this.targetBlock,
                                                this.targetFacing,
                                                mop.field_72307_f
                                            );
                                        mc.field_71439_g.func_71038_i();
                                        this.targetBlock = null;
                                        this.targetFacing = null;
                                        this.targetHitVec = null;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (this.isEnabled()) {
            this.lastSlot = event.setSlot(this.lastSlot);
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled() && mc.field_71462_r == null) {
            if (this.showProgress.getValue() && mc.field_71466_p != null) {
                if (this.progress != this.lastProgress) {
                    this.animStartProgress = this.progress;
                    this.animTargetProgress = this.progress;
                    this.animStartTime = System.currentTimeMillis();
                    this.lastProgress = this.progress;
                }

                long elapsed = System.currentTimeMillis() - this.animStartTime;
                long animDuration = 250L;
                float displayProgress;
                if (elapsed < 250L) {
                    float t = (float)elapsed / 250.0F;
                    displayProgress = quadInOutEasing(t);
                } else {
                    displayProgress = 1.0F;
                }

                ScaledResolution sr = new ScaledResolution(mc);
                float radius = 10.0F;
                float thickness = 3.0F;
                float cx = sr.func_78326_a() / 2.0F - 1.0F;
                float cy = sr.func_78328_b() / 2.0F;
                drawCircle(cx, cy, radius, 80, thickness, 0.0F, 0.0F, 0.0F, 0.4F);
                if (this.progress >= 0.999F) {
                    drawCircle(cx, cy, radius, 80, thickness, 0.0F, 1.0F, 0.0F, 1.0F);
                } else {
                    float ratio = Math.max(0.0F, Math.min(1.0F, this.progress * displayProgress));
                    float startAngle = 90.0F;
                    float endAngle = startAngle + ratio * 360.0F + 0.5F;
                    int r = (int)((1.0F - ratio) * 255.0F + 0.5F);
                    int g = (int)(ratio * 255.0F + 0.5F);
                    r = Math.max(0, Math.min(255, r));
                    g = Math.max(0, Math.min(255, g));
                    int color = 0xFF000000 | r << 16 | g << 8 | 0;
                    drawCircleArc(cx, cy, radius, startAngle, endAngle, thickness, color);
                }
            }
        }
    }

    private int findBestBlockSlot(boolean preferStrong) {
        int bestSlot = -1;
        int bestScore = preferStrong ? Integer.MAX_VALUE : Integer.MIN_VALUE;

        for (int slot = 0; slot <= 8; slot++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(slot);
            if (stack != null && stack.field_77994_a != 0 && stack.func_77973_b() instanceof ItemBlock) {
                Block block = ((ItemBlock)stack.func_77973_b()).func_179223_d();
                String blockName = block.func_149739_a().replace("tile.", "");
                Integer score = this.BLOCK_SCORE.get(blockName);
                if (score != null && (preferStrong ? score < bestScore : score > bestScore)) {
                    bestScore = score;
                    bestSlot = slot;
                    if (preferStrong && score == 0) {
                        break;
                    }
                }
            }
        }

        return bestSlot;
    }

    private void findBestPlacement() {
        Vec3 playerPos = mc.field_71439_g.func_174791_d();
        BlockPos feetPos = new BlockPos(playerPos.field_72450_a, playerPos.field_72448_b, playerPos.field_72449_c);
        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        double reach = this.range.getValue().doubleValue();
        double reachSq = reach * reach;
        double rp12 = (reach + 1.0) * (reach + 1.0);
        BlockPos roofTarget = feetPos.func_177981_b(2);
        if (!this.isAir(roofTarget)) {
            this.sidesAim(eye, reach, feetPos);
        } else {
            List<AutoBlockIn.BlockData> supports = new ArrayList<>();
            int minX = (int)Math.floor(eye.field_72450_a - reach);
            int maxX = (int)Math.floor(eye.field_72450_a + reach);
            int minY = (int)Math.floor(eye.field_72448_b - 1.0);
            int maxY = (int)Math.floor(eye.field_72448_b + reach);
            int minZ = (int)Math.floor(eye.field_72449_c - reach);
            int maxZ = (int)Math.floor(eye.field_72449_c + reach);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (!this.isAir(p)) {
                            double dx = x + 0.5 - eye.field_72450_a;
                            double dy = y + 0.5 - eye.field_72448_b;
                            double dz = z + 0.5 - eye.field_72449_c;
                            if (!(dx * dx + dy * dy + dz * dz > rp12)) {
                                double d2 = this.dist2PointAABB(eye, x, y, z);
                                if (!(d2 > reachSq)) {
                                    Vec3 mid = new Vec3(x + 0.5, y + 0.5, z + 0.5);
                                    MovingObjectPosition mop = mc.field_71441_e
                                        .func_147447_a(eye, mid, false, false, false);
                                    if (mop != null && mop.func_178782_a().equals(p)) {
                                        supports.add(new AutoBlockIn.BlockData(p, d2));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (supports.isEmpty()) {
                this.sidesAim(eye, reach, feetPos);
            } else {
                supports.sort(Comparator.comparingDouble(a -> a.distance));

                for (AutoBlockIn.BlockData bd : supports) {
                    if (this.tryPlaceOnBlock(bd.pos, eye, reach, roofTarget)) {
                        return;
                    }
                }

                Queue<BlockPos> q = new LinkedList<>();
                Map<BlockPos, BlockPos> parent = new HashMap<>();
                Set<BlockPos> visited = new HashSet<>();

                for (AutoBlockIn.BlockData bd : supports) {
                    BlockPos sup = bd.pos;

                    for (EnumFacing f : EnumFacing.values()) {
                        BlockPos node = sup.func_177972_a(f);
                        if (this.isAir(node) && !visited.contains(node)) {
                            visited.add(node);
                            parent.put(node, null);
                            q.add(node);
                        }
                    }
                }

                BlockPos endNode = null;
                int nodesSeen = 0;

                while (!q.isEmpty() && nodesSeen < 8964) {
                    BlockPos cur = q.poll();
                    nodesSeen++;
                    if (cur.func_177951_i(roofTarget) <= 1.5) {
                        endNode = cur;
                        break;
                    }

                    for (EnumFacing f : EnumFacing.values()) {
                        BlockPos nxt = cur.func_177972_a(f);
                        if (!visited.contains(nxt) && this.isAir(nxt)) {
                            visited.add(nxt);
                            parent.put(nxt, cur);
                            q.add(nxt);
                        }
                    }
                }

                if (endNode == null) {
                    this.sidesAim(eye, reach, feetPos);
                } else {
                    List<BlockPos> path = new ArrayList<>();

                    for (BlockPos cur = endNode; cur != null; cur = parent.get(cur)) {
                        path.add(cur);
                    }

                    Collections.reverse(path);

                    for (BlockPos place : path) {
                        if (this.isAir(place)) {
                            boolean placedThis = false;

                            for (AutoBlockIn.BlockData bd : supports) {
                                BlockPos sup = bd.pos;
                                if (this.isAdjacent(sup, place) && this.tryPlaceOnBlock(sup, eye, reach, place)) {
                                    return;
                                }
                            }

                            for (EnumFacing f : EnumFacing.values()) {
                                BlockPos sup = place.func_177972_a(f);
                                if (!this.isAir(sup) && this.tryPlaceOnBlock(sup, eye, reach, place)) {
                                    return;
                                }
                            }

                            if (placedThis) {
                                break;
                            }
                        }
                    }

                    this.sidesAim(eye, reach, feetPos);
                }
            }
        }
    }

    private boolean isAdjacent(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.func_177958_n() - b.func_177958_n());
        int dy = Math.abs(a.func_177956_o() - b.func_177956_o());
        int dz = Math.abs(a.func_177952_p() - b.func_177952_p());
        return dx + dy + dz == 1;
    }

    private boolean tryPlaceOnBlock(BlockPos supportBlock, Vec3 eye, double reach, BlockPos targetPos) {
        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos placementPos = supportBlock.func_177972_a(facing);
            if (placementPos.equals(targetPos)) {
                int n = (int)Math.round(5.0);

                for (int r = 0; r <= n; r++) {
                    double v = r * 0.2 + (Math.random() * 0.020000000000000004 * 2.0 - 0.020000000000000004);
                    if (v < 0.0) {
                        v = 0.0;
                    } else if (v > 1.0) {
                        v = 1.0;
                    }

                    for (int c = 0; c <= n; c++) {
                        double u = c * 0.2 + (Math.random() * 0.020000000000000004 * 2.0 - 0.020000000000000004);
                        if (u < 0.0) {
                            u = 0.0;
                        } else if (u > 1.0) {
                            u = 1.0;
                        }

                        Vec3 hitPos = this.getHitPosOnFace(supportBlock, facing, u, v);
                        float[] rot = this.getRotationsWrapped(
                            eye, hitPos.field_72450_a, hitPos.field_72448_b, hitPos.field_72449_c
                        );
                        MovingObjectPosition mop = this.rayTraceBlock(rot[0], rot[1], reach);
                        if (mop != null
                            && mop.field_72313_a == MovingObjectType.BLOCK
                            && mop.func_178782_a().equals(supportBlock)
                            && mop.field_178784_b == facing) {
                            this.targetBlock = supportBlock;
                            this.targetFacing = facing;
                            this.targetHitVec = mop.field_72307_f;
                            this.aimYaw = rot[0];
                            this.aimPitch = rot[1];
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void sidesAim(Vec3 eye, double reach, BlockPos feetPos) {
        List<BlockPos> goals = new ArrayList<>();

        for (int[] d : DIRS) {
            BlockPos headPos = feetPos.func_177982_a(d[0], 1, d[2]);
            if (this.isAir(headPos)) {
                goals.add(headPos);
            }
        }

        for (int[] d : DIRS) {
            BlockPos feetGoal = feetPos.func_177982_a(d[0], 0, d[2]);
            if (this.isAir(feetGoal)) {
                goals.add(feetGoal);
            }
        }

        if (!goals.isEmpty()) {
            EntityPlayer enemy = this.getClosestEnemy();
            if (enemy != null) {
                goals.sort(Comparator.comparingDouble(p -> p.func_177951_i(enemy.func_180425_c())));
            }

            this.findBestForGoals(goals, eye, reach);
        }
    }

    private void findBestForGoals(List<BlockPos> goals, Vec3 eye, double reach) {
        for (BlockPos goal : goals) {
            for (EnumFacing facing : EnumFacing.values()) {
                BlockPos support = goal.func_177972_a(facing);
                if (!this.isAir(support)) {
                    Vec3 center = new Vec3(
                        support.func_177958_n() + 0.5, support.func_177956_o() + 0.5, support.func_177952_p() + 0.5
                    );
                    if (!(eye.func_72438_d(center) > reach)) {
                        int n = (int)Math.round(5.0);

                        for (int r = 0; r <= n; r++) {
                            double v = r * 0.2 + (Math.random() * 0.020000000000000004 * 2.0 - 0.020000000000000004);
                            if (v < 0.0) {
                                v = 0.0;
                            } else if (v > 1.0) {
                                v = 1.0;
                            }

                            for (int c = 0; c <= n; c++) {
                                double u = c * 0.2
                                    + (Math.random() * 0.020000000000000004 * 2.0 - 0.020000000000000004);
                                if (u < 0.0) {
                                    u = 0.0;
                                } else if (u > 1.0) {
                                    u = 1.0;
                                }

                                Vec3 hitPos = this.getHitPosOnFace(support, facing.func_176734_d(), u, v);
                                float[] rot = this.getRotationsWrapped(
                                    eye, hitPos.field_72450_a, hitPos.field_72448_b, hitPos.field_72449_c
                                );
                                MovingObjectPosition mop = this.rayTraceBlock(rot[0], rot[1], reach);
                                if (mop != null
                                    && mop.field_72313_a == MovingObjectType.BLOCK
                                    && mop.func_178782_a().equals(support)
                                    && mop.field_178784_b == facing.func_176734_d()) {
                                    this.targetBlock = support;
                                    this.targetFacing = facing.func_176734_d();
                                    this.targetHitVec = mop.field_72307_f;
                                    this.aimYaw = rot[0];
                                    this.aimPitch = rot[1];
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Vec3 getHitPosOnFace(BlockPos block, EnumFacing face, double u, double v) {
        double x = block.func_177958_n() + 0.5;
        double y = block.func_177956_o() + 0.5;
        double z = block.func_177952_p() + 0.5;
        switch (face) {
            case DOWN:
                y = block.func_177956_o() + 0.05;
                x = block.func_177958_n() + u;
                z = block.func_177952_p() + v;
                break;
            case UP:
                y = block.func_177956_o() + 1.0 - 0.05;
                x = block.func_177958_n() + u;
                z = block.func_177952_p() + v;
                break;
            case NORTH:
                z = block.func_177952_p() + 0.05;
                x = block.func_177958_n() + u;
                y = block.func_177956_o() + v;
                break;
            case SOUTH:
                z = block.func_177952_p() + 1.0 - 0.05;
                x = block.func_177958_n() + u;
                y = block.func_177956_o() + v;
                break;
            case WEST:
                x = block.func_177958_n() + 0.05;
                z = block.func_177952_p() + u;
                y = block.func_177956_o() + v;
                break;
            case EAST:
                x = block.func_177958_n() + 1.0 - 0.05;
                z = block.func_177952_p() + u;
                y = block.func_177956_o() + v;
        }

        return new Vec3(x, y, z);
    }

    private boolean isAir(BlockPos pos) {
        Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
        return block == Blocks.field_150350_a
            || block == Blocks.field_150355_j
            || block == Blocks.field_150358_i
            || block == Blocks.field_150353_l
            || block == Blocks.field_150356_k
            || block == Blocks.field_150480_ab;
    }

    private void updateProgress() {
        Vec3 playerPos = mc.field_71439_g.func_174791_d();
        BlockPos feetPos = new BlockPos(playerPos.field_72450_a, playerPos.field_72448_b, playerPos.field_72449_c);
        int filled = 0;
        int total = 9;
        if (!this.isAir(feetPos.func_177981_b(2))) {
            filled++;
        }

        for (int[] d : DIRS) {
            if (!this.isAir(feetPos.func_177982_a(d[0], 0, d[2]))) {
                filled++;
            }

            if (!this.isAir(feetPos.func_177982_a(d[0], 1, d[2]))) {
                filled++;
            }
        }

        this.progress = (float)filled / total;
    }

    private MovingObjectPosition rayTraceBlock(float yaw, float pitch, double range) {
        float yawRad = (float)Math.toRadians(yaw);
        float pitchRad = (float)Math.toRadians(pitch);
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        Vec3 start = mc.field_71439_g.func_174824_e(1.0F);
        Vec3 end = start.func_72441_c(x * range, y * range, z * range);
        return mc.field_71441_e.func_72933_a(start, end);
    }

    private boolean withinRotationTolerance(float targetYaw, float targetPitch) {
        float dy = Math.abs(MathHelper.func_76142_g(targetYaw - this.serverYaw));
        float dp = Math.abs(MathHelper.func_76142_g(targetPitch - this.serverPitch));
        return dy <= this.rotationTolerance.getValue().intValue() && dp <= this.rotationTolerance.getValue().intValue();
    }

    private double dist2PointAABB(Vec3 p, int x, int y, int z) {
        double minX = x;
        double maxX = x + 1;
        double minY = y;
        double maxY = y + 1;
        double minZ = z;
        double maxZ = z + 1;
        double cx = this.clamp(p.field_72450_a, minX, maxX);
        double cy = this.clamp(p.field_72448_b, minY, maxY);
        double cz = this.clamp(p.field_72449_c, minZ, maxZ);
        double dx = p.field_72450_a - cx;
        double dy = p.field_72448_b - cy;
        double dz = p.field_72449_c - cz;
        return dx * dx + dy * dy + dz * dz;
    }

    private double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private float[] getRotationsWrapped(Vec3 eye, double tx, double ty, double tz) {
        double dx = tx - eye.field_72450_a;
        double dy = ty - eye.field_72448_b;
        double dz = tz - eye.field_72449_c;
        double hd = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        yaw = this.normYaw(yaw);
        float pitch = (float)Math.toDegrees(-Math.atan2(dy, hd));
        return new float[]{yaw, pitch};
    }

    private float normYaw(float yaw) {
        yaw = (yaw % 360.0F + 360.0F) % 360.0F;
        return yaw > 180.0F ? yaw - 360.0F : yaw;
    }

    private boolean isTargetAdjacent(BlockPos target) {
        Vec3 feet = mc.field_71439_g.func_174791_d();
        int fx = (int)Math.floor(feet.field_72450_a);
        int fy = (int)Math.floor(feet.field_72448_b);
        int fz = (int)Math.floor(feet.field_72449_c);
        int tx = target.func_177958_n();
        int ty = target.func_177956_o();
        int tz = target.func_177952_p();
        if (tx == fx && tz == fz && ty == fy + 2) {
            return true;
        }

        for (int[] d : DIRS) {
            if (tx == fx + d[0] && tz == fz + d[2] && (ty == fy || ty == fy + 1)) {
                return true;
            }
        }

        return false;
    }

    private EntityPlayer getClosestEnemy() {
        Vec3 myPos = mc.field_71439_g.func_174791_d();
        double boxSize = 10.0;
        EntityPlayer best = null;
        double bestDist = Double.POSITIVE_INFINITY;

        for (Object obj : mc.field_71441_e.field_73010_i) {
            EntityPlayer p = (EntityPlayer)obj;
            if (p != mc.field_71439_g && !(p.func_110143_aJ() <= 0.0F)) {
                double dx = p.field_70165_t - myPos.field_72450_a;
                if (!(dx > boxSize) && !(dx < -boxSize)) {
                    double dy = p.field_70163_u - myPos.field_72448_b;
                    if (!(dy > boxSize) && !(dy < -boxSize)) {
                        double dz = p.field_70161_v - myPos.field_72449_c;
                        if (!(dz > boxSize) && !(dz < -boxSize)) {
                            double d2 = dx * dx + dy * dy + dz * dz;
                            if (d2 < bestDist) {
                                bestDist = d2;
                                best = p;
                            }
                        }
                    }
                }
            }
        }

        return best;
    }

    private static float quadInOutEasing(float t) {
        return t < 0.5F ? 2.0F * t * t : -1.0F + (4.0F - 2.0F * t) * t;
    }

    private static void drawCircle(
        float cx, float cy, float radius, int segments, float lineWidth, float r, float g, float b, float a
    ) {
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        GL11.glColor4f(r, g, b, a);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(2);

        for (int i = 0; i <= segments; i++) {
            double theta = (Math.PI * 2) * i / segments;
            float x = (float)(radius * Math.cos(theta)) + cx;
            float y = (float)(radius * Math.sin(theta)) + cy;
            GL11.glVertex2f(x, y);
        }

        GL11.glEnd();
        GL11.glDisable(2848);
        GL11.glEnable(3553);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    private static void drawCircleArc(
        float cx, float cy, float radius, float startAngle, float endAngle, float lineWidth, int color
    ) {
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = (color >> 24 & 0xFF) / 255.0F;
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        GL11.glColor4f(r, g, b, a);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(3);

        for (float angle = startAngle; angle <= endAngle; angle++) {
            double theta = Math.toRadians(angle + 180.0F);
            float x = (float)(radius * Math.cos(theta)) + cx;
            float y = (float)(radius * Math.sin(theta)) + cy;
            GL11.glVertex2f(x, y);
        }

        GL11.glEnd();
        GL11.glDisable(2848);
        GL11.glEnable(3553);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glLineWidth(1.0F);
        GL11.glPopMatrix();
    }

    public int getSlot() {
        return this.lastSlot;
    }

    private static class BlockData {
        BlockPos pos;
        double distance;

        BlockData(BlockPos pos, double distance) {
            this.pos = pos;
            this.distance = distance;
        }
    }
}
