package miau.module.modules.player.scaffold.rotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miau.event.impl.UpdateEvent;
import miau.module.modules.player.Scaffold;
import miau.property.Property;
import miau.property.properties.ModeProperty;
import miau.util.math.RandomUtil;
import miau.util.player.RotationUtil;
import net.minecraft.util.MathHelper;

public class RotationHandler {
    private final Scaffold scaffold;
    private final Map<Integer, IRotationLogic> rotationLogics = new HashMap<>();
    public final ModeProperty rotationMode = new ModeProperty(
        "rotations", 2, new String[]{"NONE", "Normal", "Backwards", "Sideways", "Beta", "Telly"}
    );

    public List<Property<?>> getProperties() {
        return Arrays.asList(this.rotationMode);
    }

    public RotationHandler(Scaffold scaffold) {
        this.scaffold = scaffold;
        this.rotationLogics.put(1, new DefaultRotation());
        this.rotationLogics.put(2, new BackwardsRotation());
        this.rotationLogics.put(3, new SidewaysRotation());
        this.rotationLogics.put(4, new BetaRotation());
        this.rotationLogics.put(5, new SnapRotation());
    }

    public void handleInitialRotation(UpdateEvent event, float currentYaw, float yawDiffTo180, float diagonalYaw) {
        IRotationLogic logic = this.rotationLogics.get(this.rotationMode.getValue());
        if (logic != null) {
            logic.handleInitialRotation(this.scaffold, event, currentYaw, yawDiffTo180, diagonalYaw);
        }
    }

    public void handleUpdateRotation(
        UpdateEvent event, float yawDiffTo180, float diagonalYaw, boolean towerRotating, boolean willPlaceThisTick
    ) {
        int mode = this.rotationMode.getValue();
        boolean betaMode = mode == 4;
        boolean snapMode = mode == 5;
        boolean betaTelly = this.scaffold.betaFeature.isBetaTellyMode();
        if (mode != 0) {
            float targetYaw = this.scaffold.yaw;
            float targetPitch = this.scaffold.pitch;
            if (!betaMode
                && !snapMode
                && this.scaffold.towering
                && (
                    Scaffold.mc.field_71439_g.field_70181_x > 0.0
                        || Scaffold.mc.field_71439_g.field_70163_u > this.scaffold.startY + 1
                )) {
                this.handleTowerRotation(event, towerRotating);
                targetYaw = this.scaffold.yaw;
                targetPitch = this.scaffold.pitch;
            }

            if (betaTelly
                && !snapMode
                && this.scaffold.towering
                && (
                    Scaffold.mc.field_71439_g.field_70181_x > 0.0
                        || Scaffold.mc.field_71439_g.field_70163_u > this.scaffold.startY + 1
                )) {
                float yawDiff = MathHelper.func_76142_g(this.scaffold.yaw - event.getYaw());
                float tolerance = this.scaffold.rotationTick >= 2
                    ? RandomUtil.nextFloat(
                        this.scaffold.options.tellystartrotationminspeed.getValue(),
                        this.scaffold.options.tellystartrotationmaxspeed.getValue()
                    )
                    : RandomUtil.nextFloat(
                        this.scaffold.options.tellynormalrotationminspeed.getValue(),
                        this.scaffold.options.tellynormalrotationmaxspeed.getValue()
                    );
                if (Math.abs(yawDiff) > tolerance) {
                    float clampedYaw = RotationUtil.clampAngle(yawDiff, tolerance);
                    this.scaffold.yaw = RotationUtil.quantizeAngle(event.getYaw() + clampedYaw);
                    this.scaffold.rotationTick = Math.max(this.scaffold.rotationTick, 1);
                }
            }

            float placeYaw;
            float placePitch;
            if (betaMode) {
                float[] pipelineResult = ((BetaRotation)this.rotationLogics.get(4))
                    .handleBetaUpdate(this.scaffold, event, yawDiffTo180, diagonalYaw, towerRotating, willPlaceThisTick);
                placeYaw = pipelineResult[0];
                placePitch = pipelineResult[1];
                this.scaffold.placeYaw = placeYaw;
                this.scaffold.placePitch = placePitch;
            } else if (snapMode) {
                ((SnapRotation)this.rotationLogics.get(5)).updateRotation(this.scaffold, event);
                placeYaw = this.scaffold.yaw;
                placePitch = this.scaffold.pitch;
                this.scaffold.placeYaw = placeYaw;
                this.scaffold.placePitch = placePitch;
            } else {
                float[] placeGcd = RotationUtil.flexRotation(targetYaw, targetPitch, event.getYaw(), event.getPitch());
                placeYaw = placeGcd[0];
                placePitch = placeGcd[1];
                this.scaffold.placeYaw = placeYaw;
                this.scaffold.placePitch = placePitch;
            }

            boolean moveFix = this.scaffold.options.movementCorrection.getValue();
            float packetYaw = placeYaw;
            float packetPitch = placePitch;
            if (moveFix && mode == 2 && !Float.isNaN(this.scaffold.bridgeYaw) && !willPlaceThisTick) {
                float bridgePitch = !Float.isNaN(this.scaffold.placePitch) ? this.scaffold.placePitch : targetPitch;
                float[] bridgeGcd = RotationUtil.flexRotation(
                    this.scaffold.bridgeYaw, bridgePitch, event.getYaw(), event.getPitch()
                );
                packetYaw = bridgeGcd[0];
                packetPitch = bridgeGcd[1];
            }

            targetYaw = packetYaw;
            targetPitch = packetPitch;
            if (!betaMode && !snapMode && willPlaceThisTick) {
                float deltaX = Math.abs(MathHelper.func_76142_g(targetYaw - event.getYaw()));
                if (deltaX > 2.0F
                    && !Float.isNaN(this.scaffold.lastPlacedAbsPacketYawDelta)
                    && Math.abs(deltaX - this.scaffold.lastPlacedAbsPacketYawDelta) < 1.0E-4F) {
                    double gcdStep = RotationUtil.mouseGcdStepMultiplier();
                    if (gcdStep >= 0.01) {
                        targetYaw += (float)(this.scaffold.duplicatePlaceRotNudgeSign * gcdStep);
                        this.scaffold.duplicatePlaceRotNudgeSign = -this.scaffold.duplicatePlaceRotNudgeSign;
                    }
                }

                this.scaffold.lastPlacedAbsPacketYawDelta = deltaX;
            }

            event.setRotation(targetYaw, targetPitch, 3);
            this.scaffold.lastMoveFixPacketYaw = targetYaw;
            if (moveFix) {
                event.setPervRotation(targetYaw, 3);
            }
        }
    }

