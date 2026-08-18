package miau.module.modules.combat;

import com.google.common.base.CaseFormat;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.HitSlowDownEvent;
import miau.event.impl.JumpEvent;
import miau.event.impl.KnockbackEvent;
import miau.event.impl.LivingUpdateEvent;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.StrafeEvent;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.mixin.IAccessorEntity;
import miau.module.Module;
import miau.module.modules.combat.velocity.AAC5Velocity;
import miau.module.modules.combat.velocity.AACPushVelocity;
import miau.module.modules.combat.velocity.AACVelocity;
import miau.module.modules.combat.velocity.AACZeroVelocity;
import miau.module.modules.combat.velocity.AACv4Velocity;
import miau.module.modules.combat.velocity.AttackReduceVelocity;
import miau.module.modules.combat.velocity.BlocksMCVelocity;
import miau.module.modules.combat.velocity.BufferAbuseVelocity;
import miau.module.modules.combat.velocity.BuzzReverseVelocity;
import miau.module.modules.combat.velocity.CancelVelocity;
import miau.module.modules.combat.velocity.CustomVelocity;
import miau.module.modules.combat.velocity.DelayVelocity;
import miau.module.modules.combat.velocity.DexlandVelocity;
import miau.module.modules.combat.velocity.FBDelayVelocity;
import miau.module.modules.combat.velocity.GhostBlockVelocity;
import miau.module.modules.combat.velocity.GlitchVelocity;
import miau.module.modules.combat.velocity.GrimC03Velocity;
import miau.module.modules.combat.velocity.GrimReduceVelocity;
import miau.module.modules.combat.velocity.GrimVerticalVelocity;
import miau.module.modules.combat.velocity.HylexVelocity;
import miau.module.modules.combat.velocity.HypixelAirVelocity;
import miau.module.modules.combat.velocity.HypixelMovingVelocity;
import miau.module.modules.combat.velocity.HypixelVelocity;
import miau.module.modules.combat.velocity.Intave13Velocity;
import miau.module.modules.combat.velocity.Intave1412Velocity;
import miau.module.modules.combat.velocity.Intave1433Velocity;
import miau.module.modules.combat.velocity.Intave14Velocity;
import miau.module.modules.combat.velocity.IntaveFlagVelocity;
import miau.module.modules.combat.velocity.IntaveStrongVelocity;
import miau.module.modules.combat.velocity.IntaveTimerVelocity;
import miau.module.modules.combat.velocity.IntaveVelocity;
import miau.module.modules.combat.velocity.JumpVelocity;
import miau.module.modules.combat.velocity.KarhuVelocity;
import miau.module.modules.combat.velocity.KazerVelocity;
import miau.module.modules.combat.velocity.LegitClickVelocity;
import miau.module.modules.combat.velocity.LegitVelocity;
import miau.module.modules.combat.velocity.LiquidBounceDelayVelocity;
import miau.module.modules.combat.velocity.LuckyvnVelocity;
import miau.module.modules.combat.velocity.MatrixReduce2Velocity;
import miau.module.modules.combat.velocity.MatrixReduce3Velocity;
import miau.module.modules.combat.velocity.MatrixReduceVelocity;
import miau.module.modules.combat.velocity.OldGrimVelocity;
import miau.module.modules.combat.velocity.OldPolarVelocity;
import miau.module.modules.combat.velocity.PolarJumpVelocity;
import miau.module.modules.combat.velocity.PolarVelocity;
import miau.module.modules.combat.velocity.ReverseVelocity;
import miau.module.modules.combat.velocity.S32PacketVelocity;
import miau.module.modules.combat.velocity.SimpleVelocity;
import miau.module.modules.combat.velocity.SmoothReverseVelocity;
import miau.module.modules.combat.velocity.StandardVelocity;
import miau.module.modules.combat.velocity.ThreeFPracVelocity;
import miau.module.modules.combat.velocity.UniversoCraftOldVelocity;
import miau.module.modules.combat.velocity.VelocityMode;
import miau.module.modules.combat.velocity.VulcanVelocity;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;

