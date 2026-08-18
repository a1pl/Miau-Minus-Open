package miau.module.modules.ghost;

import java.lang.reflect.Method;
import java.util.Objects;
import miau.event.EventTarget;
import miau.event.impl.LeftClickMouseEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.module.modules.render.Keystrokes;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.util.client.KeyBindUtil;
import miau.util.math.RandomUtil;
import miau.util.player.ItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;
import org.lwjgl.input.Mouse;

public class AutoClicker extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private boolean clickPending = false;
    private long clickDelay = 0L;
    public final FloatProperty cps = new FloatProperty("cps", 8.0F, 12.0F, 1.0F, 20.0F);
    public final BooleanProperty weaponsOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty breakBlocks = new BooleanProperty("break-blocks", true);
    public final BooleanProperty inventory = new BooleanProperty("inventory", false);

    private long getNextClickDelay() {
        return 1000L / RandomUtil.nextLong(this.cps.getValue().intValue(), this.cps.getSecondValue().intValue());
    }

    private boolean isBreakingBlock() {
        return mc.field_71476_x != null && mc.field_71476_x.field_72313_a == MovingObjectType.BLOCK;
    }

    private boolean canClick() {
        if (this.weaponsOnly.getValue() && !ItemUtil.hasRawUnbreakingEnchant() && !ItemUtil.isHoldingSword()) {
            return false;
        } else if (this.breakBlocks.getValue() && this.isBreakingBlock() && !this.hasValidTarget()) {
            GameType gameType12 = mc.field_71442_b.func_178889_l();
            return gameType12 != GameType.SURVIVAL && gameType12 != GameType.CREATIVE;
        } else {
            return true;
        }
    }

    private boolean isValidTarget(EntityPlayer entityPlayer) {
        if (entityPlayer == mc.field_71439_g || entityPlayer == mc.field_71439_g.field_70154_o) {
            return false;
        } else if (entityPlayer == mc.func_175606_aa() || entityPlayer == mc.func_175606_aa().field_70154_o) {
            return false;
        } else {
            return entityPlayer.field_70725_aQ > 0
                ? false
                : mc.field_71476_x != null && mc.field_71476_x.field_72308_g == entityPlayer;
        }
    }

    private boolean hasValidTarget() {
        return mc.field_71441_e
            .field_72996_f
            .stream()
            .filter(e -> e instanceof EntityPlayer)
            .map(e -> (EntityPlayer)e)
            .anyMatch(this::isValidTarget);
    }

    public AutoClicker() {
        super("AutoClicker", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.clickDelay > 0L) {
                this.clickDelay -= 50L;
            }

            if (mc.field_71462_r != null && !this.inventory.getValue()) {
                this.clickPending = false;
            } else {
                if (this.clickPending) {
                    this.clickPending = false;
                    if (mc.field_71462_r == null) {
                        KeyBindUtil.updateKeyState(mc.field_71474_y.field_74312_F.func_151463_i());
                    }
                }

                boolean isMouseDown = Mouse.isButtonDown(0);
                if (this.isEnabled() && this.canClick() && isMouseDown && !mc.field_71439_g.func_71039_bw()) {
                    while (this.clickDelay <= 0L) {
                        this.clickPending = true;
                        this.clickDelay = this.clickDelay + this.getNextClickDelay();
                        if (mc.field_71462_r != null) {
                            long time = System.currentTimeMillis();

                            try {
                                Method m = GuiScreen.class
                                    .getDeclaredMethod("mouseClicked", int.class, int.class, int.class);
                                m.setAccessible(true);
                                int mouseX = Mouse.getEventX() * mc.field_71462_r.field_146294_l / mc.field_71443_c;
                                int mouseY = mc.field_71462_r.field_146295_m
                                    - Mouse.getEventY() * mc.field_71462_r.field_146295_m / mc.field_71440_d
                                    - 1;
                                m.invoke(mc.field_71462_r, mouseX, mouseY, 0);
                            } catch (Exception var8) {
                            }
                        } else {
                            KeyBindUtil.setKeyBindState(mc.field_71474_y.field_74312_F.func_151463_i(), false);
                            KeyBindUtil.pressKeyOnce(mc.field_71474_y.field_74312_F.func_151463_i());
                            Keystrokes.recordLeftClick();
                        }
                    }
                }
            }
        }
    }

    @EventTarget(4)
    public void onCLick(LeftClickMouseEvent event) {
        if (this.isEnabled() && !event.isCancelled() && !this.clickPending) {
            this.clickDelay = this.clickDelay + this.getNextClickDelay();
        }
    }

    @Override
    public void onEnabled() {
        this.clickDelay = 0L;
    }

    @Override
    public String[] getSuffix() {
        return Objects.equals(this.cps.getValue(), this.cps.getSecondValue())
            ? new String[]{String.valueOf(this.cps.getValue().intValue())}
            : new String[]{
                String.format("%d-%d", this.cps.getValue().intValue(), this.cps.getSecondValue().intValue())
            };
    }
}
