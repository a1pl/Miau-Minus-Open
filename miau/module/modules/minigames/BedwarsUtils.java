package miau.module.modules.minigames;

import java.util.ArrayList;
import java.util.List;
import miau.event.EventTarget;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render2DEvent;
import miau.module.Module;
import miau.module.modules.minigames.bedwarsutils.BedwarsComponent;
import miau.module.modules.minigames.bedwarsutils.features.EventTimersFeature;
import miau.module.modules.minigames.bedwarsutils.features.UpgradeHUDFeature;
import miau.property.Property;

public class BedwarsUtils extends Module {
    private final List<BedwarsComponent> components = new ArrayList<>();
    public final UpgradeHUDFeature upgradeHUDFeature = new UpgradeHUDFeature(this);
    public final EventTimersFeature eventTimersFeature = new EventTimersFeature(this);

    public BedwarsUtils() {
        super("BedwarsUtils", false, true);
        this.components.add(this.upgradeHUDFeature);
        this.components.add(this.eventTimersFeature);
    }

    @Override
    public List<Property<?>> getAdditionalProperties() {
        List<Property<?>> props = new ArrayList<>();

        for (BedwarsComponent component : this.components) {
            props.addAll(component.getProperties());
        }

        return props;
    }

    @Override
    public void onEnabled() {
        super.onEnabled();
        this.components.forEach(BedwarsComponent::onReset);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        this.components.forEach(c -> c.onPacket(event));
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        this.components.forEach(c -> c.onRender2D(event));
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.components.forEach(BedwarsComponent::onReset);
    }
}
