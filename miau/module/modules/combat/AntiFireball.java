package miau.module.modules.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.management.RotationState;
import miau.module.Module;
import miau.module.modules.render.HUD;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.network.PacketUtil;
import miau.util.player.ItemUtil;
import miau.util.player.MoveUtil;
import miau.util.player.PlayerUtil;
import miau.util.player.RotationUtil;
import miau.util.player.TeamUtil;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;

public class AntiFireball extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final ArrayList<EntityFireball> farList = new ArrayList<>();
    private final ArrayList<EntityFireball> nearList = new ArrayList<>();
    private EntityFireball target = null;
    public final FloatProperty range = new FloatProperty("range", 5.0F, 3.0F, 8.0F);
    public final IntProperty fov = new IntProperty("fov", 360, 1, 360);
    public final BooleanProperty rotations = new BooleanProperty("rotations", true);
    public final BooleanProperty swing = new BooleanProperty("swing", true);
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"});
    public final ModeProperty showTarget = new ModeProperty("show-target", 0, new String[]{"NONE", "DEFAULT", "HUD"});

    private boolean isValidTarget(EntityFireball entityFireball) {
        return !entityFireball.func_174813_aQ().func_181656_b()
            && RotationUtil.distanceToEntity(entityFireball) <= this.range.getValue().floatValue() + 3.0
            && RotationUtil.angleToEntity(entityFireball) <= this.fov.getValue().intValue();
    }

    private void doAttackAnimation() {
        if (this.swing.getValue()) {
            mc.field_71439_g.func_71038_i();
        } else {
            PacketUtil.sendPacket(new C0APacketAnimation());
        }
    }

    public AntiFireball() {
        super("AntiFireball", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            List<EntityFireball> fireballs = mc.field_71441_e
                .field_72996_f
                .stream()
                .filter(entity -> entity instanceof EntityFireball)
                .map(entity -> (EntityFireball)entity)
                .collect(Collectors.toList());
            this.farList.removeIf(entityFireball -> !fireballs.contains(entityFireball));
            this.nearList.removeIf(entityFireball -> !fireballs.contains(entityFireball));

            for (EntityFireball fireball : fireballs) {
                if (!this.farList.contains(fireball) && !this.nearList.contains(fireball)) {
                    if (RotationUtil.distanceToEntity(fireball) > 3.0) {
                        this.farList.add(fireball);
                    } else {
                        this.nearList.add(fireball);
                    }
                }
            }

            if (mc.field_71439_g.field_71075_bZ.field_75101_c) {
                this.target = null;
            } else {
                this.target = this.farList
                    .stream()
                    .filter(this::isValidTarget)
                    .min(Comparator.comparingDouble(RotationUtil::distanceToEntity))
                    .orElse(null);
            }
        }
    }

    @EventTarget(4)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            EntityFireball fireball = this.target;
            if (TeamUtil.isEntityLoaded(fireball)) {
                float[] rotations = RotationUtil.getRotationsToBox(
                    this.target.func_174813_aQ(), event.getYaw(), event.getPitch(), 180.0F, 0.0F
                );
                if (this.rotations.getValue()
                    && !ItemUtil.isHoldingNonEmpty()
                    && !ItemUtil.isUsingBow()
                    && !ItemUtil.hasHoldItem()) {
                    event.setRotation(rotations[0], rotations[1], 0);
                    event.setPervRotation(
                        this.moveFix.getValue() != 0 ? rotations[0] : mc.field_71439_g.field_70177_z, 0
                    );
                }

                if (!Miau.playerStateManager.attacking
                    && !Miau.playerStateManager.digging
                    && !Miau.playerStateManager.placing) {
                    this.doAttackAnimation();
                    if (RotationUtil.distanceToEntity(this.target) <= this.range.getValue().floatValue()) {
                        PacketUtil.sendPacket(new C02PacketUseEntity(this.target, Action.ATTACK));
                        PlayerUtil.attackEntity(this.target);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (this.isEnabled()
            && this.moveFix.getValue() == 1
            && RotationState.isActived()
            && RotationState.getPriority() == 0.0F
            && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled() && this.showTarget.getValue() != 0 && TeamUtil.isEntityLoaded(this.target)) {
            Color color = new Color(-1);
            switch (this.showTarget.getValue()) {
                case 1:
                    double dist = (this.target.field_70165_t - this.target.field_70142_S)
                            * (mc.field_71439_g.field_70165_t - this.target.field_70165_t)
                        + (this.target.field_70163_u - this.target.field_70137_T)
                            * (
                                mc.field_71439_g.field_70163_u
                                    + mc.field_71439_g.func_70047_e()
                                    - this.target.field_70163_u
                                    - this.target.field_70131_O / 2.0
                            )
                        + (this.target.field_70161_v - this.target.field_70136_U)
                            * (mc.field_71439_g.field_70161_v - this.target.field_70161_v);
                    if (dist < 0.0) {
                        color = new Color(16733525);
                    } else {
                        color = new Color(5635925);
                    }
                    break;
                case 2:
                    color = ((HUD)Miau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
            }

            RenderUtil.enableRenderState();
            RenderUtil.drawEntityBox(this.target, color.getRed(), color.getGreen(), color.getBlue());
            RenderUtil.disableRenderState();
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.farList.clear();
        this.nearList.clear();
    }
}
