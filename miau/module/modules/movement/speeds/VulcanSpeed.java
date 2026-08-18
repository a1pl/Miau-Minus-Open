package miau.module.modules.movement.speeds;

import miau.event.impl.LivingUpdateEvent;
import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorC03PacketPlayer;
import miau.module.modules.movement.Speed;
import miau.util.player.MoveUtil;
import net.minecraft.block.BlockAir;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;

public class VulcanSpeed extends SpeedMode {
    private boolean jumped;
    private int jumpTicks;
    private int jump;

    public VulcanSpeed(String name, Speed parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.jumped = false;
        this.jumpTicks = 0;
        this.jump = 0;
    }

    private double predictedMotion(double motion, int ticks) {
        if (ticks == 0) {
            return motion;
        }

        double predicted = motion;

        for (int i = 0; i < ticks; i++) {
            predicted = (predicted - 0.08) * 0.98F;
        }

        return predicted;
    }

    @Override
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.parent.canBoost()) {
            if (MoveUtil.getSpeedLevel() < 0.22) {
                MoveUtil.setSpeed(0.22, MoveUtil.getMoveYaw());
            }

            if (mc.field_71439_g.field_70122_E) {
                this.jumpTicks = 0;
                if (MoveUtil.isMoving()) {
                    mc.field_71439_g.func_70664_aZ();
                    this.jump++;
                    this.jumped = true;
                    if (mc.field_71439_g.func_70644_a(Potion.field_76424_c) && mc.field_71439_g.field_70173_aa > 11) {
                        MoveUtil.setSpeed(
                            0.06 * (1 + mc.field_71439_g.func_70660_b(Potion.field_76424_c).func_76458_c()) + 0.485,
                            MoveUtil.getMoveYaw()
                        );
                    } else if (mc.field_71439_g.field_70173_aa > 11) {
                        MoveUtil.setSpeed(0.485, MoveUtil.getMoveYaw());
                    } else {
                        MoveUtil.setSpeed(MoveUtil.getSpeedLevel(), MoveUtil.getMoveYaw());
                    }
                }

                mc.field_71439_g.field_71158_b.field_78901_c = false;
            } else if (this.jumped) {
                this.jumpTicks++;
                switch (this.jumpTicks) {
                    case 1:
                        MoveUtil.setSpeed(MoveUtil.getSpeedLevel(), MoveUtil.getMoveYaw());
                        break;
                    case 2:
                        if (this.jump % 4 != 1 && !mc.field_71439_g.field_70124_G) {
                            mc.field_71439_g.field_70181_x = this.predictedMotion(mc.field_71439_g.field_70181_x, 2);
                        }
                    case 3:
                    case 6:
                    case 7:
                    default:
                        break;
                    case 4:
                        if (this.jump % 4 == 1 || mc.field_71439_g.field_70124_G) {
                            mc.field_71439_g.field_70181_x = this.predictedMotion(mc.field_71439_g.field_70181_x, 4);
                        }
                        break;
                    case 5:
                        if (this.jump % 4 == 1) {
                            MoveUtil.setSpeed(MoveUtil.getSpeedLevel(), MoveUtil.getMoveYaw());
                        }
                        break;
                    case 8:
                        MoveUtil.setSpeed(MoveUtil.getSpeedLevel(), MoveUtil.getMoveYaw());
                        break;
                    case 9:
                        if (!(
                            mc.field_71441_e
                                .func_180495_p(
                                    new BlockPos(
                                        mc.field_71439_g.field_70165_t,
                                        mc.field_71439_g.field_70163_u + mc.field_71439_g.field_70181_x,
                                        mc.field_71439_g.field_70161_v
                                    )
                                )
                                .func_177230_c() instanceof BlockAir
                        )) {
                            MoveUtil.setSpeed(MoveUtil.getSpeedLevel(), MoveUtil.getMoveYaw());
                        }

                        MoveUtil.setSpeed(MoveUtil.getSpeedLevel(), MoveUtil.getMoveYaw());
                }
            }
        }
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND
            && event.getPacket() instanceof C03PacketPlayer
            && mc.field_71439_g.field_70181_x < 0.0) {
            ((IAccessorC03PacketPlayer)event.getPacket()).setOnGround(true);
        }
    }
}
