package miau.module.modules.combat;

import java.util.ArrayList;
import java.util.List;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.mixin.IAccessorPlayerControllerMP;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.util.client.ChatUtil;
import miau.util.misc.BackTrackUtil;
import miau.util.misc.SomeUtil;
import miau.util.player.RayCastUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

public class AutoRod extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final BooleanProperty facingEnemy = new BooleanProperty("FacingEnemy", true);
    private final BooleanProperty ignoreOnEnemyLowHealth = new BooleanProperty(
        "IgnoreOnEnemyLowHealth", true, () -> this.facingEnemy.getValue()
    );
    private final BooleanProperty healthFromScoreboard = new BooleanProperty(
        "HealthFromScoreboard", false, () -> this.facingEnemy.getValue() && this.ignoreOnEnemyLowHealth.getValue()
    );
    private final BooleanProperty absorption = new BooleanProperty(
        "Absorption", false, () -> this.facingEnemy.getValue() && this.ignoreOnEnemyLowHealth.getValue()
    );
    private final FloatProperty activationDistance = new FloatProperty("ActivationDistance", 3.0F, 8.0F, 1.0F, 8.0F);
    private final IntProperty enemiesNearby = new IntProperty("EnemiesNearby", 1, 1, 5);
    private final IntProperty playerHealthThreshold = new IntProperty("PlayerHealthThreshold", 5, 1, 20);
    private final IntProperty enemyHealthThreshold = new IntProperty(
        "EnemyHealthThreshold", 5, 1, 20, () -> this.facingEnemy.getValue() && this.ignoreOnEnemyLowHealth.getValue()
    );
    private final IntProperty escapeHealthThreshold = new IntProperty("EscapeHealthThreshold", 10, 1, 20);
    private final IntProperty pushDelay = new IntProperty("PushDelay", 100, 50, 1000);
    private final IntProperty pullbackDelay = new IntProperty("PullbackDelay", 500, 50, 1000);
    private final BooleanProperty onUsingItem = new BooleanProperty("OnUsingItem", false);
    private final BooleanProperty onlyOnKillAura = new BooleanProperty("OnlyOnKillAura", false);
    private final BooleanProperty disSetInventory = new BooleanProperty("SetInventorySlotOnDisable", false);
    private final IntProperty disSetInventorySlot = new IntProperty(
        "SetInventorySlotOnDisable-Slot", 0, 0, 8, () -> this.disSetInventory.getValue()
    );
    private final BooleanProperty rangeDebugger = new BooleanProperty("RangeDebugger", false);
    private final BooleanProperty switchBackAfterUse = new BooleanProperty("SwitchBackAfterUse", true);
    private final BooleanProperty pullWhenTargetHurt = new BooleanProperty("PullWhenTargetHurt", false);
    private final IntProperty targetHurtTime = new IntProperty(
        "TargetHurtTime", 9, 0, 10, () -> this.pullWhenTargetHurt.getValue()
    );
    private EntityLivingBase target = null;
    private final TimerUtil pushTimer = new TimerUtil();
    private final TimerUtil rodPullTimer = new TimerUtil();
    private boolean rodInUse = false;
    private int switchBack = -1;

    public AutoRod() {
        super("AutoRod", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{
            String.format("%s - %s", this.activationDistance.getValue(), this.activationDistance.getSecondValue())
        };
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            ItemStack heldItem = player.func_70694_bm();
            boolean usingRod = player.func_71039_bw()
                    && heldItem != null
                    && heldItem.func_77973_b() == Items.field_151112_aM
                || this.rodInUse;
            if (this.onlyOnKillAura.getValue()) {
                KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
                if (killAura == null || !killAura.isEnabled() && killAura.getTarget() == null) {
                    return;
                }
            }

            if (usingRod
                && this.pullWhenTargetHurt.getValue()
                && this.target != null
                && this.target.field_70737_aN >= this.targetHurtTime.getValue()) {
                if (this.switchBack != -1
                    && player.field_71071_by.field_70461_c != this.switchBack
                    && this.switchBackAfterUse.getValue()) {
                    player.field_71071_by.field_70461_c = this.switchBack;
                    ((IAccessorPlayerControllerMP)mc.field_71442_b).callSyncCurrentPlayItem();
                } else {
                    player.func_71034_by();
                }

                this.switchBack = -1;
                this.rodInUse = false;
                this.pushTimer.reset();
            } else {
                if (usingRod) {
                    if (this.rodPullTimer.hasTimeElapsed(this.pullbackDelay.getValue().intValue())) {
                        if (this.switchBack != -1
                            && player.field_71071_by.field_70461_c != this.switchBack
                            && this.switchBackAfterUse.getValue()) {
                            player.field_71071_by.field_70461_c = this.switchBack;
                            ((IAccessorPlayerControllerMP)mc.field_71442_b).callSyncCurrentPlayItem();
                        } else {
                            player.func_71034_by();
                        }

                        this.switchBack = -1;
                        this.rodInUse = false;
                        this.pushTimer.reset();
                    }
                } else {
                    boolean rod = false;
                    if (this.facingEnemy.getValue()
                        && this.getHealth(player) >= this.playerHealthThreshold.getValue().intValue()) {
                        Entity facingEntity = mc.field_71476_x == null ? null : mc.field_71476_x.field_72308_g;
                        List<Entity> nearbyEnemies = this.getAllNearbyEnemies();
                        if (facingEntity == null) {
                            MovingObjectPosition mop = RayCastUtil.rayCast(
                                mc.field_71439_g.field_70177_z,
                                mc.field_71439_g.field_70125_A,
                                this.activationDistance.getSecondValue().floatValue()
                            );
                            facingEntity = mop == null ? null : mop.field_72308_g;
                        }

                        if (!this.onUsingItem.getValue()) {
                            ItemStack itemInUse = mc.field_71439_g.func_71011_bu();
                            if ((itemInUse == null || itemInUse.func_77973_b() != Items.field_151112_aM)
                                && (mc.field_71439_g.func_71039_bw() || this.killAuraBlocking())) {
                                return;
                            }
                        }

                        if (SomeUtil.isSelected(facingEntity)) {
                            double distance = facingEntity != null
                                ? BackTrackUtil.getDistanceToEntityBox(facingEntity)
                                : Double.MAX_VALUE;
                            float realDistance = (float)(Math.round(distance * 100.0) / 100.0);
                            if (this.rangeDebugger.getValue()) {
                                ChatUtil.display(
                                    String.format(
                                        "%.2f | %.2f", realDistance, this.activationDistance.getValue().doubleValue()
                                    )
                                );
                            }

                            if (distance >= this.activationDistance.getValue().floatValue()
                                && nearbyEnemies.size() <= this.enemiesNearby.getValue()) {
                                if (this.ignoreOnEnemyLowHealth.getValue()) {
                                    if (facingEntity instanceof EntityLivingBase
                                        && this.getHealth((EntityLivingBase)facingEntity)
                                            >= this.enemyHealthThreshold.getValue().intValue()) {
                                        rod = true;
                                        this.target = (EntityLivingBase)facingEntity;
                                    }
                                } else {
                                    rod = true;
                                    if (facingEntity instanceof EntityLivingBase) {
                                        this.target = (EntityLivingBase)facingEntity;
                                    }
                                }
                            }
                        }
                    } else if (this.getHealth(player) <= this.escapeHealthThreshold.getValue().intValue()) {
                        rod = true;
                    } else if (!this.facingEnemy.getValue()) {
                        rod = true;
                    }

                    if (rod && this.pushTimer.hasTimeElapsed(this.pushDelay.getValue().intValue())) {
                        ItemStack held = mc.field_71439_g.func_70694_bm();
                        if (held == null || held.func_77973_b() != Items.field_151112_aM) {
                            int rodSlot = this.findRod(36, 45);
                            if (rodSlot == -1) {
                                return;
                            }

                            this.switchBack = mc.field_71439_g.field_71071_by.field_70461_c;
                            mc.field_71439_g.field_71071_by.field_70461_c = rodSlot;
                            ((IAccessorPlayerControllerMP)mc.field_71442_b).callSyncCurrentPlayItem();
                        }

                        this.rod();
                    }
                }
            }
        }
    }

    private void rod() {
        int rod = this.findRod(36, 45);
        if (rod != -1) {
            mc.field_71439_g.field_71071_by.field_70461_c = rod;
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(rod);
            mc.field_71442_b.func_78769_a(mc.field_71439_g, mc.field_71441_e, stack);
            this.rodInUse = true;
            this.rodPullTimer.reset();
        }
    }

    private int findRod(int startSlot, int endSlot) {
        for (int i = startSlot; i < endSlot; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.field_70462_a[i - 36];
            if (stack != null && stack.func_77973_b() == Items.field_151112_aM) {
                return i - 36;
            }
        }

        return -1;
    }

    private List<Entity> getAllNearbyEnemies() {
        EntityLivingBase player = mc.field_71439_g;
        List<Entity> result = new ArrayList<>();
        if (player != null && mc.field_71441_e != null) {
            for (Object o : mc.field_71441_e.field_72996_f) {
                if (o instanceof Entity) {
                    Entity entity = (Entity)o;
                    if (SomeUtil.isSelected(entity)) {
                        double distance = BackTrackUtil.getDistanceToEntityBox(entity);
                        if (distance < this.activationDistance.getSecondValue().floatValue()
                            && distance > this.activationDistance.getValue().floatValue()) {
                            result.add(entity);
                        }
                    }
                }
            }

            return result;
        } else {
            return result;
        }
    }

    private float getHealth(EntityLivingBase entity) {
        float health = entity.func_110143_aJ();
        if (this.absorption.getValue()) {
            health += entity.func_110139_bj();
        }

        return health;
    }

    private boolean killAuraBlocking() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        return killAura != null && killAura.isBlocking();
    }

    @Override
    public void onDisabled() {
        this.target = null;
        if (this.disSetInventory.getValue() && mc.field_71439_g != null) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.disSetInventorySlot.getValue();
            ((IAccessorPlayerControllerMP)mc.field_71442_b).callSyncCurrentPlayItem();
        }
    }
}
