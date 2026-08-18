package miau.module.modules.ghost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.util.client.ChatUtil;
import miau.util.player.RotationUtil;
import miau.util.render.RenderUtil;
import miau.util.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class AutoLadderClutch extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final FloatProperty fallDistance = new FloatProperty("Fall Distance", 3.0F, 2.0F, 10.0F);
    public final FloatProperty reach = new FloatProperty("Reach", 4.5F, 2.0F, 5.5F);
    public final FloatProperty placeDelay = new FloatProperty("Place Delay (ms)", 50.0F, 0.0F, 200.0F);
    public final FloatProperty maxFov = new FloatProperty("FOV", 180.0F, 0.0F, 180.0F);
    public final BooleanProperty wallBuilder = new BooleanProperty("Wall Builder", true);
    public final BooleanProperty autoSneak = new BooleanProperty("Auto Sneak", true);
    public final BooleanProperty legitAutoCenter = new BooleanProperty("Legit Auto Center", true);
    public final BooleanProperty esp = new BooleanProperty("ESP", true);
    public final BooleanProperty debug = new BooleanProperty("Debug", false);
    private final Set<String> BUILDING_BLOCKS = new HashSet<>(
        Arrays.asList(
            "wool",
            "planks",
            "wood",
            "log",
            "log2",
            "stone",
            "cobblestone",
            "glass",
            "stained_glass",
            "clay",
            "hardened_clay",
            "stained_hardened_clay",
            "end_stone",
            "obsidian"
        )
    );
    private static final int ACTION_NONE = 0;
    private static final int ACTION_BASE = 1;
    private static final int ACTION_LADDER = 2;
    private int originalSlot = -1;
    private int actionSlot = -1;
    private int queuedAction = 0;
    private int pendingBaseTicks = 0;
    private boolean isClutching = false;
    private boolean placeQueued = false;
    private boolean hasAim = false;
    private boolean waitingForBase = false;
    private boolean wasCentering = false;
    private long lastPlaceTime = 0L;
    private float aimYaw = 0.0F;
    private float aimPitch = 0.0F;
    private float serverYaw = 0.0F;
    private float serverPitch = 0.0F;
    private double lockX = -1.0;
    private double lockZ = -1.0;
    private BlockPos hitAt = null;
    private Vec3 hitVec = null;
    private EnumFacing hitSide = null;
    private BlockPos targetCell = null;
    private BlockPos pendingBase = null;
    private BlockPos ladderEsp = null;
    private BlockPos baseEsp = null;

    public AutoLadderClutch() {
        super("LadderClutch", false);
    }

    @Override
    public void onEnabled() {
        if (mc.field_71439_g != null) {
            this.serverYaw = mc.field_71439_g.field_70177_z;
            this.serverPitch = mc.field_71439_g.field_70125_A;
        }

        this.resetState();
    }

    @Override
    public void onDisabled() {
        this.resetState();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.SEND && event.getPacket() instanceof C03PacketPlayer) {
                C03PacketPlayer packet = (C03PacketPlayer)event.getPacket();
                if (packet.func_149463_k()) {
                    this.serverYaw = packet.func_149462_g();
                    this.serverPitch = packet.func_149470_h();
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.PRE) {
                if (mc.field_71439_g == null || mc.field_71441_e == null) {
                    this.resetState();
                } else if (!this.isActivationValid()) {
                    this.resetState();
                } else {
                    if (this.originalSlot == -1) {
                        this.originalSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                    }

                    boolean activelyClutching = this.placeQueued || this.waitingForBase || this.isClutching;
                    if (activelyClutching && this.legitAutoCenter.getValue()) {
                        Vec3 pos = mc.field_71439_g.func_174791_d();
                        if (this.lockX == -1.0) {
                            this.lockX = Math.floor(pos.field_72450_a) + 0.5;
                            this.lockZ = Math.floor(pos.field_72449_c) + 0.5;
                            this.wasCentering = true;
                        }

                        double diffX = this.lockX - pos.field_72450_a;
                        double diffZ = this.lockZ - pos.field_72449_c;
                        double distSq = diffX * diffX + diffZ * diffZ;
                        if (distSq > 0.015) {
                            float targetYaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
                            float yawDiff = this.wrapYawDelta(mc.field_71439_g.field_70177_z, targetYaw);
                            this.setKeyBindState(mc.field_71474_y.field_74351_w, Math.abs(yawDiff) < 65.0F);
                            this.setKeyBindState(mc.field_71474_y.field_74368_y, Math.abs(yawDiff) > 115.0F);
                            this.setKeyBindState(mc.field_71474_y.field_74370_x, yawDiff < -25.0F && yawDiff > -155.0F);
                            this.setKeyBindState(mc.field_71474_y.field_74366_z, yawDiff > 25.0F && yawDiff < 155.0F);
                        } else {
                            this.idleKeys();
                        }
                    } else {
                        if (this.wasCentering) {
                            this.idleKeys();
                            this.wasCentering = false;
                        }

                        this.lockX = -1.0;
                        this.lockZ = -1.0;
                    }

                    if (this.waitingForBase) {
                        this.pendingBaseTicks++;
                        if (this.pendingBaseTicks > 8) {
                            this.waitingForBase = false;
                            this.pendingBase = null;
                            this.pendingBaseTicks = 0;
                        }
                    }

                    Float[] rots = this.getRotations();
                    if (rots != null) {
                        event.setRotation(rots[0], rots[1], 10);
                        RotationUtil.serverYaw = rots[0];
                        RotationUtil.serverPitch = rots[1];
                    }

                    if (this.placeQueued) {
                        if (System.currentTimeMillis() - this.lastPlaceTime >= this.placeDelay.getValue().longValue()) {
                            this.placeQueued = false;
                            if (this.actionSlot >= 0
                                && this.actionSlot <= 8
                                && this.hitAt != null
                                && this.hitVec != null
                                && this.hitSide != null) {
                                mc.field_71439_g.field_71071_by.field_70461_c = this.actionSlot;
                                ItemStack stack = mc.field_71439_g.func_70694_bm();
                                if (mc.field_71442_b
                                    .func_178890_a(
                                        mc.field_71439_g,
                                        mc.field_71441_e,
                                        stack,
                                        this.hitAt,
                                        this.hitSide,
                                        this.hitVec
                                    )) {
                                    mc.field_71439_g.func_71038_i();
                                    this.lastPlaceTime = System.currentTimeMillis();
                                    if (this.queuedAction == 1) {
                                        this.waitingForBase = true;
                                        this.pendingBase = this.targetCell;
                                        this.pendingBaseTicks = 0;
                                        this.baseEsp = this.targetCell;
                                        this.debugPrint("Structural wall base created.");
                                    } else if (this.queuedAction == 2) {
                                        this.ladderEsp = this.targetCell;
                                        this.waitingForBase = false;
                                        this.pendingBase = null;
                                        this.pendingBaseTicks = 0;
                                        if (this.autoSneak.getValue()) {
                                            this.setKeyBindState(mc.field_71474_y.field_74311_E, true);
                                            this.isClutching = true;
                                        }

                                        this.debugPrint("Ladder clutch executed.");
                                    }
                                }

                                if (this.originalSlot != -1) {
                                    mc.field_71439_g.field_71071_by.field_70461_c = this.originalSlot;
                                }

                                this.clearQueuedAction();
                            } else {
                                this.clearQueuedAction();
                            }
                        }
                    }
                }
            }
        }
    }

    private Float[] getRotations() {
        if (this.placeQueued && this.hasAim) {
            return new Float[]{this.aimYaw, this.aimPitch};
        }

        this.clearQueuedAction();
        if (this.waitingForBase && this.pendingBase != null) {
            Block baseBlock = BlockUtil.getBlock(this.pendingBase);
            if (BlockUtil.isSolid(baseBlock)) {
                int ladderSlot = this.findLadderSlot();
                if (ladderSlot != -1 && this.prepareLadderOnBase(this.pendingBase, ladderSlot)) {
                    return new Float[]{this.aimYaw, this.aimPitch};
                }
            }

            return null;
        } else {
            int ladderSlot = this.findLadderSlot();
            if (ladderSlot == -1) {
                return null;
            }

            double r = this.reach.getValue().doubleValue();
            MovingObjectPosition wallRay = this.raycastBlock(
                r, mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A
            );
            if (wallRay != null && wallRay.field_72313_a == MovingObjectType.BLOCK) {
                BlockPos wall = wallRay.func_178782_a();
                EnumFacing face = wallRay.field_178784_b;
                if (this.isHorizontal(face)) {
                    BlockPos ladderCell = wall.func_177972_a(face);
                    if (this.isAir(ladderCell) && this.prepareRayPlacement(wallRay, ladderCell, ladderSlot, 2)) {
                        return new Float[]{this.aimYaw, this.aimPitch};
                    }
                }
            }

            if (this.prepareNearbyWallLadder(ladderSlot, r)) {
                return new Float[]{this.aimYaw, this.aimPitch};
            }

            if (!this.wallBuilder.getValue()) {
                return null;
            }

            BlockPos playerCell = new BlockPos(
                mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v
            );
            BlockPos ladderCell = playerCell.func_177967_a(EnumFacing.DOWN, 2);
            int blockSlot = this.findBuildingBlockSlot();
            if (blockSlot == -1) {
                return null;
            }

            EnumFacing preferred = this.getPlayerFacing(mc.field_71439_g.field_70177_z);
            EnumFacing[] faces = this.orderedHorizontalFaces(preferred);
            BlockPos bestStructureCell = null;
            Object[] bestSupport = null;

            for (EnumFacing face : faces) {
                BlockPos cell = ladderCell.func_177972_a(face);
                if (this.isAir(cell)) {
                    Object[] support = this.findPlacementSupport(cell);
                    if (support != null) {
                        bestStructureCell = cell;
                        bestSupport = support;
                        break;
                    }
                }
            }

            if (bestStructureCell == null) {
                return null;
            }

            BlockPos supportBlock = (BlockPos)bestSupport[0];
            EnumFacing supportFace = (EnumFacing)bestSupport[1];
            this.baseEsp = bestStructureCell;
            return this.prepareManualPlacement(supportBlock, supportFace, bestStructureCell, blockSlot, 1)
                ? new Float[]{this.aimYaw, this.aimPitch}
                : null;
        }
    }

    private float normYaw(float yaw) {
        yaw = (yaw % 360.0F + 360.0F) % 360.0F;
        return yaw > 180.0F ? yaw - 360.0F : yaw;
    }

    private float wrapYawDelta(float base, float target) {
        float d = target - base;

        while (d <= -180.0F) {
            d += 360.0F;
        }

        while (d > 180.0F) {
            d -= 360.0F;
        }

        return d;
    }

    private float unwrapYaw(float yaw, float prevYaw) {
        return prevYaw + (((yaw - prevYaw + 180.0F) % 360.0F + 360.0F) % 360.0F - 180.0F);
    }

    private float[] getRotationsWrapped(Vec3 eye, double tx, double ty, double tz) {
        double dx = tx - eye.field_72450_a;
        double dy = ty - eye.field_72448_b;
        double dz = tz - eye.field_72449_c;
        double hd = Math.sqrt(dx * dx + dz * dz);
        float yawWrapped = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        yawWrapped = this.normYaw(yawWrapped);
        float pitch = (float)Math.toDegrees(-Math.atan2(dy, hd));
        return new float[]{yawWrapped, pitch};
    }

    private Object[] findBestRotation(BlockPos support, EnumFacing face) {
        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        float curYawW = this.normYaw(this.serverYaw);
        float curPit = this.serverPitch;
        float cliYawW = this.normYaw(mc.field_71439_g.field_70177_z);
        float cliPit = mc.field_71439_g.field_70125_A;
        double INSET = 0.05;
        double STEP = 0.2;
        double JIT = 0.2;
        double insetTop = 1.0 - INSET - 0.001;
        double insetBot = INSET + 0.001;
        int GRID = (int)Math.round(1.0 / STEP);
        float fov = this.maxFov.getValue();
        ArrayList<Object[]> cands = new ArrayList<>();

        for (int rr = 0; rr <= GRID; rr++) {
            boolean ltr = (rr & 1) == 0;
            double v = rr * STEP + (Math.random() * STEP * JIT * 2.0 - STEP * JIT);
            if (v < 0.0) {
                v = 0.0;
            } else if (v > 1.0) {
                v = 1.0;
            }

            for (int cc = 0; cc <= GRID; cc++) {
                double cu = cc * STEP + (Math.random() * STEP * JIT * 2.0 - STEP * JIT);
                if (cu < 0.0) {
                    cu = 0.0;
                } else if (cu > 1.0) {
                    cu = 1.0;
                }

                double u = ltr ? cu : 1.0 - cu;
                double px = support.func_177958_n();
                double py = support.func_177956_o();
                double pz = support.func_177952_p();
                if (face == EnumFacing.UP) {
                    px += u;
                    pz += v;
                    py += insetTop;
                } else if (face == EnumFacing.DOWN) {
                    px += u;
                    pz += v;
                    py += insetBot;
                } else if (face == EnumFacing.SOUTH) {
                    px += u;
                    py += v;
                    pz += insetTop;
                } else if (face == EnumFacing.NORTH) {
                    px += u;
                    py += v;
                    pz += insetBot;
                } else if (face == EnumFacing.EAST) {
                    pz += u;
                    py += v;
                    px += insetTop;
                } else {
                    if (face != EnumFacing.WEST) {
                        continue;
                    }

                    pz += u;
                    py += v;
                    px += insetBot;
                }

                float[] rotW = this.getRotationsWrapped(eye, px, py, pz);
                float yawW = rotW[0];
                float pit = rotW[1];
                if (!(Math.abs(this.wrapYawDelta(cliYawW, yawW)) > fov)
                    && !(Math.abs(pit - cliPit) > 90.0F)
                    && !(Math.abs(pit) > 90.0F)) {
                    double cost = Math.abs((double)this.wrapYawDelta(curYawW, yawW)) + Math.abs((double)(pit - curPit));
                    cands.add(new Object[]{cost, yawW, pit, new Vec3(px, py, pz)});
                }
            }
        }

        if (cands.isEmpty()) {
            return null;
        }

        cands.sort((a, b) -> Double.compare((Double)a[0], (Double)b[0]));
        double r = this.reach.getValue().doubleValue();

        for (Object[] cand : cands) {
            float yawW = (Float)cand[1];
            float pit = (Float)cand[2];
            float yawUnwrapped = this.unwrapYaw(yawW, this.serverYaw);
            Vec3 hitPoint = (Vec3)cand[3];
            MovingObjectPosition verified = this.raycastBlock(r, yawUnwrapped, pit);
            if (verified != null
                && verified.field_72313_a == MovingObjectType.BLOCK
                && verified.func_178782_a().equals(support)
                && face == verified.field_178784_b) {
                return new Object[]{yawUnwrapped, pit, hitPoint};
            }
        }

        return null;
    }

    private boolean prepareManualPlacement(BlockPos block, EnumFacing face, BlockPos placeCell, int slot, int action) {
        Object[] bestRot = this.findBestRotation(block, face);
        if (bestRot == null) {
            return false;
        }

        this.hitAt = block;
        this.aimYaw = (Float)bestRot[0];
        this.aimPitch = (Float)bestRot[1];
        this.hitVec = (Vec3)bestRot[2];
        this.hitSide = face;
        this.targetCell = placeCell;
        this.actionSlot = slot;
        this.queuedAction = action;
        this.hasAim = true;
        this.placeQueued = true;
        return true;
    }

    private boolean prepareRayPlacement(MovingObjectPosition ray, BlockPos placeCell, int slot, int action) {
        return this.prepareManualPlacement(ray.func_178782_a(), ray.field_178784_b, placeCell, slot, action);
    }

    private boolean prepareNearbyWallLadder(int ladderSlot, double reach) {
        Vec3 playerPosition = mc.field_71439_g.func_174791_d();
        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        int centerX = (int)Math.floor(playerPosition.field_72450_a);
        int centerY = (int)Math.floor(playerPosition.field_72448_b);
        int centerZ = (int)Math.floor(playerPosition.field_72449_c);
        int radius = (int)Math.ceil(reach);
        ArrayList<Object[]> candidates = new ArrayList<>();
        EnumFacing[] faces = new EnumFacing[]{EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST};

        for (int y = centerY + 1; y >= centerY - 3; y--) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    BlockPos support = new BlockPos(x, y, z);
                    Block block = BlockUtil.getBlock(support);
                    if (BlockUtil.isSolid(block)) {
                        for (EnumFacing face : faces) {
                            BlockPos ladderCell = support.func_177972_a(face);
                            if (this.isAir(ladderCell)
                                && ladderCell.func_177958_n() == centerX
                                && ladderCell.func_177952_p() == centerZ) {
                                Vec3 point = this.facePoint(support, face);
                                double distanceSq = eye.func_72436_e(point);
                                if (!(distanceSq > reach * reach)) {
                                    double verticalPenalty = Math.abs(
                                            ladderCell.func_177956_o() - playerPosition.field_72448_b
                                        )
                                        * 0.35;
                                    double score = distanceSq + verticalPenalty;
                                    candidates.add(new Object[]{score, support, face, ladderCell});
                                }
                            }
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return false;
        }

        candidates.sort((a, b) -> Double.compare(((Number)a[0]).doubleValue(), ((Number)b[0]).doubleValue()));

        for (Object[] candidate : candidates) {
            BlockPos support = (BlockPos)candidate[1];
            EnumFacing face = (EnumFacing)candidate[2];
            BlockPos ladderCell = (BlockPos)candidate[3];
            if (this.prepareManualPlacement(support, face, ladderCell, ladderSlot, 2)) {
                return true;
            }
        }

        return false;
    }

    private boolean prepareLadderOnBase(BlockPos base, int ladderSlot) {
        EnumFacing preferred = this.faceTowardPlayer(base);
        EnumFacing[] faces = this.orderedHorizontalFaces(preferred);

        for (EnumFacing face : faces) {
            BlockPos ladderCell = base.func_177972_a(face);
            if (this.isAir(ladderCell) && this.prepareManualPlacement(base, face, ladderCell, ladderSlot, 2)) {
                return true;
            }
        }

        return false;
    }

    private Object[] findPlacementSupport(BlockPos target) {
        EnumFacing[] directions = new EnumFacing[]{
            EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST
        };

        for (EnumFacing direction : directions) {
            BlockPos support = target.func_177972_a(direction);
            Block block = BlockUtil.getBlock(support);
            if (BlockUtil.isSolid(block)) {
                EnumFacing clickedFace = direction.func_176734_d();
                return new Object[]{support, clickedFace};
            }
        }

        return null;
    }

    private int findLadderSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (stack != null && stack.field_77994_a > 0 && stack.func_77973_b().func_77658_a().contains("ladder")) {
                return i;
            }
        }

        return -1;
    }

    private int findBuildingBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (stack != null
                && stack.field_77994_a >= 3
                && stack.func_77973_b() instanceof ItemBlock
                && !stack.func_77973_b().func_77658_a().contains("ladder")) {
                for (String allowed : this.BUILDING_BLOCKS) {
                    if (stack.func_77973_b().func_77658_a().contains(allowed)) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    private boolean isActivationValid() {
        if (mc.field_71462_r != null) {
            return false;
        } else if (mc.field_71439_g.field_70181_x >= -0.1) {
            return false;
        } else if (mc.field_71439_g.field_70122_E) {
            return false;
        } else {
            return !mc.field_71439_g.func_70090_H() && !mc.field_71439_g.func_180799_ab()
                ? mc.field_71439_g.field_70143_R >= this.fallDistance.getValue()
                : false;
        }
    }

    private boolean isAir(BlockPos position) {
        Block block = BlockUtil.getBlock(position);
        return block instanceof BlockAir;
    }

    private boolean isHorizontal(EnumFacing face) {
        return face == EnumFacing.NORTH
            || face == EnumFacing.SOUTH
            || face == EnumFacing.EAST
            || face == EnumFacing.WEST;
    }

    private EnumFacing getPlayerFacing(float yaw) {
        float normalized = (yaw % 360.0F + 360.0F) % 360.0F;
        if (normalized < 45.0F || normalized >= 315.0F) {
            return EnumFacing.SOUTH;
        } else if (normalized < 135.0F) {
            return EnumFacing.WEST;
        } else {
            return normalized < 225.0F ? EnumFacing.NORTH : EnumFacing.EAST;
        }
    }

    private EnumFacing faceTowardPlayer(BlockPos base) {
        double dx = mc.field_71439_g.field_70165_t - (base.func_177958_n() + 0.5);
        double dz = mc.field_71439_g.field_70161_v - (base.func_177952_p() + 0.5);
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx >= 0.0 ? EnumFacing.EAST : EnumFacing.WEST;
        } else {
            return dz >= 0.0 ? EnumFacing.SOUTH : EnumFacing.NORTH;
        }
    }

    private EnumFacing[] orderedHorizontalFaces(EnumFacing preferred) {
        if (preferred == EnumFacing.NORTH) {
            return new EnumFacing[]{EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.SOUTH};
        } else if (preferred == EnumFacing.SOUTH) {
            return new EnumFacing[]{EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.NORTH};
        } else {
            return preferred == EnumFacing.EAST
                ? new EnumFacing[]{EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.WEST}
                : new EnumFacing[]{EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST};
        }
    }

    private Vec3 facePoint(BlockPos block, EnumFacing face) {
        return new Vec3(
            block.func_177958_n() + 0.5 + face.func_82601_c() * 0.5,
            block.func_177956_o() + 0.5 + face.func_96559_d() * 0.5,
            block.func_177952_p() + 0.5 + face.func_82599_e() * 0.5
        );
    }

    private void clearQueuedAction() {
        this.placeQueued = false;
        this.hasAim = false;
        this.actionSlot = -1;
        this.queuedAction = 0;
        this.hitAt = null;
        this.hitVec = null;
        this.hitSide = null;
        this.targetCell = null;
    }

    private void resetState() {
        if (this.originalSlot != -1 && mc.field_71439_g != null) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.originalSlot;
        }

        if (this.isClutching) {
            this.setKeyBindState(mc.field_71474_y.field_74311_E, false);
        }

        if (this.wasCentering) {
            this.idleKeys();
        }

        this.originalSlot = -1;
        this.isClutching = false;
        this.waitingForBase = false;
        this.wasCentering = false;
        this.pendingBase = null;
        this.pendingBaseTicks = 0;
        this.ladderEsp = null;
        this.baseEsp = null;
        this.lockX = -1.0;
        this.lockZ = -1.0;
        this.clearQueuedAction();
    }

    private void idleKeys() {
        this.setKeyBindState(mc.field_71474_y.field_74351_w, false);
        this.setKeyBindState(mc.field_71474_y.field_74368_y, false);
        this.setKeyBindState(mc.field_71474_y.field_74370_x, false);
        this.setKeyBindState(mc.field_71474_y.field_74366_z, false);
    }

    private void setKeyBindState(KeyBinding binding, boolean pressed) {
        KeyBinding.func_74510_a(binding.func_151463_i(), pressed);
    }

    private MovingObjectPosition raycastBlock(double distance, float yaw, float pitch) {
        Vec3 eyePos = mc.field_71439_g.func_174824_e(1.0F);
        Vec3 lookVec = this.getVectorForRotation(pitch, yaw);
        Vec3 targetPos = eyePos.func_72441_c(
            lookVec.field_72450_a * distance, lookVec.field_72448_b * distance, lookVec.field_72449_c * distance
        );
        return mc.field_71441_e.func_147447_a(eyePos, targetPos, false, false, true);
    }

    private Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.func_76134_b(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f1 = MathHelper.func_76126_a(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f2 = -MathHelper.func_76134_b(-pitch * (float) (Math.PI / 180.0));
        float f3 = MathHelper.func_76126_a(-pitch * (float) (Math.PI / 180.0));
        return new Vec3(f1 * f2, f3, f * f2);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled()) {
            if (this.esp.getValue()) {
                if (this.baseEsp != null) {
                    RenderUtil.drawBlockBoundingBox(this.baseEsp, 1.0, 204, 255, 153, 153, 2.0F);
                    RenderUtil.drawBlockBox(this.baseEsp, 1.0, 255, 153, 0);
                }

                if (this.ladderEsp != null) {
                    RenderUtil.drawBlockBoundingBox(this.ladderEsp, 1.0, 51, 255, 85, 204, 2.0F);
                    RenderUtil.drawBlockBox(this.ladderEsp, 1.0, 51, 255, 85);
                }
            }
        }
    }

    private void debugPrint(String message) {
        if (this.debug.getValue()) {
            ChatUtil.display("§8[§bAutoLadder§8] " + message);
        }
    }
}
