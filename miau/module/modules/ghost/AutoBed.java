package miau.module.modules.ghost;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import miau.event.EventTarget;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.management.RotationState;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class AutoBed extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final String[] BLOCK_NAMES = new String[]{"Wool", "Wood", "Endstone", "Clay", "Glass"};
    private static final Block[] BLOCKS = new Block[]{
        Blocks.field_150325_L,
        Blocks.field_150344_f,
        Blocks.field_150377_bs,
        Blocks.field_150405_ch,
        Blocks.field_150359_w
    };
    public final ModeProperty block1Inside = new ModeProperty("Block 1 Inside", 1, BLOCK_NAMES);
    public final ModeProperty block2Outside = new ModeProperty("Block 2 Outside", 2, BLOCK_NAMES);
    public final ModeProperty layoutMode = new ModeProperty(
        "Layout Mode", 2, new String[]{"Basic 1", "Basic 2", "Basic 3"}
    );
    public final FloatProperty range = new FloatProperty("range", 4.5F, 3.0F, 6.0F);
    public final IntProperty speed = new IntProperty("speed", 30, 5, 100);
    public final IntProperty smooth = new IntProperty("smooth", 50, 0, 100);
    public final IntProperty placeDelay = new IntProperty("place-delay", 50, 0, 300);
    public final IntProperty rotationTolerance = new IntProperty("rotation-tolerance", 5, 1, 45);
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"});
    public final BooleanProperty autoSneak = new BooleanProperty("auto-sneak", true);
    public final BooleanProperty stopSprint = new BooleanProperty("stop-sprint", true);
    public final BooleanProperty autoDisable = new BooleanProperty("auto-disable", true);
    public final BooleanProperty autoSwitch = new BooleanProperty("auto-switch", true);
    public final BooleanProperty rayCastCheck = new BooleanProperty("ray-cast", true);
    public final BooleanProperty silentSwing = new BooleanProperty("silent-swing", false);
    public final BooleanProperty fallbackToWool = new BooleanProperty("fallback-wool", true);
    private final List<BlockPos> bedPositions = new ArrayList<>();
    private final List<BlockPos> targetQueue = new ArrayList<>();
    private final Set<BlockPos> layer1Positions = new HashSet<>();
    private long lastPlaceTime = 0L;
    private int originalSlot = -1;
    private float serverYaw;
    private float serverPitch;
    private float aimYaw;
    private float aimPitch;
    private BlockPos targetBlock;
    private EnumFacing targetFacing;
    private Vec3 targetHitVec;
    private boolean isSneakingSent = false;
    private boolean shouldPlaceNextTick = false;
    private static final double INSET = 0.08;
    private static final double STEP = 0.2;
    private static final double JIT = 0.010000000000000002;

    public AutoBed() {
        super("AutoBed", false);
    }

    @Override
    public void onEnabled() {
        this.bedPositions.clear();
        this.targetQueue.clear();
        this.layer1Positions.clear();
        this.lastPlaceTime = 0L;
        this.targetBlock = null;
        this.targetFacing = null;
        this.targetHitVec = null;
        this.isSneakingSent = false;
        this.shouldPlaceNextTick = false;
        if (mc.field_71439_g != null) {
            this.originalSlot = mc.field_71439_g.field_71071_by.field_70461_c;
            this.serverYaw = mc.field_71439_g.field_70177_z;
            this.serverPitch = mc.field_71439_g.field_70125_A;
            this.aimYaw = this.serverYaw;
            this.aimPitch = this.serverPitch;
            this.sendBlockRequirementMessage();
        }

        this.findBeds();
        this.buildTargetStructure();
    }

    private void sendBlockRequirementMessage() {
        String b1Name = BLOCK_NAMES[this.block1Inside.getValue()];
        String b2Name = BLOCK_NAMES[this.block2Outside.getValue()];
        String msg = "";
        switch (this.layoutMode.getValue()) {
            case 0:
                msg = EnumChatFormatting.BLUE + "[AutoBed] " + EnumChatFormatting.WHITE + "Need 8 " + b1Name;
                break;
            case 1:
                msg = EnumChatFormatting.BLUE
                    + "[AutoBed] "
                    + EnumChatFormatting.WHITE
                    + "Need 18 "
                    + b1Name
                    + " and 8 "
                    + b2Name;
                break;
            case 2:
                msg = EnumChatFormatting.BLUE
                    + "[AutoBed] "
                    + EnumChatFormatting.WHITE
                    + "Need 22 "
                    + b1Name
                    + " and 8 "
                    + b2Name;
        }

        mc.field_71439_g.func_145747_a(new ChatComponentText(msg));
    }

    @Override
    public void onDisabled() {
        this.stopSneakingIfNeeded();
        if (this.originalSlot >= 0
            && this.originalSlot < 9
            && mc.field_71439_g != null
            && mc.field_71439_g.field_71071_by.field_70461_c != this.originalSlot) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.originalSlot;
        }

        this.bedPositions.clear();
        this.targetQueue.clear();
        this.layer1Positions.clear();
        this.targetBlock = null;
        this.targetFacing = null;
        this.targetHitVec = null;
        this.shouldPlaceNextTick = false;
    }

    @EventTarget(1)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.PRE) {
                if (mc.field_71439_g != null && mc.field_71441_e != null && mc.field_71462_r == null) {
                    this.serverYaw = event.getYaw();
                    this.serverPitch = event.getPitch();
                    if (this.stopSprint.getValue() && !this.targetQueue.isEmpty()) {
                        mc.field_71439_g.func_70031_b(false);
                    }

                    if (this.bedPositions.isEmpty()) {
                        this.findBeds();
                        if (!this.bedPositions.isEmpty()) {
                            this.buildTargetStructure();
                        }
                    }

                    if (this.targetQueue.isEmpty()) {
                        this.stopSneakingIfNeeded();
                        if (this.autoDisable.getValue()) {
                            this.toggle();
                        }
                    } else {
                        this.targetQueue.removeIf(pos -> !this.canReplace(pos));
                        if (this.targetQueue.isEmpty()) {
                            this.stopSneakingIfNeeded();
                        } else {
                            this.findBestPlacement();
                            if (this.targetBlock != null && this.targetFacing != null && this.targetHitVec != null) {
                                BlockPos destinationPos = this.targetBlock.func_177972_a(this.targetFacing);
                                Block requiredBlock = this.getRequiredBlock(destinationPos);
                                if (this.autoSwitch.getValue()) {
                                    int slot = this.findBlockSlot(requiredBlock);
                                    if (slot == -1 && this.fallbackToWool.getValue()) {
                                        slot = this.findBlockSlot(Blocks.field_150325_L);
                                    }

                                    if (slot == -1) {
                                        slot = this.findAnyBlockSlot();
                                    }

                                    if (slot != -1 && mc.field_71439_g.field_71071_by.field_70461_c != slot) {
                                        mc.field_71439_g.field_71071_by.field_70461_c = slot;
                                    }
                                }

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
                                float smoothVal = this.smooth.getValue().floatValue() / 100.0F;
                                float smoothedYawDiff = yawDiff * (1.0F - smoothVal * 0.7F);
                                float smoothedPitchDiff = pitchDiff * (1.0F - smoothVal * 0.7F);
                                float yawStep = MathHelper.func_76131_a(smoothedYawDiff, -maxTurn, maxTurn);
                                float pitchStep = MathHelper.func_76131_a(smoothedPitchDiff, -maxTurn, maxTurn);
                                this.aimYaw = this.serverYaw + yawStep;
                                this.aimPitch = MathHelper.func_76131_a(this.serverPitch + pitchStep, -90.0F, 90.0F);
                                event.setRotation(this.aimYaw, this.aimPitch, 6);
                                event.setPervRotation(
                                    this.moveFix.getValue() != 0 ? this.aimYaw : mc.field_71439_g.field_70177_z, 6
                                );
                                boolean placingOnBed = this.autoSneak.getValue()
                                    && mc.field_71441_e.func_180495_p(this.targetBlock).func_177230_c() instanceof BlockBed;
                                if (placingOnBed) {
                                    this.startSneaking();
                                } else {
                                    this.stopSneakingIfNeeded();
                                }

                                this.shouldPlaceNextTick = this.withinRotationTolerance(this.aimYaw, this.aimPitch);
                            } else {
                                this.shouldPlaceNextTick = false;
                                this.stopSneakingIfNeeded();
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
                if (mc.field_71439_g != null && mc.field_71441_e != null && mc.field_71462_r == null) {
                    if (this.shouldPlaceNextTick
                        && this.targetBlock != null
                        && this.targetFacing != null
                        && this.targetHitVec != null) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - this.lastPlaceTime >= this.placeDelay.getValue().intValue()) {
                            MovingObjectPosition mop = this.rayTraceBlock(
                                this.serverYaw, this.serverPitch, this.range.getValue().doubleValue()
                            );
                            boolean isValidHit = mop != null
                                && mop.field_72313_a == MovingObjectType.BLOCK
                                && mop.func_178782_a().equals(this.targetBlock)
                                && mop.field_178784_b == this.targetFacing;
                            if (this.rayCastCheck.getValue()
                                && (!isValidHit || mop.field_72307_f.func_72436_e(this.targetHitVec) > 0.1)) {
                                return;
                            }

                            if (isValidHit) {
                                ItemStack heldStack = mc.field_71439_g.field_71071_by.func_70448_g();
                                if (heldStack != null
                                    && heldStack.func_77973_b() instanceof ItemBlock
                                    && mc.field_71442_b
                                        .func_178890_a(
                                            mc.field_71439_g,
                                            mc.field_71441_e,
                                            heldStack,
                                            this.targetBlock,
                                            this.targetFacing,
                                            mop.field_72307_f
                                        )) {
                                    if (this.silentSwing.getValue()) {
                                        mc.field_71439_g.field_71174_a.func_147297_a(new C0APacketAnimation());
                                    } else {
                                        mc.field_71439_g.func_71038_i();
                                    }

                                    this.lastPlaceTime = currentTime;
                                    this.targetQueue.remove(this.targetBlock.func_177972_a(this.targetFacing));
                                    this.targetBlock = null;
                                    this.targetFacing = null;
                                    this.targetHitVec = null;
                                    this.shouldPlaceNextTick = false;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void startSneaking() {
        if (!this.isSneakingSent) {
            mc.field_71439_g
                .field_71174_a
                .func_147297_a(new C0BPacketEntityAction(mc.field_71439_g, Action.START_SNEAKING));
            this.isSneakingSent = true;
        }
    }

    private void stopSneaking() {
        if (this.isSneakingSent) {
            mc.field_71439_g
                .field_71174_a
                .func_147297_a(new C0BPacketEntityAction(mc.field_71439_g, Action.STOP_SNEAKING));
            this.isSneakingSent = false;
        }
    }

    private void stopSneakingIfNeeded() {
        if (this.isSneakingSent) {
            this.stopSneaking();
        }
    }

    private void findBestPlacement() {
        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        double reach = this.range.getValue().doubleValue();
        this.targetBlock = null;
        this.targetFacing = null;
        this.targetHitVec = null;
        this.targetQueue.sort(Comparator.comparingDouble(pos -> this.getEyeDistanceSq(pos)));

        for (BlockPos candidate : new ArrayList<>(this.targetQueue)) {
            if (!(this.getEyeDistanceSq(candidate) > reach * reach)) {
                for (EnumFacing facing : EnumFacing.values()) {
                    BlockPos support = candidate.func_177972_a(facing);
                    if (this.isSolid(support)) {
                        EnumFacing placeFacing = facing.func_176734_d();
                        if (this.tryPlaceOnBlock(support, eye, reach, placeFacing)) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean tryPlaceOnBlock(BlockPos supportBlock, Vec3 eye, double reach, EnumFacing facing) {
        int n = (int)Math.round(5.0);

        for (int r = 0; r <= n; r++) {
            double v = r * 0.2 + (Math.random() * 0.010000000000000002 * 2.0 - 0.010000000000000002);
            if (v < 0.0) {
                v = 0.0;
            } else if (v > 1.0) {
                v = 1.0;
            }

            for (int c = 0; c <= n; c++) {
                double u = c * 0.2 + (Math.random() * 0.010000000000000002 * 2.0 - 0.010000000000000002);
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

        return false;
    }

    private Vec3 getHitPosOnFace(BlockPos block, EnumFacing face, double u, double v) {
        double x = block.func_177958_n() + 0.5;
        double y = block.func_177956_o() + 0.5;
        double z = block.func_177952_p() + 0.5;
        switch (face) {
            case DOWN:
                y = block.func_177956_o() + 0.08;
                x = block.func_177958_n() + u;
                z = block.func_177952_p() + v;
                break;
            case UP:
                y = block.func_177956_o() + 1.0 - 0.08;
                x = block.func_177958_n() + u;
                z = block.func_177952_p() + v;
                break;
            case NORTH:
                z = block.func_177952_p() + 0.08;
                x = block.func_177958_n() + u;
                y = block.func_177956_o() + v;
                break;
            case SOUTH:
                z = block.func_177952_p() + 1.0 - 0.08;
                x = block.func_177958_n() + u;
                y = block.func_177956_o() + v;
                break;
            case WEST:
                x = block.func_177958_n() + 0.08;
                z = block.func_177952_p() + u;
                y = block.func_177956_o() + v;
                break;
            case EAST:
                x = block.func_177958_n() + 1.0 - 0.08;
                z = block.func_177952_p() + u;
                y = block.func_177956_o() + v;
        }

        return new Vec3(x, y, z);
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

    private void findBeds() {
        this.bedPositions.clear();
        BlockPos pPos = new BlockPos(mc.field_71439_g);

        for (int x = -5; x <= 5; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = pPos.func_177982_a(x, y, z);
                    if (mc.field_71441_e.func_180495_p(pos).func_177230_c() instanceof BlockBed) {
                        this.bedPositions.add(pos);
                    }
                }
            }
        }
    }

    private void buildTargetStructure() {
        this.targetQueue.clear();
        this.layer1Positions.clear();
        if (!this.bedPositions.isEmpty()) {
            Set<BlockPos> l1Set = new LinkedHashSet<>();
            Set<BlockPos> l2Set = new LinkedHashSet<>();
            if (this.layoutMode.getValue() == 2) {
                BlockPos bed1 = this.bedPositions.get(0);
                BlockPos bed2 = this.bedPositions.size() > 1 ? this.bedPositions.get(1) : bed1;
                EnumFacing bedFacing = EnumFacing.NORTH;
                if (this.bedPositions.size() > 1) {
                    if (bed2.func_177958_n() > bed1.func_177958_n()) {
                        bedFacing = EnumFacing.EAST;
                    } else if (bed2.func_177958_n() < bed1.func_177958_n()) {
                        bedFacing = EnumFacing.WEST;
                    } else if (bed2.func_177952_p() > bed1.func_177952_p()) {
                        bedFacing = EnumFacing.SOUTH;
                    } else if (bed2.func_177952_p() < bed1.func_177952_p()) {
                        bedFacing = EnumFacing.NORTH;
                    }
                }

                EnumFacing left = bedFacing.func_176735_f();
                EnumFacing right = bedFacing.func_176746_e();
                EnumFacing back = bedFacing.func_176734_d();
                l1Set.add(bed1.func_177972_a(back));
                l1Set.add(bed2.func_177972_a(bedFacing));
                l1Set.add(bed1.func_177972_a(left));
                l1Set.add(bed1.func_177972_a(right));
                l1Set.add(bed2.func_177972_a(left));
                l1Set.add(bed2.func_177972_a(right));
                l1Set.add(bed1.func_177972_a(back).func_177972_a(left));
                l1Set.add(bed1.func_177972_a(back).func_177972_a(right));
                l1Set.add(bed2.func_177972_a(bedFacing).func_177972_a(left));
                l1Set.add(bed2.func_177972_a(bedFacing).func_177972_a(right));
                l2Set.add(bed1.func_177967_a(back, 2));
                l2Set.add(bed2.func_177967_a(bedFacing, 2));
                l2Set.add(bed1.func_177967_a(left, 2));
                l2Set.add(bed1.func_177967_a(right, 2));
                l2Set.add(bed2.func_177967_a(left, 2));
                l2Set.add(bed2.func_177967_a(right, 2));
                l1Set.add(bed1.func_177984_a());
                l1Set.add(bed2.func_177984_a());
                l1Set.add(bed1.func_177972_a(back).func_177984_a());
                l1Set.add(bed2.func_177972_a(bedFacing).func_177984_a());
                l1Set.add(bed1.func_177972_a(left).func_177984_a());
                l1Set.add(bed1.func_177972_a(right).func_177984_a());
                l1Set.add(bed2.func_177972_a(left).func_177984_a());
                l1Set.add(bed2.func_177972_a(right).func_177984_a());
                l1Set.add(bed1.func_177972_a(back).func_177972_a(left).func_177984_a());
                l1Set.add(bed1.func_177972_a(back).func_177972_a(right).func_177984_a());
                l1Set.add(bed2.func_177972_a(bedFacing).func_177972_a(left).func_177984_a());
                l1Set.add(bed2.func_177972_a(bedFacing).func_177972_a(right).func_177984_a());
                l2Set.add(bed1.func_177981_b(2));
                l2Set.add(bed2.func_177981_b(2));
                l1Set.removeAll(this.bedPositions);
                l2Set.removeAll(l1Set);
                l2Set.removeAll(this.bedPositions);
                this.layer1Positions.addAll(l1Set);
                List<BlockPos> allPositions = new ArrayList<>();
                allPositions.addAll(l1Set);
                allPositions.addAll(l2Set);
                allPositions.sort(
                    Comparator.comparingInt(Vec3i::func_177956_o)
                        .thenComparingDouble(pos -> this.getEyeDistanceSq(pos))
                );
                this.targetQueue.addAll(allPositions);
            } else {
                for (BlockPos b : this.bedPositions) {
                    l1Set.add(b.func_177984_a());

                    for (EnumFacing f : EnumFacing.field_176754_o) {
                        l1Set.add(b.func_177972_a(f));
                    }
                }

                if (this.layoutMode.getValue() == 1) {
                    for (BlockPos l1 : l1Set) {
                        l2Set.add(l1.func_177984_a());

                        for (EnumFacing f : EnumFacing.field_176754_o) {
                            l2Set.add(l1.func_177972_a(f));
                        }
                    }

                    l2Set.removeAll(l1Set);
                    l2Set.removeAll(this.bedPositions);
                }

                this.layer1Positions.addAll(l1Set);
                List<BlockPos> allPositions = new ArrayList<>();
                allPositions.addAll(l1Set);
                allPositions.addAll(l2Set);
                allPositions.sort(Comparator.comparingInt(Vec3i::func_177956_o));
                this.targetQueue.addAll(allPositions);
            }
        }
    }

    private boolean isSolid(BlockPos pos) {
        Block b = mc.field_71441_e.func_180495_p(pos).func_177230_c();
        return !b.func_176200_f(mc.field_71441_e, pos)
            && (b.func_149730_j() || b instanceof BlockBed || b.func_149662_c());
    }

    private boolean canReplace(BlockPos pos) {
        return mc.field_71441_e.func_180495_p(pos).func_177230_c().func_176200_f(mc.field_71441_e, pos);
    }

    private Block getRequiredBlock(BlockPos pos) {
        Block primary = BLOCKS[this.block1Inside.getValue()];
        Block secondary = BLOCKS[this.block2Outside.getValue()];
        return this.layer1Positions.contains(pos) ? primary : secondary;
    }

    private int findBlockSlot(Block target) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (stack != null
                && stack.func_77973_b() instanceof ItemBlock
                && ((ItemBlock)stack.func_77973_b()).func_179223_d() == target) {
                return i;
            }
        }

        return -1;
    }

    private int findAnyBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (stack != null && stack.func_77973_b() instanceof ItemBlock) {
                return i;
            }
        }

        return -1;
    }

    private double getEyeDistanceSq(BlockPos pos) {
        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        Vec3 target = new Vec3(pos.func_177958_n() + 0.5, pos.func_177956_o() + 0.5, pos.func_177952_p() + 0.5);
        return eye.func_72436_e(target);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{
            BLOCK_NAMES[this.block1Inside.getValue()] + " + " + BLOCK_NAMES[this.block2Outside.getValue()],
            this.layoutMode.getModeString()
        };
    }
}
