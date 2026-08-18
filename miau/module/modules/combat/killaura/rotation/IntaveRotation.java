package miau.module.modules.combat.killaura.rotation;

import java.util.Random;
import miau.component.RotationComponent;
import miau.event.impl.UpdateEvent;
import miau.module.modules.combat.KillAura;
import miau.util.player.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class IntaveRotation extends RotationMode {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final Random RNG = new Random();
    private long tick = 0L;
    private double noiseSeed;
    private double yawNoisePhase;
    private double pitchNoisePhase;
    private double yawNoiseFreq = 0.07;
    private double pitchNoiseFreq = 0.11;
    private double yawBias;
    private double pitchBias;
    private double yawBiasTarget;
    private double pitchBiasTarget;
    private int biasChangeCooldown;
    private int gcdBreakTimer = 0;
    private int gcdBreakInterval = 3;
    private boolean gcdOffsetPositive = true;
    private double overshootYaw;
    private double overshootPitch;
    private int correctionTicks;
    private double currentSpeedMultiplier = 1.0;
    private double speedPhase;
    private int swingSkipCounter = 0;

    public IntaveRotation(KillAura killAura) {
        super(killAura, "INTAVE");
        this.noiseSeed = RNG.nextDouble() * 1000.0;
        this.yawNoisePhase = RNG.nextDouble() * 100.0;
        this.pitchNoisePhase = RNG.nextDouble() * 100.0;
        this.yawBiasTarget = (RNG.nextDouble() - 0.5) * 4.0;
        this.pitchBiasTarget = (RNG.nextDouble() - 0.5) * 3.0;
    }

    @Override
    public float[] processRotations(float[] targetRots, float[] lastRots, double rotSpeed, UpdateEvent event) {
        if (rotSpeed == 0.0) {
            return lastRots;
        }

        RotationComponent.setActive(true, this.killAura.moveFix.getValue());
        this.tick++;
        EntityLivingBase target = this.killAura.getTarget();
        if (target == null) {
            RotationComponent.markSmoothed(lastRots);
            return lastRots;
        }

        this.speedPhase = this.speedPhase + (0.03 + (RNG.nextDouble() - 0.5) * 0.01);
        double speedWave = 0.7 + 0.3 * (0.5 + 0.5 * Math.sin(this.speedPhase));
        double yawDist = Math.abs(MathHelper.func_76142_g(targetRots[0] - lastRots[0]));
        double pitchDist = Math.abs(targetRots[1] - lastRots[1]);
        double totalDist = Math.sqrt(yawDist * yawDist + pitchDist * pitchDist);
        double distanceFactor = 0.6 + 0.4 * Math.min(1.0, totalDist / 15.0);
        double effectiveSpeed = rotSpeed * speedWave * distanceFactor;
        if (effectiveSpeed < 2.0) {
            effectiveSpeed = 2.0;
        }

        if (effectiveSpeed > 180.0) {
            effectiveSpeed = 180.0;
        }

        this.yawNoisePhase = this.yawNoisePhase + (this.yawNoiseFreq + (RNG.nextDouble() - 0.5) * 0.008);
        this.pitchNoisePhase = this.pitchNoisePhase + (this.pitchNoiseFreq + (RNG.nextDouble() - 0.5) * 0.008);
        if (this.tick % 100L == 0L) {
            this.yawNoiseFreq = 0.04 + RNG.nextDouble() * 0.12;
            this.pitchNoiseFreq = 0.06 + RNG.nextDouble() * 0.15;
        }

        Vec3 aimPoint = this.computeNoisyAimPoint(target);
        if (aimPoint == null) {
            RotationComponent.markSmoothed(lastRots);
            return lastRots;
        }

        Vec3 eyePos = mc.field_71439_g.func_174824_e(1.0F);
        double dx = aimPoint.field_72450_a - eyePos.field_72450_a;
        double dy = aimPoint.field_72448_b - eyePos.field_72448_b;
        double dz = aimPoint.field_72449_c - eyePos.field_72449_c;
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        float baseYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float basePitch = (float)(-Math.toDegrees(Math.atan2(dy, horizDist)));
        this.biasChangeCooldown--;
        if (this.biasChangeCooldown <= 0) {
            this.yawBiasTarget = (RNG.nextDouble() - 0.5) * 5.5;
            this.pitchBiasTarget = (RNG.nextDouble() - 0.5) * 6.0;
            this.biasChangeCooldown = 8 + RNG.nextInt(20);
        }

        this.yawBias = this.yawBias + (this.yawBiasTarget - this.yawBias) * 0.15;
        this.pitchBias = this.pitchBias + (this.pitchBiasTarget - this.pitchBias) * 0.12;
        double yawOscillation = MathHelper.func_76126_a((float)this.yawNoisePhase) * 1.2;
        double pitchOscillation = MathHelper.func_76126_a((float)this.pitchNoisePhase) * 1.8;
        double yawJitter = (RNG.nextDouble() - 0.5) * 0.8;
        double pitchJitter = (RNG.nextDouble() - 0.5) * 1.2;
        if (this.correctionTicks > 0) {
            baseYaw += (float)this.overshootYaw;
            basePitch += (float)this.overshootPitch;
            this.correctionTicks--;
            this.overshootYaw *= 0.7;
            this.overshootPitch *= 0.7;
            if (Math.abs(this.overshootYaw) < 0.01) {
                this.overshootYaw = 0.0;
            }

            if (Math.abs(this.overshootPitch) < 0.01) {
                this.overshootPitch = 0.0;
            }
        } else if (RNG.nextDouble() < 0.08 && totalDist < 10.0) {
            this.overshootYaw = (RNG.nextDouble() - 0.5) * 2.5;
            this.overshootPitch = (RNG.nextDouble() - 0.5) * 2.0;
            this.correctionTicks = 3 + RNG.nextInt(5);
        }

        float targetYaw = baseYaw + (float)this.yawBias + (float)yawOscillation + (float)yawJitter;
        float targetPitch = basePitch + (float)this.pitchBias + (float)pitchOscillation + (float)pitchJitter;
        targetPitch = MathHelper.func_76131_a(targetPitch, -90.0F, 90.0F);
        float yaw = lastRots[0];
        float pitch = lastRots[1];
        float deltaYaw = MathHelper.func_76142_g(targetYaw - yaw);
        float deltaPitch = targetPitch - pitch;
        double angularDist = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
        if (angularDist > 0.001) {
            double distYaw = Math.abs(deltaYaw / angularDist);
            double distPitch = Math.abs(deltaPitch / angularDist);
            double maxYawStep = effectiveSpeed * distYaw;
            double maxPitchStep = effectiveSpeed * distPitch;
            float moveYaw = (float)Math.max(Math.min(deltaYaw, maxYawStep), -maxYawStep);
            float movePitch = (float)Math.max(Math.min(deltaPitch, maxPitchStep), -maxPitchStep);
            yaw = lastRots[0] + moveYaw;
            pitch = lastRots[1] + movePitch;
        }

        this.gcdBreakTimer++;
        this.gcdBreakInterval = 5 + RNG.nextInt(8);
        float gcdYaw = yaw;
        float gcdPitch = pitch;
        if (this.gcdBreakTimer >= this.gcdBreakInterval) {
            this.gcdBreakTimer = 0;
            float sens = mc.field_71474_y.field_74341_c * 0.6F + 0.2F;
            double mult = sens * sens * sens * 8.0F * 0.15;
            this.gcdOffsetPositive = !this.gcdOffsetPositive;
            float offset = (float)(mult * (this.gcdOffsetPositive ? 1 : -1) * 0.5);
            gcdYaw += offset * 0.3F;
            gcdPitch += offset * 0.5F;
            gcdPitch = MathHelper.func_76131_a(gcdPitch, -90.0F, 90.0F);
        }

        float[] fixedRotations = RotationUtil.applySensitivityPatch(gcdYaw, gcdPitch, lastRots[0], lastRots[1]);
        yaw = fixedRotations[0];
        pitch = MathHelper.func_76131_a(fixedRotations[1], -90.0F, 90.0F);
        if (Math.abs(yaw - lastRots[0]) < 0.01 && angularDist > 0.5) {
            yaw += (RNG.nextFloat() - 0.5F) * 0.05F;
        }

        if (Math.abs(pitch - lastRots[1]) < 0.01 && angularDist > 0.5) {
            pitch += (RNG.nextFloat() - 0.5F) * 0.05F;
            pitch = MathHelper.func_76131_a(pitch, -90.0F, 90.0F);
        }

        int iterations = (int)(Minecraft.func_175610_ah() / 20 + RNG.nextDouble() * 5.0);

        for (int i = 1; i <= iterations; i++) {
            if (Math.abs(yaw - lastRots[0]) + Math.abs(pitch - lastRots[1]) > 1.0E-4) {
                yaw += (float)((RNG.nextDouble() - 0.5) / 1500.0);
                pitch -= (float)(RNG.nextDouble() / 300.0);
            }

            float[] fixPass = RotationUtil.applySensitivityPatch(yaw, pitch, lastRots[0], lastRots[1]);
            yaw = fixPass[0];
            pitch = Math.max(-90.0F, Math.min(90.0F, fixPass[1]));
        }

        float finalDeltaYaw = MathHelper.func_76142_g(yaw - lastRots[0]);
        if (Math.abs(finalDeltaYaw) > 170.0F) {
            yaw = lastRots[0] + Math.copySign(170.0F, finalDeltaYaw);
        }

        float[] result = new float[]{yaw, pitch};
        RotationComponent.markSmoothed(result);
        return result;
    }

    private Vec3 computeNoisyAimPoint(EntityLivingBase target) {
        if (target == null) {
            return null;
        }

        AxisAlignedBB bb = target.func_174813_aQ();
        double bbWidth = bb.field_72336_d - bb.field_72340_a;
        double bbHeight = bb.field_72337_e - bb.field_72338_b;
        double bbDepth = bb.field_72334_f - bb.field_72339_c;
        double baseX = (bb.field_72340_a + bb.field_72336_d) / 2.0;
        double baseY = bb.field_72338_b + bbHeight * 0.75;
        double baseZ = (bb.field_72339_c + bb.field_72334_f) / 2.0;
        double hNoise1 = this.improvedNoise(this.noiseSeed, this.yawNoisePhase * 0.5, 0.0);
        double hNoise2 = this.improvedNoise(this.noiseSeed + 100.0, this.pitchNoisePhase * 0.5, 0.0);
        double horizontalSpread = 0.35 * Math.min(0.6, bbWidth);
        double hOffsetX = hNoise1 * horizontalSpread;
        double hOffsetZ = hNoise2 * horizontalSpread;
        double vNoise = this.improvedNoise(this.noiseSeed + 200.0, this.pitchNoisePhase * 0.3, this.yawNoisePhase * 0.2);
        double targetSpeed = Math.hypot(
            target.field_70165_t - target.field_70142_S, target.field_70161_v - target.field_70136_U
        );
        double verticalSpread = 0.25 + 0.3 * Math.min(1.0, targetSpeed * 5.0);
        if (bbHeight > 1.5) {
            verticalSpread += 0.15;
        }

        double vOffset = vNoise * bbHeight * verticalSpread;
        double predX = target.field_70165_t + (target.field_70165_t - target.field_70142_S) * 0.2;
        double predY = target.field_70163_u + (target.field_70163_u - target.field_70137_T) * 0.2;
        double predZ = target.field_70161_v + (target.field_70161_v - target.field_70136_U) * 0.2;
        double aimX = predX + hOffsetX;
        double aimY = predY + baseY - target.field_70163_u + vOffset;
        double aimZ = predZ + hOffsetZ;
        double margin = 0.05;
        aimX = Math.max(bb.field_72340_a + margin, Math.min(bb.field_72336_d - margin, aimX));
        aimY = Math.max(bb.field_72338_b + margin, Math.min(bb.field_72337_e - margin, aimY));
        aimZ = Math.max(bb.field_72339_c + margin, Math.min(bb.field_72334_f - margin, aimZ));
        return new Vec3(aimX, aimY, aimZ);
    }

    private double improvedNoise(double seed, double x, double y) {
        double value = Math.sin(seed * 127.1 + x * 311.7 + y * 74.7) * 43758.5453;
        value -= Math.floor(value);
        return value * 2.0 - 1.0;
    }

    @Override
    public int hashCode() {
        return "INTAVE".hashCode();
    }
}
