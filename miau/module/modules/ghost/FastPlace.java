package miau.module.modules.ghost;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import miau.event.EventTarget;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorMinecraft;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.util.player.RotationUtil;
import miau.util.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockObsidian;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class FastPlace extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final DecimalFormat df = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.US));
    private long delayMS = 0L;
    public final FloatProperty delay = new FloatProperty("delay", 1.0F, 1.0F, 3.0F);
    public final BooleanProperty blocksOnly = new BooleanProperty("blocks-only", true);
    public final BooleanProperty placeFix = new BooleanProperty("place-fix", true);
    public final BooleanProperty skipObsidian = new BooleanProperty("skip-obsidian", true);
    public final BooleanProperty skipInteractable = new BooleanProperty("skip-interactable", true);

    private boolean canPlace() {
        ItemStack stack = mc.field_71439_g.func_70694_bm();
        if (stack != null) {
            Item item = stack.func_77973_b();
            if (item instanceof ItemFishingRod) {
                return false;
            }

            if (item instanceof ItemBlock) {
                Block block = ((ItemBlock)item).func_179223_d();
                if (this.skipObsidian.getValue() && block instanceof BlockObsidian) {
                    return false;
                }

                if (this.skipInteractable.getValue() && BlockUtil.isInteractable(block)) {
                    return false;
                }

                if (!this.placeFix.getValue()) {
                    return true;
                }

                MovingObjectPosition mop = RotationUtil.rayTrace(
                    mc.field_71439_g.field_70177_z,
                    mc.field_71439_g.field_70125_A,
                    mc.field_71442_b.func_78757_d(),
                    1.0F
                );
                return mop != null
                    && mop.field_72313_a == MovingObjectType.BLOCK
                    && ((ItemBlock)item)
                        .func_179222_a(
                            mc.field_71441_e, mop.func_178782_a(), mop.field_178784_b, mc.field_71439_g, stack
                        );
            }
        }

        return !this.blocksOnly.getValue();
    }

    public FastPlace() {
        super("FastPlace", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            int rightClickDelayTimer = ((IAccessorMinecraft)mc).getRightClickDelayTimer();
            if (rightClickDelayTimer == 4) {
                this.delayMS = this.delayMS + (long)(50.0F * this.delay.getValue());
            }

            if (this.delayMS > 0L) {
                this.delayMS -= 50L;
            }

            if (this.delayMS <= 0L && rightClickDelayTimer > 1 && this.canPlace()) {
                ((IAccessorMinecraft)mc).setRightClickDelayTimer(0);
            }
        }
    }

    @Override
    public void onDisabled() {
        this.delayMS = 0L;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{df.format(this.delay.getValue())};
    }
}
