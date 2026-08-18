package miau.module.modules.misc;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.LivingUpdateEvent;
import miau.module.Module;
import miau.module.modules.combat.Velocity;
import miau.module.modules.combat.velocity.StandardVelocity;
import miau.module.modules.combat.velocity.VelocityMode;
import miau.property.properties.BooleanProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;

public class Fireball extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty autoDisable = new BooleanProperty("Auto disable", false);
    private boolean active = false;
    private int tickCounter = 0;
    private boolean resetting = false;
    private boolean done = false;
    private int savedHorizontal = -1;
    private int savedExplosionHorizontal = -1;

    public Fireball() {
        super("Fireball", false);
    }

    @Override
    public void onEnabled() {
        this.active = false;
        this.tickCounter = 0;
        this.resetting = false;
        this.done = false;
        this.savedHorizontal = -1;
        this.savedExplosionHorizontal = -1;
    }

    @Override
    public void onDisabled() {
        this.restoreVelocity();
    }

    private StandardVelocity getStandard() {
        Velocity velocity = (Velocity)Miau.moduleManager.modules.get(Velocity.class);
        if (velocity == null) {
            return null;
        }

        for (VelocityMode mode : velocity.modes) {
            if (mode instanceof StandardVelocity) {
                return (StandardVelocity)mode;
            }
        }

        return null;
    }

    private void applyVelocity(int horizontal) {
        StandardVelocity standard = this.getStandard();
        if (standard != null) {
            if (this.savedHorizontal == -1) {
                this.savedHorizontal = standard.horizontal.getValue();
                this.savedExplosionHorizontal = standard.explosionHorizontal.getValue();
            }

            standard.horizontal.setValue(horizontal);
            standard.explosionHorizontal.setValue(horizontal);
        }
    }

    private void restoreVelocity() {
        StandardVelocity standard = this.getStandard();
        if (standard != null && this.savedHorizontal != -1) {
            standard.horizontal.setValue(this.savedHorizontal);
            standard.explosionHorizontal.setValue(this.savedExplosionHorizontal);
            this.savedHorizontal = -1;
            this.savedExplosionHorizontal = -1;
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            ItemStack currentItem = mc.field_71439_g.field_71071_by.func_70448_g();
            boolean isHoldingFireCharge = currentItem != null && currentItem.func_77973_b() instanceof ItemFireball;
            boolean rmb = Mouse.isButtonDown(1);
            if (rmb && isHoldingFireCharge) {
                if (!this.active) {
                    this.active = true;
                    this.resetting = false;
                    this.done = false;
                    this.applyVelocity(100);
                }
            } else if (this.active) {
                this.active = false;
                this.tickCounter = 10;
                this.resetting = true;
            }

            if (!isHoldingFireCharge && !this.resetting && !this.done) {
                this.tickCounter = 10;
                this.resetting = true;
            }

            if (this.resetting && this.tickCounter > 0) {
                this.tickCounter--;
                if (this.tickCounter == 0 && !this.done) {
                    this.applyVelocity(0);
                    this.done = true;
                    this.resetting = false;
                }
            }

            if (this.autoDisable.getValue() && this.done) {
                this.setEnabled(false);
            }
        }
    }
}
