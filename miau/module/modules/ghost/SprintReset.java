package miau.module.modules.ghost;

import java.util.concurrent.ThreadLocalRandom;
import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.util.player.MoveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;

public class SprintReset extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private long lastAttackTime = 0L;
    private EntityLivingBase lastTarget = null;
    private boolean serverSprintState = false;
    private boolean shouldReset = false;
    private int resetDelayTicks = 0;
    private int tickCounter = 0;
    private int attackSelfHurtTime = 0;
    public final BooleanProperty notWhenHurt = new BooleanProperty("not-when-hurt", true);
    public final IntProperty minTargetTicksFromGround = new IntProperty("min-target-ticks-ground", 0, 0, 10);

    public SprintReset() {
        super("SprintReset", false);
    }

    @Override
    public void onEnabled() {
        this.shouldReset = false;
        this.resetDelayTicks = 0;
        this.tickCounter = 0;
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (this.isEnabled()) {
            if (e.getType() == EventType.SEND && e.getPacket() instanceof C0BPacketEntityAction) {
                C0BPacketEntityAction packet = (C0BPacketEntityAction)e.getPacket();
                if (packet.func_180764_b() == Action.START_SPRINTING) {
                    this.serverSprintState = true;
                } else if (packet.func_180764_b() == Action.STOP_SPRINTING) {
                    this.serverSprintState = false;
                }
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled()) {
            if (event.getTarget() instanceof EntityLivingBase) {
                this.lastTarget = (EntityLivingBase)event.getTarget();
                this.lastAttackTime = System.currentTimeMillis();
                this.attackSelfHurtTime = mc.field_71439_g.field_70737_aN;
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.lastTarget != null) {
                int ping = 0;
                if (mc.func_147114_u() != null && mc.field_71439_g != null) {
                    NetworkPlayerInfo info = mc.func_147114_u().func_175102_a(mc.field_71439_g.func_110124_au());
                    if (info != null) {
                        ping = info.func_178853_c();
                    }
                }

                long attackWindow = 200L + ping;
                boolean attackRecently = System.currentTimeMillis() - this.lastAttackTime <= attackWindow;
                if (this.lastTarget.field_70737_aN == 10
                    && attackRecently
                    && mc.field_71439_g.func_70051_ag()
                    && this.serverSprintState) {
                    this.shouldReset = true;
                    this.resetDelayTicks = ThreadLocalRandom.current().nextInt(2, 5);
                    this.tickCounter = 0;
                }
            }

            if (this.shouldReset) {
                this.tickCounter++;
                if (this.tickCounter >= this.resetDelayTicks) {
                    this.shouldReset = false;
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            if (this.shouldReset() && mc.field_71439_g.field_71158_b != null) {
                mc.field_71439_g.field_71158_b.field_78900_b = 0.0F;
                mc.field_71439_g.field_71158_b.field_78902_a = 0.0F;
            }
        }
    }

    private boolean shouldReset() {
        if (!this.shouldReset) {
            return false;
        }

        if (this.lastTarget == null) {
            return false;
        }

        if (this.attackSelfHurtTime != 0 && mc.field_71439_g.field_70737_aN <= 2) {
            return false;
        }

        boolean inCritFall = mc.field_71439_g.field_70143_R > 0.0F
            && !mc.field_71439_g.field_70122_E
            && !mc.field_71439_g.func_70617_f_()
            && !mc.field_71439_g.func_70090_H();
        if (inCritFall) {
            return false;
        }

        if (mc.field_71439_g.field_70737_aN != 0 && this.notWhenHurt.getValue()) {
            return false;
        }

        if (mc.field_71439_g.field_70737_aN != 0) {
            double targetDistToGround = this.lastTarget.field_70163_u - MoveUtil.findGround(this.lastTarget);
            double ticksToGround = targetDistToGround / 0.5;
            if (ticksToGround < this.minTargetTicksFromGround.getValue().intValue()) {
                return false;
            }
        }

        return true;
    }
}
