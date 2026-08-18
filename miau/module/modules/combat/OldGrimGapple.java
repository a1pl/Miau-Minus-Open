package miau.module.modules.combat;

import java.awt.Color;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.UpdateEvent;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.util.client.ChatUtil;
import miau.util.network.PacketUtil;
import miau.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;

public class OldGrimGapple extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final IntProperty c = new IntProperty("C03PacketPlayer", 32, 32, 40);
    private final IntProperty eatHealth = new IntProperty("EatHealth", 12, 1, 18);
    private final BooleanProperty autoEat = new BooleanProperty("AutoGapple", true);
    private final BooleanProperty progressBar2 = new BooleanProperty("ProgressBar2", false);
    private final BooleanProperty stopWhenNoTarget = new BooleanProperty("StopWhenNoTarget", true);
    private final BooleanProperty stuck = new BooleanProperty("Stuck", false);
    private final BooleanProperty isEatingGapple = new BooleanProperty("DisplayStateInDynamicIsland", true);
    private final BooleanProperty checkAbsorption = new BooleanProperty("CheckAbsorption", true);
    private final IntProperty minAbsorption = new IntProperty("MinAbsorption", 0, 0, 8);
    public float eatingProgress = 0.0F;
    public boolean shouldShowIndicator = false;
    private double x = 0.0;
    private double y = 0.0;
    private double z = 0.0;
    private boolean cancelMove = false;
    private boolean r = false;
    private int ticks = 0;
    private int pauseTicks = 0;
    private float yaw = 0.0F;
    private float pitch = 0.0F;
    private boolean shouldEat = false;
    public boolean isEating = false;
    private int slot = -1;

    public OldGrimGapple() {
        super("OldGrimGapple", false);
    }

    @Override
    public void onEnabled() {
        this.shouldEat = false;
        this.isEating = false;
        this.eatingProgress = 0.0F;
        this.shouldShowIndicator = false;
        this.ticks = 0;
        this.pauseTicks = 0;
        this.stopStuck();
    }

    @Override
    public void onDisabled() {
        this.ticks = 0;
        this.pauseTicks = 0;
        this.shouldEat = false;
        this.isEating = false;
        this.eatingProgress = 0.0F;
        this.shouldShowIndicator = false;
        this.stopStuck();
    }

    @EventTarget
    public void onWorld(LoadWorldEvent event) {
        this.shouldEat = false;
        this.isEating = false;
        this.eatingProgress = 0.0F;
        this.shouldShowIndicator = false;
        this.ticks = 0;
        this.pauseTicks = 0;
        this.stopStuck();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof C03PacketPlayer && this.cancelMove && this.ticks < this.c.getValue()) {
            if (event.getPacket() instanceof C05PacketPlayerLook) {
                C05PacketPlayerLook packet = (C05PacketPlayerLook)event.getPacket();
                this.yaw = packet.func_149462_g();
                this.pitch = packet.func_149470_h();
            }

            this.ticks++;
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        this.yaw = mc.field_71439_g.field_70177_z;
        this.pitch = mc.field_71439_g.field_70125_A;
        this.slot = this.getGApple();
        boolean shouldContinueEating = this.checkShouldEat();
        if (shouldContinueEating && this.slot >= 0) {
            this.isEating = true;
            if (this.pauseTicks > 0) {
                this.pauseTicks--;
                if (this.pauseTicks <= 0 && this.stuck.getValue()) {
                    this.stopStuck();
                }

                this.eatingProgress = 0.0F;
                return;
            }

            if (this.stuck.getValue() && !this.cancelMove && this.pauseTicks == 0) {
                this.stuck();
            }

            if (this.ticks < this.c.getValue() && !this.cancelMove) {
                this.ticks++;
            }

            this.eatingProgress = (float)this.ticks / this.c.getValue().intValue();
            this.shouldShowIndicator = true;
            if (this.ticks >= this.c.getValue()) {
                PacketUtil.sendPacket(new C09PacketHeldItemChange(this.slot));
                PacketUtil.sendPacket(
                    new C08PacketPlayerBlockPlacement(mc.field_71439_g.field_71071_by.func_70301_a(this.slot))
                );
                if (this.stuck.getValue()) {
                    this.release();
                }

                PacketUtil.sendPacket(new C09PacketHeldItemChange(mc.field_71439_g.field_71071_by.field_70461_c));
                PacketUtil.sendPacket(
                    new C08PacketPlayerBlockPlacement(
                        mc.field_71439_g.field_71071_by.func_70301_a(mc.field_71439_g.field_71071_by.field_70461_c)
                    )
                );
                this.ticks = 0;
                this.pauseTicks = 2;
                this.eatingProgress = 0.0F;
                if (mc.field_71439_g.field_70173_aa % 20 == 0) {
                    ChatUtil.display("§6Auto Eating...");
                }
            }
        } else {
            this.shouldEat = false;
            this.isEating = false;
            if (this.stuck.getValue()) {
                this.stopStuck();
            }

            this.ticks = 0;
            this.pauseTicks = 0;
            this.eatingProgress = 0.0F;
            this.shouldShowIndicator = false;
            if (shouldContinueEating && this.slot < 0 && mc.field_71439_g.field_70173_aa % 40 == 0) {
                ChatUtil.display("§4NoGapple!");
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEating && this.progressBar2.getValue()) {
            ScaledResolution scaledScreen = new ScaledResolution(mc);
            float width = scaledScreen.func_78326_a();
            float height = scaledScreen.func_78328_b();
            this.drawProgressBar(width, height);
        }
    }

    private void stuck() {
        if (!this.r) {
            this.x = mc.field_71439_g.field_70159_w;
            this.y = mc.field_71439_g.field_70181_x;
            this.z = mc.field_71439_g.field_70179_y;
            this.r = true;
        }

        this.cancelMove = true;
    }

    private void stopStuck() {
        this.cancelMove = false;
        if (this.r) {
            mc.field_71439_g.field_70159_w = this.x;
            mc.field_71439_g.field_70181_x = this.y;
            mc.field_71439_g.field_70179_y = this.z;
            this.r = false;
        }
    }

    private void release() {
        PacketUtil.sendPacket(new C05PacketPlayerLook(this.yaw, this.pitch, mc.field_71439_g.field_70122_E));
        int count = Math.max(this.ticks - 1, 0);

        for (int i = 0; i < count; i++) {
            PacketUtil.sendPacket(new C03PacketPlayer(mc.field_71439_g.field_70122_E));
        }
    }

    private int getGApple() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (stack != null && stack.func_77973_b() instanceof ItemAppleGold) {
                return i;
            }
        }

        return -1;
    }

    private boolean checkShouldEat() {
        if (!this.autoEat.getValue()) {
            return false;
        }

        boolean hasTarget = this.checkKillAuraTarget();
        boolean healthOk = this.checkHealthCondition();
        boolean absorptionOk = this.checkAbsorptionCondition();
        if (healthOk && hasTarget && absorptionOk && !this.shouldEat) {
            ChatUtil.display("§aAuto eating started");
            this.shouldEat = true;
            this.isEating = true;
        }

        if ((!healthOk || !hasTarget || !absorptionOk) && this.shouldEat) {
            ChatUtil.display("§eAuto eating stopped");
            this.shouldEat = false;
            this.isEating = false;
        }

        return this.shouldEat;
    }

    private boolean checkHealthCondition() {
        int currentHealth = Math.round(mc.field_71439_g.func_110143_aJ());
        return currentHealth <= this.eatHealth.getValue();
    }

    private boolean checkAbsorptionCondition() {
        return !this.checkAbsorption.getValue()
            ? true
            : mc.field_71439_g.func_110139_bj() <= this.minAbsorption.getValue().intValue();
    }

    private boolean checkKillAuraTarget() {
        if (!this.stopWhenNoTarget.getValue()) {
            return true;
        }

        KillAura killAura = (KillAura)Miau.moduleManager.modules.get(KillAura.class);
        return killAura != null && killAura.getTarget() != null;
    }

    private void drawProgressBar(float width, float height) {
        float progressLength = 140.0F;
        float startY = height / 4.0F * 3.0F;
        float startX = width / 2.0F - progressLength / 2.0F;
        float progressRatio = Math.min(Math.max((float)this.ticks / this.c.getValue().intValue(), 0.0F), 1.0F);
        float currentProgress = progressLength * progressRatio;
        int progressPercent = (int)(progressRatio * 100.0F);
        this.showShadow(startX - 2.0F, startY - 2.0F, progressLength + 4.0F, 11.0F, 0.3F);
        RenderUtil.drawRoundedRectangle(
            startX, startY, startX + progressLength, startY + 7.0F, 2.0F, new Color(0, 0, 0, 128).getRGB()
        );
        if (currentProgress != 0.0F) {
            RenderUtil.drawRoundedGradientRect(
                startX,
                startY,
                startX + currentProgress,
                startY + 7.0F,
                3.0F,
                new Color(76, 157, 240, 255).getRGB(),
                new Color(53, 200, 167, 255).getRGB(),
                new Color(76, 157, 240, 255).getRGB(),
                new Color(53, 200, 167, 255).getRGB()
            );
        }

        String percentText = progressPercent + "%";
        mc.field_71466_p
            .func_175063_a(percentText, startX + progressLength + 5.0F, startY, new Color(255, 255, 255, 255).getRGB());
    }

    private void showShadow(float startX, float startY, float width, float height, float shadowStrength) {
        RenderUtil.drawRoundedRectangle(
            startX, startY, startX + width, startY + height, 3.0F, new Color(0, 0, 0, 120).getRGB()
        );
    }
}
