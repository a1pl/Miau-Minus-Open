package miau.module.modules.combat;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.util.network.PacketUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemAxe;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;

public class ArmorBreaker extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final IntProperty switchBackDelay = new IntProperty("SwitchBackDelay", 100, 0, 1000);
    public final BooleanProperty spoof = new BooleanProperty("SpoofItem", true);
    public final IntProperty spoofTicks = new IntProperty("SpoofTicks", 10, 1, 20, this.spoof::getValue);
    public final BooleanProperty onlyOnKillAura = new BooleanProperty("OnlyOnKillAura", false);
    private boolean attackEnemy = false;
    private int axeSlot = -1;
    private int originalSlot = -1;
    private boolean shouldSwitchBack = false;
    private final TimerUtil switchTimer = new TimerUtil();

    public ArmorBreaker() {
        super("ArmorBreaker", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && event.getType() == EventType.PRE) {
            if (!this.onlyOnKillAura.getValue() || this.isKillAuraActive()) {
                if (this.shouldSwitchBack
                    && this.axeSlot != -1
                    && this.originalSlot != -1
                    && this.switchTimer.hasTimeElapsed(this.switchBackDelay.getValue().intValue())) {
                    if (this.spoof.getValue()) {
                        Miau.slotComponent.setSlot(this.originalSlot, false);
                    } else {
                        mc.field_71439_g.field_71071_by.field_70461_c = this.originalSlot;
                    }

                    this.axeSlot = -1;
                    this.originalSlot = -1;
                    this.shouldSwitchBack = false;
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
                        int foundAxeSlot = -1;

                        for (int i = 0; i < 9; i++) {
                            if (mc.field_71439_g.field_71071_by.func_70301_a(i) != null
                                && mc.field_71439_g.field_71071_by.func_70301_a(i).func_77973_b() instanceof ItemAxe) {
                                foundAxeSlot = i;
                                break;
                            }
                        }

                        if (foundAxeSlot != -1) {
                            if (foundAxeSlot != mc.field_71439_g.field_71071_by.field_70461_c) {
                                this.originalSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                                this.axeSlot = foundAxeSlot;
                                PacketUtil.sendPacket(new C09PacketHeldItemChange(this.axeSlot));
                                PacketUtil.sendPacket(event.getPacket());
                                PacketUtil.sendPacket(new C09PacketHeldItemChange(this.originalSlot));
                                event.setCancelled(true);
                                this.shouldSwitchBack = true;
                                this.switchTimer.reset();
                                if (this.spoof.getValue()) {
                                    Miau.slotComponent.setSlot(this.axeSlot, false);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisabled() {
        if (this.originalSlot != -1 && mc.field_71439_g != null) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.originalSlot;
        }

        this.axeSlot = -1;
        this.originalSlot = -1;
        this.shouldSwitchBack = false;
        this.attackEnemy = false;
    }

    private boolean isKillAuraActive() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        return killAura != null && killAura.isEnabled();
    }
}
