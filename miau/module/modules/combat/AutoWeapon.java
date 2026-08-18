package miau.module.modules.combat;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorPlayerControllerMP;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.network.PacketUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;

public class AutoWeapon extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Normal", "SwitchWeapon"});
    public final ModeProperty itemSelect = new ModeProperty(
        "Item", 0, new String[]{"Sword", "Sword&Axe&Pickaxe", "Sword&Axe&EnchantedStick", "All"}
    );
    public final BooleanProperty useCustomWeightToCalculateWeaponLevel = new BooleanProperty(
        "UseCustomWeightToCalculateWeaponLevel", false
    );
    public final IntProperty damageWeight = new IntProperty(
        "DamageWeight", 70, 1, 100, () -> this.useCustomWeightToCalculateWeaponLevel.getValue()
    );
    public final IntProperty knockbackWeight = new IntProperty(
        "KnockbackWeight", 20, 0, 100, () -> this.useCustomWeightToCalculateWeaponLevel.getValue()
    );
    public final IntProperty fireAspectWeight = new IntProperty(
        "FireAspectWeight", 20, 0, 100, () -> this.useCustomWeightToCalculateWeaponLevel.getValue()
    );
    public final IntProperty switchBackDelay = new IntProperty(
        "SwitchBackDelay", 500, 1, 2000, () -> this.mode.getValue() == 1
    );
    public final BooleanProperty spoof = new BooleanProperty("SpoofItem", false);
    public final IntProperty spoofTicks = new IntProperty("SpoofTicks", 10, 1, 20, this.spoof::getValue);
    public final BooleanProperty cancelAttackWhenNotUsingBestWeapon = new BooleanProperty(
        "CancelAttackWhenNotUsingBestWeapon", false, () -> this.mode.getValue() == 0
    );
    public final BooleanProperty onlyOnKillAura = new BooleanProperty("OnlyOnKillAura", false);
    private boolean attackEnemy = false;
    private int bestWeaponSlot = -1;
    private int originalSlot = -1;
    private final TimerUtil switchTimer = new TimerUtil();

    public AutoWeapon() {
        super("AutoWeapon", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && event.getType() == EventType.PRE) {
            if (!this.onlyOnKillAura.getValue() || this.isKillAuraActive()) {
                if (this.mode.getValue() == 1
                    && this.bestWeaponSlot != -1
                    && this.originalSlot != -1
                    && this.switchTimer.hasTimeElapsed(this.switchBackDelay.getValue().intValue())) {
                    if (this.spoof.getValue()) {
                        Miau.slotComponent.setSlot(this.originalSlot, false);
                    } else {
                        mc.field_71439_g.field_71071_by.field_70461_c = this.originalSlot;
                    }

                    this.bestWeaponSlot = -1;
                    this.originalSlot = -1;
                }
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null) {
            if (!this.onlyOnKillAura.getValue() || this.isKillAuraActive()) {
                this.attackEnemy = true;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && event.getType() == EventType.SEND) {
            if (!this.onlyOnKillAura.getValue() || this.isKillAuraActive()) {
                if (event.getPacket() instanceof C02PacketUseEntity) {
                    C02PacketUseEntity packet = (C02PacketUseEntity)event.getPacket();
                    if (packet.func_149565_c() == Action.ATTACK && this.attackEnemy) {
                        this.attackEnemy = false;
                        int bestSlot = -1;
                        double bestScore = -1.0;

                        for (int i = 0; i < 9; i++) {
                            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
                            if (stack != null && this.isWeapon(stack)) {
                                double score = this.getLevelScore(stack);
                                if (score > bestScore) {
                                    bestScore = score;
                                    bestSlot = i;
                                }
                            }
                        }

                        if (bestSlot != -1) {
                            boolean isHoldingBest = bestSlot == mc.field_71439_g.field_71071_by.field_70461_c;
                            if (this.cancelAttackWhenNotUsingBestWeapon.getValue() && !isHoldingBest) {
                                event.setCancelled(true);
                            }

                            if (!isHoldingBest) {
                                if (this.mode.getValue() == 0) {
                                    this.selectSlot(bestSlot);
                                } else {
                                    this.bestWeaponSlot = bestSlot;
                                    this.originalSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                                    this.switchTimer.reset();
                                    this.selectSlot(bestSlot);
                                    int secondBest = this.originalSlot;
                                    double secondScore = -1.0;

                                    for (int i = 0; i < 9; i++) {
                                        if (i != bestSlot) {
                                            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
                                            if (stack != null && this.isWeapon(stack)) {
                                                double score = this.getLevelScore(stack);
                                                if (score > secondScore) {
                                                    secondScore = score;
                                                    secondBest = i;
                                                }
                                            }
                                        }
                                    }

                                    this.selectSlot(secondBest);
                                }

                                PacketUtil.sendPacket(event.getPacket());
                                event.setCancelled(true);
                            }
                        }
                    }
                }
            }
        }
    }

    private void selectSlot(int slot) {
        if (this.spoof.getValue()) {
            Miau.slotComponent.setSlot(slot, false);
        } else {
            mc.field_71439_g.field_71071_by.field_70461_c = slot;
            ((IAccessorPlayerControllerMP)mc.field_71442_b).callSyncCurrentPlayItem();
        }
    }

    private boolean isWeapon(ItemStack stack) {
        Item item = stack.func_77973_b();
        switch (this.itemSelect.getValue()) {
            case 0:
                return item instanceof ItemSword;
            case 1:
                return item instanceof ItemSword || item instanceof ItemTool;
            case 2:
                return item instanceof ItemSword
                    || item instanceof ItemTool
                    || EnchantmentHelper.func_77506_a(Enchantment.field_180313_o.field_77352_x, stack) >= 1
                        && (item == Items.field_151055_y || item == Items.field_151072_bj);
            default:
                return item != null;
        }
    }

    private double getLevelScore(ItemStack stack) {
        Item item = stack.func_77973_b();
        double attackDamage = 1.0;
        if (item instanceof ItemSword) {
            attackDamage = ((ItemSword)item).func_150931_i();
        } else if (item instanceof ItemTool) {
            attackDamage = ((ItemTool)item).func_150913_i().func_78000_c();
        }

        return this.useCustomWeightToCalculateWeaponLevel.getValue()
            ? attackDamage * this.damageWeight.getValue().intValue()
                + EnchantmentHelper.func_77506_a(Enchantment.field_180313_o.field_77352_x, stack)
                    * this.knockbackWeight.getValue()
                + EnchantmentHelper.func_77506_a(Enchantment.field_77334_n.field_77352_x, stack)
                    * this.fireAspectWeight.getValue()
            : attackDamage;
    }

    @Override
    public void onDisabled() {
        this.bestWeaponSlot = -1;
        this.originalSlot = -1;
        this.attackEnemy = false;
    }

    private boolean isKillAuraActive() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        return killAura != null && killAura.isEnabled();
    }
}
