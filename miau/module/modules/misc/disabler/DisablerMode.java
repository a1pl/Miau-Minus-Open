package miau.module.modules.misc.disabler;

import miau.event.impl.JumpEvent;
import miau.event.impl.LivingUpdateEvent;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.StrafeEvent;
import miau.event.impl.TickEvent;
import miau.module.modules.misc.Disabler;
import net.minecraft.client.Minecraft;

public abstract class DisablerMode {
    protected static final Minecraft mc = Minecraft.func_71410_x();
    protected final String name;
    protected final Disabler parent;

    public DisablerMode(String name, Disabler parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return this.name;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick(TickEvent event) {
    }

    public void onPacket(PacketEvent event) {
    }

    public void onStrafe(StrafeEvent event) {
    }

    public void onLivingUpdate(LivingUpdateEvent event) {
    }

    public void onMoveInput(MoveInputEvent event) {
    }

    public void onJump(JumpEvent event) {
    }

    public void onRender2D(Render2DEvent event) {
    }

    public void onLoadWorld(LoadWorldEvent event) {
    }
}
