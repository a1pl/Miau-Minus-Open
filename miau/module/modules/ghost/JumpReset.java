package miau.module.modules.ghost;

import miau.event.EventTarget;
import miau.event.impl.KnockbackEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.PercentProperty;
import miau.util.client.KeyBindUtil;
import miau.util.player.RayCastUtil;
import miau.util.player.SimulatedPlayer;
import miau.util.player.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class JumpReset extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private boolean setJump;
    private boolean ignoreNext;
    private boolean aiming;
    private int lastHurtTime;
    private double lastFallDistance;
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"STANDARD", "POLAR"});
    public final PercentProperty chance = new PercentProperty("chance", 100);
    public final BooleanProperty mouseDown = new BooleanProperty("mouse-down", false);
    public final BooleanProperty movingForward = new BooleanProperty("moving-forward", true);
    public final BooleanProperty aimingOnPlayer = new BooleanProperty("aiming-on-player", true);
    public final FloatProperty exitRange = new FloatProperty(
        "exit-range", 3.0F, 2.0F, 6.0F, () -> this.mode.getValue() == 1
    );
    public final IntProperty predictionTicks = new IntProperty(
        "prediction-ticks", 2, 0, 5, () -> this.mode.getValue() == 1
    );

    public JumpReset() {
        super("JumpReset", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.mode.getValue() != 1) {
            if (event.getType() == EventType.PRE) {
                int hurtTime = mc.field_71439_g.field_70737_aN;
                boolean onGround = mc.field_71439_g.field_70122_E;
                if (onGround && this.lastFallDistance > 3.0 && !mc.field_71439_g.field_71075_bZ.field_75101_c) {
                    this.ignoreNext = true;
                }

                if (hurtTime > this.lastHurtTime) {
                    boolean mouseDownCheck = KeyBindUtil.isKeyDown(-100) || !this.mouseDown.getValue();
                    boolean aimingCheck = this.aiming || !this.aimingOnPlayer.getValue();
                    boolean forwardCheck = KeyBindUtil.isKeyDown(mc.field_71474_y.field_74351_w.func_151463_i())
                        || !this.movingForward.getValue();
                    if (!this.ignoreNext
                        && onGround
                        && aimingCheck
                        && forwardCheck
                        && mouseDownCheck
                        && !mc.field_71439_g.func_70027_ad()
                        && Math.random() * 100.0 < this.chance.getValue().intValue()
                        && !this.hasBadEffect()) {
                        this.setJump = true;
                        KeyBindUtil.setKeyBindState(mc.field_71474_y.field_74314_A.func_151463_i(), true);
                    }

                    this.ignoreNext = false;
                }

                this.lastHurtTime = hurtTime;
                this.lastFallDistance = mc.field_71439_g.field_70143_R;
            } else if (event.getType() == EventType.POST) {
                if (this.mode.getValue() == 1) {
                    return;
                }

                if (this.setJump && !KeyBindUtil.isKeyDown(mc.field_71474_y.field_74314_A.func_151463_i())) {
                    this.setJump = false;
                    KeyBindUtil.setKeyBindState(mc.field_71474_y.field_74314_A.func_151463_i(), false);
                }
            }
        }
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (this.isEnabled() && this.mode.getValue() == 1 && mc.field_71439_g.field_70122_E && event.getY() > 0.0) {
            KeyBindUtil.setKeyBindState(mc.field_71474_y.field_74314_A.func_151463_i(), true);
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled() && this.mode.getValue() == 1) {
            EntityLivingBase target = null;
            double closestDist = Double.MAX_VALUE;

            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (entity instanceof EntityPlayer
                    && entity != mc.field_71439_g
                    && !TeamUtil.isSameTeam((EntityPlayer)entity)
                    && !TeamUtil.isFriend((EntityPlayer)entity)) {
                    double dist = mc.field_71439_g.func_70032_d(entity);
                    if (dist <= 6.0 && dist < closestDist) {
                        closestDist = dist;
                        target = (EntityLivingBase)entity;
                    }
                }
            }

            if (target != null && this.shouldJump(target)) {
                KeyBindUtil.setKeyBindState(mc.field_71474_y.field_74314_A.func_151463_i(), true);
            }
        }
    }

    private boolean shouldJump(EntityLivingBase target) {
        SimulatedPlayer sim = SimulatedPlayer.fromClientPlayer(mc.field_71439_g.field_71158_b);
        int simHurtTime = mc.field_71439_g.field_70737_aN;
        int predTicks = this.predictionTicks.getValue();

        for (int i = 0; i < predTicks; i++) {
            sim.tick();
            if (simHurtTime > 0) {
                simHurtTime--;
            }
        }

        if (simHurtTime <= 0) {
            ItemStack targetHeld = target.func_70694_bm();
            int knockbackLevel = 0;
            if (targetHeld != null) {
                knockbackLevel = EnchantmentHelper.func_77506_a(Enchantment.field_180313_o.field_77352_x, targetHeld);
            }

            double kb = knockbackLevel + (target.func_70051_ag() ? 1.0 : 0.0);
            float yawHead = target.field_70759_as;
            sim.motionX = sim.motionX + -MathHelper.func_76126_a(yawHead * (float) Math.PI / 180.0F) * kb * 0.5;
            sim.motionZ = sim.motionZ + MathHelper.func_76134_b(yawHead * (float) Math.PI / 180.0F) * kb * 0.5;
            sim.motionY += 0.1;
        }

        double targetDeltaX = target.field_70165_t - target.field_70142_S;
        double targetDeltaY = target.field_70163_u - target.field_70137_T;
        double targetDeltaZ = target.field_70161_v - target.field_70136_U;
        double predTargetX = target.field_70165_t + targetDeltaX;
        double predTargetY = target.field_70163_u + targetDeltaY;
        double predTargetZ = target.field_70161_v + targetDeltaZ;
        double dx = sim.getPos().field_72450_a - predTargetX;
        double dy = sim.getPos().field_72448_b - predTargetY;
        double dz = sim.getPos().field_72449_c - predTargetZ;
        double predDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double currentDist = mc.field_71439_g.func_70032_d(target);
        double exitRangeVal = this.exitRange.getValue().floatValue();
        return mc.field_71439_g.func_70051_ag() && predDist > exitRangeVal && currentDist <= exitRangeVal;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.mode.getValue() != 1) {
            if (event.getType() == EventType.SEND) {
                if (event.getPacket() instanceof C03PacketPlayer) {
                    C03PacketPlayer packet = (C03PacketPlayer)event.getPacket();
                    float yaw;
                    float pitch;
                    if (packet instanceof C06PacketPlayerPosLook) {
                        yaw = ((C06PacketPlayerPosLook)packet).func_149462_g();
                        pitch = ((C06PacketPlayerPosLook)packet).func_149470_h();
                    } else {
                        if (!(packet instanceof C05PacketPlayerLook)) {
                            return;
                        }

                        yaw = ((C05PacketPlayerLook)packet).func_149462_g();
                        pitch = ((C05PacketPlayerLook)packet).func_149470_h();
                    }

                    MovingObjectPosition mop = RayCastUtil.rayCast(yaw, pitch, 5.0, 0.0F, mc.field_71439_g);
                    if (mop != null
                        && mop.field_72313_a == MovingObjectType.ENTITY
                        && mop.field_72308_g instanceof EntityOtherPlayerMP) {
                        this.aiming = true;
                    } else {
                        this.aiming = false;
                    }
                }
            }
        }
    }

    private boolean hasBadEffect() {
        for (PotionEffect effect : mc.field_71439_g.func_70651_bq()) {
            int potionId = effect.func_76456_a();
            if (potionId == Potion.field_76430_j.func_76396_c()
                || potionId == Potion.field_76436_u.func_76396_c()
                || potionId == Potion.field_82731_v.func_76396_c()) {
                return true;
            }
        }

        return false;
    }
}
