package miau.module.modules.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.TickEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.module.modules.combat.KillAura;
import miau.property.properties.BooleanProperty;
import miau.property.properties.ColorProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.biome.BiomeGenBase;
import org.lwjgl.opengl.GL11;

public final class Ambience extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final List<Ambience.Particle> particles = new ArrayList<>();
    private EntityLivingBase currentTarget = null;
    private long lastHitTime = 0L;
    public final IntProperty time = new IntProperty("Time", 0, 0, 22999);
    public final IntProperty speed = new IntProperty("Time Speed", 0, 0, 20);
    public final ModeProperty weather = new ModeProperty(
        "Weather", 0, new String[]{"Unchanged", "Clear", "Rain", "Heavy Snow", "Light Snow", "Nether Particles"}
    );
    public final ColorProperty snowColor = new ColorProperty(
        "Snow Color",
        Color.WHITE.getRGB(),
        () -> !this.weather.getModeString().equals("Heavy Snow") && !this.weather.getModeString().equals("Light Snow")
    );
    public final BooleanProperty particleEnvironment = new BooleanProperty("Particle Environment", false);
    public final ColorProperty particleColor = new ColorProperty(
        "Particle Color", new Color(0, 200, 255).getRGB(), this.particleEnvironment::getValue
    );
    public final IntProperty particleAmount = new IntProperty(
        "Particle Amount", 3, 1, 10, this.particleEnvironment::getValue
    );
    public final FloatProperty particleRadius = new FloatProperty(
        "Particle Radius", 1.2F, 0.5F, 3.0F, this.particleEnvironment::getValue
    );
    public final FloatProperty particleSpeed = new FloatProperty(
        "Particle Speed", 3.0F, 0.5F, 10.0F, this.particleEnvironment::getValue
    );

    public Ambience() {
        super("Ambience", false);
    }

    @Override
    public void onDisabled() {
        if (mc.field_71441_e != null) {
            mc.field_71441_e.func_72894_k(0.0F);
            mc.field_71441_e.func_72912_H().func_176142_i(Integer.MAX_VALUE);
            mc.field_71441_e.func_72912_H().func_76080_g(0);
            mc.field_71441_e.func_72912_H().func_76090_f(0);
            mc.field_71441_e.func_72912_H().func_76084_b(false);
            mc.field_71441_e.func_72912_H().func_76069_a(false);
        }

        this.particles.clear();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.particleEnvironment.getValue()) {
                EntityLivingBase target = this.getTarget();
                if (target != null && mc.field_71439_g.field_70173_aa % 2 == 0) {
                    for (int i = 0; i < this.particleAmount.getValue(); i++) {
                        this.particles.add(new Ambience.Particle(target));
                    }
                }

                this.particles.forEach(Ambience.Particle::update);
                this.particles.removeIf(p -> p.age >= p.maxAge);
            } else {
                this.particles.clear();
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.field_71441_e != null) {
            mc.field_71441_e
                .func_72877_b(
                    this.time.getValue().intValue() + System.currentTimeMillis() * this.speed.getValue().intValue()
                );
        }

        if (this.isEnabled() && this.particleEnvironment.getValue() && !this.particles.isEmpty()) {
            GL11.glPushMatrix();
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glEnable(2929);
            GL11.glDepthMask(false);
            GL11.glBlendFunc(770, 1);
            GL11.glDisable(2884);
            int colorValue = this.particleColor.getValue();
            float baseR = (colorValue >> 16 & 0xFF) / 255.0F;
            float baseG = (colorValue >> 8 & 0xFF) / 255.0F;
            float baseB = (colorValue & 0xFF) / 255.0F;

            for (Ambience.Particle p : this.particles) {
                if (p.positions.size() > 1) {
                    for (int i = 0; i < p.positions.size() - 1; i++) {
                        double[] pos1 = p.positions.get(i);
                        double[] pos2 = p.positions.get(i + 1);
                        float x1 = (float)(pos1[0] - mc.func_175598_ae().field_78730_l);
                        float y1 = (float)(pos1[1] - mc.func_175598_ae().field_78731_m);
                        float z1 = (float)(pos1[2] - mc.func_175598_ae().field_78728_n);
                        float x2 = (float)(pos2[0] - mc.func_175598_ae().field_78730_l);
                        float y2 = (float)(pos2[1] - mc.func_175598_ae().field_78731_m);
                        float z2 = (float)(pos2[2] - mc.func_175598_ae().field_78728_n);
                        float progress = (float)i / p.positions.size();
                        float alpha = progress * (1.0F - (float)p.age / p.maxAge) * 0.8F;
                        float dynamicWidth = 0.5F + progress * 2.2F;
                        GL11.glLineWidth(dynamicWidth);
                        GL11.glBegin(1);
                        GL11.glColor4f(baseR * progress, baseG * progress, baseB * progress, alpha * 0.5F);
                        GL11.glVertex3f(x1, y1, z1);
                        GL11.glColor4f(baseR, baseG, baseB, alpha);
                        GL11.glVertex3f(x2, y2, z2);
                        GL11.glEnd();
                    }

                    double[] headPos = p.positions.get(p.positions.size() - 1);
                    float hx = (float)(headPos[0] - mc.func_175598_ae().field_78730_l);
                    float hy = (float)(headPos[1] - mc.func_175598_ae().field_78731_m);
                    float hz = (float)(headPos[2] - mc.func_175598_ae().field_78728_n);
                    float headAlpha = 1.0F - (float)p.age / p.maxAge;
                    GL11.glPointSize(4.5F);
                    GL11.glBegin(0);
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, headAlpha * 0.9F);
                    GL11.glVertex3f(hx, hy, hz);
                    GL11.glColor4f(baseR, baseG, baseB, headAlpha);
                    GL11.glVertex3f(hx, hy, hz);
                    GL11.glEnd();
                }
            }

            GL11.glLineWidth(1.0F);
            GL11.glEnable(2884);
            GL11.glDepthMask(true);
            GL11.glDisable(2929);
            GL11.glEnable(3553);
            GL11.glDisable(3042);
            GL11.glPopMatrix();
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE && mc.field_71439_g != null && mc.field_71439_g.field_70173_aa % 20 == 0) {
            switch (this.weather.getModeString()) {
                case "Clear":
                    mc.field_71441_e.func_72894_k(0.0F);
                    mc.field_71441_e.func_72912_H().func_176142_i(Integer.MAX_VALUE);
                    mc.field_71441_e.func_72912_H().func_76080_g(0);
                    mc.field_71441_e.func_72912_H().func_76090_f(0);
                    mc.field_71441_e.func_72912_H().func_76084_b(false);
                    mc.field_71441_e.func_72912_H().func_76069_a(false);
                    break;
                case "Nether Particles":
                case "Light Snow":
                case "Heavy Snow":
                case "Rain":
                    mc.field_71441_e.func_72894_k(1.0F);
                    mc.field_71441_e.func_72912_H().func_176142_i(0);
                    mc.field_71441_e.func_72912_H().func_76080_g(Integer.MAX_VALUE);
                    mc.field_71441_e.func_72912_H().func_76090_f(Integer.MAX_VALUE);
                    mc.field_71441_e.func_72912_H().func_76084_b(true);
                    mc.field_71441_e.func_72912_H().func_76069_a(false);
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof S03PacketTimeUpdate) {
            event.setCancelled(true);
        } else if (event.getPacket() instanceof S2BPacketChangeGameState
            && !this.weather.getModeString().equals("Unchanged")) {
            S2BPacketChangeGameState s2b = (S2BPacketChangeGameState)event.getPacket();
            if (s2b.func_149138_c() == 1 || s2b.func_149138_c() == 2) {
                event.setCancelled(true);
            }
        }
    }

    public float getFloatTemperature(BlockPos blockPos, BiomeGenBase biomeGenBase) {
        if (this.isEnabled()) {
            switch (this.weather.getModeString()) {
                case "Nether Particles":
                case "Light Snow":
                case "Heavy Snow":
                    return 0.1F;
                case "Rain":
                    return 0.2F;
            }
        }

        return biomeGenBase.func_180626_a(blockPos);
    }

    public boolean skipRainParticles() {
        String name = this.weather.getModeString();
        return this.isEnabled()
            && (name.equals("Light Snow") || name.equals("Heavy Snow") || name.equals("Nether Particles"));
    }

    private EntityLivingBase getTarget() {
        if (mc.field_71476_x != null
            && mc.field_71476_x.field_72308_g instanceof EntityLivingBase
            && mc.field_71439_g.field_82175_bq) {
            this.currentTarget = (EntityLivingBase)mc.field_71476_x.field_72308_g;
            this.lastHitTime = System.currentTimeMillis();
        }

        if (this.currentTarget != null
            && !this.currentTarget.field_70128_L
            && this.currentTarget.func_110143_aJ() > 0.0F
            && System.currentTimeMillis() - this.lastHitTime < 3000L) {
            return this.currentTarget;
        }

        KillAura killAura = (KillAura)Miau.moduleManager.getModule(KillAura.class);
        return (EntityLivingBase)(killAura != null && killAura.isEnabled() && killAura.getTarget() != null
            ? killAura.getTarget()
            : mc.field_71439_g);
    }

    private class Particle {
        public double x;
        public double y;
        public double z;
        public int age;
        public int maxAge;
        private EntityLivingBase target;
        private double vx;
        private double vy;
        private double vz;
        private double angle;
        private double angleSpeed;
        public final List<double[]> positions = new ArrayList<>();

        public Particle(EntityLivingBase target) {
            this.target = target;
            this.age = 0;
            this.maxAge = 40 + (int)(Math.random() * 20.0);
            double range = 10.0;
            if (target == Ambience.mc.field_71439_g && Ambience.mc.field_71474_y.field_74320_O == 0) {
                Vec3 look = Ambience.mc.field_71439_g.func_70040_Z();
                this.x = target.field_70165_t + look.field_72450_a * 1.5 + (Math.random() - 0.5) * range;
                this.y = target.field_70163_u + Ambience.mc.field_71439_g.func_70047_e() + (Math.random() - 0.5) * 1.5;
                this.z = target.field_70161_v + look.field_72449_c * 1.5 + (Math.random() - 0.5) * range;
            } else {
                this.x = target.field_70165_t + (Math.random() - 0.5) * (range * 1.5);
                this.y = target.field_70163_u + Math.random() * target.field_70131_O + (Math.random() - 0.5) * 3.0;
                this.z = target.field_70161_v + (Math.random() - 0.5) * (range * 1.5);
            }

            double speed = Ambience.this.particleSpeed.getValue().floatValue() * 0.05;
            this.vx = (Math.random() - 0.5) * speed;
            this.vy = (Math.random() - 0.4) * speed * 0.7;
            this.vz = (Math.random() - 0.5) * speed;
            this.angle = Math.random() * Math.PI * 2.0;
            this.angleSpeed = (Math.random() - 0.5) * 0.15;
        }

        public void update() {
            this.age++;
            this.angle = this.angle + this.angleSpeed;
            this.vx = this.vx + Math.cos(this.angle) * 0.02;
            this.vz = this.vz + Math.sin(this.angle) * 0.02;
            this.x = this.x + this.vx;
            this.y = this.y + this.vy;
            this.z = this.z + this.vz;
            double range = 12.0;
            if (Math.abs(this.x - this.target.field_70165_t) > range) {
                this.vx *= -1.0;
            }

            if (Math.abs(this.z - this.target.field_70161_v) > range) {
                this.vz *= -1.0;
            }

            if (this.y < this.target.field_70163_u - 0.5
                || this.y > this.target.field_70163_u + this.target.field_70131_O + 6.0) {
                this.vy *= -1.0;
            }

            this.positions.add(new double[]{this.x, this.y, this.z});
            if (this.positions.size() > 15) {
                this.positions.remove(0);
            }
        }
    }
}
