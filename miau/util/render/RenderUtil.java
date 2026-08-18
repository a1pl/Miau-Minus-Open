package miau.util.render;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;
import miau.Miau;
import miau.enums.ChatColors;
import miau.mixin.IAccessorEntityRenderer;
import miau.mixin.IAccessorMinecraft;
import miau.mixin.IAccessorRenderManager;
import miau.ui.clickgui.ClickGui;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

public class RenderUtil {
    private static Minecraft mc = Minecraft.func_71410_x();
    private static Frustum cameraFrustum = new Frustum();
    private static IntBuffer viewportBuffer = GLAllocation.func_74527_f(16);
    private static FloatBuffer modelViewBuffer = GLAllocation.func_74529_h(16);
    private static FloatBuffer projectionBuffer = GLAllocation.func_74529_h(16);
    private static FloatBuffer vectorBuffer = GLAllocation.func_74529_h(4);
    private static Map<Integer, RenderUtil.EnchantmentData> enchantmentMap = new RenderUtil.EnchantmentMap();
    private static Frustum frustum = new Frustum();
    private static final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private static final FloatBuffer SCREEN_COORDS = BufferUtils.createFloatBuffer(3);
    private static final int SCISSOR_PUSH_STACK_DEPTH = 4;
    private static final IntBuffer SCISSOR_PUSH_BUF = BufferUtils.createIntBuffer(16);
    private static final int[][] scissorPushStack = new int[4][5];
    private static int scissorPushDepth = 0;
    private static final Map<String, ResourceLocation> iconCache = new HashMap<>();

    private static ChatColors getColorForLevel(int currentLevel, int maxLevel) {
        if (currentLevel > maxLevel) {
            return ChatColors.LIGHT_PURPLE;
        }

        if (currentLevel == maxLevel) {
            return ChatColors.RED;
        }

        switch (currentLevel) {
            case 1:
                return ChatColors.AQUA;
            case 2:
                return ChatColors.GREEN;
            case 3:
                return ChatColors.YELLOW;
            case 4:
                return ChatColors.GOLD;
            default:
                return ChatColors.GRAY;
        }
    }

    public static void drawOutlinedString(String text, float x, float y) {
        String string2 = text.replaceAll("(?i)§[\\da-f]", "");
        mc.field_71466_p.func_175065_a(string2, x + 1.0F, y, 0, false);
        mc.field_71466_p.func_175065_a(string2, x - 1.0F, y, 0, false);
        mc.field_71466_p.func_175065_a(string2, x, y + 1.0F, 0, false);
        mc.field_71466_p.func_175065_a(string2, x, y - 1.0F, 0, false);
        mc.field_71466_p.func_175065_a(text, x, y, -1, false);
    }

    public static void renderEnchantmentText(ItemStack itemStack, float x, float y, float scale) {
        NBTTagList nBTTagList = itemStack.func_77973_b() == Items.field_151134_bR
            ? Items.field_151134_bR.func_92110_g(itemStack)
            : itemStack.func_77986_q();
        if (nBTTagList != null) {
            for (int i = 0; i < nBTTagList.func_74745_c(); i++) {
                RenderUtil.EnchantmentData enchantmentData = enchantmentMap.get(
                    nBTTagList.func_150305_b(i).func_74762_e("id")
                );
                if (enchantmentData != null) {
                    short s = nBTTagList.func_150305_b(i).func_74765_d("lvl");
                    ChatColors chatColors = getColorForLevel(s, enchantmentData.maxLevel);
                    drawOutlinedString(
                        ChatColors.formatColor(
                            String.format("&r%s%s%d&r", enchantmentData.shortName, chatColors, Integer.valueOf(s))
                        ),
                        x * (1.0F / scale),
                        (y + i * 4.0F) * (1.0F / scale)
                    );
                }
            }
        }
    }

    public static void renderItemInGUI(ItemStack itemStack, int x, int y) {
        GlStateManager.func_179094_E();
        GlStateManager.func_179132_a(true);
        GlStateManager.func_179086_m(256);
        RenderHelper.func_74520_c();
        GL11.glDisable(2896);
        GlStateManager.func_179094_E();
        GlStateManager.func_179152_a(1.0F, 1.0F, -0.01F);
        mc.func_175599_af().field_77023_b = -150.0F;
        mc.func_175599_af().func_180450_b(itemStack, x, y);
        mc.func_175599_af().func_175030_a(mc.field_71466_p, itemStack, x, y);
        mc.func_175599_af().field_77023_b = 0.0F;
        GlStateManager.func_179121_F();
        RenderHelper.func_74518_a();
        GlStateManager.func_179141_d();
        GlStateManager.func_179084_k();
        GlStateManager.func_179098_w();
        GlStateManager.func_179121_F();
        GlStateManager.func_179094_E();
        GlStateManager.func_179152_a(0.5F, 0.5F, 0.5F);
        GlStateManager.func_179097_i();
        renderEnchantmentText(itemStack, x, y, 0.5F);
        GlStateManager.func_179126_j();
        GlStateManager.func_179152_a(2.0F, 2.0F, 2.0F);
        GlStateManager.func_179121_F();
    }

    public static void renderPotionEffect(PotionEffect potionEffect, int x, int y) {
        int n3 = Potion.field_76425_a[potionEffect.func_76456_a()].func_76392_e();
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.func_179094_E();
        GlStateManager.func_179132_a(true);
        GlStateManager.func_179086_m(256);
        GlStateManager.func_179094_E();
        GlStateManager.func_179152_a(1.0F, 1.0F, -0.01F);
        mc.func_110434_K().func_110577_a(new ResourceLocation("textures/gui/container/inventory.png"));
        Gui.func_146110_a(x, y, n3 % 8 * 18, 198 + n3 / 8 * 18, 18, 18, 256.0F, 256.0F);
        GlStateManager.func_179121_F();
        GlStateManager.func_179141_d();
        GlStateManager.func_179084_k();
        GlStateManager.func_179098_w();
        GlStateManager.func_179121_F();
    }

    public static void drawRect(float x1, float y1, float x2, float y2, int color) {
        if (color != 0) {
            boolean texture2D = GL11.glIsEnabled(3553);
            boolean blend = GL11.glIsEnabled(3042);
            if (texture2D) {
                GL11.glDisable(3553);
            }

            if (!blend) {
                GL11.glEnable(3042);
            }

            GL11.glBlendFunc(770, 771);
            setColor(color);
            Tessellator tessellator = Tessellator.func_178181_a();
            WorldRenderer worldrenderer = tessellator.func_178180_c();
            worldrenderer.func_181668_a(7, DefaultVertexFormats.field_181705_e);
            worldrenderer.func_181662_b(x1, y1, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x1, y2, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x2, y2, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x2, y1, 0.0).func_181675_d();
            tessellator.func_78381_a();
            if (!blend) {
                GL11.glDisable(3042);
            }

            if (texture2D) {
                GL11.glEnable(3553);
            }

            GlStateManager.func_179117_G();
        }
    }

    public static void drawOutLineRect(
        float x, float y, float width, float height, float size, Color internalColor, Color borderColor
    ) {
        drawRect(x, y, x + width, y + height, internalColor.getRGB());
        drawRect(x, y, x + size, y + height, borderColor.getRGB());
        drawRect(x, y - size, x + width + size, y, borderColor.getRGB());
        drawRect(x + width, y, x + width + size, y + height, borderColor.getRGB());
        drawRect(x, y + height - size, x + width, y + height, borderColor.getRGB());
    }

    public static int getContrastTextColor(Color bgColor) {
        double luminance = (0.299 * bgColor.getRed() + 0.587 * bgColor.getGreen() + 0.114 * bgColor.getBlue()) / 255.0;
        return luminance > 0.6 ? -16777216 : -1;
    }

    public static void drawRect3D(float x1, float y1, float x2, float y2, int color) {
        if (color != 0) {
            setColor(color);
            GL11.glEnable(2881);
            GL11.glHint(3155, 4354);
            Tessellator tessellator = Tessellator.func_178181_a();
            WorldRenderer worldrenderer = tessellator.func_178180_c();
            worldrenderer.func_181668_a(9, DefaultVertexFormats.field_181705_e);

            for (int i = 0; i < 2; i++) {
                worldrenderer.func_181662_b(x1, y1, 0.0).func_181675_d();
                worldrenderer.func_181662_b(x1, y2, 0.0).func_181675_d();
                worldrenderer.func_181662_b(x2, y2, 0.0).func_181675_d();
                worldrenderer.func_181662_b(x2, y1, 0.0).func_181675_d();
            }

            tessellator.func_78381_a();
            GL11.glDisable(2881);
            GlStateManager.func_179117_G();
        }
    }

    public static void drawOutlineRect(
        float x1, float y1, float x2, float y2, float lineWidth, int backgroundColor, int lineColor
    ) {
        drawRect(0.0F, 0.0F, x2, 27.0F, backgroundColor);
        if (lineColor != 0) {
            setColor(lineColor);
            GL11.glLineWidth(lineWidth);
            GL11.glEnable(2848);
            GL11.glHint(3154, 4354);
            Tessellator tessellator = Tessellator.func_178181_a();
            WorldRenderer worldrenderer = tessellator.func_178180_c();
            worldrenderer.func_181668_a(1, DefaultVertexFormats.field_181705_e);
            worldrenderer.func_181662_b(x1, y1, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x1, y2, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x2, y2, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x2, y1, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x1, y1, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x2, y1, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x1, y2, 0.0).func_181675_d();
            worldrenderer.func_181662_b(x2, y2, 0.0).func_181675_d();
            tessellator.func_78381_a();
            GL11.glDisable(2848);
            GL11.glLineWidth(2.0F);
            GlStateManager.func_179117_G();
        }
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(1, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(x1, y1, 0.0).func_181675_d();
        worldrenderer.func_181662_b(x2, y2, 0.0).func_181675_d();
        tessellator.func_78381_a();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0F);
        GlStateManager.func_179117_G();
    }

    public static void drawLine3D(
        Vec3 start,
        double endX,
        double endY,
        double endZ,
        float red,
        float green,
        float blue,
        float alpha,
        float lineWidth
    ) {
        GlStateManager.func_179094_E();
        GlStateManager.func_179131_c(red, green, blue, alpha);
        boolean bl = mc.field_71474_y.field_74336_f;
        mc.field_71474_y.field_74336_f = false;
        ((IAccessorEntityRenderer)mc.field_71460_t)
            .callSetupCameraTransform(((IAccessorMinecraft)mc).getTimer().field_74281_c, 2);
        mc.field_71474_y.field_74336_f = bl;
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(1, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(start.field_72450_a, start.field_72448_b, start.field_72449_c).func_181675_d();
        worldrenderer.func_181662_b(
                endX - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX(),
                endY - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY(),
                endZ - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ()
            )
            .func_181675_d();
        tessellator.func_78381_a();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0F);
        GlStateManager.func_179117_G();
        GlStateManager.func_179121_F();
    }

    public static void drawArrow(float centerX, float centerY, float angle, float length, float lineWidth, int color) {
        float f6 = angle + (float)Math.toRadians(45.0);
        float f7 = angle - (float)Math.toRadians(45.0);
        setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(1, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(centerX, centerY, 0.0).func_181675_d();
        worldrenderer.func_181662_b(centerX + length * (float)Math.cos(f6), centerY + length * (float)Math.sin(f6), 0.0)
            .func_181675_d();
        worldrenderer.func_181662_b(centerX, centerY, 0.0).func_181675_d();
        worldrenderer.func_181662_b(centerX + length * (float)Math.cos(f7), centerY + length * (float)Math.sin(f7), 0.0)
            .func_181675_d();
        tessellator.func_78381_a();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0F);
        GlStateManager.func_179117_G();
    }