    private void handleTowerRotation(UpdateEvent event, boolean towerRotating) {
        float yawDiff = MathHelper.func_76142_g(this.scaffold.yaw - event.getYaw());
        float tolerance = this.scaffold.rotationTick >= 2
            ? RandomUtil.nextFloat(
                this.scaffold.options.tellystartrotationminspeed.getValue(),
                this.scaffold.options.tellystartrotationmaxspeed.getValue()
            )
            : RandomUtil.nextFloat(
                this.scaffold.options.tellynormalrotationminspeed.getValue(),
                this.scaffold.options.tellynormalrotationmaxspeed.getValue()
            );
        if (Math.abs(yawDiff) > tolerance) {
            float clampedYaw = RotationUtil.clampAngle(yawDiff, tolerance);
            this.scaffold.yaw = RotationUtil.quantizeAngle(event.getYaw() + clampedYaw);
            this.scaffold.rotationTick = Math.max(this.scaffold.rotationTick, 1);
        }

        if (towerRotating && this.scaffold.isTowering()) {
            float yawDelta = MathHelper.func_76142_g(Scaffold.mc.field_71439_g.field_70177_z - event.getYaw());
            this.scaffold.yaw = RotationUtil.quantizeAngle(
                event.getYaw() + yawDelta * RandomUtil.nextFloat(0.98F, 0.99F)
            );
            this.scaffold.pitch = RotationUtil.quantizeAngle(RandomUtil.nextFloat(30.0F, 80.0F));
        }

        this.scaffold.rotationTick = 3;
        this.scaffold.towering = true;
    }
}
