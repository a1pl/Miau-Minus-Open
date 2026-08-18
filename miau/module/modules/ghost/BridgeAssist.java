package miau.module.modules.ghost;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import miau.event.EventTarget;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.module.Module;
import miau.module.modules.ghost.bridgeassist.mode.NormalMode;
import miau.module.modules.ghost.bridgeassist.mode.SilentMode;
import miau.property.Property;
import miau.property.properties.ModeProperty;
import miau.util.player.ItemUtil;
import net.minecraft.client.Minecraft;

public class BridgeAssist extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Normal", "Silent"});
    private final NormalMode normalMode = new NormalMode(this);
    private final SilentMode silentMode = new SilentMode(this);

    public BridgeAssist() {
        super("Bridge Assist", false);
    }

    @Override
    public List<Property<?>> getAdditionalProperties() {
        List<Property<?>> props = new ArrayList<>();
        props.addAll(this.normalMode.getProperties());
        props.addAll(this.silentMode.getProperties());
        return props;
    }

    @Override
    public String[] getSuffix() {
        if (this.mode.getModeString().equals("Silent")) {
            return new String[]{"Silent"};
        } else {
            return Objects.equals(this.normalMode.delayMs.getValue(), this.normalMode.delayMs.getSecondValue())
                ? new String[]{String.valueOf(this.normalMode.delayMs.getValue().intValue())}
                : new String[]{
                    String.format(
                        "%d-%d",
                        this.normalMode.delayMs.getValue().intValue(),
                        this.normalMode.delayMs.getSecondValue().intValue()
                    )
                };
        }
    }

    @Override
    public void onDisabled() {
        this.normalMode.onDisabled();
        this.silentMode.onDisabled();
    }

    public boolean isHoldingBlock() {
        return ItemUtil.isHoldingBlock();
    }

    @EventTarget(4)
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            if (this.mode.getModeString().equals("Normal")) {
                this.normalMode.onTick(event);
            }
        }
    }

    @EventTarget(4)
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled() && mc.field_71462_r == null) {
            if (this.mode.getModeString().equals("Normal")) {
                this.normalMode.onMoveInput(event);
            } else if (this.mode.getModeString().equals("Silent")) {
                this.silentMode.onMoveInput(event);
            }
        }
    }

    @EventTarget(1)
    public void onUpdate(UpdateEvent e) {
        if (this.isEnabled()) {
            if (this.mode.getModeString().equals("Silent")) {
                this.silentMode.onUpdate(e);
            }
        }
    }
}
