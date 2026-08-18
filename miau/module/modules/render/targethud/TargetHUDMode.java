package miau.module.modules.render.targethud;

import java.awt.Color;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import miau.module.modules.render.TargetHUD;
import miau.util.render.GLUtil;
import miau.util.render.ShapeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Timer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

public abstract class TargetHUDMode {
    protected final TargetHUD parent;
    protected static final Minecraft mc = Minecraft.func_71410_x();
    protected static final DecimalFormat healthFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    protected static final DecimalFormat diffFormat = new DecimalFormat(
        "+0.0;-0.0", new DecimalFormatSymbols(Locale.US)
    );
    private static Field timerField = null;

    public TargetHUDMode(TargetHUD parent) {
        this.parent = parent;
    }

    public abstract void render(EntityLivingBase var1, float var2, float var3);

    protected float getPartialTicks() {
        if (timerField != null) {
            try {
                return ((Timer)timerField.get(mc)).field_74281_c;
            } catch (Exception var6) {
            }
        }

        try {
            for (Field f : Minecraft.class.getDeclaredFields()) {
                if (f.getType() == Timer.class) {
                    f.setAccessible(true);
                    timerField = f;
                    return ((Timer)f.get(mc)).field_74281_c;
                }
            }
        } catch (Exception var5) {
        }

        return 1.0F;
    }

    protected double[] projectTo2D(EntityLivingBase entity) {
        float partialTicks = this.getPartialTicks();
        double renderX = mc.func_175598_ae().field_78730_l;
        double renderY = mc.func_175598_ae().field_78731_m;
        double renderZ = mc.func_175598_ae().field_78728_n;
        double x = entity.field_70142_S + (entity.field_70165_t - entity.field_70142_S) * partialTicks - renderX;
        double y = entity.field_70137_T
            + (entity.field_70163_u - entity.field_70137_T) * partialTicks
            - renderY
            + entity.field_70131_O / 2.0;
        double z = entity.field_70136_U + (entity.field_70161_v - entity.field_70136_U) * partialTicks - renderZ;
        FloatBuffer winCoords = BufferUtils.createFloatBuffer(3);
        boolean result = GLU.gluProject(
            (float)x, (float)y, (float)z, TargetHUD.MODELVIEW, TargetHUD.PROJECTION, TargetHUD.VIEWPORT, winCoords
        );
        if (result) {
            float winZ = winCoords.get(2);
            if (winZ >= 0.0F && winZ <= 1.0F) {
                ScaledResolution sr = new ScaledResolution(mc);
                double screenX = winCoords.get(0) / sr.func_78325_e();
                double screenY = (TargetHUD.VIEWPORT.get(3) - winCoords.get(1)) / sr.func_78325_e();
                return new double[]{screenX, screenY};
            }
        }

        return null;
    }

    protected float calculateDistanceScale(EntityLivingBase target) {
        double distance = mc.field_71439_g.func_70032_d(target);
        return (float)MathHelper.func_151237_a(1.0 - (distance - 3.0) * 0.06, 0.45, 1.25);
    }

    public void drawHead(ResourceLocation skin, int width, int height, Color color) {
        GL11.glColor4f(
            color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F
        );
        mc.func_110434_K().func_110577_a(skin);
        Gui.func_152125_a(2, 2, 8.0F, 8.0F, 8, 8, width, height, 64.0F, 64.0F);
    }

    protected void renderPlayer2D(float x, float y, float width, float height, AbstractClientPlayer player) {
        GLUtil.startBlend();
        mc.func_110434_K().func_110577_a(player.func_110306_p());
        Gui.func_152125_a((int)x, (int)y, 8.0F, 8.0F, 8, 8, (int)width, (int)height, 64.0F, 64.0F);
        GLUtil.endBlend();
    }

    public void drawArmorHUD(EntityPlayer player, int y, int x) {
        GL11.glPushMatrix();
        List<ItemStack> stuff = new ArrayList<>();

        for (int index = 3; index >= 0; index--) {
            ItemStack armor = player.field_71071_by.field_70460_b[index];
            if (armor != null) {
                stuff.add(armor);
            }
        }

        if (player.func_71045_bC() != null) {
            stuff.add(player.func_71045_bC());
        }

        int split = -3;

        for (ItemStack item : stuff) {
            if (mc.field_71441_e != null) {
                RenderHelper.func_74520_c();
                split += 16;
            }

            GlStateManager.func_179094_E();
            GlStateManager.func_179118_c();
            GlStateManager.func_179086_m(256);
            GlStateManager.func_179147_l();
            mc.func_175599_af().field_77023_b = -150.0F;
            mc.func_175599_af().func_180450_b(item, split + x + 18, y + 17);
            mc.func_175599_af().field_77023_b = 0.0F;
            GlStateManager.func_179147_l();
            GlStateManager.func_179152_a(0.5F, 0.5F, 0.5F);
            GlStateManager.func_179097_i();
            GlStateManager.func_179140_f();
            GlStateManager.func_179126_j();
            GlStateManager.func_179152_a(2.0F, 2.0F, 2.0F);
            GlStateManager.func_179141_d();
            GlStateManager.func_179121_F();
        }

        GL11.glPopMatrix();
    }

