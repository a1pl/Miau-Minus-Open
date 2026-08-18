package miau.module.modules.player;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import miau.Miau;
import miau.enums.BlinkModules;
import miau.event.EventTarget;
import miau.event.impl.HitBlockEvent;
import miau.event.impl.LeftClickMouseEvent;
import miau.event.impl.LivingUpdateEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.RightClickMouseEvent;
import miau.event.impl.SafeWalkEvent;
import miau.event.impl.StrafeEvent;
import miau.event.impl.SwapItemEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.management.RotationState;
import miau.module.Module;
import miau.module.modules.movement.LongJump;
import miau.module.modules.player.scaffold.ScaffoldComponent;
import miau.module.modules.player.scaffold.ScaffoldPlacementUtil;
import miau.module.modules.player.scaffold.ScaffoldUtils;
import miau.module.modules.player.scaffold.features.BetaFeature;
import miau.module.modules.player.scaffold.features.BlockRenderFeature;
import miau.module.modules.player.scaffold.features.KeepYFeature;
import miau.module.modules.player.scaffold.features.MultiPlaceFeature;
import miau.module.modules.player.scaffold.features.SafeWalkFeature;
import miau.module.modules.player.scaffold.features.SneakFeature;
import miau.module.modules.player.scaffold.features.SwingFeature;
import miau.module.modules.player.scaffold.features.TowerFeature;
import miau.module.modules.player.scaffold.rotations.RotationHandler;
import miau.module.modules.render.HUD;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.PercentProperty;
import miau.util.client.KeyBindUtil;
import miau.util.font.FontRepository;
import miau.util.math.RandomUtil;
import miau.util.player.ItemUtil;
import miau.util.player.MoveUtil;
import miau.util.player.PlayerUtil;
import miau.util.player.RotationUtil;
import miau.util.shader.RoundedUtils;
import miau.util.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldSettings.GameType;

