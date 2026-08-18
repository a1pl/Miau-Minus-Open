package miau.module.modules.combat;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.util.network.PacketUtil;
import miau.util.player.RotationUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

public class AutoProjectile extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final BooleanProperty facingEnemy = new BooleanProperty("FacingEnemy", true);
    public final FloatProperty range = new FloatProperty("Range", 8.0F, 1.0F, 20.0F);
    private final IntProperty throwDelay = new IntProperty("ThrowDelay", 1250, 50, 2000);
    private final IntProperty switchBackDelay = new IntProperty("SwitchBackDelay", 500, 50, 2000);
    private final BooleanProperty onlyOnKillAura = new BooleanProperty("OnlyOnKillAura", false);
    private final TimerUtil throwTimer = new TimerUtil();
    private final TimerUtil projectilePullTimer = new TimerUtil();
    private boolean projectileInUse = false;
    private int switchBack = -1;

    public AutoProjectile() {
        super("AutoProjectile", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            if (event.getType() == EventType.PRE) {
                if (this.onlyOnKillAura.getValue()) {
                    KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
                    if (killAura == null || !killAura.isEnabled()) {
                        return;
                    }
                }

                boolean usingProjectile = mc.field_71439_g.func_71039_bw()
                        && mc.field_71439_g.func_70694_bm() != null
                        && (
                            mc.field_71439_g.func_70694_bm().func_77973_b() == Items.field_151126_ay
                                || mc.field_71439_g.func_70694_bm().func_77973_b() == Items.field_151110_aK
                        )
                    || this.projectileInUse;
                if (usingProjectile) {
                    if (this.projectilePullTimer.hasTimeElapsed(this.switchBackDelay.getValue().intValue())) {
                        if (this.switchBack != -1 && mc.field_71439_g.field_71071_by.field_70461_c != this.switchBack) {
                            mc.field_71439_g.field_71071_by.field_70461_c = this.switchBack;
                            mc.field_71442_b.func_78765_e();
                        } else {
                            mc.field_71439_g.func_71034_by();
                        }

                        this.switchBack = -1;
                        this.projectileInUse = false;
                        this.throwTimer.reset();
                    }
                } else {
                    boolean throwProjectile = false;
                    if (this.facingEnemy.getValue()) {
                        Entity facingEntity = mc.field_71476_x != null ? mc.field_71476_x.field_72308_g : null;
                        if (facingEntity == null) {
                            facingEntity = this.raycastEntity(this.range.getValue());
                        }

                        if (facingEntity != null && RotationUtil.distanceToEntity(facingEntity) <= 0.0) {
                            facingEntity = null;
                        }

                        if (facingEntity != null && this.isSelected(facingEntity)) {
                            throwProjectile = true;
                        }
                    } else {
                        throwProjectile = true;
                    }

                    if (throwProjectile && this.throwTimer.hasTimeElapsed(this.throwDelay.getValue().intValue())) {
                        if (mc.field_71439_g.func_70694_bm() == null
                            || mc.field_71439_g.func_70694_bm().func_77973_b() != Items.field_151126_ay
                                && mc.field_71439_g.func_70694_bm().func_77973_b() != Items.field_151110_aK) {
                            int projectile = this.findProjectile();
                            if (projectile == -1) {
                                return;
                            }

                            this.switchBack = mc.field_71439_g.field_71071_by.field_70461_c;
                            mc.field_71439_g.field_71071_by.field_70461_c = projectile;
                            mc.field_71442_b.func_78765_e();
                        }

                        this.throwProjectile();
                    }
                }
            }
        }
    }

    private void throwProjectile() {
        int projectile = this.findProjectile();
        if (projectile != -1) {
            mc.field_71439_g.field_71071_by.field_70461_c = projectile;
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(projectile);
            if (stack != null) {
                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(stack));
            }

            this.projectileInUse = true;
            this.projectilePullTimer.reset();
        }
    }

    private int findProjectile() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (stack != null
                && (stack.func_77973_b() == Items.field_151126_ay || stack.func_77973_b() == Items.field_151110_aK)) {
                return i;
            }
        }

        return -1;
    }

    private Entity raycastEntity(float range) {
        Entity best = null;
        double bestAngle = Double.MAX_VALUE;

        for (Object o : mc.field_71441_e.field_72996_f) {
            if (o instanceof EntityLivingBase) {
                EntityLivingBase entity = (EntityLivingBase)o;
                if (this.isSelected(entity)
                    && !(RotationUtil.distanceToEntity(entity) > range)
                    && RotationUtil.rayTrace(entity) == null) {
                    float angle = RotationUtil.angleToEntity(entity);
                    if (angle < bestAngle) {
                        bestAngle = angle;
                        best = entity;
                    }
                }
            }
        }

        return best;
    }

    private boolean isSelected(Entity entity) {
        if (entity == null || !(entity instanceof EntityLivingBase)) {
            return false;
        } else {
            return entity == mc.field_71439_g || entity.field_70128_L ? false : !(entity instanceof EntityArmorStand);
        }
    }

    @Override
    public void onDisabled() {
        this.throwTimer.reset();
        this.projectilePullTimer.reset();
        this.projectileInUse = false;
        this.switchBack = -1;
    }
}
