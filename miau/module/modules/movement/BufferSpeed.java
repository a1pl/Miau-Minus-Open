package miau.module.modules.movement;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorEntityPlayer;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import miau.util.world.BlockUtil;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockSlime;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

public class BufferSpeed extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty speedLimit = new BooleanProperty("SpeedLimit", true);
    public final FloatProperty maxSpeed = new FloatProperty(
        "MaxSpeed", 2.0F, 1.0F, 5.0F, () -> this.speedLimit.getValue()
    );
    public final BooleanProperty buffer = new BooleanProperty("Buffer", true);
    public final BooleanProperty stairs = new BooleanProperty("Stairs", true);
    public final ModeProperty stairsMode = new ModeProperty(
        "StairsMode", 1, new String[]{"Old", "New"}, () -> this.stairs.getValue()
    );
    public final FloatProperty stairsBoost = new FloatProperty(
        "StairsBoost", 1.87F, 1.0F, 2.0F, () -> this.stairs.getValue() && this.stairsMode.getModeString().equals("Old")
    );
    public final BooleanProperty slabs = new BooleanProperty("Slabs", true);
    public final ModeProperty slabsMode = new ModeProperty(
        "SlabsMode", 1, new String[]{"Old", "New"}, () -> this.slabs.getValue()
    );
    public final FloatProperty slabsBoost = new FloatProperty(
        "SlabsBoost", 1.87F, 1.0F, 2.0F, () -> this.slabs.getValue() && this.slabsMode.getModeString().equals("Old")
    );
    public final BooleanProperty ice = new BooleanProperty("Ice", false);
    public final FloatProperty iceBoost = new FloatProperty("IceBoost", 1.342F, 1.0F, 2.0F, () -> this.ice.getValue());
    public final BooleanProperty snow = new BooleanProperty("Snow", true);
    public final FloatProperty snowBoost = new FloatProperty("SnowBoost", 1.87F, 1.0F, 2.0F, () -> this.snow.getValue());
    public final BooleanProperty snowPort = new BooleanProperty("SnowPort", true, () -> this.snow.getValue());
    public final BooleanProperty wall = new BooleanProperty("Wall", true);
    public final ModeProperty wallMode = new ModeProperty(
        "WallMode", 1, new String[]{"Old", "New"}, () -> this.wall.getValue()
    );
    public final FloatProperty wallBoost = new FloatProperty(
        "WallBoost", 1.87F, 1.0F, 2.0F, () -> this.wall.getValue() && this.wallMode.getModeString().equals("Old")
    );
    public final BooleanProperty headBlock = new BooleanProperty("HeadBlock", true);
    public final FloatProperty headBlockBoost = new FloatProperty(
        "HeadBlockBoost", 1.87F, 1.0F, 2.0F, () -> this.headBlock.getValue()
    );
    public final BooleanProperty slime = new BooleanProperty("Slime", true);
    public final BooleanProperty airStrafe = new BooleanProperty("AirStrafe", false);
    public final BooleanProperty noHurt = new BooleanProperty("NoHurt", true);
    private double speed = 0.0;
    private boolean down = false;
    private boolean forceDown = false;
    private boolean fastHop = false;
    private boolean hadFastHop = false;
    private boolean legitHop = false;

    public BufferSpeed() {
        super("BufferSpeed", false);
    }

    private void reset() {
        if (mc.field_71439_g != null) {
            this.legitHop = true;
            this.speed = 0.0;
            if (this.hadFastHop) {
                ((IAccessorEntityPlayer)mc.field_71439_g).setSpeedInAir(0.02F);
                this.hadFastHop = false;
            }
        }
    }

    private void boost(float boost) {
        mc.field_71439_g.field_70159_w *= boost;
        mc.field_71439_g.field_70179_y *= boost;
        this.speed = MoveUtil.getSpeed();
        if (this.speedLimit.getValue() && this.speed > this.maxSpeed.getValue().floatValue()) {
            this.speed = this.maxSpeed.getValue().floatValue();
        }
    }

    private boolean isNearBlock() {
        BlockPos[] blocks = new BlockPos[]{
            new BlockPos(
                mc.field_71439_g.field_70165_t,
                mc.field_71439_g.field_70163_u + 1.0,
                mc.field_71439_g.field_70161_v - 0.7
            ),
            new BlockPos(
                mc.field_71439_g.field_70165_t + 0.7,
                mc.field_71439_g.field_70163_u + 1.0,
                mc.field_71439_g.field_70161_v
            ),
            new BlockPos(
                mc.field_71439_g.field_70165_t,
                mc.field_71439_g.field_70163_u + 1.0,
                mc.field_71439_g.field_70161_v + 0.7
            ),
            new BlockPos(
                mc.field_71439_g.field_70165_t - 0.7,
                mc.field_71439_g.field_70163_u + 1.0,
                mc.field_71439_g.field_70161_v
            )
        };

        for (BlockPos blockPos : blocks) {
            IBlockState blockState = mc.field_71441_e.func_180495_p(blockPos);
            AxisAlignedBB collisionBoundingBox = blockState.func_177230_c()
                .func_180640_a(mc.field_71441_e, blockPos, blockState);
            if ((
                        collisionBoundingBox == null
                            || collisionBoundingBox.field_72336_d == collisionBoundingBox.field_72338_b + 1.0
                    )
                    && !blockState.func_177230_c().func_149751_l()
                    && blockState.func_177230_c() == Blocks.field_150355_j
                    && !(blockState.func_177230_c() instanceof BlockSlab)
                || blockState.func_177230_c() == Blocks.field_180401_cv) {
                return true;
            }
        }

        return false;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g == null || mc.field_71441_e == null) {
                return;
            }

            if (Miau.moduleManager.modules.get(Speed.class).isEnabled()
                || this.noHurt.getValue() && mc.field_71439_g.field_70737_aN > 0) {
                this.reset();
                return;
            }

            BlockPos blockPos = new BlockPos(mc.field_71439_g);
            if (this.forceDown || this.down && mc.field_71439_g.field_70181_x == 0.0) {
                mc.field_71439_g.field_70181_x = -1.0;
                this.down = false;
                this.forceDown = false;
            }

            if (this.fastHop) {
                ((IAccessorEntityPlayer)mc.field_71439_g).setSpeedInAir(0.0211F);
                this.hadFastHop = true;
            } else if (this.hadFastHop) {
                ((IAccessorEntityPlayer)mc.field_71439_g).setSpeedInAir(0.02F);
                this.hadFastHop = false;
            }

            if (!MoveUtil.isMoving()
                || mc.field_71439_g.func_70093_af()
                || mc.field_71439_g.func_70090_H()
                || mc.field_71474_y.field_74314_A.func_151470_d()) {
                this.reset();
                return;
            }

            if (mc.field_71439_g.field_70122_E) {
                this.fastHop = false;
                if (this.slime.getValue()
                    && (
                        BlockUtil.getBlock(blockPos.func_177977_b()) instanceof BlockSlime
                            || BlockUtil.getBlock(blockPos) instanceof BlockSlime
                    )) {
                    mc.field_71439_g.func_70664_aZ();
                    mc.field_71439_g.field_70159_w = mc.field_71439_g.field_70181_x * 1.132;
                    mc.field_71439_g.field_70181_x = 0.08;
                    mc.field_71439_g.field_70179_y = mc.field_71439_g.field_70181_x * 1.132;
                    this.down = true;
                    return;
                }

                if (this.slabs.getValue() && BlockUtil.getBlock(blockPos) instanceof BlockSlab) {
                    if (this.slabsMode.getModeString().equalsIgnoreCase("old")) {
                        this.boost(this.slabsBoost.getValue());
                        return;
                    }

                    this.fastHop = true;
                    if (this.legitHop) {
                        mc.field_71439_g.func_70664_aZ();
                        mc.field_71439_g.field_70122_E = false;
                        this.legitHop = false;
                        return;
                    }

                    mc.field_71439_g.field_70122_E = false;
                    MoveUtil.strafe(0.375);
                    mc.field_71439_g.func_70664_aZ();
                    mc.field_71439_g.field_70181_x = 0.41;
                    return;
                }

                if (this.stairs.getValue()
                    && (
                        BlockUtil.getBlock(blockPos.func_177977_b()) instanceof BlockStairs
                            || BlockUtil.getBlock(blockPos) instanceof BlockStairs
                    )) {
                    if (this.stairsMode.getModeString().equalsIgnoreCase("old")) {
                        this.boost(this.stairsBoost.getValue());
                        return;
                    }

                    this.fastHop = true;
                    if (this.legitHop) {
                        mc.field_71439_g.func_70664_aZ();
                        mc.field_71439_g.field_70122_E = false;
                        this.legitHop = false;
                        return;
                    }

                    mc.field_71439_g.field_70122_E = false;
                    MoveUtil.strafe(0.375);
                    mc.field_71439_g.func_70664_aZ();
                    mc.field_71439_g.field_70181_x = 0.41;
                    return;
                }

                this.legitHop = true;
                if (this.headBlock.getValue() && BlockUtil.getBlock(blockPos.func_177981_b(2)) != Blocks.field_150350_a
                    )
                 {
                    this.boost(this.headBlockBoost.getValue());
                    return;
                }

                if (this.ice.getValue()
                    && (
                        BlockUtil.getBlock(blockPos.func_177977_b()) == Blocks.field_150432_aD
                            || BlockUtil.getBlock(blockPos.func_177977_b()) == Blocks.field_150403_cj
                    )) {
                    this.boost(this.iceBoost.getValue());
                    return;
                }

                if (this.snow.getValue()
                    && BlockUtil.getBlock(blockPos) == Blocks.field_150431_aC
                    && (
                        this.snowPort.getValue()
                            || mc.field_71439_g.field_70163_u - (int)mc.field_71439_g.field_70163_u >= 0.125
                    )) {
                    if (mc.field_71439_g.field_70163_u - (int)mc.field_71439_g.field_70163_u >= 0.125) {
                        this.boost(this.snowBoost.getValue());
                    } else {
                        mc.field_71439_g.func_70664_aZ();
                        this.forceDown = true;
                    }

                    return;
                }

                if (this.wall.getValue()) {
                    if (this.wallMode.getModeString().equalsIgnoreCase("old")) {
                        if (mc.field_71439_g.field_70124_G && this.isNearBlock()
                            || BlockUtil.getBlock(new BlockPos(mc.field_71439_g).func_177981_b(2))
                                != Blocks.field_150350_a) {
                            this.boost(this.wallBoost.getValue());
                            return;
                        }
                    } else if (this.isNearBlock() && !mc.field_71439_g.field_71158_b.field_78901_c) {
                        mc.field_71439_g.func_70664_aZ();
                        mc.field_71439_g.field_70181_x = 0.08;
                        mc.field_71439_g.field_70159_w *= 0.99;
                        mc.field_71439_g.field_70179_y *= 0.99;
                        this.down = true;
                        return;
                    }
                }

                if (this.buffer.getValue() && this.speed > 0.2) {
                    this.speed /= 1.02F;
                    MoveUtil.strafe();
                }
            } else {
                this.speed = 0.0;
                if (this.airStrafe.getValue()) {
                    MoveUtil.strafe();
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()
            && event.getType() == EventType.RECEIVE
            && event.getPacket() instanceof S08PacketPlayerPosLook) {
            this.speed = 0.0;
        }
    }

    @Override
    public void onEnabled() {
        this.reset();
    }

    @Override
    public void onDisabled() {
        this.reset();
    }
}
