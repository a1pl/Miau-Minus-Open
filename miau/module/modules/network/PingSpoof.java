package miau.module.modules.network;

import miau.component.PingSpoofComponent;
import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.util.math.MathUtil;

public class PingSpoof extends Module {
    private final IntProperty delayMin = new IntProperty("delay-min", 1000, 50, 30000);
    private final IntProperty delayMax = new IntProperty("delay-max", 1500, 50, 30000);
    private final BooleanProperty teleports = new BooleanProperty("delay-teleports", false);
    private final BooleanProperty velocity = new BooleanProperty("delay-velocity", false);
    private final BooleanProperty entities = new BooleanProperty("delay-entities", false);

    public PingSpoof() {
        super("PingSpoof", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.delayMin.getValue() + "-" + this.delayMax.getValue()};
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.isEnabled()) {
                int amount = (int)MathUtil.getRandom(
                    this.delayMin.getValue().intValue(), this.delayMax.getValue().intValue()
                );
                PingSpoofComponent.spoof(
                    amount, true, this.velocity.getValue(), this.teleports.getValue(), this.entities.getValue()
                );
            }
        }
    }
}
