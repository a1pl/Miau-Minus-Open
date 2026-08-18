package miau.module.modules.ghost;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.module.modules.movement.Sprint;
import miau.module.modules.player.Scaffold;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.player.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class Clutch extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty hurtTime = new BooleanProperty("hurt-time", true);
    public final BooleanProperty rotations = new BooleanProperty("rotations", false);
    public final FloatProperty rotationSpeed = new FloatProperty(
        "rotation-speed", 100.0F, 1.0F, 180.0F, this.rotations::getValue
    );
    public final ModeProperty turnOffMode = new ModeProperty("disable-mode", 0, new String[]{"MOVE", "FORWARD", "NONE"});
    public final IntProperty enableDelay = new IntProperty("enable-delay", 0, 0, 100);
    public final IntProperty disableDelay = new IntProperty("disable-delay", 0, 0, 100);
    public final IntProperty sprintReEnable = new IntProperty("sprint-re-enable", 150, 0, 500);
    public final IntProperty minFallDist = new IntProperty("min-fall-distance", 0, 0, 10);
    private boolean falling = false;
    private boolean suppressed = false;
    private boolean needsSprint = false;
    private boolean fireballActive = false;
    private boolean fireballDelayActive = false;
    private int fireballDelayTicks = 0;
    private long airTimeStart = 0L;
    private long landTimeStart = 0L;
    private long sprintTimer = 0L;
    private long lastClutchEndTime = 0L;
    private static final int FALL_SCAN_DEPTH = 10;
    private static final long CHAIN_WINDOW_MS = 750L;

    public Clutch() {
        super("Clutch", false);
    }

    @Override
    public void onEnabled() {
        this.falling = false;
        this.suppressed = false;
        this.needsSprint = false;
        this.fireballActive = false;
        this.fireballDelayActive = false;
        this.fireballDelayTicks = 0;
        this.airTimeStart = 0L;
        this.landTimeStart = 0L;
        this.sprintTimer = 0L;
        this.lastClutchEndTime = 0L;
    }

    @Override
    public void onDisabled() {
        if (this.falling) {
            this.disableScaffold();
        }

        this.falling = false;
        this.suppressed = false;
        this.needsSprint = false;
        this.airTimeStart = 0L;
        this.landTimeStart = 0L;
        this.sprintTimer = 0L;
        this.lastClutchEndTime = 0L;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()) {
            if (event.getType() == EventType.PRE) {
                if (mc.field_71439_g != null && mc.field_71441_e != null) {
                    if (mc.field_71439_g.func_175149_v()) {
                        if (this.isScaffoldEnabled()) {
                            this.disableScaffold();
                        }
                    } else {
                        long now = System.currentTimeMillis();
                        int dist = this.fallDistance();
                        int minFall = this.minFallDist.getValue();
                        long enableDly = this.enableDelay.getValue().intValue();
                        long disableDly = this.disableDelay.getValue().intValue();
                        long sprintDly = this.sprintReEnable.getValue().intValue();
                        this.updateFireballState();
                        this.handleTurnoff(now);
                        if (!mc.field_71439_g.field_70122_E && mc.field_71439_g.field_70181_x < -0.01) {
                            if (this.airTimeStart == 0L) {
                                this.airTimeStart = now;
                            }

                            boolean inChainWindow = this.lastClutchEndTime != 0L && now - this.lastClutchEndTime < 750L;
                            boolean shouldTrigger;
                            if (inChainWindow) {
                                shouldTrigger = true;
                            } else if (minFall == 0) {
                                shouldTrigger = dist == -1;
                            } else {
                                shouldTrigger = dist == -1 || dist >= minFall;
                            }

                            if (this.hurtTime.getValue()) {
                                shouldTrigger = shouldTrigger && mc.field_71439_g.field_70737_aN > 0;
                            }

                            if (shouldTrigger
                                && !this.suppressed
                                && !this.isScaffoldEnabled()
                                && !mc.field_71439_g.field_71075_bZ.field_75100_b
                                && !this.fireballActive
                                && now - this.airTimeStart >= enableDly) {
                                this.setScaffold(true);
                                this.setSprint(false);
                                this.falling = true;
                                this.needsSprint = true;
                                this.sprintTimer = 0L;
                                this.lastClutchEndTime = 0L;
                            }
                        } else if (mc.field_71439_g.field_70122_E) {
                            this.airTimeStart = 0L;
                            this.suppressed = false;
                            if (!this.falling) {
                                this.landTimeStart = 0L;
                            }
                        }

                        if (this.falling && this.rotations.getValue()) {
                            float[] smoothed = RotationUtil.smooth(
                                new float[]{mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A},
                                new float[]{mc.field_71439_g.field_70177_z, 90.0F},
                                this.rotationSpeed.getValue().floatValue(),
                                null,
                                0.0
                            );
                            mc.field_71439_g.field_70125_A = smoothed[1];
                        }

                        if (this.falling && (mc.field_71439_g.field_70122_E || dist != -1 && dist < 1)) {
                            if (this.landTimeStart == 0L) {
                                this.landTimeStart = now;
                            }

                            if (this.isScaffoldEnabled() && now - this.landTimeStart >= disableDly) {
                                this.stopClutch(now, false);
                            }
                        }

                        if (!this.falling
                            && this.needsSprint
                            && this.sprintTimer != 0L
                            && now - this.sprintTimer >= sprintDly) {
                            this.setSprint(true);
                            this.needsSprint = false;
                            this.sprintTimer = 0L;
                        }
                    }
                }
            }
        }
    }

    private void updateFireballState() {
        ItemStack held = mc.field_71439_g.func_70694_bm();
        if (held != null
            && held.func_77977_a().contains("fire_charge")
            && mc.field_71474_y.field_74313_G.func_151470_d()) {
            this.fireballActive = true;
            if (!this.fireballDelayActive) {
                this.fireballDelayActive = true;
                this.fireballDelayTicks = 20;
            }
        }

        if (this.fireballDelayActive && this.fireballDelayTicks > 0) {
            this.fireballDelayTicks--;
            if (this.fireballDelayTicks < 2) {
                this.fireballActive = false;
                this.fireballDelayActive = false;
            }
        }
    }

    private void handleTurnoff(long now) {
        if (this.falling) {
            int mode = this.turnOffMode.getValue();
            switch (mode) {
                case 0:
                    if (this.isMoving() || this.isAnyMovementKeyDown()) {
                        this.stopClutch(now, true);
                    }
                    break;
                case 1:
                    if (mc.field_71474_y.field_74351_w.func_151470_d()) {
                        this.stopClutch(now, true);
                    }
                case 2:
            }
        }
    }

    private void stopClutch(long now, boolean suppress) {
        this.disableScaffold();
        this.falling = false;
        this.suppressed = suppress;
        this.landTimeStart = 0L;
        this.lastClutchEndTime = now;
        if (this.needsSprint) {
            this.sprintTimer = now;
        }
    }

    private boolean isScaffoldEnabled() {
        Module mod = Miau.moduleManager.modules.get(Scaffold.class);
        return mod != null && mod.isEnabled();
    }

    private void setScaffold(boolean enabled) {
        Module mod = Miau.moduleManager.modules.get(Scaffold.class);
        if (mod != null && mod.isEnabled() != enabled) {
            mod.toggle();
        }
    }

    private void disableScaffold() {
        Module mod = Miau.moduleManager.modules.get(Scaffold.class);
        if (mod != null && mod.isEnabled()) {
            mod.toggle();
        }
    }

    private void setSprint(boolean enabled) {
        Module mod = Miau.moduleManager.modules.get(Sprint.class);
        if (mod != null && mod.isEnabled() != enabled) {
            mod.toggle();
        }
    }

    private int fallDistance() {
        Vec3 pos = mc.field_71439_g.func_174791_d();
        int startY = MathHelper.func_76128_c(pos.field_72448_b) - 1;
        if (startY < 0) {
            return -1;
        }

        int minY = Math.max(0, startY - 10);
        int px = MathHelper.func_76128_c(pos.field_72450_a);
        int pz = MathHelper.func_76128_c(pos.field_72449_c);

        for (int i = startY; i >= minY; i--) {
            Block block = mc.field_71441_e.func_180495_p(new BlockPos(px, i, pz)).func_177230_c();
            if (block != Blocks.field_150350_a) {
                String name = block.func_149739_a();
                if (!name.contains("sign") && !name.contains("torch")) {
                    return startY - i;
                }
            }
        }

        return -1;
    }

    private boolean isMoving() {
        return mc.field_71439_g.field_71158_b.field_78900_b != 0.0F
            || mc.field_71439_g.field_71158_b.field_78902_a != 0.0F;
    }

    private boolean isAnyMovementKeyDown() {
        return mc.field_71474_y.field_74351_w.func_151470_d()
            || mc.field_71474_y.field_74368_y.func_151470_d()
            || mc.field_71474_y.field_74370_x.func_151470_d()
            || mc.field_71474_y.field_74366_z.func_151470_d()
            || mc.field_71474_y.field_74314_A.func_151470_d()
            || mc.field_71474_y.field_74311_E.func_151470_d();
    }
}
