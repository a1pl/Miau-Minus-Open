package miau.module.modules.player.scaffold.features;

import java.util.Arrays;
import java.util.List;
import miau.event.impl.UpdateEvent;
import miau.module.modules.player.Scaffold;
import miau.module.modules.player.scaffold.ScaffoldComponent;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public class KeepYFeature implements ScaffoldComponent {
    private final Scaffold scaffold;
    private final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty keepY = new ModeProperty(
        "keep-y", 0, new String[]{"NONE", "VANILLA", "Extra 1 Block", "TELLY", "EXTRATELLY", "BETA"}
    );
    public final BooleanProperty keepYonPress = new BooleanProperty(
        "keep-y-on-press", false, () -> this.keepY.getValue() != 0
    );
    public final BooleanProperty tellyRightClick = new BooleanProperty(
        "telly-on-right-click", false, () -> this.keepY.getValue() == 3 || this.keepY.getValue() == 4
    );
    public final FloatProperty betaTickDelay = new FloatProperty(
        "beta-tick-delay", 3.0F, 1.0F, 10.0F, () -> this.keepY.getValue() == 5
    );
    public final FloatProperty betaSmoothSpeed = new FloatProperty(
        "beta-smooth-speed", 0.6F, 0.1F, 1.0F, () -> this.keepY.getValue() == 5
    );
    public int betaAirTicks = 0;
    public boolean betaPlacedThisCycle = false;

    public KeepYFeature(Scaffold scaffold) {
        this.scaffold = scaffold;
    }

    @Override
    public List<Property<?>> getProperties() {
        return Arrays.asList(
            this.keepY, this.keepYonPress, this.tellyRightClick, this.betaTickDelay, this.betaSmoothSpeed
        );
    }

    @Override
    public void onEnable() {
        this.betaAirTicks = 0;
        this.betaPlacedThisCycle = false;
    }

    @Override
    public void onDisable() {
        this.betaAirTicks = 0;
        this.betaPlacedThisCycle = false;
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (this.keepY.getValue() == 5) {
            if (this.mc.field_71439_g.field_70122_E) {
                this.betaAirTicks = 0;
                this.betaPlacedThisCycle = false;
            } else {
                this.betaAirTicks++;
            }
        } else {
            this.betaAirTicks = 0;
            this.betaPlacedThisCycle = false;
        }

        if (this.mc.field_71439_g.field_70122_E) {
            if (this.scaffold.stage > 0) {
                this.scaffold.stage--;
            }

            if (this.scaffold.stage < 0) {
                this.scaffold.stage++;
            }

            if (this.scaffold.stage == 0
                && this.keepY.getValue() != 0
                && (
                    this.keepYonPress.getValue()
                        ? Scaffold.mc.field_71474_y.field_74313_G.func_151470_d()
                        : !this.mc.field_71474_y.field_74314_A.func_151470_d()
                )) {
                this.scaffold.stage = 1;
            }

            this.scaffold.startY = this.scaffold.shouldKeepY
                ? this.scaffold.startY
                : MathHelper.func_76128_c(this.mc.field_71439_g.field_70163_u);
            this.scaffold.shouldKeepY = false;
        }
    }
}
