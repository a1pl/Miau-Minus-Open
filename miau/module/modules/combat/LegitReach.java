package miau.module.modules.combat;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.world.WorldSettings.GameType;

public class LegitReach extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Intave", "FakePlayer"});
    public final BooleanProperty aura = new BooleanProperty("Aura", false);
    public final IntProperty pulseDelay = new IntProperty("PulseDelay", 200, 50, 500);
    public final IntProperty intaveTestHurtTime = new IntProperty("Intave-Packets", 5, 0, 30);
    private EntityOtherPlayerMP fakePlayer = null;
    private EntityLivingBase currentTarget = null;
    private boolean shown = false;
    private final TimerUtil pulseTimer = new TimerUtil();

    public LegitReach() {
        super("LegitReach", false);
    }

    @Override
    public void onDisabled() {
        this.removeFakePlayer();
    }

    private void removeFakePlayer() {
        if (this.fakePlayer != null) {
            this.currentTarget = null;
            mc.field_71441_e.func_72900_e(this.fakePlayer);
            this.fakePlayer = null;
            this.shown = false;
        }
    }

    private void attackEntity(EntityLivingBase entity) {
        mc.field_71439_g.func_71038_i();
        mc.field_71439_g.field_71174_a.func_147297_a(new C02PacketUseEntity(entity, Action.ATTACK));
        if (mc.field_71442_b.func_178889_l() != GameType.SPECTATOR) {
            mc.field_71439_g.func_71059_n(entity);
        }
    }

    private void createFakePlayer(EntityLivingBase target) {
        if (mc.field_71441_e != null) {
            NetworkPlayerInfo playerInfo = mc.func_147114_u().func_175102_a(target.func_110124_au());
            if (playerInfo != null) {
                EntityOtherPlayerMP faker = new EntityOtherPlayerMP(mc.field_71441_e, playerInfo.func_178845_a());
                faker.field_70759_as = target.field_70759_as;
                faker.field_70761_aq = target.field_70761_aq;
                faker.func_82149_j(target);
                faker.func_70606_j(target.func_110143_aJ());

                for (int index = 0; index < 5; index++) {
                    ItemStack stack = target.func_71124_b(index);
                    if (stack != null) {
                        faker.func_70062_b(index, stack);
                    }
                }

                mc.field_71441_e.func_73027_a(-1337, faker);
                this.fakePlayer = faker;
                this.shown = true;
            }
        }
    }

    private void updateFakePlayer(boolean intaveMode) {
        if (this.fakePlayer != null && this.currentTarget != null) {
            EntityLivingBase faker = this.fakePlayer;
            EntityLivingBase target = this.currentTarget;
            if (faker.func_70089_S() && !target.field_70128_L && target.func_70089_S()) {
                faker.func_70606_j(target.func_110143_aJ());

                for (int index = 0; index < 5; index++) {
                    ItemStack stack = target.func_71124_b(index);
                    if (stack != null) {
                        faker.func_70062_b(index, stack);
                    }
                }

                boolean pulse = intaveMode
                    ? mc.field_71439_g.field_70173_aa % this.intaveTestHurtTime.getValue() == 0
                    : this.pulseTimer.hasTimeElapsed(this.pulseDelay.getValue().intValue());
                if (pulse) {
                    faker.field_70759_as = target.field_70759_as;
                    faker.field_70761_aq = target.field_70761_aq;
                    faker.func_82149_j(target);
                    this.pulseTimer.reset();
                }
            } else {
                this.removeFakePlayer();
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        Entity target = event.getTarget();
        if (target instanceof EntityLivingBase) {
            EntityLivingBase entity = (EntityLivingBase)target;
            if (this.fakePlayer == null) {
                this.currentTarget = entity;
                this.createFakePlayer(entity);
            } else if (event.getTarget() == this.fakePlayer) {
                if (this.currentTarget != null) {
                    this.attackEntity(this.currentTarget);
                }
            } else {
                this.removeFakePlayer();
                this.currentTarget = entity;
                this.createFakePlayer(entity);
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null && this.currentTarget != null) {
                KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
                if (!this.aura.getValue() || killAura != null && killAura.isEnabled()) {
                    String modeString = this.mode.getModeString();
                    if (modeString.equals("Intave")) {
                        this.updateFakePlayer(true);
                        if (!this.shown
                            && this.currentTarget != null
                            && mc.func_147114_u().func_175102_a(this.currentTarget.func_110124_au()) != null) {
                            this.createFakePlayer(this.currentTarget);
                        }
                    } else if (modeString.equals("FakePlayer")) {
                        this.updateFakePlayer(false);
                        if (!this.shown
                            && this.currentTarget != null
                            && mc.func_147114_u().func_175102_a(this.currentTarget.func_110124_au()) != null) {
                            this.createFakePlayer(this.currentTarget);
                        }
                    }
                } else {
                    this.removeFakePlayer();
                }
            } else {
                this.removeFakePlayer();
            }
        }
    }
}
