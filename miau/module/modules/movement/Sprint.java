package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.TickEvent;
import miau.mixin.IAccessorEntityLivingBase;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.util.client.KeyBindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

public class Sprint extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private boolean wasSprinting = false;
    public final BooleanProperty foxFix = new BooleanProperty("fov-fix", true);

    public Sprint() {
        super("Sprint", true, true);
    }

    public boolean shouldApplyFovFix(IAttributeInstance attribute) {
        if (!this.foxFix.getValue()) {
            return false;
        }

        AttributeModifier attributeModifier = ((IAccessorEntityLivingBase)mc.field_71439_g)
            .getSprintingSpeedBoostModifier();
        return attribute.func_111127_a(attributeModifier.func_111167_a()) == null && this.wasSprinting;
    }

    public boolean shouldKeepFov(boolean boolean2) {
        return this.foxFix.getValue() && !boolean2 && this.wasSprinting;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            switch (event.getType()) {
                case PRE:
                    KeyBindUtil.setKeyBindState(mc.field_71474_y.field_151444_V.func_151463_i(), true);
                    break;
                case POST:
                    this.wasSprinting = mc.field_71439_g.func_70051_ag();
            }
        }
    }

    @Override
    public void onDisabled() {
        this.wasSprinting = false;
        KeyBindUtil.updateKeyState(mc.field_71474_y.field_151444_V.func_151463_i());
    }
}
