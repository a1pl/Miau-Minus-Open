package miau.module.modules.ghost;

import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.LivingUpdateEvent;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemSword;
import org.lwjgl.input.Mouse;

public class BlockHit extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Legit"});
    public final BooleanProperty rmb = new BooleanProperty("RightClick Only", false);
    public final FloatProperty hurtime = new FloatProperty("Hurtime", 2.0F, 0.0F, 10.0F);
    public EntityLivingBase target;
    public boolean down;

    public BlockHit() {
        super("BlockHit", false, false);
    }

    @Override
    public void onEnabled() {
        this.target = null;
        this.down = false;
    }

    @Override
    public void onDisabled() {
        if (this.down) {
            this.release();
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (event.getTarget() != null && event.getTarget() instanceof EntityLivingBase) {
            this.target = (EntityLivingBase)event.getTarget();
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (mc.field_71439_g != null) {
            if (this.isPlayerHoldingSword()) {
                if (this.rmb.getValue() && !Mouse.isButtonDown(1)) {
                    if (this.down) {
                        this.release();
                    }
                } else if (this.target == null) {
                    if (this.down) {
                        this.release();
                    }
                } else if (!this.down && this.target.field_70737_aN > this.hurtime.getValue()) {
                    this.press();
                } else {
                    if (this.down && this.target.field_70737_aN <= this.hurtime.getValue()) {
                        this.release();
                    }
                }
            }
        }
    }

    public boolean isPlayerHoldingSword() {
        return mc.field_71439_g.func_71045_bC() != null
            && mc.field_71439_g.func_71045_bC().func_77973_b() instanceof ItemSword;
    }

    private void release() {
        int key = mc.field_71474_y.field_74313_G.func_151463_i();
        KeyBinding.func_74510_a(key, false);
        this.down = false;
        this.target = null;
    }

    private void press() {
        this.down = true;
        int key = mc.field_71474_y.field_74313_G.func_151463_i();
        KeyBinding.func_74510_a(key, true);
    }
}
