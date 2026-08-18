package miau.module.modules.player.scaffold.features;

import java.util.Arrays;
import java.util.List;
import miau.event.impl.SafeWalkEvent;
import miau.module.modules.player.Scaffold;
import miau.module.modules.player.scaffold.ScaffoldComponent;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import miau.util.player.PlayerUtil;
import net.minecraft.client.Minecraft;

public class SafeWalkFeature implements ScaffoldComponent {
    private final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty safeWalk = new BooleanProperty("safe-walk", true);

    public SafeWalkFeature(Scaffold scaffold) {
    }

    @Override
    public List<Property<?>> getProperties() {
        return Arrays.asList(this.safeWalk);
    }

    @Override
    public void onSafeWalk(SafeWalkEvent event) {
        if (this.mc.field_71439_g != null
            && this.safeWalk.getValue()
            && this.mc.field_71439_g.field_70122_E
            && this.mc.field_71439_g.field_70181_x <= 0.0
            && PlayerUtil.canMove(this.mc.field_71439_g.field_70159_w, this.mc.field_71439_g.field_70179_y, -1.0)) {
            event.setSafeWalk(true);
        }
    }
}
