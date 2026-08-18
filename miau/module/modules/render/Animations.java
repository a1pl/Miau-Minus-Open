package miau.module.modules.render;

import miau.Miau;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.IntProperty;
import miau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

public final class Animations extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final String[] BLOCK_MODES = new String[]{
        "None",
        "1.7",
        "Sunny",
        "Lucid",
        "Astro",
        "Smooth",
        "Spin",
        "Leaked",
        "Old",
        "Exhibition",
        "Exhibition Old",
        "Exhibition New",
        "Swong",
        "Stella",
        "Flup",
        "Noov",
        "Komorebi",
        "Rhys",
        "Swing",
        "?",
        "Stab",
        "Beta",
        "Dortware",
        "Avatar",
        "Tap",
        "Slide"
    };
    private static final double a = Math.PI;
    private static final float b = 180.0F;
    public final ModeProperty blockAnimation = new ModeProperty("Block Animation", 0, BLOCK_MODES);
    public final BooleanProperty onlyWhenBlocking = new BooleanProperty("Update Position Only When Blocking", true);
    public final IntProperty swingSpeed = new IntProperty("Swing Speed", 1, -200, 50);
    public final FloatProperty x = new FloatProperty("X", 0.0F, -2.0F, 2.0F);
    public final FloatProperty y = new FloatProperty("Y", 0.0F, -2.0F, 2.0F);
    public final FloatProperty z = new FloatProperty("Z", 0.0F, -2.0F, 2.0F);
    public final FloatProperty scale = new FloatProperty("Scale", 1.0F, 0.1F, 2.0F);
    public final BooleanProperty alwaysShow = new BooleanProperty("Always Show", false);

    public Animations() {
        super("Animations", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.blockAnimation.getModeString()};
    }

    public static boolean apply(float swingProgress, float equipProgress, AbstractClientPlayer player) {
        Animations animations = (Animations)Miau.moduleManager.modules.get(Animations.class);
        if (animations != null && animations.isEnabled() && player != null) {
            animations.renderBlock(swingProgress, equipProgress, player);
            return true;
        } else {
            return false;
        }
    }

    public static int getSwingAnimationEnd(EntityLivingBase entity, int original) {
        Animations animations = (Animations)Miau.moduleManager.modules.get(Animations.class);
        if (animations != null && animations.isEnabled() && entity == mc.field_71439_g) {
            float speedVal = animations.swingSpeed.getValue().intValue();
            float multiplier = -speedVal / 100.0F + 1.0F;
            return (int)(original * multiplier);
        } else {
            return original;
        }
    }

    private void transformFirstPersonItem(float equipProgress, float swingProgress) {
        GlStateManager.func_179109_b(0.56F, -0.52F, -0.71999997F);
        GlStateManager.func_179109_b(0.0F, equipProgress * -0.6F, 0.0F);
        GlStateManager.func_179114_b(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.func_76126_a(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.func_76126_a(MathHelper.func_76129_c(swingProgress) * (float) Math.PI);
        GlStateManager.func_179114_b(f * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.func_179114_b(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.func_179114_b(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.func_179152_a(0.4F, 0.4F, 0.4F);
    }

    private void blockTransformation() {
        GlStateManager.func_179109_b(-0.5F, 0.2F, 0.0F);
        GlStateManager.func_179114_b(30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.func_179114_b(-80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.func_179114_b(60.0F, 0.0F, 1.0F, 0.0F);
    }

    private void doItemUsedTransformations(float swingProgress) {
        float f = MathHelper.func_76126_a(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.func_76126_a(MathHelper.func_76129_c(swingProgress) * (float) Math.PI);
        GlStateManager.func_179114_b(f * -15.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.func_179114_b(f1 * -50.0F, 1.0F, 0.0F, 0.0F);
    }

    private void renderBlock(float swingProgress, float equipProgress, AbstractClientPlayer player) {
        if (!this.onlyWhenBlocking.getValue()) {
            GlStateManager.func_179109_b(this.x.getValue(), this.y.getValue(), this.z.getValue());
        }

        double var7 = this.scale.getValue().doubleValue();
        float animationProgression = this.alwaysShow.getValue() ? 0.0F : equipProgress;
        float convertedProgress = MathHelper.func_76126_a(MathHelper.func_76129_c(swingProgress) * (float) Math.PI);
        if (this.onlyWhenBlocking.getValue()) {
            GlStateManager.func_179109_b(this.x.getValue(), this.y.getValue(), this.z.getValue());
        }

        switch (this.blockAnimation.getModeString()) {
            case "None":
                this.transformFirstPersonItem(animationProgression, 0.0F);
                GlStateManager.func_179139_a(var7, var7, var7);
                this.blockTransformation();
                break;
            case "1.7":
                this.transformFirstPersonItem(animationProgression, swingProgress);
                GlStateManager.func_179139_a(var7, var7, var7);
                this.blockTransformation();
                break;
            case "Sunny":
                var7 = 0.99;
                GlStateManager.func_179109_b(0.05F, -0.05F, -0.12F);
                this.transformFirstPersonItem(animationProgression + 0.15F, swingProgress);
                GlStateManager.func_179139_a(var7, var7, var7);
                this.blockTransformation();
                GlStateManager.func_179109_b(-0.5F, 0.2F, 0.0F);
                break;
            case "Lucid":
                this.transformFirstPersonItem(animationProgression - 0.1F, swingProgress);
                GlStateManager.func_179139_a(var7, var7, var7);
                this.blockTransformation();
                break;
            case "Astro":
                GlStateManager.func_179109_b(0.0F, 0.03F, -0.05F);
                this.transformFirstPersonItem(animationProgression / 2.0F, swingProgress);
                GlStateManager.func_179139_a(var7, var7, var7);
                GlStateManager.func_179114_b(convertedProgress * 30.0F / 2.0F, -convertedProgress, -0.0F, 9.0F);
                GlStateManager.func_179114_b(convertedProgress * 40.0F, 1.0F, -convertedProgress / 2.0F, -0.0F);
                this.blockTransformation();
                break;
            case "Tap":
                GL11.glTranslatef(0.0F, 0.3F, 0.0F);
                float smooth = swingProgress * 0.8F - swingProgress * swingProgress * 0.8F;
                GlStateManager.func_179139_a(var7, var7, var7);
                GlStateManager.func_179109_b(0.56F, -0.52F, -0.71999997F);
                GlStateManager.func_179114_b(45.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.func_179114_b(smooth * -90.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.func_179152_a(0.37F, 0.37F, 0.37F);
                this.blockTransformation();
                break;
            case "Beta":
                GL11.glTranslatef(0.0F, 0.3F, 0.0F);
                float var15 = MathHelper.func_76126_a(swingProgress * swingProgress * (float) Math.PI);
                this.transformFirstPersonItem(equipProgress * 0.5F, 0.0F);
                GlStateManager.func_179139_a(var7, var7, var7);
                GlStateManager.func_179114_b(-var15 * 55.0F / 2.0F, -8.0F, -0.0F, 9.0F);
                GlStateManager.func_179114_b(-var15 * 45.0F, 1.0F, var15 / 2.0F, -0.0F);
                this.blockTransformation();
                GL11.glTranslated(1.2, 0.3, 0.5);
                GL11.glTranslatef(-1.0F, mc.field_71439_g.func_70093_af() ? -0.1F : -0.2F, 0.2F);
                break;
            case "Slide":
                GL11.glTranslatef(0.0F, 0.3F, 0.0F);
                float smooth2 = swingProgress * 0.8F - swingProgress * swingProgress * 0.8F;
                GlStateManager.func_179139_a(var7, var7, var7);
                GlStateManager.func_179109_b(0.56F, -0.52F, -0.71999997F);
                GlStateManager.func_179109_b(0.0F, equipProgress * 0.3F * -0.6F, 0.0F);
                GlStateManager.func_179114_b(45.0F, 0.0F, 2.0F + smooth2 * 0.5F, smooth2 * 3.0F);
                GlStateManager.func_179114_b(0.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.func_179152_a(0.37F, 0.37F, 0.37F);
                this.blockTransformation();
                break;
            case "Avatar":
                GlStateManager.func_179109_b(0.56F, -0.52F, -0.71999997F);
                GlStateManager.func_179114_b(45.0F, 0.0F, 1.0F, 0.0F);
                float f = MathHelper.func_76126_a(swingProgress * swingProgress * (float) Math.PI);
                float f1 = MathHelper.func_76126_a(MathHelper.func_76129_c(swingProgress) * (float) Math.PI);
                GlStateManager.func_179114_b(f * -20.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.func_179114_b(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.func_179114_b(f1 * -40.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.func_179152_a(0.4F, 0.4F, 0.4F);
                this.blockTransformation();
                break;
            case "Smooth":
                this.transformFirstPersonItem(animationProgression, 0.0F);
                GlStateManager.func_179139_a(var7, var7, var7);
                float ySmooth = -convertedProgress * 2.0F;
                GlStateManager.func_179109_b(0.0F, ySmooth / 10.0F + 0.1F, 0.0F);
                GlStateManager.func_179114_b(ySmooth * 10.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.func_179114_b(250.0F, 0.2F, 1.0F, -0.6F);
                GlStateManager.func_179114_b(-10.0F, 1.0F, 0.5F, 1.0F);
                GlStateManager.func_179114_b(-ySmooth * 20.0F, 1.0F, 0.5F, 1.0F);
                break;
            case "Stab":
                float spin = MathHelper.func_76126_a(MathHelper.func_76129_c(swingProgress) * (float) Math.PI);
                GlStateManager.func_179109_b(0.6F, 0.3F, -0.6F + -spin * 0.7F);
                GlStateManager.func_179114_b(6090.0F, 0.0F, 0.0F, 0.1F);
                GlStateManager.func_179114_b(6085.0F, 0.0F, 0.1F, 0.0F);
                GlStateManager.func_179114_b(6110.0F, 0.1F, 0.0F, 0.0F);
                this.transformFirstPersonItem(0.0F, 0.0F);
                GlStateManager.func_179139_a(var7, var7, var7);
                this.blockTransformation();
                break;
            case "Spin":
                this.transformFirstPersonItem(animationProgression, 0.0F);
                GlStateManager.func_179139_a(var7, var7, var7);
                GlStateManager.func_179109_b(0.0F, 0.2F, -1.0F);
                GlStateManager.func_179114_b(-59.0F, -1.0F, 0.0F, 3.0F);
                GlStateManager.func_179114_b((float)(-(System.currentTimeMillis() / 2L % 360L)), 1.0F, 0.0F, 0.0F);
                GlStateManager.func_179114_b(60.0F, 0.0F, 1.0F, 0.0F);
                break;
            case "Leaked":
                GlStateManager.func_179109_b(0.0F, -0.03F, -0.13F);
                this.transformFirstPersonItem(animationProgression / 3.0F, 0.0F);
                GlStateManager.func_179139_a(var7, var7, var7);
                GlStateManager.func_179109_b(0.0F, 0.1F, 0.0F);
                this.blockTransformation();
                GlStateManager.func_179114_b(convertedProgress * 20.0F / 2.0F, 0.0F, 1.0F, 1.5F);
                GlStateManager.func_179114_b(-convertedProgress * 200.0F / 4.0F, 1.0F, 0.9F, 0.0F);
                break;
            case "Old":
                GlStateManager.func_179109_b(0.0F, 0.1F, 0.0F);
                this.transformFirstPersonItem(animationProgression / 2.0F - 0.2F, swingProgress);
                GlStateManager.func_179139_a(var7, var7, var7);
                this.blockTransformation();
                break;
            case "Exhibition":
                GlStateManager.func_179109_b(0.0F, -0.05F, 0.0F);
                this.transformFirstPersonItem(animationProgression / 2.0F, 0.0F);
                GlStateManager.func_179139_a(var7, var7, var7);
                GlStateManager.func_179109_b(0.0F, 0.3F, -0.0F);
                GlStateManager.func_179114_b(-convertedProgress * 31.0F, 1.0F, 0.0F, 2.0F);
                GlStateManager.func_179114_b(-convertedProgress * 33.0F, 1.5F, convertedProgress / 1.1F, 0.0F);
                this.blockTransformation();
                break;
            case "Exhibition Old":
                GlStateManager.func_179109_b(0.0F, -0.05F, 0.0F);
                GlStateManager.func_179109_b(-0.04F, 0.13F, 0.0F);
                this.transformFirstPersonItem(animationProgression / 2.5F, 0.0F);
                GlStateManager.func_179139_a(var7, var7, var7);
                GlStateManager.func_179114_b(-convertedProgress * 40.0F / 2.0F, convertedProgress / 2.0F, 1.0F, 4.0F);
                GlStateManager.func_179114_b(-convertedProgress * 30.0F, 1.0F, convertedProgress / 3.0F, -0.0F);
                this.blockTransformation();
                break;
            case "Exhibition New":
                GlStateManager.func_179109_b(0.0F, -0.04F, -0.01F);
                this.transformFirstPersonItem(animationProgression / 2.0F, 0.0F);
                GlStateManager.func_179139_a(var7, var7, var7);
                GlStateManager.func_179109_b(0.0F, 0.3F, -0.0F);
                GlStateManager.func_179114_b(-convertedProgress * 30.0F, 1.0F, 0.0F, 2.0F);
                GlStateManager.func_179114_b(-convertedProgress * 44.0F, 1.5F, convertedProgress / 1.2F, 0.0F);
                this.blockTransformation();
                break;
            case "Swong":
                GlStateManager.func_179109_b(0.0F, 0.1F, -0.05F);
                this.transformFirstPersonItem(animationProgression / 2.0F, swingProgress);
                GlStateManager.func_179139_a(var7, var7, var7);
                GlStateManager.func_179114_b(convertedProgress * 30.0F, -convertedProgress, -0.0F, 9.0F);
                GlStateManager.func_179114_b(convertedProgress * 40.0F, 1.0F, -convertedProgress, -0.0F);
                this.blockTransformation();
                break;
            case "Stella":
                this.transformFirstPersonItem(-0.1F, swingProgress);
                GlStateManager.func_179139_a(var7, var7, var7);
                GlStateManager.func_179109_b(-0.5F, 0.4F, -0.2F);
                GlStateManager.func_179114_b(30.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.func_179114_b(-70.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.func_179114_b(40.0F, 0.0F, 1.0F, 0.0F);
                break;
            case "Flup":
                GlStateManager.func_179109_b(0.0F, 0.1F, -0.05F);
                this.transformFirstPersonItem(animationProgression, 0.0F);
                GlStateManager.func_179139_a(var7, var7, var7);
                this.blockTransformation();
                GlStateManager.func_179109_b(-0.05F, 0.2F, 0.0F);
                GlStateManager.func_179114_b(-convertedProgress * 70.0F / 2.0F, -8.0F, -0.0F, 9.0F);
                GlStateManager.func_179114_b(-convertedProgress * 70.0F, 1.0F, -0.4F, -0.0F);
                break;
            case "Noov":
                this.transformFirstPersonItem(animationProgression / 1.5F, 0.0F);
                GlStateManager.func_179139_a(var7, var7, var7);
                this.blockTransformation();
                GlStateManager.func_179109_b(-0.05F, 0.3F, 0.3F);
                GlStateManager.func_179114_b(-convertedProgress * 140.0F, 8.0F, 0.0F, 8.0F);
                GlStateManager.func_179114_b(convertedProgress * 180.0F, 8.0F, 0.0F, 8.0F);
                break;
            case "Komorebi":
                this.transformFirstPersonItem(-0.25F, 1.0F + convertedProgress / 10.0F);
                GlStateManager.func_179139_a(var7, var7, var7);
                GL11.glRotated(-convertedProgress * 25.0F, 1.0, 0.0, 0.0);
                this.blockTransformation();
                break;
            case "Rhys":
                GlStateManager.func_179109_b(0.41F, -0.25F, -0.5555557F);
                GlStateManager.func_179109_b(0.0F, 0.0F, 0.0F);
                GlStateManager.func_179114_b(35.0F, 0.0F, 1.5F, 0.0F);
                float racism = MathHelper.func_76126_a(swingProgress * swingProgress / 64.0F * (float) Math.PI);
                GlStateManager.func_179114_b(racism * -5.0F, 0.0F, 0.0F, 0.0F);
                GlStateManager.func_179114_b(convertedProgress * -12.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.func_179114_b(convertedProgress * -65.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.func_179139_a(var7, var7, var7);
                this.blockTransformation();
                break;
            case "Swing":
                this.transformFirstPersonItem(animationProgression, swingProgress);
                GlStateManager.func_179139_a(var7, var7, var7);
                this.blockTransformation();
                GlStateManager.func_179109_b(-0.3F, -0.1F, -0.0F);
                break;
            case "?":
                this.transformFirstPersonItem(animationProgression, swingProgress);
                GlStateManager.func_179139_a(var7, var7, var7);
                GL11.glTranslatef(-0.35F, 0.1F, 0.0F);
                GL11.glTranslatef(-0.05F, -0.1F, 0.1F);
                this.blockTransformation();
                break;
            case "Dortware":
                float var1_dort = MathHelper.func_76126_a((float)(swingProgress * swingProgress * Math.PI - 3.0));
                float var_dort = MathHelper.func_76126_a((float)(MathHelper.func_76129_c(swingProgress) * Math.PI));
                this.transformFirstPersonItem(animationProgression, 1.0F);
                GlStateManager.func_179114_b(-var_dort * 10.0F, 0.0F, 15.0F, 200.0F);
                GlStateManager.func_179114_b(-var_dort * 10.0F, 300.0F, var_dort / 2.0F, 1.0F);
                this.blockTransformation();
                GL11.glTranslated(2.4, 0.3, 0.5);
                GL11.glTranslatef(-2.1F, -0.2F, 0.1F);
                GlStateManager.func_179114_b(var1_dort * 13.0F, -10.0F, -1.4F, -10.0F);
        }
    }
}
