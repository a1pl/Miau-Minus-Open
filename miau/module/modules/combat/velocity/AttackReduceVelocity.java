package miau.module.modules.combat.velocity;

import miau.Miau;
import miau.event.EventManager;
import miau.event.impl.AttackEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorEntity;
import miau.module.Module;
import miau.module.modules.combat.KillAura;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.util.client.ChatUtil;
import miau.util.player.MoveUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class AttackReduceVelocity extends VelocityMode {
    private boolean slot = false;
    private boolean attack = false;
    private boolean swing = false;
    private boolean block = false;
    private boolean inventory = false;
    private boolean dig = false;
    public final BooleanProperty reduce = new BooleanProperty("Reduce", true);
    public final BooleanProperty tickExactEnable = new BooleanProperty("TickExact", true);
    public final IntProperty tick500 = new IntProperty("500", 3, 0, 20);
    public final IntProperty tick1000 = new IntProperty("1000", 4, 0, 20);
    public final IntProperty tick2000 = new IntProperty("2000", 4, 0, 20);
    public final IntProperty tick3000 = new IntProperty("3000", 5, 0, 20);
    public final IntProperty tick4000 = new IntProperty("4000", 6, 0, 20);
    public final IntProperty tick5000 = new IntProperty("5000", 6, 0, 20);
    public final IntProperty tick6000 = new IntProperty("6000", 7, 0, 20);
    public final IntProperty tick7000 = new IntProperty("7000", 7, 0, 20);
    public final IntProperty tick8000 = new IntProperty("8000", 8, 0, 20);
    public final IntProperty tick9000 = new IntProperty("9000", 8, 0, 20);
    public final IntProperty tick10000 = new IntProperty("10000", 9, 0, 20);
    private int reduceTicks = 0;
    private int anInt = 0;

    public AttackReduceVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        this.reduceTicks = 0;
        this.anInt = 0;
    }

    @Override
    public void onDisable() {
        this.reduceTicks = 0;
        this.anInt = 0;
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (mc.field_71439_g != null) {
            if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
                Packet<?> packet = event.getPacket();
                if (packet instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity velocity = (S12PacketEntityVelocity)packet;
                    if (velocity.func_149412_c() == mc.field_71439_g.func_145782_y()) {
                        this.reduceTicks = this.ReduceTicks(velocity.func_149411_d(), velocity.func_149409_f());
                    }
                }
            }

            if (event.getType() == EventType.SEND && !event.isCancelled()) {
                Packet<?> packet = event.getPacket();
                if (packet instanceof C09PacketHeldItemChange) {
                    this.slot = true;
                } else if (packet instanceof C0APacketAnimation) {
                    this.swing = true;
                } else if (packet instanceof C02PacketUseEntity) {
                    C02PacketUseEntity useEntity = (C02PacketUseEntity)packet;
                    if (useEntity.func_149565_c() == Action.ATTACK) {
                        this.attack = true;
                    }
                } else if (packet instanceof C08PacketPlayerBlockPlacement) {
                    this.block = true;
                } else if (packet instanceof C07PacketPlayerDigging) {
                    this.block = true;
                    this.dig = true;
                } else if (!(packet instanceof C0DPacketCloseWindow)
                    && !(packet instanceof C0EPacketClickWindow)
                    && (
                        !(packet instanceof C16PacketClientStatus)
                            || ((C16PacketClientStatus)packet).func_149435_c() != EnumState.OPEN_INVENTORY_ACHIEVEMENT
                    )) {
                    if (packet instanceof C03PacketPlayer) {
                        this.resetBadPackets();
                    }
                } else {
                    this.inventory = true;
                }
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (this.reduceTicks > 0 && this.reduce.getValue()) {
            this.reduceTicks--;
            Module killAura = Miau.moduleManager.modules.get(KillAura.class);
            if (killAura instanceof KillAura && killAura.isEnabled()) {
                EntityLivingBase target = ((KillAura)killAura).getTarget();
                if (target != null) {
                    if (!((IAccessorEntity)mc.field_71439_g).getIsInWeb()) {
                        if (mc.field_71439_g.func_70051_ag()) {
                            if (MoveUtil.isMoving()) {
                                if (target != mc.field_71439_g) {
                                    if (!this.badPackets()) {
                                        if (mc.func_147114_u() != null) {
                                            EventManager.call(new AttackEvent(target));
                                            mc.func_147114_u().func_147297_a(new C0APacketAnimation());
                                            mc.func_147114_u()
                                                .func_147297_a(new C02PacketUseEntity(target, Action.ATTACK));
                                        }

                                        mc.field_71439_g.field_70159_w *= 0.6;
                                        mc.field_71439_g.field_70179_y *= 0.6;
                                        mc.field_71439_g.func_70031_b(false);
                                        this.anInt++;
                                        ChatUtil.sendRaw("Reduce" + this.anInt);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int ReduceTicks(int motionX, int motionZ) {
        double kb = Math.hypot(motionX, motionZ);
        if (!this.tickExactEnable.getValue()) {
            double ticks = 6.43153527E-4 * kb + 2.9419087136;
            int result = (int)Math.round(ticks);
            if (result < 1) {
                result = 1;
            }

            if (result > 10) {
                result = 10;
            }

            return result;
        } else if (kb <= 500.0) {
            return this.tick500.getValue();
        } else if (kb <= 1000.0) {
            return this.tick1000.getValue();
        } else if (kb <= 2000.0) {
            return this.tick2000.getValue();
        } else if (kb <= 3000.0) {
            return this.tick3000.getValue();
        } else if (kb <= 4000.0) {
            return this.tick4000.getValue();
        } else if (kb <= 5000.0) {
            return this.tick5000.getValue();
        } else if (kb <= 6000.0) {
            return this.tick6000.getValue();
        } else if (kb <= 7000.0) {
            return this.tick7000.getValue();
        } else if (kb <= 8000.0) {
            return this.tick8000.getValue();
        } else {
            return kb <= 9000.0 ? this.tick9000.getValue() : this.tick10000.getValue();
        }
    }

    private boolean badPackets() {
        return this.badPackets(false, false, false, false, false, false);
    }

    private boolean badPackets(boolean p1, boolean p2, boolean p3, boolean p4, boolean p5, boolean p6) {
        if (this.slot && !p1) {
            return true;
        } else if (this.attack && !p2) {
            return true;
        } else if (this.swing && !p3) {
            return true;
        } else if (this.block && !p4) {
            return true;
        } else {
            return this.inventory && !p5 ? true : this.dig && !p6;
        }
    }

    private void resetBadPackets() {
        this.slot = false;
        this.swing = false;
        this.attack = false;
        this.block = false;
        this.inventory = false;
        this.dig = false;
    }
}
