package miau.module.modules.combat.velocity;

import miau.event.impl.JumpEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.modules.combat.Velocity;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import net.minecraft.entity.player.EntityPlayer;

public class AACPushVelocity extends VelocityMode {
    private boolean jump = false;
    public final FloatProperty aacPushXZReducer = new FloatProperty("xz-reducer", 2.0F, 1.0F, 3.0F);
    public final BooleanProperty aacPushYReducer = new BooleanProperty("y-reducer", true);

    public AACPushVelocity(String name, Velocity parent) {
        super(name, parent);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayer player = Velocity.mc.field_71439_g;
            if (player != null) {
                if (this.jump) {
                    if (player.field_70122_E) {
                        this.jump = false;
                    }
                } else {
                    if (player.field_70737_aN > 0 && player.field_70159_w != 0.0 && player.field_70179_y != 0.0) {
                        player.field_70122_E = true;
                    }

                    if (player.field_70172_ad > 0 && this.aacPushYReducer.getValue()) {
                        player.field_70181_x -= 0.014999993;
                    }
                }

                if (player.field_70172_ad >= 19) {
                    float reduce = this.aacPushXZReducer.getValue();
                    player.field_70159_w /= reduce;
                    player.field_70179_y /= reduce;
                }
            }
        }
    }

    @Override
    public void onJump(JumpEvent event) {
        EntityPlayer player = Velocity.mc.field_71439_g;
        if (player != null) {
            this.jump = true;
        }
    }
}
