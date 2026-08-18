package miau.module.modules.combat.velocity;

import miau.Miau;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorEntity;
import miau.module.modules.combat.KillAura;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.util.network.PacketUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class OldGrimVelocity extends VelocityMode {
    public final BooleanProperty oldGrimRayCast = new BooleanProperty("ray-cast", true);
    public final BooleanProperty oldGrimLegit = new BooleanProperty("legit", false);
    public final BooleanProperty webValue = new BooleanProperty("cancel-in-web", false);
    public final BooleanProperty liquidValue = new BooleanProperty("cancel-in-liquid", false);
    public final FloatProperty oldGrimAttackReduce = new FloatProperty("attack-reduce", 0.5F, 0.0F, 1.0F);
    private boolean oldGrimVelocity = false;
    private boolean oldGrimAttacked = false;

    public OldGrimVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (event.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
                    if (packet.func_149412_c() != player.func_145782_y()) {
                        return;
                    }

                    boolean inWeb = ((IAccessorEntity)player).getIsInWeb() && this.webValue.getValue();
                    boolean inLiquid = (player.func_70090_H() || player.func_180799_ab())
                        && this.liquidValue.getValue();
                    if (inWeb || inLiquid) {
                        return;
                    }

                    double horizontalStrength = Math.sqrt(
                        Math.pow(packet.func_149411_d(), 2.0) + Math.pow(packet.func_149409_f(), 2.0)
                    );
                    if (horizontalStrength <= 1000.0) {
                        return;
                    }

                    this.oldGrimVelocity = true;
                    this.oldGrimAttacked = false;
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (player.field_70737_aN == 0) {
                    this.oldGrimVelocity = false;
                    this.oldGrimAttacked = false;
                }

                if (this.oldGrimVelocity && !this.oldGrimAttacked) {
                    Entity entity = null;
                    if (this.oldGrimRayCast.getValue()) {
                        entity = VelocityUtil.getNearestEntityInRange(3.2F);
                    } else {
                        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
                        EntityLivingBase target = killAura != null ? killAura.getTarget() : null;
                        if (target != null && player.func_70032_d(target) <= 3.0) {
                            entity = target;
                        }
                    }

                    if (entity != null) {
                        boolean state = player.func_70051_ag();
                        if (!state) {
                            PacketUtil.sendPacket(new C0BPacketEntityAction(player, Action.START_SPRINTING));
                        }

                        int count = this.oldGrimLegit.getValue() ? 1 : 6;

                        for (int i = 0; i < count; i++) {
                            PacketUtil.sendPacket(new C0APacketAnimation());
                            PacketUtil.sendPacket(
                                new C02PacketUseEntity(
                                    entity, net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK
                                )
                            );
                            if (!this.oldGrimLegit.getValue()) {
                                PacketUtil.sendPacket(
                                    new C02PacketUseEntity(
                                        entity, net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK
                                    )
                                );
                            }
                        }

                        if (!state) {
                            PacketUtil.sendPacket(new C03PacketPlayer(player.field_70122_E));
                            PacketUtil.sendPacket(new C0BPacketEntityAction(player, Action.STOP_SPRINTING));
                        }

                        this.oldGrimAttacked = true;
                        player.field_70159_w = player.field_70159_w * this.oldGrimAttackReduce.getValue().floatValue();
                        player.field_70179_y = player.field_70179_y * this.oldGrimAttackReduce.getValue().floatValue();
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.oldGrimVelocity = false;
        this.oldGrimAttacked = false;
    }
}
