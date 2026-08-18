package miau.module.modules.render;

import java.awt.Color;
import java.util.stream.Collectors;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.Render3DEvent;
import miau.mixin.IAccessorMinecraft;
import miau.mixin.IAccessorRenderManager;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ColorProperty;
import miau.util.render.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

public class ChestESP extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final ColorProperty chest = new ColorProperty("chest", new Color(255, 170, 0).getRGB());
    public final ColorProperty trappedChest = new ColorProperty("trapped-chest", new Color(255, 43, 0).getRGB());
    public final ColorProperty enderChest = new ColorProperty("ender-chest", new Color(26, 17, 0).getRGB());
    public final BooleanProperty tracers = new BooleanProperty("tracers", false);

    public ChestESP() {
        super("ChestESP", false);
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled()) {
            RenderUtil.enableRenderState();

            for (TileEntity chest : mc.field_71441_e
                .field_147482_g
                .stream()
                .filter(
                    tileEntity -> tileEntity instanceof TileEntityChest || tileEntity instanceof TileEntityEnderChest
                )
                .collect(Collectors.toList())) {
                Block block = mc.field_71441_e.func_180495_p(chest.func_174877_v()).func_177230_c();
                double minZ = 0.0625;
                double minX = 0.0625;
                double maxZ = 0.9375;
                double maxX = 0.9375;
                Color color;
                if (block instanceof BlockChest) {
                    if (block.func_149744_f()) {
                        color = new Color(this.trappedChest.getValue());
                    } else {
                        color = new Color(this.chest.getValue());
                    }

                    EnumFacing facing = (EnumFacing)mc.field_71441_e
                        .func_180495_p(chest.func_174877_v())
                        .func_177229_b(BlockChest.field_176459_a);
                    switch (facing) {
                        case NORTH:
                            if (mc.field_71441_e.func_180495_p(chest.func_174877_v().func_177974_f()).func_177230_c()
                                == block) {
                                continue;
                            }

                            if (mc.field_71441_e.func_180495_p(chest.func_174877_v().func_177976_e()).func_177230_c()
                                == block) {
                                minX--;
                            }
                            break;
                        case SOUTH:
                            if (mc.field_71441_e.func_180495_p(chest.func_174877_v().func_177976_e()).func_177230_c()
                                == block) {
                                continue;
                            }

                            if (mc.field_71441_e.func_180495_p(chest.func_174877_v().func_177974_f()).func_177230_c()
                                == block) {
                                maxX++;
                            }
                            break;
                        case WEST:
                            if (mc.field_71441_e.func_180495_p(chest.func_174877_v().func_177978_c()).func_177230_c()
                                == block) {
                                continue;
                            }

                            if (mc.field_71441_e.func_180495_p(chest.func_174877_v().func_177968_d()).func_177230_c()
                                == block) {
                                maxZ++;
                            }
                            break;
                        case EAST:
                            if (mc.field_71441_e.func_180495_p(chest.func_174877_v().func_177968_d()).func_177230_c()
                                != block) {
                                if (mc.field_71441_e
                                        .func_180495_p(chest.func_174877_v().func_177978_c())
                                        .func_177230_c()
                                    == block) {
                                    minZ--;
                                }
                                break;
                            }
                        default:
                            continue;
                    }
                } else {
                    color = new Color(this.enderChest.getValue());
                }

                AxisAlignedBB aabb = new AxisAlignedBB(
                        chest.func_174877_v().func_177958_n() + minX,
                        chest.func_174877_v().func_177956_o() + 0.0,
                        chest.func_174877_v().func_177952_p() + minZ,
                        chest.func_174877_v().func_177958_n() + maxX,
                        chest.func_174877_v().func_177956_o() + 0.875,
                        chest.func_174877_v().func_177952_p() + maxZ
                    )
                    .func_72317_d(
                        -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX(),
                        -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY(),
                        -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ()
                    );
                RenderUtil.drawBoundingBox(aabb, color.getRed(), color.getGreen(), color.getBlue(), 255, 1.5F);
                if (this.tracers.getValue()) {
                    Vec3 vec;
                    if (mc.field_71474_y.field_74320_O == 0) {
                        vec = new Vec3(0.0, 0.0, 1.0)
                            .func_178789_a(
                                (float)(
                                    -Math.toRadians(
                                        RenderUtil.lerpFloat(
                                            mc.func_175606_aa().field_70125_A,
                                            mc.func_175606_aa().field_70127_C,
                                            ((IAccessorMinecraft)mc).getTimer().field_74281_c
                                        )
                                    )
                                )
                            )
                            .func_178785_b(
                                (float)(
                                    -Math.toRadians(
                                        RenderUtil.lerpFloat(
                                            mc.func_175606_aa().field_70177_z,
                                            mc.func_175606_aa().field_70126_B,
                                            ((IAccessorMinecraft)mc).getTimer().field_74281_c
                                        )
                                    )
                                )
                            );
                    } else {
                        vec = new Vec3(0.0, 0.0, 0.0)
                            .func_178789_a(
                                (float)(
                                    -Math.toRadians(
                                        RenderUtil.lerpFloat(
                                            mc.field_71439_g.field_70726_aT,
                                            mc.field_71439_g.field_70727_aS,
                                            ((IAccessorMinecraft)mc).getTimer().field_74281_c
                                        )
                                    )
                                )
                            )
                            .func_178785_b(
                                (float)(
                                    -Math.toRadians(
                                        RenderUtil.lerpFloat(
                                            mc.field_71439_g.field_71109_bG,
                                            mc.field_71439_g.field_71107_bF,
                                            ((IAccessorMinecraft)mc).getTimer().field_74281_c
                                        )
                                    )
                                )
                            );
                    }

                    vec = new Vec3(
                        vec.field_72450_a, vec.field_72448_b + mc.func_175606_aa().func_70047_e(), vec.field_72449_c
                    );
                    float opacity = ((Tracers)Miau.moduleManager.modules.get(Tracers.class))
                            .opacity
                            .getValue()
                            .intValue()
                        / 100.0F;
                    RenderUtil.drawLine3D(
                        vec,
                        chest.func_174877_v().func_177958_n() + 0.5,
                        chest.func_174877_v().func_177956_o() + 0.5,
                        chest.func_174877_v().func_177952_p() + 0.5,
                        color.getRed() / 255.0F,
                        color.getGreen() / 255.0F,
                        color.getBlue() / 255.0F,
                        opacity,
                        1.5F
                    );
                }
            }

            RenderUtil.disableRenderState();
        }
    }
}
