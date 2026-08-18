package miau.module.modules.player.scaffold.features;

import java.util.Collections;
import java.util.List;
import miau.event.impl.LivingUpdateEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.UpdateEvent;
import miau.module.modules.player.Scaffold;
import miau.module.modules.player.scaffold.ScaffoldComponent;
import miau.property.Property;
import miau.util.client.KeyBindUtil;

public class BetaFeature implements ScaffoldComponent {
    private final Scaffold scaffold;
    public int betaAirTicks = 0;
    public int betaGroundTicks = 0;
    public int betaPlaceCooldown = 0;
    public float lastBetaSentYaw = Float.NaN;
    public float lastBetaSentPitch = Float.NaN;
    public long lastBetaPitchQuotient = 0L;
    public int betaPlaceTicks = 999;

    public BetaFeature(Scaffold scaffold) {
        this.scaffold = scaffold;
    }

    @Override
    public List<Property<?>> getProperties() {
        return Collections.emptyList();
    }

    public boolean isBetaMode() {
        int mode = this.scaffold.rotationHandler.rotationMode.getValue();
        return mode == 4 || mode == 5;
    }

    public boolean isBetaTellyMode() {
        int mode = this.scaffold.rotationHandler.rotationMode.getValue();
        return mode == 5
            || this.isBetaMode()
                && (
                    this.scaffold.keepYFeature.keepY.getValue() == 3
                        || this.scaffold.keepYFeature.keepY.getValue() == 4
                )
                && (!this.scaffold.keepYFeature.tellyRightClick.getValue() || this.isRightClickHeld());
    }

    private boolean isRightClickHeld() {
        return Scaffold.mc.field_71474_y != null && Scaffold.mc.field_71474_y.field_74313_G.func_151470_d();
    }

    public void quietBetaMovement() {
        if (this.isBetaMode() && !this.isBetaTellyMode() && Scaffold.mc.field_71439_g != null) {
            Scaffold.mc.field_71439_g.func_70031_b(false);
            if (Scaffold.mc.field_71474_y != null) {
                KeyBindUtil.setKeyBindState(Scaffold.mc.field_71474_y.field_151444_V.func_151463_i(), false);
            }
        }
    }

    public boolean canBetaPlaceNow() {
        if (!this.isBetaMode()) {
            return true;
        }

        if (Scaffold.mc.field_71439_g != null && !this.scaffold.placedThisTick && this.betaPlaceCooldown <= 0) {
            if ((this.isBetaTellyMode() || !Scaffold.mc.field_71439_g.func_70051_ag())
                && !Scaffold.mc.field_71439_g.field_70123_F
                && Scaffold.mc.field_71439_g.field_70737_aN <= 0) {
                return !Scaffold.mc.field_71439_g.field_70122_E
                    ? this.betaAirTicks > 1
                    : Math.abs(Scaffold.mc.field_71439_g.field_70181_x) < 1.0E-4 && this.betaGroundTicks > 0;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (this.isBetaMode() && Scaffold.mc.field_71439_g != null) {
            if (Scaffold.mc.field_71439_g.field_70122_E) {
                this.betaGroundTicks++;
                this.betaAirTicks = 0;
            } else {
                this.betaAirTicks++;
                this.betaGroundTicks = 0;
            }

            if (this.betaPlaceCooldown > 0) {
                this.betaPlaceCooldown--;
            }
        } else {
            this.betaAirTicks = 0;
            this.betaGroundTicks = 0;
            this.betaPlaceCooldown = 0;
        }
    }

    @Override
    public void onMoveInput(MoveInputEvent event) {
        this.quietBetaMovement();
    }

    @Override
    public void onLivingUpdate(LivingUpdateEvent event) {
        this.quietBetaMovement();
    }

    @Override
    public void onDisable() {
        this.betaAirTicks = 0;
        this.betaGroundTicks = 0;
        this.betaPlaceCooldown = 0;
        this.lastBetaSentYaw = Float.NaN;
        this.lastBetaSentPitch = Float.NaN;
        this.lastBetaPitchQuotient = 0L;
        this.betaPlaceTicks = 999;
    }
}
