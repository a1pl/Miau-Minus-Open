package miau.module.modules.player.scaffold.features;

import java.util.Arrays;
import java.util.List;
import miau.module.modules.player.Scaffold;
import miau.module.modules.player.scaffold.ScaffoldComponent;
import miau.property.Property;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.util.math.RandomUtil;
import miau.util.player.PlayerUtil;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;

public class SneakFeature implements ScaffoldComponent {
    private final Scaffold scaffold;
    public int sneakingTicks = -1;
    public int placements = 0;
    public int pause = 0;
    public int slow = 0;
    public int ticksOnAir = 0;
    private boolean silentSneaking = false;
    public final ModeProperty sneakMode = new ModeProperty("sneak-mode", 0, new String[]{"OFF", "NORMAL", "SILENT"});
    public final FloatProperty startSneaking = new FloatProperty(
        "start-sneaking", 0.0F, 0.0F, 5.0F, () -> this.sneakMode.getValue() != 0
    );
    public final FloatProperty stopSneaking = new FloatProperty(
        "stop-sneaking", 0.0F, 0.0F, 5.0F, () -> this.sneakMode.getValue() != 0
    );
    public final IntProperty sneakEvery = new IntProperty("sneak-every", 1, 1, 10, () -> this.sneakMode.getValue() != 0);
    public final FloatProperty sneakingSpeed = new FloatProperty(
        "sneaking-speed", 0.2F, 0.2F, 1.0F, () -> this.sneakMode.getValue() != 0
    );

    public SneakFeature(Scaffold scaffold) {
        this.scaffold = scaffold;
    }

    @Override
    public List<Property<?>> getProperties() {
        return Arrays.asList(this.sneakMode, this.startSneaking, this.stopSneaking, this.sneakEvery, this.sneakingSpeed);
    }

    private void setSneakingState(boolean state) {
        Minecraft mc = Minecraft.func_71410_x();
        if (mc.field_71439_g != null) {
            int mode = this.sneakMode.getValue();
            if (mode == 1) {
                if (this.silentSneaking) {
                    mc.field_71439_g
                        .field_71174_a
                        .func_147297_a(new C0BPacketEntityAction(mc.field_71439_g, Action.STOP_SNEAKING));
                    this.silentSneaking = false;
                }

                KeyBinding.func_74510_a(mc.field_71474_y.field_74311_E.func_151463_i(), state);
            } else if (mode == 2) {
                KeyBinding.func_74510_a(mc.field_71474_y.field_74311_E.func_151463_i(), false);
                if (state) {
                    if (!this.silentSneaking) {
                        mc.field_71439_g
                            .field_71174_a
                            .func_147297_a(new C0BPacketEntityAction(mc.field_71439_g, Action.START_SNEAKING));
                        this.silentSneaking = true;
                    }
                } else if (this.silentSneaking) {
                    mc.field_71439_g
                        .field_71174_a
                        .func_147297_a(new C0BPacketEntityAction(mc.field_71439_g, Action.STOP_SNEAKING));
                    this.silentSneaking = false;
                }
            } else {
                if (this.silentSneaking) {
                    mc.field_71439_g
                        .field_71174_a
                        .func_147297_a(new C0BPacketEntityAction(mc.field_71439_g, Action.STOP_SNEAKING));
                    this.silentSneaking = false;
                }

                KeyBinding.func_74510_a(mc.field_71474_y.field_74311_E.func_151463_i(), false);
            }
        }
    }

    public void calculateSneaking() {
        if (this.sneakMode.getValue() == 0 && this.pause <= 0) {
            this.setSneakingState(false);
        } else {
            if (this.ticksOnAir == 0 && this.sneakingTicks < 0) {
                this.setSneakingState(false);
            }

            this.sneakingTicks--;
            int ahead = (int)this.startSneaking.getValue().floatValue();
            int place = (int)RandomUtil.nextFloat(
                this.scaffold.options.placeDelay.getValue(), this.scaffold.options.placeDelay.getSecondValue()
            );
            int after = (int)this.stopSneaking.getValue().floatValue();
            if (this.pause > 0) {
                this.pause--;
                this.sneakingTicks = 0;
                this.placements = 0;
            }

            if (this.sneakingTicks >= 0) {
                this.setSneakingState(true);
            } else {
                if (this.ticksOnAir > 0) {
                    this.sneakingTicks = after;
                }

                if ((
                        this.ticksOnAir > 0
                            || PlayerUtil.blockRelativeToPlayer(
                                Minecraft.func_71410_x().field_71439_g.field_70159_w * ahead,
                                1.0,
                                Minecraft.func_71410_x().field_71439_g.field_70179_y * ahead
                            ) instanceof BlockAir
                    )
                    && this.placements <= 0) {
                    this.sneakingTicks = ahead + place + after;
                    this.placements = this.sneakEvery.getValue();
                }

                if (this.sneakingTicks < 0) {
                    this.setSneakingState(false);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.setSneakingState(false);
        this.sneakingTicks = -1;
        this.placements = 0;
        this.pause = 0;
        this.slow = 0;
        this.ticksOnAir = 0;
    }
}
