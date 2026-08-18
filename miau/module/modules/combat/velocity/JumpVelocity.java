package miau.module.modules.combat.velocity;

import miau.event.impl.PacketEvent;
import miau.event.impl.StrafeEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorEntityLivingBase;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;

public class JumpVelocity extends VelocityMode {
    public final IntProperty chance = new IntProperty("chance", 100, 0, 100);
    public final ModeProperty jumpCooldownMode = new ModeProperty(
        "jump-cooldown-mode", 0, new String[]{"Ticks", "ReceivedHits"}
    );
    public final IntProperty ticksUntilJump = new IntProperty(
        "ticks-until-jump", 4, 0, 20, () -> this.jumpCooldownMode.getValue() == 0
    );
    public final IntProperty hitsUntilJump = new IntProperty(
        "received-hits-until-jump", 2, 0, 5, () -> this.jumpCooldownMode.getValue() == 1
    );
    public final BooleanProperty jumpResetOnlyOnSwing = new BooleanProperty("jump-reset-only-on-swing", false);
    private int limitUntilJump = 0;
    private boolean hasReceivedVelocity = false;
    private boolean shouldJump = false;

    public JumpVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.limitUntilJump = 0;
        this.hasReceivedVelocity = false;
        this.shouldJump = false;
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                double packetDirection = 0.0;
                boolean velocity = false;
                if (event.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                    if (packet.func_149412_c() != player.func_145782_y()) {
                        return;
                    }

                    packetDirection = Math.atan2(packet.func_149411_d(), packet.func_149409_f());
                    velocity = true;
                } else if (event.getPacket() instanceof S27PacketExplosion) {
                    double motionX = player.field_70159_w + ((S27PacketExplosion)event.getPacket()).func_149149_c();
                    double motionZ = player.field_70179_y + ((S27PacketExplosion)event.getPacket()).func_149147_e();
                    packetDirection = Math.atan2(motionX, motionZ);
                    velocity = true;
                }

                if (velocity) {
                    double degreePlayer = VelocityUtil.getDirection();
                    double degreePacket = Math.floorMod((int)Math.toDegrees(packetDirection), 360);
                    double angle = Math.abs(degreePacket + degreePlayer);
                    angle = Math.floorMod((int)angle, 360);
                    double threshold = 120.0;
                    boolean inRange = angle >= 180.0 - threshold / 2.0 && angle <= 180.0 + threshold / 2.0;
                    if (inRange) {
                        this.hasReceivedVelocity = true;
                    }
                }
            }
        }
    }

    @Override
    public void onStrafe(StrafeEvent event) {
        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player != null) {
            if (this.jumpCooldownMode.getValue() == 0) {
                this.limitUntilJump++;
            } else if (player.field_70737_aN == 9) {
                this.limitUntilJump++;
            }

            if (this.hasReceivedVelocity) {
                boolean ready = this.jumpCooldownMode.getValue() == 0
                    ? this.limitUntilJump >= this.ticksUntilJump.getValue()
                    : this.limitUntilJump >= this.hitsUntilJump.getValue();
                boolean swinging = !this.jumpResetOnlyOnSwing.getValue() || player.field_82175_bq;
                if (!((IAccessorEntityLivingBase)player).getIsJumping()
                    && VelocityUtil.randomInt(0, 100) <= this.chance.getValue()
                    && ready
                    && swinging
                    && player.field_70122_E
                    && player.field_70737_aN == 9
                    && player.func_70051_ag()) {
                    VelocityUtil.tryJump();
                    this.limitUntilJump = 0;
                }

                this.hasReceivedVelocity = false;
            }
        }
    }

    @Override
    public void onDisable() {
        this.limitUntilJump = 0;
        this.hasReceivedVelocity = false;
        this.shouldJump = false;
    }
}
