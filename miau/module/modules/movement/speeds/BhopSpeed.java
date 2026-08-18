package miau.module.modules.movement.speeds;

import java.util.ArrayList;
import java.util.List;
import miau.Miau;
import miau.event.impl.LivingUpdateEvent;
import miau.event.impl.PacketEvent;
import miau.event.types.EventType;
import miau.module.modules.movement.Speed;
import miau.module.modules.player.Scaffold;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class BhopSpeed extends SpeedMode {
    private int inAirTicks = 0;
    private int dmgTicks = 0;
    private boolean collided = false;
    private boolean down = false;
    private boolean dmg = false;
    public final ModeProperty bhopMode = new ModeProperty("Bhop Mode", 0, new String[]{"Ground", "8 tick"});
    public final BooleanProperty disableWhileScaffold = new BooleanProperty("Disable while scaffold", false);
    public final BooleanProperty rotateYaw = new BooleanProperty("Rotate Yaw", false);

    public BhopSpeed(String name, Speed parent) {
        super(name, parent);
    }

    @Override
    public List<Property<?>> getProperties() {
        List<Property<?>> props = new ArrayList<>();
        props.add(this.bhopMode);
        props.add(this.disableWhileScaffold);
        props.add(this.rotateYaw);
        return props;
    }

    @Override
    public void onEnable() {
        this.inAirTicks = 0;
        this.dmgTicks = 0;
        this.collided = false;
        this.down = false;
        this.dmg = false;
    }

    @Override
    public void onDisable() {
        this.inAirTicks = 0;
        this.dmgTicks = 0;
        this.collided = false;
        this.down = false;
        this.dmg = false;
    }

    private boolean scaffoldEnabled() {
        Scaffold scaffold = (Scaffold)Miau.moduleManager.modules.get(Scaffold.class);
        return scaffold != null && scaffold.isEnabled();
    }

    @Override
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            if (this.parent.canBoost() || !mc.field_71439_g.field_70122_E) {
                if (!this.disableWhileScaffold.getValue() || !this.scaffoldEnabled()) {
                    this.inAirTicks = mc.field_71439_g.field_70122_E ? 0 : this.inAirTicks + 1;
                    if (this.dmg && this.dmgTicks > 0) {
                        this.dmgTicks--;
                    }

                    if (mc.field_71439_g.field_70123_F) {
                        this.collided = true;
                    } else if (mc.field_71439_g.field_70122_E) {
                        this.collided = false;
                    }

                    if (mc.field_71439_g.field_70122_E) {
                        if (this.dmg && this.dmgTicks == 0) {
                            this.dmg = false;
                        }

                        this.down = false;
                        mc.field_71439_g.func_70664_aZ();
                        if (MoveUtil.isMoving()) {
                            if (!this.rotateYaw.getValue()) {
                                if (MoveUtil.getForwardValue() != -1) {
                                    MoveUtil.setSpeed(this.getBhopSpeed(), MoveUtil.getMoveYaw());
                                } else {
                                    MoveUtil.setSpeed(this.getBhopSpeed() - 0.3, MoveUtil.getMoveYaw());
                                }
                            } else {
                                MoveUtil.setSpeed(this.getBhopSpeed(), MoveUtil.getMoveYaw());
                            }
                        }
                    } else if (this.bhopMode.getValue() == 1 && MoveUtil.isMoving() && !this.collided && !this.dmg) {
                        int simpleY = (int)Math.round(mc.field_71439_g.field_70163_u % 1.0 * 10000.0);
                        if (simpleY == 13) {
                            mc.field_71439_g.field_70181_x -= 0.02483;
                            this.down = true;
                        }

                        if (simpleY == 2000) {
                            mc.field_71439_g.field_70181_x -= 0.1913;
                        }

                        if (this.down) {
                            mc.field_71439_g.field_70163_u -= 1.0E-5;
                        }

                        if (simpleY == 3426) {
                            MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                        }
                    }
                }
            }
        }
    }

    private double getBhopSpeed() {
        int level = MoveUtil.getSpeedLevel();
        switch (level) {
            case 1:
                return 0.51;
            case 2:
                return 0.59;
            case 3:
                return 0.69;
            case 4:
                return 0.78;
            default:
                return 0.48;
        }
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity velocity = (S12PacketEntityVelocity)event.getPacket();
                if (velocity.func_149412_c() == mc.field_71439_g.func_145782_y()) {
                    this.dmg = true;
                    this.dmgTicks = 8;
                }
            }
        }
    }
}
