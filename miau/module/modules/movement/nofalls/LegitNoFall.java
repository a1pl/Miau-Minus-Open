package miau.module.modules.movement.nofalls;

import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.modules.movement.NoFall;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class LegitNoFall extends NoFallMode {
    private int lastMlgSlot = -1;
    private boolean mlgPlaced = false;

    public LegitNoFall(String name, NoFall parent) {
        super(name, parent);
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            this.handleLegitMlg();
        }
    }

    private void handleLegitMlg() {
        if (mc.field_71439_g != null && mc.field_71441_e != null && mc.field_71442_b != null) {
            if (!mc.field_71439_g.field_70122_E
                && !mc.field_71439_g.field_71075_bZ.field_75100_b
                && !mc.field_71439_g.func_70090_H()
                && !mc.field_71439_g.func_70617_f_()) {
                if (!(mc.field_71439_g.field_70143_R < this.parent.distance.getValue())
                    && !(mc.field_71439_g.field_70181_x >= -0.1)) {
                    int waterSlot = this.findWaterBucketSlot();
                    if (waterSlot != -1) {
                        BlockPos target = this.findMlgTarget();
                        if (target != null) {
                            if (this.lastMlgSlot == -1) {
                                this.lastMlgSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                            }

                            mc.field_71439_g.field_71071_by.field_70461_c = waterSlot;
                            mc.field_71442_b.func_78765_e();
                            mc.field_71439_g.field_70125_A = 90.0F;
                            if (!this.mlgPlaced
                                && mc.field_71439_g
                                        .func_70011_f(
                                            target.func_177958_n() + 0.5,
                                            target.func_177956_o() + 0.5,
                                            target.func_177952_p() + 0.5
                                        )
                                    <= mc.field_71442_b.func_78757_d() + 1.5F) {
                                Vec3 hitVec = new Vec3(
                                    target.func_177958_n() + 0.5,
                                    target.func_177956_o() + 1.0,
                                    target.func_177952_p() + 0.5
                                );
                                ItemStack stack = mc.field_71439_g.field_71071_by.func_70448_g();
                                if (stack != null
                                    && mc.field_71442_b
                                        .func_178890_a(
                                            mc.field_71439_g, mc.field_71441_e, stack, target, EnumFacing.UP, hitVec
                                        )) {
                                    mc.field_71439_g.func_71038_i();
                                    this.mlgPlaced = true;
                                }
                            }
                        }
                    }
                }
            } else {
                this.resetLegitMlg();
            }
        }
    }

    private int findWaterBucketSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.field_70462_a[i];
            if (stack != null && stack.func_77973_b() == Items.field_151131_as) {
                return i;
            }
        }

        return -1;
    }

    private BlockPos findMlgTarget() {
        BlockPos playerPos = new BlockPos(
            mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v
        );

        for (int y = 1; y <= 6; y++) {
            BlockPos pos = playerPos.func_177979_c(y);
            if (!mc.field_71441_e.func_175623_d(pos) && mc.field_71441_e.func_175623_d(pos.func_177984_a())) {
                return pos;
            }
        }

        MovingObjectPosition ray = mc.field_71441_e
            .func_147447_a(
                new Vec3(
                    mc.field_71439_g.field_70165_t,
                    mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e(),
                    mc.field_71439_g.field_70161_v
                ),
                new Vec3(
                    mc.field_71439_g.field_70165_t,
                    mc.field_71439_g.field_70163_u - mc.field_71442_b.func_78757_d() - 2.0,
                    mc.field_71439_g.field_70161_v
                ),
                false,
                true,
                false
            );
        return ray != null && ray.field_72313_a == MovingObjectType.BLOCK ? ray.func_178782_a() : null;
    }

    private void resetLegitMlg() {
        if (this.lastMlgSlot != -1 && mc.field_71439_g != null) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.lastMlgSlot;
            mc.field_71442_b.func_78765_e();
        }

        this.lastMlgSlot = -1;
        this.mlgPlaced = false;
    }

    @Override
    public void onDisable() {
        this.resetLegitMlg();
    }
}
