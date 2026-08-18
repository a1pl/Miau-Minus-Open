package miau.module.modules.render;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import miau.enums.ChatColors;
import miau.event.EventTarget;
import miau.event.impl.Render3DEvent;
import miau.mixin.IAccessorRenderManager;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.PercentProperty;
import miau.util.player.TeamUtil;
import miau.util.render.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;

public class ItemESP extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final PercentProperty opacity = new PercentProperty("opacity", 25);
    public final BooleanProperty outline = new BooleanProperty("outline", false);
    public final BooleanProperty itemCount = new BooleanProperty("item-count", true);
    public final BooleanProperty autoScale = new BooleanProperty("auto-scale", true);
    public final BooleanProperty emeralds = new BooleanProperty("emeralds", true);
    public final BooleanProperty diamonds = new BooleanProperty("diamonds", true);
    public final BooleanProperty goldd = new BooleanProperty("gold", true);
    public final BooleanProperty iron = new BooleanProperty("iron", true);

    private boolean shouldHighlightItem(int itemId) {
        return this.emeralds.getValue() && this.isEmeraldItem(itemId)
            || this.diamonds.getValue() && this.isDiamondItem(itemId)
            || this.goldd.getValue() && this.isGoldItem(itemId)
            || this.iron.getValue() && this.isIronItem(itemId);
    }

    private boolean isEmeraldItem(int itemId) {
        Item item = Item.func_150899_d(itemId);
        Block block = Block.func_149634_a(item);
        return item == Items.field_151166_bC || block == Blocks.field_150475_bE || block == Blocks.field_150412_bA;
    }

    private boolean isDiamondItem(int itemId) {
        Item item = Item.func_150899_d(itemId);
        Block block = Block.func_149634_a(item);
        return item == Items.field_151045_i
            || item == Items.field_151048_u
            || item == Items.field_151046_w
            || item == Items.field_151047_v
            || item == Items.field_151056_x
            || item == Items.field_151012_L
            || item == Items.field_151161_ac
            || item == Items.field_151163_ad
            || item == Items.field_151173_ae
            || item == Items.field_151175_af
            || block == Blocks.field_150484_ah
            || block == Blocks.field_150482_ag;
    }

    private boolean isGoldItem(int itemId) {
        Item item = Item.func_150899_d(itemId);
        Block block = Block.func_149634_a(item);
        return item == Items.field_151043_k
            || item == Items.field_151074_bl
            || item == Items.field_151153_ao
            || block == Blocks.field_150340_R
            || block == Blocks.field_150352_o;
    }

    private boolean isIronItem(int itemId) {
        Item item = Item.func_150899_d(itemId);
        Block block = Block.func_149634_a(item);
        return item == Items.field_151042_j || block == Blocks.field_150339_S || block == Blocks.field_150366_p;
    }

    private Color getItemColor(int itemId) {
        if (this.isEmeraldItem(itemId)) {
            return new Color(ChatColors.GREEN.toAwtColor());
        } else if (this.isDiamondItem(itemId)) {
            return new Color(ChatColors.AQUA.toAwtColor());
        } else if (this.isGoldItem(itemId)) {
            return new Color(ChatColors.YELLOW.toAwtColor());
        } else {
            return this.isIronItem(itemId)
                ? new Color(ChatColors.WHITE.toAwtColor())
                : new Color(ChatColors.GRAY.toAwtColor());
        }
    }

    private int getItemPriority(int itemId) {
        if (this.isEmeraldItem(itemId)) {
            return 4;
        } else if (this.isDiamondItem(itemId)) {
            return 3;
        } else if (this.isGoldItem(itemId)) {
            return 2;
        } else {
            return this.isIronItem(itemId) ? 1 : 0;
        }
    }

    public ItemESP() {
        super("ItemESP", false);
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled()) {
            LinkedHashMap<ItemESP.ItemData, Integer> itemMap = new LinkedHashMap<>();

            for (Entity entity : TeamUtil.getLoadedEntitiesSorted()) {
                if (entity.field_70173_aa >= 3
                    && (entity.field_70158_ak || RenderUtil.isInViewFrustum(entity.func_174813_aQ(), 0.125))
                    && entity instanceof EntityItem) {
                    EntityItem entityItem = (EntityItem)entity;
                    ItemStack stack = entityItem.func_92059_d();
                    if (stack.field_77994_a > 0) {
                        int itemId = Item.func_150891_b(stack.func_77973_b());
                        if (this.shouldHighlightItem(itemId)) {
                            double x = RenderUtil.lerpDouble(
                                entityItem.field_70165_t, entityItem.field_70142_S, event.getPartialTicks()
                            );
                            double y = RenderUtil.lerpDouble(
                                entityItem.field_70163_u, entityItem.field_70137_T, event.getPartialTicks()
                            );
                            double z = RenderUtil.lerpDouble(
                                entityItem.field_70161_v, entityItem.field_70136_U, event.getPartialTicks()
                            );
                            ItemESP.ItemData data = new ItemESP.ItemData(itemId, x, y, z);
                            Integer id = itemMap.get(data);
                            itemMap.put(
                                new ItemESP.ItemData(itemId, x, y, z), stack.field_77994_a + (id == null ? 0 : id)
                            );
                        }
                    }
                }
            }

            for (Entry<ItemESP.ItemData, Integer> itemEntry : itemMap.entrySet().stream().sorted((entry1, entry2) -> {
                int o = this.getItemPriority(entry1.getKey().itemId);
                int o2 = this.getItemPriority(entry2.getKey().itemId);
                return Integer.compare(o, o2);
            }).collect(Collectors.toList())) {
                Color itemColor = this.getItemColor(itemEntry.getKey().itemId);
                double x = itemEntry.getKey().x - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosX();
                double y = itemEntry.getKey().y - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosY();
                double z = itemEntry.getKey().z - ((IAccessorRenderManager)mc.func_175598_ae()).getRenderPosZ();
                double distance = mc.func_175606_aa()
                    .func_70011_f(itemEntry.getKey().x, itemEntry.getKey().y, itemEntry.getKey().z);
                double scale = 0.5 + 0.375 * ((Math.max(6.0, this.autoScale.getValue() ? distance : 6.0) - 6.0) / 28.0);
                AxisAlignedBB axisAlignedBB = new AxisAlignedBB(
                    x - scale * 0.5, y, z - scale * 0.5, x + scale * 0.5, y + scale, z + scale * 0.5
                );
                RenderUtil.enableRenderState();
                if (this.opacity.getValue() > 0) {
                    RenderUtil.drawFilledBox(
                        axisAlignedBB, itemColor.getRed(), itemColor.getGreen(), itemColor.getBlue()
                    );
                    GlStateManager.func_179117_G();
                }

                if (this.outline.getValue()) {
                    RenderUtil.drawBoundingBox(
                        axisAlignedBB, itemColor.getRed(), itemColor.getGreen(), itemColor.getBlue(), 255, 1.5F
                    );
                    GlStateManager.func_179117_G();
                }

                RenderUtil.disableRenderState();
                if (this.itemCount.getValue()) {
                    GlStateManager.func_179094_E();
                    GlStateManager.func_179137_b(x, y + scale * 0.5, z);
                    GlStateManager.func_179114_b(mc.func_175598_ae().field_78735_i * -1.0F, 0.0F, 1.0F, 0.0F);
                    float flip = mc.field_71474_y.field_74320_O == 2 ? -1.0F : 1.0F;
                    GlStateManager.func_179114_b(mc.func_175598_ae().field_78732_j, flip, 0.0F, 0.0F);
                    double fontScale = -0.04375
                        - 0.0328125 * ((Math.max(6.0, this.autoScale.getValue() ? distance : 6.0) - 6.0) / 28.0);
                    GlStateManager.func_179139_a(fontScale, fontScale, 1.0);
                    GlStateManager.func_179097_i();
                    String countText = String.format("%d", itemEntry.getValue());
                    RenderUtil.drawOutlinedString(
                        countText,
                        (mc.field_71466_p.func_78256_a(countText) / 2.0F - 0.5F) * -1.0F,
                        (mc.field_71466_p.field_78288_b / 2 - 0.5F) * -1.0F
                    );
                    GlStateManager.func_179126_j();
                    GlStateManager.func_179117_G();
                    GlStateManager.func_179121_F();
                }
            }
        }
    }

    public static class ItemData {
        private final int hashCode;
        public final int itemId;
        public final double x;
        public final double y;
        public final double z;

        public ItemData(int id, double x, double y, double z) {
            this.itemId = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.hashCode = Objects.hash(id, (int)x, (int)y, (int)z);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object != null && this.getClass() == object.getClass()) {
                ItemESP.ItemData itemData = (ItemESP.ItemData)object;
                return this.itemId == itemData.itemId
                    && (int)this.x == (int)itemData.x
                    && (int)this.y == (int)itemData.y
                    && (int)this.z == (int)itemData.z;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }
}
