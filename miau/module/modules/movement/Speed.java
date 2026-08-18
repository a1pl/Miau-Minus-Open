package miau.module.modules.movement;

import java.util.ArrayList;
import java.util.List;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.JumpEvent;
import miau.event.impl.LivingUpdateEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.StrafeEvent;
import miau.mixin.IAccessorEntity;
import miau.mixin.IAccessorMinecraft;
import miau.module.Module;
import miau.module.modules.movement.speeds.BhopSpeed;
import miau.module.modules.movement.speeds.DefaultSpeed;
import miau.module.modules.movement.speeds.LegitSpeed;
import miau.module.modules.movement.speeds.LowHopSpeed;
import miau.module.modules.movement.speeds.PolarSpeed;
import miau.module.modules.movement.speeds.SpeedMode;
import miau.module.modules.movement.speeds.VulcanSpeed;
import miau.module.modules.player.Scaffold;
import miau.property.Property;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import net.minecraft.client.Minecraft;

public class Speed extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty(
        "mode", 0, new String[]{"DEFAULT", "LEGIT", "LowHop", "VULCAN", "POLAR", "BHOP"}
    );
    private final SpeedMode[] modes = new SpeedMode[]{
        new DefaultSpeed("DEFAULT", this),
        new LegitSpeed("LEGIT", this),
        new LowHopSpeed("LowHop", this),
        new VulcanSpeed("VULCAN", this),
        new PolarSpeed("POLAR", this),
        new BhopSpeed("BHOP", this)
    };
    private int lastMode = -1;

    public Speed() {
        super("Speed", false);
    }

    public boolean canBoost() {
        Scaffold scaffold = (Scaffold)Miau.moduleManager.modules.get(Scaffold.class);
        return !scaffold.isEnabled()
            && MoveUtil.isForwardPressed()
            && mc.field_71439_g.func_71024_bL().func_75116_a() > 6
            && !mc.field_71439_g.func_70093_af()
            && !mc.field_71439_g.func_70090_H()
            && !mc.field_71439_g.func_180799_ab()
            && !((IAccessorEntity)mc.field_71439_g).getIsInWeb();
    }

    @Override
    public List<Property<?>> getAdditionalProperties() {
        List<Property<?>> props = new ArrayList<>();

        for (SpeedMode m : this.modes) {
            props.addAll(m.getProperties());
        }

        return props;
    }

    @EventTarget(3)
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled()) {
            this.modes[this.mode.getValue()].onStrafe(event);
        }
    }

    @EventTarget(3)
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled()) {
            if (this.mode.getValue() != this.lastMode) {
                if (this.lastMode != -1) {
                    this.modes[this.lastMode].onDisable();
                }

                ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
                this.lastMode = this.mode.getValue();
                this.modes[this.mode.getValue()].onEnable();
            }

            this.modes[this.mode.getValue()].onLivingUpdate(event);
        }
    }

    @EventTarget(3)
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            this.modes[this.mode.getValue()].onPacket(event);
        }
    }

    @Override
    public void onDisabled() {
        for (SpeedMode m : this.modes) {
            m.onDisable();
        }

        ((IAccessorMinecraft)mc).getTimer().field_74278_d = 1.0F;
    }

    @EventTarget(3)
    public void onJump(JumpEvent event) {
        if (this.isEnabled()) {
            this.modes[this.mode.getValue()].onJump(event);
        }
    }

    @EventTarget(3)
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            this.modes[this.mode.getValue()].onMoveInput(event);
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
