package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.JumpEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.PlayerUpdateEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import miau.util.world.BlockUtil;
import net.minecraft.block.BlockPane;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.BlockPos;

public class HighJump extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty(
        "Mode", 0, new String[]{"Vanilla", "FairFight0.6.0", "Damage", "AACv3", "DAC", "Mineplex", "Matrix"}
    );
    public final FloatProperty height = new FloatProperty(
        "Height",
        2.0F,
        1.1F,
        5.0F,
        () -> this.mode.getModeString().equals("Vanilla") || this.mode.getModeString().equals("Damage")
    );
    public final FloatProperty matrixMotionY = new FloatProperty(
        "Matrix-MotionY", 0.998F, 0.42F, 2.0F, () -> this.mode.getModeString().equalsIgnoreCase("Matrix")
    );
    public final IntProperty matrixTicks = new IntProperty(
        "Matrix-Ticks", 4, 1, 20, () -> this.mode.getModeString().equalsIgnoreCase("Matrix")
    );
    public final BooleanProperty glass = new BooleanProperty("OnlyGlassPane", false);
    private boolean active = false;
    private boolean falling = false;
    private boolean moving = false;
    private int ticksSinceJump = 0;

    public HighJump() {
        super("HighJump", false);
    }

    @Override
    public void onEnabled() {
        if (this.mode.getModeString().equalsIgnoreCase("Matrix")) {
            this.ticksSinceJump = 0;
            this.falling = false;
            this.active = false;
            this.moving = MoveUtil.isMoving();
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null && mc.field_71441_e != null) {
                if (!this.glass.getValue() || BlockUtil.getBlock(new BlockPos(mc.field_71439_g)) instanceof BlockPane) {
                    String mode = this.mode.getModeString().toLowerCase();
                    if (mode.equals("damage")) {
                        if (mc.field_71439_g.field_70737_aN > 0 && mc.field_71439_g.field_70122_E) {
                            mc.field_71439_g.field_70181_x = mc.field_71439_g.field_70181_x
                                + 0.42F * this.height.getValue();
                        }
                    } else if (mode.equals("aacv3")) {
                        if (!mc.field_71439_g.field_70122_E) {
                            mc.field_71439_g.field_70181_x += 0.059;
                        }
                    } else if (mode.equals("dac")) {
                        if (!mc.field_71439_g.field_70122_E) {
                            mc.field_71439_g.field_70181_x += 0.049999;
                        }
                    } else if (mode.equals("mineplex")) {
                        if (!mc.field_71439_g.field_70122_E) {
                            MoveUtil.strafe(0.35);
                        }
                    } else if (mode.equals("matrix")) {
                        if (!this.moving) {
                            MoveUtil.strafe(0.16);
                            this.moving = true;
                        }

                        if (mc.field_71439_g.field_70124_G) {
                            this.active = true;
                        }

                        if (this.ticksSinceJump == 1) {
                            mc.field_71439_g.field_70181_x = this.matrixMotionY.getValue().floatValue();
                        }

                        if (mc.field_71439_g.field_70124_G && this.ticksSinceJump > this.matrixTicks.getValue()) {
                            this.setEnabled(false);
                        }

                        if (!mc.field_71439_g.field_70122_E && this.ticksSinceJump >= 2) {
                            mc.field_71439_g.field_70181_x += 0.0034999;
                            if (!this.falling
                                && mc.field_71439_g.field_70181_x < 0.0
                                && mc.field_71439_g.field_70181_x > -0.05) {
                                mc.field_71439_g.field_70181_x = 0.0029999;
                                this.falling = true;
                                this.setEnabled(false);
                            }
                        }

                        if (this.active) {
                            this.ticksSinceJump++;
                        }
                    }

                    if (!mc.field_71439_g.field_70122_E) {
                        if (mode.equals("mineplex")) {
                            mc.field_71439_g.field_70181_x = mc.field_71439_g.field_70181_x
                                + (mc.field_71439_g.field_70143_R == 0.0F ? 0.0499 : 0.05);
                        }

                        if (mode.equals("fairfight0.6.0")
                            && mc.field_71439_g.func_70090_H()
                            && BlockUtil.getBlock(mc.field_71439_g.func_180425_c().func_177963_a(-0.5, 1.5, -0.5))
                                == Blocks.field_150355_j
                            && mc.field_71439_g.field_70143_R >= 2.0) {
                            mc.field_71439_g.field_70181_x = 1.9;
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onJump(JumpEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            if (!this.glass.getValue() || BlockUtil.getBlock(new BlockPos(mc.field_71439_g)) instanceof BlockPane) {
                String mode = this.mode.getModeString().toLowerCase();
                if (mode.equals("vanilla")) {
                    event.setJumpoff(0.42F * this.height.getValue());
                } else if (mode.equals("mineplex")) {
                    event.setJumpoff(0.47F);
                }
            }
        }
    }

    @EventTarget
    public void onMotion(PlayerUpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            if (this.mode.getModeString().equalsIgnoreCase("Matrix") && this.ticksSinceJump == 1) {
                mc.field_71439_g.field_70122_E = false;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.RECEIVE) {
            if (mc.field_71439_g != null) {
                if (this.mode.getModeString().equalsIgnoreCase("Matrix")
                    && event.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                    if (packet.func_149412_c() == mc.field_71439_g.func_145782_y() && packet.func_149410_e() < -500) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
