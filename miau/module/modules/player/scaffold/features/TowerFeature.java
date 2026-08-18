package miau.module.modules.player.scaffold.features;

import java.util.Arrays;
import java.util.List;
import miau.event.impl.StrafeEvent;
import miau.event.impl.UpdateEvent;
import miau.module.modules.player.Scaffold;
import miau.module.modules.player.scaffold.ScaffoldComponent;
import miau.property.Property;
import miau.property.properties.ModeProperty;
import miau.util.player.ItemUtil;
import miau.util.player.MoveUtil;
import miau.util.player.PlayerUtil;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;

public class TowerFeature implements ScaffoldComponent {
    private final Scaffold scaffold;
    public final ModeProperty tower = new ModeProperty("tower", 0, new String[]{"NONE", "VANILLA", "TELLY"});

    public TowerFeature(Scaffold scaffold) {
        this.scaffold = scaffold;
    }

    @Override
    public List<Property<?>> getProperties() {
        return Arrays.asList(this.tower);
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (Scaffold.mc.field_71439_g.field_70122_E) {
            this.scaffold.towering = false;
        }
    }

    @Override
    public void onStrafe(StrafeEvent event) {
        if (!Scaffold.mc.field_71439_g.field_70123_F
            && Scaffold.mc.field_71439_g.field_70737_aN <= 5
            && !Scaffold.mc.field_71439_g.func_70644_a(Potion.field_76430_j)
            && Scaffold.mc.field_71474_y.field_74314_A.func_151470_d()
            && ItemUtil.isHoldingBlock()) {
            int yState = (int)(Scaffold.mc.field_71439_g.field_70163_u % 1.0 * 100.0);
            switch (this.tower.getValue()) {
                case 1:
                    this.handleVanillaTower(event, yState);
                    break;
                default:
                    this.scaffold.towerTick = 0;
                    this.scaffold.towerDelay = 0;
            }
        } else {
            this.scaffold.towerTick = 0;
            this.scaffold.towerDelay = 0;
        }
    }

    private void handleVanillaTower(StrafeEvent event, int yState) {
        switch (this.scaffold.towerTick) {
            case 0:
                if (Scaffold.mc.field_71439_g.field_70122_E) {
                    this.scaffold.towerTick = 1;
                    Scaffold.mc.field_71439_g.field_70181_x = -0.0784000015258789;
                }

                return;
            case 1:
                if (yState == 0 && PlayerUtil.isAirBelow()) {
                    this.scaffold.startY = MathHelper.func_76128_c(Scaffold.mc.field_71439_g.field_70163_u);
                    this.scaffold.towerTick = 2;
                    Scaffold.mc.field_71439_g.field_70181_x = 0.42F;
                    if (MoveUtil.isForwardPressed()) {
                        MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                    } else {
                        MoveUtil.setSpeed(0.0);
                        event.setForward(0.0F);
                        event.setStrafe(0.0F);
                    }

                    return;
                }

                this.scaffold.towerTick = 0;
                return;
            case 2:
                this.scaffold.towerTick = 3;
                Scaffold.mc.field_71439_g.field_70181_x = 0.75 - Scaffold.mc.field_71439_g.field_70163_u % 1.0;
                return;
            case 3:
                this.scaffold.towerTick = 1;
                Scaffold.mc.field_71439_g.field_70181_x = 1.0 - Scaffold.mc.field_71439_g.field_70163_u % 1.0;
                return;
            default:
                this.scaffold.towerTick = 0;
        }
    }
}
