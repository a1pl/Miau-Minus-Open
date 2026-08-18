package miau.module.modules.player;

import java.util.ArrayList;
import java.util.List;
import miau.event.EventTarget;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.util.time.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;

public class AutoSoup extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final FloatProperty delay = new FloatProperty("delay(ms)", 50.0F, 100.0F, 0.0F, 200.0F);
    public final FloatProperty coolDown = new FloatProperty("cooldown(ms)", 1000.0F, 1200.0F, 0.0F, 5000.0F);
    public final FloatProperty health = new FloatProperty("health", 7.0F, 0.0F, 20.0F);
    public final BooleanProperty invConsume = new BooleanProperty("consume in inv", false);
    public final BooleanProperty autoRefill = new BooleanProperty("auto refil", true);
    public final FloatProperty invWait = new FloatProperty("invWait(ms)", 50.0F, 100.0F, 0.0F, 200.0F);
    public final FloatProperty invCoolDown = new FloatProperty("refill delay(ms)", 50.0F, 100.0F, 0.0F, 200.0F);
    private final TimerUtil cdTimer = new TimerUtil();
    private final TimerUtil invCdTimer = new TimerUtil();
    private final TimerUtil eatTimer = new TimerUtil();
    private AutoSoup.State state = AutoSoup.State.WAITINGTOSWITCH;
    private int originalSlot;
    private boolean inInv;
    private List<Integer> sortedSlots = new ArrayList<>();
    private float ranDelay;
    private int soupSlot;

    public AutoSoup() {
        super("AutoSoup", false);
    }

    @Override
    public void onDisabled() {
        this.state = AutoSoup.State.WAITINGTOSWITCH;
        this.inInv = false;
        super.onDisabled();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null && mc.field_71441_e != null) {
                if ((this.invConsume.getValue() || mc.field_71462_r == null)
                    && mc.field_71439_g.func_110143_aJ() < this.health.getValue()
                    && this.cdTimer.hasTimeElapsed((long)this.ranDelay)) {
                    switch (this.state) {
                        case WAITINGTOSWITCH:
                            this.ranDelay = randomRange(this.delay);
                            this.state = AutoSoup.State.NONE;
                            break;
                        case NONE:
                            this.soupSlot = this.getSoupSlot();
                            if (this.soupSlot == -1) {
                                return;
                            }

                            this.originalSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                            mc.field_71439_g.field_71071_by.field_70461_c = this.soupSlot;
                            this.ranDelay = randomRange(this.delay);
                            this.state = AutoSoup.State.SWITCHED;
                            break;
                        case SWITCHED:
                            mc.field_71439_g.field_71071_by.field_70461_c = this.soupSlot;
                            mc.field_71442_b
                                .func_78769_a(
                                    mc.field_71439_g,
                                    mc.field_71441_e,
                                    mc.field_71439_g.field_71071_by.func_70301_a(this.soupSlot)
                                );
                            this.eatTimer.reset();
                            this.state = AutoSoup.State.EATING;
                            break;
                        case EATING:
                            if (mc.field_71439_g.func_71057_bx() >= 4 && this.isHeldItemSoup()) {
                                if (this.eatTimer.hasTimeElapsed(2000L)) {
                                    this.state = AutoSoup.State.DROPPING;
                                    this.ranDelay = randomRange(this.delay);
                                }
                                break;
                            }

                            this.state = AutoSoup.State.DROPPING;
                            this.ranDelay = randomRange(this.delay);
                            break;
                        case DROPPING:
                            mc.field_71442_b
                                .func_78769_a(
                                    mc.field_71439_g,
                                    mc.field_71441_e,
                                    mc.field_71439_g.field_71071_by.func_70301_a(this.soupSlot)
                                );
                            mc.field_71439_g.field_71071_by.field_70461_c = this.originalSlot;
                            this.ranDelay = randomRange(this.coolDown);
                            this.state = AutoSoup.State.WAITINGTOSWITCH;
                    }

                    this.cdTimer.reset();
                }

                if (this.autoRefill.getValue() && mc.field_71462_r instanceof GuiInventory) {
                    if (!this.inInv) {
                        this.ranDelay = randomRange(this.invWait);
                        this.invCdTimer.reset();
                        this.generateSlots();
                        this.inInv = true;
                    }

                    if (!this.sortedSlots.isEmpty() && this.invCdTimer.hasTimeElapsed((long)this.ranDelay)) {
                        mc.field_71442_b
                            .func_78753_a(
                                mc.field_71439_g.field_71070_bA.field_75152_c,
                                this.sortedSlots.get(0),
                                0,
                                1,
                                mc.field_71439_g
                            );
                        this.ranDelay = randomRange(this.invCoolDown);
                        this.invCdTimer.reset();
                        this.sortedSlots.remove(0);
                    }
                } else {
                    this.inInv = false;
                }
            }
        }
    }

    private boolean isHeldItemSoup() {
        ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(this.soupSlot);
        return stack != null && stack.func_77973_b() instanceof ItemSoup;
    }

    private void generateSlots() {
        List<Integer> slots = new ArrayList<>();
        int slotsNeeded = 0;

        for (int i = 0; i <= 8; i++) {
            if (mc.field_71439_g.field_71071_by.func_70301_a(i) == null) {
                slotsNeeded++;
            }
        }

        for (int i = 0;
            i < mc.field_71439_g.field_71069_bz.func_75138_a().size()
                && (slots.isEmpty() || slots.size() < slotsNeeded);
            i++
        ) {
            ItemStack stack = (ItemStack)mc.field_71439_g.field_71069_bz.func_75138_a().get(i);
            if (stack != null
                && (stack.func_77973_b() instanceof ItemSoup || stack.func_77973_b() == Items.field_151009_A)
                && (i < 36 || i > 44)) {
                slots.add(i);
            }
        }

        this.sortedSlots = slots;
    }

    private int getSoupSlot() {
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack itemInSlot = mc.field_71439_g.field_71071_by.func_70301_a(slot);
            if (itemInSlot != null
                && (itemInSlot.func_77973_b() instanceof ItemSoup || itemInSlot.func_77973_b() == Items.field_151009_A)
                )
             {
                return slot;
            }
        }

        return -1;
    }

    private static float randomRange(FloatProperty prop) {
        if (prop.isDoubleSlider() && prop.getSecondValue() != null) {
            float min = prop.getValue();
            float max = prop.getSecondValue();
            if (min > max) {
                float temp = min;
                min = max;
                max = temp;
            }

            return min + (float)Math.random() * (max - min);
        } else {
            return prop.getValue();
        }
    }

    public boolean isHealing() {
        return this.state != AutoSoup.State.WAITINGTOSWITCH && this.state != AutoSoup.State.NONE;
    }

    private enum State {
        WAITINGTOSWITCH,
        NONE,
        SWITCHED,
        EATING,
        DROPPING;
    }
}
