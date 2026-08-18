package miau.module.modules.movement;

import miau.event.EventTarget;
import miau.event.impl.JumpEvent;
import miau.module.Module;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.util.world.BlockUtil;
import net.minecraft.block.BlockSlime;
import net.minecraft.client.Minecraft;

public class SlimeJump extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final FloatProperty motion = new FloatProperty("Motion", 0.42F, 0.2F, 1.0F);
    public final ModeProperty mode = new ModeProperty("Mode", 1, new String[]{"Set", "Add"});

    public SlimeJump() {
        super("SlimeJump", false);
    }

    @EventTarget
    public void onJump(JumpEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            if (BlockUtil.getBlock(mc.field_71439_g.func_180425_c().func_177977_b()) instanceof BlockSlime) {
                if (this.mode.getModeString().equalsIgnoreCase("set")) {
                    event.setJumpoff(this.motion.getValue());
                } else {
                    mc.field_71439_g.field_70181_x = 0.0;
                    event.setJumpoff(this.motion.getValue());
                }
            }
        }
    }
}
