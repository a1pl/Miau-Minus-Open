package miau.module.modules.combat.velocity;

import miau.Miau;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorMinecraft;
import miau.module.modules.combat.KillAura;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.util.player.MoveUtil;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class LegitVelocity extends VelocityMode {
    private boolean hasReceivedVelocity = false;
    private float velocityYaw = 0.0F;
    private int nextHurtTime = 0;
    private int nextAttacks = 0;
    public final BooleanProperty directionReduce = new BooleanProperty("direction-reduce", false);
    public final BooleanProperty singleTickAttack = new BooleanProperty("single-tick-attack", false);
    public final IntProperty mHurtTime = new IntProperty("min-hurt-time", 9, 1, 10, this.singleTickAttack::getValue);
    public final IntProperty mmHurtTime = new IntProperty("max-hurt-time", 10, 1, 10, this.singleTickAttack::getValue);
    public final IntProperty minAttacks = new IntProperty("min-attacks", 3, 1, 20, this.singleTickAttack::getValue);
    public final IntProperty maxAttacks = new IntProperty("max-attacks", 5, 1, 20, this.singleTickAttack::getValue);

    public LegitVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity s12 = (S12PacketEntityVelocity)event.getPacket();
            if (s12.func_149412_c() == mc.field_71439_g.func_145782_y()) {
                double velX = s12.func_149411_d() / 8000.0;
                double velZ = s12.func_149409_f() / 8000.0;
                this.velocityYaw = (float)MathHelper.func_76138_g(Math.toDegrees(Math.atan2(velZ, velX)) + 90.0);
                this.hasReceivedVelocity = true;
                if (this.singleTickAttack.getValue()) {
                    this.nextHurtTime = this.mHurtTime.getValue()
                        + (int)(Math.random() * (this.mmHurtTime.getValue() - this.mHurtTime.getValue() + 1));
                    this.nextAttacks = this.minAttacks.getValue()
                        + (int)(Math.random() * (this.maxAttacks.getValue() - this.minAttacks.getValue() + 1));
                }
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.singleTickAttack.getValue()
                && this.hasReceivedVelocity
                && mc.field_71439_g.field_70737_aN == this.nextHurtTime
                && mc.field_71476_x != null
                && mc.field_71476_x.field_72313_a == MovingObjectType.ENTITY
                && !mc.field_71439_g.func_71039_bw()) {
                for (int i = 0; i < this.nextAttacks; i++) {
                    ((IAccessorMinecraft)mc).callClickMouse();
                }
            }

            this.hasReceivedVelocity = false;
        }
    }

    @Override
    public void onMoveInput(MoveInputEvent event) {
        if (this.directionReduce.getValue() && mc.field_71439_g.field_70737_aN != 0) {
            KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
            if (killAura.isEnabled() && killAura.getTarget() != null) {
                MoveUtil.fixMovement(this.velocityYaw);
            }
        }
    }
}
