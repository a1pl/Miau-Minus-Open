package miau.module.modules.ghost;

import com.google.common.base.CaseFormat;
import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;

public class MoreKB extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"WTAP"});
    public final IntProperty reSprintDelay = new IntProperty("resprint-delay", 0, 0, 9);
    public final BooleanProperty notWhenHurt = new BooleanProperty("not-when-hurt", false);
    public final IntProperty minTargetTicksFromGround = new IntProperty("min-target-ticks-from-ground", 5, 0, 12);
    private final TimerUtil attackTimer = new TimerUtil();
    private EntityLivingBase target;
    private final TimerUtil reSprintTimer = new TimerUtil();
    private boolean resyncNeeded;
    private int nextSprintTime;
    private int selfHurtTimeOnAttack;
    private int targetOffGroundTicks;

    public MoreKB() {
        super("MoreKB", false);
    }

    @Override
    public void onEnabled() {
        this.target = null;
        this.resyncNeeded = false;
        this.targetOffGroundTicks = 0;
    }

    @Override
    public void onDisabled() {
        this.target = null;
        this.resyncNeeded = false;
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (this.isEnabled()) {
            if (e.getTarget() instanceof EntityLivingBase) {
                if (e.getTarget() == this.target) {
                    this.attackTimer.reset();
                } else {
                    this.target = (EntityLivingBase)e.getTarget();
                    this.attackTimer.reset();
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (this.isEnabled()) {
            if (e.getType() == EventType.PRE) {
                if (this.target == null || mc.field_71439_g.func_70032_d(this.target) > 4.5) {
                    this.target = this.getTarget(4.5);
                }

                if (this.target != null) {
                    if (this.target.field_70122_E) {
                        this.targetOffGroundTicks = 0;
                    } else {
                        this.targetOffGroundTicks++;
                    }

                    if (this.target.field_70737_aN == 10
                        && !this.attackTimer.hasTimeElapsed(200L + this.getPing())
                        && mc.field_71439_g.func_70051_ag()) {
                        this.selfHurtTimeOnAttack = mc.field_71439_g.field_70737_aN;
                        this.resyncNeeded = true;
                        this.nextSprintTime = this.reSprintDelay.getValue();
                        this.reSprintTimer.reset();
                    }
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (this.isEnabled()) {
            if (this.shouldReset()) {
                mc.field_71439_g.field_71158_b.field_78900_b = 0.0F;
                mc.field_71439_g.field_71158_b.field_78902_a = 0.0F;
            }
        }
    }

    private boolean shouldReset() {
        if (this.resyncNeeded && this.reSprintTimer.hasTimeElapsed(this.nextSprintTime * 50L)) {
            this.resyncNeeded = false;
            boolean hurtTime = this.selfHurtTimeOnAttack != 0 && mc.field_71439_g.field_70737_aN <= 2;
            boolean crit = mc.field_71439_g.field_70143_R > 0.0F
                && !mc.field_71439_g.field_70122_E
                && !mc.field_71439_g.func_70617_f_()
                && !mc.field_71439_g.func_70090_H()
                && !mc.field_71439_g.func_70644_a(Potion.field_76440_q)
                && mc.field_71439_g.field_70154_o == null;
            boolean strictHurtTime = mc.field_71439_g.field_70737_aN != 0 && this.notWhenHurt.getValue();
            boolean targetAir = mc.field_71439_g.field_70737_aN != 0
                && this.targetOffGroundTicks < this.minTargetTicksFromGround.getValue();
            return !hurtTime && !crit && !strictHurtTime && !targetAir;
        } else {
            return false;
        }
    }

    private int getPing() {
        if (!mc.func_71356_B() && mc.func_147114_u() != null && mc.field_71439_g != null) {
            NetworkPlayerInfo info = mc.func_147114_u().func_175102_a(mc.field_71439_g.func_110124_au());
            return info != null ? info.func_178853_c() : 0;
        } else {
            return 0;
        }
    }

    private EntityLivingBase getTarget(double range) {
        EntityLivingBase bestTarget = null;
        double bestDistance = range;

        for (Entity entity : mc.field_71441_e.field_72996_f) {
            if (entity instanceof EntityLivingBase && entity != mc.field_71439_g) {
                double dist = mc.field_71439_g.func_70032_d(entity);
                if (dist <= bestDistance) {
                    bestDistance = dist;
                    bestTarget = (EntityLivingBase)entity;
                }
            }
        }

        return bestTarget;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }

    @Override
    public void verifyValue(String value) {
    }
}
