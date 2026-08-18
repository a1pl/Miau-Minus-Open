package miau.module.modules.movement;

import java.util.ArrayList;
import java.util.List;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.RightClickMouseEvent;
import miau.event.impl.UpdateEvent;
import miau.module.Module;
import miau.module.modules.movement.noslow.NoSlowMode;
import miau.module.modules.movement.noslow.OMExhiNoSlow;
import miau.module.modules.movement.noslow.OMGrimNoSlow;
import miau.module.modules.movement.noslow.OMGrimTestNoSlow;
import miau.module.modules.movement.noslow.OMHypixelNoSlow;
import miau.module.modules.movement.noslow.OMIntaveNoSlow;
import miau.module.modules.movement.noslow.OMLuckyvnNoSlow;
import miau.module.modules.movement.noslow.OMNCPNoSlow;
import miau.module.modules.movement.noslow.OMNewGrimNoSlow;
import miau.module.modules.movement.noslow.OMOldGrimNoSlow;
import miau.module.modules.movement.noslow.OMOldIntaveNoSlow;
import miau.module.modules.movement.noslow.OMVanillaNoSlow;
import miau.module.modules.movement.noslow.OMVulcanNoSlow;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.ItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty(
        "Mode",
        0,
        new String[]{
            "Vanilla",
            "Intave",
            "Old Grim",
            "Grim Test",
            "Hypixel",
            "NCP",
            "Old Intave",
            "Exhi",
            "New Grim",
            "Vulcan",
            "Grim 1.9",
            "Luckyvn"
        }
    );
    public final IntProperty grimTestMaxTicks = new IntProperty(
        "grim-test-max-ticks", 5, 1, 20, () -> this.mode.getValue() == 3
    );
    public final IntProperty grimInterval = new IntProperty("grim-interval", 4, 1, 20, () -> this.mode.getValue() == 8);
    public final IntProperty vulcanInterval = new IntProperty(
        "vulcan-interval", 5, 1, 20, () -> this.mode.getValue() == 9
    );
    public final BooleanProperty hypixelJump = new BooleanProperty(
        "hypixel-jump", true, () -> this.mode.getValue() == 4
    );
    public final BooleanProperty swordValue = new BooleanProperty("sword", true);
    public final BooleanProperty foodValue = new BooleanProperty("food", true);
    public final BooleanProperty potionValue = new BooleanProperty("potion", true);
    public final BooleanProperty bowValue = new BooleanProperty("bow", true);
    public final BooleanProperty antiSwitch = new BooleanProperty("anti-switch", false);
    private final List<NoSlowMode> modes = new ArrayList<>();

    public NoSlow() {
        super("NoSlow", false);
        this.modes.add(new OMVanillaNoSlow("Vanilla", this));
        this.modes.add(new OMIntaveNoSlow("Intave", this));
        this.modes.add(new OMGrimNoSlow("Old Grim", this));
        this.modes.add(new OMGrimTestNoSlow("Grim Test", this));
        this.modes.add(new OMHypixelNoSlow("Hypixel", this));
        this.modes.add(new OMNCPNoSlow("NCP", this));
        this.modes.add(new OMOldIntaveNoSlow("Old Intave", this));
        this.modes.add(new OMExhiNoSlow("Exhi", this));
        this.modes.add(new OMNewGrimNoSlow("New Grim", this));
        this.modes.add(new OMVulcanNoSlow("Vulcan", this));
        this.modes.add(new OMOldGrimNoSlow("Grim 1.9", this));
        this.modes.add(new OMLuckyvnNoSlow("Luckyvn", this));
    }

    private NoSlowMode getActiveMode() {
        return this.modes.get(this.mode.getValue());
    }

    public void onEnable() {
        this.getActiveMode().onEnable();
    }

    public void onDisable() {
        this.getActiveMode().onDisable();
    }

    public boolean isSwordActive() {
        return this.swordValue.getValue() && ItemUtil.isHoldingSword();
    }

    public boolean isFoodActive() {
        return this.foodValue.getValue() && ItemUtil.isEating();
    }

    public boolean isBowActive() {
        return this.bowValue.getValue() && ItemUtil.isUsingBow();
    }

    public boolean isPotionActive() {
        return this.potionValue.getValue()
            && mc.field_71439_g.func_71039_bw()
            && mc.field_71439_g.func_70694_bm().func_77973_b() instanceof ItemPotion;
    }

    public boolean isAntiSwitchActive() {
        if (this.isEnabled() && this.antiSwitch.getValue() && mc.field_71439_g != null && mc.field_71441_e != null) {
            ItemStack heldItem = mc.field_71439_g.func_70694_bm();
            return heldItem != null && heldItem.func_77973_b() instanceof ItemSword
                ? mc.field_71439_g.func_71039_bw()
                : false;
        } else {
            return false;
        }
    }

    public boolean isAnyActive() {
        return mc.field_71439_g.func_71039_bw()
            && (this.isSwordActive() || this.isFoodActive() || this.isBowActive() || this.isPotionActive());
    }

    public boolean shouldCancelSlowdown() {
        if (!this.isEnabled()) {
            return false;
        }

        NoSlowMode activeMode = this.getActiveMode();
        return activeMode instanceof OMGrimTestNoSlow ? ((OMGrimTestNoSlow)activeMode).shouldCancelSlowdown() : true;
    }

    public boolean canSprint() {
        return true;
    }

    public float getMotionMultiplier() {
        return this.mode.getValue() == 2 ? 0.35F : 1.0F;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()) {
            this.getActiveMode().onUpdate(event);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            this.getActiveMode().onPacket(event);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            this.getActiveMode().onRightClick(event);
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
