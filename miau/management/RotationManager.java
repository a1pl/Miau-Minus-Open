package miau.management;

import miau.event.EventTarget;
import miau.event.impl.Render3DEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public class RotationManager {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private float lastUpdate = Float.NaN;
    private float yawDelta = Float.NaN;
    private float pitchDelta = Float.NaN;
    private int priority = Integer.MIN_VALUE;
    private boolean rotated = false;
    private boolean silentMode = false;
    private float silentYaw;
    private float silentPitch;
    private boolean silentActive = false;

    public void setSilentRotation(float yaw, float pitch, int priority) {
        if (this.priority <= priority) {
            this.silentMode = true;
            this.silentYaw = yaw;
            this.silentPitch = pitch;
            this.silentActive = true;
            RotationState.applyState(true, yaw, pitch, yaw, priority);
            this.rotated = true;
        }
    }

    public void setRotation(float yaw, float pitch, int priority, boolean force) {
        if (this.priority <= priority) {
            this.silentMode = false;
            this.priority = priority;
            this.yawDelta = MathHelper.func_76142_g(yaw - mc.field_71439_g.field_70177_z);
            this.pitchDelta = MathHelper.func_76131_a(pitch - mc.field_71439_g.field_70125_A, -90.0F, 90.0F);
            this.lastUpdate = 0.0F;
            this.rotated = force;
            this.applyRotation(0.0F);
        }
    }

    public boolean isRotated() {
        return this.rotated;
    }

    public boolean isSilentActive() {
        return this.silentActive;
    }

    public float getSilentYaw() {
        return this.silentYaw;
    }

    public float getSilentPitch() {
        return this.silentPitch;
    }

    private void applyRotation(float partialTicks) {
        if (!Float.isNaN(this.lastUpdate) && !Float.isNaN(this.yawDelta) && !Float.isNaN(this.pitchDelta)) {
            if (mc.field_71439_g != null
                && !Float.isNaN(this.yawDelta)
                && !Float.isNaN(this.pitchDelta)
                && !Float.isNaN(this.lastUpdate)) {
                float yaw = this.yawDelta * (partialTicks - this.lastUpdate);
                if (yaw != 0.0F) {
                    mc.field_71439_g.field_70126_B = mc.field_71439_g.field_70177_z;
                    mc.field_71439_g.field_70177_z += yaw;
                }

                float pitch = this.pitchDelta * (partialTicks - this.lastUpdate);
                if (pitch != 0.0F) {
                    mc.field_71439_g.field_70127_C = mc.field_71439_g.field_70125_A;
                    mc.field_71439_g.field_70125_A += pitch;
                    mc.field_71439_g.field_70125_A = MathHelper.func_76131_a(
                        mc.field_71439_g.field_70125_A, -90.0F, 90.0F
                    );
                }

                this.lastUpdate = partialTicks;
            }
        }
    }

    private void resetRotationState() {
        this.lastUpdate = Float.NaN;
        this.yawDelta = Float.NaN;
        this.pitchDelta = Float.NaN;
        this.priority = Integer.MIN_VALUE;
        this.rotated = false;
        this.silentMode = false;
        this.silentActive = false;
    }

    @EventTarget(0)
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (!this.silentMode) {
                this.applyRotation(1.0F);
            }

            this.resetRotationState();
        }
    }

    @EventTarget(0)
    public void onRender3D(Render3DEvent event) {
        if (!this.silentMode) {
            this.applyRotation(event.getPartialTicks());
        }
    }
}
