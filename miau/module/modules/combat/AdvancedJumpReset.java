package miau.module.modules.combat;

import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorEntity;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.math.RandomUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;

public class AdvancedJumpReset extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final ModeProperty jumpRate = new ModeProperty(
        "How to calculate jump rate", 1, new String[]{"Tick since last velocity", "Simple RNG", "Polar Safe RNG"}
    );
    private final IntProperty minTickSinceLastVL = new IntProperty(
        "Min ticks since last jump reset",
        3,
        0,
        20,
        () -> this.jumpRate.getModeString().equals("Tick since last velocity")
    );
    private final IntProperty simpleRNG = new IntProperty(
        "Simple RNG jump rate", 75, 0, 100, () -> this.jumpRate.getModeString().equals("Simple RNG")
    );
    private final ModeProperty pauseWhen = new ModeProperty(
        "Pause jump reset when", 0, new String[]{"Server flag packet received", "Not in combat", "Both"}
    );
    private final IntProperty serverLagTick = new IntProperty(
        "Min Tick since server lag packet received",
        3,
        0,
        20,
        () -> this.pauseWhen.getModeString().contains("Server flag packet received")
    );
    private final IntProperty notInCombatTick = new IntProperty(
        "Min Tick since not in combat", 3, 0, 20, () -> this.pauseWhen.getModeString().contains("Not in combat")
    );
    private final FloatProperty hurtTimeRange = new FloatProperty("Jump reset if hurt time in", 5.0F, 9.0F, 1.0F, 10.0F);
    private final ModeProperty howToJump = new ModeProperty(
        "How to jump", 0, new String[]{"Functional", "Legitimize", "Motion"}
    );
    private final FloatProperty motionHeight = new FloatProperty(
        "Motion height", 0.42F, 0.1F, 1.0F, () -> this.howToJump.getModeString().equals("Motion")
    );
    private final BooleanProperty reduce = new BooleanProperty("Reduce", false);
    private final ModeProperty reduceEvent = new ModeProperty(
        "Reduce when what happened", 1, new String[]{"Jumped", "Hurt time updated"}, () -> this.reduce.getValue()
    );
    private final ModeProperty reduceMode = new ModeProperty(
        "Reduce calculation", 0, new String[]{"Linear", "Smooth"}, () -> this.reduce.getValue()
    );
    private final FloatProperty reduceHurtTime = new FloatProperty(
        "Reduce hurt time by",
        1.0F,
        3.0F,
        1.0F,
        10.0F,
        () -> this.reduce.getValue() && this.reduceEvent.getModeString().equals("Hurt time updated")
    );
    private final FloatProperty reduceFactor = new FloatProperty(
        "Basic reduce factor", 0.6F, 0.0F, 1.0F, () -> this.reduce.getValue()
    );
    private final FloatProperty reduceFactorWhileHit = new FloatProperty(
        "Reduce factor while hitting", 0.6F, 0.0F, 1.0F, () -> this.reduce.getValue()
    );
    private final FloatProperty reduceFactorWhileSprint = new FloatProperty(
        "Reduce factor while sprinting", 0.6F, 0.0F, 1.0F, () -> this.reduce.getValue()
    );
    private final FloatProperty reduceFactorWhileHitSprint = new FloatProperty(
        "Reduce factor while hitting and sprinting", 0.6F, 0.0F, 1.0F, () -> this.reduce.getValue()
    );
    private final FloatProperty activeMotion = new FloatProperty("Active motion", 700.0F, 7000.0F, 300.0F, 32000.0F);
    private final BooleanProperty stopWhenBackward = new BooleanProperty("Stop when S Pressed", true);
    private final BooleanProperty stopWhenBlocking = new BooleanProperty("Stop when Blocking", true);
    private final BooleanProperty stopWhenSneaking = new BooleanProperty("Stop when Sneaking", true);
    private final BooleanProperty stopWhenFire = new BooleanProperty("Stop when on fire", true);
    private final BooleanProperty stopWhenSpeed = new BooleanProperty("Stop when Speed potion", false);
    private final BooleanProperty stopWhenJumpBoost = new BooleanProperty("Stop when Jump Boost", false);
    private final BooleanProperty stopWhenInInventory = new BooleanProperty("Stop when in inventory", true);
    private final BooleanProperty stopWhenBadSurrounding = new BooleanProperty("Stop when in bad surrounding", true);
    private final BooleanProperty stopWhenInAir = new BooleanProperty("Stop when in air", true);
    private int tickSinceLastVelocity = 0;
    private int tickSinceLastAttack = 0;
    private int tickSinceLastFlag = 0;
    private boolean shouldJump = false;
    private int lastHurtTime = 0;
    private int lastVelocitySize = 0;

    public AdvancedJumpReset() {
        super("AdvancedJumpReset", false);
    }

    private void onHurtTimeUpdate() {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            if (this.shouldReduce() && this.reduceEvent.getModeString().equals("Hurt time updated")) {
                this.doReduce();
            }

            if (player.field_70737_aN >= this.hurtTimeRange.getValue()
                && player.field_70737_aN <= this.hurtTimeRange.getSecondValue()
                && this.canJump(this.lastVelocitySize)
                && this.shouldJump) {
                this.doJump();
                if (this.shouldReduce() && this.reduceEvent.getModeString().equals("Jumped")) {
                    this.doReduce();
                }

                this.tickSinceLastVelocity = 0;
                this.shouldJump = false;
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            if (player.field_70737_aN != this.lastHurtTime) {
                this.lastHurtTime = player.field_70737_aN;
                this.onHurtTimeUpdate();
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity)event.getPacket();
            if (packet.func_149412_c() == mc.field_71439_g.func_145782_y()) {
                this.shouldJump = true;
                this.lastVelocitySize = (int)MathHelper.func_76133_a(
                    packet.func_149411_d() * packet.func_149411_d() + packet.func_149409_f() * packet.func_149409_f()
                );
            }
        } else {
            if (event.getPacket() instanceof S08PacketPlayerPosLook) {
                this.tickSinceLastFlag = 0;
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        this.tickSinceLastAttack = 0;
    }

    private void doReduce() {
        if (this.reduce.getValue()) {
            EntityPlayer player = mc.field_71439_g;
            if (player != null) {
                if (player.field_70737_aN >= this.reduceHurtTime.getValue()
                    && player.field_70737_aN <= this.reduceHurtTime.getSecondValue()) {
                    float original = this.reduceFactor.getValue();
                    if (player.func_70051_ag()) {
                        original = this.reduceFactorWhileSprint.getValue();
                    }

                    if (this.tickSinceLastAttack < 3) {
                        original = this.reduceFactorWhileHit.getValue();
                    }

                    if (player.func_70051_ag() && this.tickSinceLastAttack < 3) {
                        original = this.reduceFactorWhileHitSprint.getValue();
                    }

                    float amount = original;
                    if (this.reduceMode.getModeString().equals("Smooth")) {
                        amount = 1.0F - original;
                    }

                    amount = MathHelper.func_76131_a(amount, 0.0F, 1.0F);
                    player.field_70159_w *= amount;
                    player.field_70179_y *= amount;
                }
            }
        }
    }

    private void doJump() {
        EntityPlayer player = mc.field_71439_g;
        if (player != null) {
            switch (this.howToJump.getModeString()) {
                case "Functional":
                    if (!mc.field_71474_y.field_74314_A.func_151470_d()) {
                        player.func_70664_aZ();
                    }
                    break;
                case "Legitimize":
                    KeyBinding.func_74507_a(mc.field_71474_y.field_74314_A.func_151463_i());
                    break;
                case "Motion":
                    player.field_70181_x = this.motionHeight.getValue().floatValue();
            }
        }
    }

    private boolean shouldPause() {
        switch (this.pauseWhen.getModeString()) {
            case "Server flag packet received":
                return this.tickSinceLastFlag < this.serverLagTick.getValue();
            case "Not in combat":
                return this.tickSinceLastAttack < this.notInCombatTick.getValue();
            case "Both":
                return this.tickSinceLastFlag < this.serverLagTick.getValue()
                    || this.tickSinceLastAttack < this.notInCombatTick.getValue();
            default:
                return false;
        }
    }

    private boolean shouldReduce() {
        EntityPlayer player = mc.field_71439_g;
        if (player == null) {
            return false;
        } else {
            return this.reduceEvent.getModeString().equals("Jumped")
                ? this.shouldJump
                : player.field_70737_aN >= this.hurtTimeRange.getValue()
                    && player.field_70737_aN <= this.hurtTimeRange.getSecondValue();
        }
    }

    private void resetAll() {
        this.tickSinceLastVelocity = 0;
        this.tickSinceLastAttack = 0;
        this.tickSinceLastFlag = 0;
        this.shouldJump = false;
        this.lastHurtTime = 0;
        this.lastVelocitySize = -1;
    }

    @Override
    public void onEnabled() {
        this.resetAll();
    }

    @Override
    public void onDisabled() {
        this.resetAll();
    }

    @EventTarget
    public void onWorld(LoadWorldEvent event) {
        this.resetAll();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            this.tickSinceLastVelocity++;
            this.tickSinceLastAttack++;
            this.tickSinceLastFlag++;
        }
    }

    private boolean canJump(int xzAverageMotion) {
        EntityPlayer player = mc.field_71439_g;
        if (player == null) {
            return false;
        }

        if (xzAverageMotion < this.activeMotion.getValue() || xzAverageMotion > this.activeMotion.getSecondValue()) {
            return false;
        }

        if (this.stopWhenBlocking.getValue() && player.func_70632_aY()) {
            return false;
        }

        if (this.stopWhenBackward.getValue() && player.field_70701_bs == -1.0F) {
            return false;
        }

        if (this.stopWhenSneaking.getValue() && player.func_70093_af()) {
            return false;
        }

        if (this.stopWhenFire.getValue() && player.func_70027_ad()) {
            return false;
        }

        if (this.stopWhenSpeed.getValue() && player.func_70644_a(Potion.field_76424_c)) {
            return false;
        }

        if (this.stopWhenJumpBoost.getValue() && player.func_70644_a(Potion.field_76430_j)) {
            return false;
        }

        if (this.stopWhenInInventory.getValue() && mc.field_71462_r != null) {
            return false;
        }

        if (!this.stopWhenBadSurrounding.getValue()
            || !player.field_70123_F
                && !player.func_70090_H()
                && !player.func_180799_ab()
                && !player.func_70617_f_()
                && !((IAccessorEntity)player).getIsInWeb()) {
            if (this.stopWhenInAir.getValue() && !player.field_70122_E) {
                return false;
            }

            if (this.shouldPause()) {
                return false;
            }

            switch (this.jumpRate.getModeString()) {
                case "Tick since last velocity":
                    return this.tickSinceLastVelocity >= this.minTickSinceLastVL.getValue();
                case "Simple RNG":
                    return RandomUtil.nextInt(0, 100) <= this.simpleRNG.getValue();
                case "Polar Safe RNG":
                    return player.field_70173_aa % 2 == 0;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }
}
