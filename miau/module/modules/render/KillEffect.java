package miau.module.modules.render;

import miau.event.EventTarget;
import miau.event.impl.AttackEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;

public class KillEffect extends Module {
    private final BooleanProperty lightning = new BooleanProperty("Lightning", true);
    private final BooleanProperty blood = new BooleanProperty("Blood Explosion", true);
    private final BooleanProperty explosion = new BooleanProperty("Explosion", true);
    private final BooleanProperty fireSmoke = new BooleanProperty("Fire N Smoke", true);
    private EntityLivingBase target;
    private static final Minecraft mc = Minecraft.func_71410_x();

    public KillEffect() {
        super("KillEffect", false, true);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE
            && this.target != null
            && !mc.field_71441_e.field_72996_f.contains(this.target)) {
            if (this.lightning.getValue()) {
                EntityLightningBolt entityLightningBolt = new EntityLightningBolt(
                    mc.field_71441_e, this.target.field_70165_t, this.target.field_70163_u, this.target.field_70161_v
                );
                mc.field_71441_e.func_73027_a((int)(-Math.random() * 100000.0), entityLightningBolt);
                mc.field_71439_g.func_85030_a("ambient.weather.thunder", 1.0F, 1.0F);
            }

            if (this.explosion.getValue()) {
                for (int i = 0; i <= 8; i++) {
                    mc.field_71452_i.func_178926_a(this.target, EnumParticleTypes.FLAME);
                }

                mc.field_71439_g.func_85030_a("item.fireCharge.use", 1.0F, 1.0F);
            }

            if (this.blood.getValue()) {
                double startY = this.target.field_70163_u;
                double endY = this.target.field_70163_u + this.target.field_70131_O + 0.4;
                double step = 0.4;

                for (int i = 0; i < 100; i++) {
                    for (double y = startY; y <= endY; y += step) {
                        mc.field_71441_e
                            .func_175688_a(
                                EnumParticleTypes.BLOCK_CRACK,
                                this.target.field_70165_t,
                                y,
                                this.target.field_70161_v,
                                0.0,
                                0.0,
                                0.0,
                                new int[]{Block.func_176210_f(Blocks.field_150451_bX.func_176223_P())}
                            );
                    }
                }

                for (double y = startY; y <= endY; y += step) {
                    mc.field_71439_g.func_85030_a("dig.stone", 1.0F, 1.0F);
                }
            }

            if (this.fireSmoke.getValue()) {
                for (int i = 0; i < 25; i++) {
                    double offsetX = (Math.random() - 0.5) * this.target.field_70130_N * 1.5;
                    double offsetZ = (Math.random() - 0.5) * this.target.field_70130_N * 1.5;
                    double offsetY = Math.random() * this.target.field_70131_O;
                    mc.field_71441_e
                        .func_175688_a(
                            EnumParticleTypes.FLAME,
                            this.target.field_70165_t + offsetX,
                            this.target.field_70163_u + offsetY,
                            this.target.field_70161_v + offsetZ,
                            0.0,
                            0.08,
                            0.0,
                            new int[0]
                        );
                    mc.field_71441_e
                        .func_175688_a(
                            EnumParticleTypes.SMOKE_LARGE,
                            this.target.field_70165_t + offsetX,
                            this.target.field_70163_u + offsetY,
                            this.target.field_70161_v + offsetZ,
                            0.0,
                            0.12,
                            0.0,
                            new int[0]
                        );
                }

                mc.field_71439_g.func_85030_a("item.fireCharge.use", 1.0F, 0.6F);
            }

            this.target = null;
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (event.getTarget() instanceof EntityLivingBase) {
            this.target = (EntityLivingBase)event.getTarget();
        }
    }
}
