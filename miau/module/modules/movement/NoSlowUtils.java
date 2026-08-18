package miau.module.modules.movement;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.PlayerUpdateEvent;
import miau.event.impl.Render2DEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ModeProperty;
import miau.util.client.KeyBindUtil;
import miau.util.network.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;

public class NoSlowUtils extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ModeProperty mode = new ModeProperty(
        "NoSlow Mode", 0, new String[]{"Gamma", "Blink", "Float", "Disabled"}
    );
    public final BooleanProperty autoJumpGamma = new BooleanProperty("Auto Jump Gamma", false);
    private int offGroundTicks;
    private int ticks;
    private boolean send;
    private boolean doJump;
    private boolean blinking;

    public NoSlowUtils() {
        super("NoSlowUtils", false);
    }

    @Override
    public void onEnabled() {
        this.ticks = 0;
        this.blinking = false;
    }

    @EventTarget
    public void onPreUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            this.doJump = this.autoJumpGamma.getValue();
        }
    }

    @EventTarget
    public void onPreMotion(PlayerUpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            if (this.isBlinkEnabled()) {
                this.blinking = true;
            } else {
                this.blinking = false;
            }

            switch (this.mode.getValue()) {
                case 0:
                    this.gammaNoSlow();
                    break;
                case 1:
                    this.blinkNoSlow();
                    break;
                case 2:
                    this.floatNoSlow();
            }
        }
    }

    @EventTarget
    public void onRenderTick(Render2DEvent event) {
        if (this.isEnabled() && this.blinking && this.ticks > 1 && mc.field_71462_r == null) {
            String text = "blinking: §";
            if (this.ticks > 50) {
                text = text + "c";
            } else if (this.ticks > 30) {
                text = text + "6";
            } else if (this.ticks > 20) {
                text = text + "e";
            } else {
                text = text + "a";
            }

            text = text + this.ticks;
            ScaledResolution sr = new ScaledResolution(mc);
            int wid = mc.field_71466_p.func_78256_a(text) / 2 - 2;
            mc.field_71466_p.func_175063_a(text, sr.func_78326_a() / 2.0F - wid, sr.func_78328_b() / 2.0F + 13.0F, -1);
        }
    }

    private void gammaNoSlow() {
        if (mc.field_71439_g.field_70122_E) {
            this.offGroundTicks = 0;
        } else {
            this.offGroundTicks++;
        }

        if (this.offGroundTicks == 2 && this.send) {
            this.send = false;
            PacketUtil.sendPacketNoEvent(
                new C08PacketPlayerBlockPlacement(
                    new BlockPos(-1, -1, -1), 255, mc.field_71439_g.func_70694_bm(), 0.0F, 0.0F, 0.0F
                )
            );
        } else if (mc.field_71439_g.func_71039_bw() && this.isConsumable(mc.field_71439_g.func_70694_bm())) {
            mc.field_71439_g.field_70163_u += 1.0E-14;
        }
    }

    @EventTarget
    public void onPacketSent(PacketEvent event) {
        if (this.isEnabled()
            && event.getType() == EventType.SEND
            && mc.field_71439_g != null
            && mc.field_71441_e != null) {
            if (this.mode.getValue() == 0) {
                if (event.getPacket() instanceof C08PacketPlayerBlockPlacement) {
                    C08PacketPlayerBlockPlacement blockPlacement = (C08PacketPlayerBlockPlacement)event.getPacket();
                    if (blockPlacement.func_149568_f() == 255
                        && this.isConsumable(blockPlacement.func_149574_g())
                        && this.offGroundTicks < 2) {
                        if (mc.field_71439_g.field_70122_E && !this.doJump) {
                            KeyBindUtil.setKeyBindState(mc.field_71474_y.field_74314_A.func_151463_i(), false);
                        } else if (mc.field_71439_g.field_70122_E && this.doJump) {
                            KeyBindUtil.setKeyBindState(mc.field_71474_y.field_74314_A.func_151463_i(), false);
                            mc.field_71439_g.func_70664_aZ();
                        }

                        this.send = true;
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private boolean isConsumable(ItemStack itemStack) {
        return itemStack != null && itemStack.func_77973_b() != null
            ? itemStack.func_77973_b() instanceof ItemFood
                || itemStack.func_77973_b() instanceof ItemAppleGold
                || itemStack.func_77973_b() instanceof ItemSoup
                || itemStack.func_77973_b() instanceof ItemBow
            : false;
    }

    private void blinkNoSlow() {
        if (!mc.field_71439_g.func_71039_bw()) {
            this.disableBlink();
            this.ticks = 0;
        }

        if (mc.field_71439_g.func_71039_bw() && this.isConsumable(mc.field_71439_g.func_70694_bm())) {
            this.enableBlink();
            this.ticks++;
        }
    }

    private void floatNoSlow() {
        if (this.conditions()) {
            mc.field_71439_g.field_70163_u += 1.0E-7;
        }
    }

    private boolean conditions() {
        return (this.holding("apple") || this.holding("potion")) && mc.field_71439_g.field_70122_E;
    }

    private boolean holding(String itemType) {
        ItemStack heldItem = mc.field_71439_g.func_70694_bm();
        return heldItem != null && heldItem.func_77973_b() != null
            ? heldItem.func_77973_b().func_77658_a().toLowerCase().contains(itemType)
            : false;
    }

    private boolean isBlinkEnabled() {
        Blink blink = (Blink)Miau.moduleManager.modules.get(Blink.class);
        return blink != null && blink.isEnabled();
    }

    private void enableBlink() {
        Blink blink = (Blink)Miau.moduleManager.modules.get(Blink.class);
        if (blink != null && !blink.isEnabled()) {
            blink.setEnabled(true);
        }
    }

    private void disableBlink() {
        Blink blink = (Blink)Miau.moduleManager.modules.get(Blink.class);
        if (blink != null && blink.isEnabled()) {
            blink.setEnabled(false);
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