    public static void drawTriangle(float centerX, float centerY, float angle, float length, int color) {
        float f5 = angle + (float)Math.toRadians(26.25);
        float f6 = angle - (float)Math.toRadians(26.25);
        setColor(color);
        GL11.glEnable(2881);
        GL11.glHint(3155, 4354);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(9, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(centerX, centerY, 0.0).func_181675_d();
        worldrenderer.func_181662_b(centerX + length * (float)Math.cos(f5), centerY + length * (float)Math.sin(f5), 0.0)
            .func_181675_d();
        worldrenderer.func_181662_b(centerX + length * (float)Math.cos(f6), centerY + length * (float)Math.sin(f6), 0.0)
            .func_181675_d();
        tessellator.func_78381_a();
        GL11.glDisable(2881);
        GlStateManager.func_179117_G();
    }

    public static void drawFramebuffer(Framebuffer framebuffer) {
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        GlStateManager.func_179144_i(framebuffer.field_147617_g);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(7, DefaultVertexFormats.field_181707_g);
        worldrenderer.func_181662_b(0.0, 0.0, 0.0).func_181673_a(0.0, 1.0).func_181675_d();
        worldrenderer.func_181662_b(0.0, scaledResolution.func_78328_b(), 0.0).func_181673_a(0.0, 0.0).func_181675_d();
        worldrenderer.func_181662_b(scaledResolution.func_78326_a(), scaledResolution.func_78328_b(), 0.0)
            .func_181673_a(1.0, 0.0)
            .func_181675_d();
        worldrenderer.func_181662_b(scaledResolution.func_78326_a(), 0.0, 0.0).func_181673_a(1.0, 1.0).func_181675_d();
        tessellator.func_78381_a();
    }

    public static void fillCircle(double x, double y, double radius, int segments, int color) {
        GlStateManager.func_179147_l();
        GlStateManager.func_179090_x();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        setColor(color);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(6, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(x, y, 0.0).func_181675_d();

        for (int i = 0; i <= segments; i++) {
            double angle = i * ((Math.PI * 2) / segments);
            double px = x + Math.cos(angle) * radius;
            double py = y + Math.sin(angle) * radius;
            worldrenderer.func_181662_b(px, py, 0.0).func_181675_d();
        }

        tessellator.func_78381_a();
        GlStateManager.func_179098_w();
        GlStateManager.func_179084_k();
        GlStateManager.func_179117_G();
    }

    public static void drawCircle(
        double centerX, double centerY, double centerZ, double radius, int segments, int color
    ) {
        setColor(color);
        GL11.glLineWidth(3.0F);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(2, DefaultVertexFormats.field_181705_e);

        for (int i = 0; i <= segments; i++) {
            double d5 = i * ((Math.PI * 2) / segments);
            worldrenderer.func_181662_b(centerX + Math.cos(d5) * radius, centerY, centerZ + Math.sin(d5) * radius)
                .func_181675_d();
        }

        tessellator.func_78381_a();
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0F);
        GlStateManager.func_179117_G();
    }

    public static void drawEntityCircle(Entity entity, double radius, int segments, int color) {
        double d2 = lerpDouble(
                entity.field_70165_t, entity.field_70142_S, ((IAccessorMinecraft)mc).getTimer().field_74281_c
            )
            - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX();
        double d3 = lerpDouble(
                entity.field_70163_u, entity.field_70137_T, ((IAccessorMinecraft)mc).getTimer().field_74281_c
            )
            - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY();
        double d4 = lerpDouble(
                entity.field_70161_v, entity.field_70136_U, ((IAccessorMinecraft)mc).getTimer().field_74281_c
            )
            - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ();
        drawCircle(d2, d3, d4, radius, segments, color);
    }

    public static void drawFilledBox(AxisAlignedBB axisAlignedBB, int red, int green, int blue) {
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldRenderer = tessellator.func_178180_c();
        worldRenderer.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        worldRenderer.func_181662_b(
                axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f
            )
            .func_181669_b(red, green, blue, 63)
            .func_181675_d();
        tessellator.func_78381_a();
    }

    public static void drawBoundingBox(
        AxisAlignedBB axisAlignedBB, int red, int green, int blue, int alpha, float lineWidth
    ) {
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        RenderGlobal.func_181563_a(axisAlignedBB, red, green, blue, alpha);
        GL11.glDisable(2848);
        GL11.glLineWidth(2.0F);
    }

    public static void drawEntityBox(Entity entity, int red, int green, int blue) {
        double d2 = lerpDouble(
            entity.field_70165_t, entity.field_70142_S, ((IAccessorMinecraft)mc).getTimer().field_74281_c
        );
        double d3 = lerpDouble(
            entity.field_70163_u, entity.field_70137_T, ((IAccessorMinecraft)mc).getTimer().field_74281_c
        );
        double d4 = lerpDouble(
            entity.field_70161_v, entity.field_70136_U, ((IAccessorMinecraft)mc).getTimer().field_74281_c
        );
        drawFilledBox(
            entity.func_174813_aQ()
                .func_72314_b(0.1F, 0.1F, 0.1F)
                .func_72317_d(d2 - entity.field_70165_t, d3 - entity.field_70163_u, d4 - entity.field_70161_v)
                .func_72317_d(
                    -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX(),
                    -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY(),
                    -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ()
                ),
            red,
            green,
            blue
        );
    }

    public static void drawEntityBoundingBox(
        Entity entity, int red, int green, int blue, int alpha, float lineWidth, double expand
    ) {
        double d2 = lerpDouble(
            entity.field_70165_t, entity.field_70142_S, ((IAccessorMinecraft)mc).getTimer().field_74281_c
        );
        double d3 = lerpDouble(
            entity.field_70163_u, entity.field_70137_T, ((IAccessorMinecraft)mc).getTimer().field_74281_c
        );
        double d4 = lerpDouble(
            entity.field_70161_v, entity.field_70136_U, ((IAccessorMinecraft)mc).getTimer().field_74281_c
        );
        drawBoundingBox(
            entity.func_174813_aQ()
                .func_72314_b(expand, expand, expand)
                .func_72317_d(d2 - entity.field_70165_t, d3 - entity.field_70163_u, d4 - entity.field_70161_v)
                .func_72317_d(
                    -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX(),
                    -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY(),
                    -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ()
                ),
            red,
            green,
            blue,
            alpha,
            lineWidth
        );
    }

    public static void drawBlockBox(BlockPos blockPos, double height, int red, int green, int blue) {
        drawFilledBox(
            new AxisAlignedBB(
                    blockPos.func_177958_n(),
                    blockPos.func_177956_o(),
                    blockPos.func_177952_p(),
                    blockPos.func_177958_n() + 1.0,
                    blockPos.func_177956_o() + height,
                    blockPos.func_177952_p() + 1.0
                )
                .func_72317_d(
                    -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX(),
                    -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY(),
                    -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ()
                ),
            red,
            green,
            blue
        );
    }

    public static void drawBlockBoundingBox(
        BlockPos blockPos, double height, int red, int green, int blue, int alpha, float lineWidth
    ) {
        drawBoundingBox(
            new AxisAlignedBB(
                    blockPos.func_177958_n(),
                    blockPos.func_177956_o(),
                    blockPos.func_177952_p(),
                    blockPos.func_177958_n() + 1.0,
                    blockPos.func_177956_o() + height,
                    blockPos.func_177952_p() + 1.0
                )
                .func_72317_d(
                    -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX(),
                    -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY(),
                    -((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ()
                ),
            red,
            green,
            blue,
            alpha,
            lineWidth
        );
    }

    public static void drawCornerESP(EntityPlayer entity, float red, float green, float blue) {
        float x = (float)(
            lerpDouble(entity.field_70165_t, entity.field_70142_S, ((IAccessorMinecraft)mc).getTimer().field_74281_c)
                - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX()
        );
        float y = (float)(
            lerpDouble(entity.field_70163_u, entity.field_70137_T, ((IAccessorMinecraft)mc).getTimer().field_74281_c)
                - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY()
        );
        float z = (float)(
            lerpDouble(entity.field_70161_v, entity.field_70136_U, ((IAccessorMinecraft)mc).getTimer().field_74281_c)
                - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ()
        );
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b(x, y + entity.field_70131_O / 2.0F, z);
        GlStateManager.func_179114_b(-mc.func_175598_ae().field_78735_i, 0.0F, 1.0F, 0.0F);
        GlStateManager.func_179152_a(-0.098F, -0.098F, 0.098F);
        float width = (float)(26.6 * entity.field_70130_N / 2.0);
        float height = 12.0F;
        GlStateManager.func_179124_c(red, green, blue);
        draw3DRect(width, height - 1.0F, width - 4.0F, height);
        draw3DRect(-width, height - 1.0F, -width + 4.0F, height);
        draw3DRect(-width, height, -width + 1.0F, height - 4.0F);
        draw3DRect(width, height, width - 1.0F, height - 4.0F);
        draw3DRect(width, -height, width - 4.0F, -height + 1.0F);
        draw3DRect(-width, -height, -width + 4.0F, -height + 1.0F);
        draw3DRect(-width, -height + 1.0F, -width + 1.0F, -height + 4.0F);
        draw3DRect(width, -height + 1.0F, width - 1.0F, -height + 4.0F);
        GlStateManager.func_179124_c(0.0F, 0.0F, 0.0F);
        draw3DRect(width, height, width - 4.0F, height + 0.2F);
        draw3DRect(-width, height, -width + 4.0F, height + 0.2F);
        draw3DRect(-width - 0.2F, height + 0.2F, -width, height - 4.0F);
        draw3DRect(width + 0.2F, height + 0.2F, width, height - 4.0F);
        draw3DRect(width + 0.2F, -height, width - 4.0F, -height - 0.2F);
        draw3DRect(-width - 0.2F, -height, -width + 4.0F, -height - 0.2F);
        draw3DRect(-width - 0.2F, -height, -width, -height + 4.0F);
        draw3DRect(width + 0.2F, -height, width, -height + 4.0F);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.func_179121_F();
    }

    public static void drawFake2DESP(EntityPlayer entity, float red, float green, float blue) {
        float x = (float)(
            lerpDouble(entity.field_70165_t, entity.field_70142_S, ((IAccessorMinecraft)mc).getTimer().field_74281_c)
                - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX()
        );
        float y = (float)(
            lerpDouble(entity.field_70163_u, entity.field_70137_T, ((IAccessorMinecraft)mc).getTimer().field_74281_c)
                - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY()
        );
        float z = (float)(
            lerpDouble(entity.field_70161_v, entity.field_70136_U, ((IAccessorMinecraft)mc).getTimer().field_74281_c)
                - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ()
        );
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b(x, y + entity.field_70131_O / 2.0F, z);
        GlStateManager.func_179114_b(-mc.func_175598_ae().field_78735_i, 0.0F, 1.0F, 0.0F);
        GlStateManager.func_179152_a(-0.1F, -0.1F, 0.1F);
        GlStateManager.func_179124_c(red, green, blue);
        float width = (float)(23.3 * entity.field_70130_N / 2.0);
        float height = 12.0F;
        draw3DRect(width, height, -width, height + 0.4F);
        draw3DRect(width, -height, -width, -height + 0.4F);
        draw3DRect(width, -height + 0.4F, width - 0.4F, height + 0.4F);
        draw3DRect(-width, -height + 0.4F, -width + 0.4F, height + 0.4F);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.func_179121_F();
    }

    public static void draw3DRect(float x1, float y1, float x2, float y2) {
        GL11.glBegin(9);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
    }

    public static Vector4d projectToScreen(Entity entity, double screenScale) {
        double d3 = lerpDouble(
            entity.field_70165_t, entity.field_70142_S, ((IAccessorMinecraft)mc).getTimer().field_74281_c
        );
        double d4 = lerpDouble(
            entity.field_70163_u, entity.field_70137_T, ((IAccessorMinecraft)mc).getTimer().field_74281_c
        );
        double d5 = lerpDouble(
            entity.field_70161_v, entity.field_70136_U, ((IAccessorMinecraft)mc).getTimer().field_74281_c
        );
        AxisAlignedBB axisAlignedBB = entity.func_174813_aQ()
            .func_72314_b(0.1F, 0.1F, 0.1F)
            .func_72317_d(d3 - entity.field_70165_t, d4 - entity.field_70163_u, d5 - entity.field_70161_v);
        Vector4d vector4d = null;

        for (Vector3d vector3d : new Vector3d[]{
            new Vector3d(axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c),
            new Vector3d(axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c),
            new Vector3d(axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c),
            new Vector3d(axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72339_c),
            new Vector3d(axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f),
            new Vector3d(axisAlignedBB.field_72340_a, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f),
            new Vector3d(axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f),
            new Vector3d(axisAlignedBB.field_72336_d, axisAlignedBB.field_72337_e, axisAlignedBB.field_72334_f)
        }) {
            GL11.glGetFloat(2982, modelViewBuffer);
            GL11.glGetFloat(2983, projectionBuffer);
            GL11.glGetInteger(2978, viewportBuffer);
            if (GLU.gluProject(
                (float)(vector3d.x - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX()),
                (float)(vector3d.y - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY()),
                (float)(vector3d.z - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ()),
                modelViewBuffer,
                projectionBuffer,
                viewportBuffer,
                vectorBuffer
            )) {
                vector3d = new Vector3d(
                    vectorBuffer.get(0) / screenScale,
                    (Display.getHeight() - vectorBuffer.get(1)) / screenScale,
                    vectorBuffer.get(2)
                );
                if (vector3d.z >= 0.0 && vector3d.z < 1.0) {
                    if (vector4d == null) {
                        vector4d = new Vector4d(vector3d.x, vector3d.y, vector3d.z, 0.0);
                    }

                    vector4d.x = Math.min(vector3d.x, vector4d.x);
                    vector4d.y = Math.min(vector3d.y, vector4d.y);
                    vector4d.z = Math.max(vector3d.x, vector4d.z);
                    vector4d.w = Math.max(vector3d.y, vector4d.w);
                }
            }
        }

        return vector4d;
    }

    public static boolean isInViewFrustum(AxisAlignedBB axisAlignedBB, double expand) {
        cameraFrustum.func_78547_a(
            mc.func_175606_aa().field_70165_t, mc.func_175606_aa().field_70163_u, mc.func_175606_aa().field_70161_v
        );
        return cameraFrustum.func_78546_a(axisAlignedBB.func_72314_b(expand, expand, expand));
    }

    public static void enableRenderState() {
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        GlStateManager.func_179090_x();
        GlStateManager.func_179129_p();
        GlStateManager.func_179118_c();
        GlStateManager.func_179097_i();
    }

    public static void disableRenderState() {
        GlStateManager.func_179126_j();
        GlStateManager.func_179141_d();
        GlStateManager.func_179089_o();
        GlStateManager.func_179098_w();
        GlStateManager.func_179084_k();
    }

    public static void setColor(int argb) {
        float f = (argb >> 24 & 0xFF) / 255.0F;
        float f2 = (argb >> 16 & 0xFF) / 255.0F;
        float f3 = (argb >> 8 & 0xFF) / 255.0F;
        float f4 = (argb & 0xFF) / 255.0F;
        GlStateManager.func_179131_c(f2, f3, f4, f);
    }

    public static float lerpFloat(float current, float previous, float t) {
        return previous + (current - previous) * t;
    }

    public static double lerpDouble(double current, double previous, double t) {
        return previous + (current - previous) * t;
    }

    public static void renderBlock(BlockPos blockPos, int color, boolean outline, boolean shade) {
        renderBox(
            blockPos.func_177958_n(),
            blockPos.func_177956_o(),
            blockPos.func_177952_p(),
            1.0,
            1.0,
            1.0,
            color,
            outline,
            shade
        );
    }

    public static void renderChest(BlockPos blockPos, int color, boolean outline, boolean shade) {
        renderBox(
            blockPos.func_177958_n() + 0.0625F,
            blockPos.func_177956_o(),
            blockPos.func_177952_p() + 0.0625F,
            0.875,
            0.875,
            0.875,
            color,
            outline,
            shade
        );
    }

    public static void renderChestBatch(List<BlockPos> positions, int color, boolean outline, boolean shade) {
        renderChestBatch(positions, color, color, outline, shade);
    }

    public static void renderChestBatch(
        List<BlockPos> positions, int outlineColor, int shadeColor, boolean outline, boolean shade
    ) {
        if (positions != null && !positions.isEmpty()) {
            double vx = mc.func_175598_ae().field_78730_l;
            double vy = mc.func_175598_ae().field_78731_m;
            double vz = mc.func_175598_ae().field_78728_n;
            GL11.glPushMatrix();
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(3042);
            GL11.glLineWidth(2.0F);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            float outlineA = (outlineColor >> 24 & 0xFF) / 255.0F;
            float outlineR = (outlineColor >> 16 & 0xFF) / 255.0F;
            float outlineG = (outlineColor >> 8 & 0xFF) / 255.0F;
            float outlineB = (outlineColor & 0xFF) / 255.0F;
            float shadeA = (shadeColor >> 24 & 0xFF) / 255.0F;
            float shadeR = (shadeColor >> 16 & 0xFF) / 255.0F;
            float shadeG = (shadeColor >> 8 & 0xFF) / 255.0F;
            float shadeB = (shadeColor & 0xFF) / 255.0F;

            for (BlockPos blockPos : positions) {
                double xPos = blockPos.func_177958_n() + 0.0625 - vx;
                double yPos = blockPos.func_177956_o() - vy;
                double zPos = blockPos.func_177952_p() + 0.0625 - vz;
                AxisAlignedBB axisAlignedBB = new AxisAlignedBB(
                    xPos, yPos, zPos, xPos + 0.875, yPos + 0.875, zPos + 0.875
                );
                if (outline) {
                    GL11.glColor4f(outlineR, outlineG, outlineB, outlineA);
                    RenderGlobal.func_181561_a(axisAlignedBB);
                }

                if (shade) {
                    drawBoundingBox(axisAlignedBB, shadeR, shadeG, shadeB, shadeA);
                }
            }

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glDisable(3042);
            GL11.glPopMatrix();
        }
    }

    public static void renderBlock(BlockPos blockPos, int color, double y2, boolean outline, boolean shade) {
        renderBox(
            blockPos.func_177958_n(),
            blockPos.func_177956_o(),
            blockPos.func_177952_p(),
            1.0,
            y2,
            1.0,
            color,
            outline,
            shade
        );
    }

    public static void scissor(double x, double y, double width, double height) {
        ScaledResolution sr = new ScaledResolution(mc);
        if (mc.field_71462_r instanceof ClickGui && ClickGui.openingScale != 1.0F) {
            double scaleFactor = ClickGui.openingScale;
            double centerX = sr.func_78326_a() / 2.0;
            double centerY = sr.func_78328_b() / 2.0;
            x = centerX + (x - centerX) * scaleFactor;
            y = centerY + (y - centerY) * scaleFactor;
            width *= scaleFactor;
            height *= scaleFactor;
        }

        double guiScale = ClickGui.getActiveRenderScale();
        x *= guiScale;
        y *= guiScale;
        width *= guiScale;
        height *= guiScale;
        int scale = sr.func_78325_e();
        double screenH = sr.func_78328_b();
        int left = (int)Math.floor(x * scale);
        int right = (int)Math.ceil((x + width) * scale);
        int scaledWidth = Math.max(0, right - left);
        double bottomGui = y + height;
        int glBottom = (int)Math.floor((screenH - bottomGui) * scale);
        int glTop = (int)Math.ceil((screenH - y) * scale);
        int scaledHeight = Math.max(0, glTop - glBottom);
        if (scaledWidth >= 0 && scaledHeight >= 0) {
            GL11.glScissor(left, glBottom, scaledWidth, scaledHeight);
        }
    }

    public static void scissorPushGui(double x, double y, double width, double height) {
        ScaledResolution sr = new ScaledResolution(mc);
        if (mc.field_71462_r instanceof ClickGui && ClickGui.openingScale != 1.0F) {
            double scaleFactor = ClickGui.openingScale;
            double centerX = sr.func_78326_a() / 2.0;
            double centerY = sr.func_78328_b() / 2.0;
            x = centerX + (x - centerX) * scaleFactor;
            y = centerY + (y - centerY) * scaleFactor;
            width *= scaleFactor;
            height *= scaleFactor;
        }

        double guiScale = ClickGui.getActiveRenderScale();
        x *= guiScale;
        y *= guiScale;
        width *= guiScale;
        height *= guiScale;
        int scale = sr.func_78325_e();
        double screenH = sr.func_78328_b();
        int left = (int)Math.floor(x * scale);
        int right = (int)Math.ceil((x + width) * scale);
        int scaledWidth = Math.max(0, right - left);
        double bottomGui = y + height;
        int glBottom = (int)Math.floor((screenH - bottomGui) * scale);
        int glTop = (int)Math.ceil((screenH - y) * scale);
        int scaledHeight = Math.max(0, glTop - glBottom);
        boolean wasEnabled = GL11.glIsEnabled(3089);
        int[] saved = scissorPushStack[scissorPushDepth++];
        if (scissorPushDepth > 4) {
            throw new IllegalStateException("Scissor stack overflow");
        }

        if (wasEnabled) {
            ((Buffer)SCISSOR_PUSH_BUF).clear();
            GL11.glGetInteger(3088, SCISSOR_PUSH_BUF);
            saved[0] = 1;
            saved[1] = SCISSOR_PUSH_BUF.get(0);
            saved[2] = SCISSOR_PUSH_BUF.get(1);
            saved[3] = SCISSOR_PUSH_BUF.get(2);
            saved[4] = SCISSOR_PUSH_BUF.get(3);
            int ix = Math.max(saved[1], left);
            int iy = Math.max(saved[2], glBottom);
            int iw = Math.max(0, Math.min(saved[1] + saved[3], left + scaledWidth) - ix);
            int ih = Math.max(0, Math.min(saved[2] + saved[4], glBottom + scaledHeight) - iy);
            GL11.glScissor(ix, iy, iw, ih);
        } else {
            saved[0] = 0;
            GL11.glEnable(3089);
            GL11.glScissor(left, glBottom, scaledWidth, scaledHeight);
        }
    }

    public static void scissorPop() {
        int[] saved = scissorPushStack[--scissorPushDepth];
        if (saved[0] == 1) {
            GL11.glScissor(saved[1], saved[2], saved[3], saved[4]);
        } else {
            GL11.glDisable(3089);
        }
    }

    public static boolean isInViewFrustum(Entity entity) {
        return entity == null ? false : isInViewFrustum(entity.func_174813_aQ()) || entity.field_70158_ak;
    }

    public static boolean isInViewFrustum(AxisAlignedBB bb) {
        if (bb == null) {
            return false;
        }

        Entity view = mc.func_175606_aa();
        if (view == null) {
            return true;
        }

        frustum.func_78547_a(view.field_70165_t, view.field_70163_u, view.field_70161_v);
        return frustum.func_78546_a(bb);
    }

    public static boolean isWithinDistanceSqToRenderView(Entity entity, double maxDistSq) {
        if (entity == null) {
            return false;
        }

        Entity view = mc.func_175606_aa();
        return view == null ? false : entity.func_70068_e(view) <= maxDistSq;
    }

    public static boolean isBlockPosWithinDistanceSqToView(BlockPos pos, double maxDistSq) {
        if (pos == null) {
            return false;
        }

        Entity view = mc.func_175606_aa();
        if (view == null) {
            return false;
        }

        double dx = pos.func_177958_n() + 0.5 - view.field_70165_t;
        double dy = pos.func_177956_o() + 0.5 - view.field_70163_u;
        double dz = pos.func_177952_p() + 0.5 - view.field_70161_v;
        return dx * dx + dy * dy + dz * dz <= maxDistSq;
    }

    public static void drawRect(double left, double top, double right, double bottom, int color) {
        float f3 = (color >> 24 & 0xFF) / 255.0F;
        float f = (color >> 16 & 0xFF) / 255.0F;
        float f1 = (color >> 8 & 0xFF) / 255.0F;
        float f2 = (color & 0xFF) / 255.0F;
        GlStateManager.func_179094_E();
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        GlStateManager.func_179147_l();
        GlStateManager.func_179090_x();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        GlStateManager.func_179131_c(f, f1, f2, f3);
        worldrenderer.func_181668_a(7, DefaultVertexFormats.field_181705_e);
        worldrenderer.func_181662_b(left, bottom, 0.0).func_181675_d();
        worldrenderer.func_181662_b(right, bottom, 0.0).func_181675_d();
        worldrenderer.func_181662_b(right, top, 0.0).func_181675_d();
        worldrenderer.func_181662_b(left, top, 0.0).func_181675_d();
        tessellator.func_78381_a();
        GlStateManager.func_179098_w();
        GlStateManager.func_179084_k();
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.func_179121_F();
    }

    public static void drawPlayerBoundingBox(Vec3 pos, int color) {
        GlStateManager.func_179094_E();
        double x = pos.field_72450_a - mc.func_175598_ae().field_78730_l;
        double y = pos.field_72448_b - mc.func_175598_ae().field_78731_m;
        double z = pos.field_72449_c - mc.func_175598_ae().field_78728_n;
        AxisAlignedBB bbox = mc.field_71439_g.func_174813_aQ().func_72314_b(0.1, 0.1, 0.1);
        AxisAlignedBB axis = new AxisAlignedBB(
            bbox.field_72340_a - mc.field_71439_g.field_70165_t + x,
            bbox.field_72338_b - mc.field_71439_g.field_70163_u + y,
            bbox.field_72339_c - mc.field_71439_g.field_70161_v + z,
            bbox.field_72336_d - mc.field_71439_g.field_70165_t + x,
            bbox.field_72337_e - mc.field_71439_g.field_70163_u + y,
            bbox.field_72334_f - mc.field_71439_g.field_70161_v + z
        );
        float a = (color >> 24 & 0xFF) / 255.0F;
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        GL11.glLineWidth(2.0F);
        GL11.glColor4f(r, g, b, a);
        drawBoundingBox(axis, r, g, b, a);
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
        GlStateManager.func_179121_F();
    }

    public static void drawOutline(float x, float y, float x2, float y2, float lineWidth, int color) {
        float f5 = (color >> 24 & 0xFF) / 255.0F;
        float f6 = (color >> 16 & 0xFF) / 255.0F;
        float f7 = (color >> 8 & 0xFF) / 255.0F;
        float f8 = (color & 0xFF) / 255.0F;
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(2848);
        GL11.glPushMatrix();
        GL11.glColor4f(f6, f7, f8, f5);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(1);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x, y2);
        GL11.glVertex2d(x2, y2);
        GL11.glVertex2d(x2, y);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x2, y);
        GL11.glVertex2d(x, y2);
        GL11.glVertex2d(x2, y2);
        GL11.glEnd();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
    }

    public static void renderBox(
        double x, double y, double z, double x2, double y2, double z2, int color, boolean outline, boolean shade
    ) {
        double xPos = x - mc.func_175598_ae().field_78730_l;
        double yPos = y - mc.func_175598_ae().field_78731_m;
        double zPos = z - mc.func_175598_ae().field_78728_n;
        GL11.glPushMatrix();
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glLineWidth(2.0F);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        float n8 = (color >> 24 & 0xFF) / 255.0F;
        float n9 = (color >> 16 & 0xFF) / 255.0F;
        float n10 = (color >> 8 & 0xFF) / 255.0F;
        float n11 = (color & 0xFF) / 255.0F;
        GL11.glColor4f(n9, n10, n11, n8);
        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(xPos, yPos, zPos, xPos + x2, yPos + y2, zPos + z2);
        if (outline) {
            RenderGlobal.func_181561_a(axisAlignedBB);
        }

        if (shade) {
            drawBoundingBox(axisAlignedBB, n9, n10, n11);
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
        GL11.glPopMatrix();
    }

    public static void renderBlockFaces(
        BlockPos blockPos, int color, boolean outline, boolean shade, Set<EnumFacing> faces
    ) {
        if (faces != null && !faces.isEmpty()) {
            double xPos = blockPos.func_177958_n() - mc.func_175598_ae().field_78730_l;
            double yPos = blockPos.func_177956_o() - mc.func_175598_ae().field_78731_m;
            double zPos = blockPos.func_177952_p() - mc.func_175598_ae().field_78728_n;
            double maxX = xPos + 1.0;
            double maxY = yPos + 1.0;
            double maxZ = zPos + 1.0;
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            float outlineA = (color >> 24 & 0xFF) / 255.0F;
            float shadeA = 0.25F;
            GL11.glPushMatrix();
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(3042);
            GL11.glLineWidth(2.0F);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            Tessellator ts = Tessellator.func_178181_a();
            WorldRenderer vb = ts.func_178180_c();
            if (shade) {
                vb.func_181668_a(7, DefaultVertexFormats.field_181706_f);
                if (faces.contains(EnumFacing.DOWN)) {
                    vb.func_181662_b(xPos, yPos, zPos).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(maxX, yPos, zPos).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(maxX, yPos, maxZ).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(xPos, yPos, maxZ).func_181666_a(r, g, b, shadeA).func_181675_d();
                }

                if (faces.contains(EnumFacing.UP)) {
                    vb.func_181662_b(xPos, maxY, zPos).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(xPos, maxY, maxZ).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(maxX, maxY, maxZ).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(maxX, maxY, zPos).func_181666_a(r, g, b, shadeA).func_181675_d();
                }

                if (faces.contains(EnumFacing.NORTH)) {
                    vb.func_181662_b(xPos, yPos, zPos).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(xPos, maxY, zPos).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(maxX, maxY, zPos).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(maxX, yPos, zPos).func_181666_a(r, g, b, shadeA).func_181675_d();
                }

                if (faces.contains(EnumFacing.SOUTH)) {
                    vb.func_181662_b(maxX, yPos, maxZ).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(maxX, maxY, maxZ).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(xPos, maxY, maxZ).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(xPos, yPos, maxZ).func_181666_a(r, g, b, shadeA).func_181675_d();
                }

                if (faces.contains(EnumFacing.WEST)) {
                    vb.func_181662_b(xPos, yPos, zPos).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(xPos, maxY, zPos).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(xPos, maxY, maxZ).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(xPos, yPos, maxZ).func_181666_a(r, g, b, shadeA).func_181675_d();
                }

                if (faces.contains(EnumFacing.EAST)) {
                    vb.func_181662_b(maxX, yPos, maxZ).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(maxX, maxY, maxZ).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(maxX, maxY, zPos).func_181666_a(r, g, b, shadeA).func_181675_d();
                    vb.func_181662_b(maxX, yPos, zPos).func_181666_a(r, g, b, shadeA).func_181675_d();
                }

                ts.func_78381_a();
            }

            if (outline) {
                GL11.glColor4f(r, g, b, outlineA);
                vb.func_181668_a(1, DefaultVertexFormats.field_181705_e);
                if (faces.contains(EnumFacing.DOWN)) {
                    vb.func_181662_b(xPos, yPos, zPos).func_181675_d();
                    vb.func_181662_b(maxX, yPos, zPos).func_181675_d();
                    vb.func_181662_b(maxX, yPos, zPos).func_181675_d();
                    vb.func_181662_b(maxX, yPos, maxZ).func_181675_d();
                    vb.func_181662_b(maxX, yPos, maxZ).func_181675_d();
                    vb.func_181662_b(xPos, yPos, maxZ).func_181675_d();
                    vb.func_181662_b(xPos, yPos, maxZ).func_181675_d();
                    vb.func_181662_b(xPos, yPos, zPos).func_181675_d();
                }

                if (faces.contains(EnumFacing.UP)) {
                    vb.func_181662_b(xPos, maxY, zPos).func_181675_d();
                    vb.func_181662_b(maxX, maxY, zPos).func_181675_d();
                    vb.func_181662_b(maxX, maxY, zPos).func_181675_d();
                    vb.func_181662_b(maxX, maxY, maxZ).func_181675_d();
                    vb.func_181662_b(maxX, maxY, maxZ).func_181675_d();
                    vb.func_181662_b(xPos, maxY, maxZ).func_181675_d();
                    vb.func_181662_b(xPos, maxY, maxZ).func_181675_d();
                    vb.func_181662_b(xPos, maxY, zPos).func_181675_d();
                }

                if (faces.contains(EnumFacing.NORTH)) {
                    vb.func_181662_b(xPos, yPos, zPos).func_181675_d();
                    vb.func_181662_b(xPos, maxY, zPos).func_181675_d();
                    vb.func_181662_b(xPos, maxY, zPos).func_181675_d();
                    vb.func_181662_b(maxX, maxY, zPos).func_181675_d();
                    vb.func_181662_b(maxX, maxY, zPos).func_181675_d();
                    vb.func_181662_b(maxX, yPos, zPos).func_181675_d();
                    vb.func_181662_b(maxX, yPos, zPos).func_181675_d();
                    vb.func_181662_b(xPos, yPos, zPos).func_181675_d();
                }

                if (faces.contains(EnumFacing.SOUTH)) {
                    vb.func_181662_b(xPos, yPos, maxZ).func_181675_d();
                    vb.func_181662_b(xPos, maxY, maxZ).func_181675_d();
                    vb.func_181662_b(xPos, maxY, maxZ).func_181675_d();
                    vb.func_181662_b(maxX, maxY, maxZ).func_181675_d();
                    vb.func_181662_b(maxX, maxY, maxZ).func_181675_d();
                    vb.func_181662_b(maxX, yPos, maxZ).func_181675_d();
                    vb.func_181662_b(maxX, yPos, maxZ).func_181675_d();
                    vb.func_181662_b(xPos, yPos, maxZ).func_181675_d();
                }

                if (faces.contains(EnumFacing.WEST)) {
                    vb.func_181662_b(xPos, yPos, zPos).func_181675_d();
                    vb.func_181662_b(xPos, maxY, zPos).func_181675_d();
                    vb.func_181662_b(xPos, maxY, zPos).func_181675_d();
                    vb.func_181662_b(xPos, maxY, maxZ).func_181675_d();
                    vb.func_181662_b(xPos, maxY, maxZ).func_181675_d();
                    vb.func_181662_b(xPos, yPos, maxZ).func_181675_d();
                    vb.func_181662_b(xPos, yPos, maxZ).func_181675_d();
                    vb.func_181662_b(xPos, yPos, zPos).func_181675_d();
                }

                if (faces.contains(EnumFacing.EAST)) {
                    vb.func_181662_b(maxX, yPos, zPos).func_181675_d();
                    vb.func_181662_b(maxX, maxY, zPos).func_181675_d();
                    vb.func_181662_b(maxX, maxY, zPos).func_181675_d();
                    vb.func_181662_b(maxX, maxY, maxZ).func_181675_d();
                    vb.func_181662_b(maxX, maxY, maxZ).func_181675_d();
                    vb.func_181662_b(maxX, yPos, maxZ).func_181675_d();
                    vb.func_181662_b(maxX, yPos, maxZ).func_181675_d();
                    vb.func_181662_b(maxX, yPos, zPos).func_181675_d();
                }

                ts.func_78381_a();
            }

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glDisable(3042);
            GL11.glPopMatrix();
        }
    }

    private static void drawBoxFaceVertex(WorldRenderer wr, double x, double y, double z, int color) {
        wr.func_181662_b(x, y, z)
            .func_181669_b(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >> 24 & 0xFF)
            .func_181675_d();
    }

    private static void drawBoxFaceVertices(WorldRenderer wr, EnumFacing face, AxisAlignedBB box, int start, int end) {
        switch (face) {
            case UP:
                drawBoxFaceVertex(wr, box.field_72340_a, box.field_72337_e, box.field_72334_f, start);
                drawBoxFaceVertex(wr, box.field_72336_d, box.field_72337_e, box.field_72334_f, end);
                drawBoxFaceVertex(wr, box.field_72336_d, box.field_72337_e, box.field_72339_c, start);
                drawBoxFaceVertex(wr, box.field_72340_a, box.field_72337_e, box.field_72339_c, end);
                break;
            case DOWN:
                drawBoxFaceVertex(wr, box.field_72336_d, box.field_72338_b, box.field_72334_f, start);
                drawBoxFaceVertex(wr, box.field_72340_a, box.field_72338_b, box.field_72334_f, end);
                drawBoxFaceVertex(wr, box.field_72340_a, box.field_72338_b, box.field_72339_c, start);
                drawBoxFaceVertex(wr, box.field_72336_d, box.field_72338_b, box.field_72339_c, end);
                break;
            case NORTH:
                drawBoxFaceVertex(wr, box.field_72336_d, box.field_72337_e, box.field_72339_c, start);
                drawBoxFaceVertex(wr, box.field_72336_d, box.field_72338_b, box.field_72339_c, end);
                drawBoxFaceVertex(wr, box.field_72340_a, box.field_72338_b, box.field_72339_c, start);
                drawBoxFaceVertex(wr, box.field_72340_a, box.field_72337_e, box.field_72339_c, end);
                break;
            case SOUTH:
                drawBoxFaceVertex(wr, box.field_72340_a, box.field_72337_e, box.field_72334_f, start);
                drawBoxFaceVertex(wr, box.field_72340_a, box.field_72338_b, box.field_72334_f, end);
                drawBoxFaceVertex(wr, box.field_72336_d, box.field_72338_b, box.field_72334_f, start);
                drawBoxFaceVertex(wr, box.field_72336_d, box.field_72337_e, box.field_72334_f, end);
                break;
            case EAST:
                drawBoxFaceVertex(wr, box.field_72336_d, box.field_72337_e, box.field_72339_c, start);
                drawBoxFaceVertex(wr, box.field_72336_d, box.field_72337_e, box.field_72334_f, end);
                drawBoxFaceVertex(wr, box.field_72336_d, box.field_72338_b, box.field_72334_f, start);
                drawBoxFaceVertex(wr, box.field_72336_d, box.field_72338_b, box.field_72339_c, end);
                break;
            case WEST:
                drawBoxFaceVertex(wr, box.field_72340_a, box.field_72337_e, box.field_72334_f, start);
                drawBoxFaceVertex(wr, box.field_72340_a, box.field_72337_e, box.field_72339_c, end);
                drawBoxFaceVertex(wr, box.field_72340_a, box.field_72338_b, box.field_72339_c, start);
                drawBoxFaceVertex(wr, box.field_72340_a, box.field_72338_b, box.field_72334_f, end);
        }
    }

    public static void drawBoxFace(
        AxisAlignedBB box, EnumFacing face, int overlayColor, int outlineColor, boolean overlay, boolean outline
    ) {
        Tessellator ts = Tessellator.func_178181_a();
        WorldRenderer wr = ts.func_178180_c();
        if (overlay) {
            wr.func_181668_a(7, DefaultVertexFormats.field_181706_f);
            drawBoxFaceVertices(wr, face, box, overlayColor, overlayColor);
            ts.func_78381_a();
        }

        if (outline) {
            wr.func_181668_a(2, DefaultVertexFormats.field_181706_f);
            drawBoxFaceVertices(wr, face, box, outlineColor, outlineColor);
            ts.func_78381_a();
        }
    }

    public static void renderBlockShape(
        BlockPos pos, IBlockState state, int color, boolean outline, boolean shade, Set<EnumFacing> visibleFaces
    ) {
        AxisAlignedBB box = state.func_177230_c().func_180646_a(mc.field_71441_e, pos);
        if (box != null) {
            double vx = mc.func_175598_ae().field_78730_l;
            double vy = mc.func_175598_ae().field_78731_m;
            double vz = mc.func_175598_ae().field_78728_n;
            int overlayColor = color & 16777215 | 1056964608;
            int outlineColor = color | 0xFF000000;
            GL11.glPushMatrix();
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(3042);
            GL11.glLineWidth(2.0F);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            AxisAlignedBB renderBox = box.func_72317_d(-vx, -vy, -vz);

            for (EnumFacing face : visibleFaces) {
                drawBoxFace(renderBox, face, overlayColor, outlineColor, shade, outline);
            }

            GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glDisable(3042);
            GL11.glPopMatrix();
        }
    }

    public static void renderBPS(boolean b, boolean b2) {
    }

    public static void renderEntity(Entity e, int type, double expand, double shift, int color, boolean damage) {
        if (e instanceof EntityLivingBase) {
            float partialTicks = ((IAccessorMinecraft)mc).getTimer().field_74281_c;
            double x = e.field_70142_S
                + (e.field_70165_t - e.field_70142_S) * partialTicks
                - mc.func_175598_ae().field_78730_l;
            double y = e.field_70137_T
                + (e.field_70163_u - e.field_70137_T) * partialTicks
                - mc.func_175598_ae().field_78731_m;
            double z = e.field_70136_U
                + (e.field_70161_v - e.field_70136_U) * partialTicks
                - mc.func_175598_ae().field_78728_n;
            float d = (float)expand / 40.0F;
            if (e instanceof EntityPlayer && damage && ((EntityPlayer)e).field_70737_aN != 0) {
                color = Color.RED.getRGB();
            }

            GlStateManager.func_179094_E();
            if (type == 3) {
                GL11.glTranslated(x, y - 0.2, z);
                GL11.glRotated(-mc.func_175598_ae().field_78735_i, 0.0, 1.0, 0.0);
                GlStateManager.func_179097_i();
                GL11.glScalef(0.03F + d, 0.03F + d, 0.03F + d);
                int outline = Color.black.getRGB();
                Gui.func_73734_a(-20, -1, -26, 75, outline);
                Gui.func_73734_a(20, -1, 26, 75, outline);
                Gui.func_73734_a(-20, -1, 21, 5, outline);
                Gui.func_73734_a(-20, 70, 21, 75, outline);
                if (color != 0) {
                    Gui.func_73734_a(-21, 0, -25, 74, color);
                    Gui.func_73734_a(21, 0, 25, 74, color);
                    Gui.func_73734_a(-21, 0, 24, 4, color);
                    Gui.func_73734_a(-21, 71, 25, 74, color);
                } else {
                    int st = ColorUtil.getChroma(2L, 0L);
                    int en = ColorUtil.getChroma(2L, 1000L);
                    dGR(-21, 0, -25, 74, st, en);
                    dGR(21, 0, 25, 74, st, en);
                    Gui.func_73734_a(-21, 0, 21, 4, en);
                    Gui.func_73734_a(-21, 71, 21, 74, st);
                }

                GlStateManager.func_179126_j();
            } else if (type == 4) {
                EntityLivingBase en = (EntityLivingBase)e;
                double health = en.func_110143_aJ() / en.func_110138_aP();
                int barHeight = (int)(74.0 * health);
                int healthColor = health < 0.3
                    ? Color.red.getRGB()
                    : (
                        health < 0.5
                            ? Color.orange.getRGB()
                            : (health < 0.7 ? Color.yellow.getRGB() : Color.green.getRGB())
                    );
                GL11.glTranslated(x, y - 0.2, z);
                GL11.glRotated(-mc.func_175598_ae().field_78735_i, 0.0, 1.0, 0.0);
                GlStateManager.func_179097_i();
                GL11.glScalef(0.03F + d, 0.03F + d, 0.03F + d);
                int i = (int)(21.0 + shift * 2.0);
                Gui.func_73734_a(i, -1, i + 4, 75, Color.black.getRGB());
                Gui.func_73734_a(i + 1, barHeight, i + 3, 74, Color.darkGray.getRGB());
                Gui.func_73734_a(i + 1, 0, i + 3, barHeight, healthColor);
                GlStateManager.func_179126_j();
            } else if (type == 6) {
                drawCircle(x, y, z, 0.7F, 45, 1.5F, color, color == 0);
            } else {
                if (color == 0) {
                    color = ColorUtil.getChroma(2L, 0L);
                }

                float a = (color >> 24 & 0xFF) / 255.0F;
                float r = (color >> 16 & 0xFF) / 255.0F;
                float g = (color >> 8 & 0xFF) / 255.0F;
                float b = (color & 0xFF) / 255.0F;
                AxisAlignedBB bbox = e.func_174813_aQ().func_72314_b(0.1 + expand, 0.1 + expand, 0.1 + expand);
                AxisAlignedBB axis = new AxisAlignedBB(
                    bbox.field_72340_a - e.field_70165_t + x,
                    bbox.field_72338_b - e.field_70163_u + y,
                    bbox.field_72339_c - e.field_70161_v + z,
                    bbox.field_72336_d - e.field_70165_t + x,
                    bbox.field_72337_e - e.field_70163_u + y,
                    bbox.field_72334_f - e.field_70161_v + z
                );
                GL11.glBlendFunc(770, 771);
                GL11.glEnable(3042);
                GL11.glDisable(3553);
                GL11.glDisable(2929);
                GL11.glDepthMask(false);
                GL11.glLineWidth(2.0F);
                GL11.glColor4f(r, g, b, a);
                if (type == 1) {
                    RenderGlobal.func_181561_a(axis);
                } else if (type == 2) {
                    drawBoundingBox(axis, r, g, b);
                }

                GL11.glEnable(3553);
                GL11.glEnable(2929);
                GL11.glDepthMask(true);
                GL11.glDisable(3042);
            }

            GlStateManager.func_179121_F();
        }
    }

    public static void drawPolygon(double n, double n2, double n3, int n4, int n5) {
        if (n4 >= 3) {
            float n6 = (n5 >> 24 & 0xFF) / 255.0F;
            float n7 = (n5 >> 16 & 0xFF) / 255.0F;
            float n8 = (n5 >> 8 & 0xFF) / 255.0F;
            float n9 = (n5 & 0xFF) / 255.0F;
            Tessellator getInstance = Tessellator.func_178181_a();
            WorldRenderer getWorldRenderer = getInstance.func_178180_c();
            GlStateManager.func_179147_l();
            GlStateManager.func_179090_x();
            GlStateManager.func_179120_a(770, 771, 1, 0);
            GL11.glColor4f(n7, n8, n9, n6);
            getWorldRenderer.func_181668_a(6, DefaultVertexFormats.field_181705_e);

            for (int i = 0; i < n4; i++) {
                double n10 = (Math.PI * 2) * i / n4 + Math.toRadians(180.0);
                getWorldRenderer.func_181662_b(n + Math.sin(n10) * n3, n2 + Math.cos(n10) * n3, 0.0).func_181675_d();
            }

            getInstance.func_78381_a();
            GlStateManager.func_179098_w();
            GlStateManager.func_179084_k();
        }
    }

    public static void drawOutlinedBox(AxisAlignedBB worldBox, double viewerX, double viewerY, double viewerZ) {
        AxisAlignedBB renderBox = worldBox.func_72317_d(-viewerX, -viewerY, -viewerZ);
        RenderGlobal.func_181561_a(renderBox);
    }

    public static void drawBoundingBox(AxisAlignedBB abb, float r, float g, float b) {
        drawBoundingBox(abb, r, g, b, 0.25F);
    }

    public static void drawBoundingBox(AxisAlignedBB abb, float r, float g, float b, float a) {
        Tessellator ts = Tessellator.func_178181_a();
        WorldRenderer vb = ts.func_178180_c();
        vb.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        ts.func_78381_a();
        vb.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        ts.func_78381_a();
        vb.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        ts.func_78381_a();
        vb.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        ts.func_78381_a();
        vb.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        ts.func_78381_a();
        vb.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72337_e, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72340_a, abb.field_72338_b, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72339_c)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72337_e, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        vb.func_181662_b(abb.field_72336_d, abb.field_72338_b, abb.field_72334_f)
            .func_181666_a(r, g, b, a)
            .func_181675_d();
        ts.func_78381_a();
    }

    public static void renderBlockModel(IBlockState blockState, BlockPos blockPos, int color) {
        renderBlockModel(
            blockState, blockPos.func_177958_n(), blockPos.func_177956_o(), blockPos.func_177952_p(), color
        );
    }

    public static void renderBlockModel(IBlockState blockState, double x, double y, double z, int color) {
        Minecraft mc = Minecraft.func_71410_x();
        BlockRendererDispatcher dispatcher = mc.func_175602_ab();
        IBakedModel model = dispatcher.func_175022_a(blockState, mc.field_71441_e, new BlockPos(x, y, z));
        double xPos = x - mc.func_175598_ae().field_78730_l;
        double yPos = y - mc.func_175598_ae().field_78731_m;
        double zPos = z - mc.func_175598_ae().field_78728_n;
        float a = (color >> 24 & 0xFF) / 255.0F;
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        GlStateManager.func_179094_E();
        GlStateManager.func_179137_b(xPos, yPos, zPos);
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        GlStateManager.func_179090_x();
        GlStateManager.func_179129_p();
        GlStateManager.func_179097_i();
        GlStateManager.func_179132_a(false);
        GlStateManager.func_179131_c(r, g, b, a);
        renderModelColoredQuads(model, r, g, b, a);
        GlStateManager.func_179132_a(true);
        GlStateManager.func_179126_j();
        GlStateManager.func_179098_w();
        GlStateManager.func_179089_o();
        GlStateManager.func_179084_k();
        GlStateManager.func_179121_F();
    }

    private static void renderModelColoredQuads(IBakedModel model, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer wr = tessellator.func_178180_c();

        for (EnumFacing face : EnumFacing.values()) {
            for (BakedQuad quad : model.func_177551_a(face)) {
                drawColoredQuad(wr, quad, r, g, b, a, tessellator);
            }
        }

        for (BakedQuad quad : model.func_177550_a()) {
            drawColoredQuad(wr, quad, r, g, b, a, tessellator);
        }
    }

    private static void drawColoredQuad(
        WorldRenderer wr, BakedQuad quad, float r, float g, float b, float a, Tessellator tessellator
    ) {
        int[] vertexData = quad.func_178209_a();
        int vertexCount = 4;
        int intsPerVertex = vertexData.length / 4;
        wr.func_181668_a(7, DefaultVertexFormats.field_181706_f);

        for (int i = 0; i < 4; i++) {
            int baseIndex = i * intsPerVertex;
            float vx = Float.intBitsToFloat(vertexData[baseIndex]);
            float vy = Float.intBitsToFloat(vertexData[baseIndex + 1]);
            float vz = Float.intBitsToFloat(vertexData[baseIndex + 2]);
            wr.func_181662_b(vx, vy, vz).func_181666_a(r, g, b, a).func_181675_d();
        }

        tessellator.func_78381_a();
    }

    public static void drawTracerLine(Entity e, int color, float lineWidth, float partialTicks) {
        if (e != null && mc.func_175598_ae() != null) {
            Entity viewEntity = mc.func_175606_aa();
            if (viewEntity == null) {
                viewEntity = mc.field_71439_g;
            }

            if (viewEntity != null) {
                double targetX = e.field_70142_S
                    + (e.field_70165_t - e.field_70142_S) * partialTicks
                    - mc.func_175598_ae().field_78730_l;
                double targetY = e.field_70137_T
                    + (e.field_70163_u - e.field_70137_T) * partialTicks
                    - mc.func_175598_ae().field_78731_m
                    + e.func_70047_e()
                    + (e.func_70093_af() ? -0.125 : 0.0);
                double targetZ = e.field_70136_U
                    + (e.field_70161_v - e.field_70136_U) * partialTicks
                    - mc.func_175598_ae().field_78728_n;
                double startX = 0.0;
                double startY = viewEntity.func_70047_e();
                double startZ = 0.0;
                if (viewEntity == mc.field_71439_g && mc.field_71474_y.field_74320_O == 0) {
                    float yaw = viewEntity.field_70177_z;
                    float pitch = viewEntity.field_70125_A;
                    double dirX = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
                    double dirY = -Math.sin(Math.toRadians(pitch));
                    double dirZ = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
                    startX = dirX;
                    startY += dirY;
                    startZ = dirZ;
                }

                float a = (color >> 24 & 0xFF) / 255.0F;
                float r = (color >> 16 & 0xFF) / 255.0F;
                float g = (color >> 8 & 0xFF) / 255.0F;
                float b = (color & 0xFF) / 255.0F;
                GL11.glPushMatrix();
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glEnable(2848);
                GL11.glDisable(3553);
                GL11.glDisable(2929);
                GL11.glDepthMask(false);
                GL11.glLineWidth(lineWidth);
                GL11.glColor4f(r, g, b, a);
                GL11.glBegin(1);
                GL11.glVertex3d(startX, startY, startZ);
                GL11.glVertex3d(targetX, targetY, targetZ);
                GL11.glEnd();
                GL11.glLineWidth(1.0F);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glDepthMask(true);
                GL11.glEnable(2929);
                GL11.glEnable(3553);
                GL11.glDisable(2848);
                GL11.glDisable(3042);
                GL11.glPopMatrix();
            }
        }
    }

    public static void dGR(int left, int top, int right, int bottom, int startColor, int endColor) {
        if (left < right) {
            int j = left;
            left = right;
            right = j;
        }

        if (top < bottom) {
            int j = top;
            top = bottom;
            bottom = j;
        }

        float f = (startColor >> 24 & 0xFF) / 255.0F;
        float f1 = (startColor >> 16 & 0xFF) / 255.0F;
        float f2 = (startColor >> 8 & 0xFF) / 255.0F;
        float f3 = (startColor & 0xFF) / 255.0F;
        float f4 = (endColor >> 24 & 0xFF) / 255.0F;
        float f5 = (endColor >> 16 & 0xFF) / 255.0F;
        float f6 = (endColor >> 8 & 0xFF) / 255.0F;
        float f7 = (endColor & 0xFF) / 255.0F;
        GlStateManager.func_179090_x();
        GlStateManager.func_179147_l();
        GlStateManager.func_179118_c();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        GlStateManager.func_179103_j(7425);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldrenderer = tessellator.func_178180_c();
        worldrenderer.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        worldrenderer.func_181662_b(right, top, 0.0).func_181666_a(f1, f2, f3, f).func_181675_d();
        worldrenderer.func_181662_b(left, top, 0.0).func_181666_a(f1, f2, f3, f).func_181675_d();
        worldrenderer.func_181662_b(left, bottom, 0.0).func_181666_a(f5, f6, f7, f4).func_181675_d();
        worldrenderer.func_181662_b(right, bottom, 0.0).func_181666_a(f5, f6, f7, f4).func_181675_d();
        tessellator.func_78381_a();
        GlStateManager.func_179103_j(7424);
        GlStateManager.func_179084_k();
        GlStateManager.func_179141_d();
        GlStateManager.func_179098_w();
    }

    public static void db(int w, int h, int r) {
        int c = r == -1 ? -1089466352 : r;
        Gui.func_73734_a(0, 0, w, h, c);
    }

    public static void drawColoredString(
        String text, char lineSplit, int x, int y, long s, long shift, boolean rect, FontRenderer fontRenderer
    ) {
        int bX = x;
        int l = 0;
        long r = 0L;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == lineSplit) {
                l++;
                x = bX;
                y += fontRenderer.field_78288_b + 5;
                r = shift * l;
            } else {
                fontRenderer.func_175065_a(String.valueOf(c), x, y, ColorUtil.getChroma(s, r), rect);
                x += fontRenderer.func_78263_a(c);
                if (c != ' ') {
                    r -= 90L;
                }
            }
        }
    }

    public static void drawCircle(
        double x, double y, double z, double radius, int sides, float lineWidth, int color, boolean chroma
    ) {
        float a = (color >> 24 & 0xFF) / 255.0F;
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        mc.field_71460_t.func_175072_h();
        GL11.glDisable(3553);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(2929);
        GL11.glEnable(2848);
        GL11.glDepthMask(false);
        GL11.glLineWidth(lineWidth);
        if (!chroma) {
            GL11.glColor4f(r, g, b, a);
        }

        GL11.glBegin(1);
        long d = 0L;
        long ed = 15000L / sides;
        long hed = ed / 2L;

        for (int i = 0; i < sides * 2; i++) {
            if (chroma) {
                if (i % 2 != 0) {
                    if (i == 47) {
                        d = hed;
                    }

                    d += ed;
                }

                int c = ColorUtil.getChroma(2L, d);
                float r2 = (c >> 16 & 0xFF) / 255.0F;
                float g2 = (c >> 8 & 0xFF) / 255.0F;
                float b2 = (c & 0xFF) / 255.0F;
                GL11.glColor3f(r2, g2, b2);
            }

            double angle = (Math.PI * 2) * i / sides + Math.toRadians(180.0);
            GL11.glVertex3d(x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius);
        }

        GL11.glEnd();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDepthMask(true);
        GL11.glDisable(2848);
        GL11.glEnable(2929);
        GL11.glDisable(3042);
        GL11.glEnable(3553);
        mc.field_71460_t.func_180436_i();
    }

    public static void drawCaret(float x, float y, int color, double width, double length) {
        GL11.glPushMatrix();
        GL11.glEnable(2848);
        GL11.glDisable(3553);
        glColor(color);
        GL11.glLineWidth((float)width);
        float halfWidth = (float)(width / 2.0);
        float xOffset = halfWidth / 2.0F;
        float yOffset = halfWidth / 2.0F;
        GL11.glBegin(1);
        GL11.glVertex2d(x - xOffset, y + yOffset);
        GL11.glVertex2d(x + length - xOffset, y - length + yOffset);
        GL11.glVertex2d(x + length - xOffset, y - length + yOffset);
        GL11.glVertex2d(x + 2.0 * length - xOffset, y + yOffset);
        GL11.glEnd();
        GL11.glEnable(3553);
        GL11.glDisable(2848);
        GL11.glPopMatrix();
    }

    public static void drawTriangle(double x, double y, double size, double widthDiv, double heightDiv, int color) {
        boolean blend = GL11.glIsEnabled(3042);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(2848);
        GL11.glPushMatrix();
        glColor(color);
        GL11.glBegin(7);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x - size / widthDiv, y + size);
        GL11.glVertex2d(x, y + size / heightDiv);
        GL11.glVertex2d(x + size / widthDiv, y + size);
        GL11.glVertex2d(x, y);
        GL11.glEnd();
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.8F);
        GL11.glBegin(2);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x - size / widthDiv, y + size);
        GL11.glVertex2d(x, y + size / heightDiv);
        GL11.glVertex2d(x + size / widthDiv, y + size);
        GL11.glVertex2d(x, y);
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glEnable(3553);
        if (!blend) {
            GL11.glDisable(3042);
        }

        GL11.glDisable(2848);
    }

    public static void glColor(int n) {
        GL11.glColor4f(
            (n >> 16 & 0xFF) / 255.0F, (n >> 8 & 0xFF) / 255.0F, (n & 0xFF) / 255.0F, (n >> 24 & 0xFF) / 255.0F
        );
    }

    public static void glColor(int n, float alpha) {
        GL11.glColor4f((n >> 16 & 0xFF) / 255.0F, (n >> 8 & 0xFF) / 255.0F, (n & 0xFF) / 255.0F, alpha);
    }

    public static void drawRoundedGradientOutlinedRectangle(
        float x, float y, float x2, float y2, float radius, int n6, int n7, int n8
    ) {
        x *= 2.0F;
        y *= 2.0F;
        x2 *= 2.0F;
        y2 *= 2.0F;
        GL11.glPushMatrix();
        GL11.glPushAttrib(1048575);
        GL11.glScaled(0.5, 0.5, 0.5);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glBegin(9);
        glColor(n6);

        for (int i = 0; i <= 90; i += 3) {
            double n9 = i * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(n9) * radius * -1.0, y + radius + Math.cos(n9) * radius * -1.0);
        }

        for (int j = 90; j <= 180; j += 3) {
            double n10 = j * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(n10) * radius * -1.0, y2 - radius + Math.cos(n10) * radius * -1.0);
        }

        for (int k = 0; k <= 90; k += 3) {
            double n11 = k * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x2 - radius + Math.sin(n11) * radius, y2 - radius + Math.cos(n11) * radius);
        }

        for (int l = 90; l <= 180; l += 3) {
            double n12 = l * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x2 - radius + Math.sin(n12) * radius, y + radius + Math.cos(n12) * radius);
        }

        GL11.glEnd();
        GL11.glPushMatrix();
        GL11.glShadeModel(7425);
        GL11.glLineWidth(2.0F);
        GL11.glBegin(2);
        if (n7 != 0L) {
            glColor(n7);
        }

        for (int n13 = 0; n13 <= 90; n13 += 3) {
            double n14 = n13 * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(n14) * radius * -1.0, y + radius + Math.cos(n14) * radius * -1.0);
        }

        for (int n15 = 90; n15 <= 180; n15 += 3) {
            double n16 = n15 * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(n16) * radius * -1.0, y2 - radius + Math.cos(n16) * radius * -1.0);
        }

        if (n8 != 0) {
            glColor(n8);
        }

        for (int n17 = 0; n17 <= 90; n17 += 3) {
            double n18 = n17 * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x2 - radius + Math.sin(n18) * radius, y2 - radius + Math.cos(n18) * radius);
        }

        for (int n19 = 90; n19 <= 180; n19 += 3) {
            double n20 = n19 * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x2 - radius + Math.sin(n20) * radius, y + radius + Math.cos(n20) * radius);
        }

        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
        GL11.glEnable(3553);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
        GL11.glLineWidth(1.0F);
        GL11.glShadeModel(7424);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void draw2DPolygon(double x, double y, double radius, int sides, int color) {
        if (sides >= 3) {
            float a = (color >> 24 & 0xFF) / 255.0F;
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            Tessellator tessellator = Tessellator.func_178181_a();
            WorldRenderer worldrenderer = tessellator.func_178180_c();
            GlStateManager.func_179147_l();
            GlStateManager.func_179090_x();
            GlStateManager.func_179120_a(770, 771, 1, 0);
            GL11.glEnable(2848);
            GL11.glColor4f(r, g, b, a);
            double rad180 = Math.toRadians(180.0);
            worldrenderer.func_181668_a(6, DefaultVertexFormats.field_181705_e);

            for (int i = 0; i < sides; i++) {
                double angle = (Math.PI * 2) * i / sides + rad180;
                worldrenderer.func_181662_b(x + Math.sin(angle) * radius, y + Math.cos(angle) * radius, 0.0)
                    .func_181675_d();
            }

            tessellator.func_78381_a();
            GlStateManager.func_179098_w();
            GlStateManager.func_179084_k();
        }
    }

    public static Framebuffer createFrameBuffer(Framebuffer framebuffer) {
        return createFrameBuffer(framebuffer, false);
    }

    public static Framebuffer createFrameBuffer(Framebuffer framebuffer, boolean depth) {
        if (needsNewFramebuffer(framebuffer)) {
            if (framebuffer != null) {
                framebuffer.func_147608_a();
            }

            return new Framebuffer(mc.field_71443_c, mc.field_71440_d, depth);
        } else {
            return framebuffer;
        }
    }

    public static boolean needsNewFramebuffer(Framebuffer framebuffer) {
        return framebuffer == null
            || framebuffer.field_147621_c != mc.field_71443_c
            || framebuffer.field_147618_d != mc.field_71440_d;
    }

    public static void drawFramebufferFullscreen(Framebuffer framebuffer) {
        if (framebuffer != null) {
            ScaledResolution sr = new ScaledResolution(mc);
            GlStateManager.func_179144_i(framebuffer.field_147617_g);
            GL11.glBegin(7);
            GL11.glTexCoord2d(0.0, 1.0);
            GL11.glVertex2d(0.0, 0.0);
            GL11.glTexCoord2d(0.0, 0.0);
            GL11.glVertex2d(0.0, sr.func_78328_b());
            GL11.glTexCoord2d(1.0, 0.0);
            GL11.glVertex2d(sr.func_78326_a(), sr.func_78328_b());
            GL11.glTexCoord2d(1.0, 1.0);
            GL11.glVertex2d(sr.func_78326_a(), 0.0);
            GL11.glEnd();
        }
    }

    public static void bindTexture(int texture) {
        GL11.glBindTexture(3553, texture);
    }

    public static void setAlphaLimit(float limit) {
        GlStateManager.func_179141_d();
        GlStateManager.func_179092_a(516, (float)(limit * 0.01));
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1.0F, Math.max(0.0F, amount));
        return new Color(
            interpolateInt(color1.getRed(), color2.getRed(), amount),
            interpolateInt(color1.getGreen(), color2.getGreen(), amount),
            interpolateInt(color1.getBlue(), color2.getBlue(), amount),
            interpolateInt(color1.getAlpha(), color2.getAlpha(), amount)
        );
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return interpolate(oldValue, newValue, (float)interpolationValue).intValue();
    }

    public static Double interpolate(double oldValue, double newValue, double interpolationValue) {
        return oldValue + (newValue - oldValue) * interpolationValue;
    }

    public static void resetColor() {
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void startBlend() {
        GLUtil.startBlend();
    }

    public static void endBlend() {
        GLUtil.endBlend();
    }

    public static Vec3 convertTo2D(int scaleFactor, double x, double y, double z) {
        GL11.glGetFloat(2982, MODELVIEW);
        GL11.glGetFloat(2983, PROJECTION);
        GL11.glGetInteger(2978, VIEWPORT);
        boolean result = GLU.gluProject((float)x, (float)y, (float)z, MODELVIEW, PROJECTION, VIEWPORT, SCREEN_COORDS);
        return result
            ? new Vec3(
                SCREEN_COORDS.get(0) / scaleFactor,
                (Display.getHeight() - SCREEN_COORDS.get(1)) / scaleFactor,
                SCREEN_COORDS.get(2)
            )
            : null;
    }

    public static RenderUtil.ProjectionContext captureProjectionContext(
        RenderUtil.ProjectionContext context, int scaleFactor
    ) {
        if (context == null) {
            context = new RenderUtil.ProjectionContext();
        }

        context.scaleFactor = scaleFactor;
        ((Buffer)context.modelView).clear();
        ((Buffer)context.projection).clear();
        ((Buffer)context.viewport).clear();
        GL11.glGetFloat(2982, context.modelView);
        GL11.glGetFloat(2983, context.projection);
        GL11.glGetInteger(2978, context.viewport);
        ((Buffer)context.modelView).rewind();
        ((Buffer)context.projection).rewind();
        ((Buffer)context.viewport).rewind();
        return context;
    }

    public static boolean projectTo2D(
        RenderUtil.ProjectionContext context, double x, double y, double z, double[] output
    ) {
        if (context != null && output != null && output.length >= 3) {
            ((Buffer)context.screenCoords).clear();
            boolean result = GLU.gluProject(
                (float)x,
                (float)y,
                (float)z,
                context.modelView,
                context.projection,
                context.viewport,
                context.screenCoords
            );
            if (!result) {
                return false;
            }

            output[0] = context.screenCoords.get(0) / context.scaleFactor;
            output[1] = (Display.getHeight() - context.screenCoords.get(1)) / context.scaleFactor;
            output[2] = context.screenCoords.get(2);
            return true;
        } else {
            return false;
        }
    }

    public static void drawRoundedRectangle(float x, float y, float x2, float y2, float radius, int color) {
        if (!(x2 <= x)) {
            float width = x2 - x;
            if (width < 3.0F) {
                radius = Math.min(radius, width / 2.0F);
            }

            x = (float)(x * 2.0);
            y = (float)(y * 2.0);
            x2 = (float)(x2 * 2.0);
            y2 = (float)(y2 * 2.0);
            GL11.glPushMatrix();
            GL11.glPushAttrib(1048575);
            GL11.glScaled(0.5, 0.5, 0.5);
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glEnable(2848);
            GL11.glBlendFunc(770, 771);
            GL11.glBegin(9);
            glColor(color);

            for (int i = 0; i <= 90; i += 3) {
                double n7 = i * (float) (Math.PI / 180.0);
                GL11.glVertex2d(x + radius + Math.sin(n7) * radius * -1.0, y + radius + Math.cos(n7) * radius * -1.0);
            }

            for (int j = 90; j <= 180; j += 3) {
                double n8 = j * (float) (Math.PI / 180.0);
                GL11.glVertex2d(x + radius + Math.sin(n8) * radius * -1.0, y2 - radius + Math.cos(n8) * radius * -1.0);
            }

            if (x2 - x >= 4.5) {
                for (int k = 0; k <= 90; k++) {
                    double n9 = k * (float) (Math.PI / 180.0);
                    GL11.glVertex2d(x2 - radius + Math.sin(n9) * radius, y2 - radius + Math.cos(n9) * radius);
                }

                for (int l = 90; l <= 180; l++) {
                    double n10 = l * (float) (Math.PI / 180.0);
                    GL11.glVertex2d(x2 - radius + Math.sin(n10) * radius, y + radius + Math.cos(n10) * radius);
                }
            }

            GL11.glEnd();
            GL11.glEnable(3553);
            GL11.glDisable(3042);
            GL11.glEnable(3553);
            GL11.glDisable(2848);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static void drawRoundedRectangleOutline(
        float x, float y, float x2, float y2, float radius, float lineWidth, int color
    ) {
        if (!(x2 <= x)) {
            float width = x2 - x;
            if (width < 3.0F) {
                radius = Math.min(radius, width / 2.0F);
            }

            x = (float)(x * 2.0);
            y = (float)(y * 2.0);
            x2 = (float)(x2 * 2.0);
            y2 = (float)(y2 * 2.0);
            GL11.glPushMatrix();
            GL11.glPushAttrib(1048575);
            GL11.glScaled(0.5, 0.5, 0.5);
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glEnable(2848);
            GL11.glLineWidth(lineWidth * 2.0F);
            GL11.glBegin(2);
            glColor(color);

            for (int i = 0; i <= 90; i += 3) {
                double n7 = i * (float) (Math.PI / 180.0);
                GL11.glVertex2d(x + radius + Math.sin(n7) * radius * -1.0, y + radius + Math.cos(n7) * radius * -1.0);
            }

            for (int j = 90; j <= 180; j += 3) {
                double n8 = j * (float) (Math.PI / 180.0);
                GL11.glVertex2d(x + radius + Math.sin(n8) * radius * -1.0, y2 - radius + Math.cos(n8) * radius * -1.0);
            }

            if (x2 - x >= 4.5) {
                for (int k = 0; k <= 90; k++) {
                    double n9 = k * (float) (Math.PI / 180.0);
                    GL11.glVertex2d(x2 - radius + Math.sin(n9) * radius, y2 - radius + Math.cos(n9) * radius);
                }

                for (int l = 90; l <= 180; l++) {
                    double n10 = l * (float) (Math.PI / 180.0);
                    GL11.glVertex2d(x2 - radius + Math.sin(n10) * radius, y + radius + Math.cos(n10) * radius);
                }
            }

            GL11.glEnd();
            GL11.glEnable(3553);
            GL11.glDisable(3042);
            GL11.glDisable(2848);
            GL11.glEnable(3553);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static void drawRectangleGL(float x, float y, float x2, float y2, int color) {
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3553);
        glColor(color);
        GL11.glBegin(7);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y);
        GL11.glEnd();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    public static void drawRoundedGradientRect(
        float x, float y, float x2, float y2, float radius, int n6, int n7, int n8, int n9
    ) {
        if (!(x2 <= x)) {
            float width = x2 - x;
            if (width < 3.0F) {
                radius = Math.min(radius, width / 2.0F);
            }

            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(2848);
            GL11.glShadeModel(7425);
            GL11.glPushMatrix();
            GL11.glPushAttrib(1048575);
            GL11.glScaled(0.5, 0.5, 0.5);
            x = (float)(x * 2.0);
            y = (float)(y * 2.0);
            x2 = (float)(x2 * 2.0);
            y2 = (float)(y2 * 2.0);
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            glColor(n6);
            GL11.glEnable(2848);
            GL11.glShadeModel(7425);
            GL11.glBegin(9);

            for (int i = 0; i <= 90; i += 3) {
                double n10 = i * (float) (Math.PI / 180.0);
                GL11.glVertex2d(x + radius + Math.sin(n10) * radius * -1.0, y + radius + Math.cos(n10) * radius * -1.0);
            }

            glColor(n7);

            for (int j = 90; j <= 180; j += 3) {
                double n11 = j * (float) (Math.PI / 180.0);
                GL11.glVertex2d(x + radius + Math.sin(n11) * radius * -1.0, y2 - radius + Math.cos(n11) * radius * -1.0);
            }

            if (x2 - x >= 4.5) {
                glColor(n8);

                for (int k = 0; k <= 90; k += 3) {
                    double n12 = k * (float) (Math.PI / 180.0);
                    GL11.glVertex2d(x2 - radius + Math.sin(n12) * radius, y2 - radius + Math.cos(n12) * radius);
                }

                glColor(n9);

                for (int l = 90; l <= 180; l += 3) {
                    double n13 = l * (float) (Math.PI / 180.0);
                    GL11.glVertex2d(x2 - radius + Math.sin(n13) * radius, y + radius + Math.cos(n13) * radius);
                }
            }

            GL11.glEnd();
            GL11.glEnable(3553);
            GL11.glDisable(3042);
            GL11.glDisable(2848);
            GL11.glDisable(3042);
            GL11.glEnable(3553);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            GL11.glEnable(3553);
            GL11.glDisable(3042);
            GL11.glDisable(2848);
            GL11.glShadeModel(7424);
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static int setAlpha(int rgb, double alpha) {
        if (alpha < 0.0 || alpha > 1.0) {
            alpha = 0.5;
        }

        int red = rgb >> 16 & 0xFF;
        int green = rgb >> 8 & 0xFF;
        int blue = rgb & 0xFF;
        int alphaInt = (int)(alpha * 255.0);
        return alphaInt << 24 | red << 16 | green << 8 | blue;
    }

    public static void draw2DCircle(
        float centerX, float centerY, float radius, int segments, float lineWidth, float r, float g, float b, float a
    ) {
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glEnable(2884);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glColor4f(r, g, b, a);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(2);

        for (int i = 0; i <= segments; i++) {
            double theta = (Math.PI * 2) * i / segments;
            float x = (float)(radius * Math.cos(theta)) + centerX;
            float y = (float)(radius * Math.sin(theta)) + centerY;
            GL11.glVertex2f(x, y);
        }

        GL11.glEnd();
        GL11.glDisable(3042);
        GL11.glDisable(2884);
        GL11.glEnable(3553);
        GL11.glDisable(2848);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glLineWidth(1.0F);
        GL11.glPopMatrix();
    }

    public static void draw2DCircleArc(
        float centerX, float centerY, float radius, float startAngle, float endAngle, float lineWidth, int color
    ) {
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = (color >> 24 & 0xFF) / 255.0F;
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glEnable(2884);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glColor4f(r, g, b, a);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(3);

        for (float angle = startAngle; angle <= endAngle; angle++) {
            double theta = Math.toRadians(angle + 180.0F);
            float x = (float)(radius * Math.cos(theta)) + centerX;
            float y = (float)(radius * Math.sin(theta)) + centerY;
            GL11.glVertex2f(x, y);
        }

        GL11.glEnd();
        GL11.glDisable(3042);
        GL11.glDisable(2884);
        GL11.glEnable(3553);
        GL11.glDisable(2848);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glLineWidth(1.0F);
        GL11.glPopMatrix();
    }

    public static void drawHorizontalGradientRect(
        float left, float top, float right, float bottom, int leftColor, int rightColor
    ) {
        float la = (leftColor >> 24 & 0xFF) / 255.0F;
        float lr = (leftColor >> 16 & 0xFF) / 255.0F;
        float lg = (leftColor >> 8 & 0xFF) / 255.0F;
        float lb = (leftColor & 0xFF) / 255.0F;
        float ra = (rightColor >> 24 & 0xFF) / 255.0F;
        float rr = (rightColor >> 16 & 0xFF) / 255.0F;
        float rg = (rightColor >> 8 & 0xFF) / 255.0F;
        float rb = (rightColor & 0xFF) / 255.0F;
        GlStateManager.func_179090_x();
        GlStateManager.func_179147_l();
        GlStateManager.func_179118_c();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        GlStateManager.func_179103_j(7425);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer wr = tessellator.func_178180_c();
        wr.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        wr.func_181662_b(left, bottom, 0.0).func_181666_a(lr, lg, lb, la).func_181675_d();
        wr.func_181662_b(right, bottom, 0.0).func_181666_a(rr, rg, rb, ra).func_181675_d();
        wr.func_181662_b(right, top, 0.0).func_181666_a(rr, rg, rb, ra).func_181675_d();
        wr.func_181662_b(left, top, 0.0).func_181666_a(lr, lg, lb, la).func_181675_d();
        tessellator.func_78381_a();
        GlStateManager.func_179103_j(7424);
        GlStateManager.func_179084_k();
        GlStateManager.func_179141_d();
        GlStateManager.func_179098_w();
    }

    public static void drawVerticalGradientRect(
        float left, float top, float right, float bottom, int topColor, int bottomColor
    ) {
        float ta = (topColor >> 24 & 0xFF) / 255.0F;
        float tr = (topColor >> 16 & 0xFF) / 255.0F;
        float tg = (topColor >> 8 & 0xFF) / 255.0F;
        float tb = (topColor & 0xFF) / 255.0F;
        float ba = (bottomColor >> 24 & 0xFF) / 255.0F;
        float br = (bottomColor >> 16 & 0xFF) / 255.0F;
        float bg = (bottomColor >> 8 & 0xFF) / 255.0F;
        float bb = (bottomColor & 0xFF) / 255.0F;
        GlStateManager.func_179090_x();
        GlStateManager.func_179147_l();
        GlStateManager.func_179118_c();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        GlStateManager.func_179103_j(7425);
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer wr = tessellator.func_178180_c();
        wr.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        wr.func_181662_b(right, top, 0.0).func_181666_a(tr, tg, tb, ta).func_181675_d();
        wr.func_181662_b(left, top, 0.0).func_181666_a(tr, tg, tb, ta).func_181675_d();
        wr.func_181662_b(left, bottom, 0.0).func_181666_a(br, bg, bb, ba).func_181675_d();
        wr.func_181662_b(right, bottom, 0.0).func_181666_a(br, bg, bb, ba).func_181675_d();
        tessellator.func_78381_a();
        GlStateManager.func_179103_j(7424);
        GlStateManager.func_179084_k();
        GlStateManager.func_179141_d();
        GlStateManager.func_179098_w();
    }

    public static void renderItemAndEffectIntoGui3D(ItemStack stack, int xPos, int yPos) {
        if (stack != null) {
            GlStateManager.func_179094_E();
            prepareGuiItemRenderState();
            GlStateManager.func_179132_a(true);
            GlStateManager.func_179086_m(256);
            RenderHelper.func_74519_b();
            GlStateManager.func_179094_E();
            GlStateManager.func_179152_a(1.0F, 1.0F, -0.01F);
            mc.func_175599_af().field_77023_b = -150.0F;
            mc.func_175599_af().func_180450_b(stack, xPos, yPos);
            mc.func_175599_af().field_77023_b = 0.0F;
            GlStateManager.func_179121_F();
            RenderHelper.func_74518_a();
            prepareGuiTextureRenderState();
            GlStateManager.func_179084_k();
            GlStateManager.func_179121_F();
        }
    }

    public static void renderItemAndEffectIntoGui2D(ItemStack stack, int xPos, int yPos) {
        if (stack != null) {
            prepareGuiItemRenderState();
            mc.func_175599_af().field_77023_b = -150.0F;
            GlStateManager.func_179126_j();
            RenderHelper.func_74520_c();
            mc.func_175599_af().func_180450_b(stack, xPos, yPos - 8);
            mc.func_175599_af().field_77023_b = 0.0F;
            GlStateManager.func_179097_i();
            prepareGuiTextureRenderState();
            GlStateManager.func_179084_k();
        }
    }

    public static int getDurabilityColor(float ratio) {
        if (ratio > 0.6F) {
            return 65280;
        } else {
            return ratio > 0.3F ? 16776960 : 16711680;
        }
    }

    public static void drawDurabilityBar(int xPos, int yPos, float durabilityRatio) {
        int barWidth = (int)(durabilityRatio * 13.0F);
        int barColor = getDurabilityColor(durabilityRatio);
        GlStateManager.func_179090_x();
        Tessellator tess = Tessellator.func_178181_a();
        WorldRenderer wr = tess.func_178180_c();
        wr.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        wr.func_181662_b(xPos + 2, yPos + 15, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, 1.0F).func_181675_d();
        wr.func_181662_b(xPos + 2, yPos + 16, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, 1.0F).func_181675_d();
        wr.func_181662_b(xPos + 15, yPos + 16, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, 1.0F).func_181675_d();
        wr.func_181662_b(xPos + 15, yPos + 15, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, 1.0F).func_181675_d();
        tess.func_78381_a();
        float r = (barColor >> 16 & 0xFF) / 255.0F;
        float g = (barColor >> 8 & 0xFF) / 255.0F;
        float b = (barColor & 0xFF) / 255.0F;
        wr.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        wr.func_181662_b(xPos + 2, yPos + 15, 0.0).func_181666_a(r, g, b, 1.0F).func_181675_d();
        wr.func_181662_b(xPos + 2, yPos + 16, 0.0).func_181666_a(r, g, b, 1.0F).func_181675_d();
        wr.func_181662_b(xPos + 2 + barWidth, yPos + 16, 0.0).func_181666_a(r, g, b, 1.0F).func_181675_d();
        wr.func_181662_b(xPos + 2 + barWidth, yPos + 15, 0.0).func_181666_a(r, g, b, 1.0F).func_181675_d();
        tess.func_78381_a();
        GlStateManager.func_179098_w();
    }

    public static int getEnchantColor(int level) {
        switch (level) {
            case 1:
                return 16777215;
            case 2:
                return 5636095;
            case 3:
                return 43690;
            case 4:
                return 11141290;
            case 5:
                return 16755200;
            case 6:
            case 7:
            case 8:
            case 9:
            default:
                return level > 5 ? 16733695 : 16777215;
            case 10:
                return 16733695;
        }
    }

    public static int drawEnchantWithColor(FontRenderer fr, String letter, int level, int x, int y) {
        int letterWidth = fr.func_175063_a(letter, x, y, 16777215);
        fr.func_175063_a(String.valueOf(level), letterWidth, y, getEnchantColor(level));
        return letterWidth;
    }

    public static void prepareGuiTextureRenderState() {
        GlStateManager.func_179140_f();
        GlStateManager.func_179097_i();
        GlStateManager.func_179132_a(false);
        GlStateManager.func_179098_w();
        GlStateManager.func_179141_d();
        GlStateManager.func_179092_a(516, 0.1F);
        GlStateManager.func_179147_l();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void prepareGuiItemRenderState() {
        GlStateManager.func_179140_f();
        GlStateManager.func_179098_w();
        GlStateManager.func_179141_d();
        GlStateManager.func_179092_a(516, 0.1F);
        GlStateManager.func_179147_l();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        GlStateManager.func_179126_j();
        GlStateManager.func_179132_a(true);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static String getEnchantmentAbbreviated(int id) {
        switch (id) {
            case 0:
                return "pt";
            case 1:
                return "frp";
            case 2:
                return "ff";
            case 3:
                return "blp";
            case 4:
                return "prp";
            case 5:
                return "thr";
            case 6:
                return "res";
            case 7:
                return "aa";
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            default:
                return null;
            case 16:
                return "sh";
            case 17:
                return "smt";
            case 18:
                return "ban";
            case 19:
                return "kb";
            case 20:
                return "fa";
            case 21:
                return "lot";
            case 32:
                return "eff";
            case 33:
                return "sil";
            case 34:
                return "ub";
            case 35:
                return "for";
            case 48:
                return "pow";
            case 49:
                return "pun";
            case 50:
                return "flm";
            case 51:
                return "inf";
        }
    }

    public static ResourceLocation buildWhiteMaskedTexture(
        String resourcePath, String registryName, ResourceLocation fallback
    ) {
        InputStream inputStream = null;

        try {
            if (resourcePath.startsWith("/assets/")) {
                String pathWithoutAssets = resourcePath.substring("/assets/".length());
                int slashIndex = pathWithoutAssets.indexOf(47);
                if (slashIndex != -1) {
                    String domain = pathWithoutAssets.substring(0, slashIndex);
                    String path = pathWithoutAssets.substring(slashIndex + 1);
                    inputStream = mc.func_110442_L().func_110536_a(new ResourceLocation(domain, path)).func_110527_b();
                }
            }
        } catch (Exception var13) {
        }

        if (inputStream == null) {
            inputStream = Miau.class.getResourceAsStream(resourcePath);
        }

        if (inputStream == null) {
            return fallback;
        }

        try {
            InputStream stream = inputStream;

            ResourceLocation var20;
            try {
                BufferedImage src = ImageIO.read(stream);
                int w = src.getWidth();
                int h = src.getHeight();
                BufferedImage dst = new BufferedImage(w, h, 2);

                for (int py = 0; py < h; py++) {
                    for (int px = 0; px < w; px++) {
                        int alpha = src.getRGB(px, py) >>> 24 & 0xFF;
                        if (alpha > 0) {
                            dst.setRGB(px, py, alpha << 24 | 16777215);
                        }
                    }
                }

                var20 = mc.func_110434_K().func_110578_a(registryName, new DynamicTexture(dst));
            } catch (Throwable var14) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Throwable var12) {
                        var14.addSuppressed(var12);
                    }
                }

                throw var14;
            }

            if (stream != null) {
                stream.close();
            }

            return var20;
        } catch (Exception e) {
            e.printStackTrace();
            return fallback;
        }
    }

    public static ResourceLocation getIcon(String resourcePath) {
        ResourceLocation cached = iconCache.get(resourcePath);
        if (cached != null) {
            return cached;
        }

        String registryName = "raven_icon_" + resourcePath.hashCode();
        ResourceLocation icon = buildWhiteMaskedTexture(resourcePath, registryName, null);
        if (icon != null) {
            iconCache.put(resourcePath, icon);
        }

        return icon;
    }

    public static void scaleStart(float x, float y, float scale) {
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b(x, y, 0.0F);
        GlStateManager.func_179152_a(scale, scale, 1.0F);
        GlStateManager.func_179109_b(-x, -y, 0.0F);
    }

    public static void scaleEnd() {
        GlStateManager.func_179121_F();
    }

    public static void scissor(float x, float y, float width, float height, float scale) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scaleFactor = sr.func_78325_e();
        float scaledX = (x - sr.func_78326_a() / 2.0F) * scale + sr.func_78326_a() / 2.0F;
        float scaledY = (y - sr.func_78328_b() / 2.0F) * scale + sr.func_78328_b() / 2.0F;
        float scaledW = width * scale;
        float scaledH = height * scale;
        GL11.glScissor(
            (int)(scaledX * scaleFactor),
            (int)((sr.func_78328_b() - (scaledY + scaledH)) * scaleFactor),
            (int)(scaledW * scaleFactor),
            (int)(scaledH * scaleFactor)
        );
    }

    public static void drawIcon(ResourceLocation texture, float x, float y, int size, int argbColor) {
        if (texture != null) {
            boolean depthEnabled = GL11.glIsEnabled(2929);
            boolean blendEnabled = GL11.glIsEnabled(3042);
            boolean depthMask = GL11.glGetBoolean(2930);
            prepareGuiTextureRenderState();
            mc.func_110434_K().func_110577_a(texture);
            float a = (argbColor >>> 24 & 0xFF) / 255.0F;
            float r = (argbColor >> 16 & 0xFF) / 255.0F;
            float g = (argbColor >> 8 & 0xFF) / 255.0F;
            float b = (argbColor & 0xFF) / 255.0F;
            GlStateManager.func_179131_c(r, g, b, a);
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 0.0F);
            Gui.func_146110_a(0, 0, 0.0F, 0.0F, size, size, size, size);
            GL11.glPopMatrix();
            restoreGuiRenderState(depthEnabled, blendEnabled, depthMask);
        }
    }

    public static void restoreGuiRenderState(boolean depthEnabled, boolean blendEnabled, boolean depthMask) {
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        if (blendEnabled) {
            GlStateManager.func_179147_l();
        } else {
            GlStateManager.func_179084_k();
        }

        if (depthEnabled) {
            GlStateManager.func_179126_j();
        } else {
            GlStateManager.func_179097_i();
        }

        GlStateManager.func_179132_a(depthMask);
    }

    public static final class EnchantmentData {
        public final String shortName;
        public final int maxLevel;

        public EnchantmentData(String shortName, int maxLevel) {
            this.shortName = shortName;
            this.maxLevel = maxLevel;
        }
    }

    static final class EnchantmentMap extends HashMap<Integer, RenderUtil.EnchantmentData> {
        EnchantmentMap() {
            this.put(0, new RenderUtil.EnchantmentData("Pr", 4));
            this.put(1, new RenderUtil.EnchantmentData("Fp", 4));
            this.put(2, new RenderUtil.EnchantmentData("Ff", 4));
            this.put(3, new RenderUtil.EnchantmentData("Bp", 4));
            this.put(4, new RenderUtil.EnchantmentData("Pp", 4));
            this.put(5, new RenderUtil.EnchantmentData("Re", 3));
            this.put(6, new RenderUtil.EnchantmentData("Aq", 1));
            this.put(7, new RenderUtil.EnchantmentData("Th", 3));
            this.put(8, new RenderUtil.EnchantmentData("Ds", 3));
            this.put(16, new RenderUtil.EnchantmentData("Sh", 5));
            this.put(17, new RenderUtil.EnchantmentData("Sm", 5));
            this.put(18, new RenderUtil.EnchantmentData("BoA", 5));
            this.put(19, new RenderUtil.EnchantmentData("Kb", 2));
            this.put(20, new RenderUtil.EnchantmentData("Fa", 2));
            this.put(21, new RenderUtil.EnchantmentData("Lo", 3));
            this.put(32, new RenderUtil.EnchantmentData("Ef", 5));
            this.put(33, new RenderUtil.EnchantmentData("St", 1));
            this.put(34, new RenderUtil.EnchantmentData("Ub", 3));
            this.put(35, new RenderUtil.EnchantmentData("Fo", 3));
            this.put(48, new RenderUtil.EnchantmentData("Po", 5));
            this.put(49, new RenderUtil.EnchantmentData("Pu", 2));
            this.put(50, new RenderUtil.EnchantmentData("Fl", 1));
            this.put(51, new RenderUtil.EnchantmentData("Inf", 1));
            this.put(61, new RenderUtil.EnchantmentData("LoS", 3));
            this.put(62, new RenderUtil.EnchantmentData("Lu", 3));
        }
    }

    public static final class ProjectionContext {
        private int scaleFactor;
        private final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
        private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
        private final FloatBuffer screenCoords = BufferUtils.createFloatBuffer(3);
    }
}
