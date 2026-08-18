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
import miau.util.player.RotationUtil;
import miau.util.render.RenderUtil;
import miau.util.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockLiquid;
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

public class BlockLadder extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final Set<String> PLACE_THROUGH = new HashSet<>(
        Arrays.asList("air", "water", "flowing_water", "lava", "flowing_lava", "fire")
    );
    private final Set<String> BLOCKS = new HashSet<>(
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
    public final FloatProperty fallDistance = new FloatProperty("Fall Distance (blocks)", 4.0F, 2.0F, 10.0F);
    public final FloatProperty reach = new FloatProperty("Reach (blocks)", 4.5F, 2.0F, 4.5F);
    public final FloatProperty placeDelay = new FloatProperty("Place Delay (ms)", 70.0F, 0.0F, 200.0F);
    public final BooleanProperty autoCenter = new BooleanProperty("Auto Center", true);
    public final BooleanProperty esp = new BooleanProperty("ESP", true);
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
    private long nextRandomDelay = 0L;
    private float aimYaw = 0.0F;
    private float aimPitch = 0.0F;
    private float serverYaw = 0.0F;
    private float serverPitch = 0.0F;
    private Vec3 hitAt = null;
    private Vec3 hitVec = null;
    private EnumFacing hitSide = null;
    private Vec3 targetCell = null;
    private Vec3 pendingBase = null;
    private Vec3 lockedStructureCell = null;
    private Object[] lockedSupport = null;
    private Vec3 lockedLandingESP = null;
    private Vec3 blueLandingESP = null;
    private Vec3 orangeSupportESP = null;
    private Vec3 greenLadderESP = null;

    public BlockLadder() {
        super("BlockLadder", false);
    }

    @Override
    public void onEnabled() {
        if (mc.field_71439_g != null) {
            this.serverYaw = mc.field_71439_g.field_70177_z;
            this.serverPitch = mc.field_71439_g.field_70125_A;
        }

        this.nextRandomDelay = this.placeDelay.getValue().longValue();
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
                if (mc.field_71439_g != null && mc.field_71441_e != null) {
                    if (this.findLadderSlot() == -1) {
                        if (this.wasCentering
                            || this.waitingForBase
                            || this.placeQueued
                            || this.isClutching
                            || this.lockedStructureCell != null) {
                            this.resetState();
                        }
                    } else {
                        if (this.originalSlot == -1) {
                            this.originalSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                        }

                        double fallThreshold = this.fallDistance.getValue().doubleValue();
                        boolean isFalling = mc.field_71439_g.field_70143_R >= fallThreshold
                            && !mc.field_71439_g.field_70122_E
                            && !mc.field_71439_g.func_70090_H()
                            && !mc.field_71439_g.func_180799_ab();
                        if (!isFalling) {
                            if (this.wasCentering
                                || this.waitingForBase
                                || this.placeQueued
                                || this.isClutching
                                || this.lockedStructureCell != null) {
                                this.resetState();
                            }
                        } else {
                            Float[] rots = this.getRotations();
                            if (rots != null) {
                                event.setRotation(rots[0], rots[1], 10);
                                RotationUtil.serverYaw = rots[0];
                                RotationUtil.serverPitch = rots[1];
                            }

                            if (this.lockedLandingESP != null) {
                                double dx = mc.field_71439_g.field_70165_t
                                    - (this.lockedLandingESP.field_72450_a + 0.5);
                                double dz = mc.field_71439_g.field_70161_v
                                    - (this.lockedLandingESP.field_72449_c + 0.5);
                                if (dx * dx + dz * dz > 6.25) {
                                    this.lockedLandingESP = null;
                                    this.blueLandingESP = null;
                                    this.lockedStructureCell = null;
                                    this.lockedSupport = null;
                                }
                            }

                            if (this.lockedLandingESP == null) {
                                Vec3 predictedLanding = null;
                                double px = Math.floor(mc.field_71439_g.field_70165_t);
                                double py = Math.floor(mc.field_71439_g.field_70163_u);
                                double pz = Math.floor(mc.field_71439_g.field_70161_v);
                                double vx = mc.field_71439_g.field_70159_w;
                                double vy = mc.field_71439_g.field_70181_x;
                                double vz = mc.field_71439_g.field_70179_y;

                                for (int t = 0; t < 100; t++) {
                                    vy -= 0.08;
                                    px += vx;
                                    py += vy;
                                    pz += vz;
                                    vy *= 0.98F;
                                    vx *= 0.91;
                                    vz *= 0.91;
                                    if (py < 0.0 || this.isSolid(this.worldBlock(new BlockPos(px, py, pz)))) {
                                        predictedLanding = new Vec3(Math.floor(px) + 0.5, py, Math.floor(pz) + 0.5);
                                        break;
                                    }
                                }

                                if (predictedLanding != null) {
                                    this.blueLandingESP = new Vec3(
                                        Math.floor(predictedLanding.field_72450_a),
                                        Math.floor(predictedLanding.field_72448_b),
                                        Math.floor(predictedLanding.field_72449_c)
                                    );
                                } else {
                                    this.blueLandingESP = null;
                                }
                            } else {
                                this.blueLandingESP = this.lockedLandingESP;
                            }

                            if (this.blueLandingESP != null) {
                                Vec3 candidateSupport = null;
                                Vec3 candidateLadder = null;
                                EnumFacing candidateFace = null;
                                if (this.lockedStructureCell == null) {
                                    double staticPy = Math.floor(mc.field_71439_g.field_70163_u);

                                    for (int drop = 1; drop <= 6; drop++) {
                                        Vec3 scanCell = new Vec3(
                                            this.blueLandingESP.field_72450_a,
                                            staticPy - drop,
                                            this.blueLandingESP.field_72449_c
                                        );
                                        EnumFacing[] faces = this.orderedHorizontalFaces(
                                            this.getPlayerFacing(mc.field_71439_g.field_70177_z)
                                        );

                                        for (EnumFacing face : faces) {
                                            Vec3 adjCell = this.offsetCell(scanCell, face);
                                            if (this.isAirCell(adjCell)) {
                                                Object[] support = this.findPlacementSupport(adjCell);
                                                if (support != null) {
                                                    candidateSupport = (Vec3)support[0];
                                                    candidateLadder = adjCell;
                                                    candidateFace = (EnumFacing)support[1];
                                                    break;
                                                }
                                            }
                                        }

                                        if (candidateSupport != null) {
                                            break;
                                        }
                                    }
                                }

                                if (candidateSupport != null && this.lockedStructureCell == null) {
                                    double deltaX = Math.abs(
                                        candidateLadder.field_72450_a - this.blueLandingESP.field_72450_a
                                    );
                                    double deltaZ = Math.abs(
                                        candidateLadder.field_72449_c - this.blueLandingESP.field_72449_c
                                    );
                                    if (deltaX <= 1.0 && deltaZ <= 1.0 && deltaX + deltaZ <= 2.0) {
                                        this.orangeSupportESP = candidateSupport;
                                        this.greenLadderESP = candidateLadder;
                                        this.lockedStructureCell = candidateLadder;
                                        this.lockedSupport = new Object[]{candidateSupport, candidateFace};
                                        this.lockedLandingESP = this.blueLandingESP;
                                    }
                                }

                                if (this.autoCenter.getValue()) {
                                    Vec3 alignTarget = this.blueLandingESP;
                                    if (alignTarget != null) {
                                        double targetX = alignTarget.field_72450_a + 0.5;
                                        double targetZ = alignTarget.field_72449_c + 0.5;
                                        if (this.lockedSupport != null) {
                                            Vec3 supportBlock = (Vec3)this.lockedSupport[0];
                                            double dxSup = targetX - (supportBlock.field_72450_a + 0.5);
                                            double dzSup = targetZ - (supportBlock.field_72449_c + 0.5);
                                            double lenSup = Math.sqrt(dxSup * dxSup + dzSup * dzSup);
                                            if (lenSup > 0.0) {
                                                targetX += dxSup / lenSup * 0.4;
                                                targetZ += dzSup / lenSup * 0.4;
                                            }
                                        }

                                        double diffX = targetX - Math.floor(mc.field_71439_g.field_70165_t);
                                        double diffZ = targetZ - Math.floor(mc.field_71439_g.field_70161_v);
                                        double distSq = diffX * diffX + diffZ * diffZ;
                                        if (distSq > 0.04) {
                                            double dist = Math.sqrt(distSq);
                                            double motionX = mc.field_71439_g.field_70159_w;
                                            double motionZ = mc.field_71439_g.field_70179_y;
                                            double speed = Math.sqrt(motionX * motionX + motionZ * motionZ);
                                            float targetYaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
                                            float yawDiff = this.wrapYawDelta(mc.field_71439_g.field_70177_z, targetYaw);
                                            boolean pressF = yawDiff > -90.0F && yawDiff < 90.0F;
                                            boolean pressB = yawDiff >= 90.0F || yawDiff <= -90.0F;
                                            boolean pressR = yawDiff > 0.0F;
                                            boolean pressL = yawDiff < 0.0F;
                                            if (dist < speed * 3.0 && speed > 0.05) {
                                                float motionYaw = (float)Math.toDegrees(Math.atan2(motionZ, motionX))
                                                    - 90.0F;
                                                float motionYawDiff = this.wrapYawDelta(
                                                    mc.field_71439_g.field_70177_z, motionYaw
                                                );
                                                pressF = false;
                                                pressB = false;
                                                pressL = false;
                                                pressR = false;
                                                if (motionYawDiff >= 135.0F || motionYawDiff <= -135.0F) {
                                                    pressF = true;
                                                } else if (motionYawDiff >= 45.0F && motionYawDiff < 135.0F) {
                                                    pressL = true;
                                                } else if (motionYawDiff <= -45.0F && motionYawDiff > -135.0F) {
                                                    pressR = true;
                                                } else {
                                                    pressB = true;
                                                }
                                            }

                                            this.setKeyBindState(mc.field_71474_y.field_151444_V, false);
                                            this.setKeyBindState(mc.field_71474_y.field_74351_w, pressF);
                                            this.setKeyBindState(mc.field_71474_y.field_74368_y, pressB);
                                            this.setKeyBindState(mc.field_71474_y.field_74370_x, pressL);
                                            this.setKeyBindState(mc.field_71474_y.field_74366_z, pressR);
                                            this.wasCentering = true;
                                        } else {
                                            this.resetWSAD();
                                        }
                                    }
                                }

                                if (this.waitingForBase) {
                                    this.pendingBaseTicks++;
                                    if (this.pendingBaseTicks > 20) {
                                        this.waitingForBase = false;
                                        this.pendingBase = null;
                                        this.pendingBaseTicks = 0;
                                        this.lockedStructureCell = null;
                                        this.lockedSupport = null;
                                        this.lockedLandingESP = null;
                                    }
                                }

                                if (this.placeQueued) {
                                    if (System.currentTimeMillis() - this.lastPlaceTime >= this.nextRandomDelay) {
                                        this.placeQueued = false;
                                        if (this.actionSlot >= 0
                                            && this.actionSlot <= 8
                                            && this.hitAt != null
                                            && this.hitVec != null
                                            && this.hitSide != null) {
                                            mc.field_71439_g.field_71071_by.field_70461_c = this.actionSlot;
                                            ItemStack stack = mc.field_71439_g.func_70694_bm();
                                            boolean placed = mc.field_71442_b
                                                .func_178890_a(
                                                    mc.field_71439_g,
                                                    mc.field_71441_e,
                                                    stack,
                                                    this.toBlockPos(this.hitAt),
                                                    this.hitSide,
                                                    this.hitVec
                                                );
                                            if (placed) {
                                                mc.field_71439_g.func_71038_i();
                                                this.lastPlaceTime = System.currentTimeMillis();
                                                if (this.queuedAction == 1) {
                                                    this.waitingForBase = true;
                                                    this.pendingBase = this.targetCell;
                                                    this.pendingBaseTicks = 0;
                                                    this.nextRandomDelay = 0L;
                                                } else if (this.queuedAction == 2) {
                                                    this.waitingForBase = false;
                                                    this.pendingBase = null;
                                                    this.pendingBaseTicks = 0;
                                                    this.lockedStructureCell = null;
                                                    this.lockedSupport = null;
                                                    this.lockedLandingESP = null;
                                                    double baseDelay = this.placeDelay.getValue().doubleValue();
                                                    this.nextRandomDelay = (long)(
                                                        baseDelay + (Math.random() * 40.0 - 15.0)
                                                    );
                                                    this.setKeyBindState(mc.field_71474_y.field_74311_E, true);
                                                    this.isClutching = true;
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
                } else {
                    this.resetState();
                }
            }
        }
    }

    private Float[] getRotations() {
        if (mc.field_71439_g == null) {
            return null;
        }

        if (this.findLadderSlot() == -1) {
            return null;
        }

        if (this.placeQueued && this.hasAim) {
            return new Float[]{this.aimYaw, this.aimPitch};
        }

        this.clearQueuedAction();
        if (this.waitingForBase && this.pendingBase != null) {
            int targetItemSlot = this.findLadderSlot();
            if (targetItemSlot != -1 && this.computeSecondaryPlacement(this.pendingBase, targetItemSlot)) {
                return new Float[]{this.aimYaw, this.aimPitch};
            }

            if (this.lockedSupport != null && this.findBuildingBlockSlot() != -1) {
                Vec3 primaryNode = (Vec3)this.lockedSupport[0];
                EnumFacing nodeFace = (EnumFacing)this.lockedSupport[1];
                if (this.calculateVectorAngle(primaryNode, nodeFace, this.pendingBase, this.findBuildingBlockSlot(), 1)
                    )
                 {
                    return new Float[]{this.aimYaw, this.aimPitch};
                }
            }

            return null;
        } else {
            double maxReach = this.reach.getValue().floatValue();
            double fallThreshold = this.fallDistance.getValue().doubleValue();
            boolean fallThresholdMet = mc.field_71439_g.field_70143_R >= fallThreshold;
            if (fallThresholdMet) {
                if (this.lockedStructureCell != null) {
                    Vec3 eyePos = new Vec3(
                        mc.field_71439_g.field_70165_t,
                        mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(),
                        mc.field_71439_g.field_70161_v
                    );
                    Vec3 primaryNode = (Vec3)this.lockedSupport[0];
                    EnumFacing nodeFace = (EnumFacing)this.lockedSupport[1];
                    double currentDist = eyePos.func_72438_d(this.facePoint(primaryNode, nodeFace));
                    if (currentDist <= maxReach) {
                        int primaryItemSlot = this.findBuildingBlockSlot();
                        if (primaryItemSlot != -1
                            && this.calculateVectorAngle(
                                primaryNode, nodeFace, this.lockedStructureCell, primaryItemSlot, 1
                            )) {
                            return new Float[]{this.aimYaw, this.aimPitch};
                        }
                    }
                }

                int targetItemSlot = this.findLadderSlot();
                if (targetItemSlot != -1 && this.computePrimaryPlacement(targetItemSlot, maxReach)) {
                    return new Float[]{this.aimYaw, this.aimPitch};
                }
            }

            return null;
        }
    }

    private float normYaw(float rawAngle) {
        rawAngle = (rawAngle % 360.0F + 360.0F) % 360.0F;
        return rawAngle > 180.0F ? rawAngle - 360.0F : rawAngle;
    }

    private float wrapYawDelta(float angleBase, float angleTarget) {
        float deltaValue = angleTarget - angleBase;

        while (deltaValue <= -180.0F) {
            deltaValue += 360.0F;
        }

        while (deltaValue > 180.0F) {
            deltaValue -= 360.0F;
        }

        return deltaValue;
    }

    private float[] getAngles(Vec3 originEye, double tx, double ty, double tz) {
        double dx = tx - originEye.field_72450_a;
        double dy = ty - originEye.field_72448_b;
        double dz = tz - originEye.field_72449_c;
        double planarDist = Math.sqrt(dx * dx + dz * dz);
        float processedYaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        processedYaw = this.normYaw(processedYaw);
        float processedPitch = (float)Math.toDegrees(-Math.atan2(dy, planarDist));
        return new float[]{processedYaw, processedPitch};
    }

    private Object[] evalGridCoordinates(Vec3 nodeBlock, EnumFacing faceOrientation) {
        Vec3 eyeCoordinate = new Vec3(
            mc.field_71439_g.field_70165_t,
            mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(),
            mc.field_71439_g.field_70161_v
        );
        float baseTrackYaw = this.normYaw(this.serverYaw);
        float baseTrackPitch = this.serverPitch;
        float clientInputYaw = this.normYaw(mc.field_71439_g.field_70177_z);
        float clientInputPitch = mc.field_71439_g.field_70125_A;
        double pad = 0.05;
        double step = 0.2;
        double jitter = 0.2;
        double max = 1.0 - pad - 0.001;
        double min = pad + 0.001;
        int res = (int)Math.round(1.0 / step);
        ArrayList<Object[]> perms = new ArrayList<>();

        for (int r = 0; r <= res; r++) {
            boolean dir = (r & 1) == 0;
            double v = r * step + (Math.random() * 2.0 - 1.0) * step * jitter;
            if (v < 0.0) {
                v = 0.0;
            } else if (v > 1.0) {
                v = 1.0;
            }

            for (int c = 0; c <= res; c++) {
                double rawU = c * step + (Math.random() * 2.0 - 1.0) * step * jitter;
                if (rawU < 0.0) {
                    rawU = 0.0;
                } else if (rawU > 1.0) {
                    rawU = 1.0;
                }

                double u = dir ? rawU : 1.0 - rawU;
                double x = nodeBlock.field_72450_a;
                double y = nodeBlock.field_72448_b;
                double z = nodeBlock.field_72449_c;
                if (faceOrientation == EnumFacing.UP) {
                    x += u;
                    z += v;
                    y += max;
                } else if (faceOrientation == EnumFacing.DOWN) {
                    x += u;
                    z += v;
                    y += min;
                } else if (faceOrientation == EnumFacing.SOUTH) {
                    x += u;
                    y += v;
                    z += max;
                } else if (faceOrientation == EnumFacing.NORTH) {
                    x += u;
                    y += v;
                    z += min;
                } else if (faceOrientation == EnumFacing.EAST) {
                    z += u;
                    y += v;
                    x += max;
                } else {
                    if (faceOrientation != EnumFacing.WEST) {
                        continue;
                    }

                    z += u;
                    y += v;
                    x += min;
                }

                float[] angles = this.getAngles(eyeCoordinate, x, y, z);
                float pYaw = angles[0];
                float pPitch = angles[1];
                if (!(Math.abs(pPitch - clientInputPitch) > 90.0F) && !(Math.abs(pPitch) > 90.0F)) {
                    double score = Math.abs((double)this.wrapYawDelta(baseTrackYaw, pYaw))
                        + Math.abs((double)(pPitch - baseTrackPitch));
                    perms.add(new Object[]{score, pYaw, pPitch, new Vec3(x, y, z)});
                }
            }
        }

        if (perms.isEmpty()) {
            return null;
        }

        perms.sort((a, b) -> Double.compare((Double)a[0], (Double)b[0]));
        double reachDist = this.reach.getValue().floatValue();

        for (Object[] perm : perms) {
            float oYaw = (Float)perm[1];
            float oPitch = (Float)perm[2];
            float realYaw = this.serverYaw + this.wrapYawDelta(this.serverYaw, oYaw);
            Vec3 hitVec = (Vec3)perm[3];
            MovingObjectPosition result = this.raycastBlock(reachDist, realYaw, oPitch);
            if (result != null
                && result.field_72313_a == MovingObjectType.BLOCK
                && this.sameCell(this.toVec3(result.func_178782_a()), nodeBlock)
                && faceOrientation == result.field_178784_b) {
                return new Object[]{realYaw, oPitch, hitVec};
            }
        }

        return null;
    }

    private boolean calculateVectorAngle(
        Vec3 blockTarget, EnumFacing blockFace, Vec3 placementCell, int targetHotbarSlot, int internalActionCode
    ) {
        Object[] evaluationResult = this.evalGridCoordinates(blockTarget, blockFace);
        if (evaluationResult == null) {
            return false;
        }

        this.hitAt = blockTarget;
        this.aimYaw = (Float)evaluationResult[0];
        this.aimPitch = (Float)evaluationResult[1];
        this.hitVec = (Vec3)evaluationResult[2];
        this.hitSide = blockFace;
        this.targetCell = placementCell;
        this.actionSlot = targetHotbarSlot;
        this.queuedAction = internalActionCode;
        this.hasAim = true;
        this.placeQueued = true;
        return true;
    }

    private boolean computePrimaryPlacement(int inventorySlotIndex, double distanceBound) {
        if (this.blueLandingESP == null) {
            return false;
        }

        Vec3 playerPos = new Vec3(
            mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v
        );
        Vec3 eyePos = new Vec3(
            mc.field_71439_g.field_70165_t,
            mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(),
            mc.field_71439_g.field_70161_v
        );
        int tX = (int)Math.floor(this.blueLandingESP.field_72450_a);
        int tZ = (int)Math.floor(this.blueLandingESP.field_72449_c);
        int tY = (int)Math.floor(this.blueLandingESP.field_72448_b);
        ArrayList<Object[]> nodes = new ArrayList<>();
        EnumFacing[] faces = new EnumFacing[]{EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST};

        for (int y = tY + 2; y >= tY - 2; y--) {
            for (int x = tX - 1; x <= tX + 1; x++) {
                for (int z = tZ - 1; z <= tZ + 1; z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    Block b = this.worldBlock(blockPos);
                    if (this.isSolid(b)) {
                        for (EnumFacing face : faces) {
                            BlockPos ladderCell = blockPos.func_177972_a(face);
                            if (this.isAir(ladderCell)
                                && (int)Math.floor(ladderCell.func_177958_n()) == tX
                                && (int)Math.floor(ladderCell.func_177952_p()) == tZ) {
                                Vec3 facePt = this.facePoint(this.toVec3(blockPos), face);
                                double distSq = eyePos.func_72436_e(facePt);
                                if (!(distSq > distanceBound * distanceBound)) {
                                    double weight = Math.abs(ladderCell.func_177956_o() - playerPos.field_72448_b)
                                        * 0.35;
                                    double score = distSq + weight;
                                    nodes.add(new Object[]{score, blockPos, face, ladderCell});
                                }
                            }
                        }
                    }
                }
            }
        }

        if (nodes.isEmpty()) {
            return false;
        }

        nodes.sort((a, bx) -> Double.compare((Double)a[0], (Double)bx[0]));

        for (Object[] node : nodes) {
            if (this.calculateVectorAngle(
                this.toVec3((BlockPos)node[1]),
                (EnumFacing)node[2],
                this.toVec3((BlockPos)node[3]),
                inventorySlotIndex,
                2
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean computeSecondaryPlacement(Vec3 structureBasePosition, int slotMappingIndex) {
        EnumFacing dir = this.faceTowardPlayer(structureBasePosition);
        EnumFacing[] faces = this.orderedHorizontalFaces(dir);

        for (EnumFacing f : faces) {
            Vec3 ladderCell = this.offsetCell(structureBasePosition, f);
            if (this.isAirCell(ladderCell)
                && this.calculateVectorAngle(structureBasePosition, f, ladderCell, slotMappingIndex, 2)) {
                return true;
            }
        }

        return false;
    }

    private Object[] findPlacementSupport(Vec3 blockGridCoordinate) {
        EnumFacing[] dirs = new EnumFacing[]{
            EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST
        };

        for (EnumFacing d : dirs) {
            Vec3 node = this.offsetCell(blockGridCoordinate, d);
            Block b = this.worldBlock(node);
            if (this.isSolid(b)) {
                return new Object[]{node, d.func_176734_d()};
            }
        }

        return null;
    }

    private int findLadderSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack item = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (item != null
                && item.field_77994_a > 0
                && item.func_77973_b().func_77658_a().toLowerCase().contains("ladder")) {
                return i;
            }
        }

        return -1;
    }

    private int findBuildingBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack item = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (item != null
                && item.field_77994_a >= 1
                && item.func_77973_b() instanceof ItemBlock
                && !item.func_77973_b().func_77658_a().toLowerCase().contains("ladder")) {
                String name = item.func_77973_b().func_77658_a().toLowerCase();

                for (String id : this.BLOCKS) {
                    if (name.contains(id)) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    private Block worldBlock(Vec3 pos) {
        return BlockUtil.getBlock(new BlockPos(pos.field_72450_a, pos.field_72448_b, pos.field_72449_c));
    }

    private Block worldBlock(BlockPos pos) {
        return BlockUtil.getBlock(pos);
    }

    private boolean isAirCell(Vec3 pos) {
        Block b = this.worldBlock(pos);
        return b instanceof BlockAir;
    }

    private boolean isAir(BlockPos pos) {
        Block b = this.worldBlock(pos);
        return b instanceof BlockAir;
    }

    private boolean isSolid(Block b) {
        if (b == null) {
            return false;
        } else if (b instanceof BlockAir) {
            return false;
        } else {
            return b instanceof BlockLiquid ? false : !(b instanceof BlockFire);
        }
    }

    private boolean isHorizontal(EnumFacing s) {
        return s == EnumFacing.NORTH || s == EnumFacing.SOUTH || s == EnumFacing.EAST || s == EnumFacing.WEST;
    }

    private Vec3 sideOffset(EnumFacing s) {
        return new Vec3(s.func_82601_c(), s.func_96559_d(), s.func_82599_e());
    }

    private Vec3 offsetCell(Vec3 pos, EnumFacing s) {
        return pos.func_72441_c(s.func_82601_c(), s.func_96559_d(), s.func_82599_e());
    }

    private EnumFacing getPlayerFacing(float yaw) {
        float a = (yaw % 360.0F + 360.0F) % 360.0F;
        if (a < 45.0F || a >= 315.0F) {
            return EnumFacing.SOUTH;
        } else if (a < 135.0F) {
            return EnumFacing.WEST;
        } else {
            return a < 225.0F ? EnumFacing.NORTH : EnumFacing.EAST;
        }
    }

    private EnumFacing faceTowardPlayer(Vec3 blockPos) {
        double dx = mc.field_71439_g.field_70165_t - (blockPos.field_72450_a + 0.5);
        double dz = mc.field_71439_g.field_70161_v - (blockPos.field_72449_c + 0.5);
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx >= 0.0 ? EnumFacing.EAST : EnumFacing.WEST;
        } else {
            return dz >= 0.0 ? EnumFacing.SOUTH : EnumFacing.NORTH;
        }
    }

    private EnumFacing[] orderedHorizontalFaces(EnumFacing primary) {
        if (primary == EnumFacing.NORTH) {
            return new EnumFacing[]{EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.SOUTH};
        } else if (primary == EnumFacing.SOUTH) {
            return new EnumFacing[]{EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.NORTH};
        } else {
            return primary == EnumFacing.EAST
                ? new EnumFacing[]{EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.WEST}
                : new EnumFacing[]{EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST};
        }
    }

    private Vec3 facePoint(Vec3 pos, EnumFacing face) {
        return new Vec3(
            pos.field_72450_a + 0.5 + face.func_82601_c() * 0.5,
            pos.field_72448_b + 0.5 + face.func_96559_d() * 0.5,
            pos.field_72449_c + 0.5 + face.func_82599_e() * 0.5
        );
    }

    private boolean sameCell(Vec3 a, Vec3 b) {
        return a != null && b != null
            ? (int)Math.floor(a.field_72450_a) == (int)Math.floor(b.field_72450_a)
                && (int)Math.floor(a.field_72448_b) == (int)Math.floor(b.field_72448_b)
                && (int)Math.floor(a.field_72449_c) == (int)Math.floor(b.field_72449_c)
            : false;
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

    private void resetWSAD() {
        this.setKeyBindState(mc.field_71474_y.field_151444_V, false);
        this.setKeyBindState(mc.field_71474_y.field_74351_w, false);
        this.setKeyBindState(mc.field_71474_y.field_74368_y, false);
        this.setKeyBindState(mc.field_71474_y.field_74370_x, false);
        this.setKeyBindState(mc.field_71474_y.field_74366_z, false);
    }

    private void resetState() {
        if (this.originalSlot != -1 && mc.field_71439_g != null) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.originalSlot;
        }

        if (this.isClutching) {
            this.setKeyBindState(mc.field_71474_y.field_74311_E, false);
        }

        if (this.wasCentering) {
            this.resetWSAD();
        }

        this.originalSlot = -1;
        this.isClutching = false;
        this.waitingForBase = false;
        this.wasCentering = false;
        this.pendingBase = null;
        this.pendingBaseTicks = 0;
        this.lockedLandingESP = null;
        this.blueLandingESP = null;
        this.orangeSupportESP = null;
        this.greenLadderESP = null;
        this.lockedStructureCell = null;
        this.lockedSupport = null;
        this.clearQueuedAction();
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled()) {
            if (this.esp.getValue()) {
                if (this.blueLandingESP != null) {
                    this.drawEsp(this.blueLandingESP, 570425599, false, true);
                }

                if (this.orangeSupportESP != null) {
                    this.drawEsp(this.orangeSupportESP, 587172864, false, true);
                }

                if (this.greenLadderESP != null) {
                    this.drawEsp(this.greenLadderESP, -1727987968, true, false);
                }
            }
        }
    }

    private void drawEsp(Vec3 pos, int rgb, boolean fill, boolean outline) {
        BlockPos blockPos = new BlockPos(pos.field_72450_a, pos.field_72448_b, pos.field_72449_c);
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        int a = rgb >> 24 & 0xFF;
        if (fill) {
            RenderUtil.drawBlockBox(blockPos, 1.0, r, g, b);
        }

        if (outline) {
            RenderUtil.drawBlockBoundingBox(blockPos, 1.0, r, g, b, a, 1.0F);
        }
    }

    private Vec3 toVec3(BlockPos pos) {
        return new Vec3(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p());
    }

    private BlockPos toBlockPos(Vec3 pos) {
        return new BlockPos(pos.field_72450_a, pos.field_72448_b, pos.field_72449_c);
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

    private void setKeyBindState(KeyBinding binding, boolean pressed) {
        KeyBinding.func_74510_a(binding.func_151463_i(), pressed);
    }
}
