package miau.module.modules.player;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import miau.Miau;
import miau.enums.BlinkModules;
import miau.event.EventTarget;
import miau.event.impl.HitBlockEvent;
import miau.event.impl.LeftClickMouseEvent;
import miau.event.impl.LivingUpdateEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.RightClickMouseEvent;
import miau.event.impl.SafeWalkEvent;
import miau.event.impl.StrafeEvent;
import miau.event.impl.SwapItemEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.management.RotationState;
import miau.module.Module;
import miau.module.modules.movement.LongJump;
import miau.module.modules.render.HUD;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.PercentProperty;
import miau.util.math.RandomUtil;
import miau.util.network.PacketUtil;
import miau.util.player.ItemUtil;
import miau.util.player.MoveUtil;
import miau.util.player.PlayerUtil;
import miau.util.player.RotationUtil;
import miau.util.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;
import org.lwjgl.input.Keyboard;

public class Scaffold2 extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final double[] placeOffsets = new double[]{
        0.03125,
        0.09375,
        0.15625,
        0.21875,
        0.28125,
        0.34375,
        0.40625,
        0.46875,
        0.53125,
        0.59375,
        0.65625,
        0.71875,
        0.78125,
        0.84375,
        0.90625,
        0.96875
    };
    private int rotationTick = 0;
    private int lastSlot = -1;
    private int blockCount = -1;
    private float yaw = -180.0F;
    private float pitch = 0.0F;
    private boolean canRotate = false;
    private int towerTick = 0;
    private int towerDelay = 0;
    private int stage = 0;
    private int startY = 256;
    private boolean shouldKeepY = false;
    private boolean towering = false;
    private EnumFacing targetFacing = null;
    private int safeStuckTicks = 0;
    private int safeStuckDelayTicks = 0;
    private double safePrevMotionY = 0.0;
    private double savedMotionX;
    private double savedMotionY;
    private double savedMotionZ;
    private boolean safeStuckActive = false;
    private boolean snapRotating = false;
    private boolean placedThisTick = false;
    private float lastSnapPlaceYaw = Float.NaN;
    private float lastSnapPlacePitch = Float.NaN;
    public final ModeProperty rotationMode = new ModeProperty(
        "rotations",
        2,
        new String[]{"None", "Default", "Backwards", "Sideways", "Godbridge", "Smooth", "Hypixel", "Snap"}
    );
    public final FloatProperty tellystartrotationminspeed = new FloatProperty(
        "telly-start-rotation-min-speed",
        90.0F,
        1.0F,
        180.0F,
        () -> this.keepY.getValue() == 3 || this.keepY.getValue() == 4
    );
    public final FloatProperty tellystartrotationmaxspeed = new FloatProperty(
        "telly-start-rotation-max-speed",
        95.0F,
        1.0F,
        180.0F,
        () -> this.keepY.getValue() == 3 || this.keepY.getValue() == 4
    );
    public final FloatProperty tellynormalrotationminspeed = new FloatProperty(
        "telly-normal-rotation-min-speed",
        30.0F,
        1.0F,
        180.0F,
        () -> this.keepY.getValue() == 3 || this.keepY.getValue() == 4
    );
    public final FloatProperty tellynormalrotationmaxspeed = new FloatProperty(
        "telly-normal-rotation-max-speed",
        35.0F,
        1.0F,
        180.0F,
        () -> this.keepY.getValue() == 3 || this.keepY.getValue() == 4
    );
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"None", "Silent"});
    public final ModeProperty sprintMode = new ModeProperty("sprint", 0, new String[]{"None", "Vanilla"});
    public final PercentProperty groundMotion = new PercentProperty("ground-motion", 100);
    public final PercentProperty airMotion = new PercentProperty("air-motion", 100);
    public final PercentProperty speedMotion = new PercentProperty("speed-motion", 100);
    public final ModeProperty tower = new ModeProperty("tower", 0, new String[]{"None", "Vanilla", "Extra", "Telly"});
    public final BooleanProperty hypixeltower = new BooleanProperty(
        "hypixeltower", false, () -> this.tower.getValue() == 3
    );
    public final BooleanProperty safe = new BooleanProperty("safe", false, () -> this.tower.getValue() == 3);
    public final IntProperty safeStuckDelayTicksProperty = new IntProperty(
        "safe-delay-ticks", 1, 1, 3, () -> this.tower.getValue() == 3 && this.safe.getValue()
    );
    public final ModeProperty keepY = new ModeProperty(
        "keep-y", 0, new String[]{"None", "Vanilla", "Extra", "Telly", "ExtraTelly"}
    );
    public final BooleanProperty keepYonPress = new BooleanProperty(
        "keep-y-on-press", false, () -> this.keepY.getValue() != 0
    );
    public final BooleanProperty disableWhileJumpActive = new BooleanProperty(
        "no-keep-y-on-jump-potion", false, () -> this.keepY.getValue() != 0
    );
    public final BooleanProperty multiplace = new BooleanProperty("multi-place", true);
    public final BooleanProperty safeWalk = new BooleanProperty("safe-walk", true);
    public final BooleanProperty swing = new BooleanProperty("swing", true);
    public final BooleanProperty itemSpoof = new BooleanProperty("item-spoof", false);
    public final BooleanProperty blockCounter = new BooleanProperty("block-counter", true);
    public final BooleanProperty eagle = new BooleanProperty("eagle", false);
    public final FloatProperty edgeDistance = new FloatProperty(
        "edge-distance", 0.13F, 0.0F, 0.5F, () -> this.eagle.getValue()
    );
    public final IntProperty sneakDelay = new IntProperty("sneak-delay", 80, 0, 500, () -> this.eagle.getValue());
    public final IntProperty blocksPerSneak = new IntProperty("blocks-per-sneak", 1, 1, 5, () -> this.eagle.getValue());
    private boolean eagleSneaking = false;
    private int eagleSneakTicks = 0;
    private long eagleLastSneakTime = 0L;
    private int eagleBlocksPlaced = 0;

    private boolean shouldStopSprint() {
        if (this.isTowering()) {
            return false;
        }

        boolean stage = this.keepY.getValue() == 1 || this.keepY.getValue() == 2 || this.keepY.getValue() == 4;
        return (!stage || this.stage <= 0) && this.sprintMode.getValue() == 0;
    }

    private boolean canPlace() {
        BedNuker bedNuker = (BedNuker)Miau.moduleManager.modules.get(BedNuker.class);
        if (bedNuker.isEnabled() && bedNuker.isReady()) {
            return false;
        }

        LongJump longJump = (LongJump)Miau.moduleManager.modules.get(LongJump.class);
        return !longJump.isEnabled() || !longJump.isAutoMode() || longJump.isJumping();
    }

    private EnumFacing getBestFacing(BlockPos blockPos1, BlockPos blockPos3) {
        double offset = 0.0;
        EnumFacing enumFacing = null;

        for (EnumFacing facing : EnumFacing.field_82609_l) {
            if (facing != EnumFacing.DOWN) {
                BlockPos pos = blockPos1.func_177972_a(facing);
                if (pos.func_177956_o() <= blockPos3.func_177956_o()) {
                    double distance = pos.func_177957_d(
                        blockPos3.func_177958_n() + 0.5,
                        blockPos3.func_177956_o() + 0.5,
                        blockPos3.func_177952_p() + 0.5
                    );
                    if (enumFacing == null || distance < offset || distance == offset && facing == EnumFacing.UP) {
                        offset = distance;
                        enumFacing = facing;
                    }
                }
            }
        }

        return enumFacing;
    }

    private Scaffold2.BlockData getBlockData() {
        int startY = MathHelper.func_76128_c(mc.field_71439_g.field_70163_u);
        BlockPos targetPos = new BlockPos(
            MathHelper.func_76128_c(mc.field_71439_g.field_70165_t),
            (this.stage != 0 && !this.shouldKeepY ? Math.min(startY, this.startY) : startY) - 1,
            MathHelper.func_76128_c(mc.field_71439_g.field_70161_v)
        );
        if (!BlockUtil.isReplaceable(targetPos)) {
            return null;
        }

        ArrayList<BlockPos> positions = new ArrayList<>();

        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 0; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = targetPos.func_177982_a(x, y, z);
                    if (!BlockUtil.isReplaceable(pos)
                        && !BlockUtil.isInteractable(pos)
                        && !(
                            mc.field_71439_g
                                    .func_70011_f(
                                        pos.func_177958_n() + 0.5, pos.func_177956_o() + 0.5, pos.func_177952_p() + 0.5
                                    )
                                > mc.field_71442_b.func_78757_d()
                        )
                        && (this.stage == 0 || this.shouldKeepY || pos.func_177956_o() < this.startY)) {
                        for (EnumFacing facing : EnumFacing.field_82609_l) {
                            if (facing != EnumFacing.DOWN) {
                                BlockPos blockPos = pos.func_177972_a(facing);
                                if (BlockUtil.isReplaceable(blockPos)) {
                                    positions.add(pos);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (positions.isEmpty()) {
            return null;
        }

        positions.sort(
            Comparator.comparingDouble(
                o -> o.func_177957_d(
                    targetPos.func_177958_n() + 0.5, targetPos.func_177956_o() + 0.5, targetPos.func_177952_p() + 0.5
                )
            )
        );
        BlockPos blockPos = positions.get(0);
        EnumFacing facing = this.getBestFacing(blockPos, targetPos);
        return facing == null ? null : new Scaffold2.BlockData(blockPos, facing);
    }

    private void place(BlockPos blockPos, EnumFacing enumFacing, Vec3 vec3) {
        if (ItemUtil.isHoldingBlock()
            && this.blockCount > 0
            && mc.field_71442_b
                .func_178890_a(
                    mc.field_71439_g,
                    mc.field_71441_e,
                    mc.field_71439_g.field_71071_by.func_70448_g(),
                    blockPos,
                    enumFacing,
                    vec3
                )) {
            if (mc.field_71442_b.func_178889_l() != GameType.CREATIVE) {
                this.blockCount--;
            }

            this.placedThisTick = true;
            this.eagleBlocksPlaced++;
            if (this.swing.getValue()) {
                mc.field_71439_g.func_71038_i();
            } else {
                PacketUtil.sendPacket(new C0APacketAnimation());
            }
        }
    }

    private MovingObjectPosition getPlacementMop(Scaffold2.BlockData blockData, float yaw, float pitch) {
        MovingObjectPosition mop = RotationUtil.rayTrace(yaw, pitch, mc.field_71442_b.func_78757_d(), 1.0F);
        return mop != null
                && mop.field_72313_a == MovingObjectType.BLOCK
                && mop.func_178782_a().equals(blockData.blockPos())
                && mop.field_178784_b == blockData.facing()
            ? mop
            : null;
    }

    private boolean isDuplicateSnapRotation(float yaw, float pitch) {
        return !Float.isNaN(this.lastSnapPlaceYaw)
            && Math.abs(MathHelper.func_76142_g(yaw - this.lastSnapPlaceYaw)) < 0.35F;
    }

    private float[] getSnapRotation(Scaffold2.BlockData blockData, float yaw, float pitch) {
        float baseYaw = RotationUtil.quantizeAngle(yaw);
        float basePitch = RotationUtil.quantizeAngle(MathHelper.func_76131_a(pitch, -90.0F, 90.0F));
        if (!this.isDuplicateSnapRotation(baseYaw, basePitch)) {
            return new float[]{baseYaw, basePitch};
        }

        for (int i = 0; i < 24; i++) {
            float yawStep = 0.35F + 0.075F * (i / 2);
            float pitchStep = 0.025F + 0.01F * (i / 3);
            float testYaw = RotationUtil.quantizeAngle(baseYaw + (i % 2 == 0 ? yawStep : -yawStep));
            float testPitch = RotationUtil.quantizeAngle(
                MathHelper.func_76131_a(basePitch + (i % 4 < 2 ? pitchStep : -pitchStep), -90.0F, 90.0F)
            );
            if (!this.isDuplicateSnapRotation(testYaw, testPitch)
                && this.getPlacementMop(blockData, testYaw, testPitch) != null) {
                return new float[]{testYaw, testPitch};
            }
        }

        return null;
    }

    private void rememberSnapRotation() {
        this.lastSnapPlaceYaw = this.yaw;
        this.lastSnapPlacePitch = this.pitch;
    }

    private EnumFacing yawToFacing(float yaw) {
        if (yaw < -135.0F || yaw > 135.0F) {
            return EnumFacing.NORTH;
        } else if (yaw < -45.0F) {
            return EnumFacing.EAST;
        } else {
            return yaw < 45.0F ? EnumFacing.SOUTH : EnumFacing.WEST;
        }
    }

    private double distanceToEdge(EnumFacing enumFacing) {
        switch (enumFacing) {
            case NORTH:
                return mc.field_71439_g.field_70161_v - Math.floor(mc.field_71439_g.field_70161_v);
            case EAST:
                return Math.ceil(mc.field_71439_g.field_70165_t) - mc.field_71439_g.field_70165_t;
            case SOUTH:
                return Math.ceil(mc.field_71439_g.field_70161_v) - mc.field_71439_g.field_70161_v;
            case WEST:
            default:
                return mc.field_71439_g.field_70165_t - Math.floor(mc.field_71439_g.field_70165_t);
        }
    }

    private boolean isNearEdge() {
        if (!mc.field_71439_g.field_70122_E) {
            return false;
        }

        double fracX = mc.field_71439_g.field_70165_t - Math.floor(mc.field_71439_g.field_70165_t);
        double fracZ = mc.field_71439_g.field_70161_v - Math.floor(mc.field_71439_g.field_70161_v);
        double threshold = this.edgeDistance.getValue().floatValue();
        double minDist = Math.min(Math.min(fracX, 1.0 - fracX), Math.min(fracZ, 1.0 - fracZ));
        return minDist <= threshold;
    }

    private boolean shouldSneak() {
        if (this.eagle.getValue() && mc.field_71439_g.field_70122_E) {
            if (this.eagleBlocksPlaced < this.blocksPerSneak.getValue()) {
                return false;
            } else {
                return System.currentTimeMillis() - this.eagleLastSneakTime < this.sneakDelay.getValue().intValue()
                    ? false
                    : this.isNearEdge();
            }
        } else {
            return false;
        }
    }

    private void updateEagle() {
        if (!this.eagle.getValue()) {
            this.eagleSneaking = false;
            this.eagleSneakTicks = 0;
        } else if (this.eagleSneakTicks > 0) {
            this.eagleSneakTicks--;
            if (this.eagleSneakTicks == 0) {
                this.eagleSneaking = false;
            }
        } else {
            if (this.shouldSneak()) {
                this.eagleSneaking = true;
                this.eagleSneakTicks = 2;
                this.eagleLastSneakTime = System.currentTimeMillis();
                this.eagleBlocksPlaced = 0;
            }
        }
    }

    private float getSpeed() {
        if (!mc.field_71439_g.field_70122_E) {
            return this.airMotion.getValue().intValue() / 100.0F;
        } else {
            return MoveUtil.getSpeedLevel() > 0
                ? this.speedMotion.getValue().intValue() / 100.0F
                : this.groundMotion.getValue().intValue() / 100.0F;
        }
    }

    private double getRandomOffset() {
        return 0.2155 - RandomUtil.nextDouble(1.0E-4, 9.0E-4);
    }

    private float getCurrentYaw() {
        return MoveUtil.adjustYaw(mc.field_71439_g.field_70177_z, MoveUtil.getForwardValue(), MoveUtil.getLeftValue());
    }

    private boolean isDiagonal(float yaw) {
        float absYaw = Math.abs(yaw % 90.0F);
        return absYaw > 20.0F && absYaw < 70.0F;
    }

    private boolean isTowering() {
        if (mc.field_71439_g.field_70122_E && MoveUtil.isForwardPressed() && !PlayerUtil.isAirAbove()) {
            boolean keepY = this.keepY.getValue() == 3 || this.keepY.getValue() == 4;
            boolean tower = this.tower.getValue() == 3;
            return keepY && this.stage > 0 || tower && mc.field_71474_y.field_74314_A.func_151470_d();
        } else {
            return false;
        }
    }

    public Scaffold2() {
        super("Scaffold2", false);
    }

    public int getSlot() {
        return this.lastSlot;
    }

    @EventTarget(1)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            this.placedThisTick = false;
            if (this.safeStuckDelayTicks > 0) {
                this.safeStuckDelayTicks--;
                if (this.safeStuckDelayTicks <= 0) {
                    this.safeStuckTicks = 1;
                }
            }

            if (this.safeStuckTicks > 0) {
                if (!this.safeStuckActive) {
                    this.savedMotionX = mc.field_71439_g.field_70159_w;
                    this.savedMotionY = mc.field_71439_g.field_70181_x;
                    this.savedMotionZ = mc.field_71439_g.field_70179_y;
                    this.safeStuckActive = true;
                }

                Miau.blinkManager.setBlinkState(true, BlinkModules.BLINK);
                mc.field_71439_g.field_70159_w = 0.0;
                mc.field_71439_g.field_70181_x = 0.0;
                mc.field_71439_g.field_70179_y = 0.0;
            } else if (this.safeStuckActive) {
                Miau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                mc.field_71439_g.field_70159_w = this.savedMotionX;
                mc.field_71439_g.field_70181_x = this.savedMotionY;
                mc.field_71439_g.field_70179_y = this.savedMotionZ;
                this.safeStuckActive = false;
            }

            if (this.rotationTick > 0) {
                this.rotationTick--;
            }

            this.updateEagle();
            if (this.hypixeltower.getValue()
                && mc.field_71439_g.field_70181_x <= 0.0
                && Math.sqrt(
                        mc.field_71439_g.field_70159_w * mc.field_71439_g.field_70159_w
                            + mc.field_71439_g.field_70179_y * mc.field_71439_g.field_70179_y
                    )
                    <= 0.02
                && mc.field_71439_g.field_70181_x >= -0.09
                && !Keyboard.isKeyDown(mc.field_71474_y.field_74351_w.func_151463_i())
                && !Keyboard.isKeyDown(mc.field_71474_y.field_74368_y.func_151463_i())
                && !Keyboard.isKeyDown(mc.field_71474_y.field_74370_x.func_151463_i())
                && !Keyboard.isKeyDown(mc.field_71474_y.field_74366_z.func_151463_i())
                && Keyboard.isKeyDown(mc.field_71474_y.field_74314_A.func_151463_i())) {
                mc.field_71439_g.field_70181_x = -0.38;
            }

            if (mc.field_71439_g.field_70122_E) {
                if (this.stage > 0) {
                    this.stage--;
                }

                if (this.stage < 0) {
                    this.stage++;
                }

                if (this.stage == 0
                    && this.keepY.getValue() != 0
                    && (!this.keepYonPress.getValue() || PlayerUtil.isUsingItem())
                    && (!this.disableWhileJumpActive.getValue() || !mc.field_71439_g.func_70644_a(Potion.field_76430_j))
                    && !mc.field_71474_y.field_74314_A.func_151470_d()) {
                    this.stage = 1;
                }

                this.startY = this.shouldKeepY ? this.startY : MathHelper.func_76128_c(mc.field_71439_g.field_70163_u);
                this.shouldKeepY = false;
                this.towering = false;
            }

            if (this.canPlace()) {
                ItemStack stack = mc.field_71439_g.func_70694_bm();
                int count = ItemUtil.isBlock(stack) ? stack.field_77994_a : 0;
                this.blockCount = Math.min(this.blockCount, count);
                if (this.blockCount <= 0) {
                    int slot = mc.field_71439_g.field_71071_by.field_70461_c;
                    if (this.blockCount == 0) {
                        slot--;
                    }

                    for (int i = slot; i > slot - 9; i--) {
                        int hotbarSlot = (i % 9 + 9) % 9;
                        ItemStack candidate = mc.field_71439_g.field_71071_by.func_70301_a(hotbarSlot);
                        if (ItemUtil.isBlock(candidate)) {
                            mc.field_71439_g.field_71071_by.field_70461_c = hotbarSlot;
                            this.blockCount = candidate.field_77994_a;
                            break;
                        }
                    }
                }

                float currentYaw = this.getCurrentYaw();
                float yawDiffTo180 = RotationUtil.wrapAngleDiff(currentYaw - 180.0F, event.getYaw());
                float diagonalYaw = this.isDiagonal(currentYaw)
                    ? yawDiffTo180
                    : RotationUtil.wrapAngleDiff(
                        currentYaw - 135.0F * ((currentYaw + 180.0F) % 90.0F < 45.0F ? 1.0F : -1.0F), event.getYaw()
                    );
                boolean snapMode = this.rotationMode.getValue() == 7;
                this.snapRotating = false;
                if (!this.canRotate) {
                    switch (this.rotationMode.getValue()) {
                        case 1:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                                break;
                            }

                            this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                            break;
                        case 2:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                                break;
                            }

                            this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                            break;
                        case 3:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                                break;
                            }

                            this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                            break;
                        case 4:
                            float roundedYaw = Math.round(currentYaw / 45.0F) * 45.0F;
                            this.yaw = RotationUtil.quantizeAngle(roundedYaw);
                            if (this.pitch == 0.0F || !this.canRotate) {
                                float godBridgePitch = 79.3F;
                                this.pitch = RotationUtil.quantizeAngle(godBridgePitch);
                            }
                            break;
                        case 5:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                                break;
                            }

                            float targetYaw = this.isDiagonal(currentYaw) ? diagonalYaw : yawDiffTo180;
                            float yawDiff = MathHelper.func_76142_g(targetYaw - this.yaw);
                            float pitchDiff = MathHelper.func_76142_g(85.0F - this.pitch);
                            float yawTolerance = this.rotationTick >= 2
                                ? RandomUtil.nextFloat(
                                    this.tellystartrotationminspeed.getValue(),
                                    this.tellystartrotationmaxspeed.getValue()
                                )
                                : RandomUtil.nextFloat(
                                    this.tellynormalrotationminspeed.getValue(),
                                    this.tellynormalrotationmaxspeed.getValue()
                                );
                            float pitchTolerance = this.rotationTick >= 2
                                ? RandomUtil.nextFloat(
                                    this.tellystartrotationminspeed.getValue(),
                                    this.tellystartrotationmaxspeed.getValue()
                                )
                                : RandomUtil.nextFloat(
                                    this.tellynormalrotationminspeed.getValue(),
                                    this.tellynormalrotationmaxspeed.getValue()
                                );
                            this.yaw = RotationUtil.quantizeAngle(
                                this.yaw + RotationUtil.clampAngle(yawDiff, yawTolerance)
                            );
                            this.pitch = RotationUtil.quantizeAngle(
                                this.pitch + RotationUtil.clampAngle(pitchDiff, pitchTolerance)
                            );
                            break;
                        case 6:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                                this.pitch = RotationUtil.quantizeAngle(85.0F);
                                break;
                            }

                            this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                            break;
                        case 7:
                            this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                            this.pitch = RotationUtil.quantizeAngle(85.0F);
                    }
                }

                Scaffold2.BlockData blockData = this.getBlockData();
                Vec3 hitVec = null;
                if (blockData != null) {
                    double[] x = placeOffsets;
                    double[] y = placeOffsets;
                    double[] z = placeOffsets;
                    switch (blockData.facing()) {
                        case NORTH:
                            z = new double[]{0.0};
                            break;
                        case EAST:
                            x = new double[]{1.0};
                            break;
                        case SOUTH:
                            z = new double[]{1.0};
                            break;
                        case WEST:
                            x = new double[]{0.0};
                            break;
                        case DOWN:
                            y = new double[]{0.0};
                            break;
                        case UP:
                            y = new double[]{1.0};
                    }

                    float bestYaw = -180.0F;
                    float bestPitch = 0.0F;
                    float bestDiff = 0.0F;

                    for (double dx : x) {
                        for (double dy : y) {
                            for (double dz : z) {
                                double relX = blockData.blockPos().func_177958_n()
                                    + dx
                                    - mc.field_71439_g.field_70165_t;
                                double relY = blockData.blockPos().func_177956_o()
                                    + dy
                                    - mc.field_71439_g.field_70163_u
                                    - mc.field_71439_g.func_70047_e();
                                double relZ = blockData.blockPos().func_177952_p()
                                    + dz
                                    - mc.field_71439_g.field_70161_v;
                                float baseYaw = RotationUtil.wrapAngleDiff(this.yaw, event.getYaw());
                                float[] rotations = RotationUtil.getRotationsTo(relX, relY, relZ, baseYaw, this.pitch);
                                MovingObjectPosition mop = RotationUtil.rayTrace(
                                    rotations[0], rotations[1], mc.field_71442_b.func_78757_d(), 1.0F
                                );
                                if (mop != null
                                    && mop.field_72313_a == MovingObjectType.BLOCK
                                    && mop.func_178782_a().equals(blockData.blockPos())
                                    && mop.field_178784_b == blockData.facing()) {
                                    float totalDiff = Math.abs(rotations[0] - baseYaw)
                                        + Math.abs(rotations[1] - this.pitch);
                                    if (bestYaw == -180.0F && bestPitch == 0.0F || totalDiff < bestDiff) {
                                        bestYaw = rotations[0];
                                        bestPitch = rotations[1];
                                        bestDiff = totalDiff;
                                        hitVec = mop.field_72307_f;
                                    }
                                }
                            }
                        }
                    }

                    if (bestYaw != -180.0F || bestPitch != 0.0F) {
                        this.yaw = bestYaw;
                        this.pitch = bestPitch;
                        this.canRotate = true;
                    }
                }

                boolean towerRotating = this.towering || this.isTowering();
                boolean snapAlreadyLooking = false;
                boolean snapCanPlace = true;
                if (snapMode && !towerRotating && blockData != null) {
                    MovingObjectPosition currentMop = this.getPlacementMop(blockData, event.getYaw(), event.getPitch());
                    if (currentMop != null) {
                        float[] snapRotation = this.getSnapRotation(blockData, event.getYaw(), event.getPitch());
                        if (snapRotation == null) {
                            snapCanPlace = false;
                            hitVec = null;
                        } else {
                            this.yaw = snapRotation[0];
                            this.pitch = snapRotation[1];
                            this.canRotate = true;
                            MovingObjectPosition snapMop = this.getPlacementMop(blockData, this.yaw, this.pitch);
                            hitVec = snapMop != null ? snapMop.field_72307_f : currentMop.field_72307_f;
                            this.snapRotating = true;
                            if (this.rotationTick > 1) {
                                this.rotationTick = 1;
                            }
                        }
                    } else if (hitVec != null && this.canRotate) {
                        float[] snapRotation = this.getSnapRotation(blockData, this.yaw, this.pitch);
                        if (snapRotation == null) {
                            snapCanPlace = false;
                            hitVec = null;
                        } else {
                            this.yaw = snapRotation[0];
                            this.pitch = snapRotation[1];
                            MovingObjectPosition snapMop = this.getPlacementMop(blockData, this.yaw, this.pitch);
                            if (snapMop != null) {
                                hitVec = snapMop.field_72307_f;
                            }

                            this.snapRotating = true;
                            if (this.rotationTick > 1) {
                                this.rotationTick = 1;
                            }
                        }
                    }
                }

                if (this.canRotate
                    && MoveUtil.isForwardPressed()
                    && Math.abs(MathHelper.func_76142_g(yawDiffTo180 - this.yaw)) < 90.0F) {
                    switch (this.rotationMode.getValue()) {
                        case 2:
                            this.yaw = RotationUtil.quantizeAngle(yawDiffTo180);
                            break;
                        case 3:
                            this.yaw = RotationUtil.quantizeAngle(diagonalYaw);
                    }
                }

                if (this.rotationMode.getValue() != 0 && (!snapMode || this.snapRotating || towerRotating)) {
                    float targetYaw = this.yaw;
                    float targetPitch = this.pitch;
                    if (this.towering
                        && (mc.field_71439_g.field_70181_x > 0.0 || mc.field_71439_g.field_70163_u > this.startY + 1)) {
                        float yawDiff = MathHelper.func_76142_g(this.yaw - event.getYaw());
                        float tolerance = this.rotationTick >= 2
                            ? RandomUtil.nextFloat(
                                this.tellystartrotationminspeed.getValue(), this.tellystartrotationmaxspeed.getValue()
                            )
                            : RandomUtil.nextFloat(
                                this.tellynormalrotationminspeed.getValue(),
                                this.tellynormalrotationmaxspeed.getValue()
                            );
                        if (Math.abs(yawDiff) > tolerance) {
                            float clampedYaw = RotationUtil.clampAngle(yawDiff, tolerance);
                            targetYaw = RotationUtil.quantizeAngle(event.getYaw() + clampedYaw);
                            this.rotationTick = Math.max(this.rotationTick, 1);
                        }
                    }

                    if (towerRotating && this.isTowering()) {
                        float yawDelta = MathHelper.func_76142_g(mc.field_71439_g.field_70177_z - event.getYaw());
                        targetYaw = RotationUtil.quantizeAngle(
                            event.getYaw() + yawDelta * RandomUtil.nextFloat(0.98F, 0.99F)
                        );
                        targetPitch = RotationUtil.quantizeAngle(RandomUtil.nextFloat(30.0F, 80.0F));
                        this.rotationTick = 3;
                        this.towering = true;
                    }

                    event.setRotation(targetYaw, targetPitch, 3);
                    if (this.moveFix.getValue() == 1) {
                        event.setPervRotation(targetYaw, 3);
                    }
                }

                if (blockData != null
                    && hitVec != null
                    && snapCanPlace
                    && (this.rotationTick <= 0 || snapAlreadyLooking)) {
                    this.place(blockData.blockPos(), blockData.facing(), hitVec);
                    if (snapMode) {
                        this.rememberSnapRotation();
                    }

                    if (this.multiplace.getValue() && !snapMode) {
                        for (int i = 0; i < 3; i++) {
                            blockData = this.getBlockData();
                            if (blockData == null) {
                                break;
                            }

                            MovingObjectPosition mop = RotationUtil.rayTrace(
                                this.yaw, this.pitch, mc.field_71442_b.func_78757_d(), 1.0F
                            );
                            if (mop != null
                                && mop.field_72313_a == MovingObjectType.BLOCK
                                && mop.func_178782_a().equals(blockData.blockPos())
                                && mop.field_178784_b == blockData.facing()) {
                                this.place(blockData.blockPos(), blockData.facing(), mop.field_72307_f);
                            } else {
                                hitVec = BlockUtil.getClickVec(blockData.blockPos(), blockData.facing());
                                double dx = hitVec.field_72450_a - mc.field_71439_g.field_70165_t;
                                double dy = hitVec.field_72448_b
                                    - mc.field_71439_g.field_70163_u
                                    - mc.field_71439_g.func_70047_e();
                                double dz = hitVec.field_72449_c - mc.field_71439_g.field_70161_v;
                                float[] rotations = RotationUtil.getRotationsTo(
                                    dx, dy, dz, event.getYaw(), event.getPitch()
                                );
                                if (!(Math.abs(rotations[0] - this.yaw) < 120.0F)
                                    || !(Math.abs(rotations[1] - this.pitch) < 60.0F)) {
                                    break;
                                }

                                mop = RotationUtil.rayTrace(
                                    rotations[0], rotations[1], mc.field_71442_b.func_78757_d(), 1.0F
                                );
                                if (mop == null
                                    || mop.field_72313_a != MovingObjectType.BLOCK
                                    || !mop.func_178782_a().equals(blockData.blockPos())
                                    || mop.field_178784_b != blockData.facing()) {
                                    break;
                                }

                                this.place(blockData.blockPos(), blockData.facing(), mop.field_72307_f);
                            }
                        }
                    }
                }

                if (this.targetFacing != null) {
                    if (this.rotationTick <= 0 && !this.placedThisTick) {
                        int playerBlockX = MathHelper.func_76128_c(mc.field_71439_g.field_70165_t);
                        int playerBlockY = MathHelper.func_76128_c(mc.field_71439_g.field_70163_u);
                        int playerBlockZ = MathHelper.func_76128_c(mc.field_71439_g.field_70161_v);
                        BlockPos belowPlayer = new BlockPos(playerBlockX, playerBlockY - 1, playerBlockZ);
                        hitVec = BlockUtil.getHitVec(belowPlayer, this.targetFacing, this.yaw, this.pitch);
                        this.place(belowPlayer, this.targetFacing, hitVec);
                    }

                    this.targetFacing = null;
                } else if ((this.keepY.getValue() == 2 || this.keepY.getValue() == 4)
                    && this.stage > 0
                    && !mc.field_71439_g.field_70122_E) {
                    int nextBlockY = MathHelper.func_76128_c(
                        mc.field_71439_g.field_70163_u + mc.field_71439_g.field_70181_x
                    );
                    if (nextBlockY <= this.startY && mc.field_71439_g.field_70163_u > this.startY + 1) {
                        this.shouldKeepY = true;
                        blockData = this.getBlockData();
                        if (blockData != null && this.rotationTick <= 0 && !this.placedThisTick) {
                            MovingObjectPosition mop = this.getPlacementMop(blockData, this.yaw, this.pitch);
                            if (mop != null) {
                                this.place(blockData.blockPos(), blockData.facing(), mop.field_72307_f);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled()) {
            if (this.safeStuckTicks > 0) {
                event.setForward(0.0F);
                event.setStrafe(0.0F);
            } else {
                if (!mc.field_71439_g.field_70123_F
                    && mc.field_71439_g.field_70737_aN <= 5
                    && !mc.field_71439_g.func_70644_a(Potion.field_76430_j)
                    && mc.field_71474_y.field_74314_A.func_151470_d()
                    && ItemUtil.isHoldingBlock()) {
                    int yState = (int)(mc.field_71439_g.field_70163_u % 1.0 * 100.0);
                    switch (this.tower.getValue()) {
                        case 1:
                            switch (this.towerTick) {
                                case 0:
                                    if (mc.field_71439_g.field_70122_E) {
                                        this.towerTick = 1;
                                        mc.field_71439_g.field_70181_x = -0.0784000015258789;
                                    }

                                    return;
                                case 1:
                                    if (yState == 0 && PlayerUtil.isAirBelow()) {
                                        this.startY = MathHelper.func_76128_c(mc.field_71439_g.field_70163_u);
                                        this.towerTick = 2;
                                        mc.field_71439_g.field_70181_x = 0.42F;
                                        if (MoveUtil.isForwardPressed()) {
                                            MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                                        } else {
                                            MoveUtil.setSpeed(0.0);
                                            event.setForward(0.0F);
                                            event.setStrafe(0.0F);
                                        }

                                        return;
                                    }

                                    this.towerTick = 0;
                                    return;
                                case 2:
                                    this.towerTick = 3;
                                    mc.field_71439_g.field_70181_x = 0.75 - mc.field_71439_g.field_70163_u % 1.0;
                                    return;
                                case 3:
                                    this.towerTick = 1;
                                    mc.field_71439_g.field_70181_x = 1.0 - mc.field_71439_g.field_70163_u % 1.0;
                                    return;
                                default:
                                    this.towerTick = 0;
                                    return;
                            }
                        case 2:
                            switch (this.towerTick) {
                                case 0:
                                    if (mc.field_71439_g.field_70122_E) {
                                        this.towerTick = 1;
                                        mc.field_71439_g.field_70181_x = -0.0784000015258789;
                                    }

                                    return;
                                case 1:
                                    if (yState == 0 && PlayerUtil.isAirBelow()) {
                                        this.startY = MathHelper.func_76128_c(mc.field_71439_g.field_70163_u);
                                        if (!MoveUtil.isForwardPressed()) {
                                            this.towerDelay = 2;
                                            MoveUtil.setSpeed(0.0);
                                            event.setForward(0.0F);
                                            event.setStrafe(0.0F);
                                            EnumFacing facing = this.yawToFacing(
                                                MathHelper.func_76142_g(this.yaw - 180.0F)
                                            );
                                            double distance = this.distanceToEdge(facing);
                                            if (distance > 0.1) {
                                                if (mc.field_71439_g.field_70122_E) {
                                                    Vec3i directionVec = facing.func_176730_m();
                                                    double offset = Math.min(this.getRandomOffset(), distance - 0.05);
                                                    double jitter = RandomUtil.nextDouble(0.02, 0.03);
                                                    AxisAlignedBB nextBox = mc.field_71439_g
                                                        .func_174813_aQ()
                                                        .func_72317_d(
                                                            directionVec.func_177958_n() * (offset - jitter),
                                                            0.0,
                                                            directionVec.func_177952_p() * (offset - jitter)
                                                        );
                                                    if (mc.field_71441_e
                                                        .func_72945_a(mc.field_71439_g, nextBox)
                                                        .isEmpty()) {
                                                        mc.field_71439_g.field_70181_x = -0.0784000015258789;
                                                        mc.field_71439_g
                                                            .func_70107_b(
                                                                nextBox.field_72340_a
                                                                    + (nextBox.field_72336_d - nextBox.field_72340_a)
                                                                        / 2.0,
                                                                nextBox.field_72338_b,
                                                                nextBox.field_72339_c
                                                                    + (nextBox.field_72334_f - nextBox.field_72339_c)
                                                                        / 2.0
                                                            );
                                                    }

                                                    return;
                                                }
                                            } else {
                                                this.towerTick = 2;
                                                this.targetFacing = facing;
                                                mc.field_71439_g.field_70181_x = 0.42F;
                                            }

                                            return;
                                        }

                                        this.towerTick = 2;
                                        this.towerDelay++;
                                        mc.field_71439_g.field_70181_x = 0.42F;
                                        MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                                        return;
                                    }

                                    this.towerTick = 0;
                                    this.towerDelay = 0;
                                    return;
                                case 2:
                                    this.towerTick = 3;
                                    mc.field_71439_g.field_70181_x = mc.field_71439_g.field_70181_x
                                        - RandomUtil.nextDouble(0.00101, 0.00109);
                                    return;
                                case 3:
                                    if (this.towerDelay >= 4) {
                                        this.towerTick = 4;
                                        this.towerDelay = 0;
                                    } else {
                                        this.towerTick = 1;
                                        mc.field_71439_g.field_70181_x = 1.0 - mc.field_71439_g.field_70163_u % 1.0;
                                    }

                                    return;
                                case 4:
                                    this.towerTick = 5;
                                    return;
                                case 5:
                                    if (!PlayerUtil.isAirBelow()) {
                                        this.towerTick = 0;
                                    } else {
                                        this.towerTick = 1;
                                        mc.field_71439_g.field_70181_x -= 0.08;
                                        mc.field_71439_g.field_70181_x *= 0.98F;
                                        mc.field_71439_g.field_70181_x -= 0.08;
                                        mc.field_71439_g.field_70181_x *= 0.98F;
                                    }

                                    return;
                                default:
                                    this.towerTick = 0;
                                    this.towerDelay = 0;
                                    return;
                            }
                        default:
                            this.towerTick = 0;
                            this.towerDelay = 0;
                    }
                } else {
                    this.towerTick = 0;
                    this.towerDelay = 0;
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            if (this.safeStuckTicks > 0) {
                mc.field_71439_g.field_71158_b.field_78900_b = 0.0F;
                mc.field_71439_g.field_71158_b.field_78902_a = 0.0F;
                mc.field_71439_g.field_71158_b.field_78901_c = false;
                mc.field_71439_g.field_71158_b.field_78899_d = false;
                return;
            }

            if (this.moveFix.getValue() == 1
                && RotationState.isActived()
                && RotationState.getPriority() == 3.0F
                && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }

            if (mc.field_71439_g.field_70122_E && this.stage > 0 && MoveUtil.isForwardPressed()) {
                mc.field_71439_g.field_71158_b.field_78901_c = true;
            }

            if (this.eagleSneaking && !mc.field_71439_g.field_71158_b.field_78899_d) {
                mc.field_71439_g.field_71158_b.field_78899_d = true;
                mc.field_71439_g.field_71158_b.field_78900_b *= 0.3F;
                mc.field_71439_g.field_71158_b.field_78902_a *= 0.3F;
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled()) {
            if (this.safeStuckTicks > 0) {
                mc.field_71439_g.field_70159_w = 0.0;
                mc.field_71439_g.field_70181_x = 0.0;
                mc.field_71439_g.field_70179_y = 0.0;
                this.safeStuckTicks--;
            }

            float speed = this.getSpeed();
            if (speed != 1.0F) {
                if (mc.field_71439_g.field_71158_b.field_78900_b != 0.0F
                    && mc.field_71439_g.field_71158_b.field_78902_a != 0.0F) {
                    mc.field_71439_g.field_71158_b.field_78900_b = mc.field_71439_g.field_71158_b.field_78900_b
                        * (1.0F / (float)Math.sqrt(2.0));
                    mc.field_71439_g.field_71158_b.field_78902_a = mc.field_71439_g.field_71158_b.field_78902_a
                        * (1.0F / (float)Math.sqrt(2.0));
                }

                mc.field_71439_g.field_71158_b.field_78900_b *= speed;
                mc.field_71439_g.field_71158_b.field_78902_a *= speed;
            }

            if (this.shouldStopSprint()) {
                mc.field_71439_g.func_70031_b(false);
            }

            if (this.safe.getValue() && this.tower.getValue() == 3 && mc.field_71474_y.field_74314_A.func_151470_d()) {
                float moveYaw = this.getCurrentYaw();
                boolean diagonal = this.isDiagonal(moveYaw);
                if (diagonal && !mc.field_71439_g.field_70122_E) {
                    double motionY = mc.field_71439_g.field_70181_x;
                    if (this.safePrevMotionY > 0.0 && motionY <= 0.0) {
                        double motionXZ = Math.sqrt(
                            mc.field_71439_g.field_70159_w * mc.field_71439_g.field_70159_w
                                + mc.field_71439_g.field_70179_y * mc.field_71439_g.field_70179_y
                        );
                        double motionXZSpeedBps = motionXZ * 20.0;
                        if (this.safeStuckDelayTicks <= 0 && this.safeStuckTicks <= 0 && motionXZSpeedBps >= 4.67) {
                            this.safeStuckDelayTicks = this.safeStuckDelayTicksProperty.getValue();
                        }
                    }

                    this.safePrevMotionY = motionY;
                } else {
                    this.safePrevMotionY = mc.field_71439_g.field_70181_x;
                }
            } else {
                this.safePrevMotionY = mc.field_71439_g.field_70181_x;
            }
        }
    }

    @EventTarget
    public void onSafeWalk(SafeWalkEvent event) {
        if (this.isEnabled()
            && this.safeWalk.getValue()
            && mc.field_71439_g.field_70122_E
            && mc.field_71439_g.field_70181_x <= 0.0
            && PlayerUtil.canMove(mc.field_71439_g.field_70159_w, mc.field_71439_g.field_70179_y, -1.0)) {
            event.setSafeWalk(true);
        }
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled() && this.blockCounter.getValue()) {
            int count = 0;

            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
                if (stack != null && stack.field_77994_a > 0) {
                    Item item = stack.func_77973_b();
                    if (item instanceof ItemBlock) {
                        Block block = ((ItemBlock)item).func_179223_d();
                        if (!BlockUtil.isInteractable(block) && BlockUtil.isSolid(block)) {
                            count += stack.field_77994_a;
                        }
                    }
                }
            }

            HUD hud = (HUD)Miau.moduleManager.modules.get(HUD.class);
            float scale = hud.scale.getValue();
            GlStateManager.func_179094_E();
            GlStateManager.func_179152_a(scale, scale, 0.0F);
            GlStateManager.func_179097_i();
            GlStateManager.func_179147_l();
            GlStateManager.func_179112_b(770, 771);
            mc.field_71466_p
                .func_175065_a(
                    String.format("%d block%s left", count, count != 1 ? "s" : ""),
                    (new ScaledResolution(mc).func_78326_a() / 2.0F + mc.field_71466_p.field_78288_b * 1.5F) / scale,
                    new ScaledResolution(mc).func_78328_b() / 2.0F / scale
                        - mc.field_71466_p.field_78288_b / 2.0F
                        + 1.0F,
                    (count > 0 ? Color.WHITE.getRGB() : new Color(255, 85, 85).getRGB()) | -1090519040,
                    hud.shadow.getValue()
                );
            GlStateManager.func_179084_k();
            GlStateManager.func_179126_j();
            GlStateManager.func_179121_F();
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (this.isEnabled()) {
            this.lastSlot = event.setSlot(this.lastSlot);
            event.setCancelled(true);
        }
    }

    @Override
    public void onEnabled() {
        if (mc.field_71439_g != null) {
            this.lastSlot = mc.field_71439_g.field_71071_by.field_70461_c;
        } else {
            this.lastSlot = -1;
        }

        this.blockCount = -1;
        this.rotationTick = 3;
        this.yaw = -180.0F;
        this.pitch = 0.0F;
        this.canRotate = false;
        this.towerTick = 0;
        this.towerDelay = 0;
        this.towering = false;
        this.safeStuckTicks = 0;
        this.safeStuckDelayTicks = 0;
        this.safePrevMotionY = 0.0;
        this.safeStuckActive = false;
        this.eagleSneaking = false;
        this.eagleSneakTicks = 0;
        this.eagleBlocksPlaced = 0;
        this.eagleLastSneakTime = 0L;
        this.snapRotating = false;
        this.lastSnapPlaceYaw = Float.NaN;
        this.lastSnapPlacePitch = Float.NaN;
    }

    @Override
    public void onDisabled() {
        if (mc.field_71439_g != null && this.lastSlot != -1) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.lastSlot;
        }

        Miau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
        if (this.safeStuckActive && mc.field_71439_g != null) {
            mc.field_71439_g.field_70159_w = this.savedMotionX;
            mc.field_71439_g.field_70181_x = this.savedMotionY;
            mc.field_71439_g.field_70179_y = this.savedMotionZ;
        }

        this.safeStuckTicks = 0;
        this.safeStuckDelayTicks = 0;
        this.safePrevMotionY = 0.0;
        this.safeStuckActive = false;
        this.eagleSneaking = false;
        this.eagleSneakTicks = 0;
    }

    public int getBlockCount() {
        return this.blockCount;
    }

    public static class BlockData {
        private final BlockPos blockPos;
        private final EnumFacing facing;

        public BlockData(BlockPos blockPos, EnumFacing enumFacing) {
            this.blockPos = blockPos;
            this.facing = enumFacing;
        }

        public BlockPos blockPos() {
            return this.blockPos;
        }

        public EnumFacing facing() {
            return this.facing;
        }
    }
}
