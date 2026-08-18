package miau.module.modules.render;

import java.awt.Color;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.Render2DEvent;
import miau.mixin.IAccessorMinecraft;
import miau.module.Module;
import miau.module.modules.combat.KillAura;
import miau.property.properties.ColorProperty;
import miau.property.properties.DragProperty;
import miau.property.properties.FloatProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.TextProperty;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.shader.RoundedUtils;
import miau.util.vector.Vector2d;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class WaterMark extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");
    private static Field curBlockDamageMPField = null;
    private static final ResourceLocation LOGO_IMAGE = new ResourceLocation("miau/logo.png");
    private static final Map<String, String> REQUIRED_TOOLS = new HashMap<>();
    public final DragProperty dragging = new DragProperty("WaterMark", new Vector2d(-1.0, 10.0));
    public final TextProperty clientName = new TextProperty("ClientName", "Miau Minus");
    public final FloatProperty animationSpeed = new FloatProperty("AnimationSpeed", 0.35F, 0.05F, 1.0F);
    public final FloatProperty scale = new FloatProperty("Scale", 100.0F, 15.0F, 100.0F);
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"HUD", "CUSTOM"});
    public final ColorProperty customColor = new ColorProperty("custom-color", new Color(59, 155, 240).getRGB());
    private WaterMark.State islandState = WaterMark.State.Normal;
    private float stateTransition = 0.0F;
    private float animatedProgress = 0.0F;
    private float chatAnimOffset = 0.0F;
    private float springWidth = 120.0F;
    private float[] springWidthVel = new float[]{0.0F};
    private float springHeight = 20.0F;
    private float[] springHeightVel = new float[]{0.0F};
    private float moveBps = 0.0F;
    private String currentActionText = "";
    private float currentActionProgress = 0.0F;
    private EntityLivingBase currentAttackTarget = null;
    private ItemStack miningBlockStack = null;
    private long lastRenderNanoTime = 0L;
    private boolean isScaffolding = false;
    private ItemStack scaffoldBlockStack = null;
    private int scaffoldBlocksRemaining = 0;

    public WaterMark() {
        super("WaterMark", false, true);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled() && mc.field_71439_g != null && mc.field_71441_e != null) {
            ScaledResolution sr = new ScaledResolution(mc);
            long now = System.nanoTime();
            float deltaTime = this.lastRenderNanoTime == 0L
                ? 0.016666668F
                : (float)(now - this.lastRenderNanoTime) / 1.0E9F;
            deltaTime = Math.max(0.0F, Math.min(deltaTime, 0.1F));
            this.lastRenderNanoTime = now;
            double deltaX = mc.field_71439_g.field_70165_t - mc.field_71439_g.field_70169_q;
            double deltaZ = mc.field_71439_g.field_70161_v - mc.field_71439_g.field_70166_s;
            float timerSpeed = 1.0F;

            try {
                timerSpeed = ((IAccessorMinecraft)mc).getTimer().field_74278_d;
            } catch (Exception var17) {
            }

            float targetMoveBps = (float)(Math.hypot(deltaX, deltaZ) * 20.0 * timerSpeed);
            this.moveBps = this.smoothTowards(this.moveBps, targetMoveBps, 0.25F, deltaTime);
            this.islandState = this.checkPlayerAction() ? WaterMark.State.Action : WaterMark.State.Normal;
            float targetState = this.islandState == WaterMark.State.Action ? 1.0F : 0.0F;
            this.stateTransition = this.smoothTowards(
                this.stateTransition, targetState, this.animationSpeed.getValue() * 1.5F, deltaTime
            );
            this.animatedProgress = this.smoothTowards(
                this.animatedProgress, this.currentActionProgress, this.animationSpeed.getValue() * 1.2F, deltaTime
            );
            boolean isChatOpen = mc.field_71462_r instanceof GuiChat;
            float targetChatOffset = isChatOpen ? -14.0F : 0.0F;
            this.chatAnimOffset = this.smoothTowards(
                this.chatAnimOffset, targetChatOffset, this.animationSpeed.getValue(), deltaTime
            );
            float renderY = 10.0F + this.chatAnimOffset;
            float scaleFactor = this.scale.getValue() / 100.0F;
            GlStateManager.func_179094_E();
            GlStateManager.func_179152_a(scaleFactor, scaleFactor, 1.0F);
            this.drawDynamicIsland(sr, renderY / scaleFactor, deltaTime);
            GlStateManager.func_179121_F();
        }
    }

    private float updateSpring(float current, float target, float[] vel, float stiffness, float damping, float dt) {
        float MathDt = Math.min(dt, 0.05F);
        float force = (target - current) * stiffness;
        vel[0] = (vel[0] + force * MathDt) * (float)Math.pow(damping, MathDt * 60.0F);
        return current + vel[0] * MathDt;
    }

    private float smoothTowards(float current, float target, float speed, float deltaTime) {
        float clampedSpeed = Math.max(1.0E-4F, Math.min(1.0F, speed));
        float t = 1.0F - (float)Math.pow(1.0F - clampedSpeed, deltaTime * 60.0F);
        return current + (target - current) * t;
    }

    private float getCurBlockDamageMP() {
        if (mc.field_71442_b == null) {
            return 0.0F;
        }

        try {
            if (curBlockDamageMPField == null) {
                for (Class<?> clazz = mc.field_71442_b.getClass();
                    clazz != null && clazz != Object.class;
                    clazz = clazz.getSuperclass()
                ) {
                    String[] possibleNames = new String[]{"curBlockDamageMP", "field_78770_f", "e", "f", "g"};

                    for (String name : possibleNames) {
                        try {
                            Field f = clazz.getDeclaredField(name);
                            if (f.getType() == float.class) {
                                curBlockDamageMPField = f;
                                curBlockDamageMPField.setAccessible(true);
                                break;
                            }
                        } catch (NoSuchFieldException var8) {
                        }
                    }

                    if (curBlockDamageMPField != null) {
                        break;
                    }
                }
            }

            if (curBlockDamageMPField != null) {
                return curBlockDamageMPField.getFloat(mc.field_71442_b);
            }
        } catch (Exception e) {
            curBlockDamageMPField = null;
        }

        return 0.0F;
    }

    private Module getModuleByName(String name) {
        for (Module m : Miau.moduleManager.modules.values()) {
            if (m.getName().equalsIgnoreCase(name)) {
                return m;
            }
        }

        return null;
    }

    private String getRequiredTool(Block block) {
        try {
            String registryName = ((ResourceLocation)Block.field_149771_c.func_177774_c(block)).toString();
            return REQUIRED_TOOLS.get(registryName);
        } catch (Exception e) {
            return null;
        }
    }

    private int getTotalBlockCount(ItemStack targetStack) {
        if (targetStack != null && targetStack.func_77973_b() instanceof ItemBlock) {
            int total = 0;

            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
                if (stack != null
                    && stack.func_77973_b() == targetStack.func_77973_b()
                    && stack.func_77960_j() == targetStack.func_77960_j()) {
                    total += stack.field_77994_a;
                }
            }

            return total;
        } else {
            return 0;
        }
    }

    private boolean checkPlayerAction() {
        this.currentAttackTarget = null;
        this.miningBlockStack = null;
        KillAura killAura = (KillAura)Miau.moduleManager.getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.getTarget() != null) {
            this.currentAttackTarget = killAura.getTarget();
            this.currentActionText = "Attacking " + this.currentAttackTarget.func_70005_c_();
            this.currentActionProgress = this.currentAttackTarget.func_110143_aJ()
                / this.currentAttackTarget.func_110138_aP();
            this.isScaffolding = false;
            return true;
        }

        if (mc.field_71476_x != null
            && mc.field_71476_x.field_72313_a == MovingObjectType.ENTITY
            && mc.field_71476_x.field_72308_g instanceof EntityLivingBase
            && mc.field_71439_g.field_82175_bq) {
            this.currentAttackTarget = (EntityLivingBase)mc.field_71476_x.field_72308_g;
            this.currentActionText = "Attacking " + this.currentAttackTarget.func_70005_c_();
            this.currentActionProgress = this.currentAttackTarget.func_110143_aJ()
                / this.currentAttackTarget.func_110138_aP();
            this.isScaffolding = false;
            return true;
        }

        if (this.checkScaffoldAction()) {
            return true;
        }

        this.isScaffolding = false;
        if (mc.field_71476_x != null && mc.field_71476_x.field_72313_a == MovingObjectType.BLOCK) {
            BlockPos pos = mc.field_71476_x.func_178782_a();
            if (pos != null && mc.field_71441_e != null) {
                Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
                float damage = this.getCurBlockDamageMP();
                if (damage > 0.0F) {
                    int meta = block.func_176201_c(mc.field_71441_e.func_180495_p(pos));
                    this.miningBlockStack = new ItemStack(block, 1, meta);
                    if (this.miningBlockStack.func_77973_b() == null) {
                        this.miningBlockStack = new ItemStack(block);
                    }

                    String requiredTool = this.getRequiredTool(block);
                    String toolSuffix = requiredTool != null ? " (need " + requiredTool + ")" : "";
                    this.currentActionText = "Mining " + block.func_149732_F() + toolSuffix;
                    this.currentActionProgress = damage;
                    return true;
                }
            }
        }

        ItemStack held = mc.field_71439_g.func_70694_bm();
        if (held != null && held.func_77973_b() instanceof ItemSword) {
            boolean isAttacking = mc.field_71474_y.field_74312_F.func_151470_d() || mc.field_71439_g.field_82175_bq;
            if (isAttacking) {
                this.currentActionText = "Swing Sword";
                this.currentActionProgress = 1.0F;
                return true;
            }
        }

        if (mc.field_71439_g.func_71039_bw() && held != null && !(held.func_77973_b() instanceof ItemBlock)) {
            this.currentActionText = "Using " + held.func_82833_r();
            this.currentActionProgress = 1.0F;
            return true;
        } else {
            return false;
        }
    }

    private boolean checkScaffoldAction() {
        Module scaffold = this.getModuleByName("Scaffold");
        if (scaffold != null && scaffold.isEnabled()) {
            ItemStack held = mc.field_71439_g.func_70694_bm();
            if (held != null && held.func_77973_b() instanceof ItemBlock) {
                this.scaffoldBlockStack = held;
                this.scaffoldBlocksRemaining = this.getTotalBlockCount(held);
                this.isScaffolding = true;
                this.currentActionText = "Scaffolding....";
                this.currentActionProgress = 1.0F;
                return true;
            } else {
                this.isScaffolding = false;
                return false;
            }
        } else {
            this.isScaffolding = false;
            return false;
        }
    }

    private void drawDynamicIsland(ScaledResolution sr, float y, float deltaTime) {
        Font font18 = FontRepository.getFont("inter-regular", 18.0F);
        Font font18Bold = FontRepository.getFont("inter-bold", 18.0F);
        String username = mc.field_71439_g.func_70005_c_();
        String serverIp = this.getServerIp();
        String ping = this.getPing() + "ms";
        String bpsStr = DECIMAL_FORMAT.format(this.moveBps) + " BPS";
        float logoSize = 16.0F;
        float dividerWidth = font18.getStringWidth(" | ");
        float textLenName = font18.getStringWidth(username + " ");
        float textLenIP = font18.getStringWidth(serverIp + " ");
        float textLenPing = font18Bold.getStringWidth(ping);
        float textLenBps = font18.getStringWidth(bpsStr);
        float normalWidth = 6.0F
            + logoSize
            + dividerWidth
            + textLenName
            + textLenIP
            + textLenPing
            + dividerWidth
            + textLenBps
            + 6.0F;
        float targetHeadWidth = this.currentAttackTarget instanceof AbstractClientPlayer ? 20.0F : 0.0F;
        float scaffoldExtrasWidth = 0.0F;
        if (this.isScaffolding && this.scaffoldBlockStack != null) {
            String blockCountStr = String.valueOf(this.scaffoldBlocksRemaining);
            scaffoldExtrasWidth = 104.0F
                + font18Bold.getStringWidth("Block ")
                + font18.getStringWidth(blockCountStr)
                + 12.0F
                + font18Bold.getStringWidth("BPS ")
                + font18.getStringWidth(bpsStr);
        }

        float miningExtrasWidth = 0.0F;
        if (this.miningBlockStack != null) {
            String percentStr = (int)(this.currentActionProgress * 100.0F) + "%";
            miningExtrasWidth = 92.0F + font18Bold.getStringWidth(percentStr);
        }

        float actionWidth = 6.0F
            + logoSize
            + dividerWidth
            + targetHeadWidth
            + font18.getStringWidth(this.currentActionText)
            + scaffoldExtrasWidth
            + miningExtrasWidth
            + 6.0F;
        float targetWidth = normalWidth + (actionWidth - normalWidth) * this.stateTransition;
        float targetHeight = 20.0F + (this.islandState == WaterMark.State.Action ? 1.5F : 0.0F);
        this.springWidth = this.updateSpring(
            this.springWidth, targetWidth, this.springWidthVel, 240.0F, 0.72F, deltaTime
        );
        this.springHeight = this.updateSpring(
            this.springHeight, targetHeight, this.springHeightVel, 240.0F, 0.72F, deltaTime
        );
        float boxWidth = this.springWidth;
        float boxHeight = this.springHeight;
        float scaleFactor = this.scale.getValue() / 100.0F;
        float scaledWidth = sr.func_78326_a() / scaleFactor;
        float x = (scaledWidth - boxWidth) / 2.0F;
        this.dragging.position.x = x * scaleFactor;
        this.dragging.position.y = y * scaleFactor;
        this.dragging.scale.x = boxWidth * scaleFactor;
        this.dragging.scale.y = boxHeight * scaleFactor;
        GlStateManager.func_179094_E();
        float cornerRadius = boxHeight / 2.0F;
        float borderOffset = 1.0F;
        RoundedUtils.drawRound(
            x - borderOffset,
            y - borderOffset,
            boxWidth + borderOffset * 2.0F,
            boxHeight + borderOffset * 2.0F,
            cornerRadius + borderOffset,
            new Color(255, 255, 255, 200)
        );
        RoundedUtils.drawRound(x, y, boxWidth, boxHeight, cornerRadius, new Color(15, 15, 15, 225));
        GlStateManager.func_179121_F();
        float currentX = x + 6.0F;
        this.drawLogoImage(currentX, y + (boxHeight - logoSize) / 2.0F, logoSize);
        float normalTextAlpha = Math.max(0.0F, 1.0F - this.stateTransition * 2.2F);
        float actionTextAlpha = Math.max(0.0F, (this.stateTransition - 0.25F) * 1.33F);
        if (normalTextAlpha > 0.02F) {
            float textX = currentX + logoSize;
            font18.draw(" | ", textX, y + 6.0F, this.blendAlpha(new Color(100, 100, 100), normalTextAlpha), false);
            textX += dividerWidth;
            font18.draw(username + " ", textX, y + 6.0F, this.blendAlpha(Color.WHITE, normalTextAlpha), false);
            textX += textLenName;
            font18.draw(
                serverIp + " ", textX, y + 6.0F, this.blendAlpha(new Color(200, 200, 200), normalTextAlpha), false
            );
            textX += textLenIP;
            font18Bold.draw(ping, textX, y + 6.0F, this.blendAlpha(new Color(85, 255, 85), normalTextAlpha), false);
            textX += textLenPing;
            font18.draw(" | ", textX, y + 6.0F, this.blendAlpha(new Color(100, 100, 100), normalTextAlpha), false);
            textX += dividerWidth;
            font18.draw(bpsStr, textX, y + 6.0F, this.blendAlpha(new Color(255, 185, 80), normalTextAlpha), false);
        }

        if (actionTextAlpha > 0.02F) {
            float textX = currentX + logoSize;
            font18.draw(" | ", textX, y + 6.0F, this.blendAlpha(new Color(100, 100, 100), actionTextAlpha), false);
            textX += dividerWidth;
            if (this.currentAttackTarget instanceof AbstractClientPlayer) {
                this.drawPlayerHead(
                    (AbstractClientPlayer)this.currentAttackTarget,
                    textX,
                    y + (boxHeight - 14.0F) / 2.0F,
                    14.0F,
                    actionTextAlpha
                );
                textX += 20.0F;
            }

            font18.draw(this.currentActionText, textX, y + 6.0F, this.blendAlpha(Color.WHITE, actionTextAlpha), false);
            textX += font18.getStringWidth(this.currentActionText) + 6.0F;
            if (this.miningBlockStack != null) {
                this.drawMiningStats(textX, y, boxHeight, font18Bold, actionTextAlpha);
                String percentStr = (int)(this.currentActionProgress * 100.0F) + "%";
                textX += 86.0F + font18Bold.getStringWidth(percentStr) + 6.0F;
            }

            if (this.isScaffolding && this.scaffoldBlockStack != null) {
                this.drawScaffoldStats(textX, y, boxHeight, font18, font18Bold, actionTextAlpha, bpsStr);
            }
        }
    }

    private void drawPlayerHead(AbstractClientPlayer player, float x, float y, float size, float alpha) {
        try {
            ResourceLocation skin = player.func_110306_p();
            mc.func_110434_K().func_110577_a(skin);
            GlStateManager.func_179094_E();
            GlStateManager.func_179147_l();
            GlStateManager.func_179112_b(770, 771);
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, alpha);
            Gui.func_152125_a((int)x, (int)y, 8.0F, 8.0F, 8, 8, (int)size, (int)size, 64.0F, 64.0F);
            Gui.func_152125_a((int)x, (int)y, 40.0F, 8.0F, 8, 8, (int)size, (int)size, 64.0F, 64.0F);
            GlStateManager.func_179084_k();
            GlStateManager.func_179121_F();
        } catch (Exception var7) {
        }
    }

    private void drawMiningStats(float x, float y, float boxHeight, Font font18Bold, float alpha) {
        float iconSize = 14.0F;
        float barWidth = 60.0F;
        float barHeight = 8.0F;
        if (this.miningBlockStack != null) {
            GlStateManager.func_179094_E();
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, alpha);
            mc.func_175599_af().func_175042_a(this.miningBlockStack, (int)x, (int)(y + (boxHeight - iconSize) / 2.0F));
            GlStateManager.func_179121_F();
        }

        float barX = x + iconSize + 6.0F;
        float barY = y + (boxHeight - barHeight) / 2.0F;
        RoundedUtils.drawRound(barX, barY, barWidth, barHeight, 2.0F, this.blendColor(new Color(60, 60, 60), alpha));
        float fillPercent = Math.max(0.0F, Math.min(1.0F, this.currentActionProgress));
        if (fillPercent > 0.01F) {
            RoundedUtils.drawRound(
                barX, barY, barWidth * fillPercent, barHeight, 2.0F, this.blendColor(this.getHudAccentColor(), alpha)
            );
        }

        float textX = barX + barWidth + 6.0F;
        String percentStr = (int)(fillPercent * 100.0F) + "%";
        font18Bold.draw(percentStr, textX, y + 6.0F, this.blendAlpha(new Color(85, 255, 85), alpha), false);
    }

    private void drawScaffoldStats(
        float x, float y, float boxHeight, Font font18, Font font18Bold, float alpha, String currentBpsStr
    ) {
        float iconSize = 14.0F;
        float barWidth = 70.0F;
        float barHeight = 8.0F;
        GlStateManager.func_179094_E();
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, alpha);
        mc.func_175599_af().func_175042_a(this.scaffoldBlockStack, (int)x, (int)(y + (boxHeight - iconSize) / 2.0F));
        GlStateManager.func_179121_F();
        float barX = x + iconSize + 6.0F;
        float barY = y + (boxHeight - barHeight) / 2.0F;
        RoundedUtils.drawRound(barX, barY, barWidth, barHeight, 2.0F, this.blendColor(new Color(60, 60, 60), alpha));
        int maxStack = 64;
        float fillPercent = Math.max(0.0F, Math.min(1.0F, (float)this.scaffoldBlocksRemaining / maxStack));
        if (fillPercent > 0.01F) {
            RoundedUtils.drawRound(
                barX, barY, barWidth * fillPercent, barHeight, 2.0F, this.blendColor(this.getHudAccentColor(), alpha)
            );
        }

        float statsX = barX + barWidth + 8.0F;
        font18Bold.draw("Block ", statsX, y + 6.0F, this.blendAlpha(new Color(255, 140, 0), alpha), false);
        statsX += font18Bold.getStringWidth("Block ");
        String blockCountStr = String.valueOf(this.scaffoldBlocksRemaining);
        font18.draw(blockCountStr, statsX, y + 6.0F, this.blendAlpha(new Color(85, 255, 85), alpha), false);
        statsX += font18.getStringWidth(blockCountStr) + 12.0F;
        font18Bold.draw("BPS ", statsX, y + 6.0F, this.blendAlpha(new Color(255, 235, 60), alpha), false);
        statsX += font18Bold.getStringWidth("BPS ");
        font18.draw(currentBpsStr, statsX, y + 6.0F, this.blendAlpha(new Color(255, 105, 180), alpha), false);
    }

    private Color getHudAccentColor() {
        if (this.color.getValue() == 0) {
            try {
                HUD hud = (HUD)Miau.moduleManager.modules.get(HUD.class);
                if (hud != null) {
                    return hud.getColor(System.currentTimeMillis());
                }
            } catch (Exception var2) {
            }

            return new Color(59, 155, 240);
        } else {
            try {
                Object val = this.customColor.getValue();
                if (val instanceof Color) {
                    return (Color)val;
                }

                if (val instanceof Number) {
                    return new Color(((Number)val).intValue(), true);
                }
            } catch (Exception var3) {
            }

            return new Color(59, 155, 240);
        }
    }

    private void drawLogoImage(float x, float y, float size) {
        try {
            GlStateManager.func_179094_E();
            GlStateManager.func_179147_l();
            GlStateManager.func_179112_b(770, 771);
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            mc.func_110434_K().func_110577_a(LOGO_IMAGE);
            Gui.func_146110_a((int)x, (int)y, 0.0F, 0.0F, (int)size, (int)size, (int)size, (int)size);
            GlStateManager.func_179084_k();
            GlStateManager.func_179121_F();
        } catch (Exception e) {
            Font font18Bold = FontRepository.getFont("inter-bold", 18.0F);
            font18Bold.draw(this.clientName.getValue(), x, y + 5.0F, Color.WHITE.getRGB(), false);
        }
    }

    private int blendAlpha(Color baseColor, float alphaFactor) {
        int alpha = Math.max(0, Math.min(255, (int)(baseColor.getAlpha() * alphaFactor)));
        return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha).getRGB();
    }

    private Color blendColor(Color baseColor, float alphaFactor) {
        int alpha = Math.max(0, Math.min(255, (int)(baseColor.getAlpha() * alphaFactor)));
        return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
    }

    private int getPing() {
        if (mc.field_71439_g != null && mc.func_147114_u() != null) {
            NetworkPlayerInfo playerInfo = mc.func_147114_u().func_175102_a(mc.field_71439_g.func_110124_au());
            return playerInfo != null ? playerInfo.func_178853_c() : 0;
        } else {
            return 0;
        }
    }

    private String getServerIp() {
        if (mc.field_71441_e != null && !mc.func_71356_B()) {
            return mc.func_147104_D() != null ? mc.func_147104_D().field_78845_b : "Unknown";
        } else {
            return "SinglePlayer";
        }
    }

    static {
        REQUIRED_TOOLS.put("minecraft:wool", "Shears");
        REQUIRED_TOOLS.put("minecraft:log", "Iron Axe");
        REQUIRED_TOOLS.put("minecraft:log2", "Iron Axe");
        REQUIRED_TOOLS.put("minecraft:planks", "Iron Axe");
        REQUIRED_TOOLS.put("minecraft:end_stone", "Iron Pickaxe");
        REQUIRED_TOOLS.put("minecraft:obsidian", "Diamond Pickaxe");
    }

    public enum State {
        Normal,
        Action;
    }
}