    public void rectangle(double x, double y, double x1, double y1, int color) {
        ShapeUtil.drawRect((float)x, (float)y, (float)x1, (float)y1, color);
    }

    public void rectangleBordered(
        double x, double y, double x1, double y1, double width, int internalColor, int borderColor
    ) {
        this.rectangle(x + width, y + width, x1 - width, y1 - width, internalColor);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        this.rectangle(x + width, y, x1 - width, y + width, borderColor);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        this.rectangle(x, y, x + width, y1, borderColor);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        this.rectangle(x1 - width, y, x1, y1, borderColor);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        this.rectangle(x + width, y1 - width, x1 - width, y1, borderColor);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void skeetRect(double x, double y, double x1, double y1, double size) {
        this.rectangleBordered(
            x, y - 4.0, x1 + size, y1 + size, 0.5, new Color(60, 60, 60).getRGB(), new Color(10, 10, 10).getRGB()
        );
        this.rectangleBordered(
            x + 1.0,
            y - 3.0,
            x1 + size - 1.0,
            y1 + size - 1.0,
            1.0,
            new Color(40, 40, 40).getRGB(),
            new Color(40, 40, 40).getRGB()
        );
        this.rectangleBordered(
            x + 2.5,
            y - 1.5,
            x1 + size - 2.5,
            y1 + size - 2.5,
            0.5,
            new Color(40, 40, 40).getRGB(),
            new Color(60, 60, 60).getRGB()
        );
        this.rectangleBordered(
            x + 2.5,
            y - 1.5,
            x1 + size - 2.5,
            y1 + size - 2.5,
            0.5,
            new Color(22, 22, 22).getRGB(),
            new Color(255, 255, 255, 0).getRGB()
        );
    }

    public void skeetRectSmall(double x, double y, double x1, double y1, double size) {
        this.rectangleBordered(
            x + 4.35,
            y + 0.5,
            x1 + size - 84.5,
            y1 + size - 4.35,
            0.5,
            new Color(48, 48, 48).getRGB(),
            new Color(10, 10, 10).getRGB()
        );
        this.rectangleBordered(
            x + 5.0,
            y + 1.0,
            x1 + size - 85.0,
            y1 + size - 5.0,
            0.5,
            new Color(17, 17, 17).getRGB(),
            new Color(255, 255, 255, 0).getRGB()
        );
    }

    public void drawModel(float yaw, float pitch, EntityLivingBase entityLivingBase) {
        GlStateManager.func_179117_G();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.func_179142_g();
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b(0.0F, 0.0F, 50.0F);
        GlStateManager.func_179152_a(-50.0F, 50.0F, 50.0F);
        GlStateManager.func_179114_b(180.0F, 0.0F, 0.0F, 1.0F);
        float renderYawOffset = entityLivingBase.field_70761_aq;
        float rotationYaw = entityLivingBase.field_70177_z;
        float rotationPitch = entityLivingBase.field_70125_A;
        float prevRotationYawHead = entityLivingBase.field_70758_at;
        float rotationYawHead = entityLivingBase.field_70759_as;
        GlStateManager.func_179114_b(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.func_74519_b();
        GlStateManager.func_179114_b(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.func_179114_b((float)(-Math.atan(pitch / 40.0F) * 20.0), 1.0F, 0.0F, 0.0F);
        entityLivingBase.field_70761_aq = yaw - yaw / yaw * 0.4F;
        entityLivingBase.field_70177_z = yaw - yaw / yaw * 0.4F;
        entityLivingBase.field_70125_A = pitch;
        entityLivingBase.field_70759_as = entityLivingBase.field_70177_z;
        entityLivingBase.field_70758_at = entityLivingBase.field_70177_z;
        GlStateManager.func_179109_b(0.0F, 0.0F, 0.0F);
        RenderManager renderManager = mc.func_175598_ae();
        renderManager.func_178631_a(180.0F);
        renderManager.func_178633_a(false);
        renderManager.func_147940_a(entityLivingBase, 0.0, 0.0, 0.0, 0.0F, 1.0F);
        renderManager.func_178633_a(true);
        entityLivingBase.field_70761_aq = renderYawOffset;
        entityLivingBase.field_70177_z = rotationYaw;
        entityLivingBase.field_70125_A = rotationPitch;
        entityLivingBase.field_70758_at = prevRotationYawHead;
        entityLivingBase.field_70759_as = rotationYawHead;
        GlStateManager.func_179121_F();
        RenderHelper.func_74518_a();
        GlStateManager.func_179101_C();
        GlStateManager.func_179138_g(OpenGlHelper.field_77476_b);
        GlStateManager.func_179090_x();
        GlStateManager.func_179138_g(OpenGlHelper.field_77478_a);
        GlStateManager.func_179117_G();
    }

    public Color getBlendColor(double current, double max) {
        long base = Math.round(max / 5.0);
        if (current >= base * 5L) {
            return new Color(15, 255, 15);
        } else if (current >= base * 4L) {
            return new Color(166, 255, 0);
        } else if (current >= base * 3L) {
            return new Color(255, 191, 0);
        } else {
            return current >= base * 2L ? new Color(255, 89, 0) : new Color(255, 0, 0);
        }
    }

    public Color blendColors(float[] fractions, Color[] colors, float progress) {
        if (fractions == null) {
            throw new IllegalArgumentException("Fractions can't be null");
        }

        if (colors == null) {
            throw new IllegalArgumentException("Colours can't be null");
        }

        if (fractions.length != colors.length) {
            throw new IllegalArgumentException("Fractions and colours must have equal number of elements");
        }

        int[] indicies = this.getFractionIndicies(fractions, progress);
        float[] range = new float[]{fractions[indicies[0]], fractions[indicies[1]]};
        Color[] colorRange = new Color[]{colors[indicies[0]], colors[indicies[1]]};
        float max = range[1] - range[0];
        float value = progress - range[0];
        float weight = value / max;
        return this.blend(colorRange[0], colorRange[1], 1.0F - weight);
    }

    public int[] getFractionIndicies(float[] fractions, float progress) {
        int[] range = new int[2];
        int startPoint = 0;

        while (startPoint < fractions.length && fractions[startPoint] <= progress) {
            startPoint++;
        }

        if (startPoint >= fractions.length) {
            startPoint = fractions.length - 1;
        }

        range[0] = startPoint - 1;
        range[1] = startPoint;
        return range;
    }

    public Color blend(Color color1, Color color2, double ratio) {
        float r = (float)ratio;
        float ir = 1.0F - r;
        float[] rgb1 = new float[3];
        float[] rgb2 = new float[3];
        color1.getColorComponents(rgb1);
        color2.getColorComponents(rgb2);
        float red = rgb1[0] * r + rgb2[0] * ir;
        float green = rgb1[1] * r + rgb2[1] * ir;
        float blue = rgb1[2] * r + rgb2[2] * ir;
        if (red < 0.0F) {
            red = 0.0F;
        } else if (red > 255.0F) {
            red = 255.0F;
        }

        if (green < 0.0F) {
            green = 0.0F;
        } else if (green > 255.0F) {
            green = 255.0F;
        }

        if (blue < 0.0F) {
            blue = 0.0F;
        } else if (blue > 255.0F) {
            blue = 255.0F;
        }

        Color color3 = null;

        try {
            color3 = new Color(red, green, blue);
        } catch (IllegalArgumentException exp) {
            NumberFormat nf = NumberFormat.getNumberInstance();
            exp.printStackTrace();
        }

        return color3;
    }

    public void renderEnchantText(ItemStack item, int x, float y) {
        int enchantmentY = (int)(y - 8.0F);
        if (item.func_77973_b() instanceof ItemSword) {
            float sharpness = EnchantmentHelper.func_77506_a(Enchantment.field_180314_l.field_77352_x, item);
            float fireAspect = EnchantmentHelper.func_77506_a(Enchantment.field_77334_n.field_77352_x, item);
            float knockback = EnchantmentHelper.func_77506_a(Enchantment.field_180313_o.field_77352_x, item);
            if (sharpness > 0.0F) {
                mc.field_71466_p
                    .func_175065_a("S" + this.getColor(sharpness) + (int)sharpness, x * 2, enchantmentY, 16777215, true);
                enchantmentY -= 8;
            }

            if (fireAspect > 0.0F) {
                mc.field_71466_p
                    .func_175065_a(
                        "F" + this.getColor(fireAspect) + (int)fireAspect, x * 2, enchantmentY, 16777215, true
                    );
                enchantmentY -= 8;
            }

            if (knockback > 0.0F) {
                mc.field_71466_p
                    .func_175065_a("K" + this.getColor(knockback) + (int)knockback, x * 2, enchantmentY, 16777215, true);
            }
        }

        if (item.func_77973_b() instanceof ItemArmor) {
            float protection = EnchantmentHelper.func_77506_a(Enchantment.field_180310_c.field_77352_x, item);
            float unbreaking = EnchantmentHelper.func_77506_a(Enchantment.field_77347_r.field_77352_x, item);
            float thorns = EnchantmentHelper.func_77506_a(Enchantment.field_92091_k.field_77352_x, item);
            if (protection > 0.0F) {
                mc.field_71466_p
                    .func_175065_a(
                        "P" + this.getColor(protection) + (int)protection, x * 2, enchantmentY, 16777215, true
                    );
                enchantmentY -= 8;
            }

            if (unbreaking > 0.0F) {
                mc.field_71466_p
                    .func_175065_a(
                        "U" + this.getColor(unbreaking) + (int)unbreaking, x * 2, enchantmentY, 16777215, true
                    );
                enchantmentY -= 8;
            }

            if (thorns > 0.0F) {
                mc.field_71466_p
                    .func_175065_a("T" + this.getColor(thorns) + (int)thorns, x * 2, enchantmentY, 16777215, true);
            }
        }

        if (item.func_77973_b() instanceof ItemBow) {
            float power = EnchantmentHelper.func_77506_a(Enchantment.field_77345_t.field_77352_x, item);
            float punch = EnchantmentHelper.func_77506_a(Enchantment.field_77344_u.field_77352_x, item);
            float flame = EnchantmentHelper.func_77506_a(Enchantment.field_77343_v.field_77352_x, item);
            if (power > 0.0F) {
                mc.field_71466_p
                    .func_175065_a("P" + this.getColor(power) + (int)power, x * 2, enchantmentY, 16777215, true);
                enchantmentY -= 8;
            }

            if (punch > 0.0F) {
                mc.field_71466_p
                    .func_175065_a("P" + this.getColor(punch) + (int)punch, x * 2, enchantmentY, 16777215, true);
                enchantmentY -= 8;
            }

            if (flame > 0.0F) {
                mc.field_71466_p
                    .func_175065_a("F" + this.getColor(flame) + (int)flame, x * 2, enchantmentY, 16777215, true);
            }
        }
    }

    public String getColor(float n) {
        if (n != 1.0F) {
            if (n == 2.0F) {
                return "§a";
            }

            if (n == 3.0F) {
                return "§3";
            }

            if (n == 4.0F) {
                return "§4";
            }

            if (n >= 5.0F) {
                return "§e";
            }
        }

        return "§f";
    }

    public int getNextPostion(int anim, int max, double speed) {
        if (anim == max) {
            return anim;
        }

        if (anim > max) {
            anim -= Math.max(1, (int)Math.round((anim - max) / speed));
        } else {
            anim += Math.max(1, (int)Math.round((max - anim) / speed));
        }

        return anim;
    }

    public int reAlpha(int color, float alpha) {
        Color c = new Color(color);
        float r = 0.003921569F * c.getRed();
        float g = 0.003921569F * c.getGreen();
        float b = 0.003921569F * c.getBlue();
        return new Color(r, g, b, alpha).getRGB();
    }

    public float clampValue(float value, float floor, float cap) {
        if (value < floor) {
            return floor;
        } else {
            return value > cap ? cap : value;
        }
    }

    public void drawRoundedGradientRect(
        float left,
        float top,
        float right,
        float bottom,
        float radius,
        int topLeft,
        int bottomLeft,
        int bottomRight,
        int topRight
    ) {
        float minDimension = Math.min(right - left, bottom - top);
        if (radius > minDimension / 2.0F) {
            radius = minDimension / 2.0F;
        }

        GlStateManager.func_179147_l();
        GlStateManager.func_179090_x();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        GlStateManager.func_179103_j(7425);
        GL11.glBegin(9);
        this.glColor(topLeft);

        for (int i = 0; i <= 90; i += 3) {
            GL11.glVertex2f(
                (float)(left + radius + Math.sin(Math.toRadians(i)) * radius * -1.0),
                (float)(top + radius + Math.cos(Math.toRadians(i)) * radius * -1.0)
            );
        }

        this.glColor(bottomLeft);

        for (int i = 90; i <= 180; i += 3) {
            GL11.glVertex2f(
                (float)(left + radius + Math.sin(Math.toRadians(i)) * radius * -1.0),
                (float)(bottom - radius + Math.cos(Math.toRadians(i)) * radius * -1.0)
            );
        }

        this.glColor(bottomRight);

        for (int i = 0; i <= 90; i += 3) {
            GL11.glVertex2f(
                (float)(right - radius + Math.sin(Math.toRadians(i)) * radius),
                (float)(bottom - radius + Math.cos(Math.toRadians(i)) * radius)
            );
        }

        this.glColor(topRight);

        for (int i = 90; i <= 180; i += 3) {
            GL11.glVertex2f(
                (float)(right - radius + Math.sin(Math.toRadians(i)) * radius),
                (float)(top + radius + Math.cos(Math.toRadians(i)) * radius)
            );
        }

        GL11.glEnd();
        GlStateManager.func_179103_j(7425);
        GL11.glBegin(7);
        this.glColor(topLeft);
        GL11.glVertex2f(left, top + radius);
        this.glColor(bottomLeft);
        GL11.glVertex2f(left, bottom - radius);
        this.glColor(bottomRight);
        GL11.glVertex2f(right, bottom - radius);
        this.glColor(topRight);
        GL11.glVertex2f(right, top + radius);
        GL11.glEnd();
        GL11.glBegin(7);
        this.glColor(topLeft);
        GL11.glVertex2f(left + radius, top);
        this.glColor(bottomLeft);
        GL11.glVertex2f(left + radius, bottom);
        this.glColor(bottomRight);
        GL11.glVertex2f(right - radius, bottom);
        this.glColor(topRight);
        GL11.glVertex2f(right - radius, top);
        GL11.glEnd();
        GlStateManager.func_179103_j(7424);
        GlStateManager.func_179084_k();
        GlStateManager.func_179098_w();
    }

    public void drawRoundedRectangle(float left, float top, float right, float bottom, float radius, int color) {
        float minDimension = Math.min(right - left, bottom - top);
        if (radius > minDimension / 2.0F) {
            radius = minDimension / 2.0F;
        }

        GlStateManager.func_179147_l();
        GlStateManager.func_179090_x();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        this.glColor(color);
        GL11.glBegin(9);

        for (int i = 0; i <= 90; i += 3) {
            GL11.glVertex2f(
                (float)(left + radius + Math.sin(Math.toRadians(i)) * radius * -1.0),
                (float)(top + radius + Math.cos(Math.toRadians(i)) * radius * -1.0)
            );
        }

        for (int i = 90; i <= 180; i += 3) {
            GL11.glVertex2f(
                (float)(left + radius + Math.sin(Math.toRadians(i)) * radius * -1.0),
                (float)(bottom - radius + Math.cos(Math.toRadians(i)) * radius * -1.0)
            );
        }

        for (int i = 0; i <= 90; i += 3) {
            GL11.glVertex2f(
                (float)(right - radius + Math.sin(Math.toRadians(i)) * radius),
                (float)(bottom - radius + Math.cos(Math.toRadians(i)) * radius)
            );
        }

        for (int i = 90; i <= 180; i += 3) {
            GL11.glVertex2f(
                (float)(right - radius + Math.sin(Math.toRadians(i)) * radius),
                (float)(top + radius + Math.cos(Math.toRadians(i)) * radius)
            );
        }

        GL11.glEnd();
        GL11.glBegin(7);
        GL11.glVertex2f(left, top + radius);
        GL11.glVertex2f(left, bottom - radius);
        GL11.glVertex2f(right, bottom - radius);
        GL11.glVertex2f(right, top + radius);
        GL11.glEnd();
        GL11.glBegin(7);
        GL11.glVertex2f(left + radius, top);
        GL11.glVertex2f(left + radius, bottom);
        GL11.glVertex2f(right - radius, bottom);
        GL11.glVertex2f(right - radius, top);
        GL11.glEnd();
        GlStateManager.func_179084_k();
        GlStateManager.func_179098_w();
    }

    public void glColor(int color) {
        GL11.glColor4f(
            (color >> 16 & 0xFF) / 255.0F,
            (color >> 8 & 0xFF) / 255.0F,
            (color & 0xFF) / 255.0F,
            (color >> 24 & 0xFF) / 255.0F
        );
    }

    public int mergeAlpha(int rgb, int alpha) {
        return rgb & 16777215 | alpha << 24;
    }
}
