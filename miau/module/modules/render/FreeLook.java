package miau.module.modules.render;

import miau.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MathHelper;

public class FreeLook extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private float cameraYaw;
    private float cameraPitch;
    private boolean active;

    public FreeLook() {
        super("FreeLook", false);
    }

    @Override
    public void onEnabled() {
        EntityPlayerSP player = mc.field_71439_g;
        if (player != null) {
            this.cameraYaw = player.field_70177_z;
            this.cameraPitch = player.field_70125_A;
            this.active = true;
            if (mc.field_71474_y.field_74320_O == 0) {
                mc.field_71474_y.field_74320_O = 1;
            }
        }
    }

    @Override
    public void onDisabled() {
        this.active = false;
        if (mc.field_71474_y.field_74320_O != 0) {
            mc.field_71474_y.field_74320_O = 0;
        }
    }

    public boolean isFreeLooking() {
        return this.isEnabled() && this.active && mc.field_71439_g != null;
    }

    public void updateCamera(float yawDelta, float pitchDelta) {
        this.cameraYaw += yawDelta * 0.15F;
        this.cameraPitch = MathHelper.func_76131_a(this.cameraPitch - pitchDelta * 0.15F, -90.0F, 90.0F);
    }

    public float getCameraYaw() {
        return this.cameraYaw;
    }

    public float getCameraPitch() {
        return this.cameraPitch;
    }
}
