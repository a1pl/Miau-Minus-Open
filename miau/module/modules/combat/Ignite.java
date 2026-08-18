package miau.module.modules.combat;

import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.util.network.PacketUtil;
import miau.util.time.TimerUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class Ignite extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final BooleanProperty lighter = new BooleanProperty("Lighter", true);
    private final BooleanProperty lavaBucket = new BooleanProperty("Lava", true);
    private final TimerUtil msTimer = new TimerUtil();

    public Ignite() {
        super("Ignite", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            if (event.getType() == EventType.PRE) {
                if (this.msTimer.hasTimeElapsed(500L)) {
                    int lighterInHotbar = this.lighter.getValue() ? this.findItem(Items.field_151033_d) : -1;
                    int lavaInHotbar = this.lavaBucket.getValue() ? this.findItem(Items.field_151129_at) : -1;
                    int fireInHotbar = lighterInHotbar != -1 ? lighterInHotbar : lavaInHotbar;
                    if (fireInHotbar != -1) {
                        for (Object o : mc.field_71441_e.field_72996_f) {
                            if (o instanceof EntityLivingBase) {
                                EntityLivingBase entity = (EntityLivingBase)o;
                                if (entity != mc.field_71439_g
                                    && !entity.field_70128_L
                                    && !(entity instanceof EntityArmorStand)
                                    && !entity.func_70027_ad()) {
                                    BlockPos blockPos = new BlockPos(
                                        entity.field_70165_t, entity.field_70163_u, entity.field_70161_v
                                    );
                                    if (!(mc.field_71439_g.func_174818_b(blockPos) >= 22.3)) {
                                        Block block = mc.field_71441_e.func_180495_p(blockPos).func_177230_c();
                                        if (block instanceof BlockAir
                                            && block.func_176200_f(mc.field_71441_e, blockPos)) {
                                            Miau.slotComponent.setSlot(fireInHotbar, false);
                                            ItemStack itemStack = mc.field_71439_g
                                                .field_71071_by
                                                .func_70301_a(fireInHotbar);
                                            if (itemStack == null) {
                                                return;
                                            }

                                            if (itemStack.func_77973_b() instanceof ItemBucket) {
                                                float[] rotations = this.getRotations(
                                                    blockPos.func_177958_n() + 0.5,
                                                    blockPos.func_177956_o() + 0.5,
                                                    blockPos.func_177952_p() + 0.5
                                                );
                                                PacketUtil.sendPacket(
                                                    new C05PacketPlayerLook(
                                                        rotations[0], rotations[1], mc.field_71439_g.field_70122_E
                                                    )
                                                );
                                                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(itemStack));
                                            } else {
                                                for (EnumFacing side : EnumFacing.values()) {
                                                    BlockPos neighbor = blockPos.func_177972_a(side);
                                                    Block nBlock = mc.field_71441_e
                                                        .func_180495_p(neighbor)
                                                        .func_177230_c();
                                                    if (!(nBlock instanceof BlockAir) && nBlock.func_149730_j()) {
                                                        float[] rotations = this.getRotations(
                                                            neighbor.func_177958_n() + 0.5,
                                                            neighbor.func_177956_o() + 0.5,
                                                            neighbor.func_177952_p() + 0.5
                                                        );
                                                        PacketUtil.sendPacket(
                                                            new C05PacketPlayerLook(
                                                                rotations[0],
                                                                rotations[1],
                                                                mc.field_71439_g.field_70122_E
                                                            )
                                                        );
                                                        Vec3 hitVec = new Vec3(
                                                            side.func_176730_m().func_177958_n(),
                                                            side.func_176730_m().func_177956_o(),
                                                            side.func_176730_m().func_177952_p()
                                                        );
                                                        PacketUtil.sendPacket(
                                                            new C08PacketPlayerBlockPlacement(
                                                                neighbor,
                                                                side.func_176734_d().func_176745_a(),
                                                                itemStack,
                                                                (float)hitVec.field_72450_a,
                                                                (float)hitVec.field_72448_b,
                                                                (float)hitVec.field_72449_c
                                                            )
                                                        );
                                                        mc.field_71439_g.func_71038_i();
                                                        break;
                                                    }
                                                }
                                            }

                                            Miau.slotComponent
                                                .setSlot(mc.field_71439_g.field_71071_by.field_70461_c, false);
                                            PacketUtil.sendPacket(
                                                new C05PacketPlayerLook(
                                                    mc.field_71439_g.field_70177_z,
                                                    mc.field_71439_g.field_70125_A,
                                                    mc.field_71439_g.field_70122_E
                                                )
                                            );
                                            this.msTimer.reset();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int findItem(Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (stack != null && stack.func_77973_b() == item) {
                return i;
            }
        }

        return -1;
    }

    private float[] getRotations(double targetX, double targetY, double targetZ) {
        double diffX = targetX - mc.field_71439_g.field_70165_t;
        double diffY = targetY - (mc.field_71439_g.func_174813_aQ().field_72338_b + mc.field_71439_g.eyeHeight);
        double diffZ = targetZ - mc.field_71439_g.field_70161_v;
        double sqrtXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, sqrtXZ)));
        float finalYaw = mc.field_71439_g.field_70177_z + MathHelper.func_76142_g(yaw - mc.field_71439_g.field_70177_z);
        float finalPitch = mc.field_71439_g.field_70125_A
            + MathHelper.func_76142_g(pitch - mc.field_71439_g.field_70125_A);
        return new float[]{finalYaw, finalPitch};
    }
}