public class Scaffold extends Module {
    public static final Minecraft mc = Minecraft.func_71410_x();
    private static final int ROTATION_BETA = 4;
    public static final double[] placeOffsets = new double[]{
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
    public final RotationHandler rotationHandler = new RotationHandler(this);
    public final KeepYFeature keepYFeature = new KeepYFeature(this);
    public final TowerFeature towerFeature = new TowerFeature(this);
    public final SneakFeature sneakFeature = new SneakFeature(this);
    public final SafeWalkFeature safeWalkFeature = new SafeWalkFeature(this);
    public final BetaFeature betaFeature = new BetaFeature(this);
    public final MultiPlaceFeature multiPlaceFeature = new MultiPlaceFeature(this);
    public final SwingFeature swingFeature = new SwingFeature(this);
    public final BlockRenderFeature blockRenderFeature = new BlockRenderFeature(this);
    private final List<ScaffoldComponent> components = new ArrayList<>();
    public final Scaffold.ScaffoldOptions options = new Scaffold.ScaffoldOptions();
    public int rotationTick = 0;
    public int lastSlot = -1;
    public int blockCount = -1;
    public float animationProgress = 0.0F;
    public long lastFrame = System.currentTimeMillis();
    public float yaw = -180.0F;
    public float pitch = 0.0F;
    public boolean canRotate = false;
    public int towerTick = 0;
    public int towerDelay = 0;
    public int stage = 0;
    public boolean shouldKeepY = false;
    public boolean placedThisTick = false;
    public int blocksPlaced = 0;
    public boolean towering = false;
    public EnumFacing targetFacing = null;
    public int startY = 0;
    public float placeYaw;
    public float placePitch;
    public float bridgeYaw = Float.NaN;
    public float lastMoveFixPacketYaw = Float.NaN;
    public float lastPlacedAbsPacketYawDelta = Float.NaN;
    public int duplicatePlaceRotNudgeSign = 1;
    public final BooleanProperty vulcanDisabler = new BooleanProperty(
        "vulcan-disabler", false, () -> this.rotationHandler.rotationMode.getValue() == 5
    );

    public Scaffold() {
        super("Scaffold", false);
        this.components.add(this.sneakFeature);
        this.components.add(this.keepYFeature);
        this.components.add(this.multiPlaceFeature);
        this.components.add(this.swingFeature);
        this.components.add(this.safeWalkFeature);
        this.components.add(this.towerFeature);
        this.components.add(this.betaFeature);
        this.components.add(this.blockRenderFeature);
    }

    public int getSlot() {
        return this.lastSlot;
    }

    public int getBlockCount() {
        return this.blockCount;
    }

    public float getSpeed() {
        return this.options.speedMotion.getValue().intValue() / 100.0F;
    }

    public float getCurrentYaw() {
        return MoveUtil.adjustYaw(mc.field_71439_g.field_70177_z, MoveUtil.getForwardValue(), MoveUtil.getLeftValue());
    }

    public boolean isTowering() {
        if (mc.field_71439_g.field_70122_E && MoveUtil.isForwardPressed() && !PlayerUtil.isAirAbove()) {
            boolean keepYActive = this.keepYFeature.keepY.getValue() == 3 || this.keepYFeature.keepY.getValue() == 4;
            boolean towerActive = this.towerFeature.tower.getValue() == 3;
            return keepYActive && this.stage > 0 || towerActive && mc.field_71474_y.field_74314_A.func_151470_d();
        } else {
            return false;
        }
    }

    public boolean isRightClickHeld() {
        return mc.field_71474_y != null && mc.field_71474_y.field_74313_G.func_151470_d();
    }

    private boolean canPlace() {
        BedNuker bedNuker = (BedNuker)Miau.moduleManager.modules.get(BedNuker.class);
        if (bedNuker.isEnabled() && bedNuker.isReady()) {
            return false;
        }

        LongJump longJump = (LongJump)Miau.moduleManager.modules.get(LongJump.class);
        return !longJump.isEnabled() || !longJump.isAutoMode() || longJump.isJumping();
    }

    private boolean shouldStopSprint() {
        if (this.betaFeature.isBetaMode() && !this.betaFeature.isBetaTellyMode()) {
            return true;
        } else if (this.isTowering()) {
            return false;
        } else {
            int k = this.keepYFeature.keepY.getValue();
            boolean stageActive = k == 1 || k == 2 || k == 3 || k == 5;
            int sprint = this.options.sprintMode.getValue();
            if ((!stageActive || this.stage <= 0) && sprint == 0) {
                return true;
            } else {
                return sprint != 4 && sprint != 6 && sprint != 7
                    ? sprint == 2 && mc.field_71439_g.field_70122_E || sprint == 3 && !mc.field_71439_g.field_70122_E
                    : false;
            }
        }
    }

    private void applySprintMode() {
        if (!this.shouldStopSprint()) {
            int sprint = this.options.sprintMode.getValue();
            if (sprint >= 1 && sprint <= 7) {
                KeyBindUtil.setKeyBindState(mc.field_71474_y.field_151444_V.func_151463_i(), true);
            }
        }
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

    public Scaffold.BlockData getBlockData() {
        int sy = MathHelper.func_76128_c(mc.field_71439_g.field_70163_u);
        BlockPos targetPos = new BlockPos(
            MathHelper.func_76128_c(mc.field_71439_g.field_70165_t),
            (this.stage != 0 && !this.shouldKeepY ? Math.min(sy, this.startY) : sy) - 1,
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
                            if (facing != EnumFacing.DOWN && BlockUtil.isReplaceable(pos.func_177972_a(facing))) {
                                positions.add(pos);
                                break;
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
        BlockPos bpos = positions.get(0);
        EnumFacing facing = this.getBestFacing(bpos, targetPos);
        return facing == null ? null : new Scaffold.BlockData(bpos, facing);
    }

    private MovingObjectPosition getPlacementMop(Scaffold.BlockData blockData, float yaw, float pitch) {
        return ScaffoldPlacementUtil.verifyPlacement(blockData, yaw, pitch);
    }

    public void place(BlockPos blockPos, EnumFacing enumFacing, Vec3 vec3) {
        if (this.betaFeature.canBetaPlaceNow()) {
            ItemStack activeItem = Miau.slotComponent.getItemStack();
            if (activeItem != null
                && ItemUtil.isBlock(activeItem)
                && this.blockCount > 0
                && mc.field_71442_b
                    .func_178890_a(mc.field_71439_g, mc.field_71441_e, activeItem, blockPos, enumFacing, vec3)) {
                if (mc.field_71442_b.func_178889_l() != GameType.CREATIVE) {
                    this.blockCount--;
                }

                this.placedThisTick = true;
                this.blocksPlaced++;
                this.blockRenderFeature.markPlaced(blockPos.func_177972_a(enumFacing));
                if (this.betaFeature.isBetaMode()) {
                    this.betaFeature.betaPlaceCooldown = 1;
                    this.betaFeature.betaPlaceTicks = 0;
                }

                this.sneakFeature.placements--;

                for (ScaffoldComponent comp : this.components) {
                    comp.onBlockPlaced();
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled()) {
            for (ScaffoldComponent comp : this.components) {
                comp.onRender3D(event);
            }
        }
    }

    @EventTarget(1)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            this.placedThisTick = false;
            this.betaFeature.onUpdate(event);
            if (this.rotationTick > 0) {
                this.rotationTick--;
            }

            if (mc.field_71439_g.field_70122_E) {
                this.sneakFeature.ticksOnAir = 0;
            } else {
                this.sneakFeature.ticksOnAir++;
            }

            this.sneakFeature.calculateSneaking();
            this.towerFeature.onUpdate(event);
            this.keepYFeature.onUpdate(event);
            if (this.canPlace()) {
                ItemStack stack = Miau.slotComponent.getItemStack();
                int count = stack != null && stack.func_77973_b() instanceof ItemBlock ? stack.field_77994_a : 0;
                this.blockCount = Math.min(this.blockCount, count);
                if (this.blockCount <= 0) {
                    int slot = Miau.slotComponent.getItemIndex();
                    if (this.blockCount == 0) {
                        slot--;
                    }

                    for (int i = slot; i > slot - 9; i--) {
                        int hotbarSlot = (i % 9 + 9) % 9;
                        ItemStack candidate = mc.field_71439_g.field_71071_by.func_70301_a(hotbarSlot);
                        if (candidate != null && candidate.func_77973_b() instanceof ItemBlock) {
                            Miau.slotComponent.setSlot(hotbarSlot);
                            this.blockCount = candidate.field_77994_a;
                            break;
                        }
                    }
                }

                float currentYaw = this.getCurrentYaw();
                float yawDiffTo180 = RotationUtil.wrapAngleDiff(currentYaw - 180.0F, event.getYaw());
                float diagonalYaw = ScaffoldUtils.isDiagonal(currentYaw)
                    ? yawDiffTo180
                    : RotationUtil.wrapAngleDiff(
                        currentYaw - 135.0F * ((currentYaw + 180.0F) % 90.0F < 45.0F ? 1.0F : -1.0F), event.getYaw()
                    );
                int rotMode = this.rotationHandler.rotationMode.getValue();
                boolean betaMode = rotMode == 4;
                if (!this.canRotate) {
                    this.rotationHandler.handleInitialRotation(event, currentYaw, yawDiffTo180, diagonalYaw);
                }

                Scaffold.BlockData blockData = this.getBlockData();
                Vec3 hitVec = null;
                if (blockData != null) {
                    float baseYaw = RotationUtil.wrapAngleDiff(this.yaw, event.getYaw());
                    ScaffoldPlacementUtil.PlacementAim aim = ScaffoldPlacementUtil.resolveAim(
                        blockData, baseYaw, this.pitch, placeOffsets
                    );
                    if (aim != null) {
                        this.yaw = aim.yaw;
                        this.pitch = aim.pitch;
                        hitVec = aim.hitVec;
                        this.canRotate = true;
                    } else if (betaMode) {
                        this.canRotate = false;
                    }
                }

                boolean towerRotating = this.towering || this.isTowering();
                if (this.canRotate
                    && MoveUtil.isForwardPressed()
                    && Math.abs(MathHelper.func_76142_g(yawDiffTo180 - this.yaw)) < 90.0F
                    && blockData != null
                    && (rotMode == 2 || rotMode == 3)) {
                    float styleYaw = rotMode == 2 ? yawDiffTo180 : diagonalYaw;
                    float[] bridgeGcd = RotationUtil.flexRotation(
                        styleYaw, this.pitch, event.getYaw(), event.getPitch()
                    );
                    this.bridgeYaw = bridgeGcd[0];
                    this.yaw = bridgeGcd[0];
                    this.pitch = bridgeGcd[1];
                    ScaffoldPlacementUtil.PlacementAim styled = ScaffoldPlacementUtil.resolveAim(
                        blockData, bridgeGcd[0], bridgeGcd[1], placeOffsets
                    );
                    if (styled == null) {
                        styled = ScaffoldPlacementUtil.resolveAim(
                            blockData, event.getYaw(), event.getPitch(), placeOffsets
                        );
                    }

                    if (styled != null) {
                        this.yaw = styled.yaw;
                        this.pitch = styled.pitch;
                        this.placePitch = styled.pitch;
                        hitVec = styled.hitVec;
                    } else {
                        hitVec = null;
                    }
                }

                boolean willPlaceThisTick = blockData != null
                    && hitVec != null
                    && this.rotationTick <= 0
                    && this.sneakFeature.ticksOnAir
                        >= RandomUtil.nextFloat(
                            this.options.placeDelay.getValue(), this.options.placeDelay.getSecondValue()
                        );
                this.rotationHandler
                    .handleUpdateRotation(event, yawDiffTo180, diagonalYaw, towerRotating, willPlaceThisTick);
                if (betaMode && blockData != null && hitVec != null) {
                    MovingObjectPosition verifiedMop = this.getPlacementMop(blockData, this.placeYaw, this.placePitch);
                    if (verifiedMop == null) {
                        verifiedMop = this.getPlacementMop(blockData, this.yaw, this.pitch);
                    }

                    hitVec = verifiedMop != null ? verifiedMop.field_72307_f : null;
                }

                if (willPlaceThisTick) {
                    this.place(blockData.blockPos, blockData.facing, hitVec);
                    if (this.multiPlaceFeature.multiplace.getValue()) {
                        for (int i = 0; i < 3; i++) {
                            blockData = this.getBlockData();
                            if (blockData == null) {
                                break;
                            }

                            MovingObjectPosition mop = this.getPlacementMop(blockData, this.yaw, this.pitch);
                            if (mop != null) {
                                this.place(blockData.blockPos, blockData.facing, mop.field_72307_f);
                            } else {
                                hitVec = BlockUtil.getClickVec(blockData.blockPos, blockData.facing);
                                double dx = hitVec.field_72450_a - mc.field_71439_g.field_70165_t;
                                double dy = hitVec.field_72448_b
                                    - mc.field_71439_g.field_70163_u
                                    - mc.field_71439_g.func_70047_e();
                                double dz = hitVec.field_72449_c - mc.field_71439_g.field_70161_v;
                                float[] rots = RotationUtil.getRotationsTo(dx, dy, dz, event.getYaw(), event.getPitch());
                                if (!(Math.abs(rots[0] - this.yaw) < 120.0F)
                                    || !(Math.abs(rots[1] - this.pitch) < 60.0F)) {
                                    break;
                                }

                                mop = this.getPlacementMop(blockData, rots[0], rots[1]);
                                if (mop == null) {
                                    break;
                                }

                                this.place(blockData.blockPos, blockData.facing, mop.field_72307_f);
                            }
                        }
                    }
                }

                if (this.options.sprintMode.getValue() == 7 && this.blocksPlaced >= 3) {
                    this.yaw = mc.field_71439_g.field_70177_z;
                    this.placeYaw = mc.field_71439_g.field_70177_z;
                    event.setRotation(mc.field_71439_g.field_70177_z, event.getPitch(), 3);
                    this.blocksPlaced = 0;
                }

                if (this.options.sprintMode.getValue() == 7
                    && Math.abs(
                            MathHelper.func_76142_g(mc.field_71439_g.field_70177_z) - MathHelper.func_76142_g(this.yaw)
                        )
                        > 90.0F) {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_151444_V.func_151463_i(), false);
                    mc.field_71439_g.func_70031_b(false);
                }

                if (this.targetFacing != null) {
                    if (betaMode) {
                        this.targetFacing = null;
                    } else if (this.rotationTick <= 0 && !this.placedThisTick) {
                        int px = MathHelper.func_76128_c(mc.field_71439_g.field_70165_t);
                        int py = MathHelper.func_76128_c(mc.field_71439_g.field_70163_u);
                        int pz = MathHelper.func_76128_c(mc.field_71439_g.field_70161_v);
                        BlockPos belowPlayer = new BlockPos(px, py - 1, pz);
                        hitVec = BlockUtil.getHitVec(belowPlayer, this.targetFacing, this.yaw, this.pitch);
                        this.place(belowPlayer, this.targetFacing, hitVec);
                    }

                    this.targetFacing = null;
                } else if ((
                        this.keepYFeature.keepY.getValue() == 2
                            || this.keepYFeature.keepY.getValue() == 3
                            || this.keepYFeature.keepY.getValue() == 4
                    )
                    && this.stage > 0
                    && !mc.field_71439_g.field_70122_E) {
                    int nextBlockY = MathHelper.func_76128_c(
                        mc.field_71439_g.field_70163_u + mc.field_71439_g.field_70181_x
                    );
                    if (nextBlockY <= this.startY && mc.field_71439_g.field_70163_u > this.startY + 1) {
                        this.shouldKeepY = true;
                        if (this.keepYFeature.keepY.getValue() != 4) {
                            blockData = this.getBlockData();
                            if (blockData != null && this.rotationTick <= 0 && !this.placedThisTick) {
                                MovingObjectPosition mop = this.getPlacementMop(blockData, this.yaw, this.pitch);
                                if (mop != null) {
                                    this.place(blockData.blockPos, blockData.facing, mop.field_72307_f);
                                }
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
            if (this.betaFeature.isBetaMode() && !this.betaFeature.isBetaTellyMode()) {
                this.towerTick = 0;
                this.towerDelay = 0;
                if (this.keepYFeature.keepY.getValue() != 3 && this.keepYFeature.keepY.getValue() != 4) {
                    return;
                }
            }

            this.towerFeature.onStrafe(event);
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            this.betaFeature.onMoveInput(event);
            boolean applyMoveFix = this.options.movementCorrection.getValue();
            if (applyMoveFix
                && RotationState.isActived()
                && RotationState.getPriority() == 3.0F
                && MoveUtil.isForwardPressed()) {
                float strafeRef = Float.isNaN(this.lastMoveFixPacketYaw)
                    ? RotationState.getSmoothedYaw()
                    : this.lastMoveFixPacketYaw;
                MoveUtil.fixStrafe(strafeRef);
            }

            if (mc.field_71439_g.field_70122_E && this.stage > 0 && MoveUtil.isForwardPressed()) {
                mc.field_71439_g.field_71158_b.field_78901_c = true;
            }

            if (this.sneakFeature.slow-- > 0) {
                mc.field_71439_g.field_71158_b.field_78900_b = 0.0F;
                mc.field_71439_g.field_71158_b.field_78902_a = 0.0F;
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled()) {
            this.betaFeature.onLivingUpdate(event);
            float speed = this.betaFeature.isBetaMode() && !this.betaFeature.isBetaTellyMode() ? 1.0F : this.getSpeed();
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
            } else {
                this.applySprintMode();
            }
        }
    }

    @EventTarget
    public void onSafeWalk(SafeWalkEvent event) {
        this.safeWalkFeature.onSafeWalk(event);
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (mc.field_71439_g != null) {
            long currentFrame = System.currentTimeMillis();
            float delta = (float)(currentFrame - this.lastFrame) / 1000.0F;
            this.lastFrame = currentFrame;
            boolean shouldShow = this.isEnabled() && this.options.blockCounter.getValue();
            float target = shouldShow ? 1.0F : 0.0F;
            this.animationProgress = this.animationProgress + (target - this.animationProgress) * 12.0F * delta;
            this.animationProgress = Math.max(0.0F, Math.min(1.0F, this.animationProgress));
            if (!(this.animationProgress <= 0.01F)) {
                ItemStack itemStack = null;
                int count = 0;
                ItemStack held = Miau.slotComponent.getItemStack();
                if (held != null && held.func_77973_b() instanceof ItemBlock) {
                    itemStack = held;
                }

                for (int i = 0; i < 9; i++) {
                    ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
                    if (stack != null && stack.field_77994_a > 0) {
                        Item item = stack.func_77973_b();
                        if (item instanceof ItemBlock) {
                            Block block = ((ItemBlock)item).func_179223_d();
                            if (!BlockUtil.isInteractable(block) && BlockUtil.isSolid(block)) {
                                count += stack.field_77994_a;
                                if (itemStack == null) {
                                    itemStack = stack;
                                }
                            }
                        }
                    }
                }

                if (itemStack != null) {
                    ScaledResolution sr = new ScaledResolution(mc);
                    String amount = String.valueOf(count);
                    String info = "Blocks: " + amount;
                    float textWidth = FontRepository.getHudFont(18).width(info);
                    float width = 24.0F + textWidth + 8.0F;
                    float height = 22.0F;
                    float x = (sr.func_78326_a() - width) / 2.0F;
                    float y = sr.func_78328_b() - 90.0F;
                    GlStateManager.func_179094_E();
                    float centerX = x + width / 2.0F;
                    float centerY = y + height / 2.0F;
                    GlStateManager.func_179109_b(centerX, centerY, 0.0F);
                    GlStateManager.func_179152_a(this.animationProgress, this.animationProgress, 1.0F);
                    GlStateManager.func_179109_b(-centerX, -centerY, 0.0F);
                    HUD hud = (HUD)Miau.moduleManager.getModule(HUD.class);
                    int bgAlpha = (int)(150.0F * this.animationProgress);
                    RoundedUtils.drawRound(x, y, width, height, 4.0F, new Color(0, 0, 0, bgAlpha));
                    GlStateManager.func_179094_E();
                    RenderHelper.func_74520_c();
                    mc.func_175599_af().func_180450_b(itemStack, (int)x + 4, (int)y + 3);
                    RenderHelper.func_74518_a();
                    GlStateManager.func_179121_F();
                    GlStateManager.func_179147_l();
                    int textAlpha = (int)(255.0F * this.animationProgress);
                    float fontY = y + height / 2.0F - FontRepository.getHudFont(18).height() / 2.0F;
                    float textX = x + 24.0F;
                    FontRepository.getHudFont(18)
                        .drawWithShadow(info, textX, fontY, new Color(200, 200, 200, textAlpha).getRGB());
                    GlStateManager.func_179121_F();
                }
            }
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
            this.lastSlot = Miau.slotComponent.getItemIndex();
            int sMode = this.options.sprintMode.getValue();
            if (mc.field_71439_g.field_70122_E
                && this.options.jumpWhenUse.getValue()
                && (sMode == 1 || sMode == 2 || sMode == 3 || sMode == 5)) {
                mc.field_71439_g.func_70664_aZ();
            }
        } else {
            this.lastSlot = -1;
        }

        this.blockCount = -1;
        this.blocksPlaced = 0;
        this.rotationTick = 3;
        this.yaw = -180.0F;
        this.pitch = 0.0F;
        this.canRotate = false;
        this.towerTick = 0;
        this.towerDelay = 0;
        this.towering = false;
        this.sneakFeature.sneakingTicks = -1;
        this.sneakFeature.placements = 0;
        this.sneakFeature.pause = 0;
        this.sneakFeature.slow = 0;
        this.sneakFeature.ticksOnAir = 0;
        this.betaFeature.betaAirTicks = 0;
        this.betaFeature.betaGroundTicks = 0;
        this.betaFeature.betaPlaceCooldown = 0;
        this.lastPlacedAbsPacketYawDelta = Float.NaN;
        this.duplicatePlaceRotNudgeSign = 1;
        this.betaFeature.lastBetaSentYaw = Float.NaN;
        this.betaFeature.lastBetaSentPitch = Float.NaN;
        this.betaFeature.lastBetaPitchQuotient = 0L;
        this.betaFeature.betaPlaceTicks = 999;

        for (ScaffoldComponent comp : this.components) {
            comp.onEnable();
        }
    }

    @Override
    public void onDisabled() {
        if (mc.field_71439_g != null && this.lastSlot != -1) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.lastSlot;
        }

        Miau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
        this.sneakFeature.sneakingTicks = -1;
        this.sneakFeature.placements = 0;
        this.sneakFeature.pause = 0;
        this.sneakFeature.slow = 0;
        this.sneakFeature.ticksOnAir = 0;
        this.betaFeature.betaAirTicks = 0;
        this.betaFeature.betaGroundTicks = 0;
        this.betaFeature.betaPlaceCooldown = 0;
        this.betaFeature.lastBetaSentYaw = Float.NaN;
        this.betaFeature.lastBetaSentPitch = Float.NaN;
        this.betaFeature.lastBetaPitchQuotient = 0L;
        this.betaFeature.betaPlaceTicks = 999;
        this.lastPlacedAbsPacketYawDelta = Float.NaN;
        this.duplicatePlaceRotNudgeSign = 1;

        for (ScaffoldComponent comp : this.components) {
            comp.onDisable();
        }

        KeyBinding.func_74510_a(mc.field_71474_y.field_74311_E.func_151463_i(), false);
    }

    @Override
    public List<Property<?>> getAdditionalProperties() {
        List<Property<?>> props = new ArrayList<>();
        props.addAll(this.rotationHandler.getProperties());

        for (Property<?> prop : this.options.getProperties()) {
            BooleanSupplier original = prop.getVisibleChecker();
            if (original != null) {
                prop.setVisibleChecker(original);
            }

            props.add(prop);
        }

        for (ScaffoldComponent comp : this.components) {
            for (Property<?> prop : comp.getProperties()) {
                BooleanSupplier original = prop.getVisibleChecker();
                if (original != null) {
                    prop.setVisibleChecker(original);
                }

                props.add(prop);
            }
        }

        return props;
    }

    public static class BlockData {
        public final BlockPos blockPos;
        public final EnumFacing facing;

        public BlockData(BlockPos blockPos, EnumFacing facing) {
            this.blockPos = blockPos;
            this.facing = facing;
        }
    }

    public class ScaffoldOptions {
        public final FloatProperty tellystartrotationminspeed = new FloatProperty(
            "telly-start-rotation-min-speed", 40.0F, 1.0F, 180.0F, this::isKeepYActive
        );
        public final FloatProperty tellystartrotationmaxspeed = new FloatProperty(
            "telly-start-rotation-max-speed", 95.0F, 1.0F, 180.0F, this::isKeepYActive
        );
        public final FloatProperty tellynormalrotationminspeed = new FloatProperty(
            "telly-normal-rotation-min-speed", 30.0F, 1.0F, 180.0F, this::isKeepYActive
        );
        public final FloatProperty tellynormalrotationmaxspeed = new FloatProperty(
            "telly-normal-rotation-max-speed", 35.0F, 1.0F, 180.0F, this::isKeepYActive
        );
        public final BooleanProperty movementCorrection = new BooleanProperty("movement-correction", true);
        public final ModeProperty sprintMode = new ModeProperty(
            "sprint",
            0,
            new String[]{
                "NONE", "VANILLA", "OFF_GROUND", "ON_GROUND", "HypixelBeta", "Hypixel", "OldIntave", "HypixelTest"
            }
        );
        public final BooleanProperty jumpWhenUse = new BooleanProperty("jump-when-use", true, () -> {
            int mode = this.sprintMode.getValue();
            return mode == 1 || mode == 2 || mode == 3 || mode == 5;
        });
        public final PercentProperty speedMotion = new PercentProperty("speed-motion", 100);
        public final BooleanProperty blockCounter = new BooleanProperty("block-counter", true);
        public final FloatProperty placeDelay = new FloatProperty("place-delay", 0.0F, 0.0F, 5.0F);

        private boolean isKeepYActive() {
            int val = Scaffold.this.keepYFeature.keepY.getValue();
            return val == 3 || val == 4 || val == 5;
        }

        public List<Property<?>> getProperties() {
            List<Property<?>> list = new ArrayList<>();
            list.add(this.tellystartrotationminspeed);
            list.add(this.tellystartrotationmaxspeed);
            list.add(this.tellynormalrotationminspeed);
            list.add(this.tellynormalrotationmaxspeed);
            list.add(this.movementCorrection);
            list.add(this.sprintMode);
            list.add(this.jumpWhenUse);
            list.add(this.speedMotion);
            list.add(this.blockCounter);
            list.add(this.placeDelay);
            return list;
        }
    }
}
