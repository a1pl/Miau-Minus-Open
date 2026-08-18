package miau.module.modules.player;

import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorPlayerControllerMP;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.PercentProperty;
import miau.util.player.PlayerUtil;
import miau.util.world.BlockUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class SpeedMine extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final IAccessorPlayerControllerMP accessor = (IAccessorPlayerControllerMP)mc.field_71442_b;
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Percentage", "Ticks"});
    public final PercentProperty speed = new PercentProperty("speed", 50);
    public final IntProperty ticks = new IntProperty("ticks", 1, 1, 100);
    public final BooleanProperty ignoringMiningFatigue = new BooleanProperty("ignore-mining-fatigue", false);
    public final BooleanProperty equalAirGroundDig = new BooleanProperty("equal-air-ground-dig", true);

    public SpeedMine() {
        super("SpeedMine", false);
    }

    @EventTarget
    public void onPreUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.ignoringMiningFatigue.getValue()) {
                mc.field_71439_g.func_82170_o(Potion.field_76419_f.func_76396_c());
            }

            this.accessor.setBlockHitDelay(0);
            double percentageFaster = 0.0;
            switch (this.mode.getValue()) {
                case 0:
                    percentageFaster = this.speed.getValue().doubleValue() / 100.0;
                    if (!mc.field_71439_g.field_70122_E
                        && mc.field_71439_g.field_70173_aa % 5 == 0
                        && this.equalAirGroundDig.getValue()) {
                        this.accessor.setCurBlockDamageMP(this.accessor.getCurBlockDamageMP() / 5.0F);
                        percentageFaster = 0.8;
                    }

                    if (PlayerUtil.blockRelativeToPlayer(0.0F, (float)mc.field_71439_g.field_70181_x, 0.0F)
                            != Blocks.field_150350_a
                        && !mc.field_71439_g.field_70122_E
                        && this.equalAirGroundDig.getValue()) {
                        this.accessor.setCurBlockDamageMP(this.accessor.getCurBlockDamageMP() * 5.0F);
                        percentageFaster -= 0.8;
                    }
                    break;
                case 1:
                    if (mc.field_71476_x != null && mc.field_71476_x.field_72313_a == MovingObjectType.BLOCK) {
                        BlockPos blockPos = mc.field_71476_x.func_178782_a();
                        float blockHardness = BlockUtil.getBlock(blockPos)
                            .func_180647_a(mc.field_71439_g, mc.field_71441_e, blockPos);
                        percentageFaster = blockHardness * this.ticks.getValue().intValue();
                    }

                    if (!mc.field_71439_g.field_70122_E
                        && mc.field_71439_g.field_70173_aa % 5 == 0
                        && this.equalAirGroundDig.getValue()) {
                        this.accessor.setCurBlockDamageMP(this.accessor.getCurBlockDamageMP() / 5.0F);
                        percentageFaster = 0.81;
                    }

                    if (PlayerUtil.blockRelativeToPlayer(0.0F, (float)mc.field_71439_g.field_70181_x, 0.0F)
                            != Blocks.field_150350_a
                        && !mc.field_71439_g.field_70122_E
                        && this.equalAirGroundDig.getValue()) {
                        this.accessor.setCurBlockDamageMP(this.accessor.getCurBlockDamageMP() * 5.0F);
                        percentageFaster -= 0.81;
                    }
            }

            float curDamage = this.accessor.getCurBlockDamageMP();
            if (curDamage > 1.0 - percentageFaster && curDamage < 0.99F) {
                this.accessor.setCurBlockDamageMP(0.99F);
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
