package miau.module.modules.movement;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorEntity;
import miau.mixin.IAccessorMinecraft;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.network.PacketUtil;
import miau.util.player.MoveUtil;
import miau.util.time.TimerUtil;
import miau.util.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.stats.StatList;
import net.minecraft.util.BlockPos;

public class Step extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty modeValue = new ModeProperty(
        "Mode",
        2,
        new String[]{
            "Vanilla",
            "Jump",
            "NCP",
            "MotionNCP",
            "OldNCP",
            "AAC",
            "LAAC",
            "AAC3.3.4",
            "Spartan",
            "Rewinside",
            "BlocksMCTimer",
            "Matrix"
        }
    );
    public final FloatProperty height = new FloatProperty(
        "Height", 1.0F, 0.6F, 10.0F, () -> !usesStepHeight(this.modeValue.getModeString())
    );
    public final FloatProperty jumpHeight = new FloatProperty(
        "JumpHeight", 0.42F, 0.37F, 0.42F, () -> this.modeValue.getModeString().equals("Jump")
    );
    public final IntProperty delay = new IntProperty("Delay", 0, 0, 500);
    public final BooleanProperty twoBlock = new BooleanProperty(
        "2Block", true, () -> this.modeValue.getModeString().equals("Matrix")
    );
    public final BooleanProperty instant = new BooleanProperty(
        "Instant", true, () -> this.modeValue.getModeString().equals("Matrix") && this.twoBlock.getValue()
    );
    private boolean isStep = false;
    private double stepX = 0.0;
    private double stepY = 0.0;
    private double stepZ = 0.0;
    private int ncpNextStep = 0;
    private boolean spartanSwitch = false;
    private boolean isAACStep = false;
    private int matrixTicks = 0;
    private boolean matrixDoJump = false;
    private int timerPhase = 0;
    private final TimerUtil timer = new TimerUtil();

    public Step() {
        super("Step", false);
    }

    private static boolean usesStepHeight(String mode) {
        return mode.equals("Jump")
            || mode.equals("MotionNCP")
            || mode.equals("LAAC")
            || mode.equals("AAC3.3.4")
            || mode.equals("BlocksMCTimer")
            || mode.equals("Matrix");
    }

    private boolean isInLiquid() {
        return mc.field_71439_g.func_70055_a(Material.field_151586_h)
            || mc.field_71439_g.func_70055_a(Material.field_151587_i);
    }

    private boolean isNearChest() {
        BlockPos pos = new BlockPos(mc.field_71439_g);

        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Block block = BlockUtil.getBlock(pos.func_177982_a(x, y, z));
                    if (block == Blocks.field_150486_ae
                        || block == Blocks.field_150477_bB
                        || block == Blocks.field_150447_bR) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void fakeJump() {
        mc.field_71439_g.field_70160_al = true;
        mc.field_71439_g.func_71029_a(StatList.field_75953_u);
    }

    private boolean couldStep() {
        if (mc.field_71439_g == null) {
            return false;
        }

        if (!mc.field_71439_g.func_70093_af() && !mc.field_71474_y.field_74314_A.func_151470_d()) {
            double yaw = MoveUtil.getMoveDirection();
            double heightOffset = 1.001335979112147;

            for (int i = -10; i <= 10; i++) {
                double adjustedYaw = yaw + i * Math.toRadians(8.0);
                double x = -Math.sin(adjustedYaw) * 0.2;
                double z = Math.cos(adjustedYaw) * 0.2;
                if (!mc.field_71441_e
                    .func_147461_a(mc.field_71439_g.func_174813_aQ().func_72317_d(x, heightOffset, z))
                    .isEmpty()) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private void sendStepPackets() {
        double x = mc.field_71439_g.field_70165_t;
        double y = mc.field_71439_g.field_70163_u;
        double z = mc.field_71439_g.field_70161_v;
        PacketUtil.sendPacket(new C04PacketPlayerPosition(x, y + 0.41999998688698, z, false));
        PacketUtil.sendPacket(new C04PacketPlayerPosition(x, y + 0.7531999805212, z, false));
        PacketUtil.sendPacket(new C04PacketPlayerPosition(x, y + 1.00133597911215, z, true));
        PacketUtil.sendPacket(new C04PacketPlayerPosition(x, y + 1.42133596599913, z, false));
        PacketUtil.sendPacket(new C04PacketPlayerPosition(x, y + 1.75453595963335, z, false));
        PacketUtil.sendPacket(new C04PacketPlayerPosition(x, y + 2.0026719582243, z, false));
    }

    private void stepConfirm() {
        if (mc.field_71439_g != null && this.isStep) {
            if (mc.field_71439_g.func_174813_aQ().field_72338_b - this.stepY > 0.6) {
                String mode = this.modeValue.getModeString();
                if (mode.equals("NCP") || mode.equals("AAC")) {
                    this.fakeJump();
                    PacketUtil.sendPacket(
                        new C04PacketPlayerPosition(this.stepX, this.stepY + 0.41999998688698, this.stepZ, false)
                    );
                    PacketUtil.sendPacket(
                        new C04PacketPlayerPosition(this.stepX, this.stepY + 0.7531999805212, this.stepZ, false)
                    );
                    this.timer.reset();
                } else if (mode.equals("Spartan")) {
                    this.fakeJump();
                    if (this.spartanSwitch) {
                        PacketUtil.sendPacket(
                            new C04PacketPlayerPosition(this.stepX, this.stepY + 0.41999998688698, this.stepZ, false)
                        );
                        PacketUtil.sendPacket(
                            new C04PacketPlayerPosition(this.stepX, this.stepY + 0.7531999805212, this.stepZ, false)
                        );
                        PacketUtil.sendPacket(
                            new C04PacketPlayerPosition(this.stepX, this.stepY + 1.001335979112147, this.stepZ, false)
                        );
                    } else {
                        PacketUtil.sendPacket(
                            new C04PacketPlayerPosition(this.stepX, this.stepY + 0.6, this.stepZ, false)
                        );
                    }

                    this.spartanSwitch = !this.spartanSwitch;
                    this.timer.reset();
                } else if (mode.equals("Rewinside")) {
                    this.fakeJump();
                    PacketUtil.sendPacket(
                        new C04PacketPlayerPosition(this.stepX, this.stepY + 0.41999998688698, this.stepZ, false)
                    );
                    PacketUtil.sendPacket(
                        new C04PacketPlayerPosition(this.stepX, this.stepY + 0.7531999805212, this.stepZ, false)
                    );
                    PacketUtil.sendPacket(
                        new C04PacketPlayerPosition(this.stepX, this.stepY + 1.001335979112147, this.stepZ, false)
                    );
                    this.timer.reset();
                }
            }

            this.isStep = false;
            this.stepX = 0.0;
            this.stepY = 0.0;
            this.stepZ = 0.0;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g == null || mc.field_71441_e == null) {
                return;
            }

            String mode = this.modeValue.getModeString();
            if (mc.field_71439_g.func_70617_f_()
                || this.isInLiquid()
                || ((IAccessorEntity)mc.field_71439_g).getIsInWeb()) {
                return;
            }

            if (!MoveUtil.isMoving()) {
                return;
            }

            if (mode.equals("Matrix")) {
                mc.field_71439_g.field_70138_W = this.twoBlock.getValue() ? 2.0F : 1.0F;
                if (this.matrixDoJump) {
                    if ((this.matrixTicks <= 0 || !mc.field_71439_g.field_70122_E) && this.matrixTicks <= 5) {
                        if (this.matrixTicks % 3 == 0) {
                            mc.field_71439_g.field_70122_E = true;
                            mc.field_71439_g.func_70664_aZ();
                        }

                        this.matrixTicks++;
                        return;
                    }

                    this.matrixTicks = 0;
                    this.matrixDoJump = false;
                    return;
                }

                if (this.couldStep() && mc.field_71439_g.field_70122_E && mc.field_71439_g.field_70123_F) {
                    if (this.instant.getValue() && this.twoBlock.getValue()) {
                        this.sendStepPackets();
                        ((IAccessorMinecraft)mc).getTimer().field_74278_d = 0.14285715F;
                    } else if (this.twoBlock.getValue()) {
                        this.matrixDoJump = true;
                        this.matrixTicks = 0;
                    } else {
                        ((IAccessorMinecraft)mc).getTimer().field_74278_d = 0.33333F;
                        PacketUtil.sendPacket(
                            new C04PacketPlayerPosition(
                                mc.field_71439_g.field_70165_t,
                                mc.field_71439_g.field_70163_u + 0.42F,
                                mc.field_71439_g.field_70161_v,
                                false
                            )
                        );
                        PacketUtil.sendPacket(
                            new C04PacketPlayerPosition(
                                mc.field_71439_g.field_70165_t,
                                mc.field_71439_g.field_70163_u + 0.42F,
                                mc.field_71439_g.field_70161_v,
                                true
                            )
                        );
                    }
                }

                return;
            }

            if (mode.equals("Jump")) {
                if (mc.field_71439_g.field_70123_F
                    && mc.field_71439_g.field_70122_E
                    && !mc.field_71474_y.field_74314_A.func_151470_d()) {
                    this.fakeJump();
                    mc.field_71439_g.field_70181_x = this.jumpHeight.getValue().floatValue();
                }
            } else if (mode.equals("BlocksMCTimer")) {
                if (mc.field_71439_g.field_70122_E && mc.field_71439_g.field_70123_F) {
                    if (!this.couldStep() || this.isNearChest()) {
                        ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
                        this.timerPhase = 0;
                        return;
                    }

                    this.fakeJump();
                    mc.field_71439_g.func_70664_aZ();
                    switch (this.timerPhase) {
                        case 0:
                            ((IAccessorMinecraft)mc).getTimer().field_74278_d = 5.0F;
                            this.timerPhase = 1;
                            break;
                        case 1:
                            ((IAccessorMinecraft)mc).getTimer().field_74278_d = 0.2F;
                            this.timerPhase = 2;
                            break;
                        case 2:
                            ((IAccessorMinecraft)mc).getTimer().field_74278_d = 4.0F;
                            this.timerPhase = 3;
                            break;
                        case 3:
                            MoveUtil.strafe(0.27);
                            ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
                            this.timerPhase = 0;
                    }
                }
            } else if (mode.equals("LAAC")) {
                if (mc.field_71439_g.field_70123_F) {
                    if (mc.field_71439_g.field_70122_E && this.timer.hasTimeElapsed(this.delay.getValue().intValue())) {
                        this.isStep = true;
                        this.fakeJump();
                        mc.field_71439_g.field_70181_x += 0.620000001490116;
                        double yaw = MoveUtil.getMoveDirection();
                        mc.field_71439_g.field_70159_w = mc.field_71439_g.field_70159_w - Math.sin(yaw) * 0.2;
                        mc.field_71439_g.field_70179_y = mc.field_71439_g.field_70179_y + Math.cos(yaw) * 0.2;
                        this.timer.reset();
                    }

                    mc.field_71439_g.field_70122_E = true;
                } else {
                    this.isStep = false;
                }
            } else if (mode.equals("AAC3.3.4")) {
                if (mc.field_71439_g.field_70123_F && MoveUtil.isMoving()) {
                    if (mc.field_71439_g.field_70122_E && this.couldStep()) {
                        mc.field_71439_g.field_70159_w *= 1.26;
                        mc.field_71439_g.field_70179_y *= 1.26;
                        mc.field_71439_g.func_70664_aZ();
                        this.isAACStep = true;
                    }

                    if (this.isAACStep) {
                        mc.field_71439_g.field_70181_x -= 0.015;
                        if (!mc.field_71439_g.func_71039_bw() && mc.field_71439_g.field_71158_b.field_78902_a == 0.0F) {
                            mc.field_71439_g.field_70747_aH = 0.3F;
                        }
                    }
                } else {
                    this.isAACStep = false;
                }
            }

            if (mode.equals("MotionNCP")
                && mc.field_71439_g.field_70123_F
                && !mc.field_71474_y.field_74314_A.func_151470_d()) {
                if (mc.field_71439_g.field_70122_E && this.couldStep()) {
                    this.fakeJump();
                    mc.field_71439_g.field_70181_x = 0.41999998688698;
                    this.ncpNextStep = 1;
                } else if (this.ncpNextStep == 1) {
                    mc.field_71439_g.field_70181_x = 0.33319999363422;
                    this.ncpNextStep = 2;
                } else if (this.ncpNextStep == 2) {
                    double yaw = MoveUtil.getMoveDirection();
                    mc.field_71439_g.field_70181_x = 0.24813599859094704;
                    mc.field_71439_g.field_70159_w = -Math.sin(yaw) * 0.7;
                    mc.field_71439_g.field_70179_y = Math.cos(yaw) * 0.7;
                    this.ncpNextStep = 0;
                }
            }

            if (!usesStepHeight(mode)) {
                if (Miau.moduleManager.modules.get(Fly.class).isEnabled() && mc.field_71439_g.func_70694_bm() == null) {
                    mc.field_71439_g.field_70138_W = 0.0F;
                } else if (mc.field_71439_g.field_70122_E
                    && this.timer.hasTimeElapsed(this.delay.getValue().intValue())) {
                    float heightValue = this.height.getValue();
                    mc.field_71439_g.field_70138_W = heightValue;
                    if (heightValue > 0.6F) {
                        this.isStep = true;
                        this.stepX = mc.field_71439_g.field_70165_t;
                        this.stepY = mc.field_71439_g.field_70163_u;
                        this.stepZ = mc.field_71439_g.field_70161_v;
                    }
                } else {
                    mc.field_71439_g.field_70138_W = 0.6F;
                }
            }
        }
    }

    @EventTarget
    public void onUpdatePost(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            this.stepConfirm();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()
            && event.getType() == EventType.SEND
            && event.getPacket() instanceof C03PacketPlayer
            && this.isStep
            && this.modeValue.getModeString().equals("OldNCP")) {
            mc.field_71439_g.field_70181_x += 0.07;
            this.isStep = false;
        }
    }

    @Override
    public void onDisabled() {
        if (mc.field_71439_g != null) {
            mc.field_71439_g.field_70138_W = 0.6F;
        }

        ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.modeValue.getModeString()};
    }
}
