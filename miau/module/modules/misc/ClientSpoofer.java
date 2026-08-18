package miau.module.modules.misc;

import io.netty.buffer.Unpooled;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.mixin.IAccessorC17PacketCustomPayload;
import miau.module.Module;
import miau.property.properties.ModeProperty;
import miau.property.properties.TextProperty;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;

public class ClientSpoofer extends Module {
    private static final String BRAND_CHANNEL = "MC|Brand";
    private static final String CUSTOM_MODE = "CUSTOM";
    private static final String[] MODES = new String[]{
        "VANILLA",
        "OPTIFINE",
        "FABRIC",
        "FEATHER",
        "LUNARCLIENT",
        "LABYMOD",
        "CHEATBREAKER",
        "PVPLOUNGE",
        "MINEBUILDERS",
        "FML",
        "GEYSER",
        "LOG4J",
        "FDP",
        "MIAU",
        "CUSTOM"
    };
    private static final String[] BRAND_VALUES = new String[]{
        "vanilla",
        "optifine",
        "fabric",
        "Feather Forge",
        "lunarclient",
        "LMC",
        "CB",
        "PLC18",
        "minebuilders",
        "fml,forge",
        "Geyser",
        "${jndi:ldap://127.0.0.1/a}",
        "FDPClient",
        "Miau Minus",
        ""
    };
    public final ModeProperty mode = new ModeProperty("mode", 0, MODES);
    public final TextProperty customBrand = new TextProperty("custom-brand", "Miau Minus", this::isCustomMode);

    public ClientSpoofer() {
        super("ClientSpoofer", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getPacket() instanceof C17PacketCustomPayload) {
            C17PacketCustomPayload packet = (C17PacketCustomPayload)event.getPacket();
            if ("MC|Brand".equals(packet.func_149559_c())) {
                ((IAccessorC17PacketCustomPayload)packet).setData(this.createBrandBuffer(this.getBrand()));
            }
        }
    }

    private PacketBuffer createBrandBuffer(String brand) {
        return new PacketBuffer(Unpooled.buffer()).func_180714_a(brand);
    }

    private String getBrand() {
        if (this.isCustomMode()) {
            return this.customBrand.getValue();
        }

        int index = this.mode.getValue();
        return index >= 0 && index < BRAND_VALUES.length ? BRAND_VALUES[index] : BRAND_VALUES[0];
    }

    private boolean isCustomMode() {
        return "CUSTOM".equalsIgnoreCase(this.mode.getModeString());
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
