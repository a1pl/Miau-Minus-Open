package miau.module.modules.ghost;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import miau.event.EventTarget;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.client.KeyBindUtil;
import miau.util.player.ItemUtil;
import miau.util.player.PlayerUtil;
import miau.util.player.RotationUtil;
import miau.util.player.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class AimAssist extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final Random random = new Random();
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"NORMAL"});
    public final IntProperty speed = new IntProperty("speed", 10, 1, 30);
    public final FloatProperty multipointHorizontal = new FloatProperty("multipoint-horizontal", 0.0F, 0.0F, 100.0F);
    public final FloatProperty multipointVertical = new FloatProperty("multipoint-vertical", 0.0F, 0.0F, 100.0F);
    public final FloatProperty randomization = new FloatProperty("randomization", 50.0F, 0.0F, 100.0F);
    public final FloatProperty fov = new FloatProperty("fov", 90.0F, 15.0F, 360.0F);
    public final FloatProperty range = new FloatProperty("range", 4.5F, 0.0F, 5.0F);
    public final ModeProperty sortMode = new ModeProperty(
        "sort", 1, new String[]{"HEALTH", "ANGLE", "HURT_TIME", "DISTANCE"}
    );
    public final BooleanProperty ignoreBehindWalls = new BooleanProperty("ignore-behind-walls", false);
    public final BooleanProperty ignoreBehindEntities = new BooleanProperty("ignore-behind-entities", false);
    public final BooleanProperty aimInvis = new BooleanProperty("aim-invis", false);
    public final BooleanProperty clickAim = new BooleanProperty("require-mouse", true);
    public final BooleanProperty ignoreTeammates = new BooleanProperty("ignore-teammates", true);
    public final BooleanProperty stopWhenBreaking = new BooleanProperty("stop-when-breaking", false);
    public final BooleanProperty keepMoveDirection = new BooleanProperty("keep-move-direction", true);
    public final IntProperty hoverDelay = new IntProperty("hover-delay", 100, 0, 500);
    public final BooleanProperty weaponOnly = new BooleanProperty("weapon-only", false);
    private long miningStartTime = -1L;

    public AimAssist() {
        super("AimAssist", false);
    }

    @Override
    public void onDisabled() {
        this.miningStartTime = -1L;
        super.onDisabled();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.POST) {
            if (this.mode.getValue() == 0 && this.conditionsMet()) {
                EntityPlayer target = this.getEnemy();
                if (target != null) {
                    boolean allowThroughBlocks = !this.ignoreBehindWalls.getValue();
                    boolean allowThroughEntities = !this.ignoreBehindEntities.getValue();
                    float[] rot = RotationUtil.getRotationsWithBackup(
                        target,
                        this.multipointHorizontal.getValue().floatValue(),
                        this.multipointVertical.getValue().floatValue(),
                        mc.field_71439_g.field_70177_z,
                        mc.field_71439_g.field_70125_A,
                        this.range.getValue().floatValue(),
                        allowThroughBlocks,
                        allowThroughEntities
                    );
                    if (rot != null) {
                        float[] smooth = RotationUtil.smoothRotation(
                            mc.field_71439_g.field_70177_z,
                            mc.field_71439_g.field_70125_A,
                            rot[0],
                            rot[1],
                            this.speed.getValue(),
                            this.randomization.getValue()
                        );
                        mc.field_71439_g.field_70177_z = smooth[0];
                        mc.field_71439_g.field_70125_A = smooth[1];
                    }
                }
            }
        }
    }

    private EntityPlayer getEnemy() {
        float viewYaw = mc.field_71439_g.field_70177_z;
        List<EntityPlayer> candidates = new ArrayList<>();

        for (Entity player : mc.field_71441_e.field_73010_i) {
            if (player instanceof EntityPlayer) {
                EntityPlayer entityPlayer = (EntityPlayer)player;
                if (entityPlayer != mc.field_71439_g
                    && entityPlayer.field_70725_aQ == 0
                    && !TeamUtil.isFriend(entityPlayer)
                    && (!this.ignoreTeammates.getValue() || !TeamUtil.isSameTeam(entityPlayer))
                    && (this.aimInvis.getValue() || !entityPlayer.func_82150_aj())
                    && !(
                        RotationUtil.distanceSqFromEyeToClosestOnAABB(entityPlayer)
                            > this.range.getValue() * this.range.getValue()
                    )
                    && !TeamUtil.isBot(entityPlayer)) {
                    if (this.fov.getValue() != 360.0F) {
                        double deltaX = entityPlayer.field_70165_t - mc.field_71439_g.field_70165_t;
                        double deltaZ = entityPlayer.field_70161_v - mc.field_71439_g.field_70161_v;
                        float targetYaw = (float)(Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0F;
                        float diff = Math.abs(MathHelper.func_76142_g(targetYaw - viewYaw));
                        if (diff > this.fov.getValue() / 2.0F) {
                            continue;
                        }
                    }

                    candidates.add(entityPlayer);
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        Comparator<EntityPlayer> primary;
        switch (this.sortMode.getValue()) {
            case 0:
                primary = Comparator.comparingDouble(p -> p.func_110143_aJ() + p.func_110139_bj());
                break;
            case 1:
                primary = Comparator.comparingDouble(p -> {
                    float[] rots = RotationUtil.getRotations(p);
                    double yawDelta = Math.abs(MathHelper.func_76142_g(rots[0] - viewYaw));
                    double pitchDelta = Math.abs(MathHelper.func_76142_g(rots[1] - mc.field_71439_g.field_70125_A));
                    return yawDelta + pitchDelta;
                });
                break;
            case 2:
                primary = Comparator.comparingInt(p -> p.field_70737_aN);
                break;
            case 3:
            default:
                primary = Comparator.comparingDouble(p -> mc.field_71439_g.func_70068_e(p));
        }

        candidates.sort(primary.thenComparingDouble(p -> mc.field_71439_g.func_70068_e(p)));
        if (!this.ignoreBehindWalls.getValue() && !this.ignoreBehindEntities.getValue()) {
            return candidates.get(0);
        }

        boolean allowThroughBlocks = !this.ignoreBehindWalls.getValue();
        boolean allowThroughEntities = !this.ignoreBehindEntities.getValue();

        for (EntityPlayer candidate : candidates) {
            if (RotationUtil.hasValidAimPoint(
                candidate,
                this.multipointHorizontal.getValue().floatValue(),
                this.multipointVertical.getValue().floatValue(),
                this.range.getValue().floatValue(),
                allowThroughBlocks,
                allowThroughEntities
            )) {
                return candidate;
            }
        }

        return null;
    }

    private boolean conditionsMet() {
        if (mc.field_71462_r != null || !mc.field_71415_G) {
            return false;
        }

        if (this.weaponOnly.getValue() && !ItemUtil.isHoldingSword()) {
            return false;
        }

        if (this.clickAim.getValue() && !KeyBindUtil.isKeyDown(mc.field_71474_y.field_74312_F.func_151463_i())) {
            return false;
        }

        if (this.stopWhenBreaking.getValue()
            && PlayerUtil.isAttacking()
            && mc.field_71476_x != null
            && mc.field_71476_x.field_72313_a == MovingObjectType.BLOCK) {
            if (this.miningStartTime == -1L) {
                this.miningStartTime = System.currentTimeMillis();
            }

            long elapsed = System.currentTimeMillis() - this.miningStartTime;
            if (elapsed >= this.hoverDelay.getValue().intValue()) {
                return false;
            }
        } else {
            this.miningStartTime = -1L;
        }

        return true;
    }
}
