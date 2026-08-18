package miau.module.modules.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.Render3DEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.module.modules.combat.KillAura;
import miau.module.modules.render.targethud.ExhibitionMode;
import miau.module.modules.render.targethud.IdleoMode;
import miau.module.modules.render.targethud.MyauMode;
import miau.module.modules.render.targethud.RavenMode;
import miau.module.modules.render.targethud.TargetHUDMode;
import miau.module.modules.render.targethud.TirumMode;
import miau.property.properties.BooleanProperty;
import miau.property.properties.DragProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.PercentProperty;
import miau.ui.clickgui.ClickGui;
import miau.util.player.TeamUtil;
import miau.util.render.RenderUtil;
import miau.util.time.TimerUtil;
import miau.util.vector.Vector2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class TargetHUD extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final TimerUtil lastAttackTimer = new TimerUtil();
    private EntityLivingBase lastTarget = null;
    private EntityLivingBase target = null;
    public EntityLivingBase renderEntity;
    public static final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);
    public static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
    public static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    public TargetHUDMode[] targetHUDModes;
    public final ModeProperty mode = new ModeProperty(
        "Mode", 0, new String[]{"Raven", "Myau", "Exhibition", "Idleo", "Tirum"}
    );
    public final ModeProperty ravenMode = new ModeProperty(
        "Raven Mode", 0, new String[]{"Modern", "Legacy"}, () -> this.mode.getValue() == 0
    );
    public final ModeProperty color = new ModeProperty(
        "Color", 0, new String[]{"DEFAULT", "HUD"}, () -> this.mode.getValue() == 1
    );
    public final FloatProperty scale = new FloatProperty("Scale", 1.0F, 0.5F, 1.5F);
    public final BooleanProperty shadow = new BooleanProperty("Shadow", true);
    public final DragProperty drag = new DragProperty("Position", new Vector2d(70.0, 30.0));
    public final BooleanProperty showStatus;
    public final BooleanProperty healthColor;
    public final BooleanProperty renderEsp;
    public final PercentProperty background;
    public final BooleanProperty head;
    public final BooleanProperty indicator;
    public final BooleanProperty outline;
    public final BooleanProperty animations;
    public final BooleanProperty kaOnly;
    public final BooleanProperty chatPreview;

    public TargetHUD() {
        super("TargetHUD", false, true);
        this.drag.render = true;
        this.showStatus = new BooleanProperty("Show win or loss", true);
        this.healthColor = new BooleanProperty("Traditional health color", false);
        this.renderEsp = new BooleanProperty("Render ESP", true);
        this.background = new PercentProperty("Background", 25, () -> this.mode.getValue() == 1);
        this.head = new BooleanProperty("Head", true, () -> this.mode.getValue() == 1);
        this.indicator = new BooleanProperty("Indicator", true, () -> this.mode.getValue() == 1);
        this.outline = new BooleanProperty("Outline", false, () -> this.mode.getValue() == 1);
        this.animations = new BooleanProperty("Animations", true, () -> this.mode.getValue() == 1);
        this.kaOnly = new BooleanProperty("KA only", true);
        this.chatPreview = new BooleanProperty("Chat preview", false);
        this.targetHUDModes = new TargetHUDMode[]{
            new RavenMode(this), new MyauMode(this), new ExhibitionMode(this), new IdleoMode(this), new TirumMode(this)
        };
    }

    @Override
    public void onDisabled() {
        this.target = null;
        this.lastTarget = null;
        if (this.targetHUDModes != null) {
            ((RavenMode)this.targetHUDModes[0]).reset();
            ((MyauMode)this.targetHUDModes[1]).reset();
        }
    }

    @Override
    public void onEnabled() {
        if (this.targetHUDModes != null) {
            ((RavenMode)this.targetHUDModes[0]).reset();
            ((MyauMode)this.targetHUDModes[1]).reset();
        }
    }

    private EntityLivingBase resolveTarget() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        if (killAura.isEnabled() && killAura.isAttackAllowed() && TeamUtil.isEntityLoaded(killAura.getTarget())) {
            return killAura.getTarget();
        } else if (!this.kaOnly.getValue()
            && !this.lastAttackTimer.hasTimeElapsed(1500L)
            && TeamUtil.isEntityLoaded(this.lastTarget)) {
            return this.lastTarget;
        } else {
            return !this.chatPreview.getValue()
                    && !(mc.field_71462_r instanceof GuiChat)
                    && !(mc.field_71462_r instanceof ClickGui)
                ? null
                : mc.field_71439_g;
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            int modeVal = this.mode.getValue();
            boolean showChatPreview = this.chatPreview.getValue() && mc.field_71462_r instanceof GuiChat;
            if (modeVal == 1) {
                this.target = this.resolveTarget();
                if (this.target != null) {
                    this.targetHUDModes[1].render(this.target, 0.0F, 0.0F);
                }
            } else if (modeVal >= 2 && modeVal < 4) {
                KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
                if (killAura == null) {
                    return;
                }

                EntityLivingBase killTarget = killAura.getTarget();
                if (killTarget != null && killAura.isEnabled()) {
                    this.target = killTarget;
                } else {
                    if (!showChatPreview) {
                        return;
                    }

                    this.target = mc.field_71439_g;
                }

                if (this.target instanceof EntityPlayer) {
                    float x = (float)this.drag.position.x;
                    float y = (float)this.drag.position.y;
                    this.targetHUDModes[modeVal].render(this.target, x, y);
                }
            } else if (modeVal == 4) {
                this.target = this.resolveTarget();
                if (this.target != null && !this.target.field_70128_L && !this.target.func_82150_aj()) {
                    this.targetHUDModes[4].render(this.target, 0.0F, 0.0F);
                }
            } else {
                KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
                if (killAura == null) {
                    return;
                }

                RavenMode raven = (RavenMode)this.targetHUDModes[0];
                EntityLivingBase killTarget = killAura.getTarget();
                if (killTarget != null && killAura.isEnabled()) {
                    this.target = killTarget;
                    raven.setLastAliveMS(System.currentTimeMillis());
                    raven.setFadeTimer(null);
                } else if (showChatPreview) {
                    this.target = mc.field_71439_g;
                    raven.setLastAliveMS(System.currentTimeMillis());
                    raven.setFadeTimer(null);
                } else {
                    if (this.target == null) {
                        return;
                    }

                    if (System.currentTimeMillis() - raven.getLastAliveMS() >= 400L && raven.getFadeTimer() == null) {
                        TimerUtil ft = new TimerUtil();
                        ft.reset();
                        raven.setFadeTimer(ft);
                    }
                }

                double health = this.target.func_110143_aJ() / this.target.func_110138_aP();
                if (this.target.field_70128_L) {
                    health = 0.0;
                }

                if (health != raven.getLastHealth()) {
                    TimerUtil ht = new TimerUtil();
                    ht.reset();
                    raven.setHealthBarTimer(ht);
                }

                raven.setLastHealth(health);
                raven.render(this.target, 0.0F, 0.0F);
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        GL11.glGetFloat(2982, MODELVIEW);
        GL11.glGetFloat(2983, PROJECTION);
        GL11.glGetInteger(2978, VIEWPORT);
        if (this.renderEsp.getValue() && this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
            if (killAura != null) {
                if (killAura.showTarget.getValue() == 0) {
                    EntityLivingBase espTarget = killAura.getTarget();
                    if (espTarget != null && killAura.isEnabled()) {
                        RenderUtil.renderEntity(espTarget, 2, 0.0, 0.0, -1, false);
                    } else if (this.renderEntity != null) {
                        RenderUtil.renderEntity(this.renderEntity, 2, 0.0, 0.0, -1, false);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.SEND && event.getPacket() instanceof C02PacketUseEntity) {
                C02PacketUseEntity packet = (C02PacketUseEntity)event.getPacket();
                if (packet.func_149565_c() != Action.ATTACK) {
                    return;
                }

                Entity entity = packet.func_149564_a(mc.field_71441_e);
                if (entity instanceof EntityLivingBase) {
                    if (entity instanceof EntityArmorStand) {
                        return;
                    }

                    this.lastAttackTimer.reset();
                    this.lastTarget = (EntityLivingBase)entity;
                }
            }
        }
    }
}
