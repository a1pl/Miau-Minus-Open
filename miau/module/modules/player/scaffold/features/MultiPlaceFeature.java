package miau.module.modules.player.scaffold.features;

import java.util.Arrays;
import java.util.List;
import miau.event.impl.UpdateEvent;
import miau.module.modules.player.Scaffold;
import miau.module.modules.player.scaffold.ScaffoldComponent;
import miau.module.modules.player.scaffold.ScaffoldPlacementUtil;
import miau.property.Property;
import miau.property.properties.BooleanProperty;
import miau.util.player.RotationUtil;
import miau.util.world.BlockUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class MultiPlaceFeature implements ScaffoldComponent {
    private final Scaffold scaffold;
    private final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty multiplace = new BooleanProperty("multi-place", true);

    @Override
    public List<Property<?>> getProperties() {
        return Arrays.asList(this.multiplace);
    }

    public MultiPlaceFeature(Scaffold scaffold) {
        this.scaffold = scaffold;
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (this.multiplace.getValue()) {
            for (int i = 0; i < 3; i++) {
                Scaffold.BlockData blockData = this.scaffold.getBlockData();
                if (blockData == null) {
                    break;
                }

                MovingObjectPosition mop = ScaffoldPlacementUtil.verifyPlacement(
                    blockData, this.scaffold.yaw, this.scaffold.pitch
                );
                if (mop != null) {
                    this.scaffold.place(blockData.blockPos, blockData.facing, mop.field_72307_f);
                } else {
                    Vec3 hitVec = BlockUtil.getClickVec(blockData.blockPos, blockData.facing);
                    double dx = hitVec.field_72450_a - this.mc.field_71439_g.field_70165_t;
                    double dy = hitVec.field_72448_b
                        - this.mc.field_71439_g.field_70163_u
                        - this.mc.field_71439_g.func_70047_e();
                    double dz = hitVec.field_72449_c - this.mc.field_71439_g.field_70161_v;
                    float[] rotations = RotationUtil.getRotationsTo(dx, dy, dz, event.getYaw(), event.getPitch());
                    if (!(Math.abs(rotations[0] - this.scaffold.yaw) < 120.0F)
                        || !(Math.abs(rotations[1] - this.scaffold.pitch) < 60.0F)) {
                        break;
                    }

                    mop = ScaffoldPlacementUtil.verifyPlacement(blockData, rotations[0], rotations[1]);
                    if (mop == null) {
                        break;
                    }

                    this.scaffold.place(blockData.blockPos, blockData.facing, mop.field_72307_f);
                }
            }
        }
    }
}
