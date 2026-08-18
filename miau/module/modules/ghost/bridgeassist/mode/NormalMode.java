package miau.module.modules.ghost.bridgeassist.mode;

import java.util.Arrays;
import java.util.List;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.modules.ghost.BridgeAssist;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.util.player.MoveUtil;
import miau.util.player.PlayerUtil;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.RandomUtils;
import org.lwjgl.input.Keyboard;

public class NormalMode {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final BridgeAssist parent;
    private int sneakDelay = 0;
    public final FloatProperty delayMs;
    public final BooleanProperty directionCheck;
    public final BooleanProperty jumpCheck;
    public final BooleanProperty pitchCheck;
    public final BooleanProperty blocksOnly;
    public final BooleanProperty normalSneakOnly;

    public NormalMode(BridgeAssist parent) {
        this.parent = parent;
        this.delayMs = new FloatProperty(
            "delay", 2.0F, 3.0F, 0.0F, 10.0F, () -> parent.mode.getModeString().equals("Normal")
        );
        this.directionCheck = new BooleanProperty(
            "direction-check", true, () -> parent.mode.getModeString().equals("Normal")
        );
        this.jumpCheck = new BooleanProperty("jump-check", true, () -> parent.mode.getModeString().equals("Normal"));
        this.pitchCheck = new BooleanProperty("pitch-check", true, () -> parent.mode.getModeString().equals("Normal"));
        this.blocksOnly = new BooleanProperty("blocks-only", true, () -> parent.mode.getModeString().equals("Normal"));
        this.normalSneakOnly = new BooleanProperty(
            "sneaking-only", false, () -> parent.mode.getModeString().equals("Normal")
        );
    }

    public List<Property<?>> getProperties() {
        return Arrays.asList(
            this.delayMs, this.directionCheck, this.jumpCheck, this.pitchCheck, this.blocksOnly, this.normalSneakOnly
        );
    }

    public void onDisabled() {
        this.sneakDelay = 0;
    }

    private boolean canMoveSafely() {
        double[] offset = MoveUtil.predictMovement();
        return PlayerUtil.canMove(
            mc.field_71439_g.field_70159_w + offset[0], mc.field_71439_g.field_70179_y + offset[1]
        );
    }

    private boolean shouldSneak() {
        if (this.directionCheck.getValue() && mc.field_71474_y.field_74351_w.func_151470_d()) {
            return false;
        } else if (this.jumpCheck.getValue() && mc.field_71474_y.field_74314_A.func_151470_d()) {
            return false;
        } else if (this.pitchCheck.getValue() && mc.field_71439_g.field_70125_A < 69.0F) {
            return false;
        } else {
            return this.normalSneakOnly.getValue()
                    && !Keyboard.isKeyDown(mc.field_71474_y.field_74311_E.func_151463_i())
                ? false
                : (!this.blocksOnly.getValue() || this.parent.isHoldingBlock()) && mc.field_71439_g.field_70122_E;
        }
    }

    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.sneakDelay > 0) {
                this.sneakDelay--;
            }

            if (this.sneakDelay == 0 && this.canMoveSafely()) {
                this.sneakDelay = RandomUtils.nextInt(
                    this.delayMs.getValue().intValue(), this.delayMs.getSecondValue().intValue() + 1
                );
            }
        }
    }

    public void onMoveInput(MoveInputEvent event) {
        if (this.normalSneakOnly.getValue()
            && Keyboard.isKeyDown(mc.field_71474_y.field_74311_E.func_151463_i())
            && this.shouldSneak()) {
            mc.field_71439_g.field_71158_b.field_78899_d = false;
            mc.field_71439_g.field_71158_b.field_78900_b /= 0.3F;
            mc.field_71439_g.field_71158_b.field_78902_a /= 0.3F;
        }

        if (!mc.field_71439_g.field_71158_b.field_78899_d
            && this.shouldSneak()
            && (this.sneakDelay > 0 || this.canMoveSafely())) {
            mc.field_71439_g.field_71158_b.field_78899_d = true;
            mc.field_71439_g.field_71158_b.field_78902_a *= 0.3F;
            mc.field_71439_g.field_71158_b.field_78900_b *= 0.3F;
        }
    }

    public int getSneakDelay() {
        return this.sneakDelay;
    }
}
