package miau.module.modules.movement;

import com.google.common.base.CaseFormat;
import miau.Miau;
import miau.enums.BlinkModules;
import miau.event.EventTarget;
import miau.event.impl.KeyEvent;
import miau.event.impl.PlayerUpdateEvent;
import miau.module.Module;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.util.math.RandomUtil;
import miau.util.player.PlayerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.util.AxisAlignedBB;

public class AntiVoid extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private boolean isInVoid = false;
    private boolean wasInVoid = false;
    private double[] lastSafePosition = null;
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"BLINK"});
    public final FloatProperty distance = new FloatProperty("distance", 5.0F, 0.0F, 16.0F);

    private void resetBlink() {
        Miau.blinkManager.setBlinkState(false, BlinkModules.ANTI_VOID);
        this.lastSafePosition = null;
    }

    private boolean canUseAntiVoid() {
        LongJump longJump = (LongJump)Miau.moduleManager.modules.get(LongJump.class);
        return !longJump.isJumping();
    }

    public AntiVoid() {
        super("AntiVoid", false);
    }

    @EventTarget(4)
    public void onUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled()) {
            this.isInVoid = !mc.field_71439_g.field_71075_bZ.field_75101_c && PlayerUtil.isInWater();
            if (this.mode.getValue() == 0) {
                if (!this.isInVoid) {
                    this.resetBlink();
                }

                if (this.lastSafePosition != null) {
                    float subWidth = mc.field_71439_g.field_70130_N / 2.0F;
                    float height = mc.field_71439_g.field_70131_O;
                    if (PlayerUtil.checkInWater(
                        new AxisAlignedBB(
                            this.lastSafePosition[0] - subWidth,
                            this.lastSafePosition[1],
                            this.lastSafePosition[2] - subWidth,
                            this.lastSafePosition[0] + subWidth,
                            this.lastSafePosition[1] + height,
                            this.lastSafePosition[2] + subWidth
                        )
                    )) {
                        this.resetBlink();
                    }
                }

                if (!this.wasInVoid && this.isInVoid && this.canUseAntiVoid()) {
                    Miau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    if (Miau.blinkManager.setBlinkState(true, BlinkModules.ANTI_VOID)) {
                        this.lastSafePosition = new double[]{
                            mc.field_71439_g.field_70169_q,
                            mc.field_71439_g.field_70167_r,
                            mc.field_71439_g.field_70166_s
                        };
                    }
                }

                if (Miau.blinkManager.getBlinkingModule() == BlinkModules.ANTI_VOID
                    && this.lastSafePosition != null
                    && this.lastSafePosition[1] - this.distance.getValue().floatValue()
                        > mc.field_71439_g.field_70163_u) {
                    Miau.blinkManager
                        .blinkedPackets
                        .offerFirst(
                            new C04PacketPlayerPosition(
                                this.lastSafePosition[0],
                                this.lastSafePosition[1] - RandomUtil.nextDouble(10.0, 20.0),
                                this.lastSafePosition[2],
                                false
                            )
                        );
                    this.resetBlink();
                }
            }

            this.wasInVoid = this.isInVoid;
        }
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (event.getKey() == mc.field_71474_y.field_74313_G.func_151463_i()) {
            ItemStack currentItem = mc.field_71439_g.field_71071_by.func_70448_g();
            if (currentItem != null && currentItem.func_77973_b() instanceof ItemEnderPearl) {
                this.resetBlink();
            }
        }
    }

    @Override
    public void onEnabled() {
        this.isInVoid = false;
        this.wasInVoid = false;
        this.resetBlink();
    }

    @Override
    public void onDisabled() {
        Miau.blinkManager.setBlinkState(false, BlinkModules.ANTI_VOID);
    }

    @Override
    public void verifyValue(String mode) {
        if (this.isEnabled()) {
            this.onDisabled();
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
