package miau.module.modules.player;

import com.google.common.base.CaseFormat;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import miau.Miau;
import miau.enums.ChatColors;
import miau.enums.DelayModules;
import miau.event.EventTarget;
import miau.event.impl.HitBlockEvent;
import miau.event.impl.KnockbackEvent;
import miau.event.impl.LeftClickMouseEvent;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.PlayerUpdateEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.RightClickMouseEvent;
import miau.event.impl.SwapItemEvent;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.management.RotationState;
import miau.mixin.IAccessorPlayerControllerMP;
import miau.module.Module;
import miau.module.modules.render.BedESP;
import miau.module.modules.render.HUD;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.PercentProperty;
import miau.util.network.PacketUtil;
import miau.util.player.ItemUtil;
import miau.util.player.MoveUtil;
import miau.util.player.PlayerUtil;
import miau.util.player.RayCastUtil;
import miau.util.player.RotationUtil;
import miau.util.render.ColorUtil;
import miau.util.render.RenderUtil;
import miau.util.time.TimerUtil;
import miau.util.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class BedNuker extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final long WHITELIST_SCAN_DELAY_MS = 1000L;
    private final TimerUtil timer = new TimerUtil();
    private final ArrayList<BlockPos> bedWhitelist = new ArrayList<>();
    private final Color colorRed = new Color(ChatColors.RED.toAwtColor());
    private final Color colorYellow = new Color(ChatColors.YELLOW.toAwtColor());
    private final Color colorGreen = new Color(ChatColors.GREEN.toAwtColor());
    private BlockPos targetBed = null;
    private int breakStage = 0;
    private int tickCounter = 0;
    private float breakProgress = 0.0F;
    private boolean isBed = false;
    private int savedSlot = -1;
    private boolean readyToBreak = false;
    private boolean breaking = false;
    private boolean waitingForStart = false;
    private long whitelistScanAt = -1L;
    private final ArrayDeque<BlockPos> bfsQueue = new ArrayDeque<>(64);
    private final HashSet<BlockPos> bfsVisited = new HashSet<>(128);
    private final ArrayList<BlockPos> bfsCandidates = new ArrayList<>(32);
    private BlockPos[] currentBedPair = null;
    private int bypassState = 0;
    private long bypassStateSince = -1L;
    private static final long WAIT_TIMEOUT_MS = 3500L;
    private static final long TIMEDOUT_COOLDOWN_MS = 2000L;
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"NORMAL", "SWAP", "BYPASS"});
    public final FloatProperty range = new FloatProperty("range", 4.5F, 3.0F, 6.0F);
    public final PercentProperty speed = new PercentProperty("speed", 0);
    public final BooleanProperty groundSpeed = new BooleanProperty("ground-spoof", false);
    public final ModeProperty ignoreVelocity = new ModeProperty(
        "ignore-velocity", 0, new String[]{"NONE", "CANCEL", "DELAY"}
    );
    public final BooleanProperty surroundings = new BooleanProperty("surroundings", true);
    public final BooleanProperty toolCheck = new BooleanProperty("tool-check", true);
    public final BooleanProperty whiteList = new BooleanProperty("whitelist", true);
    public final BooleanProperty swing = new BooleanProperty("swing", true);
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"});
    public final ModeProperty showTarget = new ModeProperty("show-target", 1, new String[]{"NONE", "DEFAULT", "HUD"});
    public final ModeProperty showProgress = new ModeProperty(
        "show-progress", 1, new String[]{"NONE", "DEFAULT", "HUD"}
    );
    public final BooleanProperty stopOnAttack = new BooleanProperty("stop-on-attack", true);
    private static final double FACE_EPSILON = 0.005;

    private void resetBreaking() {
        if (this.targetBed != null && mc.field_71441_e != null && mc.field_71439_g != null) {
            mc.field_71441_e.func_175715_c(mc.field_71439_g.func_145782_y(), this.targetBed, -1);
        }

        this.targetBed = null;
        this.breakStage = 0;
        this.tickCounter = 0;
        this.breakProgress = 0.0F;
        this.isBed = false;
        this.readyToBreak = false;
        this.breaking = false;
        this.bfsQueue.clear();
        this.bfsVisited.clear();
        this.bfsCandidates.clear();
        this.currentBedPair = null;
        this.bypassState = 0;
        this.bypassStateSince = -1L;
    }

    private void scheduleWhitelistScan() {
        this.whitelistScanAt = System.currentTimeMillis() + 1000L;
    }

    private void runPendingWhitelistScan() {
        if (this.whitelistScanAt != -1L && System.currentTimeMillis() >= this.whitelistScanAt) {
            this.whitelistScanAt = -1L;
            this.bedWhitelist.clear();
            if (mc.field_71441_e != null && mc.field_71439_g != null) {
                int sX = MathHelper.func_76128_c(mc.field_71439_g.field_70165_t);
                int sY = MathHelper.func_76128_c(mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
                int sZ = MathHelper.func_76128_c(mc.field_71439_g.field_70161_v);

                for (int i = sX - 25; i <= sX + 25; i++) {
                    for (int j = sY - 25; j <= sY + 25; j++) {
                        for (int k = sZ - 25; k <= sZ + 25; k++) {
                            BlockPos blockPos = new BlockPos(i, j, k);
                            Block block = mc.field_71441_e.func_180495_p(blockPos).func_177230_c();
                            if (block instanceof BlockBed) {
                                this.bedWhitelist.add(blockPos);
                            }
                        }
                    }
                }
            }
        }
    }

    private float calcProgress() {
        if (this.targetBed == null) {
            return 0.0F;
        }

        float progress = this.breakProgress;
        if (this.groundSpeed.getValue()) {
            int slot = ItemUtil.findInventorySlot(
                mc.field_71439_g.field_71071_by.field_70461_c,
                mc.field_71441_e.func_180495_p(this.targetBed).func_177230_c()
            );
            progress = this.tickCounter
                * this.getBreakDelta(mc.field_71441_e.func_180495_p(this.targetBed), this.targetBed, slot, true);
        }

        return Math.min(1.0F, progress / (1.0F - 0.3F * (this.speed.getValue().intValue() / 100.0F)));
    }

    private void restoreSlot() {
        if (this.savedSlot != -1) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.savedSlot;
            this.syncHeldItem();
            this.savedSlot = -1;
        }
    }

    private void syncHeldItem() {
        int currentPlayerItem = ((IAccessorPlayerControllerMP)mc.field_71442_b).getCurrentPlayerItem();
        if (mc.field_71439_g.field_71071_by.field_70461_c != currentPlayerItem) {
            mc.field_71439_g.func_71034_by();
        }

        ((IAccessorPlayerControllerMP)mc.field_71442_b).callSyncCurrentPlayItem();
    }

    private boolean hasProperTool(Block block) {
        Material material = block.func_149688_o();
        if (material != Material.field_151573_f
            && material != Material.field_151574_g
            && material != Material.field_151576_e) {
            return true;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (stack != null) {
                Item item = stack.func_77973_b();
                if (item instanceof ItemPickaxe) {
                    return true;
                }
            }
        }

        return false;
    }

    private EnumFacing getHitFacing(BlockPos blockPos) {
        double x = blockPos.func_177958_n() + 0.5 - mc.field_71439_g.field_70165_t;
        double y = blockPos.func_177956_o() + 0.25 - mc.field_71439_g.field_70163_u - mc.field_71439_g.func_70047_e();
        double z = blockPos.func_177952_p() + 0.5 - mc.field_71439_g.field_70161_v;
        float[] rotations = RotationUtil.getRotationsTo(
            x, y, z, mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A
        );
        MovingObjectPosition mop = RotationUtil.rayTrace(rotations[0], rotations[1], 8.0, 1.0F);
        return mop == null ? EnumFacing.UP : mop.field_178784_b;
    }

    private float getDigSpeed(IBlockState iBlockState, int slot, boolean boolean5) {
        ItemStack item = mc.field_71439_g.field_71071_by.func_70301_a(slot);
        float digSpeed = item == null ? 1.0F : item.func_77973_b().getDigSpeed(item, iBlockState);
        if (digSpeed > 1.0F) {
            int enchantmentLevel = EnchantmentHelper.func_77506_a(Enchantment.field_77349_p.field_77352_x, item);
            if (enchantmentLevel > 0) {
                digSpeed += enchantmentLevel * enchantmentLevel + 1;
            }
        }

        if (mc.field_71439_g.func_70644_a(Potion.field_76422_e)) {
            digSpeed *= 1.0F + (mc.field_71439_g.func_70660_b(Potion.field_76422_e).func_76458_c() + 1) * 0.2F;
        }

        if (mc.field_71439_g.func_70644_a(Potion.field_76419_f)) {
            switch (mc.field_71439_g.func_70660_b(Potion.field_76419_f).func_76458_c()) {
                case 0:
                    digSpeed *= 0.3F;
                    break;
                case 1:
                    digSpeed *= 0.09F;
                    break;
                case 2:
                    digSpeed *= 0.0027F;
                    break;
                default:
                    digSpeed *= 8.1E-4F;
            }
        }

        if (mc.field_71439_g.func_70055_a(Material.field_151586_h) && !EnchantmentHelper.func_77510_g(mc.field_71439_g)
            )
         {
            digSpeed /= 5.0F;
        }

        if (!boolean5) {
            digSpeed /= 5.0F;
        }

        return digSpeed;
    }

    boolean canHarvest(Block block, int slot) {
        if (block.func_149688_o().func_76229_l()) {
            return true;
        }

        ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(slot);
        return stack != null && stack.func_150998_b(block);
    }

    private float getBreakDelta(IBlockState iBlockState, BlockPos blockPos, int slot, boolean boolean5) {
        Block block = iBlockState.func_177230_c();
        float hardness = block.func_176195_g(mc.field_71441_e, blockPos);
        float boost = this.canHarvest(block, slot) ? 30.0F : 100.0F;
        return hardness < 0.0F ? 0.0F : this.getDigSpeed(iBlockState, slot, boolean5) / hardness / boost;
    }

    private float calcBlockStrength(BlockPos blockPos) {
        IBlockState blockState = mc.field_71441_e.func_180495_p(blockPos);
        int slot = ItemUtil.findInventorySlot(mc.field_71439_g.field_71071_by.field_70461_c, blockState.func_177230_c());
        return this.getBreakDelta(blockState, blockPos, slot, mc.field_71439_g.field_70122_E);
    }

    private Vec3 closestPointOnFace(AxisAlignedBB aabb, EnumFacing face, Vec3 point) {
        double cx = clamp(point.field_72450_a, aabb.field_72340_a + 0.005, aabb.field_72336_d - 0.005);
        double cy = clamp(point.field_72448_b, aabb.field_72338_b + 0.005, aabb.field_72337_e - 0.005);
        double cz = clamp(point.field_72449_c, aabb.field_72339_c + 0.005, aabb.field_72334_f - 0.005);
        switch (face) {
            case DOWN:
                return new Vec3(cx, aabb.field_72338_b, cz);
            case UP:
                return new Vec3(cx, aabb.field_72337_e, cz);
            case NORTH:
                return new Vec3(cx, cy, aabb.field_72339_c);
            case SOUTH:
                return new Vec3(cx, cy, aabb.field_72334_f);
            case WEST:
                return new Vec3(aabb.field_72340_a, cy, cz);
            case EAST:
                return new Vec3(aabb.field_72336_d, cy, cz);
            default:
                return new Vec3(cx, cy, cz);
        }
    }

    private static double clamp(double val, double min, double max) {
        return val < min ? min : (val > max ? max : val);
    }

    private BedNuker.HitResult computeBestHit(BlockPos pos) {
        Vec3 eyes = mc.field_71439_g.func_174824_e(1.0F);
        double maxRange = this.range.getValue().doubleValue();
        double maxRangeSq = maxRange * maxRange;
        IBlockState state = mc.field_71441_e.func_180495_p(pos);
        Block block = state.func_177230_c();
        AxisAlignedBB aabb = block.func_180640_a(mc.field_71441_e, pos, state);
        if (aabb == null) {
            aabb = new AxisAlignedBB(pos, pos.func_177982_a(1, 1, 1));
        }

        BedNuker.HitResult best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (EnumFacing face : EnumFacing.values()) {
            BlockPos adjacent = pos.func_177972_a(face);
            if (mc.field_71441_e.func_180495_p(adjacent).func_177230_c() instanceof BlockAir
                || mc.field_71441_e.func_180495_p(adjacent).func_177230_c() instanceof BlockBed
                || !mc.field_71441_e.func_180495_p(adjacent).func_177230_c().func_149730_j()) {
                Vec3 facePoint = this.closestPointOnFace(aabb, face, eyes);
                double distSq = eyes.func_72436_e(facePoint);
                if (!(distSq > maxRangeSq) && !(distSq < 0.001)) {
                    float[] rots = RotationUtil.calculate(facePoint);
                    MovingObjectPosition mop = RayCastUtil.rayCast(rots[0], rots[1], maxRange, 0.0F);
                    if (mop != null
                        && mop.field_72313_a == MovingObjectType.BLOCK
                        && mop.func_178782_a().equals(pos)
                        && distSq < bestDistSq) {
                        bestDistSq = distSq;
                        Vec3 hitVec = mop.field_72307_f != null ? mop.field_72307_f : facePoint;
                        best = new BedNuker.HitResult(hitVec, mop.field_178784_b, Math.sqrt(distSq));
                    }
                }
            }
        }

        return best;
    }

    private BedNuker.HitResult computeFallbackHit(BlockPos pos) {
        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        ArrayList<Vec3> visiblePoints = this.getVisiblePoints(pos);
        if (visiblePoints.isEmpty()) {
            double maxRange = this.range.getValue().doubleValue();
            double maxRangeSq = maxRange * maxRange;
            BedNuker.HitResult bestHit = null;
            double bestDistSq = Double.MAX_VALUE;

            for (EnumFacing face : EnumFacing.values()) {
                Block adj = mc.field_71441_e.func_180495_p(pos.func_177972_a(face)).func_177230_c();
                if (adj instanceof BlockAir) {
                    Vec3 hitVec = new Vec3(
                        pos.func_177958_n() + 0.5 + face.func_82601_c() * 0.49,
                        pos.func_177956_o() + 0.5 + face.func_96559_d() * 0.49,
                        pos.func_177952_p() + 0.5 + face.func_82599_e() * 0.49
                    );
                    double distSq = eye.func_72436_e(hitVec);
                    if (distSq < bestDistSq && distSq <= maxRangeSq) {
                        bestDistSq = distSq;
                        bestHit = new BedNuker.HitResult(hitVec, face, Math.sqrt(distSq));
                    }
                }
            }

            return bestHit;
        } else {
            Vec3 bestPoint = visiblePoints.get(0);
            double bestDistSq = eye.func_72436_e(bestPoint);

            for (int i = 1; i < visiblePoints.size(); i++) {
                double distSq = eye.func_72436_e(visiblePoints.get(i));
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    bestPoint = visiblePoints.get(i);
                }
            }

            EnumFacing facing = EnumFacing.UP;
            double cx = pos.func_177958_n() + 0.5;
            double cy = pos.func_177956_o() + 0.28125;
            double cz = pos.func_177952_p() + 0.5;
            double dx = Math.abs(bestPoint.field_72450_a - cx);
            double dy = Math.abs(bestPoint.field_72448_b - cy);
            double dz = Math.abs(bestPoint.field_72449_c - cz);
            if (dy > dx && dy > dz) {
                facing = bestPoint.field_72448_b > cy ? EnumFacing.UP : EnumFacing.DOWN;
            } else if (dx > dz) {
                facing = bestPoint.field_72450_a > cx ? EnumFacing.EAST : EnumFacing.WEST;
            } else {
                facing = bestPoint.field_72449_c > cz ? EnumFacing.SOUTH : EnumFacing.NORTH;
            }

            return new BedNuker.HitResult(bestPoint, facing, Math.sqrt(bestDistSq));
        }
    }

    private ArrayList<Vec3> buildRaytraceSamplePoints(AxisAlignedBB aabb) {
        ArrayList<Vec3> points = new ArrayList<>();
        double INSET = 0.01;
        double minX = aabb.field_72340_a;
        double minY = aabb.field_72338_b;
        double minZ = aabb.field_72339_c;
        double maxX = aabb.field_72336_d;
        double maxY = aabb.field_72337_e;
        double maxZ = aabb.field_72334_f;
        double cx = (minX + maxX) * 0.5;
        double cy = (minY + maxY) * 0.5;
        double cz = (minZ + maxZ) * 0.5;
        points.add(new Vec3(minX + 0.01, minY, minZ + 0.01));
        points.add(new Vec3(maxX - 0.01, minY, minZ + 0.01));
        points.add(new Vec3(minX + 0.01, minY, maxZ - 0.01));
        points.add(new Vec3(maxX - 0.01, minY, maxZ - 0.01));
        points.add(new Vec3(cx, minY, cz));
        points.add(new Vec3(minX + 0.01, maxY, minZ + 0.01));
        points.add(new Vec3(maxX - 0.01, maxY, minZ + 0.01));
        points.add(new Vec3(minX + 0.01, maxY, maxZ - 0.01));
        points.add(new Vec3(maxX - 0.01, maxY, maxZ - 0.01));
        points.add(new Vec3(cx, maxY, cz));
        points.add(new Vec3(minX + 0.01, minY + 0.01, minZ));
        points.add(new Vec3(maxX - 0.01, minY + 0.01, minZ));
        points.add(new Vec3(minX + 0.01, maxY - 0.01, minZ));
        points.add(new Vec3(maxX - 0.01, maxY - 0.01, minZ));
        points.add(new Vec3(cx, cy, minZ));
        points.add(new Vec3(minX + 0.01, minY + 0.01, maxZ));
        points.add(new Vec3(maxX - 0.01, minY + 0.01, maxZ));
        points.add(new Vec3(minX + 0.01, maxY - 0.01, maxZ));
        points.add(new Vec3(maxX - 0.01, maxY - 0.01, maxZ));
        points.add(new Vec3(cx, cy, maxZ));
        points.add(new Vec3(minX, minY + 0.01, minZ + 0.01));
        points.add(new Vec3(minX, maxY - 0.01, minZ + 0.01));
        points.add(new Vec3(minX, minY + 0.01, maxZ - 0.01));
        points.add(new Vec3(minX, maxY - 0.01, maxZ - 0.01));
        points.add(new Vec3(minX, cy, cz));
        points.add(new Vec3(maxX, minY + 0.01, minZ + 0.01));
        points.add(new Vec3(maxX, maxY - 0.01, minZ + 0.01));
        points.add(new Vec3(maxX, minY + 0.01, maxZ - 0.01));
        points.add(new Vec3(maxX, maxY - 0.01, maxZ - 0.01));
        points.add(new Vec3(maxX, cy, cz));
        return points;
    }

    private BlockPos findRaytraceObstruction(BlockPos target) {
        Vec3 eyes = mc.field_71439_g.func_174824_e(1.0F);
        double maxRangeSq = this.range.getValue().doubleValue() * this.range.getValue().doubleValue();
        IBlockState state = mc.field_71441_e.func_180495_p(target);
        Block block = state.func_177230_c();
        AxisAlignedBB aabb = block.func_180640_a(mc.field_71441_e, target, state);
        if (aabb == null) {
            aabb = new AxisAlignedBB(target, target.func_177982_a(1, 1, 1));
        }

        for (Vec3 candidate : this.buildRaytraceSamplePoints(aabb)) {
            if (!(eyes.func_72436_e(candidate) > maxRangeSq)) {
                MovingObjectPosition mop = mc.field_71441_e.func_72933_a(eyes, candidate);
                if (mop != null && mop.field_72313_a == MovingObjectType.BLOCK) {
                    BlockPos hitPos = mop.func_178782_a();
                    if (hitPos.equals(target)) {
                        return null;
                    }

                    Block hitBlock = mc.field_71441_e.func_180495_p(hitPos).func_177230_c();
                    if (!(hitBlock instanceof BlockAir)
                        && !(hitBlock instanceof BlockBed)
                        && !(hitBlock.func_176195_g(mc.field_71441_e, hitPos) < 0.0F)) {
                        return hitPos;
                    }

                    return null;
                }

                return null;
            }
        }

        return null;
    }

    private boolean tryClearObstruction() {
        if (this.targetBed == null) {
            return false;
        } else {
            BlockPos obstruction = this.findRaytraceObstruction(this.targetBed);
            if (obstruction != null && PlayerUtil.canReach(obstruction, this.range.getValue().doubleValue())) {
                this.targetBed = obstruction;
                this.breakStage = 0;
                this.tickCounter = 0;
                this.breakProgress = 0.0F;
                this.isBed = false;
                return true;
            } else {
                BedNuker.HitResult fallbackHit = this.computeFallbackHit(this.targetBed);
                return fallbackHit != null;
            }
        }
    }

    private BlockPos[] resolveBedPair(BlockPos bedPos) {
        IBlockState state = mc.field_71441_e.func_180495_p(bedPos);
        if (!(state.func_177230_c() instanceof BlockBed)) {
            return null;
        }

        EnumPartType part = (EnumPartType)state.func_177229_b(BlockBed.field_176472_a);
        EnumFacing facing = (EnumFacing)state.func_177229_b(BlockBed.field_176387_N);
        BlockPos foot = part == EnumPartType.FOOT ? bedPos : bedPos.func_177972_a(facing.func_176734_d());
        BlockPos head = foot.func_177972_a(facing);
        return new BlockPos[]{foot, head};
    }

    private boolean isBedExposed(BlockPos[] pair) {
        for (BlockPos bp : pair) {
            for (EnumFacing f : EnumFacing.values()) {
                if (mc.field_71441_e.func_180495_p(bp.func_177972_a(f)).func_177230_c() instanceof BlockAir) {
                    return true;
                }
            }
        }

        return false;
    }

    private Vec3 bedCenter(BlockPos[] pair) {
        double minX = Math.min(pair[0].func_177958_n(), pair[1].func_177958_n());
        double minY = Math.min(pair[0].func_177956_o(), pair[1].func_177956_o());
        double minZ = Math.min(pair[0].func_177952_p(), pair[1].func_177952_p());
        double maxX = Math.max(pair[0].func_177958_n(), pair[1].func_177958_n()) + 1.0;
        double maxY = Math.max(pair[0].func_177956_o(), pair[1].func_177956_o()) + 1.0;
        double maxZ = Math.max(pair[0].func_177952_p(), pair[1].func_177952_p()) + 1.0;
        return new Vec3((minX + maxX) * 0.5, (minY + maxY) * 0.5, (minZ + maxZ) * 0.5);
    }

    private ArrayList<Vec3> getVisiblePoints(BlockPos pos) {
        ArrayList<Vec3> visiblePoints = new ArrayList<>();
        Vec3 eyes = mc.field_71439_g.func_174824_e(1.0F);
        double maxDist = this.range.getValue().doubleValue();
        IBlockState state = mc.field_71441_e.func_180495_p(pos);
        Block block = state.func_177230_c();
        AxisAlignedBB aabb = block.func_180640_a(mc.field_71441_e, pos, state);
        if (aabb == null) {
            aabb = new AxisAlignedBB(pos, pos.func_177982_a(1, 1, 1));
        }

        double minX = aabb.field_72340_a;
        double minY = aabb.field_72338_b;
        double minZ = aabb.field_72339_c;
        double maxX = aabb.field_72336_d;
        double maxY = aabb.field_72337_e;
        double maxZ = aabb.field_72334_f;
        double cx = (minX + maxX) * 0.5;
        double cy = (minY + maxY) * 0.5;
        double cz = (minZ + maxZ) * 0.5;
        Vec3[] points = new Vec3[]{
            new Vec3(cx, cy, cz),
            new Vec3(cx, maxY - 0.01, cz),
            new Vec3(cx, minY + 0.01, cz),
            new Vec3(minX + 0.05, cy, cz),
            new Vec3(maxX - 0.05, cy, cz),
            new Vec3(cx, cy, minZ + 0.05),
            new Vec3(cx, cy, maxZ - 0.05),
            new Vec3(minX + 0.05, maxY - 0.01, minZ + 0.05),
            new Vec3(maxX - 0.05, maxY - 0.01, minZ + 0.05),
            new Vec3(minX + 0.05, maxY - 0.01, maxZ - 0.05),
            new Vec3(maxX - 0.05, maxY - 0.01, maxZ - 0.05)
        };

        for (Vec3 point : points) {
            if (!(eyes.func_72438_d(point) > maxDist)) {
                MovingObjectPosition mop = mc.field_71441_e.func_72933_a(eyes, point);
                if (mop != null && mop.field_72313_a == MovingObjectType.BLOCK && mop.func_178782_a().equals(pos)) {
                    visiblePoints.add(point);
                }
            }
        }

        return visiblePoints;
    }

    private boolean isBedVisible(BlockPos[] pair) {
        for (BlockPos bp : pair) {
            if (!this.getVisiblePoints(bp).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private BlockPos bfsFindOutermostDefenseBlock(BlockPos[] bedPair) {
        this.bfsQueue.clear();
        this.bfsVisited.clear();
        this.bfsCandidates.clear();
        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        double rangeSq = this.range.getValue().doubleValue() * this.range.getValue().doubleValue();

        for (BlockPos bp : bedPair) {
            if (this.bfsVisited.add(bp)) {
                this.bfsQueue.add(bp);
            }
        }

        int maxLayers = 10;

        for (int layer = 0; layer < maxLayers && !this.bfsQueue.isEmpty(); layer++) {
            int layerSize = this.bfsQueue.size();
            int i = 0;

            while (true) {
                if (i < layerSize) {
                    BlockPos current = this.bfsQueue.poll();
                    if (current != null) {
                        for (EnumFacing f : EnumFacing.values()) {
                            BlockPos neighbor = current.func_177972_a(f);
                            if (neighbor.func_177956_o() >= bedPair[0].func_177956_o() && this.bfsVisited.add(neighbor)
                                )
                             {
                                Block nb = mc.field_71441_e.func_180495_p(neighbor).func_177230_c();
                                if (!(nb instanceof BlockAir)) {
                                    if (nb instanceof BlockBed) {
                                        this.bfsQueue.add(neighbor);
                                    } else if (!(nb.func_176195_g(mc.field_71441_e, neighbor) < 0.0F)) {
                                        boolean hasExposedFace = false;

                                        for (EnumFacing check : EnumFacing.values()) {
                                            Block adj = mc.field_71441_e
                                                .func_180495_p(neighbor.func_177972_a(check))
                                                .func_177230_c();
                                            if (adj instanceof BlockAir) {
                                                hasExposedFace = true;
                                                break;
                                            }
                                        }

                                        if (hasExposedFace) {
                                            Vec3 hitVec = new Vec3(
                                                neighbor.func_177958_n() + 0.5,
                                                neighbor.func_177956_o() + 0.5,
                                                neighbor.func_177952_p() + 0.5
                                            );
                                            if (eye.func_72436_e(hitVec) <= rangeSq) {
                                                this.bfsCandidates.add(neighbor);
                                            }

                                            this.bfsQueue.add(neighbor);
                                        } else {
                                            this.bfsQueue.add(neighbor);
                                        }
                                    }
                                }
                            }
                        }

                        i++;
                        continue;
                    }
                }

                if (!this.bfsCandidates.isEmpty()) {
                    return this.pickBestCandidate(this.bfsCandidates, bedPair);
                }
                break;
            }
        }

        return null;
    }

    private BlockPos pickBestCandidate(ArrayList<BlockPos> candidates, BlockPos[] bedPair) {
        BlockPos bestPos = null;
        double bestScore = Double.POSITIVE_INFINITY;
        int i = 0;

        for (int n = candidates.size(); i < n; i++) {
            BlockPos pos = candidates.get(i);
            double score = this.scoreBlockTarget(pos, false, 0.0F);
            score += this.getLoSScoreModifier(pos, bedPair);
            if (score < bestScore) {
                bestScore = score;
                bestPos = pos;
            }
        }

        return bestPos;
    }

    private double getLoSScoreModifier(BlockPos candidate, BlockPos[] bedPair) {
        if (bedPair == null) {
            return 0.0;
        }

        Vec3 eyes = mc.field_71439_g.func_174824_e(1.0F);
        Vec3 bc = this.bedCenter(bedPair);
        Vec3 candCenter = new Vec3(
            candidate.func_177958_n() + 0.5, candidate.func_177956_o() + 0.5, candidate.func_177952_p() + 0.5
        );
        Vec3 los = bc.func_178788_d(eyes);
        double losLen = los.func_72433_c();
        if (losLen < 1.0E-6) {
            return 0.0;
        }

        Vec3 toCand = candCenter.func_178788_d(eyes);
        Vec3 cross = new Vec3(
            los.field_72448_b * toCand.field_72449_c - los.field_72449_c * toCand.field_72448_b,
            los.field_72449_c * toCand.field_72450_a - los.field_72450_a * toCand.field_72449_c,
            los.field_72450_a * toCand.field_72448_b - los.field_72448_b * toCand.field_72450_a
        );
        double crossMag = Math.sqrt(
            cross.field_72450_a * cross.field_72450_a
                + cross.field_72448_b * cross.field_72448_b
                + cross.field_72449_c * cross.field_72449_c
        );
        double perpDist = crossMag / losLen;
        double dot = toCand.field_72450_a * los.field_72450_a
            + toCand.field_72448_b * los.field_72448_b
            + toCand.field_72449_c * los.field_72449_c;
        return dot < 0.0 ? 100.0 : perpDist * 0.3;
    }

    private double scoreBlockTarget(BlockPos pos, boolean isBedBlock, float currentProgress) {
        Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
        int slot = ItemUtil.findInventorySlot(mc.field_71439_g.field_71071_by.field_70461_c, block);
        if (slot == -1) {
            slot = mc.field_71439_g.field_71071_by.field_70461_c;
        }

        float digRate = this.getDigSpeed(mc.field_71441_e.func_180495_p(pos), slot, mc.field_71439_g.field_70122_E);
        float hardness = block.func_176195_g(mc.field_71441_e, pos);
        if (hardness < 0.0F) {
            return Double.POSITIVE_INFINITY;
        }

        float boost = this.canHarvest(block, slot) ? 30.0F : 100.0F;
        float rate = digRate / hardness / boost;
        if (rate <= 0.0F) {
            return Double.POSITIVE_INFINITY;
        }

        double timeEst = 1.0 / rate;
        if (pos.equals(this.targetBed) && currentProgress > 0.02F) {
            timeEst -= currentProgress * 12.0;
        }

        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        return timeEst
            + eye.func_72436_e(
                    new Vec3(pos.func_177958_n() + 0.5, pos.func_177956_o() + 0.5, pos.func_177952_p() + 0.5)
                )
                * 0.002;
    }

    private BlockPos pickBestBedBlock(BlockPos[] pair) {
        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        double rangeSq = this.range.getValue().doubleValue() * this.range.getValue().doubleValue();
        BlockPos bestPos = null;
        double bestScore = Double.POSITIVE_INFINITY;

        for (BlockPos bp : pair) {
            Vec3 center = new Vec3(bp.func_177958_n() + 0.5, bp.func_177956_o() + 0.5, bp.func_177952_p() + 0.5);
            if (!(eye.func_72436_e(center) > rangeSq)) {
                BedNuker.HitResult hit = this.computeBestHit(bp);
                if (hit == null) {
                    hit = this.computeFallbackHit(bp);
                }

                if (hit != null) {
                    double score = this.scoreBlockTarget(
                        bp, true, bp.equals(this.targetBed) ? this.breakProgress : 0.0F
                    );
                    if (score < bestScore) {
                        bestScore = score;
                        bestPos = bp;
                    }
                }
            }
        }

        if (bestPos != null) {
            return bestPos;
        }

        for (BlockPos bp : pair) {
            BlockPos obstruction = this.findRaytraceObstruction(bp);
            if (obstruction != null && PlayerUtil.canReach(obstruction, this.range.getValue().doubleValue())) {
                return obstruction;
            }
        }

        BlockPos closestBlock = null;
        double closestDistSq = Double.MAX_VALUE;

        for (BlockPos bp : pair) {
            Vec3 center = new Vec3(bp.func_177958_n() + 0.5, bp.func_177956_o() + 0.5, bp.func_177952_p() + 0.5);
            double distSq = eye.func_72436_e(center);
            if (distSq <= rangeSq && distSq < closestDistSq) {
                for (EnumFacing f : EnumFacing.values()) {
                    if (mc.field_71441_e.func_180495_p(bp.func_177972_a(f)).func_177230_c() instanceof BlockAir) {
                        closestDistSq = distSq;
                        closestBlock = bp;
                        break;
                    }
                }
            }
        }

        return closestBlock;
    }

    private BlockPos validateBedPlacement(BlockPos bedPosition) {
        IBlockState blockState = mc.field_71441_e.func_180495_p(bedPosition);
        if (blockState.func_177230_c() instanceof BlockBed) {
            ArrayList<BlockPos> pos = new ArrayList<>();
            EnumPartType partType = (EnumPartType)blockState.func_177229_b(BlockBed.field_176472_a);
            EnumFacing facing = (EnumFacing)blockState.func_177229_b(BlockBed.field_176387_N);

            for (BlockPos blockPos : Arrays.asList(
                bedPosition, bedPosition.func_177972_a(partType == EnumPartType.HEAD ? facing.func_176734_d() : facing)
            )) {
                for (EnumFacing enumFacing : Arrays.asList(
                    EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST
                )) {
                    Block block = mc.field_71441_e.func_180495_p(blockPos.func_177972_a(enumFacing)).func_177230_c();
                    if (BlockUtil.isReplaceable(blockPos.func_177972_a(enumFacing))) {
                        return null;
                    }

                    if (!(block instanceof BlockBed)) {
                        pos.add(blockPos.func_177972_a(enumFacing));
                    }
                }
            }

            if (!pos.isEmpty()) {
                pos.sort(
                    (blockPosx, blockPos2) -> {
                        int o = Float.compare(this.calcBlockStrength(blockPos2), this.calcBlockStrength(blockPosx));
                        return o != 0
                            ? o
                            : Double.compare(
                                blockPosx.func_177957_d(
                                    mc.field_71439_g.field_70165_t,
                                    mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(),
                                    mc.field_71439_g.field_70161_v
                                ),
                                blockPos2.func_177957_d(
                                    mc.field_71439_g.field_70165_t,
                                    mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(),
                                    mc.field_71439_g.field_70161_v
                                )
                            );
                    }
                );
                return pos.get(0);
            }
        }

        return null;
    }

    private BlockPos findNearestBed() {
        return this.findTargetBed(
            mc.field_71439_g.field_70165_t,
            mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(),
            mc.field_71439_g.field_70161_v
        );
    }

    private BlockPos findTargetBed(double x, double y, double z) {
        if (this.mode.getValue() == 2) {
            return this.findTargetBedBypass(x, y, z);
        }

        ArrayList<BlockPos> targets = new ArrayList<>();
        int sX = MathHelper.func_76128_c(x);
        int sY = MathHelper.func_76128_c(y);
        int sZ = MathHelper.func_76128_c(z);

        for (int i = sX - 6; i <= sX + 6; i++) {
            for (int j = sY - 6; j <= sY + 6; j++) {
                for (int k = sZ - 6; k <= sZ + 6; k++) {
                    BlockPos newPos = new BlockPos(i, j, k);
                    if (!this.whiteList.getValue() || !this.bedWhitelist.contains(newPos)) {
                        Block block = mc.field_71441_e.func_180495_p(newPos).func_177230_c();
                        if (block instanceof BlockBed
                            && PlayerUtil.isBlockWithinReach(newPos, x, y, z, this.range.getValue().doubleValue())) {
                            targets.add(newPos);
                        }
                    }
                }
            }
        }

        if (targets.isEmpty()) {
            return null;
        }

        targets.sort(
            Comparator.comparingDouble(
                blockPosx -> blockPosx.func_177957_d(
                    mc.field_71439_g.field_70165_t,
                    mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(),
                    mc.field_71439_g.field_70161_v
                )
            )
        );

        for (BlockPos blockPos : targets) {
            if (this.surroundings.getValue()) {
                BlockPos pos = this.validateBedPlacement(blockPos);
                if (pos != null) {
                    Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
                    if (this.toolCheck.getValue() && !this.hasProperTool(block)) {
                        continue;
                    }

                    return pos;
                }
            }

            return blockPos;
        }

        return null;
    }

    private BlockPos findTargetBedBypass(double x, double y, double z) {
        ArrayList<BlockPos[]> bedPairs = new ArrayList<>();
        int sX = MathHelper.func_76128_c(x);
        int sY = MathHelper.func_76128_c(y);
        int sZ = MathHelper.func_76128_c(z);
        int rangeInt = (int)Math.ceil(this.range.getValue().doubleValue()) + 2;
        HashSet<Long> seenPairs = new HashSet<>();

        for (int i = sX - rangeInt; i <= sX + rangeInt; i++) {
            for (int j = sY - rangeInt; j <= sY + rangeInt; j++) {
                for (int k = sZ - rangeInt; k <= sZ + rangeInt; k++) {
                    BlockPos newPos = new BlockPos(i, j, k);
                    if (!this.whiteList.getValue() || !this.bedWhitelist.contains(newPos)) {
                        Block block = mc.field_71441_e.func_180495_p(newPos).func_177230_c();
                        if (block instanceof BlockBed) {
                            BlockPos[] pair = this.resolveBedPair(newPos);
                            if (pair != null) {
                                long pairKey = (long)pair[0].hashCode() << 32 | pair[1].hashCode() & 4294967295L;
                                if (seenPairs.add(pairKey)
                                    && (
                                        PlayerUtil.isBlockWithinReach(
                                                pair[0], x, y, z, this.range.getValue().doubleValue()
                                            )
                                            || PlayerUtil.isBlockWithinReach(
                                                pair[1], x, y, z, this.range.getValue().doubleValue()
                                            )
                                    )) {
                                    bedPairs.add(pair);
                                }
                            }
                        }
                    }
                }
            }
        }

        bedPairs.sort(Comparator.comparingDouble(pairx -> {
            Vec3 c = this.bedCenter(pairx);
            return c.func_72436_e(new Vec3(x, y, z));
        }));

        for (BlockPos[] pair : bedPairs) {
            if (this.isBedVisible(pair)) {
                for (BlockPos bp : pair) {
                    BedNuker.HitResult hit = this.computeBestHit(bp);
                    if (hit == null) {
                        hit = this.computeFallbackHit(bp);
                    }

                    if (hit != null) {
                        this.currentBedPair = pair;
                        this.bypassState = 0;
                        return bp;
                    }
                }

                BlockPos nearest = pair[0].func_177957_d(x, y, z) <= pair[1].func_177957_d(x, y, z) ? pair[0] : pair[1];
                if (PlayerUtil.isBlockWithinReach(nearest, x, y, z, this.range.getValue().doubleValue())) {
                    this.currentBedPair = pair;
                    this.bypassState = 1;
                    this.bypassStateSince = System.currentTimeMillis();
                    return nearest;
                }
            }

            BlockPos target = this.bfsFindOutermostDefenseBlock(pair);
            if (target != null) {
                this.currentBedPair = pair;
                return target;
            }
        }

        return null;
    }

    private void doSwing() {
        if (this.swing.getValue()) {
            mc.field_71439_g.func_71038_i();
        } else {
            PacketUtil.sendPacket(new C0APacketAnimation());
        }
    }

    private Color getProgressColor(int mode) {
        switch (mode) {
            case 1:
                float progress = this.calcProgress();
                if (progress <= 0.5F) {
                    return ColorUtil.interpolate(progress / 0.5F, this.colorRed, this.colorYellow);
                }

                return ColorUtil.interpolate((progress - 0.5F) / 0.5F, this.colorYellow, this.colorGreen);
            case 2:
                return ((HUD)Miau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
            default:
                return new Color(-1);
        }
    }

    public BedNuker() {
        super("BedNuker", false);
    }

    public boolean isReady() {
        return this.targetBed != null && this.readyToBreak;
    }

    public boolean isBreaking() {
        return this.targetBed != null && this.breaking;
    }

    @EventTarget(1)
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            this.runPendingWhitelistScan();
            if (!this.isEnabled()) {
                return;
            }

            AutoBlockIn autoBlockIn = (AutoBlockIn)Miau.moduleManager.modules.get(AutoBlockIn.class);
            if (autoBlockIn.isEnabled()) {
                return;
            }

            if (this.targetBed != null) {
                if (mc.field_71441_e.func_175623_d(this.targetBed)
                    || !PlayerUtil.canReach(this.targetBed, this.range.getValue().doubleValue())) {
                    this.restoreSlot();
                    this.resetBreaking();
                } else if (!this.isBed) {
                    BlockPos nearestBed = this.findNearestBed();
                    if (nearestBed != null
                        && mc.field_71441_e.func_180495_p(nearestBed).func_177230_c() instanceof BlockBed) {
                        this.resetBreaking();
                    }
                }
            }

            if (this.targetBed != null) {
                if (this.mode.getValue() == 2) {
                    if (this.bypassState == 1 && this.isBed) {
                        BedNuker.HitResult hit = this.computeBestHit(this.targetBed);
                        if (hit == null) {
                            hit = this.computeFallbackHit(this.targetBed);
                        }

                        if (hit == null) {
                            long elapsed = System.currentTimeMillis() - this.bypassStateSince;
                            if (elapsed >= 3500L) {
                                this.bypassState = 2;
                                this.bypassStateSince = System.currentTimeMillis();
                            }

                            return;
                        }

                        this.bypassState = 0;
                        this.bypassStateSince = -1L;
                        this.breakStage = 0;
                        this.tickCounter = 0;
                        this.breakProgress = 0.0F;
                    } else if (this.bypassState == 2) {
                        long elapsed = System.currentTimeMillis() - this.bypassStateSince;
                        if (elapsed >= 2000L) {
                            this.restoreSlot();
                            this.resetBreaking();
                        }

                        return;
                    }
                }

                if (this.stopOnAttack.getValue()
                    && (mc.field_71439_g.field_70737_aN > 0 || Miau.playerStateManager.attacking)) {
                    return;
                }

                int slot = ItemUtil.findInventorySlot(
                    mc.field_71439_g.field_71071_by.field_70461_c,
                    mc.field_71441_e.func_180495_p(this.targetBed).func_177230_c()
                );
                if ((this.mode.getValue() == 0 || this.mode.getValue() == 2) && this.savedSlot == -1) {
                    this.savedSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                    mc.field_71439_g.field_71071_by.field_70461_c = slot;
                    this.syncHeldItem();
                }

                switch (this.breakStage) {
                    case 0:
                        if (!mc.field_71439_g.func_71039_bw()) {
                            this.doSwing();
                            PacketUtil.sendPacket(
                                new C07PacketPlayerDigging(
                                    Action.START_DESTROY_BLOCK, this.targetBed, this.getHitFacing(this.targetBed)
                                )
                            );
                            this.doSwing();
                            mc.field_71452_i.func_180532_a(this.targetBed, this.getHitFacing(this.targetBed));
                            this.breakStage = 1;
                        }
                        break;
                    case 1:
                        if (this.mode.getValue() == 1) {
                            this.readyToBreak = false;
                        }

                        if (this.mode.getValue() != 2
                            || !this.isBed
                            || this.currentBedPair != null && this.isBedVisible(this.currentBedPair)) {
                            this.breaking = true;
                            this.tickCounter++;
                            this.breakProgress = this.breakProgress
                                + this.getBreakDelta(
                                    mc.field_71441_e.func_180495_p(this.targetBed),
                                    this.targetBed,
                                    slot,
                                    mc.field_71439_g.field_70122_E
                                );
                            float tick = this.tickCounter;
                            IBlockState blockState = mc.field_71441_e.func_180495_p(this.targetBed);
                            boolean canBreak = mc.field_71439_g.field_70122_E && this.groundSpeed.getValue();
                            BlockPos target = this.targetBed;
                            float delta = tick * this.getBreakDelta(blockState, target, slot, canBreak);
                            mc.field_71452_i.func_180532_a(this.targetBed, this.getHitFacing(this.targetBed));
                            if (this.breakProgress >= 1.0F - 0.3F * (this.speed.getValue().intValue() / 100.0F)
                                || delta >= 1.0F - 0.3F * (this.speed.getValue().intValue() / 100.0F)) {
                                if (this.mode.getValue() == 1 || this.mode.getValue() == 2) {
                                    this.readyToBreak = true;
                                    this.savedSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                                    mc.field_71439_g.field_71071_by.field_70461_c = slot;
                                    this.syncHeldItem();
                                    if (mc.field_71439_g.func_71039_bw()) {
                                        this.savedSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                                        mc.field_71439_g.field_71071_by.field_70461_c = (
                                                mc.field_71439_g.field_71071_by.field_70461_c + 1
                                            )
                                            % 9;
                                        this.syncHeldItem();
                                    }
                                }

                                this.breaking = false;
                                PacketUtil.sendPacket(
                                    new C07PacketPlayerDigging(
                                        Action.STOP_DESTROY_BLOCK, this.targetBed, this.getHitFacing(this.targetBed)
                                    )
                                );
                                this.doSwing();
                                IBlockState blockState_ = mc.field_71441_e.func_180495_p(this.targetBed);
                                Block block = blockState_.func_177230_c();
                                if (block.func_149688_o() != Material.field_151579_a) {
                                    mc.field_71441_e
                                        .func_175718_b(2001, this.targetBed, Block.func_176210_f(blockState_));
                                    mc.field_71441_e.func_175698_g(this.targetBed);
                                }

                                if (block instanceof BlockBed) {
                                    this.timer.reset();
                                }

                                this.breakStage = 2;
                            }
                        }
                        break;
                    case 2:
                        this.restoreSlot();
                        this.resetBreaking();
                }

                if (this.targetBed != null) {
                    return;
                }
            }

            if (mc.field_71439_g.field_71075_bZ.field_75099_e && this.timer.hasTimeElapsed(500L)) {
                this.targetBed = this.findNearestBed();
                this.breakStage = 0;
                this.tickCounter = 0;
                this.breakProgress = 0.0F;
                this.isBed = this.targetBed != null
                    && mc.field_71441_e.func_180495_p(this.targetBed).func_177230_c() instanceof BlockBed;
                this.restoreSlot();
                if (this.targetBed != null) {
                    this.readyToBreak = true;
                }
            }

            if (this.targetBed == null) {
                Miau.delayManager.setDelayState(false, DelayModules.BED_NUKER);
            }
        }
    }

    @EventTarget(4)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            AutoBlockIn autoBlockIn = (AutoBlockIn)Miau.moduleManager.modules.get(AutoBlockIn.class);
            if (autoBlockIn.isEnabled()) {
                return;
            }

            if (this.isReady()) {
                if (this.mode.getValue() == 2) {
                    BedNuker.HitResult hit = this.computeBestHit(this.targetBed);
                    if (hit == null) {
                        hit = this.computeFallbackHit(this.targetBed);
                    }

                    if (hit != null) {
                        float[] rots = RotationUtil.calculate(hit.hitVec);
                        event.setRotation(rots[0], rots[1], 5);
                        event.setPervRotation(
                            this.moveFix.getValue() != 0 ? rots[0] : mc.field_71439_g.field_70177_z, 5
                        );
                    } else {
                        double x = this.targetBed.func_177958_n() + 0.5 - mc.field_71439_g.field_70165_t;
                        double y = this.targetBed.func_177956_o()
                            + 0.5
                            - mc.field_71439_g.field_70163_u
                            - mc.field_71439_g.func_70047_e();
                        double z = this.targetBed.func_177952_p() + 0.5 - mc.field_71439_g.field_70161_v;
                        float[] rots = RotationUtil.getRotationsTo(x, y, z, event.getYaw(), event.getPitch());
                        event.setRotation(rots[0], rots[1], 5);
                        event.setPervRotation(
                            this.moveFix.getValue() != 0 ? rots[0] : mc.field_71439_g.field_70177_z, 5
                        );
                    }
                } else {
                    double x = this.targetBed.func_177958_n() + 0.5 - mc.field_71439_g.field_70165_t;
                    double y = this.targetBed.func_177956_o()
                        + 0.5
                        - mc.field_71439_g.field_70163_u
                        - mc.field_71439_g.func_70047_e();
                    double z = this.targetBed.func_177952_p() + 0.5 - mc.field_71439_g.field_70161_v;
                    float[] rots = RotationUtil.getRotationsTo(x, y, z, event.getYaw(), event.getPitch());
                    event.setRotation(rots[0], rots[1], 5);
                    event.setPervRotation(this.moveFix.getValue() != 0 ? rots[0] : mc.field_71439_g.field_70177_z, 5);
                }
            }
        }
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled()
            && this.isBreaking()
            && !Miau.playerStateManager.attacking
            && !Miau.playerStateManager.digging
            && !Miau.playerStateManager.placing
            && !Miau.playerStateManager.swinging) {
            this.doSwing();
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()
            && this.moveFix.getValue() == 1
            && RotationState.isActived()
            && RotationState.getPriority() == 5.0F
            && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
        }
    }

    @EventTarget(1)
    public void onKnockback(KnockbackEvent event) {
        if (this.isEnabled()
            && !event.isCancelled()
            && !(event.getY() <= 0.0)
            && this.ignoreVelocity.getValue() == 1
            && this.targetBed != null) {
            event.setCancelled(true);
            event.setX(mc.field_71439_g.field_70159_w);
            event.setY(mc.field_71439_g.field_70181_x);
            event.setZ(mc.field_71439_g.field_70179_y);
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled()
            && this.targetBed != null
            && (!this.isBed || !this.surroundings.getValue())
            && this.showProgress.getValue() != 0) {
            HUD hud = (HUD)Miau.moduleManager.modules.get(HUD.class);
            float scale = hud.scale.getValue();
            String text = String.format("%d%%", (int)(this.calcProgress() * 100.0F));
            GlStateManager.func_179094_E();
            GlStateManager.func_179152_a(scale, scale, 0.0F);
            GlStateManager.func_179097_i();
            GlStateManager.func_179147_l();
            GlStateManager.func_179112_b(770, 771);
            int width = mc.field_71466_p.func_78256_a(text);
            mc.field_71466_p
                .func_175065_a(
                    text,
                    new ScaledResolution(mc).func_78326_a() / 2.0F / scale - width / 2.0F,
                    new ScaledResolution(mc).func_78328_b() / 5.0F * 2.0F / scale,
                    this.getProgressColor(this.showProgress.getValue()).getRGB() & 16777215 | -1090519040,
                    hud.shadow.getValue()
                );
            GlStateManager.func_179084_k();
            GlStateManager.func_179126_j();
            GlStateManager.func_179121_F();
        }
    }

    @EventTarget(3)
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && this.targetBed != null && !mc.field_71441_e.func_175623_d(this.targetBed)) {
            mc.field_71441_e
                .func_175715_c(mc.field_71439_g.func_145782_y(), this.targetBed, (int)(this.calcProgress() * 10.0F) - 1);
            if (this.showTarget.getValue() != 0) {
                BedESP bedESP = (BedESP)Miau.moduleManager.modules.get(BedESP.class);
                Color color = this.getProgressColor(this.showTarget.getValue());
                RenderUtil.enableRenderState();
                BlockPos target = this.targetBed;
                double newHeight = this.isBed ? bedESP.getBlockHeight() : 1.0;
                int r = color.getRed();
                int g = color.getBlue();
                int b = color.getGreen();
                RenderUtil.drawBlockBox(target, newHeight, r, b, g);
                RenderUtil.disableRenderState();
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.waitingForStart = false;
        this.whitelistScanAt = -1L;
        this.bedWhitelist.clear();
        this.resetBreaking();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!event.isCancelled()) {
            if (event.getPacket() instanceof S02PacketChat) {
                String text = ((S02PacketChat)event.getPacket()).func_148915_c().func_150254_d();
                if (text.contains("§e§lProtect your bed and destroy the enemy bed")
                    || text.contains("§e§lDestroy the enemy bed and then eliminate them")) {
                    this.waitingForStart = true;
                }
            }

            if (event.getPacket() instanceof S08PacketPlayerPosLook && this.waitingForStart) {
                this.waitingForStart = false;
                this.bedWhitelist.clear();
                this.scheduleWhitelistScan();
            }

            if (this.isEnabled()
                && this.targetBed != null
                && this.ignoreVelocity.getValue() == 2
                && Miau.delayManager.getDelayModule() != DelayModules.BED_NUKER) {
                if (event.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                    if (packet.func_149412_c() == mc.field_71439_g.func_145782_y() && packet.func_149410_e() > 0) {
                        Miau.delayManager.delay(DelayModules.BED_NUKER);
                        Miau.delayManager.delayedPacket.offer(packet);
                        event.setCancelled(true);
                    }
                }

                if (event.getPacket() instanceof S27PacketExplosion) {
                    S27PacketExplosion explosion = (S27PacketExplosion)event.getPacket();
                    if (explosion.func_149149_c() != 0.0F
                        || explosion.func_149144_d() != 0.0F
                        || explosion.func_149147_e() != 0.0F) {
                        Miau.delayManager.delay(DelayModules.BED_NUKER);
                        Miau.delayManager.delayedPacket.offer(explosion);
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled()
            && (
                this.isReady()
                    || this.targetBed != null
                        && mc.field_71476_x != null
                        && mc.field_71476_x.field_72313_a == MovingObjectType.BLOCK
            )) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled() && this.isReady()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isEnabled()
            && (
                this.isReady()
                    || this.targetBed != null
                        && mc.field_71476_x != null
                        && mc.field_71476_x.field_72313_a == MovingObjectType.BLOCK
            )) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (this.isEnabled() && this.savedSlot != -1) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onDisabled() {
        this.resetBreaking();
        this.savedSlot = -1;
        this.waitingForStart = false;
        this.whitelistScanAt = -1L;
        this.bedWhitelist.clear();
        Miau.delayManager.setDelayState(false, DelayModules.BED_NUKER);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }

    private static class HitResult {
        final Vec3 hitVec;
        final EnumFacing facing;
        final double distance;

        HitResult(Vec3 hitVec, EnumFacing facing, double distance) {
            this.hitVec = hitVec;
            this.facing = facing;
            this.distance = distance;
        }
    }
}
