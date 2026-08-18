package miau.management;

import java.util.LinkedHashMap;
import miau.enums.FloatModules;
import miau.event.EventTarget;
import miau.event.impl.PlayerUpdateEvent;
import net.minecraft.client.Minecraft;

public class FloatManager {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final LinkedHashMap<FloatModules, Boolean> activeMap = new LinkedHashMap<>();
    private boolean floating = false;

    public boolean isPredicted() {
        return this.floating;
    }

    public boolean isFalling() {
        return mc.field_71439_g.field_70122_E
            && mc.field_71439_g.field_70163_u - mc.field_71439_g.field_70137_T < 0.0
            && mc.field_71439_g.field_70181_x < 0.0;
    }

    public boolean hasActiveModule() {
        return this.activeMap.containsValue(true);
    }

    public void setFloatState(boolean state, FloatModules floatModules) {
        this.activeMap.put(floatModules, state);
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if ((this.hasActiveModule() || this.isPredicted()) && this.isFalling()) {
            mc.field_71439_g
                .func_70107_b(
                    mc.field_71439_g.field_70165_t,
                    mc.field_71439_g.field_70163_u + 0.001,
                    mc.field_71439_g.field_70161_v
                );
            this.floating = true;
        } else {
            this.floating = false;
        }
    }
}
