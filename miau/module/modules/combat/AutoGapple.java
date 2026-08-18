package miau.module.modules.combat;

import java.util.Random;
import miau.event.EventTarget;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorEntityLivingBase;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.network.PacketUtil;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.potion.Potion;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;

public class AutoGapple extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Auto", "LegitAuto", "Legit", "Head"});
    private final FloatProperty percent = new FloatProperty("HealthPercent", 75.0F, 1.0F, 100.0F);
    private final IntProperty min = new IntProperty("MinDelay", 75, 1, 5000);
    private final IntProperty max = new IntProperty("MaxDelay", 125, 1, 5000);
    private final FloatProperty regenSec = new FloatProperty("MinRegenSec", 4.6F, 0.0F, 10.0F);
    private final BooleanProperty groundCheck = new BooleanProperty("OnlyOnGround", false);
    private final BooleanProperty waitRegen = new BooleanProperty("WaitRegen", true);
    private final BooleanProperty invCheck = new BooleanProperty("InvCheck", false);
    private final BooleanProperty absorpCheck = new BooleanProperty("NoAbsorption", true);
    private final BooleanProperty fastEatValue = new BooleanProperty(
        "FastEat",
        false,
        () -> this.mode.getModeString().equals("LegitAuto") || this.mode.getModeString().equals("Legit")
    );
    private final IntProperty eatDelayValue = new IntProperty("FastEatDelay", 14, 0, 35, this.fastEatValue::getValue);
    private final BooleanProperty eatMessage = new BooleanProperty("CreateMessageAfterEaten", false);
    private final TimerUtil timer = new TimerUtil();
    private int eating = -1;
    private int delay = 0;
    private boolean isDisable = false;
    private boolean tryHeal = false;
    private int prevSlot = -1;
    private boolean switchBack = false;

    public AutoGapple() {
        super("AutoGapple", false);
    }

    @Override
    public void onEnabled() {
        this.eating = -1;
        this.prevSlot = -1;
        this.switchBack = false;
        this.timer.reset();
        this.isDisable = false;
        this.tryHeal = false;
        this.delay = MathHelper.func_76136_a(new Random(), this.min.getValue(), this.max.getValue());
    }

    @EventTarget
    public void onWorld(LoadWorldEvent event) {
        this.isDisable = true;
        this.tryHeal = false;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        Packet<?> packet = event.getPacket();
        if (this.eating != -1 && packet instanceof C03PacketPlayer) {
            this.eating++;
        } else if (packet instanceof S09PacketHeldItemChange || packet instanceof C09PacketHeldItemChange) {
            this.eating = -1;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null) {
            if (event.getType() == EventType.PRE) {
                if (this.tryHeal) {
                    switch (this.mode.getModeString()) {
                        case "Auto":
                            int gappleInHotbar = this.findHotbar(Items.field_151153_ao);
                            if (gappleInHotbar != -1) {
                                PacketUtil.sendPacket(new C09PacketHeldItemChange(gappleInHotbar));
                                PacketUtil.sendPacket(
                                    new C08PacketPlayerBlockPlacement(mc.field_71439_g.func_70694_bm())
                                );

                                for (int i = 0; i < 35; i++) {
                                    PacketUtil.sendPacket(new C03PacketPlayer(mc.field_71439_g.field_70122_E));
                                }

                                PacketUtil.sendPacket(
                                    new C09PacketHeldItemChange(mc.field_71439_g.field_71071_by.field_70461_c)
                                );
                                if (this.eatMessage.getValue()) {
                                    mc.field_71439_g.func_145747_a(new ChatComponentText("Gapple eaten"));
                                }

                                this.tryHeal = false;
                                this.timer.reset();
                                this.delay = MathHelper.func_76136_a(
                                    new Random(), this.min.getValue(), this.max.getValue()
                                );
                            } else {
                                this.tryHeal = false;
                            }
                            break;
                        case "LegitAuto":
                            if (this.eating == -1) {
                                int gapple2 = this.findHotbar(Items.field_151153_ao);
                                if (gapple2 == -1) {
                                    this.tryHeal = false;
                                    return;
                                }

                                PacketUtil.sendPacket(new C09PacketHeldItemChange(gapple2));
                                if (this.eatMessage.getValue()) {
                                    mc.field_71439_g.func_145747_a(new ChatComponentText("Gapple eaten"));
                                }

                                mc.func_147114_u()
                                    .func_147297_a(new C08PacketPlayerBlockPlacement(mc.field_71439_g.func_70694_bm()));
                                this.eating = 0;
                            } else if (this.eating > 35
                                || this.fastEatValue.getValue() && this.eating > this.eatDelayValue.getValue()) {
                                for (int i = 0; i < 35 - this.eating; i++) {
                                    PacketUtil.sendPacket(new C03PacketPlayer(mc.field_71439_g.field_70122_E));
                                }

                                PacketUtil.sendPacket(
                                    new C09PacketHeldItemChange(mc.field_71439_g.field_71071_by.field_70461_c)
                                );
                                this.timer.reset();
                                this.tryHeal = false;
                                this.delay = MathHelper.func_76136_a(
                                    new Random(), this.min.getValue(), this.max.getValue()
                                );
                                if (this.eatMessage.getValue()) {
                                    mc.field_71439_g.func_145747_a(new ChatComponentText("Gapple eaten"));
                                }
                            }
                            break;
                        case "Legit":
                            if (this.eating == -1) {
                                int gapple3 = this.findHotbar(Items.field_151153_ao);
                                if (gapple3 == -1) {
                                    this.tryHeal = false;
                                    return;
                                }

                                if (this.prevSlot == -1) {
                                    this.prevSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                                }

                                mc.field_71439_g.field_71071_by.field_70461_c = gapple3;
                                PacketUtil.sendPacket(
                                    new C08PacketPlayerBlockPlacement(mc.field_71439_g.func_70694_bm())
                                );
                                this.eating = 0;
                            } else if (this.eating > 35
                                || this.fastEatValue.getValue() && this.eating > this.eatDelayValue.getValue()) {
                                for (int i = 0; i < 35 - this.eating; i++) {
                                    PacketUtil.sendPacket(new C03PacketPlayer(mc.field_71439_g.field_70122_E));
                                }

                                this.timer.reset();
                                this.tryHeal = false;
                                this.delay = MathHelper.func_76136_a(
                                    new Random(), this.min.getValue(), this.max.getValue()
                                );
                                if (this.eatMessage.getValue()) {
                                    mc.field_71439_g.func_145747_a(new ChatComponentText("Gapple eaten"));
                                }
                            }
                            break;
                        case "Head":
                            int headInHotbar = this.findHotbar(Items.field_151144_bL);
                            if (headInHotbar != -1) {
                                PacketUtil.sendPacket(new C09PacketHeldItemChange(headInHotbar));
                                PacketUtil.sendPacket(
                                    new C08PacketPlayerBlockPlacement(mc.field_71439_g.func_70694_bm())
                                );
                                PacketUtil.sendPacket(
                                    new C09PacketHeldItemChange(mc.field_71439_g.field_71071_by.field_70461_c)
                                );
                                this.timer.reset();
                                this.tryHeal = false;
                                this.delay = MathHelper.func_76136_a(
                                    new Random(), this.min.getValue(), this.max.getValue()
                                );
                            } else {
                                this.tryHeal = false;
                                if (this.eatMessage.getValue()) {
                                    mc.field_71439_g.func_145747_a(new ChatComponentText("Gapple eaten"));
                                }
                            }
                    }
                }

                if (mc.field_71439_g.field_70173_aa <= 10 && this.isDisable) {
                    this.isDisable = false;
                }

                int absorp = MathHelper.func_76143_f(
                    ((IAccessorEntityLivingBase)mc.field_71439_g).getAbsorptionAmount()
                );
                if (!this.tryHeal && this.prevSlot != -1) {
                    if (!this.switchBack) {
                        this.switchBack = true;
                        return;
                    }

                    mc.field_71439_g.field_71071_by.field_70461_c = this.prevSlot;
                    this.eating = -1;
                    this.prevSlot = -1;
                    this.switchBack = false;
                }

                if ((!this.groundCheck.getValue() || mc.field_71439_g.field_70122_E)
                    && (!this.invCheck.getValue() || !(mc.field_71462_r instanceof GuiContainer))
                    && (absorp <= 0 || !this.absorpCheck.getValue())) {
                    if (!this.waitRegen.getValue()
                        || !mc.field_71439_g.func_70644_a(Potion.field_76428_l)
                        || mc.field_71439_g.func_70660_b(Potion.field_76428_l).func_76459_b()
                            <= (int)(this.regenSec.getValue() * 20.0F)) {
                        if (!this.isDisable
                            && mc.field_71439_g.func_110143_aJ()
                                <= this.percent.getValue() / 100.0F * mc.field_71439_g.func_110138_aP()
                            && this.timer.hasTimeElapsed(this.delay)) {
                            if (this.tryHeal) {
                                return;
                            }

                            this.tryHeal = true;
                        }
                    }
                }
            }
        }
    }

    private int findHotbar(Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (stack != null && stack.func_77973_b() == item) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