public class Velocity extends Module {
    public static final Minecraft mc = Minecraft.func_71410_x();
    public int chanceCounter = 0;
    public int delayChanceCounter = 0;
    public boolean pendingExplosion = false;
    public boolean allowNext = true;
    public boolean jumpFlag = false;
    public boolean reverseFlag = false;
    public boolean delayActive = false;
    public boolean shouldJump = false;
    public int jumpCooldown = 0;
    public boolean hasReceivedVelocity = false;
    public int legitSmartJumpCount = 0;
    public int intaveTick = 0;
    public int intaveDamageTick = 0;
    public final BooleanProperty onSwing = new BooleanProperty("on-swing", false);
    public final List<VelocityMode> modes = new ArrayList<>();
    public final ModeProperty mode = new ModeProperty(
        "mode",
        0,
        new String[]{
            this.register(new ThreeFPracVelocity("3FPrac", this)),
            this.register(new StandardVelocity("Standard", this)),
            this.register(new LegitVelocity("Legit", this)),
            this.register(new IntaveVelocity("Intave", this)),
            this.register(new DelayVelocity("Delay", this)),
            this.register(new PolarVelocity("Polar", this)),
            this.register(new AttackReduceVelocity("AttackReduce", this)),
            this.register(new GrimReduceVelocity("GrimReduce", this)),
            this.register(new LuckyvnVelocity("Luckyvn", this)),
            this.register(new SimpleVelocity("Simple", this)),
            this.register(new CancelVelocity("Cancel", this)),
            this.register(new AACVelocity("AAC", this)),
            this.register(new AACPushVelocity("AACPush", this)),
            this.register(new AACZeroVelocity("AACZero", this)),
            this.register(new AACv4Velocity("AACv4", this)),
            this.register(new AAC5Velocity("AAC5", this)),
            this.register(new ReverseVelocity("Reverse", this)),
            this.register(new SmoothReverseVelocity("SmoothReverse", this)),
            this.register(new JumpVelocity("Jump", this)),
            this.register(new GlitchVelocity("Glitch", this)),
            this.register(new GhostBlockVelocity("GhostBlock", this)),
            this.register(new VulcanVelocity("Vulcan", this)),
            this.register(new S32PacketVelocity("S32Packet", this)),
            this.register(new MatrixReduceVelocity("MatrixReduce", this)),
            this.register(new MatrixReduce2Velocity("MatrixReduce2", this)),
            this.register(new MatrixReduce3Velocity("MatrixReduce3", this)),
            this.register(new LiquidBounceDelayVelocity("LiquidBounceDelay", this)),
            this.register(new GrimC03Velocity("GrimC03", this)),
            this.register(new BufferAbuseVelocity("BufferAbuse", this)),
            this.register(new FBDelayVelocity("DelayFB", this)),
            this.register(new CustomVelocity("Custom", this)),
            this.register(new LegitClickVelocity("LegitClick", this)),
            this.register(new GrimVerticalVelocity("GrimVertical", this)),
            this.register(new OldGrimVelocity("OldGrim", this)),
            this.register(new PolarJumpVelocity("PolarJump", this)),
            this.register(new OldPolarVelocity("OldPolar", this)),
            this.register(new BuzzReverseVelocity("BuzzReverse", this)),
            this.register(new Intave14Velocity("Intave14", this)),
            this.register(new Intave13Velocity("Intave13.0.6", this)),
            this.register(new Intave1433Velocity("Intave14.3.3", this)),
            this.register(new Intave1412Velocity("Intave14.1.2", this)),
            this.register(new IntaveTimerVelocity("IntaveTimer", this)),
            this.register(new IntaveFlagVelocity("IntaveFlag", this)),
            this.register(new IntaveStrongVelocity("IntaveStrong", this)),
            this.register(new KarhuVelocity("Karhu", this)),
            this.register(new KazerVelocity("Kazer", this)),
            this.register(new UniversoCraftOldVelocity("UniversoCraftOld", this)),
            this.register(new BlocksMCVelocity("BlocksMC", this)),
            this.register(new HylexVelocity("Hylex", this)),
            this.register(new DexlandVelocity("Dexland", this)),
            this.register(new HypixelVelocity("Hypixel", this)),
            this.register(new HypixelAirVelocity("HypixelAir", this)),
            this.register(new HypixelMovingVelocity("HypixelMoving", this))
        }
    );

    private String register(VelocityMode m) {
        this.modes.add(m);
        return m.getName();
    }

    public boolean isInLiquidOrWeb() {
        return mc.field_71439_g.func_70090_H()
            || mc.field_71439_g.func_180799_ab()
            || ((IAccessorEntity)mc.field_71439_g).getIsInWeb();
    }

    public boolean canDelay() {
        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        return mc.field_71439_g.field_70122_E && (!killAura.isEnabled() || !killAura.shouldAutoBlock());
    }

    public Velocity() {
        super("Velocity", false);
    }

    @Override
    public List<Property<?>> getAdditionalProperties() {
        List<Property<?>> props = new ArrayList<>();

        for (VelocityMode m : this.modes) {
            for (Field field : m.getClass().getDeclaredFields()) {
                field.setAccessible(true);

                try {
                    Object obj = field.get(m);
                    if (obj instanceof Property) {
                        Property<?> prop = (Property<?>)obj;
                        BooleanSupplier original = prop.getVisibleChecker();
                        prop.setVisibleChecker(
                            () -> this.getActiveMode() == m && (original == null || original.getAsBoolean())
                        );
                        props.add(prop);
                    }
                } catch (Exception var11) {
                }
            }
        }

        return props;
    }

    public VelocityMode getActiveMode() {
        return this.modes
            .stream()
            .filter(m -> m.getName().equals(this.mode.getModeString()))
            .findFirst()
            .orElse(this.modes.get(0));
    }

    @Override
    public void onEnabled() {
        this.getActiveMode().onEnable();
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (this.isEnabled()) {
            this.getActiveMode().onKnockback(event);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()) {
            this.getActiveMode().onUpdate(event);
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled()) {
            this.getActiveMode().onLivingUpdate(event);
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled()) {
            this.getActiveMode().onStrafe(event);
        }
    }

    @EventTarget
    public void onJump(JumpEvent event) {
        if (this.isEnabled()) {
            this.getActiveMode().onJump(event);
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled()) {
            this.getActiveMode().onRender3D(event);
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            this.getActiveMode().onMoveInput(event);
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled()) {
            this.getActiveMode().onAttack(event);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            this.getActiveMode().onPacket(event);
        }
    }

    @EventTarget
    public void onHitSlowDown(HitSlowDownEvent event) {
        if (this.isEnabled()) {
            this.getActiveMode().onHitSlowDown(event);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            this.getActiveMode().onTick(event);
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.onDisabled();
    }

    @Override
    public void onDisabled() {
        this.getActiveMode().onDisable();
        this.pendingExplosion = false;
        this.allowNext = true;
        this.shouldJump = false;
        this.jumpCooldown = 0;
        this.hasReceivedVelocity = false;
        this.legitSmartJumpCount = 0;
        this.intaveTick = 0;
        this.intaveDamageTick = 0;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
