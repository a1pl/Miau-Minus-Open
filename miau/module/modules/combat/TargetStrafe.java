package miau.module.modules.combat;

import java.awt.Color;
import java.util.ArrayList;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.Render3DEvent;
import miau.event.impl.StrafeEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.module.modules.movement.Fly;
import miau.module.modules.movement.LongJump;
import miau.module.modules.movement.Speed;
import miau.module.modules.render.HUD;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.MoveUtil;
import miau.util.player.PlayerUtil;
import miau.util.player.RotationUtil;
import miau.util.player.TeamUtil;
import miau.util.render.ColorUtil;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

public class TargetStrafe extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private EntityLivingBase target = null;
    private float targetYaw = Float.NaN;
    private int direction = 1;
    public final FloatProperty radius = new FloatProperty("radius", 1.0F, 0.0F, 6.0F);
    public final IntProperty points = new IntProperty("points", 6, 3, 24);
    public final BooleanProperty requirePress = new BooleanProperty("require-press", true);
    public final BooleanProperty speedOnly = new BooleanProperty("speed-only", true);
    public final ModeProperty showTarget = new ModeProperty("show-target", 1, new String[]{"NONE", "DEFAULT", "HUD"});

    private boolean canStrafe() {
        if (this.speedOnly.getValue()) {
            Speed speed = (Speed)Miau.moduleManager.modules.get(Speed.class);
            Fly fly = (Fly)Miau.moduleManager.modules.get(Fly.class);
            LongJump longJump = (LongJump)Miau.moduleManager.modules.get(LongJump.class);
            if (!speed.isEnabled() && !fly.isEnabled() && (!longJump.isEnabled() || !longJump.isJumping())) {
                return false;
            }
        }

        return !this.requirePress.getValue() || PlayerUtil.isJumping();
    }

    private EntityLivingBase getKillAuraTarget() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        if (killAura.isEnabled() && killAura.isAttackAllowed()) {
            EntityLivingBase entityLivingBase = killAura.getTarget();
            return !TeamUtil.isEntityLoaded(entityLivingBase) ? null : entityLivingBase;
        } else {
            return null;
        }
    }

    private Color getTargetColor(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            if (TeamUtil.isFriend((EntityPlayer)entityLivingBase)) {
                return Miau.friendManager.getColor();
            }

            if (TeamUtil.isTarget((EntityPlayer)entityLivingBase)) {
                return Miau.targetManager.getColor();
            }
        }

        switch (this.showTarget.getValue()) {
            case 1:
                if (!(entityLivingBase instanceof EntityPlayer)) {
                    return Color.WHITE;
                }

                return TeamUtil.getTeamColor((EntityPlayer)entityLivingBase, 1.0F);
            case 2:
                int color = ((HUD)Miau.moduleManager.modules.get(HUD.class))
                    .getColor(System.currentTimeMillis())
                    .getRGB();
                return new Color(color);
            default:
                return new Color(-1);
        }
    }

    private boolean isInWater(double x, double z) {
        return PlayerUtil.checkInWater(
            new AxisAlignedBB(
                x - 0.015,
                mc.field_71439_g.field_70163_u,
                z - 0.015,
                x + 0.015,
                mc.field_71439_g.field_70163_u + mc.field_71439_g.field_70131_O,
                z + 0.015
            )
        );
    }

    private int wrapIndex(int index, int size) {
        if (index < 0) {
            return size - 1;
        } else {
            return index >= size ? 0 : index;
        }
    }

    public TargetStrafe() {
        super("TargetStrafe", false);
    }

    public float getTargetYaw() {
        return this.targetYaw;
    }

    @EventTarget(0)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            boolean left = PlayerUtil.isMovingLeft();
            boolean right = PlayerUtil.isMovingRight();
            if (left ^ right) {
                this.direction = left ? 1 : -1;
            }

            if (!this.canStrafe()) {
                this.target = null;
                this.targetYaw = Float.NaN;
            } else {
                this.target = this.getKillAuraTarget();
                if (this.target == null) {
                    this.targetYaw = Float.NaN;
                } else {
                    ArrayList<TargetStrafe.Vec2d> vpositions = new ArrayList<>();

                    for (int i = 0; i < this.points.getValue(); i++) {
                        vpositions.add(
                            new TargetStrafe.Vec2d(
                                this.radius.getValue().floatValue()
                                    * Math.cos(i * ((Math.PI * 2) / this.points.getValue().intValue())),
                                this.radius.getValue().floatValue()
                                    * Math.sin(i * ((Math.PI * 2) / this.points.getValue().intValue()))
                            )
                        );
                    }

                    if (vpositions.isEmpty()) {
                        this.target = null;
                        this.targetYaw = Float.NaN;
                    } else {
                        double closestDistance = 0.0;
                        int closestIndex = -1;

                        for (int i = 0; i < vpositions.size(); i++) {
                            double distance = mc.field_71439_g
                                .func_70011_f(
                                    this.target.field_70165_t + vpositions.get(i).getX(),
                                    mc.field_71439_g.field_70163_u,
                                    this.target.field_70161_v + vpositions.get(i).getY()
                                );
                            if (closestIndex == -1 || distance < closestDistance) {
                                closestDistance = distance;
                                closestIndex = i;
                            }
                        }

                        if (mc.field_71439_g.field_70123_F) {
                            this.direction *= -1;
                        }

                        int nextIndex = closestIndex + this.direction;
                        nextIndex = this.wrapIndex(nextIndex, vpositions.size());
                        double nextX = this.target.field_70165_t + vpositions.get(nextIndex).getX();
                        double nextZ = this.target.field_70161_v + vpositions.get(nextIndex).getY();
                        if (this.isInWater(nextX, nextZ)) {
                            this.direction *= -1;
                            nextIndex = closestIndex + this.direction;
                            nextIndex = this.wrapIndex(nextIndex, vpositions.size());
                            nextX = this.target.field_70165_t + vpositions.get(nextIndex).getX();
                            nextZ = this.target.field_70161_v + vpositions.get(nextIndex).getY();
                        }

                        double deltaX = nextX - mc.field_71439_g.field_70165_t;
                        double deltaZ = nextZ - mc.field_71439_g.field_70161_v;
                        float currentPitch = event.getPitch();
                        float currentYaw = event.getYaw();
                        double deltaY = 0.0;
                        this.targetYaw = RotationUtil.getRotationsTo(deltaX, deltaY, deltaZ, currentYaw, currentPitch)[0];
                        event.setPervRotation(this.targetYaw, 10);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled() && !Float.isNaN(this.targetYaw) && MoveUtil.isForwardPressed()) {
            event.setStrafe(0.0F);
            event.setForward(1.0F);
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled() && TeamUtil.isEntityLoaded(this.target) && this.showTarget.getValue() != 0) {
            Color color = this.getTargetColor(this.target);
            RenderUtil.enableRenderState();
            RenderUtil.drawEntityCircle(
                this.target,
                this.radius.getValue().floatValue(),
                this.points.getValue(),
                ColorUtil.darker(color, 0.2F).getRGB()
            );
            RenderUtil.drawEntityCircle(
                this.target, this.radius.getValue().floatValue(), this.points.getValue(), color.getRGB()
            );
            RenderUtil.disableRenderState();
        }
    }

    @Override
    public void onDisabled() {
        this.target = null;
        this.targetYaw = Float.NaN;
    }

    public static class Vec2d {
        private final double x;
        private final double y;

        public Vec2d(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }
    }
}
