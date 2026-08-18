package miau.module.modules.misc;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import miau.event.EventTarget;
import miau.event.impl.JumpEvent;
import miau.event.impl.LivingUpdateEvent;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.StrafeEvent;
import miau.event.impl.TickEvent;
import miau.module.Module;
import miau.module.modules.misc.disabler.DisablerMode;
import miau.module.modules.misc.disabler.GrimAutoclickDisabler;
import miau.module.modules.misc.disabler.TransactionDisabler;
import miau.property.Property;
import miau.property.properties.ModeProperty;

public class Disabler extends Module {
    public final List<DisablerMode> modes = new ArrayList<>();
    public final TransactionDisabler transaction = new TransactionDisabler("Transaction", this);
    public final GrimAutoclickDisabler grimAutoclick = new GrimAutoclickDisabler("Grim AutoClick", this);
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Transaction", "Grim AutoClick"});

    public Disabler() {
        super("Disabler", false);
        this.modes.add(this.transaction);
        this.modes.add(this.grimAutoclick);
    }

    public DisablerMode getActiveMode() {
        String modeName = this.mode.getModeString();

        for (DisablerMode m : this.modes) {
            if (m.getName().equalsIgnoreCase(modeName)) {
                return m;
            }
        }

        return this.modes.isEmpty() ? null : this.modes.get(0);
    }

    @Override
    public void onEnabled() {
        DisablerMode active = this.getActiveMode();
        if (active != null) {
            active.onEnable();
        }
    }

    @Override
    public void onDisabled() {
        DisablerMode active = this.getActiveMode();
        if (active != null) {
            active.onDisable();
        }
    }

    @Override
    public List<Property<?>> getAdditionalProperties() {
        List<Property<?>> props = new ArrayList<>();

        for (DisablerMode m : this.modes) {
            for (Field field : m.getClass().getDeclaredFields()) {
                field.setAccessible(true);

                try {
                    Object obj = field.get(m);
                    if (obj instanceof Property) {
                        Property<?> prop = (Property<?>)obj;
                        BooleanSupplier original = prop.getVisibleChecker();
                        prop.setVisibleChecker(
                            () -> this.getActiveMode() == m && (original == null || original.getAsBoolean())
                        );
                        props.add(prop);
                    }
                } catch (Exception var11) {
                }
            }
        }

        return props;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            DisablerMode active = this.getActiveMode();
            if (active != null) {
                active.onTick(event);
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            DisablerMode active = this.getActiveMode();
            if (active != null) {
                active.onPacket(event);
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled()) {
            DisablerMode active = this.getActiveMode();
            if (active != null) {
                active.onStrafe(event);
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled()) {
            DisablerMode active = this.getActiveMode();
            if (active != null) {
                active.onLivingUpdate(event);
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            DisablerMode active = this.getActiveMode();
            if (active != null) {
                active.onMoveInput(event);
            }
        }
    }

    @EventTarget
    public void onJump(JumpEvent event) {
        if (this.isEnabled()) {
            DisablerMode active = this.getActiveMode();
            if (active != null) {
                active.onJump(event);
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled()) {
            DisablerMode active = this.getActiveMode();
            if (active != null) {
                active.onRender2D(event);
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        if (this.isEnabled()) {
            DisablerMode active = this.getActiveMode();
            if (active != null) {
                active.onLoadWorld(event);
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
